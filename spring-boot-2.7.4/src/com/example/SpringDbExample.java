package com.example;

import java.io.IOException;
import java.text.ParseException;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import jdk.jfr.Configuration;
import jdk.jfr.consumer.RecordingStream;
import org.apache.coyote.ProtocolHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(scanBasePackages = {
        "com.example",
        "com.example.db"
})
@EnableJdbcRepositories
@ImportAutoConfiguration(classes = {
        JdbcRepositoriesAutoConfiguration.class
})
public class SpringDbExample {

    static final Logger logger = LoggerFactory.getLogger(SpringDbExample.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringDbExample.class, args);
    }

    @Bean
    NamedParameterJdbcOperations namedParameterJdbcOperations(@NotNull DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    PlatformTransactionManager transactionManager(@NotNull DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    IdGeneratorFactory idGeneratorFactory() {
        AtomicInteger atomicInteger = new AtomicInteger(3);
        return () -> atomicInteger::getAndIncrement;
    }

    @Bean
    TomcatProtocolHandlerCustomizer<ProtocolHandler> tomcatProtocolHandlerCustomizer() {
        ThreadFactory delegate = Thread.ofVirtual()
                .name("app-vt-", 1L)
                .allowSetThreadLocals(true)
                .inheritInheritableThreadLocals(true)
                .factory();
        ThreadFactory factory = task -> {
            UUID uuid = UUID.randomUUID();
            Runnable wrapped = () -> {
                MDC.put("request-id", uuid.toString());
                try {
                    task.run();
                } finally {
                    MDC.remove("request-id");
                }
            };
            return delegate.newThread(wrapped);
        };
        return protocolHandler ->
                protocolHandler.setExecutor(
                        Executors.newThreadPerTaskExecutor(factory));
    }

    /**
     * @return {@linkplain TaskScheduler}のインスタンス
     * @deprecated {@code VirtualThread}のプールとしての利用は非推奨
     */
    @Deprecated
    @Bean TaskScheduler taskScheduler() {
        ThreadFactory factory = Thread.ofVirtual()
                .name("v-scheduler ", 1L)
                .allowSetThreadLocals(true)
                .inheritInheritableThreadLocals(true)
                .factory();
        ScheduledExecutorService threadPool =
                Executors.newScheduledThreadPool(2, factory);
        return new ConcurrentTaskScheduler(threadPool);
    }

    @Bean Sync sync(@Value("${use.sync:true}") @NotNull String useSync) {
        boolean value = Boolean.parseBoolean(useSync);
        return new Sync(value);
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    ReentrantLock lock() {
        return new ReentrantLock();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    JavaFlightRecorder javaFlightRecorder() throws IOException, ParseException {
        return new JavaFlightRecorder(new RecordingStream(Configuration.getConfiguration("default")));
    }

    @Bean
    CommandLineRunner configureJfr(
            @NotNull Sync sync,
            @NotNull JavaFlightRecorder javaFlightRecorder
    ) {
        return (args) -> {
            logger.info("web server use monitor = {}", sync.useMonitor());
            javaFlightRecorder.start();
        };
    }

    @Bean
    DisposableBean finishJfr(@NotNull JavaFlightRecorder javaFlightRecorder) {
        return javaFlightRecorder::finish;
    }
}
