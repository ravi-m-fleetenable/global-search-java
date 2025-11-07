package com.fleetenable.globalsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Generic search result DTO for global search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult {

    /**
     * Entity type (e.g., "order", "account", "driver", "fleet")
     */
    private String entityType;

    /**
     * Entity ID
     */
    private Long entityId;

    /**
     * Primary identifier (e.g., order number, account number, VIN)
     */
    private String primaryIdentifier;

    /**
     * Display name or title
     */
    private String displayName;

    /**
     * Secondary information
     */
    private String secondaryInfo;

    /**
     * Relevance score (0.0 to 1.0)
     */
    private Double relevance;

    /**
     * Created timestamp
     */
    private Instant createdAt;

    /**
     * Status
     */
    private String status;

    /**
     * Optional highlighted text
     */
    private String highlightedText;

    /**
     * Optional metadata as JSON string
     */
    private String metadata;
}
