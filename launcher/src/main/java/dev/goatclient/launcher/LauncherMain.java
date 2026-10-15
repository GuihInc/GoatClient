package dev.goatclient.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class LauncherMain {
    private LauncherMain() {}

    public static void main(String[] args) {
        try {
            if (Arrays.asList(args).contains("--self-test")) {
                var catalog = new ReleaseCatalog(Path.of(System.getProperty("java.io.tmpdir"), "goat-self-test-unused"));
                var releases = catalog.load();
                if (releases.isEmpty() || LauncherMain.class.getResource("/brand/logo.png") == null) {
                    throw new IOException("Recursos do launcher incompletos.");
                }
                System.out.println("GoatClient Launcher OK / " + Platform.current().os() + " / " + releases.getFirst());
                return;
            }
            Path root = Path.of(option(args, "--data-dir", Platform.current().dataDirectory().toString())).toAbsolutePath().normalize();
            Files.createDirectories(root);
            try { Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------")); }
            catch (UnsupportedOperationException ignored) { }
            LauncherSettings settings = LauncherSettings.load(root);
            String javaOverride = option(args, "--java", settings.javaPath);
            settings.javaPath = javaOverride;
            String seed = option(args, "--cache-seed", "");
            Downloader downloads = new Downloader(seed.isBlank() ? null : Path.of(seed));
            var catalog = new ReleaseCatalog(root);
            String importFile = option(args, "--import-catalog", "");
            if (!importFile.isBlank()) catalog.importFile(Path.of(importFile));
            if (Arrays.asList(args).contains("--prepare") || Arrays.asList(args).contains("--offline")) {
                String selected = option(args, "--release", "");
                var releases = catalog.load();
                var release = selected.isBlank() ? releases.getFirst() : releases.stream().filter(entry -> entry.id().equals(selected))
                        .findFirst().orElseThrow(() -> new IOException("Versão não encontrada no catálogo."));
                boolean prepare = Arrays.asList(args).contains("--prepare");
                var account = LaunchCommand.Account.offline(option(args, "--offline", settings.nickname));
                GameRunner runner = new GameRunner();
                int stopAfter = Integer.parseInt(option(args, "--stop-after", "0"));
                java.util.function.Consumer<String> progress = text -> {
                    System.out.println(text);
                    if (stopAfter > 0 && text.startsWith("Minecraft em execução")) Thread.ofVirtual().start(() -> {
                        try { Thread.sleep(java.time.Duration.ofSeconds(stopAfter)); runner.cancel(); }
                        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                    });
                };
                try {
                    int result = runner.run(root, release, settings, account, downloads, !prepare, progress);
                    if (result != 0) System.exit(result);
                } catch (InterruptedException exception) {
                    if (stopAfter <= 0) throw exception;
                    System.out.println("Teste de inicialização encerrado.");
                }
                return;
            }
            String screenshot = option(args, "--ui-smoke-test", "");
            SwingUtilities.invokeLater(() -> {
                try {
                    LauncherFrame frame = new LauncherFrame(root, settings, downloads);
                    frame.setVisible(true);
                    if (!screenshot.isBlank()) {
                        var timer = new javax.swing.Timer(1200, event -> {
                            try {
                                var content = frame.getContentPane();
                                var image = new java.awt.image.BufferedImage(content.getWidth(), content.getHeight(),
                                        java.awt.image.BufferedImage.TYPE_INT_RGB);
                                var graphics = image.createGraphics();
                                try { content.printAll(graphics); }
                                finally { graphics.dispose(); }
                                Path output = Path.of(screenshot).toAbsolutePath();
                                Files.createDirectories(output.getParent());
                                javax.imageio.ImageIO.write(image, "png", output.toFile());
                                System.out.println("Interface validada: " + output);
                                frame.dispose();
                            } catch (Exception exception) {
                                System.err.println("Falha no teste da interface: " + exception.getMessage());
                                System.exit(1);
                            }
                        });
                        timer.setRepeats(false);
                        timer.start();
                    }
                }
                catch (Exception exception) { JOptionPane.showMessageDialog(null, exception.getMessage(), "GoatClient", JOptionPane.ERROR_MESSAGE); }
            });
        } catch (Exception exception) {
            System.err.println("GoatClient: " + (exception.getMessage() == null ? "Não foi possível iniciar o launcher." : exception.getMessage()));
            System.exit(1);
        }
    }

    private static String option(String[] args, String name, String fallback) throws IOException {
        for (int index = 0; index < args.length; index++) {
            if (args[index].equals(name)) {
                if (index + 1 >= args.length || args[index + 1].startsWith("--")) throw new IOException("Informe um valor para " + name);
                return args[index + 1];
            }
        }
        return fallback;
    }
}
