package com.example.db;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorRepository extends
        CrudRepository<Author, Integer>,
        PersistenceAuthorRepository,
        QueryAuthorRepository {

    @NotNull Iterable<Author> findAuthorByName(@NotNull String name);

    @NotNull Iterable<Author> findAuthorByNameLike(@NotNull String nameLike);
}

interface PersistenceAuthorRepository {
    <A extends Author> @NotNull A createNew(@NotNull A author);
}

@Component
class PersistenceAuthorRepositoryImpl implements PersistenceAuthorRepository {

    private final @NotNull NamedParameterJdbcOperations namedParameterJdbcOperations;

    PersistenceAuthorRepositoryImpl(@NotNull NamedParameterJdbcOperations namedParameterJdbcOperations) {
        this.namedParameterJdbcOperations = namedParameterJdbcOperations;
    }

    @Override
    public <A extends Author> @NotNull A createNew(@NotNull A author) {
        //language=SQL
        Integer count = namedParameterJdbcOperations.execute("""
                        insert into authors(id, name)
                        value (:id, :name)
                        """,
                Map.of("id", author.id(), "name", author.name()),
                PreparedStatement::executeUpdate);
        if (count == null || count != 1) {
            throw new JdbcUpdateAffectedIncorrectNumberOfRowsException("Author.create", 1, count == null? -1: count);
        }
        return author;
    }
}

interface QueryAuthorRepository {

    @Nullable Author findAuthorById(int id);

    @NotNull List<Author> listAll();
}

@Component
class QueryAuthorRepositoryImpl implements QueryAuthorRepository {



    private final @NotNull NamedParameterJdbcOperations namedParameterJdbcOperations;

    QueryAuthorRepositoryImpl(@NotNull NamedParameterJdbcOperations namedParameterJdbcOperations) {
        this.namedParameterJdbcOperations = namedParameterJdbcOperations;
    }

    @Override
    public @Nullable Author findAuthorById(int id) {
        Author author = namedParameterJdbcOperations.query("""
                        select
                            a.id as id,
                            a.name as name
                        from authors as a
                        where
                            a.id = :id
                        """,
                Map.of("id", (Object) id),
                rs -> {
                    if (rs.next()) {
                        int authorId = rs.getInt("id");
                        String authorName = rs.getString("name");
                        return new Author(authorId, authorName);
                    } else {
                        return null;
                    }
                }
        );
        return author;
    }

    @Override
    public @NotNull List<Author> listAll() {
        RowMapper<Author> mapper = (rs, rowNum) -> {
            if (rs.next()) {
                int authorId = rs.getInt("id");
                String authorName = rs.getString("name");
                return new Author(authorId, authorName);
            } else {
                return null;
            }
        };
        return namedParameterJdbcOperations.query("""
                select
                  a.id as id,
                  a.name as name
                from authors as a ;
                """, mapper);
    }
}
