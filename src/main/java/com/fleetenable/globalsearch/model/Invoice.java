package com.fleetenable.globalsearch.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Invoice entity - Invoice documents linked to billings/orders
 *
 * Searchable fields: invoice_number, purchase_order_number
 * Tracks invoice generation, delivery, and payment reconciliation
 */
@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoices_invoice_number", columnList = "invoice_number"),
    @Index(name = "idx_invoices_billing_id", columnList = "billing_id"),
    @Index(name = "idx_invoices_account_id", columnList = "account_id"),
    @Index(name = "idx_invoices_status", columnList = "status"),
    @Index(name = "idx_invoices_invoice_date", columnList = "invoice_date"),
    @Index(name = "idx_invoices_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"billing", "account", "createdBy", "updatedBy"})
@EqualsAndHashCode(of = {"id"})
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", unique = true, nullable = false, length = 50)
    @NotBlank(message = "Invoice number is required")
    @Size(max = 50, message = "Invoice number must not exceed 50 characters")
    private String invoiceNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", nullable = false, unique = true)
    private Billing billing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "purchase_order_number", length = 50)
    private String purchaseOrderNumber;

    @Column(name = "terms", length = 100)
    private String terms;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "pdf_generated_at")
    private Instant pdfGeneratedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "sent_to_email", length = 255)
    private String sentToEmail;

    @Column(name = "viewed_at")
    private Instant viewedAt;

    @Column(name = "downloaded_at")
    private Instant downloadedAt;

    /**
     * Email history stored as JSONB array
     * Example: [
     *   {"sent_at": "2024-01-15T10:00:00Z", "to": "billing@example.com", "status": "delivered"},
     *   {"sent_at": "2024-01-20T14:00:00Z", "to": "billing@example.com", "status": "opened"}
     * ]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "email_history", columnDefinition = "jsonb")
    private Object emailHistory;

    /**
     * Payment schedule stored as JSONB
     * Example: {
     *   "installments": [
     *     {"due_date": "2024-02-01", "amount": 500.00, "status": "pending"},
     *     {"due_date": "2024-03-01", "amount": 500.00, "status": "pending"}
     *   ]
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "payment_schedule", columnDefinition = "jsonb")
    private Object paymentSchedule;

    /**
     * Invoice template settings stored as JSONB
     * Example: {
     *   "template_id": "default",
     *   "logo_url": "https://...",
     *   "color_scheme": "blue",
     *   "custom_fields": {"tax_id": "12-3456789"}
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "template_settings", columnDefinition = "jsonb")
    private Object templateSettings;

    /**
     * Additional metadata stored as JSONB
     * Example: {"approval_required": true, "approved_by": "John Doe", "approved_at": "2024-01-10T..."}
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object metadata;

    @Column(columnDefinition = "text")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Convenience methods
    public boolean isDraft() {
        return status == InvoiceStatus.DRAFT;
    }

    public boolean isSent() {
        return sentAt != null && status != InvoiceStatus.DRAFT;
    }

    public boolean isViewed() {
        return viewedAt != null;
    }

    public boolean isPaid() {
        return status == InvoiceStatus.PAID;
    }

    public boolean isOverdue() {
        return status != InvoiceStatus.PAID &&
               status != InvoiceStatus.CANCELLED &&
               status != InvoiceStatus.VOID &&
               dueDate != null &&
               dueDate.isBefore(LocalDate.now());
    }

    public boolean hasPdf() {
        return pdfUrl != null && !pdfUrl.isEmpty();
    }

    public enum InvoiceStatus {
        DRAFT,
        PENDING_APPROVAL,
        APPROVED,
        SENT,
        VIEWED,
        PARTIALLY_PAID,
        PAID,
        OVERDUE,
        CANCELLED,
        VOID
    }
}
