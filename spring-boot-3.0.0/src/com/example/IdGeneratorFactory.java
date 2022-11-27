package com.example;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface IdGeneratorFactory {

    @NotNull IdGenerator newIdGenerator();
}
