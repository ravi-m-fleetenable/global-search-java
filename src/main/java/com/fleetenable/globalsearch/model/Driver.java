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
import java.time.LocalDate;
import java.util.UUID;

/**
 * Driver entity - Driver profiles with license and emergency contact
 *
 * Searchable fields: first_name, last_name, email, license_number, phone_number
 * Uses GiST index for similarity-based search
 */
@Entity
@Table(name = "drivers", indexes = {
    @Index(name = "idx_drivers_license_number", columnList = "license_number"),
    @Index(name = "idx_drivers_email", columnList = "email"),
    @Index(name = "idx_drivers_status", columnList = "status"),
    @Index(name = "idx_drivers_user_id", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "createdBy", "updatedBy"})
@EqualsAndHashCode(of = {"id"})
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Column(length = 255)
    @Email(message = "Email must be valid")
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "license_number", unique = true, nullable = false, length = 50)
    @NotBlank(message = "License number is required")
    @Size(max = 50, message = "License number must not exceed 50 characters")
    private String licenseNumber;

    @Column(name = "license_state", length = 2)
    @Size(min = 2, max = 2, message = "License state must be 2 characters")
    private String licenseState;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "license_class", length = 20)
    private String licenseClass;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    /**
     * Address stored as JSONB
     * Example: {
     *   "street": "456 Oak Ave",
     *   "city": "Los Angeles",
     *   "state": "CA",
     *   "zip": "90001",
     *   "country": "USA"
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object address;

    /**
     * Emergency contact stored as JSONB
     * Example: {
     *   "name": "Jane Doe",
     *   "relationship": "Spouse",
     *   "phone": "+1-555-1234",
     *   "email": "jane@example.com"
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "emergency_contact", columnDefinition = "jsonb")
    private Object emergencyContact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private DriverStatus status = DriverStatus.ACTIVE;

    /**
     * Certifications stored as JSONB array
     * Example: [
     *   {"type": "Hazmat", "number": "H123456", "expiry": "2025-12-31"},
     *   {"type": "Tanker", "number": "T789012", "expiry": "2026-06-30"}
     * ]
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object certifications;

    /**
     * Medical certification stored as JSONB
     * Example: {"examiner": "Dr. Smith", "exam_date": "2024-01-15", "expiry": "2025-01-15", "status": "Qualified"}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "medical_cert", columnDefinition = "jsonb")
    private Object medicalCert;

    @Column(name = "background_check_date")
    private LocalDate backgroundCheckDate;

    @Column(name = "drug_test_date")
    private LocalDate drugTestDate;

    /**
     * Additional metadata stored as JSONB
     * Example: {"preferred_routes": ["I-95", "I-10"], "languages": ["en", "es"], "rating": 4.8}
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
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isActive() {
        return status == DriverStatus.ACTIVE && deletedAt == null;
    }

    public boolean isLicenseExpired() {
        return licenseExpiry != null && licenseExpiry.isBefore(LocalDate.now());
    }

    public boolean isLicenseExpiringSoon(int daysThreshold) {
        if (licenseExpiry == null) return false;
        return licenseExpiry.isBefore(LocalDate.now().plusDays(daysThreshold));
    }

    public enum DriverStatus {
        ACTIVE,
        INACTIVE,
        ON_LEAVE,
        SUSPENDED,
        TERMINATED
    }
}
