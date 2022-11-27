package com.example;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

record Ids(
        List<Integer> ids
) {

    Ids(int idStarts) {
        this(new CopyOnWriteArrayList<>(arrayOf(idStarts)));
    }

    static Integer @NotNull [] arrayOf(int idStarts) {
        Integer[] ids = new Integer[idStarts];
        for (int i = 0; i < idStarts; i++) {
            ids[i] = i + 1;
        }
        return ids;
    }

    void created() {
        ids.add(ids.size());
    }

    @NotNull @Unmodifiable List<Integer> all() {
        return List.copyOf(ids);
    }

    @NotNull Integer random(@NotNull Random random) {
        int index = random.nextInt(ids.size());
        return ids.get(index);
    }
}
