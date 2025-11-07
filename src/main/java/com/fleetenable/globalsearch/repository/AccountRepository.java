package com.fleetenable.globalsearch.repository;

import com.fleetenable.globalsearch.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Account entity
 *
 * Uses GIN index for multi-attribute fuzzy search
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find account by account number
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Find active accounts
     */
    @Query("SELECT a FROM Account a WHERE a.status = 'ACTIVE' AND a.deletedAt IS NULL")
    List<Account> findAllActive();

    /**
     * Find accounts by status
     */
    List<Account> findByStatusAndDeletedAtIsNull(Account.AccountStatus status);

    /**
     * Multi-attribute fuzzy search using GIN index
     * Searches across: account_number, account_name, company_name, contact_person, email
     *
     * Uses the GIN index: idx_accounts_search_gin
     */
    @Query(value = """
        SELECT a.*,
               similarity(
                   LOWER(a.account_number || ' ' ||
                         a.account_name || ' ' ||
                         COALESCE(a.company_name, '') || ' ' ||
                         COALESCE(a.contact_person, '') || ' ' ||
                         COALESCE(a.email, '')),
                   LOWER(:searchTerm)
               ) AS relevance
        FROM accounts a
        WHERE a.deleted_at IS NULL
          AND similarity(
              LOWER(a.account_number || ' ' ||
                    a.account_name || ' ' ||
                    COALESCE(a.company_name, '') || ' ' ||
                    COALESCE(a.contact_person, '') || ' ' ||
                    COALESCE(a.email, '')),
              LOWER(:searchTerm)
          ) > :threshold
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Account> searchFuzzy(
        @Param("searchTerm") String searchTerm,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Autocomplete for account name/company name
     * Uses word_similarity for prefix matching
     */
    @Query(value = """
        SELECT a.*,
               GREATEST(
                   word_similarity(:searchTerm, LOWER(a.account_name)),
                   word_similarity(:searchTerm, LOWER(COALESCE(a.company_name, '')))
               ) AS relevance
        FROM accounts a
        WHERE a.deleted_at IS NULL
          AND (
              word_similarity(:searchTerm, LOWER(a.account_name)) > :threshold
           OR word_similarity(:searchTerm, LOWER(COALESCE(a.company_name, ''))) > :threshold
          )
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Account> autocomplete(
        @Param("searchTerm") String searchTerm,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Search by account number with exact and fuzzy matching
     */
    @Query(value = """
        SELECT a.*,
               CASE
                   WHEN LOWER(a.account_number) = LOWER(:accountNumber) THEN 1.0
                   ELSE similarity(LOWER(a.account_number), LOWER(:accountNumber))
               END AS relevance
        FROM accounts a
        WHERE a.deleted_at IS NULL
          AND (
              LOWER(a.account_number) = LOWER(:accountNumber)
           OR similarity(LOWER(a.account_number), LOWER(:accountNumber)) > :threshold
          )
        ORDER BY relevance DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Account> searchByAccountNumber(
        @Param("accountNumber") String accountNumber,
        @Param("threshold") double threshold,
        @Param("maxResults") int maxResults
    );

    /**
     * Find accounts with outstanding balance exceeding credit limit
     */
    @Query("SELECT a FROM Account a WHERE a.creditLimit IS NOT NULL AND a.outstandingBalance > a.creditLimit AND a.deletedAt IS NULL")
    List<Account> findAccountsExceedingCreditLimit();

    /**
     * Full-text search across all searchable fields with pagination
     */
    @Query(value = """
        SELECT a.*,
               similarity(
                   LOWER(a.account_number || ' ' ||
                         a.account_name || ' ' ||
                         COALESCE(a.company_name, '') || ' ' ||
                         COALESCE(a.contact_person, '') || ' ' ||
                         COALESCE(a.email, '')),
                   LOWER(:searchTerm)
               ) AS relevance
        FROM accounts a
        WHERE a.deleted_at IS NULL
          AND similarity(
              LOWER(a.account_number || ' ' ||
                    a.account_name || ' ' ||
                    COALESCE(a.company_name, '') || ' ' ||
                    COALESCE(a.contact_person, '') || ' ' ||
                    COALESCE(a.email, '')),
              LOWER(:searchTerm)
          ) > :threshold
        ORDER BY relevance DESC
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM accounts a
        WHERE a.deleted_at IS NULL
          AND similarity(
              LOWER(a.account_number || ' ' ||
                    a.account_name || ' ' ||
                    COALESCE(a.company_name, '') || ' ' ||
                    COALESCE(a.contact_person, '') || ' ' ||
                    COALESCE(a.email, '')),
              LOWER(:searchTerm)
          ) > :threshold
        """,
        nativeQuery = true)
    Page<Account> searchFuzzyPaginated(
        @Param("searchTerm") String searchTerm,
        @Param("threshold") double threshold,
        Pageable pageable
    );
}
