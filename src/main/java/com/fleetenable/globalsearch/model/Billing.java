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
 * Billing entity - Payment records linked to orders
 *
 * Searchable fields: billing_number, transaction_id
 * Tracks payment status, methods, and reconciliation
 */
@Entity
@Table(name = "billings", indexes = {
    @Index(name = "idx_billings_billing_number", columnList = "billing_number"),
    @Index(name = "idx_billings_order_id", columnList = "order_id"),
    @Index(name = "idx_billings_account_id", columnList = "account_id"),
    @Index(name = "idx_billings_status", columnList = "status"),
    @Index(name = "idx_billings_due_date", columnList = "due_date"),
    @Index(name = "idx_billings_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"order", "account", "createdBy", "updatedBy"})
@EqualsAndHashCode(of = {"id"})
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "billing_number", unique = true, nullable = false, length = 50)
    @NotBlank(message = "Billing number is required")
    @Size(max = 50, message = "Billing number must not exceed 50 characters")
    private String billingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_reason", length = 255)
    private String discountReason;

    @Column(name = "additional_fees", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal additionalFees = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "amount_paid", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "amount_due", precision = 15, scale = 2)
    private BigDecimal amountDue;

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private BillingStatus status = BillingStatus.PENDING;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    /**
     * Payment details stored as JSONB
     * Example: {
     *   "card_last4": "1234",
     *   "card_brand": "Visa",
     *   "ach_account_last4": "5678",
     *   "check_number": "12345"
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "payment_details", columnDefinition = "jsonb")
    private Object paymentDetails;

    /**
     * Line items stored as JSONB array
     * Example: [
     *   {"description": "Shipping", "quantity": 1, "unit_price": 150.00, "total": 150.00},
     *   {"description": "Fuel Surcharge", "quantity": 1, "unit_price": 25.00, "total": 25.00}
     * ]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "line_items", columnDefinition = "jsonb")
    private Object lineItems;

    @Column(name = "reconciled")
    @Builder.Default
    private Boolean reconciled = false;

    @Column(name = "reconciled_at")
    private Instant reconciledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciled_by")
    private User reconciledBy;

    /**
     * Additional metadata stored as JSONB
     * Example: {"payment_processor": "Stripe", "processor_fee": 4.50, "notes": "..."}
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
    public boolean isPaid() {
        return status == BillingStatus.PAID && paidDate != null;
    }

    public boolean isOverdue() {
        return status != BillingStatus.PAID &&
               status != BillingStatus.CANCELLED &&
               dueDate != null &&
               dueDate.isBefore(LocalDate.now());
    }

    public boolean isPartiallyPaid() {
        return status == BillingStatus.PARTIALLY_PAID &&
               amountPaid != null &&
               amountPaid.compareTo(BigDecimal.ZERO) > 0 &&
               amountPaid.compareTo(totalAmount) < 0;
    }

    public BigDecimal calculateBalance() {
        if (totalAmount == null) return BigDecimal.ZERO;
        if (amountPaid == null) return totalAmount;
        return totalAmount.subtract(amountPaid);
    }

    public enum BillingStatus {
        PENDING,
        SENT,
        VIEWED,
        PARTIALLY_PAID,
        PAID,
        OVERDUE,
        CANCELLED,
        REFUNDED
    }

    public enum PaymentMethod {
        CREDIT_CARD,
        DEBIT_CARD,
        ACH,
        CHECK,
        CASH,
        WIRE_TRANSFER,
        ACCOUNT_CREDIT,
        OTHER
    }
}
