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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fleet entity - Vehicle inventory with VIN, license plate, and specifications
 *
 * Searchable fields: vin, license_plate, make, model, unit_number
 * Uses GIN index for multi-attribute fuzzy search
 */
@Entity
@Table(name = "fleets", indexes = {
    @Index(name = "idx_fleets_vin", columnList = "vin"),
    @Index(name = "idx_fleets_license_plate", columnList = "license_plate"),
    @Index(name = "idx_fleets_unit_number", columnList = "unit_number"),
    @Index(name = "idx_fleets_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"createdBy", "updatedBy"})
@EqualsAndHashCode(of = {"id"})
public class Fleet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 17)
    @NotBlank(message = "VIN is required")
    @Size(min = 17, max = 17, message = "VIN must be exactly 17 characters")
    private String vin;

    @Column(name = "license_plate", unique = true, nullable = false, length = 20)
    @NotBlank(message = "License plate is required")
    @Size(max = 20, message = "License plate must not exceed 20 characters")
    private String licensePlate;

    @Column(name = "license_state", length = 2)
    @Size(min = 2, max = 2, message = "License state must be 2 characters")
    private String licenseState;

    @Column(name = "unit_number", unique = true, length = 50)
    private String unitNumber;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Make is required")
    @Size(max = 100, message = "Make must not exceed 100 characters")
    private String make;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Model is required")
    @Size(max = 100, message = "Model must not exceed 100 characters")
    private String model;

    @Column(name = "model_year")
    private Integer modelYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 50)
    @Builder.Default
    private VehicleType vehicleType = VehicleType.TRUCK;

    @Column(length = 50)
    private String color;

    @Column(name = "fuel_type", length = 50)
    private String fuelType;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_price", precision = 15, scale = 2)
    private java.math.BigDecimal purchasePrice;

    @Column(name = "current_value", precision = 15, scale = 2)
    private java.math.BigDecimal currentValue;

    @Column(name = "odometer_reading")
    private Integer odometerReading;

    @Column(name = "odometer_unit", length = 10)
    @Builder.Default
    private String odometerUnit = "miles";

    /**
     * Specifications stored as JSONB
     * Example: {
     *   "engine": "6.7L Cummins Diesel",
     *   "transmission": "Automatic",
     *   "gvwr": "26000",
     *   "payload_capacity": "15000",
     *   "towing_capacity": "20000",
     *   "dimensions": {"length": "240", "width": "96", "height": "120"}
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object specifications;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private FleetStatus status = FleetStatus.ACTIVE;

    @Column(name = "registration_expiry")
    private LocalDate registrationExpiry;

    @Column(name = "inspection_expiry")
    private LocalDate inspectionExpiry;

    @Column(name = "insurance_expiry")
    private LocalDate insuranceExpiry;

    /**
     * Insurance information stored as JSONB
     * Example: {
     *   "provider": "State Farm",
     *   "policy_number": "POL123456",
     *   "coverage_amount": 1000000,
     *   "deductible": 5000
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "insurance_info", columnDefinition = "jsonb")
    private Object insuranceInfo;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    @Column(name = "maintenance_interval_miles")
    private Integer maintenanceIntervalMiles;

    /**
     * Maintenance records stored as JSONB array
     * Example: [
     *   {"date": "2024-01-15", "type": "Oil Change", "cost": 150.00, "odometer": 45000},
     *   {"date": "2024-02-20", "type": "Tire Rotation", "cost": 80.00, "odometer": 48000}
     * ]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "maintenance_records", columnDefinition = "jsonb")
    private Object maintenanceRecords;

    /**
     * GPS tracking information stored as JSONB
     * Example: {"device_id": "GPS123456", "provider": "Geotab", "last_location": {...}}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "gps_info", columnDefinition = "jsonb")
    private Object gpsInfo;

    /**
     * Additional metadata stored as JSONB
     * Example: {"parking_location": "Lot A", "assigned_driver_id": 42, "custom_fields": {...}}
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
        return status == FleetStatus.ACTIVE && deletedAt == null;
    }

    public boolean isRegistrationExpired() {
        return registrationExpiry != null && registrationExpiry.isBefore(LocalDate.now());
    }

    public boolean isInspectionExpired() {
        return inspectionExpiry != null && inspectionExpiry.isBefore(LocalDate.now());
    }

    public boolean isInsuranceExpired() {
        return insuranceExpiry != null && insuranceExpiry.isBefore(LocalDate.now());
    }

    public boolean isMaintenanceDue() {
        if (nextMaintenanceDate != null) {
            return nextMaintenanceDate.isBefore(LocalDate.now().plusDays(7));
        }
        if (maintenanceIntervalMiles != null && odometerReading != null && lastMaintenanceDate != null) {
            // Simplified check - real implementation would track odometer at last maintenance
            return true; // Placeholder
        }
        return false;
    }

    public String getDisplayName() {
        return modelYear + " " + make + " " + model + " (" + unitNumber + ")";
    }

    public enum VehicleType {
        TRUCK,
        VAN,
        TRAILER,
        TRACTOR,
        STRAIGHT_TRUCK,
        SPRINTER,
        BOX_TRUCK,
        FLATBED,
        REFRIGERATED,
        OTHER
    }

    public enum FleetStatus {
        ACTIVE,
        INACTIVE,
        IN_MAINTENANCE,
        OUT_OF_SERVICE,
        SOLD,
        TOTALED
    }
}
