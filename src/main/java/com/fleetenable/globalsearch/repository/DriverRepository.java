package com.fleetenable.globalsearch.repository;

import com.fleetenable.globalsearch.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Driver entity
 *
 * Uses GiST index for similarity-based search
 */
@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    /**
     * Find driver by license number
     */
    Optional<Driver> findByLicenseNumber(String licenseNumber);

    /**
     * Find active drivers
     */
    @Query("SELECT d FROM Driver d WHERE d.status = 'ACTIVE' AND d.deletedAt IS NULL")
    List<Driver> findAllActive();

    /**
     * Find drivers by status
     */
    List<Driver> findByStatusAndDeletedAtIsNull(Driver.DriverStatus status);

    /**
     * Find drivers with expiring licenses
     */
    @Query("SELECT d FROM Driver d WHERE d.licenseExpiry BETWEEN :startDate AND :endDate AND d.deletedAt IS NULL")
    List<Driver> findWithExpiringLicenses(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Multi-attribute fuzzy search using GiST index
     * Searches across: first_name, last_name, email, license_number, phone_number
     *
     * Uses the GiST index: idx_drivers_search_gist
     */
    @Query(value = """
        SELECT d.*,
               similarity(
                   LOWER(d.first_name || ' ' || d.last_name || ' ' ||
                         COALESCE(d.email, '') || ' ' ||
                         d.license_number || ' ' ||
                         COALESCE(d.phone_number, '')),
                   LOWER(:searchTerm)
               ) AS relevance
        FROM drivers d
        WHERE d.deleted_at IS NULL
          AND similarity(
              LOWER(d.first_name || ' ' || d.last_name || ' ' ||
                    COALESCE(d.email, '') || ' ' ||
                    d.license_number || ' ' ||
                    COALESCE(d.phone_number, '')),
              LOWER(:searchTerm)
          ) > :threshold
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Driver> searchFuzzy(
        @Param("searchTerm") String searchTerm,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Autocomplete for driver name
     * Uses word_similarity for prefix matching
     */
    @Query(value = """
        SELECT d.*,
               GREATEST(
                   word_similarity(:searchTerm, LOWER(d.first_name)),
                   word_similarity(:searchTerm, LOWER(d.last_name)),
                   word_similarity(:searchTerm, LOWER(d.first_name || ' ' || d.last_name))
               ) AS relevance
        FROM drivers d
        WHERE d.deleted_at IS NULL
          AND d.status = 'ACTIVE'
          AND (
              word_similarity(:searchTerm, LOWER(d.first_name)) > :threshold
           OR word_similarity(:searchTerm, LOWER(d.last_name)) > :threshold
           OR word_similarity(:searchTerm, LOWER(d.first_name || ' ' || d.last_name)) > :threshold
          )
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Driver> autocomplete(
        @Param("searchTerm") String searchTerm,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Search by license number with fuzzy matching
     */
    @Query(value = """
        SELECT d.*,
               similarity(LOWER(d.license_number), LOWER(:licenseNumber)) AS relevance
        FROM drivers d
        WHERE d.deleted_at IS NULL
          AND similarity(LOWER(d.license_number), LOWER(:licenseNumber)) > :threshold
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Driver> searchByLicenseNumber(
        @Param("licenseNumber") String licenseNumber,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Search by full name (first + last) with high relevance
     */
    @Query(value = """
        SELECT d.*,
               similarity(LOWER(d.first_name || ' ' || d.last_name), LOWER(:fullName)) AS relevance
        FROM drivers d
        WHERE d.deleted_at IS NULL
          AND similarity(LOWER(d.first_name || ' ' || d.last_name), LOWER(:fullName)) > :threshold
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Driver> searchByFullName(
        @Param("fullName") String fullName,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Find drivers requiring background check
     */
    @Query("SELECT d FROM Driver d WHERE d.backgroundCheckDate IS NULL OR d.backgroundCheckDate < :beforeDate AND d.deletedAt IS NULL")
    List<Driver> findRequiringBackgroundCheck(@Param("beforeDate") LocalDate beforeDate);

    /**
     * Find drivers requiring drug test
     */
    @Query("SELECT d FROM Driver d WHERE d.drugTestDate IS NULL OR d.drugTestDate < :beforeDate AND d.deletedAt IS NULL")
    List<Driver> findRequiringDrugTest(@Param("beforeDate") LocalDate beforeDate);
}
