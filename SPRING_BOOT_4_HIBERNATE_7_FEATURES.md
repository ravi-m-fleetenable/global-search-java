# Spring Boot 4 + Hibernate 7 Advanced Features
## Leveraging Next-Generation Framework Capabilities for Global Search

**Version:** 2.0
**Date:** 2025-11-07
**Framework Versions:**
- Spring Boot 4.0 (GA: November 2025)
- Spring Framework 7.0
- Hibernate ORM 7.0+ (Released: May 19, 2025)
- Jakarta Persistence (JPA) 3.2
- Jakarta Data 1.0

---

## Table of Contents

1. [Executive Overview](#executive-overview)
2. [Technology Stack Update](#technology-stack-update)
3. [Hibernate Data Repositories (Jakarta Data 1.0)](#hibernate-data-repositories-jakarta-data-10)
4. [Type-Safe Criteria API](#type-safe-criteria-api)
5. [JPA 3.2 Advanced Features](#jpa-32-advanced-features)
6. [JSON & PostgreSQL Integration](#json--postgresql-integration)
7. [Vector Search Support](#vector-search-support)
8. [StatelessSession for Bulk Operations](#statelesssession-for-bulk-operations)
9. [Enhanced Null Safety](#enhanced-null-safety)
10. [Performance Optimizations](#performance-optimizations)
11. [Implementation Examples](#implementation-examples)
12. [Migration from Spring Boot 3.x](#migration-from-spring-boot-3x)

---

## 1. Executive Overview

### Why Upgrade to Spring Boot 4 + Hibernate 7?

This update leverages **cutting-edge** framework capabilities released in 2025:

| Feature Category | Benefit | Impact on Global Search |
|-----------------|---------|-------------------------|
| **Compile-Time Type Safety** | Jakarta Data repositories catch errors at compile time | Zero runtime query errors, faster development |
| **Enhanced Criteria API** | Fluent, type-safe query building | Cleaner search code, better maintainability |
| **JPA 3.2 Set Operations** | `union()`, `intersect()`, `except()` in Criteria | Complex cross-entity searches without native SQL |
| **JSON Native Support** | PostgreSQL JSON functions in HQL | Direct JSONB querying for addresses, arrays |
| **Vector Search** | Semantic search capabilities | Future: AI-powered search relevance |
| **StatelessSession Bulk Ops** | Batch insert/update/delete | Faster data migrations, bulk updates |
| **JSpecify Null Safety** | IDE-enforced null checks | Fewer NullPointerExceptions |

### Key Advantages Over Spring Boot 3.x

```
┌──────────────────────────────────────────────────────────────┐
│           Spring Boot 3.5 (Current Spec)                     │
├──────────────────────────────────────────────────────────────┤
│  ✓ Native queries (String-based, error-prone)               │
│  ✓ JPA Criteria (verbose, type-unsafe in places)            │
│  ✓ Spring Data JPA (limited compile-time checking)          │
│  ✗ No Jakarta Data support                                  │
│  ✗ Limited JSON function support                            │
│  ✗ No vector search integration                             │
└──────────────────────────────────────────────────────────────┘

                              ⬇️ UPGRADE ⬇️

┌──────────────────────────────────────────────────────────────┐
│           Spring Boot 4.0 + Hibernate 7                      │
├──────────────────────────────────────────────────────────────┤
│  ✓ Jakarta Data repositories (compile-time validated)       │
│  ✓ New fluent Criteria API (fully type-safe)                │
│  ✓ JPA 3.2 set operations (union/intersect/except)          │
│  ✓ Native JSON/XML functions in HQL                          │
│  ✓ Vector search via Spring Data JPA 4                      │
│  ✓ Enhanced StatelessSession with batch operations          │
│  ✓ JSpecify null safety annotations                         │
└──────────────────────────────────────────────────────────────┘
```

---

## 2. Technology Stack Update

### Updated Dependencies (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.0</version> <!-- Spring Boot 4 GA: Nov 2025 -->
        <relativePath/>
    </parent>

    <groupId>com.fleetenable</groupId>
    <artifactId>global-search</artifactId>
    <version>2.0.0-SNAPSHOT</version>
    <name>Global Search API - Spring Boot 4</name>

    <properties>
        <java.version>25</java.version>
        <maven.compiler.source>25</maven.compiler.source>
        <maven.compiler.target>25</maven.compiler.target>
        <hibernate.version>7.1.0.Final</hibernate.version>
        <spring-data.version>4.0.0</spring-data.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.7</version>
        </dependency>

        <!-- Hibernate ORM 7 -->
        <dependency>
            <groupId>org.hibernate.orm</groupId>
            <artifactId>hibernate-core</artifactId>
            <version>${hibernate.version}</version>
        </dependency>

        <!-- NEW: Hibernate Data Repositories (Jakarta Data 1.0) -->
        <dependency>
            <groupId>org.hibernate.orm</groupId>
            <artifactId>hibernate-repositories</artifactId>
            <version>${hibernate.version}</version>
        </dependency>

        <!-- NEW: Hibernate Vector Support for PostgreSQL -->
        <dependency>
            <groupId>org.hibernate.orm</groupId>
            <artifactId>hibernate-vector</artifactId>
            <version>${hibernate.version}</version>
        </dependency>

        <!-- NEW: Hibernate JPA Metamodel Generator (compile-time) -->
        <dependency>
            <groupId>org.hibernate.orm</groupId>
            <artifactId>hibernate-jpamodelgen</artifactId>
            <version>${hibernate.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Flyway for Database Migrations -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
            <version>10.21.0</version>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
            <version>10.21.0</version>
        </dependency>

        <!-- Redis Cache -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- JWT Authentication -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>

        <!-- NEW: JSpecify Annotations (Null Safety) -->
        <dependency>
            <groupId>org.jspecify</groupId>
            <artifactId>jspecify</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- Lombok (optional, for cleaner code) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.36</version>
            <scope>provided</scope>
        </dependency>

        <!-- Monitoring & Observability -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>1.20.4</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>1.20.4</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <source>25</source>
                    <target>25</target>
                    <!-- Enable annotation processing for JPA metamodel -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.hibernate.orm</groupId>
                            <artifactId>hibernate-jpamodelgen</artifactId>
                            <version>${hibernate.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.36</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Application Configuration (application.yml)

```yaml
spring:
  application:
    name: global-search-api

  datasource:
    url: jdbc:postgresql://localhost:5432/global_search
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    # NEW: Hibernate 7 uses Jakarta Persistence 3.2
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # Use Flyway for schema management
    properties:
      hibernate:
        # Enable Hibernate 7 features
        dialect: org.hibernate.dialect.PostgreSQLDialect
        show_sql: false
        format_sql: true
        use_sql_comments: true

        # NEW: Enable Jakarta Data repositories
        data:
          repositories:
            enabled: true

        # NEW: Enable vector support for pgvector
        vector:
          enabled: true

        # Query optimization
        query:
          fail_on_pagination_over_collection_fetch: true
          in_clause_parameter_padding: true

        # Connection management
        connection:
          provider_disables_autocommit: true

        # Statistics for monitoring
        generate_statistics: true

        # JSONB support
        jdbc:
          lob:
            non_contextual_creation: true

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutes

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

# Search Configuration
search:
  similarity:
    global: 0.3      # pg_trgm.similarity_threshold
    word: 0.6        # pg_trgm.word_similarity_threshold
  autocomplete:
    min-length: 2
    max-results: 10
  global-search:
    max-results: 100
  pagination:
    default-size: 20
    max-size: 100

# Logging
logging:
  level:
    root: INFO
    com.fleetenable: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
    org.springframework.data.jpa: DEBUG
```

---

## 3. Hibernate Data Repositories (Jakarta Data 1.0)

### What is Jakarta Data?

**Jakarta Data 1.0** provides a compile-time validated repository abstraction. Unlike Spring Data JPA (runtime validation), **Jakarta Data catches errors at compile time**.

### Key Benefits

| Feature | Spring Data JPA (Runtime) | Hibernate Data Repositories (Compile-Time) |
|---------|---------------------------|---------------------------------------------|
| **Query Validation** | Runtime (test execution) | Compile-time (during build) |
| **Type Safety** | Partial (method names) | Full (HQL syntax + types) |
| **IDE Support** | Limited | Full autocomplete, error highlighting |
| **Performance** | Reflection-based | Generated code (faster) |
| **Refactoring Safety** | Manual testing required | Compiler errors on breaking changes |

### Implementation Example

#### Define the Entity

```java
package com.fleetenable.globalsearch.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String orderNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String[] hawbNumbers;  // Array stored as JSONB

    @Column(length = 50, nullable = false)
    private String status;  // pending, confirmed, in_transit, delivered, etc.

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Address origin;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Address destination;

    @Nullable
    @Column
    private LocalDateTime pickupDate;

    @Nullable
    @Column
    private LocalDateTime deliveryDate;

    @Nullable
    @Column
    private LocalDateTime estimatedDelivery;

    @Column(precision = 10, scale = 2)
    private Double totalWeight;

    @Column(precision = 15, scale = 2)
    private Double totalValue;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fleet_id")
    private Fleet fleet;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_dispatcher_id")
    private User assignedDispatcher;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Embedded Address class
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String street;
        private String city;
        private String state;
        private String zip;
        private String country;
    }
}
```

#### Generate Static Metamodel

During compilation, Hibernate JPA Metamodel Generator creates `Order_.java`:

```java
package com.fleetenable.globalsearch.model.entity;

import jakarta.persistence.metamodel.*;
import org.jspecify.annotations.Nullable;

@StaticMetamodel(Order.class)
public abstract class Order_ {
    public static volatile SingularAttribute<Order, Long> id;
    public static volatile SingularAttribute<Order, String> orderNumber;
    public static volatile SingularAttribute<Order, String[]> hawbNumbers;
    public static volatile SingularAttribute<Order, String> status;
    public static volatile SingularAttribute<Order, Order.Address> origin;
    public static volatile SingularAttribute<Order, Order.Address> destination;
    public static volatile SingularAttribute<Order, LocalDateTime> pickupDate;
    public static volatile SingularAttribute<Order, LocalDateTime> deliveryDate;
    public static volatile SingularAttribute<Order, LocalDateTime> estimatedDelivery;
    public static volatile SingularAttribute<Order, Double> totalWeight;
    public static volatile SingularAttribute<Order, Double> totalValue;
    public static volatile SingularAttribute<Order, String> notes;
    public static volatile SingularAttribute<Order, Account> account;
    public static volatile SingularAttribute<Order, Driver> driver;
    public static volatile SingularAttribute<Order, Fleet> fleet;
    public static volatile SingularAttribute<Order, User> assignedDispatcher;
    public static volatile SingularAttribute<Order, LocalDateTime> createdAt;
    public static volatile SingularAttribute<Order, LocalDateTime> updatedAt;
}
```

#### Jakarta Data Repository Interface

```java
package com.fleetenable.globalsearch.repository;

import com.fleetenable.globalsearch.model.entity.Order;
import com.fleetenable.globalsearch.model.entity.Order_;
import jakarta.data.repository.*;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Jakarta Data repository for Order entities.
 *
 * Queries are VALIDATED AT COMPILE TIME:
 * - HQL syntax errors → Compiler error
 * - Type mismatches → Compiler error
 * - Invalid field names → Compiler error
 */
@Repository
public interface OrderRepository extends DataRepository<Order, Long> {

    // DERIVED QUERIES (automatically implemented by Hibernate)

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByStatus(String status);

    List<Order> findByStatusIn(List<String> statuses);

    List<Order> findByAccountId(Long accountId);

    // Pagination and sorting (type-safe with static metamodel)
    List<Order> findByStatus(String status, Sort<Order> sort);

    Page<Order> findByStatus(String status, Pageable pageable);

    // EXPLICIT HQL QUERIES (compile-time validated!)

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt > :since")
    List<Order> findRecentOrdersByStatus(String status, LocalDateTime since);

    @Query("""
        SELECT o FROM Order o
        WHERE LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(o.notes) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY o.createdAt DESC
        """)
    List<Order> searchOrders(String query);

    // FUZZY SEARCH with PostgreSQL pg_trgm (compile-time validated!)
    @Query(value = """
        SELECT o.* FROM orders o
        WHERE similarity(LOWER(o.order_number), LOWER(:query)) > :threshold
           OR similarity(LOWER(COALESCE(o.notes, '')), LOWER(:query)) > :threshold
        ORDER BY similarity(LOWER(o.order_number), LOWER(:query)) DESC
        """, nativeQuery = true)
    List<Order> fuzzySearchOrders(String query, double threshold);

    // COUNT queries
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(String status);

    // PROJECTIONS (DTO results)
    @Query("SELECT o.orderNumber, o.status, o.createdAt FROM Order o WHERE o.account.id = :accountId")
    List<OrderSummary> findOrderSummariesByAccount(Long accountId);

    // NAMED PARAMETERS with null safety
    @Query("""
        SELECT o FROM Order o
        WHERE o.status = :status
          AND (:driverId IS NULL OR o.driver.id = :driverId)
          AND (:fleetId IS NULL OR o.fleet.id = :fleetId)
        ORDER BY o.createdAt DESC
        """)
    List<Order> findOrdersFiltered(String status, @Nullable Long driverId, @Nullable Long fleetId);

    // UPDATE queries (compile-time validated!)
    @Query("UPDATE Order o SET o.status = :newStatus WHERE o.id = :orderId")
    int updateOrderStatus(Long orderId, String newStatus);

    // DELETE queries
    @Query("DELETE FROM Order o WHERE o.status = :status AND o.createdAt < :before")
    int deleteOldOrders(String status, LocalDateTime before);

    // Projection interface for DTOs
    interface OrderSummary {
        String getOrderNumber();
        String getStatus();
        LocalDateTime getCreatedAt();
    }
}
```

**Key Advantage:** The `@Query` annotations are **validated at compile time**. If you write:
```java
@Query("SELECT o FROM Ordar o WHERE o.statuss = :status")  // Typos!
```

You get a **compiler error immediately**:
```
Error: Unresolved entity reference 'Ordar' [line 1, column 15]
Error: Field 'statuss' not found in entity 'Order' [line 1, column 34]
```

---

## 4. Type-Safe Criteria API

### Hibernate 7 New Criteria API

Hibernate 7 introduces **`SelectionSpecification`** - a fluent, fully type-safe alternative to the verbose JPA Criteria API.

### Example: Fuzzy Search with Type Safety

```java
package com.fleetenable.globalsearch.service;

import com.fleetenable.globalsearch.model.entity.*;
import jakarta.persistence.EntityManager;
import org.hibernate.query.criteria.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdvancedSearchService {

    private final EntityManager entityManager;

    public AdvancedSearchService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Type-safe fuzzy search using Hibernate 7 SelectionSpecification API
     */
    public List<Order> fuzzySearchOrders(String query, String status) {
        // Create type-safe selection specification
        var orders = SelectionSpecification.create(Order.class,
            "FROM Order WHERE status = :status")
            .parameter("status", status)

            // Add similarity restriction (compile-time type-safe!)
            .restrict(Restriction.custom(
                "similarity(LOWER(orderNumber), LOWER(:query)) > 0.3",
                "query", query
            ))

            // Sort by relevance (type-safe with static metamodel)
            .sort(Order.desc(
                "similarity(LOWER(orderNumber), LOWER(:query))"
            ))

            // Fetch related entities (avoid N+1)
            .fetch(Path.from(Order.class)
                .to(Order_.account))
            .fetch(Path.from(Order.class)
                .to(Order_.driver))

            // Create query and execute
            .createQuery(entityManager)
            .setPage(Page.first(50))
            .getResultList();

        return orders;
    }

    /**
     * Multi-attribute search with type-safe criteria
     */
    public List<Account> searchAccounts(String query, String accountType) {
        var accounts = SelectionSpecification.create(Account.class,
            """
            FROM Account
            WHERE accountType = :accountType
              AND (
                similarity(LOWER(accountNumber), LOWER(:query)) > 0.3 OR
                similarity(LOWER(accountName), LOWER(:query)) > 0.3 OR
                similarity(LOWER(companyName), LOWER(:query)) > 0.3
              )
            """)
            .parameter("accountType", accountType)
            .parameter("query", query)
            .sort(Order.desc(
                "GREATEST(" +
                "  similarity(LOWER(accountNumber), LOWER(:query))," +
                "  similarity(LOWER(accountName), LOWER(:query))," +
                "  similarity(LOWER(companyName), LOWER(:query))" +
                ")"
            ))
            .createQuery(entityManager)
            .getResultList();

        return accounts;
    }

    /**
     * Complex cross-entity search using JPA 3.2 UNION
     */
    public List<SearchResult> globalSearch(String query) {
        var cb = entityManager.getCriteriaBuilder();

        // Query 1: Search orders
        var orderQuery = cb.createQuery(SearchResult.class);
        var orderRoot = orderQuery.from(Order.class);
        orderQuery.select(cb.construct(SearchResult.class,
            cb.literal("order"),
            orderRoot.get(Order_.id).as(String.class),
            orderRoot.get(Order_.orderNumber),
            cb.function("similarity", Double.class,
                cb.lower(orderRoot.get(Order_.orderNumber)),
                cb.literal(query.toLowerCase())
            )
        ));

        // Query 2: Search accounts
        var accountQuery = cb.createQuery(SearchResult.class);
        var accountRoot = accountQuery.from(Account.class);
        accountQuery.select(cb.construct(SearchResult.class,
            cb.literal("account"),
            accountRoot.get(Account_.id).as(String.class),
            accountRoot.get(Account_.accountName),
            cb.function("similarity", Double.class,
                cb.lower(accountRoot.get(Account_.accountName)),
                cb.literal(query.toLowerCase())
            )
        ));

        // NEW in JPA 3.2: UNION operator
        var unionQuery = orderQuery.union(accountQuery);

        // Execute combined query
        return entityManager.createQuery(unionQuery)
            .setMaxResults(100)
            .getResultList();
    }
}
```

---

## 5. JPA 3.2 Advanced Features

### New Set Operations: UNION, INTERSECT, EXCEPT

```java
package com.fleetenable.globalsearch.service;

import com.fleetenable.globalsearch.model.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SetOperationSearchService {

    private final EntityManager entityManager;

    public SetOperationSearchService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * UNION: Combine results from multiple entity searches
     * NEW in JPA 3.2!
     */
    public List<String> searchAllIdentifiers(String query) {
        var cb = entityManager.getCriteriaBuilder();

        // Search orders by order number
        var orderQuery = cb.createQuery(String.class);
        var orderRoot = orderQuery.from(Order.class);
        orderQuery.select(orderRoot.get(Order_.orderNumber))
            .where(cb.like(
                cb.lower(orderRoot.get(Order_.orderNumber)),
                "%" + query.toLowerCase() + "%"
            ));

        // Search accounts by account number
        var accountQuery = cb.createQuery(String.class);
        var accountRoot = accountQuery.from(Account.class);
        accountQuery.select(accountRoot.get(Account_.accountNumber))
            .where(cb.like(
                cb.lower(accountRoot.get(Account_.accountNumber)),
                "%" + query.toLowerCase() + "%"
            ));

        // Search fleets by VIN
        var fleetQuery = cb.createQuery(String.class);
        var fleetRoot = fleetQuery.from(Fleet.class);
        fleetQuery.select(fleetRoot.get(Fleet_.vin))
            .where(cb.like(
                cb.lower(fleetRoot.get(Fleet_.vin)),
                "%" + query.toLowerCase() + "%"
            ));

        // UNION all results (removes duplicates)
        var unionQuery = orderQuery
            .union(accountQuery)
            .union(fleetQuery);

        return entityManager.createQuery(unionQuery).getResultList();
    }

    /**
     * INTERSECT: Find entities matching multiple criteria
     * NEW in JPA 3.2!
     */
    public List<Long> findOrdersInBothSets(
        String status1, String status2,
        Long accountId
    ) {
        var cb = entityManager.getCriteriaBuilder();

        // Query 1: Orders with status1 for accountId
        var query1 = cb.createQuery(Long.class);
        var root1 = query1.from(Order.class);
        query1.select(root1.get(Order_.id))
            .where(cb.and(
                cb.equal(root1.get(Order_.status), status1),
                cb.equal(root1.get(Order_.account).get(Account_.id), accountId)
            ));

        // Query 2: Orders with status2 for accountId
        var query2 = cb.createQuery(Long.class);
        var root2 = query2.from(Order.class);
        query2.select(root2.get(Order_.id))
            .where(cb.and(
                cb.equal(root2.get(Order_.status), status2),
                cb.equal(root2.get(Order_.account).get(Account_.id), accountId)
            ));

        // INTERSECT (find orders that match both conditions)
        var intersectQuery = query1.intersect(query2);

        return entityManager.createQuery(intersectQuery).getResultList();
    }

    /**
     * EXCEPT: Find entities in first set but not in second
     * NEW in JPA 3.2!
     */
    public List<Long> findOrdersNotDelivered(Long accountId) {
        var cb = entityManager.getCriteriaBuilder();

        // Query 1: All orders for account
        var allOrdersQuery = cb.createQuery(Long.class);
        var allRoot = allOrdersQuery.from(Order.class);
        allOrdersQuery.select(allRoot.get(Order_.id))
            .where(cb.equal(
                allRoot.get(Order_.account).get(Account_.id),
                accountId
            ));

        // Query 2: Delivered orders
        var deliveredQuery = cb.createQuery(Long.class);
        var deliveredRoot = deliveredQuery.from(Order.class);
        deliveredQuery.select(deliveredRoot.get(Order_.id))
            .where(cb.and(
                cb.equal(
                    deliveredRoot.get(Order_.account).get(Account_.id),
                    accountId
                ),
                cb.equal(deliveredRoot.get(Order_.status), "delivered")
            ));

        // EXCEPT (all orders EXCEPT delivered ones)
        var exceptQuery = allOrdersQuery.except(deliveredQuery);

        return entityManager.createQuery(exceptQuery).getResultList();
    }
}
```

### Enhanced EntityGraph API

```java
@Service
public class EntityGraphSearchService {

    private final EntityManager entityManager;

    public EntityGraphSearchService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Use enhanced EntityGraph to optimize fetching
     * Improved in JPA 3.2!
     */
    public List<Order> findOrdersWithDetails(String status) {
        // Create dynamic entity graph
        EntityGraph<Order> graph = entityManager.createEntityGraph(Order.class);

        // Fetch associations in single query (avoid N+1)
        graph.addAttributeNodes(
            Order_.account,
            Order_.driver,
            Order_.fleet,
            Order_.assignedDispatcher
        );

        // Nested graph for account details
        Subgraph<Account> accountSubgraph = graph.addSubgraph(Order_.account);
        accountSubgraph.addAttributeNodes(
            Account_.accountName,
            Account_.companyName,
            Account_.address
        );

        // Create query with entity graph hint
        return entityManager.createQuery(
            "SELECT o FROM Order o WHERE o.status = :status",
            Order.class
        )
        .setParameter("status", status)
        .setHint("jakarta.persistence.fetchgraph", graph)
        .getResultList();
    }
}
```

---

## 6. JSON & PostgreSQL Integration

### Native JSON Functions in HQL

Hibernate 7 supports PostgreSQL JSONB functions **directly in HQL**:

```java
package com.fleetenable.globalsearch.service;

import com.fleetenable.globalsearch.model.entity.Order;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JsonSearchService {

    private final EntityManager entityManager;

    public JsonSearchService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Search within JSONB array (HAWB numbers)
     * NEW: Native JSON functions in HQL!
     */
    public List<Order> findOrdersByHawbNumber(String hawbNumber) {
        return entityManager.createQuery("""
            SELECT o FROM Order o
            WHERE jsonb_array_contains(o.hawbNumbers, :hawb) = true
            """, Order.class)
            .setParameter("hawb", hawbNumber)
            .getResultList();
    }

    /**
     * Search within JSONB object (Address)
     */
    public List<Order> findOrdersByCity(String city) {
        return entityManager.createQuery("""
            SELECT o FROM Order o
            WHERE json_extract(o.origin, '$.city') = :city
               OR json_extract(o.destination, '$.city') = :city
            """, Order.class)
            .setParameter("city", city)
            .getResultList();
    }

    /**
     * Query nested JSONB with operators
     */
    public List<Order> findOrdersByStateAndZip(String state, String zip) {
        return entityManager.createQuery("""
            SELECT o FROM Order o
            WHERE json_extract(o.destination, '$.state') = :state
              AND json_extract(o.destination, '$.zip') = :zip
            """, Order.class)
            .setParameter("state", state)
            .setParameter("zip", zip)
            .getResultList();
    }

    /**
     * JSON containment queries
     */
    public List<Account> findAccountsByAddressComponents(
        String city, String state
    ) {
        return entityManager.createQuery("""
            SELECT a FROM Account a
            WHERE jsonb_contains(
                a.address,
                jsonb_object('city', :city, 'state', :state)
            ) = true
            """, Account.class)
            .setParameter("city", city)
            .setParameter("state", state)
            .getResultList();
    }
}
```

### JSONB Index Support

```sql
-- Create GIN index on JSONB columns for fast queries
CREATE INDEX idx_orders_hawb_gin ON orders USING GIN (hawb_numbers);
CREATE INDEX idx_orders_origin_gin ON orders USING GIN (origin);
CREATE INDEX idx_orders_destination_gin ON orders USING GIN (destination);
CREATE INDEX idx_accounts_address_gin ON accounts USING GIN (address);

-- Query performance: O(log n) instead of O(n)
```

---

## 7. Vector Search Support

### Enable Semantic Search with PostgreSQL pgvector

Spring Data JPA 4 + Hibernate 7 **natively support** vector embeddings for **AI-powered semantic search**.

### Setup pgvector Extension

```sql
-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Add vector column to orders table
ALTER TABLE orders ADD COLUMN embedding vector(1536);  -- OpenAI embedding size

-- Create HNSW index for fast similarity search
CREATE INDEX ON orders USING hnsw (embedding vector_cosine_ops);
```

### Entity with Vector Embedding

```java
package com.fleetenable.globalsearch.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // NEW: Vector embedding for semantic search
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;

    // ... other fields
}
```

### Vector Search Repository

```java
package com.fleetenable.globalsearch.repository;

import com.fleetenable.globalsearch.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VectorSearchRepository extends JpaRepository<Order, Long> {

    /**
     * Semantic search using cosine similarity
     * NEW: Hibernate 7 vector support!
     */
    @Query(value = """
        SELECT *,
               1 - (embedding <=> CAST(:queryEmbedding AS vector)) AS similarity
        FROM orders
        WHERE 1 - (embedding <=> CAST(:queryEmbedding AS vector)) > :threshold
        ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Order> semanticSearch(
        @Param("queryEmbedding") float[] queryEmbedding,
        @Param("threshold") double threshold,
        @Param("limit") int limit
    );

    /**
     * Hybrid search: Combine keyword and semantic search
     */
    @Query(value = """
        SELECT *,
               similarity(LOWER(order_number), LOWER(:keyword)) AS keyword_score,
               1 - (embedding <=> CAST(:embedding AS vector)) AS semantic_score,
               (
                 0.3 * similarity(LOWER(order_number), LOWER(:keyword)) +
                 0.7 * (1 - (embedding <=> CAST(:embedding AS vector)))
               ) AS combined_score
        FROM orders
        WHERE similarity(LOWER(order_number), LOWER(:keyword)) > 0.1
           OR (1 - (embedding <=> CAST(:embedding AS vector))) > 0.5
        ORDER BY combined_score DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Order> hybridSearch(
        @Param("keyword") String keyword,
        @Param("embedding") float[] embedding,
        @Param("limit") int limit
    );
}
```

### Generate Embeddings (Example with Spring AI)

```java
package com.fleetenable.globalsearch.service;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private final EmbeddingClient embeddingClient;

    public EmbeddingService(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    /**
     * Generate vector embedding for text
     */
    public float[] generateEmbedding(String text) {
        var embedding = embeddingClient.embed(text);
        return embedding.stream()
            .map(Double::floatValue)
            .toArray(float[]::new);
    }

    /**
     * Update order embedding when created/updated
     */
    public void updateOrderEmbedding(Order order) {
        String searchableText = String.join(" ",
            order.getOrderNumber(),
            order.getNotes() != null ? order.getNotes() : ""
        );

        float[] embedding = generateEmbedding(searchableText);
        order.setEmbedding(embedding);
    }
}
```

---

## 8. StatelessSession for Bulk Operations

### Enhanced StatelessSession in Hibernate 7

```java
package com.fleetenable.globalsearch.service;

import com.fleetenable.globalsearch.model.entity.Order;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BulkOperationService {

    private final SessionFactory sessionFactory;

    public BulkOperationService(EntityManagerFactory emf) {
        this.sessionFactory = emf.unwrap(SessionFactory.class);
    }

    /**
     * NEW in Hibernate 7: Batch insert with StatelessSession
     */
    public void bulkInsertOrders(List<Order> orders) {
        try (StatelessSession session = sessionFactory.openStatelessSession()) {
            session.getTransaction().begin();

            // NEW: insertMultiple() for batch operations
            session.insertMultiple(orders);

            session.getTransaction().commit();
        }
    }

    /**
     * NEW in Hibernate 7: Batch update with StatelessSession
     */
    public void bulkUpdateOrderStatus(List<Order> orders, String newStatus) {
        try (StatelessSession session = sessionFactory.openStatelessSession()) {
            session.getTransaction().begin();

            orders.forEach(order -> order.setStatus(newStatus));

            // NEW: updateMultiple() for batch operations
            session.updateMultiple(orders);

            session.getTransaction().commit();
        }
    }

    /**
     * NEW in Hibernate 7: Batch delete with StatelessSession
     */
    public void bulkDeleteOrders(List<Order> orders) {
        try (StatelessSession session = sessionFactory.openStatelessSession()) {
            session.getTransaction().begin();

            // NEW: deleteMultiple() for batch operations
            session.deleteMultiple(orders);

            session.getTransaction().commit();
        }
    }

    /**
     * High-performance data migration using StatelessSession
     */
    public void migrateOrdersFromMongoDB(List<MongoOrder> mongoOrders) {
        List<Order> orders = mongoOrders.stream()
            .map(this::convertToJpaOrder)
            .toList();

        // Use StatelessSession for faster bulk insert (no caching, no dirty checking)
        try (StatelessSession session = sessionFactory.openStatelessSession()) {
            session.getTransaction().begin();

            // Process in batches of 1000
            int batchSize = 1000;
            for (int i = 0; i < orders.size(); i += batchSize) {
                int end = Math.min(i + batchSize, orders.size());
                List<Order> batch = orders.subList(i, end);

                session.insertMultiple(batch);

                // Flush periodically
                if ((i / batchSize) % 10 == 0) {
                    session.getTransaction().commit();
                    session.getTransaction().begin();
                }
            }

            session.getTransaction().commit();
        }
    }

    private Order convertToJpaOrder(MongoOrder mongoOrder) {
        // Conversion logic
        return null;
    }
}
```

---

## 9. Enhanced Null Safety

### JSpecify Annotations in Spring Framework 7

```java
package com.fleetenable.globalsearch.service;

import com.fleetenable.globalsearch.model.entity.Order;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NullSafeSearchService {

    private final OrderRepository orderRepository;

    public NullSafeSearchService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Method signature with JSpecify annotations
     * IDE will warn about null-related bugs!
     */
    public @NonNull List<Order> searchOrders(
        @NonNull String query,
        @Nullable String status,
        @Nullable Long accountId
    ) {
        // query is guaranteed non-null (compiler/IDE checks)
        if (query.isEmpty()) {  // Safe - no NPE possible
            return List.of();
        }

        // status and accountId can be null
        if (status != null && accountId != null) {
            return orderRepository.findOrdersFiltered(status, null, null);
        } else if (status != null) {
            return orderRepository.findByStatus(status);
        } else {
            return orderRepository.searchOrders(query);
        }
    }

    /**
     * Return type with null safety
     */
    public @NonNull Optional<Order> findOrderByNumber(@NonNull String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    /**
     * Method that explicitly allows null return (discouraged)
     */
    public @Nullable Order findOrderOrNull(@NonNull String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber).orElse(null);
    }
}
```

### Benefits

- **IDE Integration:** IntelliJ IDEA, Eclipse, VS Code highlight potential NPEs
- **Compile-Time Safety:** Optional compilation with `-Xlint:nullness` flag
- **Better Documentation:** Method signatures clearly indicate nullable parameters
- **Fewer Runtime Errors:** Catch null-related bugs during development

---

## 10. Performance Optimizations

### Multiple TaskDecorator Support (Spring Boot 4)

```java
package com.fleetenable.globalsearch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfiguration {

    /**
     * NEW in Spring Boot 4: Multiple TaskDecorators!
     * No need for manual chaining anymore
     */
    @Bean
    public TaskDecorator tracingDecorator() {
        return runnable -> {
            // Add tracing context
            String traceId = MDC.get("traceId");
            return () -> {
                MDC.put("traceId", traceId);
                try {
                    runnable.run();
                } finally {
                    MDC.remove("traceId");
                }
            };
        };
    }

    @Bean
    public TaskDecorator loggingDecorator() {
        return runnable -> {
            // Add logging context
            return () -> {
                log.info("Executing async task");
                runnable.run();
                log.info("Async task completed");
            };
        };
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutor(
        List<TaskDecorator> decorators  // Auto-injected!
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("search-async-");

        // NEW: Spring Boot 4 automatically chains decorators
        executor.setTaskDecorators(decorators);

        executor.initialize();
        return executor;
    }
}
```

### Native Image Support (GraalVM 24)

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <version>0.10.3</version>
    <configuration>
        <imageName>global-search-api</imageName>
        <mainClass>com.fleetenable.globalsearch.GlobalSearchApplication</mainClass>
        <buildArgs>
            <buildArg>--no-fallback</buildArg>
            <buildArg>-H:+ReportExceptionStackTraces</buildArg>
        </buildArgs>
    </configuration>
</plugin>
```

Build native image:
```bash
mvn -Pnative native:compile
```

**Benefits:**
- **Instant startup:** < 100ms (vs. 5-10 seconds JVM)
- **Low memory footprint:** 50-70% reduction
- **Faster peak performance**

---

## 11. Implementation Examples

### Complete Search Service with All Features

```java
package com.fleetenable.globalsearch.service;

import com.fleetenable.globalsearch.model.entity.*;
import com.fleetenable.globalsearch.repository.*;
import jakarta.persistence.EntityManager;
import org.hibernate.query.criteria.*;
import org.jspecify.annotations.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class GlobalSearchService {

    private final EntityManager entityManager;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final DriverRepository driverRepository;
    private final FleetRepository fleetRepository;
    private final VectorSearchRepository vectorSearchRepository;
    private final EmbeddingService embeddingService;

    public GlobalSearchService(
        EntityManager entityManager,
        OrderRepository orderRepository,
        AccountRepository accountRepository,
        DriverRepository driverRepository,
        FleetRepository fleetRepository,
        VectorSearchRepository vectorSearchRepository,
        EmbeddingService embeddingService
    ) {
        this.entityManager = entityManager;
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
        this.driverRepository = driverRepository;
        this.fleetRepository = fleetRepository;
        this.vectorSearchRepository = vectorSearchRepository;
        this.embeddingService = embeddingService;
    }

    /**
     * Global fuzzy search across all entities
     * Uses: Hibernate Data Repositories + pg_trgm
     */
    public @NonNull GlobalSearchResponse globalSearch(
        @NonNull String query,
        @Nullable String entityType,
        @Nullable Set<String> statuses,
        @NonNull Pageable pageable
    ) {
        if (entityType != null) {
            return searchSingleEntityType(query, entityType, statuses, pageable);
        }

        // Search across all entity types using UNION (JPA 3.2)
        List<GlobalSearchResult> results = performGlobalSearch(query, statuses);

        // Sort by relevance
        results.sort(Comparator.comparing(GlobalSearchResult::relevance).reversed());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), results.size());
        List<GlobalSearchResult> page = results.subList(start, end);

        return GlobalSearchResponse.builder()
            .results(page)
            .totalResults(results.size())
            .currentPage(pageable.getPageNumber())
            .totalPages((int) Math.ceil((double) results.size() / pageable.getPageSize()))
            .build();
    }

    /**
     * Autocomplete with word_similarity
     * Uses: Jakarta Data repository + PostgreSQL pg_trgm
     */
    @Cacheable(value = "autocomplete", key = "#prefix + '-' + #entityType")
    public @NonNull List<AutocompleteResult> autocomplete(
        @NonNull String prefix,
        @NonNull String entityType
    ) {
        return switch (entityType.toLowerCase()) {
            case "order" -> orderRepository.autocompleteOrderNumber(prefix);
            case "account" -> accountRepository.autocompleteAccountNumber(prefix);
            case "driver" -> driverRepository.autocompleteDriverName(prefix);
            case "fleet" -> fleetRepository.autocompleteVehicle(prefix);
            default -> List.of();
        };
    }

    /**
     * Hybrid search: Keyword + Semantic (Vector)
     * Uses: Hibernate 7 vector support + pg_trgm
     */
    public @NonNull List<Order> hybridSearch(
        @NonNull String query,
        double keywordWeight,
        double semanticWeight
    ) {
        // Generate embedding for semantic search
        float[] queryEmbedding = embeddingService.generateEmbedding(query);

        // Perform hybrid search (keyword + vector)
        return vectorSearchRepository.hybridSearch(
            query,
            queryEmbedding,
            100  // limit
        );
    }

    /**
     * Faceted search with aggregations
     * Uses: JPA 3.2 Criteria API
     */
    @Cacheable(value = "facets", key = "#entityType")
    public @NonNull Map<String, Map<String, Long>> getFacets(
        @NonNull String entityType
    ) {
        Map<String, Map<String, Long>> facets = new HashMap<>();

        if ("order".equals(entityType)) {
            // Status facet
            var statusFacet = entityManager.createQuery(
                "SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status",
                Object[].class
            ).getResultList().stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
            facets.put("status", statusFacet);

            // Account type facet
            var accountTypeFacet = entityManager.createQuery(
                "SELECT o.account.accountType, COUNT(o) FROM Order o GROUP BY o.account.accountType",
                Object[].class
            ).getResultList().stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
            facets.put("accountType", accountTypeFacet);
        }

        return facets;
    }

    /**
     * Advanced search with type-safe Criteria API
     * Uses: Hibernate 7 SelectionSpecification
     */
    public @NonNull List<Order> advancedSearch(
        @Nullable String query,
        @Nullable String status,
        @Nullable Long accountId,
        @Nullable Long driverId,
        @Nullable LocalDateRange dateRange
    ) {
        var spec = SelectionSpecification.create(Order.class, "FROM Order o");

        if (query != null) {
            spec = spec.restrict(Restriction.custom(
                "similarity(LOWER(o.orderNumber), LOWER(:query)) > 0.3",
                "query", query
            ));
        }

        if (status != null) {
            spec = spec.restrict(Restriction.equal(Order_.status, status));
        }

        if (accountId != null) {
            spec = spec.restrict(Restriction.equal(
                Path.from(Order.class).to(Order_.account).to(Account_.id),
                accountId
            ));
        }

        if (driverId != null) {
            spec = spec.restrict(Restriction.equal(
                Path.from(Order.class).to(Order_.driver).to(Driver_.id),
                driverId
            ));
        }

        if (dateRange != null) {
            spec = spec.restrict(Restriction.between(
                Order_.createdAt,
                dateRange.start(),
                dateRange.end()
            ));
        }

        // Fetch associations to avoid N+1
        spec = spec
            .fetch(Path.from(Order.class).to(Order_.account))
            .fetch(Path.from(Order.class).to(Order_.driver))
            .sort(Order.desc(Order_.createdAt));

        return spec.createQuery(entityManager)
            .setMaxResults(100)
            .getResultList();
    }

    // Private helper methods
    private GlobalSearchResponse searchSingleEntityType(
        String query,
        String entityType,
        Set<String> statuses,
        Pageable pageable
    ) {
        // Entity-specific search implementation
        return GlobalSearchResponse.builder().build();
    }

    private List<GlobalSearchResult> performGlobalSearch(
        String query,
        Set<String> statuses
    ) {
        // Cross-entity search using UNION
        return List.of();
    }
}
```

---

## 12. Migration from Spring Boot 3.x

### Breaking Changes

| Feature | Spring Boot 3.x | Spring Boot 4.0 | Migration Action |
|---------|----------------|-----------------|------------------|
| **Baseline Java** | JDK 17 | JDK 17 (JDK 25 recommended) | Update `java.version` in pom.xml |
| **Jakarta EE** | Jakarta EE 9/10 | Jakarta EE 11 | Update imports (`javax.*` → `jakarta.*`) |
| **Hibernate** | Hibernate 6.x | Hibernate 7.0 | Update ORM code, test queries |
| **Null Annotations** | JSR 305 (`@Nullable`) | JSpecify (`@Nullable`) | Replace annotations |
| **Servlet API** | Servlet 5.0/6.0 | Servlet 6.1 | Update to Tomcat 11+ / Jetty 12.1+ |

### Migration Steps

1. **Update pom.xml:**
   ```xml
   <parent>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-parent</artifactId>
       <version>4.0.0</version>
   </parent>
   <properties>
       <java.version>25</java.version>
   </properties>
   ```

2. **Replace Null Annotations:**
   ```java
   // Before (Spring Boot 3.x)
   import org.springframework.lang.Nullable;
   import org.springframework.lang.NonNull;

   // After (Spring Boot 4.0)
   import org.jspecify.annotations.Nullable;
   import org.jspecify.annotations.NonNull;
   ```

3. **Add Hibernate 7 Dependencies:**
   ```xml
   <dependency>
       <groupId>org.hibernate.orm</groupId>
       <artifactId>hibernate-repositories</artifactId>
       <version>7.1.0.Final</version>
   </dependency>
   <dependency>
       <groupId>org.hibernate.orm</groupId>
       <artifactId>hibernate-vector</artifactId>
       <version>7.1.0.Final</version>
   </dependency>
   ```

4. **Enable Metamodel Generation:**
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
       <configuration>
           <annotationProcessorPaths>
               <path>
                   <groupId>org.hibernate.orm</groupId>
                   <artifactId>hibernate-jpamodelgen</artifactId>
                   <version>7.1.0.Final</version>
               </path>
           </annotationProcessorPaths>
       </configuration>
   </plugin>
   ```

5. **Update to Tomcat 11:**
   ```xml
   <dependency>
       <groupId>org.apache.tomcat.embed</groupId>
       <artifactId>tomcat-embed-core</artifactId>
       <version>11.0.0</version>
   </dependency>
   ```

6. **Test Thoroughly:**
   - Run all unit tests
   - Run integration tests with Testcontainers
   - Perform load testing
   - Verify query performance

---

## Conclusion

### Summary of Benefits

By upgrading to **Spring Boot 4 + Hibernate 7**, the Global Search implementation gains:

1. **Compile-Time Type Safety:** Jakarta Data repositories catch errors during compilation
2. **Cleaner Code:** Fluent Criteria API with static metamodel
3. **Advanced Queries:** JPA 3.2 set operations (UNION, INTERSECT, EXCEPT)
4. **Native JSON Support:** Query JSONB columns directly in HQL
5. **AI-Powered Search:** Vector embeddings for semantic search
6. **Bulk Performance:** StatelessSession batch operations
7. **Null Safety:** JSpecify annotations prevent NPEs
8. **Future-Proof:** Latest LTS (Java 25, Jakarta EE 11)

### Recommendation

**Proceed with Spring Boot 4 + Hibernate 7** for:
- Maximum type safety and developer productivity
- Future-proof technology stack (2025 releases)
- Advanced search capabilities (vector, JSON, fuzzy)
- Best-in-class performance optimizations

---

**Document Version:** 2.0
**Last Updated:** 2025-11-07
**Framework Versions:** Spring Boot 4.0, Hibernate 7.0+, JPA 3.2, Jakarta Data 1.0
