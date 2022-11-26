package com.example;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

record Message(
        @NotNull String start,
        @NotNull String id,
        @NotNull String end
) {

    @NotNull String text() {
        return "[id=%s,start=%s,end=%s]".formatted(id, start, end);
    }

    Message(@NotNull Instant start, @NotNull String id, @NotNull Instant end) {
        this(start.toString(), id, end.toString());
    }

    static @Nullable Message parse(@NotNull String text) {
        List<String> list = text.lines().toList();
        if (list.size() != 3) {
            return null;
        }
        try {
            Instant start = Instant.parse(list.get(0));
            String id = list.get(1);
            Instant end = Instant.parse(list.get(2));
            return new Message(start, id, end);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
