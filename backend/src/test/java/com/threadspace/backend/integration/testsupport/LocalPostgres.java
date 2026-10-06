package com.threadspace.backend.integration.testsupport;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * A disposable Postgres cluster in a temp directory on a random localhost port,
 * started from the locally installed server binaries. Nothing is shared with any
 * other Postgres on the machine. Tests should skip when {@link #binDir()} is empty.
 */
public final class LocalPostgres implements AutoCloseable {

    private final Path dataDir;
    private final Path binDir;
    private final int port;

    private LocalPostgres(Path binDir, Path dataDir, int port) {
        this.binDir = binDir;
        this.dataDir = dataDir;
        this.port = port;
    }

    public static Optional<Path> binDir() {
        List<Path> candidates = new ArrayList<>();
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                candidates.add(Path.of(dir));
            }
        }
        candidates.add(Path.of("/opt/homebrew/bin"));
        candidates.add(Path.of("/usr/local/bin"));
        return candidates.stream()
                .filter(dir -> Files.isExecutable(dir.resolve("initdb")) && Files.isExecutable(dir.resolve("pg_ctl")))
                .findFirst();
    }

    public static LocalPostgres start() throws Exception {
        Path bin = binDir().orElseThrow(() -> new IllegalStateException("Postgres binaries not found"));
        Path data = Files.createTempDirectory("threadspace-pg-");
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        run(bin.resolve("initdb").toString(), "-D", data.toString(), "-U", "postgres",
                "--auth=trust", "-E", "UTF8", "--no-instructions");
        run(bin.resolve("pg_ctl").toString(), "-D", data.toString(), "-l", data.resolve("server.log").toString(),
                "-w", "-o", "-p " + port + " -k " + data + " -c listen_addresses=127.0.0.1", "start");
        return new LocalPostgres(bin, data, port);
    }

    public String jdbcUrl() {
        return "jdbc:postgresql://127.0.0.1:" + port + "/postgres";
    }

    public int port() {
        return port;
    }

    @Override
    public void close() throws Exception {
        try {
            run(binDir.resolve("pg_ctl").toString(), "-D", dataDir.toString(), "-m", "immediate", "-w", "stop");
        } finally {
            try (Stream<Path> files = Files.walk(dataDir)) {
                files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private static void run(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IllegalStateException("Command failed: " + String.join(" ", command) + "\n" + output);
        }
    }
}
