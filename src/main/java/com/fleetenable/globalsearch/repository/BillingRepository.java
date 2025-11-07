package com.fleetenable.globalsearch.repository;

import com.fleetenable.globalsearch.model.Billing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Billing entity
 *
 * Searchable fields: billing_number, transaction_id
 */
@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {

    /**
     * Find billing by billing number
     */
    Optional<Billing> findByBillingNumber(String billingNumber);

    /**
     * Find billings by order ID
     */
    @Query("SELECT b FROM Billing b WHERE b.order.id = :orderId AND b.deletedAt IS NULL")
    List<Billing> findByOrderId(@Param("orderId") Long orderId);

    /**
     * Find billings by account ID
     */
    @Query("SELECT b FROM Billing b WHERE b.account.id = :accountId AND b.deletedAt IS NULL ORDER BY b.createdAt DESC")
    List<Billing> findByAccountId(@Param("accountId") Long accountId);

    /**
     * Find billings by status
     */
    List<Billing> findByStatusAndDeletedAtIsNull(Billing.BillingStatus status);

    /**
     * Find overdue billings
     */
    @Query("SELECT b FROM Billing b WHERE b.dueDate < :today AND b.status NOT IN ('PAID', 'CANCELLED') AND b.deletedAt IS NULL")
    List<Billing> findOverdue(@Param("today") LocalDate today);

    /**
     * Find billings by due date range
     */
    @Query("SELECT b FROM Billing b WHERE b.dueDate BETWEEN :startDate AND :endDate AND b.deletedAt IS NULL")
    List<Billing> findByDueDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find recent billings
     */
    @Query("SELECT b FROM Billing b WHERE b.createdAt >= :since AND b.deletedAt IS NULL ORDER BY b.createdAt DESC")
    List<Billing> findRecentBillings(@Param("since") Instant since);

    /**
     * Search billing number with fuzzy matching
     */
    @Query(value = """
        SELECT b.*,
               similarity(LOWER(b.billing_number), LOWER(:billingNumber)) AS relevance
        FROM billings b
        WHERE b.deleted_at IS NULL
          AND similarity(LOWER(b.billing_number), LOWER(:billingNumber)) > :threshold
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Billing> searchByBillingNumber(
        @Param("billingNumber") String billingNumber,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Search transaction ID with fuzzy matching
     */
    @Query(value = """
        SELECT b.*,
               similarity(LOWER(b.transaction_id), LOWER(:transactionId)) AS relevance
        FROM billings b
        WHERE b.transaction_id IS NOT NULL
          AND b.deleted_at IS NULL
          AND similarity(LOWER(b.transaction_id), LOWER(:transactionId)) > :threshold
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Billing> searchByTransactionId(
        @Param("transactionId") String transactionId,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Find unreconciled billings
     */
    @Query("SELECT b FROM Billing b WHERE b.reconciled = false AND b.status = 'PAID' AND b.deletedAt IS NULL")
    List<Billing> findUnreconciled();

    /**
     * Calculate total outstanding amount for an account
     */
    @Query("SELECT COALESCE(SUM(b.amountDue), 0) FROM Billing b WHERE b.account.id = :accountId AND b.status NOT IN ('PAID', 'CANCELLED') AND b.deletedAt IS NULL")
    BigDecimal calculateOutstandingAmountByAccount(@Param("accountId") Long accountId);

    /**
     * Find partially paid billings
     */
    @Query("SELECT b FROM Billing b WHERE b.status = 'PARTIALLY_PAID' AND b.deletedAt IS NULL ORDER BY b.dueDate ASC")
    List<Billing> findPartiallyPaid();

    /**
     * Autocomplete for billing number
     */
    @Query(value = """
        SELECT b.*,
               word_similarity(:searchTerm, LOWER(b.billing_number)) AS relevance
        FROM billings b
        WHERE b.deleted_at IS NULL
          AND word_similarity(:searchTerm, LOWER(b.billing_number)) > :threshold
        ORDER BY relevance DESC, b.created_at DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Billing> autocomplete(
        @Param("searchTerm") String searchTerm,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );
}
