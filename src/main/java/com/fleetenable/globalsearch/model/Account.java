package com.fleetenable.globalsearch.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Account entity - Customer/Organization data
 *
 * Searchable fields: account_number, account_name, company_name, contact_person, email
 * Uses GIN index for multi-attribute fuzzy search
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_accounts_account_number", columnList = "account_number"),
    @Index(name = "idx_accounts_email", columnList = "email"),
    @Index(name = "idx_accounts_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"createdBy", "updatedBy"})
@EqualsAndHashCode(of = {"id"})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false, length = 50)
    @NotBlank(message = "Account number is required")
    @Size(max = 50, message = "Account number must not exceed 50 characters")
    private String accountNumber;

    @Column(name = "account_name", nullable = false, length = 255)
    @NotBlank(message = "Account name is required")
    @Size(max = 255, message = "Account name must not exceed 255 characters")
    private String accountName;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 50)
    @Builder.Default
    private AccountType accountType = AccountType.STANDARD;

    @Column(name = "contact_person", length = 255)
    private String contactPerson;

    @Column(length = 255)
    @Email(message = "Email must be valid")
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Address stored as JSONB
     * Example: {
     *   "street": "123 Main St",
     *   "city": "New York",
     *   "state": "NY",
     *   "zip": "10001",
     *   "country": "USA"
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object address;

    /**
     * Billing address stored as JSONB (can differ from main address)
     */
    @Type(JsonBinaryType.class)
    @Column(name = "billing_address", columnDefinition = "jsonb")
    private Object billingAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "credit_limit", precision = 15, scale = 2)
    private java.math.BigDecimal creditLimit;

    @Column(name = "outstanding_balance", precision = 15, scale = 2)
    @Builder.Default
    private java.math.BigDecimal outstandingBalance = java.math.BigDecimal.ZERO;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    /**
     * Tax information stored as JSONB
     * Example: {"tax_id": "12-3456789", "tax_exempt": false, "tax_rate": 0.08}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "tax_info", columnDefinition = "jsonb")
    private Object taxInfo;

    /**
     * Additional metadata stored as JSONB
     * Example: {"industry": "Logistics", "preferred_carrier": "FedEx", "custom_fields": {...}}
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
    public boolean isActive() {
        return status == AccountStatus.ACTIVE && deletedAt == null;
    }

    public boolean isCreditLimitExceeded() {
        if (creditLimit == null) return false;
        return outstandingBalance != null && outstandingBalance.compareTo(creditLimit) > 0;
    }

    public enum AccountType {
        STANDARD,
        PREMIUM,
        ENTERPRISE,
        TRIAL
    }

    public enum AccountStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        CLOSED
    }
}
