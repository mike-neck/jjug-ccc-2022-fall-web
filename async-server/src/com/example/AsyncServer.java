package com.example;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class AsyncServer {
    static final Logger logger = LoggerFactory.getLogger(AsyncServer.class);

    //Mac だと io.netty:netty-resolver-dns-native-macos が必要
    public static void main(String[] args) {
        SpringApplication.run(AsyncServer.class, args);
    }

    @Bean
    RouterFunction<ServerResponse> routerFunction(@NotNull WebClientCache webClientCache) {
        AtomicInteger requestIdGen = new AtomicInteger();
        return route(GET("/api"), request -> {
            Instant start = Instant.now();
            int requestId = requestIdGen.getAndIncrement();
            String uid = request.headers().firstHeader("X-ID");
            String userId = uid == null ? "00000000-%06d".formatted(requestId) :
                    "%s-%06d".formatted(uid, requestId);

            WebClient webClient = webClientCache.getClient(userId.hashCode());

            return webClient.get()
                    .uri("http://localhost:8000/api")
                    .header("X-ID", userId)
                    .exchangeToMono(response -> SvResponse.parse(uid, userId, response))
                    .flatMap(SvResponse::createResponse)
                    .flatMap(res -> Mono.deferContextual(ctx -> takeLog(res, res.statusCode(), start, uid)))
                    .onErrorResume(th -> errorResponse(userId, th));
        });
    }

    @Bean
    WebClientCache webClientCache() {
        Map<Integer, WebClient> clientMap = new ConcurrentHashMap<>();
        return hash -> {
            int key = hash % 7;
            return clientMap.computeIfAbsent(key, k -> WebClient.builder().build());
        };
    }

    static <R> @NotNull Mono<R> takeLog(@NotNull R object, @NotNull HttpStatus httpStatus, @NotNull Instant start, @Nullable String uid) {
        Instant end = Instant.now();
        long duration = Duration.between(start, end).toMillis();
        logger.info("{} {}ms id={} start={} end={}", httpStatus, duration, uid, start, end);
        return Mono.just(object);
    }

    static @NotNull Mono<ServerResponse> errorResponse(
            @NotNull String userId,
            @NotNull Throwable th) {
        logger.info("[{}] - unhandled error: {} {}", userId, th.getClass(), th.getMessage(), th);
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("unhandled error");
    }
}
