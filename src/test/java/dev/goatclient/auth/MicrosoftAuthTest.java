package dev.goatclient.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MicrosoftAuthTest {
    private static final String CLIENT_ID = "21ce9eb9-4ae6-45bf-a7de-a72e75fda111";
    private static final String DEVICE_REPLY = """
            {"device_code":"device-secret","user_code":"GOAT-1234",
             "verification_uri":"https://www.microsoft.com/link","expires_in":900,"interval":1}
            """;
    private HttpServer server;
    private MicrosoftAuth auth;
    private final List<Long> delays = new ArrayList<>();
    private final ConcurrentHashMap<String, Request> requests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Reply> replies = new ConcurrentHashMap<>();
    private final AtomicInteger polls = new AtomicInteger();
    private boolean slowPolling;

    @BeforeEach void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        replies.put("/device", new Reply(200, DEVICE_REPLY));
        replies.put("/token", new Reply(200, "{\"access_token\":\"ms-secret\"}"));
        replies.put("/xbox", new Reply(200, "{\"Token\":\"xbox-secret\"}"));
        replies.put("/xsts", new Reply(200, """
                {"Token":"xsts-secret","DisplayClaims":{"xui":[{"uhs":"user-hash","xid":"12345"}]}}
                """));
        replies.put("/minecraft", new Reply(200, "{\"access_token\":\"mc-secret\",\"expires_in\":3600}"));
        replies.put("/license", new Reply(200, "{\"items\":[{\"name\":\"game_minecraft\"}]}"));
        replies.put("/profile", new Reply(200, "{\"id\":\"1234567812344234a234123456789abc\",\"name\":\"GoatPlayer\"}"));
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            requests.put(path, new Request(exchange.getRequestMethod(),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                    exchange.getRequestHeaders().getFirst("Authorization")));
            Reply reply = replies.getOrDefault(path, new Reply(404, "{}"));
            if (path.equals("/token")) {
                int count = polls.incrementAndGet();
                if (slowPolling && count < 3) reply = new Reply(400, count == 1
                        ? "{\"error\":\"authorization_pending\"}" : "{\"error\":\"slow_down\"}");
            }
            byte[] body = reply.body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        auth = new MicrosoftAuth(HttpClient.newHttpClient(), new MicrosoftAuth.Endpoints(
                base.resolve("/device"), base.resolve("/token"), base.resolve("/xbox"), base.resolve("/xsts"),
                base.resolve("/minecraft"), base.resolve("/license"), base.resolve("/profile")), delays::add);
    }

    @AfterEach void stop() { server.stop(0); }

    @Test void completesTheWholeChainAndRespectsPollingBackoff() throws Exception {
        slowPolling = true;
        var account = auth.finish(auth.begin(CLIENT_ID), ignored -> {});
        assertEquals("GoatPlayer", account.name());
        assertEquals(UUID.fromString("12345678-1234-4234-a234-123456789abc"), account.uuid());
        assertEquals("mc-secret", account.accessToken());
        assertEquals("12345", account.xuid());
        assertEquals(List.of(1L, 1L, 6L), delays);
        assertTrue(requests.get("/device").body.contains("scope=XboxLive.signin"));
        assertFalse(requests.get("/device").body.contains("offline_access"));
        assertTrue(requests.get("/token").body.contains("device_code=device-secret"));
        var xbox = JsonParser.parseString(requests.get("/xbox").body).getAsJsonObject();
        assertEquals("d=ms-secret", xbox.getAsJsonObject("Properties").get("RpsTicket").getAsString());
        var minecraft = JsonParser.parseString(requests.get("/minecraft").body).getAsJsonObject();
        assertEquals("XBL3.0 x=user-hash;xsts-secret", minecraft.get("identityToken").getAsString());
        assertEquals("Bearer mc-secret", requests.get("/profile").authorization);
        assertTrue(account.expiresAt().isAfter(Instant.now()));
    }

    @Test void missingMinecraftProfileIsNotAnAuthenticatedAccount() throws Exception {
        replies.put("/profile", new Reply(404, "{\"error\":\"NOT_FOUND\"}"));
        var code = auth.begin(CLIENT_ID);
        var error = assertThrows(MicrosoftAuth.AuthException.class, () -> auth.finish(code, ignored -> {}));
        assertTrue(error.getMessage().contains("não tem um perfil Minecraft Java"));
    }

    @Test void rejectsMissingLicenseBeforeLoadingAProfile() throws Exception {
        replies.put("/license", new Reply(200, "{\"items\":[]}"));
        var code = auth.begin(CLIENT_ID);
        assertTrue(assertThrows(MicrosoftAuth.AuthException.class,
                () -> auth.finish(code, ignored -> {})).getMessage().contains("licença ativa"));
        assertFalse(requests.containsKey("/profile"));
    }

    @Test void handlesDeclinedAndExpiredDeviceCodes() throws Exception {
        replies.put("/token", new Reply(400, "{\"error\":\"authorization_declined\"}"));
        var code = auth.begin(CLIENT_ID);
        assertTrue(assertThrows(MicrosoftAuth.AuthException.class,
                () -> auth.finish(code, ignored -> {})).getMessage().contains("cancelado"));
        var expired = new MicrosoftAuth.DeviceCode(CLIENT_ID, "secret", "code", URI.create("https://microsoft.com/link"),
                Instant.now().minusSeconds(1), 1);
        assertTrue(assertThrows(MicrosoftAuth.AuthException.class,
                () -> auth.finish(expired, ignored -> {})).getMessage().contains("expirou"));
    }

    @Test void handlesXboxFamilyRestriction() throws Exception {
        replies.put("/xsts", new Reply(401, "{\"XErr\":2148916238}"));
        var code = auth.begin(CLIENT_ID);
        assertTrue(assertThrows(MicrosoftAuth.AuthException.class,
                () -> auth.finish(code, ignored -> {})).getMessage().contains("família Microsoft"));
        assertFalse(requests.containsKey("/minecraft"));
    }

    @Test void rejectsUnexpectedBrowserDestinations() {
        replies.put("/device", new Reply(200, DEVICE_REPLY.replace("https://www.microsoft.com/link", "https://example.com/login")));
        assertThrows(MicrosoftAuth.AuthException.class, () -> auth.begin(CLIENT_ID));
    }

    @Test void errorsAndDiagnosticStringsDoNotExposeCredentials() throws Exception {
        replies.put("/minecraft", new Reply(403, "{\"error_description\":\"mc-secret ms-secret\"}"));
        var code = auth.begin(CLIENT_ID);
        var error = assertThrows(MicrosoftAuth.AuthException.class, () -> auth.finish(code, ignored -> {}));
        assertFalse(error.toString().contains("secret"));
        assertFalse(code.toString().contains("device-secret"));
        var account = new MicrosoftAccount("GoatPlayer", UUID.randomUUID(), "mc-secret", "123", CLIENT_ID, Instant.now());
        assertFalse(account.toString().contains("mc-secret"));
    }

    @Test void malformedServiceResponsesAreSanitized() throws Exception {
        replies.put("/device", new Reply(200, "invalid secret response"));
        var error = assertThrows(MicrosoftAuth.AuthException.class, () -> auth.begin(CLIENT_ID));
        assertFalse(error.toString().contains("secret"));
    }

    @Test void cancellationStopsBeforeSendingCredentials() throws Exception {
        var code = auth.begin(CLIENT_ID);
        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedException.class, () -> auth.finish(code, ignored -> {}));
        } finally {
            Thread.interrupted();
        }
        assertFalse(requests.containsKey("/token"));
    }

    @Test void requiresAnApplicationIdWithoutMakingRequests() {
        assertThrows(MicrosoftAuth.AuthException.class, () -> auth.begin(""));
        assertTrue(requests.isEmpty());
    }

    private record Reply(int status, String body) {}
    private record Request(String method, String body, String authorization) {}
}
