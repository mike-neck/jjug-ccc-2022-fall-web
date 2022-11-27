package com.example.values;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record AuthorName(
        @NotNull String value
) {

    static final int MAX_LENGTH = 60;

    public static @Nullable AuthorName create(@Nullable String value) {
        if (value == null) {
            return null;
        }
        int length = value.codePointCount(0, value.length());
        if (MAX_LENGTH < length) {
            return null;
        }
        return new AuthorName(value);
    }
}
