package com.fleetenable.globalsearch.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * POD (Proof of Delivery) entity - Delivery confirmation with signatures, photos, and location
 *
 * Linked to orders with delivery verification data
 */
@Entity
@Table(name = "pods", indexes = {
    @Index(name = "idx_pods_order_id", columnList = "order_id"),
    @Index(name = "idx_pods_signed_by", columnList = "signed_by"),
    @Index(name = "idx_pods_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"order", "capturedBy"})
@EqualsAndHashCode(of = {"id"})
public class POD {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "signed_by", length = 255)
    private String signedBy;

    @Column(name = "signature_url", length = 500)
    private String signatureUrl;

    /**
     * Photo URLs stored as JSONB array
     * Example: [
     *   "https://s3.amazonaws.com/pod-photos/12345-front.jpg",
     *   "https://s3.amazonaws.com/pod-photos/12345-signature.jpg",
     *   "https://s3.amazonaws.com/pod-photos/12345-package.jpg"
     * ]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "photo_urls", columnDefinition = "jsonb")
    private Object photoUrls;

    @Column(name = "delivery_notes", columnDefinition = "text")
    private String deliveryNotes;

    /**
     * Delivery location stored as JSONB (captured via GPS)
     * Example: {
     *   "latitude": 34.0522,
     *   "longitude": -118.2437,
     *   "accuracy": 10.5,
     *   "altitude": 71.2,
     *   "address": "123 Main St, Los Angeles, CA 90001"
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object location;

    @Column(name = "delivered_at", nullable = false)
    private Instant deliveredAt;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_condition", length = 50)
    @Builder.Default
    private DeliveryCondition deliveryCondition = DeliveryCondition.GOOD;

    @Column(name = "damage_reported")
    @Builder.Default
    private Boolean damageReported = false;

    /**
     * Damage details stored as JSONB
     * Example: {
     *   "description": "Box was crushed on one corner",
     *   "severity": "Minor",
     *   "photos": ["damage-1.jpg", "damage-2.jpg"]
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "damage_details", columnDefinition = "jsonb")
    private Object damageDetails;

    @Column(name = "temperature_recorded", precision = 5, scale = 2)
    private java.math.BigDecimal temperatureRecorded;

    @Column(name = "temperature_unit", length = 1)
    @Builder.Default
    private String temperatureUnit = "F";

    /**
     * Additional metadata stored as JSONB
     * Example: {"delivery_method": "Front Door", "access_code": "1234", "gate_code": "5678"}
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captured_by")
    private User capturedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Convenience methods
    public boolean hasSignature() {
        return signatureUrl != null && !signatureUrl.isEmpty();
    }

    public boolean hasPhotos() {
        return photoUrls != null;
    }

    public boolean hasDamage() {
        return damageReported != null && damageReported;
    }

    public enum DeliveryCondition {
        GOOD,
        MINOR_DAMAGE,
        MAJOR_DAMAGE,
        REFUSED,
        PARTIALLY_DELIVERED
    }
}
