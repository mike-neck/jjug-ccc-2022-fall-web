package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.moznion.gimei.Gimei;
import net.moznion.gimei.name.Name;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public class DbAppClient implements Runnable {
    static final int ID_STARTS = 2;

    static final @NotNull String API = "http://localhost:8080/api";
    static final @NotNull String AUTHORS = "http://localhost:8080/api/authors";

    static final int REQUEST = 120;
    static final @NotNull Duration INTERVAL = Duration.ofMillis(1_000L);


    public static void main(String[] args) throws InterruptedException {
        ThreadFactory threadFactory = Thread.ofVirtual().name("v-thread", 1L)
                .allowSetThreadLocals(true)
                .factory();
        CountDownLatch finish = new CountDownLatch(1);
        DbAppClient dbAppClient = new DbAppClient(finish);
        long interval = TimeUnit.MILLISECONDS.convert(INTERVAL);
        try (
                var scheduler = Executors.newSingleThreadScheduledExecutor();
                var logExecutor = Executors.newSingleThreadExecutor()
        ) {
            logExecutor.submit(dbAppClient);
            TaskFactory taskFactory = new TaskFactory(REQUEST, threadFactory, dbAppClient);
            scheduler.scheduleAtFixedRate(
                    taskFactory,
                    200L,
                    interval,
                    TimeUnit.MILLISECONDS
            );
            dbAppClient.log("start client");
            taskFactory.latch.await();
            dbAppClient.log("finish client");
            finish.countDown();
        }
    }

    final @NotNull CountDownLatch latch;
    final @NotNull BlockingQueue<String> queue;

    DbAppClient(
            @NotNull CountDownLatch latch) {
        this.latch = latch;
        this.queue = new LinkedBlockingQueue<>();
    }

    void log(@NotNull String text) {
        queue.add(text);
    }

    @Override
    public void run() {
        while (0 < latch.getCount()) {
            try {
                long convert = TimeUnit.MILLISECONDS.convert(DbAppClient.INTERVAL);
                String text = queue.poll(convert, TimeUnit.MILLISECONDS);
                if (text != null) {
                    System.out.println(text);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
