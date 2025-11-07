package com.fleetenable.globalsearch.controller;

import com.fleetenable.globalsearch.model.Driver;
import com.fleetenable.globalsearch.repository.DriverRepository;
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
 * Driver REST Controller
 */
@RestController
@RequestMapping("/api/v1/drivers")
@Tag(name = "Drivers", description = "Driver management APIs")
@RequiredArgsConstructor
@Slf4j
public class DriverController {

    private final DriverRepository driverRepository;

    @GetMapping
    @Operation(summary = "Get all drivers", description = "Retrieve all active drivers")
    public ResponseEntity<List<Driver>> getAllDrivers() {
        List<Driver> drivers = driverRepository.findAllActive();
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get driver by ID", description = "Retrieve a specific driver by ID")
    public ResponseEntity<Driver> getDriverById(@PathVariable Long id) {
        return driverRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/license/{licenseNumber}")
    @Operation(summary = "Get driver by license number", description = "Retrieve driver by license number")
    public ResponseEntity<Driver> getDriverByLicense(@PathVariable String licenseNumber) {
        return driverRepository.findByLicenseNumber(licenseNumber)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get drivers by status", description = "Retrieve drivers by status")
    public ResponseEntity<List<Driver>> getDriversByStatus(@PathVariable String status) {
        try {
            Driver.DriverStatus driverStatus = Driver.DriverStatus.valueOf(status.toUpperCase());
            List<Driver> drivers = driverRepository.findByStatusAndDeletedAtIsNull(driverStatus);
            return ResponseEntity.ok(drivers);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/expiring-licenses")
    @Operation(summary = "Get drivers with expiring licenses", description = "Find drivers whose licenses expire soon")
    public ResponseEntity<List<Driver>> getDriversWithExpiringLicenses(
        @Parameter(description = "Days ahead to check")
        @RequestParam(defaultValue = "30") int days
    ) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        List<Driver> drivers = driverRepository.findWithExpiringLicenses(startDate, endDate);
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/search")
    @Operation(summary = "Search drivers with fuzzy matching", description = "Search drivers using pg_trgm")
    public ResponseEntity<List<Driver>> searchDrivers(
        @Parameter(description = "Search query", required = true)
        @RequestParam String q,

        @Parameter(description = "Similarity threshold (0.0-1.0)")
        @RequestParam(defaultValue = "0.3") double threshold,

        @Parameter(description = "Maximum results")
        @RequestParam(defaultValue = "50") int limit
    ) {
        List<Driver> drivers = driverRepository.searchFuzzy(q, threshold, limit);
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/search/name")
    @Operation(summary = "Search drivers by full name", description = "Search drivers by full name with fuzzy matching")
    public ResponseEntity<List<Driver>> searchByFullName(
        @Parameter(description = "Full name", required = true)
        @RequestParam String name,

        @Parameter(description = "Similarity threshold (0.0-1.0)")
        @RequestParam(defaultValue = "0.3") double threshold,

        @Parameter(description = "Maximum results")
        @RequestParam(defaultValue = "50") int limit
    ) {
        List<Driver> drivers = driverRepository.searchByFullName(name, threshold, limit);
        return ResponseEntity.ok(drivers);
    }

    @PostMapping
    @Operation(summary = "Create driver", description = "Create a new driver")
    public ResponseEntity<Driver> createDriver(@RequestBody Driver driver) {
        Driver saved = driverRepository.save(driver);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update driver", description = "Update an existing driver")
    public ResponseEntity<Driver> updateDriver(@PathVariable Long id, @RequestBody Driver driver) {
        if (!driverRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        driver.setId(id);
        Driver updated = driverRepository.save(driver);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete driver", description = "Soft delete a driver")
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        return driverRepository.findById(id)
            .map(driver -> {
                driver.setDeletedAt(java.time.Instant.now());
                driverRepository.save(driver);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
