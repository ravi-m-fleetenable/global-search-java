package com.fleetenable.globalsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Global Search API - Production-Grade Spring Boot Application
 *
 * Features:
 * - PostgreSQL pg_trgm fuzzy search
 * - Partitioned tables for scalability
 * - JWT authentication
 * - Role-based authorization
 * - Comprehensive API documentation
 * - Production monitoring with Micrometer
 *
 * @author Fleet Enable Team
 * @version 2.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
@EnableCaching
@EnableAsync
@EnableScheduling
public class GlobalSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlobalSearchApplication.class, args);
    }
}
