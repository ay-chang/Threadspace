package com.threadspace.backend.integration.testsupport;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpServer;

/**
 * A throwaway local HTTP server for exercising API clients over a real socket:
 * it records every request and answers with whatever the test's handler returns.
 */
public final class StubServer implements AutoCloseable {

    public record Request(String method, String path, String rawQuery, Map<String, String> query,
            Map<String, List<String>> headers, String body) {

        public String header(String name) {
            return headers.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(name))
                    .findFirst()
                    .map(e -> e.getValue().get(0))
                    .orElse(null);
        }
    }

    public record Response(int status, String contentType, byte[] body) {

        public static Response json(int status, String body) {
            return new Response(status, "application/json", body.getBytes(StandardCharsets.UTF_8));
        }

        public static Response bytes(int status, String contentType, byte[] body) {
            return new Response(status, contentType, body);
        }
    }

    private final HttpServer server;
    private final List<Request> requests = new CopyOnWriteArrayList<>();

    public StubServer(Function<Request, Response> handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String rawQuery = exchange.getRequestURI().getRawQuery();
            Request request = new Request(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    rawQuery,
                    parseQuery(rawQuery),
                    Map.copyOf(exchange.getRequestHeaders()),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            requests.add(request);

            Response response = handler.apply(request);
            exchange.getResponseHeaders().set("Content-Type", response.contentType());
            exchange.sendResponseHeaders(response.status(), response.body().length == 0 ? -1 : response.body().length);
            if (response.body().length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(response.body());
                }
            }
            exchange.close();
        });
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public List<Request> requests() {
        return requests;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return Map.of();
        }
        return java.util.Arrays.stream(rawQuery.split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(
                        kv -> URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        kv -> kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "",
                        (a, b) -> b,
                        java.util.LinkedHashMap::new));
    }
}
