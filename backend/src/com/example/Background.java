package com.example;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class Background {
    static final Logger logger = LoggerFactory.getLogger(Background.class);

    public static void main(String[] args) {
        SpringApplication.run(Background.class);
    }

    @NotNull String response(@NotNull String id, @NotNull Instant start, int delay) {
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        logger.info("handle:[{}]delay={} start={} end={} [{}]", id, 2_000 + delay, start, end, duration.toMillis());
        return """
                %s
                %s
                %s
                """.formatted(start, id, end);
    }

    @Bean
    RouterFunction<ServerResponse> routerFunction() {
        return route(GET("/api"), request -> {
            String id = request.headers().firstHeader("X-ID");
            if (id == null) {
                return ServerResponse.badRequest()
                        .contentType(MediaType.TEXT_PLAIN)
                        .bodyValue("no-id");
            }
            Instant start = Instant.now();
            int adj = ThreadLocalRandom.current().nextInt(400) - 200;
            //レスポンスに2秒かかってしまいますの
            return Mono.delay(Duration.ofMillis(2_000 + adj))
                    .map(n -> response(id, start, adj))
                    .flatMap(msg ->
                            ServerResponse.ok()
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .header("X-ID", id)
                                    .bodyValue(msg));
        });
    }
}
