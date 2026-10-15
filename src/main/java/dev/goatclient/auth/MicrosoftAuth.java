package dev.goatclient.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Microsoft device authorization -> Xbox Live -> XSTS -> licensed Minecraft profile. */
public final class MicrosoftAuth {
    private static final String SCOPE = "XboxLive.signin";
    private final HttpClient http;
    private final Endpoints endpoints;
    private final Waiter waiter;

    public MicrosoftAuth() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                        .followRedirects(HttpClient.Redirect.NEVER).build(),
                Endpoints.production(), seconds -> Thread.sleep(Duration.ofSeconds(seconds)));
    }

    // Injectable transport endpoints and wait function keep tests entirely local.
    MicrosoftAuth(HttpClient http, Endpoints endpoints, Waiter waiter) {
        this.http = http;
        this.endpoints = endpoints;
        this.waiter = waiter;
    }

    public DeviceCode begin(String applicationId) throws IOException, InterruptedException {
        String clientId;
        try {
            clientId = UUID.fromString(applicationId.strip()).toString();
        } catch (RuntimeException exception) {
            throw new AuthException("Configure um Application (client) ID válido da Microsoft.");
        }
        Reply reply = form(endpoints.deviceCode, Map.of("client_id", clientId, "scope", SCOPE));
        requireSuccess(reply, "solicitar o código Microsoft");
        JsonObject json = reply.json;
        try {
            URI verification = URI.create(required(json, "verification_uri"));
            if (!"https".equals(verification.getScheme()) || verification.getUserInfo() != null
                    || !java.util.Set.of("microsoft.com", "www.microsoft.com", "login.microsoftonline.com")
                    .contains(verification.getHost())) {
                throw new AuthException("A Microsoft retornou um endereço de login inesperado.");
            }
            long lifetime = json.get("expires_in").getAsLong();
            if (lifetime <= 0 || lifetime > 3600) throw new IllegalArgumentException();
            int interval = json.has("interval") ? json.get("interval").getAsInt() : 5;
            return new DeviceCode(clientId, required(json, "device_code"), required(json, "user_code"),
                    verification, Instant.now().plusSeconds(lifetime), Math.max(1, Math.min(interval, 60)));
        } catch (AuthException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }
    }

    public MicrosoftAccount finish(DeviceCode code, Consumer<String> progress) throws IOException, InterruptedException {
        String microsoftToken = poll(code);
        progress.accept("Validando conta Xbox...");
        JsonObject xblProperties = new JsonObject();
        xblProperties.addProperty("AuthMethod", "RPS");
        xblProperties.addProperty("SiteName", "user.auth.xboxlive.com");
        xblProperties.addProperty("RpsTicket", "d=" + microsoftToken);
        Reply xbl = json(endpoints.xbox, authBody(xblProperties, "http://auth.xboxlive.com"));
        requireSuccess(xbl, "autenticar no Xbox Live");

        JsonObject xstsProperties = new JsonObject();
        xstsProperties.addProperty("SandboxId", "RETAIL");
        var userTokens = new com.google.gson.JsonArray();
        userTokens.add(required(xbl.json, "Token"));
        xstsProperties.add("UserTokens", userTokens);
        Reply xsts = json(endpoints.xsts, authBody(xstsProperties, "rp://api.minecraftservices.com/"));
        if (xsts.status == 401) {
            String error = xsts.json.has("XErr") ? xsts.json.get("XErr").getAsString() : "";
            throw new AuthException(switch (error) {
                case "2148916233" -> "Crie um perfil Xbox para essa conta Microsoft e tente novamente.";
                case "2148916238" -> "Essa conta precisa ser configurada em uma família Microsoft.";
                case "2148916235" -> "O Xbox Live não está disponível para a região dessa conta.";
                default -> "O Xbox não autorizou essa conta. Confira as configurações da conta Microsoft.";
            });
        }
        requireSuccess(xsts, "autorizar a conta Xbox");
        String uhs;
        String xuid;
        try {
            JsonObject claims = xsts.json.getAsJsonObject("DisplayClaims").getAsJsonArray("xui")
                    .get(0).getAsJsonObject();
            uhs = required(claims, "uhs");
            xuid = claims.has("xid") ? claims.get("xid").getAsString() : "";
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }

        progress.accept("Verificando Minecraft Java...");
        JsonObject login = new JsonObject();
        login.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + required(xsts.json, "Token"));
        Reply minecraft = json(endpoints.minecraftLogin, login);
        requireSuccess(minecraft, "entrar nos serviços do Minecraft");
        String token = required(minecraft.json, "access_token");
        Reply license = get(endpoints.license, token);
        requireSuccess(license, "verificar a licença Minecraft Java");
        boolean licensed = false;
        try {
            for (var item : license.json.getAsJsonArray("items")) {
                String name = item.getAsJsonObject().get("name").getAsString();
                if (name.equals("game_minecraft") || name.equals("product_minecraft")) licensed = true;
            }
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }
        if (!licensed) throw new AuthException("Essa conta não possui uma licença ativa de Minecraft Java.");
        Reply profile = get(endpoints.profile, token);
        if (profile.status == 404) throw new AuthException(
                "Essa conta não tem um perfil Minecraft Java. Verifique a licença e crie o perfil no site do Minecraft.");
        requireSuccess(profile, "carregar o perfil Minecraft Java");
        try {
            String id = required(profile.json, "id");
            if (!id.matches("[0-9a-fA-F]{32}")) throw new IllegalArgumentException();
            UUID uuid = UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-"
                    + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20));
            long expires = minecraft.json.get("expires_in").getAsLong();
            if (expires <= 0) throw new IllegalArgumentException();
            return new MicrosoftAccount(OfflineAccount.validateName(required(profile.json, "name")), uuid,
                    token, xuid, code.clientId, Instant.now().plusSeconds(expires));
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }
    }

    private String poll(DeviceCode code) throws IOException, InterruptedException {
        long interval = code.intervalSeconds;
        while (Instant.now().isBefore(code.expiresAt)) {
            waiter.await(interval);
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            if (!Instant.now().isBefore(code.expiresAt)) break;
            Reply reply = form(endpoints.token, Map.of("client_id", code.clientId,
                    "device_code", code.deviceCode, "grant_type", "urn:ietf:params:oauth:grant-type:device_code"));
            if (reply.status == 200) return required(reply.json, "access_token");
            String error = reply.json.has("error") ? reply.json.get("error").getAsString() : "";
            switch (error) {
                case "authorization_pending" -> { }
                case "slow_down" -> interval += 5;
                case "authorization_declined", "access_denied" -> throw new AuthException("Login cancelado na Microsoft.");
                case "expired_token" -> throw new AuthException("O código expirou. Inicie o login novamente.");
                default -> throw new AuthException("A Microsoft recusou o login. Confira o Application (client) ID e a configuração do aplicativo.");
            }
        }
        throw new AuthException("O código expirou. Inicie o login novamente.");
    }

    private Reply form(URI uri, Map<String, String> values) throws IOException, InterruptedException {
        String body = values.entrySet().stream().map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        return send(HttpRequest.newBuilder(uri).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    private Reply json(URI uri, JsonObject body) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())));
    }

    private Reply get(URI uri, String token) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri).header("Authorization", "Bearer " + token).GET());
    }

    private Reply send(HttpRequest.Builder request) throws IOException, InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        HttpResponse<String> response = http.send(request.timeout(Duration.ofSeconds(25))
                .header("Accept", "application/json").build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        try {
            return new Reply(response.statusCode(), JsonParser.parseString(response.body()).getAsJsonObject());
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }
    }

    private static JsonObject authBody(JsonObject properties, String relyingParty) {
        JsonObject body = new JsonObject();
        body.add("Properties", properties);
        body.addProperty("RelyingParty", relyingParty);
        body.addProperty("TokenType", "JWT");
        return body;
    }

    private static void requireSuccess(Reply reply, String stage) throws AuthException {
        if (reply.status < 200 || reply.status >= 300) {
            throw new AuthException("Não foi possível " + stage + " (HTTP " + reply.status + ").");
        }
    }

    private static String required(JsonObject json, String name) throws AuthException {
        try {
            String value = json.get(name).getAsString();
            if (value.isBlank()) throw new IllegalArgumentException();
            return value;
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }
    }

    private static AuthException invalidResponse() {
        return new AuthException("O serviço de autenticação retornou uma resposta inválida. Tente novamente.");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record DeviceCode(String clientId, String deviceCode, String userCode, URI verificationUri,
                             Instant expiresAt, int intervalSeconds) {
        @Override public String toString() { return "DeviceCode[credentials=REDACTED]"; }
    }

    record Endpoints(URI deviceCode, URI token, URI xbox, URI xsts, URI minecraftLogin, URI license, URI profile) {
        static Endpoints production() {
            String microsoft = "https://login.microsoftonline.com/consumers/oauth2/v2.0/";
            return new Endpoints(URI.create(microsoft + "devicecode"), URI.create(microsoft + "token"),
                    URI.create("https://user.auth.xboxlive.com/user/authenticate"),
                    URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"),
                    URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"),
                    URI.create("https://api.minecraftservices.com/entitlements/license?requestId=" + UUID.randomUUID()),
                    URI.create("https://api.minecraftservices.com/minecraft/profile"));
        }
    }

    @FunctionalInterface interface Waiter { void await(long seconds) throws InterruptedException; }
    private record Reply(int status, JsonObject json) {}
    public static final class AuthException extends IOException {
        public AuthException(String message) { super(message); }
    }
}
