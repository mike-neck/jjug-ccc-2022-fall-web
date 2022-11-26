package com.example;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

sealed interface SvResponse permits SvResponse.IoError, SvResponse.Success {
    static @NotNull Mono<SvResponse> parse(String uid, String userId, @NotNull ClientResponse response) {
        List<String> xid = response.headers().header("X-ID");
        String did = xid.isEmpty() ? userId : xid.get(0);
        if (response.statusCode() != HttpStatus.OK) {
            return Mono.just(new IoError(uid, did, new Exception("http status = %s".formatted(response.statusCode()))));
        }
        return response.body(BodyExtractors.toDataBuffers())
                .<List<DataBuffer>>collect(ArrayList::new, List::add)
                .map(buffers -> buffers
                        .stream()
                        .map(buffer -> buffer.toString(StandardCharsets.UTF_8))
                        .collect(Collectors.joining()))
                .map(text -> Optional.ofNullable(Message.parse(text))
                        .<SvResponse>map(message -> new Success(uid, did, message))
                        .orElseGet(() -> new IoError(uid, did, new Exception("invalid text error\n%s".formatted(text)))));
    }

    @NotNull Mono<ServerResponse> createResponse();

    record IoError(String uid, String did, @NotNull Throwable e) implements SvResponse {
        static final Logger logger = LoggerFactory.getLogger(IoError.class);

        @Override
        public @NotNull Mono<ServerResponse> createResponse() {
            logger.info("error response: {}", e.getMessage());
            return ServerResponse
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-ID", uid)
                    .header("X-BACKEND-ID", did)
                    .contentType(MediaType.TEXT_PLAIN)
                    .bodyValue(e.getMessage());
        }
    }

    record Success(String uid, String did, @NotNull Message message) implements SvResponse {
        @Override
        public @NotNull Mono<ServerResponse> createResponse() {
            return ServerResponse
                    .ok()
                    .header("X-ID", uid)
                    .header("X-BACKEND-ID", did)
                    .contentType(MediaType.TEXT_PLAIN)
                    .bodyValue(message.text());
        }
    }
}
