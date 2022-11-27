package com.example;

import com.example.db.Author;
import com.example.values.AuthorName;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("api")
public record Web(
        @NotNull Clock clock,
        @NotNull RestTemplate restTemplate,
        @NotNull ReentrantLock lock,
        @NotNull Db db,
        @NotNull Sync sync
) {

    static final Logger logger = LoggerFactory.getLogger(Web.class);

    @GetMapping(value = "", produces = "application/json")
    @NotNull ResponseEntity<Map<String, String>> api() {
        // synchronizedするオブジェクトの使用とともにロックを取得
        // 取得できなければブロック
        // ここでのブロックはアンマウントする
        if (sync.useMonitor()) {
            return monitorApiCall();
        } else {
            return lockApiCall();
        }
    }

    @NotNull ResponseEntity<Map<String, String>> monitorApiCall() {
        synchronized (lock) {
            return apiCall();
        }
    }

    @NotNull ResponseEntity<Map<String, String>> lockApiCall() {
        ReentrantLock reentrantLock = lock;
        reentrantLock.lock();
        try {
            return apiCall();
        } finally {
            reentrantLock.unlock();
        }
    }

    @NotNull
    private ResponseEntity<Map<String, String>> apiCall() {
        String reqId = MDC.get("request-id");
        int id = ThreadLocalRandom.current().nextInt(0, 10000);
        logger.info("synchronized request-id: {}, id: {}", reqId, id);
        RequestEntity<Void> request = RequestEntity
                .get("http://localhost:8000/api")
                .header("X-ID", Integer.toString(id))
                .accept(MediaType.APPLICATION_JSON).build();
        // HTTP リクエストを投げますわよ！I/Oわよ！アンマウントですわよ！
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        Map<String, String> map = Map.of("response", response.getStatusCode().getReasonPhrase());
        return ResponseEntity.ok(map);
    }

    @GetMapping(value = "authors/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull ResponseEntity<Author> getAuthor(@PathVariable("id") int id) {
        String path = "/api/authors/%d".formatted(id);
        log(HttpMethod.GET, path);
        Author author = db.findAuthorById(id);
        if (author == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "author not found by id:%d".formatted(id));
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(author);
    }

    private void log(@NotNull HttpMethod method, @NotNull String path) {
        Instant now = Instant.now(clock);
        String requestId = MDC.get("request-id");
        logger.info("{} {} -> now={}, requestId={}", method, path, now, requestId);
    }

    @GetMapping(value = "authors", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull ResponseEntity<List<Author>> getAuthors() {
        log(HttpMethod.GET, "/api/authors");
        List<Author> authors = db.listAllAuthors();
        return ResponseEntity.ok(authors);
    }

    @PostMapping(value = "authors", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @NotNull ResponseEntity<Void> newAuthor(@RequestParam("name") @NotNull String name) {
        log(HttpMethod.POST, "/api/authors  new-name=%s".formatted(name));
        AuthorName authorName = AuthorName.create(name);
        if (authorName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid name");
        }
        Author author = db.newAuthor(authorName);

        return ResponseEntity.created(URI.create(authorsResource(author))).build();
    }

    private static @NotNull String authorsResource(@NotNull Author author) {
        return "/api/authors/%d".formatted(author.id());
    }
}
