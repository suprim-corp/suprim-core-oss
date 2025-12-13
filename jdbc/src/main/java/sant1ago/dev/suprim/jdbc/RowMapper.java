package sant1ago.dev.suprim.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Functional interface for mapping a ResultSet row to an object.
 *
 * <pre>{@code
 * RowMapper<User> mapper = rs -> new User(
 *     rs.getLong("id"),
 *     rs.getString("email")
 * );
 *
 * List<User> users = executor.query(query, mapper);
 * }</pre>
 *
 * @param <T> the type to map to
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Map a single row of the ResultSet to an object.
     *
     * @param rs the ResultSet positioned at the current row
     * @return the mapped object
     * @throws SQLException if a database access error occurs
     */
    T map(ResultSet rs) throws SQLException;
}
