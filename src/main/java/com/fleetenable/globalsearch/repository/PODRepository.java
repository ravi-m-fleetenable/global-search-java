package com.fleetenable.globalsearch.repository;

import com.fleetenable.globalsearch.model.POD;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for POD (Proof of Delivery) entity
 */
@Repository
public interface PODRepository extends JpaRepository<POD, Long> {

    /**
     * Find POD by order ID
     */
    @Query("SELECT p FROM POD p WHERE p.order.id = :orderId")
    Optional<POD> findByOrderId(@Param("orderId") Long orderId);

    /**
     * Find PODs with damage reports
     */
    @Query("SELECT p FROM POD p WHERE p.damageReported = true ORDER BY p.createdAt DESC")
    List<POD> findWithDamageReports();

    /**
     * Find PODs by delivery condition
     */
    List<POD> findByDeliveryCondition(POD.DeliveryCondition deliveryCondition);

    /**
     * Find recent PODs
     */
    @Query("SELECT p FROM POD p WHERE p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<POD> findRecentPODs(@Param("since") Instant since);

    /**
     * Find PODs by signed by name (fuzzy search)
     */
    @Query(value = """
        SELECT p.*,
               similarity(LOWER(p.signed_by), LOWER(:signedBy)) AS relevance
        FROM pods p
        WHERE similarity(LOWER(p.signed_by), LOWER(:signedBy)) > :threshold
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<POD> searchBySignedBy(
        @Param("signedBy") String signedBy,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Find PODs by delivery date range
     */
    @Query("SELECT p FROM POD p WHERE p.deliveredAt BETWEEN :startDate AND :endDate ORDER BY p.deliveredAt DESC")
    List<POD> findByDeliveryDateRange(
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Find PODs with photos
     */
    @Query(value = "SELECT * FROM pods WHERE photo_urls IS NOT NULL ORDER BY created_at DESC", nativeQuery = true)
    List<POD> findWithPhotos();

    /**
     * Find PODs with signatures
     */
    @Query("SELECT p FROM POD p WHERE p.signatureUrl IS NOT NULL ORDER BY p.createdAt DESC")
    List<POD> findWithSignatures();
}
