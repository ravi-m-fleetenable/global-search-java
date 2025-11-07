package com.fleetenable.globalsearch.service;

import com.fleetenable.globalsearch.config.SearchConfig;
import com.fleetenable.globalsearch.dto.SearchResult;
import com.fleetenable.globalsearch.model.*;
import com.fleetenable.globalsearch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global Search Service - Cross-entity search with UNION-like behavior
 *
 * Searches across all entities: Orders, Accounts, Drivers, Fleets
 * Uses pg_trgm fuzzy matching for typo tolerance
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalSearchService {

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final DriverRepository driverRepository;
    private final FleetRepository fleetRepository;
    private final SearchConfig searchConfig;

    /**
     * Perform global search across all entities
     *
     * @param searchTerm The search query
     * @param maxResults Maximum results to return (optional)
     * @return List of search results sorted by relevance
     */
    public List<SearchResult> searchAll(String searchTerm, Integer maxResults) {
        log.info("Global search: term='{}', maxResults={}", searchTerm, maxResults);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        int limit = maxResults != null ? maxResults : searchConfig.getGlobalSearch().getMaxResults();
        double threshold = searchConfig.getSimilarity().getGlobal();

        // Calculate time range for partition pruning (orders only)
        Instant startDate = Instant.now().minus(
            searchConfig.getGlobalSearch().getDefaultTimeRangeMonths() * 30L,
            ChronoUnit.DAYS
        );

        // Search in parallel for better performance
        List<SearchResult> allResults = new ArrayList<>();

        // Search orders (with partition pruning)
        try {
            List<Order> orders = orderRepository.searchFuzzyRecent(searchTerm, startDate, threshold, limit);
            allResults.addAll(orders.stream()
                .map(this::convertOrderToSearchResult)
                .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error searching orders", e);
        }

        // Search accounts
        try {
            List<Account> accounts = accountRepository.searchFuzzy(searchTerm, threshold, limit);
            allResults.addAll(accounts.stream()
                .map(this::convertAccountToSearchResult)
                .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error searching accounts", e);
        }

        // Search drivers
        try {
            List<Driver> drivers = driverRepository.searchFuzzy(searchTerm, threshold, limit);
            allResults.addAll(drivers.stream()
                .map(this::convertDriverToSearchResult)
                .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error searching drivers", e);
        }

        // Search fleets
        try {
            List<Fleet> fleets = fleetRepository.searchFuzzy(searchTerm, threshold, limit);
            allResults.addAll(fleets.stream()
                .map(this::convertFleetToSearchResult)
                .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error searching fleets", e);
        }

        // Sort by relevance and limit results
        return allResults.stream()
            .sorted(Comparator.comparing(SearchResult::getRelevance).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Search within a specific entity type
     */
    public List<SearchResult> searchByEntityType(String searchTerm, String entityType, Integer maxResults) {
        log.info("Entity search: term='{}', entityType='{}', maxResults={}", searchTerm, entityType, maxResults);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        int limit = maxResults != null ? maxResults : searchConfig.getGlobalSearch().getMaxResults();
        double threshold = searchConfig.getSimilarity().getGlobal();

        return switch (entityType.toLowerCase()) {
            case "order", "orders" -> searchOrders(searchTerm, threshold, limit);
            case "account", "accounts" -> searchAccounts(searchTerm, threshold, limit);
            case "driver", "drivers" -> searchDrivers(searchTerm, threshold, limit);
            case "fleet", "fleets" -> searchFleets(searchTerm, threshold, limit);
            default -> {
                log.warn("Unknown entity type: {}", entityType);
                yield List.of();
            }
        };
    }

    private List<SearchResult> searchOrders(String searchTerm, double threshold, int limit) {
        Instant startDate = Instant.now().minus(
            searchConfig.getGlobalSearch().getDefaultTimeRangeMonths() * 30L,
            ChronoUnit.DAYS
        );
        List<Order> orders = orderRepository.searchFuzzyRecent(searchTerm, startDate, threshold, limit);
        return orders.stream()
            .map(this::convertOrderToSearchResult)
            .collect(Collectors.toList());
    }

    private List<SearchResult> searchAccounts(String searchTerm, double threshold, int limit) {
        List<Account> accounts = accountRepository.searchFuzzy(searchTerm, threshold, limit);
        return accounts.stream()
            .map(this::convertAccountToSearchResult)
            .collect(Collectors.toList());
    }

    private List<SearchResult> searchDrivers(String searchTerm, double threshold, int limit) {
        List<Driver> drivers = driverRepository.searchFuzzy(searchTerm, threshold, limit);
        return drivers.stream()
            .map(this::convertDriverToSearchResult)
            .collect(Collectors.toList());
    }

    private List<SearchResult> searchFleets(String searchTerm, double threshold, int limit) {
        List<Fleet> fleets = fleetRepository.searchFuzzy(searchTerm, threshold, limit);
        return fleets.stream()
            .map(this::convertFleetToSearchResult)
            .collect(Collectors.toList());
    }

    // Conversion methods
    private SearchResult convertOrderToSearchResult(Order order) {
        return SearchResult.builder()
            .entityType("order")
            .entityId(order.getId())
            .primaryIdentifier(order.getOrderNumber())
            .displayName("Order " + order.getOrderNumber())
            .secondaryInfo(order.getAccount() != null ? order.getAccount().getAccountName() : null)
            .relevance(0.0) // Set by database query
            .createdAt(order.getCreatedAt())
            .status(order.getStatus().name())
            .build();
    }

    private SearchResult convertAccountToSearchResult(Account account) {
        return SearchResult.builder()
            .entityType("account")
            .entityId(account.getId())
            .primaryIdentifier(account.getAccountNumber())
            .displayName(account.getAccountName())
            .secondaryInfo(account.getCompanyName())
            .relevance(0.0) // Set by database query
            .createdAt(account.getCreatedAt())
            .status(account.getStatus().name())
            .build();
    }

    private SearchResult convertDriverToSearchResult(Driver driver) {
        return SearchResult.builder()
            .entityType("driver")
            .entityId(driver.getId())
            .primaryIdentifier(driver.getLicenseNumber())
            .displayName(driver.getFullName())
            .secondaryInfo("License: " + driver.getLicenseNumber())
            .relevance(0.0) // Set by database query
            .createdAt(driver.getCreatedAt())
            .status(driver.getStatus().name())
            .build();
    }

    private SearchResult convertFleetToSearchResult(Fleet fleet) {
        return SearchResult.builder()
            .entityType("fleet")
            .entityId(fleet.getId())
            .primaryIdentifier(fleet.getVin())
            .displayName(fleet.getDisplayName())
            .secondaryInfo("VIN: " + fleet.getVin())
            .relevance(0.0) // Set by database query
            .createdAt(fleet.getCreatedAt())
            .status(fleet.getStatus().name())
            .build();
    }
}
