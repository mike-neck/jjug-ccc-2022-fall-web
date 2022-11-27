package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import net.moznion.gimei.Gimei;
import net.moznion.gimei.name.Name;
import org.jetbrains.annotations.NotNull;

enum RequestType {
    CREATE(2) {
        @Override
        @NotNull HttpRequest request(@NotNull Ids ids, @NotNull Random random) {
            ids.created();
            Name name = Gimei.generateName(random.nextLong());
            return HttpRequest
                    .newBuilder(URI.create(DbAppClient.AUTHORS))
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            name=%s
                            """.formatted(name.kanji())))
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("accept", "application/json")
                    .build();
        }
    },
    QUERY(5) {
        @Override
        @NotNull HttpRequest request(@NotNull Ids ids, @NotNull Random random) {
            String url = "%s/%d".formatted(DbAppClient.AUTHORS, ids.random(random));
            return HttpRequest
                    .newBuilder(URI.create(url))
                    .GET()
                    .header("accept", "application/json")
                    .build();
        }
    },
    LIST(2) {
        @Override
        @NotNull HttpRequest request(@NotNull Ids ids, @NotNull Random random) {
            return HttpRequest
                    .newBuilder(URI.create(DbAppClient.AUTHORS))
                    .GET()
                    .header("accept", "application/json")
                    .build();
        }
    },
    API(1) {
        @Override
        @NotNull HttpRequest request(@NotNull Ids ids, @NotNull Random random) {
            return HttpRequest
                    .newBuilder(URI.create(DbAppClient.API))
                    .GET()
                    .header("accept", "application/json")
                    .build();
        }
    },
    ;

    final int quantity;

    RequestType(int quantity) {
        this.quantity = quantity;
    }

    abstract @NotNull HttpRequest request(@NotNull Ids ids, @NotNull Random random);

    static @NotNull RequestType random(@NotNull Random random) {
        RequestType[] array = values();
        int sum = Arrays.stream(array)
                .mapToInt(t -> t.quantity)
                .sum();
        int p = random.nextInt(sum);
        int s = 0;
        for (RequestType type : array) {
            s += type.quantity;
            if (p < s) {
                return type;
            }
        }
        return array[random.nextInt(array.length)];
    }

    public @NotNull Runnable createTask(
            int current,
            @NotNull Ids ids,
            @NotNull Random random,
            @NotNull DbAppClient appClient,
            HttpClient client,
            CountDownLatch latch
    ) {
        return () -> {
            Thread thread = Thread.currentThread();
            try {
                HttpRequest request = request(ids, random);
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                Instant now = Instant.now();
                String log = "%s[%s]%d SUCCESS %d - %s".formatted(now, thread.getName(), current, response.statusCode(), this.name());
                appClient.log(log);
            } catch (Exception e) {
                Instant now = Instant.now();
                String log = "%s[%s]%d ERROR - %s %s: %s".formatted(now, thread.getName(), current, this.name(), e.getClass().getName(), e.getMessage());
                appClient.log(log);
            } finally {
                latch.countDown();
            }
        };
    }
}
