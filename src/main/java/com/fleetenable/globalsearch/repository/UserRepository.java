package com.fleetenable.globalsearch.repository;

import com.fleetenable.globalsearch.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity
 *
 * Provides authentication and user management queries
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find active user by username
     */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    Optional<User> findActiveByUsername(@Param("username") String username);

    /**
     * Find active user by email
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(@Param("email") String email);

    /**
     * Find users by role using JSONB contains
     */
    @Query(value = "SELECT * FROM users WHERE roles @> :role::jsonb AND deleted_at IS NULL", nativeQuery = true)
    java.util.List<User> findByRole(@Param("role") String role);

    /**
     * Search users by name with fuzzy matching (pg_trgm)
     * Uses similarity() function for typo tolerance
     */
    @Query(value = """
        SELECT u.*,
               GREATEST(
                   similarity(LOWER(u.first_name), LOWER(:searchTerm)),
                   similarity(LOWER(u.last_name), LOWER(:searchTerm)),
                   similarity(LOWER(u.first_name || ' ' || u.last_name), LOWER(:searchTerm))
               ) AS relevance
        FROM users u
        WHERE u.deleted_at IS NULL
          AND (
              similarity(LOWER(u.first_name), LOWER(:searchTerm)) > :threshold
           OR similarity(LOWER(u.last_name), LOWER(:searchTerm)) > :threshold
           OR similarity(LOWER(u.first_name || ' ' || u.last_name), LOWER(:searchTerm)) > :threshold
          )
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    java.util.List<User> searchByNameFuzzy(
        @Param("searchTerm") String searchTerm,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );
}
