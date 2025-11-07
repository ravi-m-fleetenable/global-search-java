package com.fleetenable.globalsearch.controller;

import com.fleetenable.globalsearch.dto.AutocompleteResult;
import com.fleetenable.globalsearch.dto.SearchResult;
import com.fleetenable.globalsearch.service.AutocompleteService;
import com.fleetenable.globalsearch.service.GlobalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Search REST Controller
 *
 * Provides global search and autocomplete endpoints using PostgreSQL pg_trgm
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "Global search and autocomplete APIs")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final GlobalSearchService globalSearchService;
    private final AutocompleteService autocompleteService;

    /**
     * Global search across all entities
     *
     * @param q Search query
     * @param entityType Optional entity type filter (order, account, driver, fleet)
     * @param maxResults Maximum results to return
     * @return List of search results
     */
    @GetMapping("/global")
    @Operation(summary = "Global search", description = "Search across all entities with fuzzy matching")
    public ResponseEntity<List<SearchResult>> globalSearch(
        @Parameter(description = "Search query", required = true)
        @RequestParam String q,

        @Parameter(description = "Entity type filter (optional)")
        @RequestParam(required = false) String entityType,

        @Parameter(description = "Maximum results (default: 100)")
        @RequestParam(required = false) Integer maxResults
    ) {
        log.info("Global search request: q='{}', entityType='{}', maxResults={}", q, entityType, maxResults);

        List<SearchResult> results;
        if (entityType != null && !entityType.isEmpty()) {
            results = globalSearchService.searchByEntityType(q, entityType, maxResults);
        } else {
            results = globalSearchService.searchAll(q, maxResults);
        }

        return ResponseEntity.ok(results);
    }

    /**
     * Autocomplete endpoint
     *
     * @param q Partial search query
     * @param entityType Optional entity type filter
     * @return List of autocomplete suggestions
     */
    @GetMapping("/autocomplete")
    @Operation(summary = "Autocomplete", description = "Fast prefix matching for search suggestions")
    public ResponseEntity<List<AutocompleteResult>> autocomplete(
        @Parameter(description = "Partial search query", required = true)
        @RequestParam String q,

        @Parameter(description = "Entity type filter (optional)")
        @RequestParam(required = false) String entityType
    ) {
        log.info("Autocomplete request: q='{}', entityType='{}'", q, entityType);

        List<AutocompleteResult> results = autocompleteService.autocomplete(q, entityType);
        return ResponseEntity.ok(results);
    }

    /**
     * Health check for search functionality
     */
    @GetMapping("/health")
    @Operation(summary = "Search health check", description = "Verify search services are operational")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Search services operational");
    }
}
