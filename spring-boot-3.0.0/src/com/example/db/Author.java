package com.example.db;

import com.example.values.AuthorName;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("authors")
public record Author(
        @NotNull @Id Integer id,
        @NotNull String name
) {

    public static Author createNew(int id, @NotNull AuthorName name) {
        return new Author(id, name.value());
    }
}
