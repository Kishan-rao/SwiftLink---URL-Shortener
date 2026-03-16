package com.kishanrao.shortener.integration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Checks if Docker is available for Testcontainers (daemon reachable via TCP or default).
 * Used to skip integration tests when Docker is not usable (e.g. Docker Desktop
 * not exposing TCP on Windows).
 */
public final class DockerAssumptions {

    private static final String DOCKER_HOST = System.getenv("DOCKER_HOST");
    private static final int TCP_CHECK_TIMEOUT_MS = 2_000;

    private DockerAssumptions() {}

    /**
     * Returns true if Docker is reachable by Testcontainers. On Windows we require
     * TCP (e.g. port 2375) because the npipe often returns a stub and fails; on other
     * OS use "docker info" as the check.
     */
    public static boolean isDockerAvailable() {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("windows");
        if (isWindows) {
            // .testcontainers.properties uses tcp://localhost:2375 on Windows
            return isTcpDockerReachable("tcp://localhost:2375");
        }
        if (DOCKER_HOST != null && DOCKER_HOST.startsWith("tcp://")) {
            return isTcpDockerReachable(DOCKER_HOST);
        }
        return isDockerCliAvailable();
    }

    private static boolean isTcpDockerReachable(String dockerHost) {
        // Parse tcp://host:port
        String withoutScheme = dockerHost.substring(6).trim();
        int colon = withoutScheme.lastIndexOf(':');
        if (colon <= 0) return false;
        String host = withoutScheme.substring(0, colon).trim();
        if (host.isEmpty()) host = "localhost";
        int port;
        try {
            port = Integer.parseInt(withoutScheme.substring(colon + 1).trim());
        } catch (NumberFormatException e) {
            return false;
        }
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), TCP_CHECK_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isDockerCliAvailable() {
        ProcessBuilder pb = new ProcessBuilder("docker", "info");
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        try {
            Process p = pb.start();
            return p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
