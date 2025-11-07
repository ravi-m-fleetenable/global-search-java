package com.fleetenable.globalsearch.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Search configuration properties
 *
 * Binds to app.search.* properties in application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "app.search")
@Getter
@Setter
public class SearchConfig {

    private Similarity similarity = new Similarity();
    private Autocomplete autocomplete = new Autocomplete();
    private GlobalSearch globalSearch = new GlobalSearch();

    @Getter
    @Setter
    public static class Similarity {
        /**
         * Global similarity threshold for fuzzy search (0.0 to 1.0)
         * Default: 0.3 (30% similarity)
         */
        private double global = 0.3;

        /**
         * Word similarity threshold for autocomplete (0.0 to 1.0)
         * Default: 0.6 (60% similarity)
         */
        private double word = 0.6;
    }

    @Getter
    @Setter
    public static class Autocomplete {
        /**
         * Minimum search term length for autocomplete
         */
        private int minLength = 2;

        /**
         * Maximum results for autocomplete
         */
        private int maxResults = 10;
    }

    @Getter
    @Setter
    public static class GlobalSearch {
        /**
         * Maximum results for global search
         */
        private int maxResults = 100;

        /**
         * Default time range in months for partition pruning
         */
        private int defaultTimeRangeMonths = 6;
    }
}
