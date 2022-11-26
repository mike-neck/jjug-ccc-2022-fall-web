package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("RedundantStringFormatCall")
public class JdkServer implements HttpHandler {

    static final int SERVER_PORT = 8080;
    public static void main(String[] args) throws IOException, InterruptedException {
        ServerType serverType = ServerType.parse(args);
        CountDownLatch latch = new CountDownLatch(1);
        try (
                ExecutorService executor = serverType.executor(latch::countDown);
                ExecutorService client = Executors.newFixedThreadPool(2)
        ) {
            serverType.showCondition();
            JdkServer jdkServer = new JdkServer(client);
            InetSocketAddress address = new InetSocketAddress(SERVER_PORT);
            HttpServer server = HttpServer.create(address, 600);
            server.setExecutor(executor);
            server.createContext("/api").setHandler(jdkServer);
            server.start();
            latch.await();
            server.stop(0);
        }
    }

    record ReqSeq(int value) {
        @NotNull Hash hash() {
            int hash = value % 101;
            return new Hash(hash);
        }
    }

    record Hash(int value) {
    }

    private final @NotNull Map<Hash, HttpClient> clients = new ConcurrentHashMap<>();
    private final @NotNull AtomicInteger count = new AtomicInteger();
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull ExecutorService executor;

    public JdkServer(@NotNull ExecutorService executor) {
        this.executor = executor;
    }

    @NotNull ReqSeq id() {
        int id = count.incrementAndGet();
        return new ReqSeq(id);
    }

    @NotNull HttpRequest newRequest(@NotNull String xid) {
        return HttpRequest
                .newBuilder(URI.create("http://localhost:8000/api"))
                .header("X-ID", xid)
                .GET()
                .build();
    }

    @NotNull HttpClient newClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(executor)
                .build();
    }

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {
        ReqSeq id = id();
        HttpClient client = clients.computeIfAbsent(id.hash(), i -> newClient());

        Instant start = Instant.now();
        String userId = exchange.getRequestHeaders().getFirst("X-ID");
        String xid = userId == null ?
                "000000-%06d".formatted(id.value()) :
                userId.matches("^\\d+$") ?
                        "%06d-%06d".formatted(Integer.parseInt(userId), id.value()) :
                        "%s-%06d".formatted(userId, id.value());
        exchange.getRequestBody().readAllBytes();
        logRequest(start, userId, xid);
        HttpRequest request = newRequest(xid);
        try {
            HttpResponse<Void> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.discarding());
            Instant end = Instant.now();
            Duration time = Duration.between(start, end);
            Map<String, Object> body = Map.of(
                    "start", start.toString(),
                    "end", end.toString(),
                    "time", time.toMillis(),
                    "id", userId == null ? "000000" : userId,
                    "xid", xid,
                    "status", response.statusCode()
            );
            String json = objectMapper.writeValueAsString(body);
            Headers headers = exchange.getResponseHeaders();
            headers.set("X-ID", userId == null ? "000000" : userId);
            headers.set("X-USER-ID", xid);
            headers.set("Content-Type", "application/json");
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream res = exchange.getResponseBody()) {
                res.write(bytes);
            }
            Instant e = Instant.now();
            logResponse(Duration.between(start, e), userId, xid, response.statusCode());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void logRequest(@NotNull Instant start, @Nullable String userId, @NotNull String xid) {
        Thread thread = Thread.currentThread();
        System.out.println("request[%s] %s user=%s local=%s".formatted(thread.getName(), start, userId, xid));
    }

    static void logResponse(@NotNull Duration time, @Nullable String userId, @NotNull String xid, int http) {
        Thread thread = Thread.currentThread();
        System.out.println("response[%s]%d %dms user=%s local=%s".formatted(thread.getName(), http, time.toMillis(), userId, xid));
    }
}
