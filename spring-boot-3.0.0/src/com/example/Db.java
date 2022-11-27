package com.example;

import com.example.db.Author;
import com.example.db.AuthorRepository;
import com.example.values.AuthorName;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Db {

    static final Logger logger = LoggerFactory.getLogger(Db.class);

    final @NotNull IdGeneratorFactory idGeneratorFactory;
    final @NotNull JdbcTemplate jdbcTemplate;
    final @NotNull AuthorRepository authorRepository;

    public Db(
            @NotNull IdGeneratorFactory idGeneratorFactory,
            @NotNull JdbcTemplate jdbcTemplate,
            @NotNull AuthorRepository authorRepository
    ) {
        this.idGeneratorFactory = idGeneratorFactory;
        this.jdbcTemplate = jdbcTemplate;
        this.authorRepository = authorRepository;
    }

    @Nullable Author findAuthorById(int id) {
        log("find-author-by-id", "in", id);
        try {
            return authorRepository.findAuthorById(id);
        } finally {
            log("find-author-by-id", "out", id);
        }
    }

    @Transactional
    @NotNull Author newAuthor(@NotNull AuthorName name) {
        log("new-author", "in", name);
        try (IdGenerator idGenerator = idGeneratorFactory.newIdGenerator()) {
            int newId = idGenerator.newId();
            Author author = Author.createNew(newId, name);
            return authorRepository.createNew(author);
        } catch (RuntimeException e) {
            logError("new-author", name, e);
            throw e;
        } finally {
            log("new-author", "out", name);
        }
    }

    private static void log(@NotNull String method, @NotNull String phase, @NotNull Object item) {
        String requestId = MDC.get("request-id");
        logger.info("{}[{}]{} author = {}", method, phase, requestId, item);
    }

    private static void logError(@NotNull String method, @NotNull Object item, @NotNull Exception e) {
        String requestId = MDC.get("request-id");
        logger.info("{}[exception]{} new-author = {}", method, requestId, item, e);
    }

    @NotNull List<Author> listAllAuthors() {
        log("listAllAuthors", "in", "all");
        try {
            return authorRepository.listAll();
        } catch (Exception e) {
            logError("listAllAuthors", "all", e);
            throw e;
        } finally {
            log("listAllAuthors", "out", "all");
        }
    }
}
