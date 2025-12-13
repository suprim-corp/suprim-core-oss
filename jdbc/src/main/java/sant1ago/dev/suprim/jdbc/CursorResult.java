package sant1ago.dev.suprim.jdbc;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Cursor-based pagination result for efficient large dataset traversal.
 * Uses encoded cursor tokens instead of page numbers.
 *
 * <pre>{@code
 * CursorResult<User> result = executor.cursorPaginate(query, null, 20, User.class, User_.ID);
 * result.getData();          // List<User>
 * result.getNextCursor();    // "eyJpZCI6MTAwfQ=="
 * result.hasMorePages();     // true
 *
 * // Next page
 * CursorResult<User> next = executor.cursorPaginate(query, result.getNextCursor(), 20, ...);
 * }</pre>
 *
 * @param <T> entity type
 */
public final class CursorResult<T> {

    private final List<T> data;
    private final String nextCursor;
    private final String previousCursor;
    private final int perPage;

    private CursorResult(List<T> data, String nextCursor, String previousCursor, int perPage) {
        this.data = data;
        this.nextCursor = nextCursor;
        this.previousCursor = previousCursor;
        this.perPage = perPage;
    }

    /**
     * Create cursor result.
     *
     * @param data           page data
     * @param nextCursor     cursor for next page (null if no more)
     * @param previousCursor cursor for previous page (null if first)
     * @param perPage        items per page
     * @param <T>            entity type
     * @return cursor result
     */
    public static <T> CursorResult<T> of(List<T> data, String nextCursor, String previousCursor, int perPage) {
        return new CursorResult<>(data, nextCursor, previousCursor, perPage);
    }

    /**
     * Get page data.
     */
    public List<T> getData() {
        return data;
    }

    /**
     * Get cursor for next page, or null if no more pages.
     */
    public String getNextCursor() {
        return nextCursor;
    }

    /**
     * Get cursor for previous page, or null if first page.
     */
    public String getPreviousCursor() {
        return previousCursor;
    }

    /**
     * Get items per page.
     */
    public int getPerPage() {
        return perPage;
    }

    /**
     * Check if there are more pages after the current.
     */
    public boolean hasMorePages() {
        return Objects.nonNull(nextCursor);
    }

    /**
     * Check if there are pages before current.
     */
    public boolean hasPreviousPages() {
        return Objects.nonNull(previousCursor);
    }

    /**
     * Check if the result is empty.
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Encode a cursor value to base64 string.
     *
     * @param value cursor value (typically last ID)
     * @return encoded cursor string
     */
    public static String encodeCursor(Object value) {
        if (Objects.isNull(value)) return null;
        return Base64.getUrlEncoder().encodeToString(value.toString().getBytes());
    }

    /**
     * Decode a cursor string back to value.
     *
     * @param cursor encoded cursor
     * @return decoded value as string
     */
    public static String decodeCursor(String cursor) {
        if (Objects.isNull(cursor) || cursor.isEmpty()) return null;
        try {
            return new String(Base64.getUrlDecoder().decode(cursor));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
