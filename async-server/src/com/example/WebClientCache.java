package com.example;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.reactive.function.client.WebClient;

@FunctionalInterface
interface WebClientCache {
    @NotNull WebClient getClient(int hash);
}
