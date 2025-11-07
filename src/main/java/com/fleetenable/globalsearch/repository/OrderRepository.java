package com.fleetenable.globalsearch.repository;

import com.fleetenable.globalsearch.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order entity (PARTITIONED TABLE)
 *
 * Uses GIN index with partial index on recent data (12 months)
 * Partition pruning optimization by filtering on created_at
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Order.OrderId> {

    /**
     * Find order by order number
     */
    @Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumber(@Param("orderNumber") String orderNumber);

    /**
     * Find orders by status
     */
    List<Order> findByStatusAndDeletedAtIsNull(Order.OrderStatus status);

    /**
     * Find orders by account
     */
    @Query("SELECT o FROM Order o WHERE o.account.id = :accountId AND o.deletedAt IS NULL ORDER BY o.createdAt DESC")
    List<Order> findByAccountId(@Param("accountId") Long accountId);

    /**
     * Find recent orders (partition pruning optimization)
     * Only scans recent partitions for better performance
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt >= :since AND o.deletedAt IS NULL ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(@Param("since") Instant since);

    /**
     * Multi-attribute fuzzy search with partition pruning
     * Searches across: order_number, hawb_numbers, notes
     *
     * Uses partial GIN index: idx_orders_search_recent (last 12 months)
     * Partition pruning: Only scans partitions within time range
     */
    @Query(value = """
        SELECT o.*,
               similarity(
                   LOWER(o.order_number || ' ' ||
                         COALESCE(o.notes, '')),
                   LOWER(:searchTerm)
               ) AS relevance
        FROM orders o
        WHERE o.created_at >= :startDate
          AND o.deleted_at IS NULL
          AND similarity(
              LOWER(o.order_number || ' ' ||
                    COALESCE(o.notes, '')),
              LOWER(:searchTerm)
          ) > :threshold
        ORDER BY relevance DESC, o.created_at DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Order> searchFuzzyRecent(
        @Param("searchTerm") String searchTerm,
        @Param("startDate") Instant startDate,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Search all partitions (slower, but comprehensive)
     */
    @Query(value = """
        SELECT o.*,
               similarity(
                   LOWER(o.order_number || ' ' ||
                         COALESCE(o.notes, '')),
                   LOWER(:searchTerm)
               ) AS relevance
        FROM orders o
        WHERE o.deleted_at IS NULL
          AND similarity(
              LOWER(o.order_number || ' ' ||
                    COALESCE(o.notes, '')),
              LOWER(:searchTerm)
          ) > :threshold
        ORDER BY relevance DESC, o.created_at DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Order> searchFuzzyAll(
        @Param("searchTerm") String searchTerm,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Search HAWB numbers in JSONB array
     * Example: Find orders containing HAWB "HAWB-12345"
     */
    @Query(value = """
        SELECT o.*
        FROM orders o
        WHERE o.hawb_numbers @> :hawbJson::jsonb
          AND o.deleted_at IS NULL
        ORDER BY o.created_at DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Order> searchByHawbNumber(
        @Param("hawbJson") String hawbJson,
        @Param("maxResults") int maxResults
    );

    /**
     * Autocomplete for order number
     */
    @Query(value = """
        SELECT o.*,
               word_similarity(:searchTerm, LOWER(o.order_number)) AS relevance
        FROM orders o
        WHERE o.created_at >= :startDate
          AND o.deleted_at IS NULL
          AND word_similarity(:searchTerm, LOWER(o.order_number)) > :threshold
        ORDER BY relevance DESC, o.created_at DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Order> autocomplete(
        @Param("searchTerm") String searchTerm,
        @Param("startDate") Instant startDate,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Find orders by date range (partition-aware)
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.deletedAt IS NULL ORDER BY o.createdAt DESC")
    List<Order> findByDateRange(
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Find orders by delivery date range
     */
    @Query("SELECT o FROM Order o WHERE o.estimatedDeliveryDate BETWEEN :startDate AND :endDate AND o.deletedAt IS NULL")
    List<Order> findByDeliveryDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find delayed orders
     */
    @Query("SELECT o FROM Order o WHERE o.estimatedDeliveryDate < :today AND o.status NOT IN ('DELIVERED', 'CANCELLED') AND o.deletedAt IS NULL")
    List<Order> findDelayedOrders(@Param("today") LocalDate today);

    /**
     * Find orders by driver
     */
    @Query("SELECT o FROM Order o WHERE o.driver.id = :driverId AND o.createdAt >= :startDate AND o.deletedAt IS NULL ORDER BY o.createdAt DESC")
    List<Order> findByDriverId(
        @Param("driverId") Long driverId,
        @Param("startDate") Instant startDate
    );

    /**
     * Find orders by fleet vehicle
     */
    @Query("SELECT o FROM Order o WHERE o.fleet.id = :fleetId AND o.createdAt >= :startDate AND o.deletedAt IS NULL ORDER BY o.createdAt DESC")
    List<Order> findByFleetId(
        @Param("fleetId") Long fleetId,
        @Param("startDate") Instant startDate
    );

    /**
     * Paginated search with partition pruning
     */
    @Query(value = """
        SELECT o.*,
               similarity(
                   LOWER(o.order_number || ' ' ||
                         COALESCE(o.notes, '')),
                   LOWER(:searchTerm)
               ) AS relevance
        FROM orders o
        WHERE o.created_at >= :startDate
          AND o.deleted_at IS NULL
          AND similarity(
              LOWER(o.order_number || ' ' ||
                    COALESCE(o.notes, '')),
              LOWER(:searchTerm)
          ) > :threshold
        ORDER BY relevance DESC, o.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM orders o
        WHERE o.created_at >= :startDate
          AND o.deleted_at IS NULL
          AND similarity(
              LOWER(o.order_number || ' ' ||
                    COALESCE(o.notes, '')),
              LOWER(:searchTerm)
          ) > :threshold
        """,
        nativeQuery = true)
    Page<Order> searchFuzzyPaginated(
        @Param("searchTerm") String searchTerm,
        @Param("startDate") Instant startDate,
        @Param("threshold") double threshold,
        Pageable pageable
    );
}
