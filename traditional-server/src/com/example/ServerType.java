package com.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * <p>
 * サーバーのスレッドのタイプを表す
 * 引数が解析できない場合はすべてのケースで引数なしにフォールバックされる
 *
 * <p>
 * 引数1: タイプ({@code platform} or {@code virtual} のいずれか)
 * 引数2: スレッドの数({@code platform}の場合のみ有効)
 */
@SuppressWarnings("RedundantStringFormatCall")
sealed interface ServerType permits ServerType.VirtualThread, ServerType.PlatformThread {

    final class VirtualThread implements ServerType {
        @Override
        public @NotNull ExecutorService executor(@NotNull Runnable abort) {
            return virtualThread();
        }

        @Override
        public void showCondition() {
            System.out.println("====");
            System.out.println(this);
            System.out.println("====");
        }

        @Override
        public String toString() {
            return "VirtualThread";
        }
    }

    record PlatformThread(
            @Range(from = 1, to = Integer.MAX_VALUE) int threadSize
    ) implements ServerType {

        PlatformThread() {
            this(Integer.MAX_VALUE);
        }

        @Override
        public @NotNull ExecutorService executor(@NotNull Runnable abort) {
            return platformThread(threadSize, e -> {
                e.printStackTrace();
                System.out.println("========");
                System.out.println("error : " + e);
                System.out.println("abort server");
                abort.run();
            });
        }

        @Override
        public void showCondition() {
            System.out.println("====");
            System.out.println(this);
            System.out.println("Threads = " + threadSize);
            System.out.println("====");
        }
    }

    @NotNull ExecutorService executor(@NotNull Runnable abort);

    void showCondition();

    static @NotNull ServerType parse(@NotNull String @NotNull ... args) {
        if (args.length == 0) {
            System.out.println("no arguments -> falling back to type PLATFORM(Int.MAX)");
            return new PlatformThread();
        }
        String type = args[0];
        if ("virtual".equals(type)) {
            System.out.println("virtual detected -> using VIRTUAL");
            return new VirtualThread();
        } else if (!"platform".equals(type)) {
            System.out.println("unknown type(%s) -> falling back to type PLATFORM(Int.MAX)".formatted(type));
            return new PlatformThread();
        }
        if (args.length == 1) {
            System.out.println("unknown size for platform -> falling back to type PLATFORM(Int.MAX)");
            return new PlatformThread();
        }
        try {
            long size = Long.parseLong(args[1]);
            if (size < 1 || Integer.MAX_VALUE < size) {
                System.out.println("unknown size for platform(%d) -> falling back to type PLATFORM(Int.MAX)".formatted(size));
                return new PlatformThread();
            }
            System.out.println("sized platform -> using PLATFORM(%d)".formatted(size));
            int threadSize = (int) size;
            return new PlatformThread(threadSize);
        } catch (NumberFormatException e) {
            System.out.println("unknown size for platform(%s) -> falling back to type PLATFORM(Int.MAX)".formatted(args[1]));
            return new PlatformThread();
        }
    }

    private static @NotNull ExecutorService platformThread(int size, @NotNull Consumer<Throwable> onError) {

        var delegate = Thread.ofPlatform()
                .name("normal-server-", 1L)
                .allowSetThreadLocals(true)
                .inheritInheritableThreadLocals(true)
                .uncaughtExceptionHandler(EXCEPTION_HANDLER)
                .factory();
        ThreadFactory factory = (runnable) -> {
            Thread thread = delegate.newThread(runnable);
            return new DelegateThread(thread);
        };
        ExecutorService executorService = Executors.newFixedThreadPool(
                size, factory
        );
        return new ServerExecutor(executorService, onError);
    }

    private static @NotNull ExecutorService virtualThread() {
        var factory = Thread.ofVirtual()
                .name("v-thread-server-", 1L)
                .allowSetThreadLocals(true)
                .inheritInheritableThreadLocals(true)
                .factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = (thread, exception) -> {
        Thread th = Thread.currentThread();
        System.out.println("unhandled exception[%s]: thread=%s, exception=%s".formatted(th.getName(), thread.getName(), exception.getClass()));
        exception.printStackTrace(System.out);
        if (exception instanceof Error error) {
            throw error;
        }
    };
}
