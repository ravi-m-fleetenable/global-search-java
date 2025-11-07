package com.fleetenable.globalsearch.repository;

import com.fleetenable.globalsearch.model.Fleet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Fleet entity
 *
 * Uses GIN index for multi-attribute fuzzy search
 */
@Repository
public interface FleetRepository extends JpaRepository<Fleet, Long> {

    /**
     * Find fleet by VIN
     */
    Optional<Fleet> findByVin(String vin);

    /**
     * Find fleet by license plate
     */
    Optional<Fleet> findByLicensePlate(String licensePlate);

    /**
     * Find fleet by unit number
     */
    Optional<Fleet> findByUnitNumber(String unitNumber);

    /**
     * Find active fleets
     */
    @Query("SELECT f FROM Fleet f WHERE f.status = 'ACTIVE' AND f.deletedAt IS NULL")
    List<Fleet> findAllActive();

    /**
     * Find fleets by status
     */
    List<Fleet> findByStatusAndDeletedAtIsNull(Fleet.FleetStatus status);

    /**
     * Find fleets by vehicle type
     */
    List<Fleet> findByVehicleTypeAndDeletedAtIsNull(Fleet.VehicleType vehicleType);

    /**
     * Multi-attribute fuzzy search using GIN index
     * Searches across: vin, license_plate, unit_number, make, model
     *
     * Uses the GIN index: idx_fleets_search_gin
     */
    @Query(value = """
        SELECT f.*,
               similarity(
                   LOWER(f.vin || ' ' ||
                         f.license_plate || ' ' ||
                         COALESCE(f.unit_number, '') || ' ' ||
                         f.make || ' ' ||
                         f.model),
                   LOWER(:searchTerm)
               ) AS relevance
        FROM fleets f
        WHERE f.deleted_at IS NULL
          AND similarity(
              LOWER(f.vin || ' ' ||
                    f.license_plate || ' ' ||
                    COALESCE(f.unit_number, '') || ' ' ||
                    f.make || ' ' ||
                    f.model),
              LOWER(:searchTerm)
          ) > :threshold
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Fleet> searchFuzzy(
        @Param("searchTerm") String searchTerm,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Autocomplete for vehicle make/model
     * Uses word_similarity for prefix matching
     */
    @Query(value = """
        SELECT f.*,
               GREATEST(
                   word_similarity(:searchTerm, LOWER(f.make)),
                   word_similarity(:searchTerm, LOWER(f.model)),
                   word_similarity(:searchTerm, LOWER(f.make || ' ' || f.model))
               ) AS relevance
        FROM fleets f
        WHERE f.deleted_at IS NULL
          AND f.status = 'ACTIVE'
          AND (
              word_similarity(:searchTerm, LOWER(f.make)) > :threshold
           OR word_similarity(:searchTerm, LOWER(f.model)) > :threshold
           OR word_similarity(:searchTerm, LOWER(f.make || ' ' || f.model)) > :threshold
          )
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Fleet> autocomplete(
        @Param("searchTerm") String searchTerm,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Search by VIN with fuzzy matching (typo tolerance)
     */
    @Query(value = """
        SELECT f.*,
               similarity(LOWER(f.vin), LOWER(:vin)) AS relevance
        FROM fleets f
        WHERE f.deleted_at IS NULL
          AND similarity(LOWER(f.vin), LOWER(:vin)) > :threshold
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Fleet> searchByVin(
        @Param("vin") String vin,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Search by make and model
     */
    @Query(value = """
        SELECT f.*,
               similarity(LOWER(f.make || ' ' || f.model), LOWER(:makeModel)) AS relevance
        FROM fleets f
        WHERE f.deleted_at IS NULL
          AND similarity(LOWER(f.make || ' ' || f.model), LOWER(:makeModel)) > :threshold
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Fleet> searchByMakeModel(
        @Param("makeModel") String makeModel,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Find vehicles with expiring registration
     */
    @Query("SELECT f FROM Fleet f WHERE f.registrationExpiry BETWEEN :startDate AND :endDate AND f.deletedAt IS NULL")
    List<Fleet> findWithExpiringRegistration(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find vehicles with expiring inspection
     */
    @Query("SELECT f FROM Fleet f WHERE f.inspectionExpiry BETWEEN :startDate AND :endDate AND f.deletedAt IS NULL")
    List<Fleet> findWithExpiringInspection(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find vehicles with expiring insurance
     */
    @Query("SELECT f FROM Fleet f WHERE f.insuranceExpiry BETWEEN :startDate AND :endDate AND f.deletedAt IS NULL")
    List<Fleet> findWithExpiringInsurance(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find vehicles requiring maintenance
     */
    @Query("SELECT f FROM Fleet f WHERE f.nextMaintenanceDate <= :date AND f.deletedAt IS NULL")
    List<Fleet> findRequiringMaintenance(@Param("date") LocalDate date);
}
