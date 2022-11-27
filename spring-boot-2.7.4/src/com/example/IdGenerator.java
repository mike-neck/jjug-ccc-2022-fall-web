package com.example;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.function.IntSupplier;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface IdGenerator extends AutoCloseable {

    int newId();

    @Override
    default void close() {
    }

    static @NotNull IdGenerator getDefault(short id) {
        LocalDateTime base = LocalDateTime.of(2022, 1, 1, 0, 0);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        long moy = Duration.between(base, now).toSeconds();
        int minOfYear = (int) (moy & 0x3ff_ffff);
        int top = minOfYear << 5;
        int bottom = ((int)id) & 0x07;
        int[] count = {0};
        return () -> {
            int c = count[0]++;
            int m = c & 0x0fff;
            int mid = m << 3;
            return top | mid | bottom;
        };
    }

    static @NotNull IdGenerator getDefault(@NotNull IntSupplier sequence, short id) {
        LocalDate pd = LocalDate.now();
        LocalDateTime base = pd.atStartOfDay();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        long moy = Duration.between(base, now).toSeconds();
        int minOfYear = (int) (moy & 0xf_ffff);
        int top = minOfYear << 12;
        int bottom = ((int)id) & 3;
        return () -> {
            int seq = sequence.getAsInt() & 0x1ff;
            int mid = seq << 3;
            return top | mid | bottom;
        };
    }
}
