package com.mts.aadati.utils.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility class for building safe, validated {@link Pageable} instances.
 * <p>
 * This class centralizes pagination defaults and bounds so that every service
 * constructs {@link Pageable} objects consistently, instead of each service
 * repeating its own validation logic.
 * <p>
 * <b>Design note:</b> {@code pageNumber} is expected to come from an external
 * client (e.g. a {@code @RequestParam} on a controller), so it is defensively
 * clamped to a non-negative value. {@code pageSize} and {@code sortBy}, in this
 * codebase, are decided internally by each service (not passed through from the
 * client) — the validation here still applies as a cheap safety net against
 * accidental misuse (e.g. a future service passing a negative size or a blank
 * sort field by mistake), not as protection against malicious client input.
 */
public final class PageableUtils {

    /**
     * The page size used when the caller passes a non-positive value,
     * or omits page size entirely via {@link #pageable(int, String)}.
     */
    private static final int DEFAULT_PAGE_SIZE = 15;

    /**
     * The maximum allowed page size. Requests for a larger page size fall back
     * to {@link #DEFAULT_PAGE_SIZE} rather than being honored, to avoid a single
     * query loading an unbounded number of rows.
     */
    private static final int MAX_PAGE_SIZE = 100;

    /** Prevents instantiation — this class only exposes static factory methods. */
    private PageableUtils() {
    }

    /**
     * Builds a {@link Pageable} using the {@link #DEFAULT_PAGE_SIZE} and
     * ascending sort order.
     *
     * @param pageNumber zero-based page index; negative values are clamped to {@code 0}
     * @param sortBy     the entity property to sort by; if {@code null} or blank,
     *                   defaults to {@code "createdAt"} (assumes the entity extends
     *                   the shared {@code Auditing} base class)
     * @return a validated {@link Pageable} sorted ascending by {@code sortBy}
     */
    public static Pageable pageable(int pageNumber, String sortBy) {
        return pageable(pageNumber, DEFAULT_PAGE_SIZE, sortBy);
    }

    /**
     * Builds a {@link Pageable} with an explicit page size and ascending sort order.
     *
     * @param pageNumber zero-based page index; negative values are clamped to {@code 0}
     * @param pageSize   desired page size; values {@code <= 0} or greater than
     *                   {@link #MAX_PAGE_SIZE} fall back to {@link #DEFAULT_PAGE_SIZE}
     * @param sortBy     the entity property to sort by; if {@code null} or blank,
     *                   defaults to {@code "createdAt"}
     * @return a validated {@link Pageable} sorted ascending by {@code sortBy}
     */
    public static Pageable pageable(int pageNumber, int pageSize, String sortBy) {
        return pageable(pageNumber, pageSize, sortBy, Sort.Direction.ASC);
    }

    /**
     * Builds a {@link Pageable} with full control over page size and sort direction.
     * This is the canonical method — the other overloads delegate to this one with
     * their own defaults filled in.
     * <p>
     * Validation applied:
     * <ul>
     *     <li>{@code pageNumber < 0} → clamped to {@code 0}</li>
     *     <li>{@code pageSize <= 0} or {@code pageSize > MAX_PAGE_SIZE} → replaced with {@link #DEFAULT_PAGE_SIZE}</li>
     *     <li>{@code sortBy} {@code null}/blank → replaced with {@code "createdAt"}</li>
     * </ul>
     *
     * @param pageNumber zero-based page index; negative values are clamped to {@code 0}
     * @param pageSize   desired page size; invalid or out-of-range values fall back to {@link #DEFAULT_PAGE_SIZE}
     * @param sortBy     the entity property to sort by; {@code null}/blank falls back to {@code "createdAt"}
     * @param direction  sort direction ({@link Sort.Direction#ASC} or {@link Sort.Direction#DESC})
     * @return a validated {@link Pageable} built from the (possibly corrected) inputs above
     */
    public static Pageable pageable(int pageNumber, int pageSize, String sortBy, Sort.Direction direction) {
        int finalPageNumber = Math.max(pageNumber, 0);
        int finalPageSize = (pageSize <= 0 || pageSize > MAX_PAGE_SIZE)
                ? DEFAULT_PAGE_SIZE
                : pageSize;
        String finalSortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;

        return PageRequest.of(finalPageNumber, finalPageSize, Sort.by(direction, finalSortBy));
    }
    /**
     * Builds a {@link Pageable} using the {@link #DEFAULT_PAGE_SIZE} and the
     * specified sort direction.
     *
     * @param pageNumber zero-based page index; negative values are clamped to {@code 0}
     * @param sortBy     the entity property to sort by; if {@code null} or blank,
     *                   defaults to {@code "createdAt"}
     * @param direction  sort direction ({@link Sort.Direction#ASC} or {@link Sort.Direction#DESC})
     * @return a validated {@link Pageable} sorted by {@code sortBy} in the specified direction
     */
    public static Pageable pageable(int pageNumber, String sortBy, Sort.Direction direction) {
        return pageable(pageNumber, DEFAULT_PAGE_SIZE, sortBy, direction);
    }

}