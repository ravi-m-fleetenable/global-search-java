package com.fleetenable.globalsearch.repository;

import com.fleetenable.globalsearch.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Invoice entity
 *
 * Searchable fields: invoice_number, purchase_order_number
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Find invoice by invoice number
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Find invoice by billing ID
     */
    @Query("SELECT i FROM Invoice i WHERE i.billing.id = :billingId AND i.deletedAt IS NULL")
    Optional<Invoice> findByBillingId(@Param("billingId") Long billingId);

    /**
     * Find invoices by account ID
     */
    @Query("SELECT i FROM Invoice i WHERE i.account.id = :accountId AND i.deletedAt IS NULL ORDER BY i.createdAt DESC")
    List<Invoice> findByAccountId(@Param("accountId") Long accountId);

    /**
     * Find invoices by status
     */
    List<Invoice> findByStatusAndDeletedAtIsNull(Invoice.InvoiceStatus status);

    /**
     * Find overdue invoices
     */
    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :today AND i.status NOT IN ('PAID', 'CANCELLED', 'VOID') AND i.deletedAt IS NULL")
    List<Invoice> findOverdue(@Param("today") LocalDate today);

    /**
     * Find invoices by due date range
     */
    @Query("SELECT i FROM Invoice i WHERE i.dueDate BETWEEN :startDate AND :endDate AND i.deletedAt IS NULL")
    List<Invoice> findByDueDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find invoices by invoice date range
     */
    @Query("SELECT i FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate AND i.deletedAt IS NULL")
    List<Invoice> findByInvoiceDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find recent invoices
     */
    @Query("SELECT i FROM Invoice i WHERE i.createdAt >= :since AND i.deletedAt IS NULL ORDER BY i.createdAt DESC")
    List<Invoice> findRecentInvoices(@Param("since") Instant since);

    /**
     * Search invoice number with fuzzy matching
     */
    @Query(value = """
        SELECT i.*,
               similarity(LOWER(i.invoice_number), LOWER(:invoiceNumber)) AS relevance
        FROM invoices i
        WHERE i.deleted_at IS NULL
          AND similarity(LOWER(i.invoice_number), LOWER(:invoiceNumber)) > :threshold
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Invoice> searchByInvoiceNumber(
        @Param("invoiceNumber") String invoiceNumber,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Search purchase order number with fuzzy matching
     */
    @Query(value = """
        SELECT i.*,
               similarity(LOWER(i.purchase_order_number), LOWER(:poNumber)) AS relevance
        FROM invoices i
        WHERE i.purchase_order_number IS NOT NULL
          AND i.deleted_at IS NULL
          AND similarity(LOWER(i.purchase_order_number), LOWER(:poNumber)) > :threshold
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Invoice> searchByPurchaseOrderNumber(
        @Param("poNumber") String poNumber,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Find draft invoices
     */
    @Query("SELECT i FROM Invoice i WHERE i.status = 'DRAFT' AND i.deletedAt IS NULL ORDER BY i.createdAt DESC")
    List<Invoice> findDrafts();

    /**
     * Find sent but unviewed invoices
     */
    @Query("SELECT i FROM Invoice i WHERE i.sentAt IS NOT NULL AND i.viewedAt IS NULL AND i.deletedAt IS NULL ORDER BY i.sentAt DESC")
    List<Invoice> findSentButUnviewed();

    /**
     * Find invoices without PDF
     */
    @Query("SELECT i FROM Invoice i WHERE i.pdfUrl IS NULL AND i.status != 'DRAFT' AND i.deletedAt IS NULL")
    List<Invoice> findWithoutPdf();

    /**
     * Autocomplete for invoice number
     */
    @Query(value = """
        SELECT i.*,
               word_similarity(:searchTerm, LOWER(i.invoice_number)) AS relevance
        FROM invoices i
        WHERE i.deleted_at IS NULL
          AND word_similarity(:searchTerm, LOWER(i.invoice_number)) > :threshold
        ORDER BY relevance DESC, i.created_at DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Invoice> autocomplete(
        @Param("searchTerm") String searchTerm,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Find invoices by purchase order number (exact match)
     */
    List<Invoice> findByPurchaseOrderNumberAndDeletedAtIsNull(String purchaseOrderNumber);
}
