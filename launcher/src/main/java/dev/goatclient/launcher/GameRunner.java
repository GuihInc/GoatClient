package dev.goatclient.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.function.Consumer;

public final class GameRunner {
    private volatile Process process;
    private volatile Thread worker;

    public boolean gameRunning() { return process != null && process.isAlive(); }

    public void cancel() {
        if (process != null && process.isAlive()) process.destroy();
        if (worker != null) worker.interrupt();
    }

    public int run(Path root, ReleaseCatalog.Release release, LauncherSettings settings,
                   LaunchCommand.Account account, Downloader downloads, boolean launch, Consumer<String> progress)
            throws IOException, InterruptedException {
        worker = Thread.currentThread();
        Path game = root.resolve("instances/" + release.id());
        Files.createDirectories(game);
        try (var channel = FileChannel.open(game.resolve(".launcher.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            var lock = channel.tryLock();
            if (lock == null) throw new IOException("Essa instalação já está sendo preparada ou está em execução.");
            try (lock) {
                var prepared = new MinecraftInstaller(root, Platform.current(), downloads).prepare(release, settings.javaPath, progress);
                if (!launch) { progress.accept("Instalação pronta."); return 0; }
                Downloader.checkInterrupted();
                SharedMinecraftOptions.load(root, game);
                var arguments = LaunchCommand.arguments(prepared, account, settings.memoryMb, Platform.current());
                Path argFile = Files.createTempFile(game, ".launch-", ".args");
                try {
                    try { Files.setPosixFilePermissions(argFile, PosixFilePermissions.fromString("rw-------")); }
                    catch (UnsupportedOperationException ignored) { /* Windows inherits the user's data-directory ACL. */ }
                    Files.writeString(argFile, LaunchCommand.argumentFile(arguments), StandardCharsets.UTF_8);
                    progress.accept("Minecraft em execução. Feche o jogo para voltar a jogar outra versão.");
                    process = new ProcessBuilder(prepared.java().toString(), "@" + argFile)
                            .directory(game.toFile()).redirectErrorStream(true).start();
                    try (var reader = process.inputReader(StandardCharsets.UTF_8);
                         var log = Files.newBufferedWriter(game.resolve("launcher.log"), StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (account.online()) line = line.replace(account.token(), "[REDACTED]");
                            log.write(line);
                            log.newLine();
                            log.flush();
                        }
                    }
                    int result = process.waitFor();
                    progress.accept(result == 0 ? "Jogo encerrado." : "O jogo encerrou com erro. Consulte launcher.log na pasta da versão.");
                    return result;
                } finally {
                    if (process != null && process.isAlive()) process.destroy();
                    process = null;
                    try { SharedMinecraftOptions.save(root, game); }
                    finally { Files.deleteIfExists(argFile); }
                }
            }
        } catch (java.nio.channels.OverlappingFileLockException exception) {
            throw new IOException("Essa instalação já está em execução.");
        } finally { worker = null; }
    }
}
