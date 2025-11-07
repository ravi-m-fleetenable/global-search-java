package com.fleetenable.globalsearch.controller;

import com.fleetenable.globalsearch.model.Fleet;
import com.fleetenable.globalsearch.repository.FleetRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Fleet REST Controller
 */
@RestController
@RequestMapping("/api/v1/fleets")
@Tag(name = "Fleets", description = "Fleet management APIs")
@RequiredArgsConstructor
@Slf4j
public class FleetController {

    private final FleetRepository fleetRepository;

    @GetMapping
    @Operation(summary = "Get all fleets", description = "Retrieve all active fleet vehicles")
    public ResponseEntity<List<Fleet>> getAllFleets() {
        List<Fleet> fleets = fleetRepository.findAllActive();
        return ResponseEntity.ok(fleets);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fleet by ID", description = "Retrieve a specific fleet vehicle by ID")
    public ResponseEntity<Fleet> getFleetById(@PathVariable Long id) {
        return fleetRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/vin/{vin}")
    @Operation(summary = "Get fleet by VIN", description = "Retrieve fleet vehicle by VIN")
    public ResponseEntity<Fleet> getFleetByVin(@PathVariable String vin) {
        return fleetRepository.findByVin(vin)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/license-plate/{licensePlate}")
    @Operation(summary = "Get fleet by license plate", description = "Retrieve fleet vehicle by license plate")
    public ResponseEntity<Fleet> getFleetByLicensePlate(@PathVariable String licensePlate) {
        return fleetRepository.findByLicensePlate(licensePlate)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get fleets by status", description = "Retrieve fleet vehicles by status")
    public ResponseEntity<List<Fleet>> getFleetsByStatus(@PathVariable String status) {
        try {
            Fleet.FleetStatus fleetStatus = Fleet.FleetStatus.valueOf(status.toUpperCase());
            List<Fleet> fleets = fleetRepository.findByStatusAndDeletedAtIsNull(fleetStatus);
            return ResponseEntity.ok(fleets);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/type/{vehicleType}")
    @Operation(summary = "Get fleets by vehicle type", description = "Retrieve fleet vehicles by type")
    public ResponseEntity<List<Fleet>> getFleetsByType(@PathVariable String vehicleType) {
        try {
            Fleet.VehicleType type = Fleet.VehicleType.valueOf(vehicleType.toUpperCase());
            List<Fleet> fleets = fleetRepository.findByVehicleTypeAndDeletedAtIsNull(type);
            return ResponseEntity.ok(fleets);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/maintenance-due")
    @Operation(summary = "Get vehicles requiring maintenance", description = "Find vehicles due for maintenance")
    public ResponseEntity<List<Fleet>> getVehiclesRequiringMaintenance() {
        List<Fleet> fleets = fleetRepository.findRequiringMaintenance(LocalDate.now().plusDays(7));
        return ResponseEntity.ok(fleets);
    }

    @GetMapping("/expiring-registration")
    @Operation(summary = "Get vehicles with expiring registration", description = "Find vehicles with expiring registration")
    public ResponseEntity<List<Fleet>> getVehiclesWithExpiringRegistration(
        @Parameter(description = "Days ahead to check")
        @RequestParam(defaultValue = "30") int days
    ) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        List<Fleet> fleets = fleetRepository.findWithExpiringRegistration(startDate, endDate);
        return ResponseEntity.ok(fleets);
    }

    @GetMapping("/search")
    @Operation(summary = "Search fleets with fuzzy matching", description = "Search fleet vehicles using pg_trgm")
    public ResponseEntity<List<Fleet>> searchFleets(
        @Parameter(description = "Search query", required = true)
        @RequestParam String q,

        @Parameter(description = "Similarity threshold (0.0-1.0)")
        @RequestParam(defaultValue = "0.3") double threshold,

        @Parameter(description = "Maximum results")
        @RequestParam(defaultValue = "50") int limit
    ) {
        List<Fleet> fleets = fleetRepository.searchFuzzy(q, threshold, limit);
        return ResponseEntity.ok(fleets);
    }

    @GetMapping("/search/vin")
    @Operation(summary = "Search by VIN with fuzzy matching", description = "Search for VIN with typo tolerance")
    public ResponseEntity<List<Fleet>> searchByVin(
        @Parameter(description = "VIN to search", required = true)
        @RequestParam String vin,

        @Parameter(description = "Similarity threshold (0.0-1.0)")
        @RequestParam(defaultValue = "0.3") double threshold,

        @Parameter(description = "Maximum results")
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<Fleet> fleets = fleetRepository.searchByVin(vin, threshold, limit);
        return ResponseEntity.ok(fleets);
    }

    @PostMapping
    @Operation(summary = "Create fleet vehicle", description = "Create a new fleet vehicle")
    public ResponseEntity<Fleet> createFleet(@RequestBody Fleet fleet) {
        Fleet saved = fleetRepository.save(fleet);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update fleet vehicle", description = "Update an existing fleet vehicle")
    public ResponseEntity<Fleet> updateFleet(@PathVariable Long id, @RequestBody Fleet fleet) {
        if (!fleetRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        fleet.setId(id);
        Fleet updated = fleetRepository.save(fleet);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete fleet vehicle", description = "Soft delete a fleet vehicle")
    public ResponseEntity<Void> deleteFleet(@PathVariable Long id) {
        return fleetRepository.findById(id)
            .map(fleet -> {
                fleet.setDeletedAt(java.time.Instant.now());
                fleetRepository.save(fleet);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
