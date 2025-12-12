package sant1ago.dev.suprim.jdbc;

import java.util.List;

/**
 * Pagination result container with metadata.
 *
 * <pre>{@code
 * PaginatedResult<User> users = executor.paginate(query, page, pageSize, User.class);
 * users.getData();        // List<User>
 * users.getCurrentPage(); // 1
 * users.getLastPage();    // 5
 * users.getTotal();       // 100
 * users.hasMorePages();   // true
 * }</pre>
 *
 * @param <T> entity type
 */
public final class PaginatedResult<T> {

    private final List<T> data;
    private final int currentPage;
    private final int perPage;
    private final long total;
    private final int lastPage;

    private PaginatedResult(List<T> data, int currentPage, int perPage, long total) {
        this.data = data;
        this.currentPage = currentPage;
        this.perPage = perPage;
        this.total = total;
        this.lastPage = perPage > 0 ? (int) Math.ceil((double) total / perPage) : 1;
    }

    /**
     * Create a paginated result.
     *
     * @param data        page data
     * @param currentPage current page number (1-based)
     * @param perPage     items per page
     * @param total       total number of items
     * @param <T>         entity type
     * @return paginated result
     */
    public static <T> PaginatedResult<T> of(List<T> data, int currentPage, int perPage, long total) {
        return new PaginatedResult<>(data, currentPage, perPage, total);
    }

    /**
     * Get page data.
     */
    public List<T> getData() {
        return data;
    }

    /**
     * Get current page number (1-based).
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Get items per page.
     */
    public int getPerPage() {
        return perPage;
    }

    /**
     * Get total number of items across all pages.
     */
    public long getTotal() {
        return total;
    }

    /**
     * Get last page number.
     */
    public int getLastPage() {
        return lastPage;
    }

    /**
     * Check if there are more pages after current.
     */
    public boolean hasMorePages() {
        return currentPage < lastPage;
    }

    /**
     * Check if there are pages before current.
     */
    public boolean hasPreviousPages() {
        return currentPage > 1;
    }

    /**
     * Check if current page is first.
     */
    public boolean isFirstPage() {
        return currentPage == 1;
    }

    /**
     * Check if current page is last.
     */
    public boolean isLastPage() {
        return currentPage >= lastPage;
    }

    /**
     * Check if result is empty.
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Get starting item number for current page (1-based).
     */
    public long getFrom() {
        if (total == 0) return 0;
        return (long) (currentPage - 1) * perPage + 1;
    }

    /**
     * Get ending item number for current page.
     */
    public long getTo() {
        if (total == 0) return 0;
        return Math.min((long) currentPage * perPage, total);
    }
}
