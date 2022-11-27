package com.example;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;

class TaskFactory implements Runnable, AutoCloseable {
    private final int max;
    final @NotNull AtomicInteger integer;
    final @NotNull ThreadFactory threadFactory;
    final @NotNull CountDownLatch latch;
    final @NotNull Ids ids;
    private final @NotNull DbAppClient appClient;
    private final @NotNull ExecutorService executor;
    private final HttpClient client;

    TaskFactory(
            int times,
            @NotNull ThreadFactory threadFactory,
            @NotNull DbAppClient appClient
    ) {
        max = times;
        this.integer = new AtomicInteger(0);
        this.threadFactory = threadFactory;
        this.latch = new CountDownLatch(times);
        this.ids = new Ids(DbAppClient.ID_STARTS);
        this.appClient = appClient;
        this.executor = Executors.newThreadPerTaskExecutor(threadFactory);
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(executor)
                .build();
    }

    @Override
    public void run() {
        int current;
        if ((current = integer.getAndIncrement()) < max) {
            Random random = ThreadLocalRandom.current();
            RequestType requestType = RequestType.random(random);
            Thread thread = threadFactory.newThread(requestType.createTask(
                    current,
                    ids,
                    random,
                    appClient,
                    client,
                    latch
            ));
            thread.start();
        }
    }

    @Override
    public void close() {
        executor.close();
    }
}
