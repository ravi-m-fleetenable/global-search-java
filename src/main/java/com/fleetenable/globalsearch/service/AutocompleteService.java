package com.fleetenable.globalsearch.service;

import com.fleetenable.globalsearch.config.SearchConfig;
import com.fleetenable.globalsearch.dto.AutocompleteResult;
import com.fleetenable.globalsearch.model.*;
import com.fleetenable.globalsearch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Autocomplete Service - Fast prefix matching using word_similarity
 *
 * Uses PostgreSQL word_similarity() function for efficient autocomplete
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutocompleteService {

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final DriverRepository driverRepository;
    private final FleetRepository fleetRepository;
    private final SearchConfig searchConfig;

    /**
     * Autocomplete across all entities
     *
     * @param searchTerm Partial search term
     * @param entityType Optional entity type filter
     * @return List of autocomplete suggestions
     */
    public List<AutocompleteResult> autocomplete(String searchTerm, String entityType) {
        log.info("Autocomplete: term='{}', entityType='{}'", searchTerm, entityType);

        if (searchTerm == null || searchTerm.trim().length() < searchConfig.getAutocomplete().getMinLength()) {
            return List.of();
        }

        double threshold = searchConfig.getSimilarity().getWord();
        int maxResults = searchConfig.getAutocomplete().getMaxResults();

        if (entityType != null && !entityType.isEmpty()) {
            return autocompleteByEntityType(searchTerm, entityType, threshold, maxResults);
        }

        // Autocomplete across all entities
        List<AutocompleteResult> allResults = new ArrayList<>();

        try {
            allResults.addAll(autocompleteAccounts(searchTerm, threshold, maxResults));
        } catch (Exception e) {
            log.error("Error in account autocomplete", e);
        }

        try {
            allResults.addAll(autocompleteDrivers(searchTerm, threshold, maxResults));
        } catch (Exception e) {
            log.error("Error in driver autocomplete", e);
        }

        try {
            allResults.addAll(autocompleteFleets(searchTerm, threshold, maxResults));
        } catch (Exception e) {
            log.error("Error in fleet autocomplete", e);
        }

        // Sort by relevance and limit
        return allResults.stream()
            .sorted(Comparator.comparing(AutocompleteResult::getRelevance).reversed())
            .limit(maxResults)
            .collect(Collectors.toList());
    }

    private List<AutocompleteResult> autocompleteByEntityType(
        String searchTerm, String entityType, double threshold, int maxResults
    ) {
        return switch (entityType.toLowerCase()) {
            case "account", "accounts" -> autocompleteAccounts(searchTerm, threshold, maxResults);
            case "driver", "drivers" -> autocompleteDrivers(searchTerm, threshold, maxResults);
            case "fleet", "fleets" -> autocompleteFleets(searchTerm, threshold, maxResults);
            case "order", "orders" -> autocompleteOrders(searchTerm, threshold, maxResults);
            default -> {
                log.warn("Unknown entity type for autocomplete: {}", entityType);
                yield List.of();
            }
        };
    }

    private List<AutocompleteResult> autocompleteAccounts(String searchTerm, double threshold, int maxResults) {
        List<Account> accounts = accountRepository.autocomplete(searchTerm, threshold, maxResults);
        return accounts.stream()
            .map(account -> AutocompleteResult.builder()
                .entityType("account")
                .entityId(account.getId())
                .text(account.getAccountName())
                .label(account.getAccountName() + " (" + account.getAccountNumber() + ")")
                .relevance(0.0) // Set by database
                .metadata(account.getCompanyName())
                .build())
            .collect(Collectors.toList());
    }

    private List<AutocompleteResult> autocompleteDrivers(String searchTerm, double threshold, int maxResults) {
        List<Driver> drivers = driverRepository.autocomplete(searchTerm, threshold, maxResults);
        return drivers.stream()
            .map(driver -> AutocompleteResult.builder()
                .entityType("driver")
                .entityId(driver.getId())
                .text(driver.getFullName())
                .label(driver.getFullName() + " - " + driver.getLicenseNumber())
                .relevance(0.0) // Set by database
                .metadata("License: " + driver.getLicenseNumber())
                .build())
            .collect(Collectors.toList());
    }

    private List<AutocompleteResult> autocompleteFleets(String searchTerm, double threshold, int maxResults) {
        List<Fleet> fleets = fleetRepository.autocomplete(searchTerm, threshold, maxResults);
        return fleets.stream()
            .map(fleet -> AutocompleteResult.builder()
                .entityType("fleet")
                .entityId(fleet.getId())
                .text(fleet.getMake() + " " + fleet.getModel())
                .label(fleet.getDisplayName())
                .relevance(0.0) // Set by database
                .metadata("VIN: " + fleet.getVin())
                .build())
            .collect(Collectors.toList());
    }

    private List<AutocompleteResult> autocompleteOrders(String searchTerm, double threshold, int maxResults) {
        // For orders, use recent data only (partition pruning)
        java.time.Instant startDate = java.time.Instant.now().minus(
            searchConfig.getGlobalSearch().getDefaultTimeRangeMonths() * 30L,
            java.time.temporal.ChronoUnit.DAYS
        );

        List<Order> orders = orderRepository.autocomplete(searchTerm, startDate, threshold, maxResults);
        return orders.stream()
            .map(order -> AutocompleteResult.builder()
                .entityType("order")
                .entityId(order.getId())
                .text(order.getOrderNumber())
                .label("Order " + order.getOrderNumber())
                .relevance(0.0) // Set by database
                .metadata(order.getStatus().name())
                .build())
            .collect(Collectors.toList());
    }
}
