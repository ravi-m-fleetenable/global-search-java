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
 * Order entity - Shipment orders with HAWB numbers, status, origin/destination
 *
 * PARTITIONED TABLE: Partitioned by created_at (monthly partitions)
 * Searchable fields: order_number, hawb_numbers, notes
 * Uses GIN index with partial index on recent data (12 months)
 *
 * NOTE: Partitioning requires composite primary key (id, created_at)
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_order_number", columnList = "order_number"),
    @Index(name = "idx_orders_account_id", columnList = "account_id"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_driver_id", columnList = "driver_id"),
    @Index(name = "idx_orders_fleet_id", columnList = "fleet_id"),
    @Index(name = "idx_orders_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"account", "driver", "fleet", "createdBy", "updatedBy"})
@EqualsAndHashCode(of = {"id", "createdAt"})
@IdClass(Order.OrderId.class) // Composite key for partitioning
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Partition key - part of composite primary key
     * Required for PostgreSQL declarative partitioning
     */
    @Id
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    @NotBlank(message = "Order number is required")
    @Size(max = 50, message = "Order number must not exceed 50 characters")
    private String orderNumber;

    /**
     * HAWB (House Air Waybill) numbers stored as JSONB array
     * Example: ["HAWB-001", "HAWB-002", "HAWB-003"]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "hawb_numbers", columnDefinition = "jsonb")
    private Object hawbNumbers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", length = 50)
    @Builder.Default
    private PriorityLevel priorityLevel = PriorityLevel.NORMAL;

    /**
     * Origin address stored as JSONB
     * Example: {
     *   "street": "123 Warehouse Rd",
     *   "city": "Dallas",
     *   "state": "TX",
     *   "zip": "75001",
     *   "country": "USA",
     *   "latitude": 32.7767,
     *   "longitude": -96.7970
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object origin;

    /**
     * Destination address stored as JSONB
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object destination;

    @Column(name = "pickup_date")
    private LocalDate pickupDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fleet_id")
    private Fleet fleet;

    @Column(name = "total_weight", precision = 10, scale = 2)
    private BigDecimal totalWeight;

    @Column(name = "weight_unit", length = 10)
    @Builder.Default
    private String weightUnit = "lbs";

    @Column(name = "total_volume", precision = 10, scale = 2)
    private BigDecimal totalVolume;

    @Column(name = "volume_unit", length = 10)
    @Builder.Default
    private String volumeUnit = "ft3";

    @Column(name = "package_count")
    private Integer packageCount;

    /**
     * Package details stored as JSONB array
     * Example: [
     *   {"type": "Box", "weight": 50, "dimensions": {"l": 24, "w": 18, "h": 12}},
     *   {"type": "Pallet", "weight": 500, "dimensions": {"l": 48, "w": 40, "h": 48}}
     * ]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "package_details", columnDefinition = "jsonb")
    private Object packageDetails;

    @Column(name = "shipping_cost", precision = 15, scale = 2)
    private BigDecimal shippingCost;

    @Column(name = "additional_charges", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal additionalCharges = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    /**
     * Service type stored as JSONB
     * Example: {"type": "Express", "level": "Next Day", "insurance": true, "signature_required": true}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "service_type", columnDefinition = "jsonb")
    private Object serviceType;

    /**
     * Special instructions stored as JSONB
     * Example: {"handling": "Fragile", "delivery_instructions": "Call before delivery", "access_restrictions": [...]}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "special_instructions", columnDefinition = "jsonb")
    private Object specialInstructions;

    /**
     * Tracking events stored as JSONB array
     * Example: [
     *   {"timestamp": "2024-01-15T10:00:00Z", "status": "Picked Up", "location": "Dallas, TX"},
     *   {"timestamp": "2024-01-15T14:30:00Z", "status": "In Transit", "location": "Fort Worth, TX"}
     * ]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "tracking_events", columnDefinition = "jsonb")
    private Object trackingEvents;

    @Column(name = "signature_required")
    @Builder.Default
    private Boolean signatureRequired = false;

    @Column(name = "proof_of_delivery_url", length = 500)
    private String proofOfDeliveryUrl;

    /**
     * Additional metadata stored as JSONB
     * Example: {"reference_numbers": ["REF123", "PO456"], "customer_notes": "Urgent", "internal_notes": "..."}
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

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Convenience methods
    public boolean isActive() {
        return deletedAt == null;
    }

    public boolean isDelivered() {
        return status == OrderStatus.DELIVERED && actualDeliveryDate != null;
    }

    public boolean isDelayed() {
        if (estimatedDeliveryDate == null || actualDeliveryDate == null) {
            return estimatedDeliveryDate != null &&
                   estimatedDeliveryDate.isBefore(LocalDate.now()) &&
                   status != OrderStatus.DELIVERED;
        }
        return actualDeliveryDate.isAfter(estimatedDeliveryDate);
    }

    public boolean isInTransit() {
        return status == OrderStatus.IN_TRANSIT;
    }

    /**
     * Composite key class for partitioned table
     * Required for PostgreSQL declarative partitioning
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderId implements java.io.Serializable {
        private Long id;
        private Instant createdAt;
    }

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        PICKED_UP,
        IN_TRANSIT,
        OUT_FOR_DELIVERY,
        DELIVERED,
        CANCELLED,
        RETURNED,
        ON_HOLD
    }

    public enum PriorityLevel {
        LOW,
        NORMAL,
        HIGH,
        URGENT,
        CRITICAL
    }
}
