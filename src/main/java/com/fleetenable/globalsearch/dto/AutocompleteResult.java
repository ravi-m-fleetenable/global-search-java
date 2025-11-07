package com.fleetenable.globalsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Autocomplete result DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutocompleteResult {

    /**
     * Entity type (e.g., "order", "account", "driver", "fleet")
     */
    private String entityType;

    /**
     * Entity ID
     */
    private Long entityId;

    /**
     * Suggested text
     */
    private String text;

    /**
     * Display label
     */
    private String label;

    /**
     * Relevance score (0.0 to 1.0)
     */
    private Double relevance;

    /**
     * Optional metadata (e.g., status, additional context)
     */
    private String metadata;
}
