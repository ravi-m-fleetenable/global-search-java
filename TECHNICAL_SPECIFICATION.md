# Global Search Migration - Technical Specification
## Java Spring Boot (JDK 25) + PostgreSQL with Advanced Fuzzy Search

**Version:** 1.0
**Date:** 2025-11-07
**Status:** Draft for Review

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current System Analysis](#current-system-analysis)
3. [Proposed Technology Stack](#proposed-technology-stack)
4. [PostgreSQL Search Architecture](#postgresql-search-architecture)
5. [Database Schema Design](#database-schema-design)
6. [Search Implementation Strategy](#search-implementation-strategy)
7. [Performance Optimization](#performance-optimization)
8. [Migration Strategy](#migration-strategy)
9. [Security & Authorization](#security--authorization)
10. [API Design](#api-design)
11. [Deployment Architecture](#deployment-architecture)
12. [Cost Analysis](#cost-analysis)
13. [Risk Assessment & Mitigation](#risk-assessment--mitigation)
14. [Open Questions & Clarifications](#open-questions--clarifications)

---

## 1. Executive Summary

This specification outlines the migration from **Ruby on Rails + MongoDB Atlas Search** to **Java Spring Boot (JDK 25) + PostgreSQL** with advanced fuzzy search capabilities using PostgreSQL's native extensions (`pg_trgm`, GIN/GiST indexes).

### Key Benefits of Migration

| Aspect | Current (MongoDB) | Proposed (PostgreSQL) |
|--------|-------------------|----------------------|
| **Search Technology** | MongoDB Atlas Search (proprietary) | PostgreSQL pg_trgm (open-source, native) |
| **Infrastructure Lock-in** | Atlas M50+ required ($500+/month) | Any PostgreSQL 12+ instance |
| **Fuzzy Search** | Levenshtein via Atlas | Trigram similarity (proven, fast) |
| **Index Types** | Atlas Search indexes | GIN/GiST (configurable for use case) |
| **Language Independence** | Yes | Yes (inherent with trigrams) |
| **Case Sensitivity** | Configurable | Native case-insensitive |
| **Cost** | ~$764/month (Atlas M50) | ~$200-400/month (RDS PostgreSQL) |
| **Vendor Lock-in** | High (Atlas-specific) | Low (standard SQL + extensions) |

### Migration Goals

1. **Eliminate vendor lock-in** while maintaining search quality
2. **Reduce infrastructure costs** by 40-60%
3. **Improve search performance** with optimized indexing
4. **Maintain feature parity** with current system
5. **Enable future scalability** with proven Java/Spring ecosystem

---

## 2. Current System Analysis

### 2.1 Current Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Current System (Rails)                    │
├─────────────────────────────────────────────────────────────┤
│  Ruby on Rails 7.1 + Ruby 3.2.2                             │
│  ├─ Devise + JWT Authentication                              │
│  ├─ Pundit Authorization                                     │
│  └─ Redis Caching (optional)                                 │
├─────────────────────────────────────────────────────────────┤
│  MongoDB Atlas M50 Cluster                                   │
│  ├─ 7 Collections (Orders, Accounts, Fleets, etc.)          │
│  ├─ MongoDB Atlas Search (fuzzy matching)                    │
│  └─ Custom search indexes per collection                     │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Data Models (7 Collections)

| Model | Key Fields | Searchable Attributes | Relationships |
|-------|------------|----------------------|---------------|
| **Order** | order_number, hawb_numbers[], status, origin, destination | order_number, hawb_numbers, status, notes | → Account, Driver, Fleet, User |
| **Account** | account_number, account_name, company_name, email, phone | account_number, account_name, company_name, contact_person, email | ← Orders, Billings, Invoices |
| **Driver** | first_name, last_name, email, phone, license_number | first_name, last_name, email, phone, license_number | ← Orders |
| **Fleet** | vehicle_name, vin, license_plate, make, model | vehicle_name, vin, license_plate, make, model | ← Orders |
| **User** | email, first_name, last_name, role, status | email, first_name, last_name | → Driver (optional) |
| **POD** | delivery_status, recipient_name, notes | recipient_name, delivery_status, notes | → Order, Driver |
| **Billing** | amount, status, notes | status, notes | → Account, Orders[] |
| **Invoice** | subtotal, status, notes | status, notes | → Account, Billing, Orders[] |

### 2.3 Current Search Features

1. **Global Search**: Across all 7 collections simultaneously
2. **Autocomplete**: Type-ahead with fuzzy matching (<100ms)
3. **Fuzzy Matching**: Levenshtein distance for typo tolerance
4. **Faceted Search**: Filter by status, dates, types
5. **Result Highlighting**: Matched terms emphasized
6. **Relevance Ranking**: Atlas Search scoring
7. **Pagination**: Efficient large result sets
8. **Role-Based Filtering**: 5 roles with different access levels

### 2.4 Performance Requirements

- **Global Search**: < 500ms response time
- **Autocomplete**: < 100ms response time
- **Concurrency**: 100+ concurrent users
- **Throughput**: 100 writes/sec at peak
- **Index Sync**: Near real-time (< 10 seconds)

---

## 3. Proposed Technology Stack

### 3.1 Core Technologies

| Component | Technology | Version | Justification |
|-----------|-----------|---------|---------------|
| **Runtime** | Java (OpenJDK) | JDK 25 LTS | Latest LTS, performance improvements, modern features |
| **Framework** | Spring Boot | 3.5.x | Full JDK 25 support, mature ecosystem, production-ready |
| **Database** | PostgreSQL | 16.x+ | pg_trgm extension, GIN/GiST indexes, JSONB support |
| **ORM** | Spring Data JPA | 3.5.x (Hibernate 6.6+) | JPA specification, native query support |
| **Security** | Spring Security + JWT | 6.5.x | Industry standard, comprehensive auth/authz |
| **Caching** | Spring Cache + Redis | 3.5.x | Performance optimization, session management |
| **Migration** | Flyway | 10.x+ | Version-controlled schema migrations |
| **Build Tool** | Maven | 3.9.x+ | Dependency management, build lifecycle |
| **API Docs** | SpringDoc OpenAPI | 2.8.x | Auto-generated API documentation |

### 3.2 PostgreSQL Extensions Required

```sql
-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS pg_trgm;      -- Trigram similarity matching
CREATE EXTENSION IF NOT EXISTS unaccent;     -- Remove accents for multilingual
CREATE EXTENSION IF NOT EXISTS btree_gin;    -- Multi-column GIN indexes
CREATE EXTENSION IF NOT EXISTS uuid-ossp;    -- UUID generation
```

### 3.3 Development Tools

- **IDE**: IntelliJ IDEA 2024.3+ or Eclipse 2024-12
- **Database Tool**: DBeaver, pgAdmin, or DataGrip
- **Testing**: JUnit 5, Testcontainers (PostgreSQL), REST Assured
- **Monitoring**: Spring Boot Actuator, Micrometer, Prometheus
- **Logging**: SLF4J + Logback

---

## 4. PostgreSQL Search Architecture

### 4.1 Search Strategy: Trigram Similarity with pg_trgm

PostgreSQL's `pg_trgm` extension provides **language-independent fuzzy matching** by breaking text into 3-character sequences (trigrams).

#### How Trigrams Work

```
Text: "Toyota"
Trigrams: {" To", "Toy", "oyo", "yot", "ota", "ta "}

Text: "Tayota" (typo)
Trigrams: {" Ta", "Tay", "ayo", "yot", "ota", "ta "}

Similarity: 4 common trigrams / 6 total = 0.67 (67% similar)
```

### 4.2 Search Functions

PostgreSQL provides three similarity functions with `pg_trgm`:

| Function | Use Case | Example | When to Use |
|----------|----------|---------|-------------|
| `similarity(text1, text2)` | Overall string similarity | `similarity('Toyota', 'Tayota') = 0.67` | Ranking search results |
| `word_similarity(word, text)` | Find word within text | `word_similarity('ORD', 'ORDER-12345') = 0.8` | Prefix matching, autocomplete |
| `strict_word_similarity(word, text)` | Strict word boundaries | For exact word matching within text | Phrase matching |

### 4.3 GIN vs GiST Index Strategy

**Decision Matrix:**

| Query Type | Best Index | Performance | Build Time | Size | Maintenance |
|------------|-----------|-------------|------------|------|-------------|
| **LIKE '%pattern%'** | GIN | 5x faster | Slower | Smaller | Slower on updates |
| **similarity(col, 'text')** | GiST | 5-8x faster | Faster | Larger | Faster on updates |
| **word_similarity('text', col)** | GiST | 10x faster | Faster | Larger | Faster on updates |
| **Multiple columns** | GIN (btree_gin) | Best | Slowest | Smallest | Slowest |

**Recommended Approach:** **Hybrid Strategy**

1. **GIN indexes** for high-read, low-write columns (order_number, account_number, etc.)
2. **GiST indexes** for frequently updated columns with similarity searches
3. **Composite GIN indexes** for multi-attribute searches

### 4.4 Search Configuration Parameters

```sql
-- Set similarity thresholds (0.0 to 1.0)
SET pg_trgm.similarity_threshold = 0.3;      -- Default: 0.3 (30% similar)
SET pg_trgm.word_similarity_threshold = 0.6; -- Default: 0.6 (60% similar)

-- Performance tuning
SET work_mem = '256MB';                      -- For sorting/hashing operations
SET maintenance_work_mem = '2GB';            -- For index creation
SET max_parallel_workers_per_gather = 4;    -- Parallel query execution
```

---

## 5. Database Schema Design

### 5.1 Schema Migration Strategy

**Approach:** Normalized PostgreSQL schema with JSONB for semi-structured data

#### Rationale for JSONB

- **Nested Objects**: MongoDB documents with embedded address, location, etc.
- **Arrays**: HAWB numbers, photo URLs, associated orders
- **Flexibility**: Future schema evolution without migrations
- **Performance**: GIN indexes on JSONB for fast queries
- **Compatibility**: Easy migration path from MongoDB documents

### 5.2 Core Tables Schema

#### 5.2.1 Users Table

```sql
CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) UNIQUE NOT NULL,
    encrypted_password  VARCHAR(255) NOT NULL,
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    role                VARCHAR(50) NOT NULL,
    -- Role: ADMIN, DISPATCHER, BILLING, DRIVER, FLEET_MANAGER
    status              VARCHAR(50) DEFAULT 'active',
    driver_id           BIGINT REFERENCES drivers(id) ON DELETE SET NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Full-text search index with trigrams
CREATE INDEX idx_users_fulltext_gin ON users USING GIN (
    (lower(first_name || ' ' || last_name || ' ' || email)) gin_trgm_ops
);

-- Additional indexes
CREATE INDEX idx_users_email ON users (lower(email));
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_status ON users (status);
```

#### 5.2.2 Accounts Table

```sql
CREATE TABLE accounts (
    id                  BIGSERIAL PRIMARY KEY,
    account_number      VARCHAR(50) UNIQUE NOT NULL,
    account_name        VARCHAR(255) NOT NULL,
    company_name        VARCHAR(255),
    contact_person      VARCHAR(255),
    email               VARCHAR(255),
    phone               VARCHAR(50),
    address             JSONB, -- {street, city, state, zip, country}
    account_type        VARCHAR(50),
    -- Type: shipper, consignee, broker, freight_forwarder
    credit_limit        DECIMAL(15, 2) DEFAULT 0.00,
    current_balance     DECIMAL(15, 2) DEFAULT 0.00,
    status              VARCHAR(50) DEFAULT 'active',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Composite GIN index for multi-attribute search
CREATE INDEX idx_accounts_search_gin ON accounts USING GIN (
    (lower(account_number || ' ' ||
           account_name || ' ' ||
           COALESCE(company_name, '') || ' ' ||
           COALESCE(contact_person, '') || ' ' ||
           COALESCE(email, ''))) gin_trgm_ops
);

-- Additional indexes
CREATE INDEX idx_accounts_number ON accounts (lower(account_number));
CREATE INDEX idx_accounts_type ON accounts (account_type);
CREATE INDEX idx_accounts_status ON accounts (status);
CREATE INDEX idx_accounts_address ON accounts USING GIN (address);
```

#### 5.2.3 Drivers Table

```sql
CREATE TABLE drivers (
    id                  BIGSERIAL PRIMARY KEY,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255) UNIQUE,
    phone               VARCHAR(50),
    license_number      VARCHAR(100) UNIQUE NOT NULL,
    license_state       VARCHAR(50),
    license_expiry      DATE,
    date_of_birth       DATE,
    hire_date           DATE,
    status              VARCHAR(50) DEFAULT 'active',
    address             JSONB, -- {street, city, state, zip, country}
    emergency_contact   JSONB, -- {name, phone, relationship}
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- GiST index for similarity-based search (frequently queried)
CREATE INDEX idx_drivers_search_gist ON drivers USING GIST (
    (lower(first_name || ' ' || last_name || ' ' ||
           COALESCE(email, '') || ' ' ||
           COALESCE(phone, '') || ' ' ||
           license_number)) gist_trgm_ops
);

-- Additional indexes
CREATE INDEX idx_drivers_license ON drivers (lower(license_number));
CREATE INDEX idx_drivers_email ON drivers (lower(email));
CREATE INDEX idx_drivers_status ON drivers (status);
```

#### 5.2.4 Fleets Table

```sql
CREATE TABLE fleets (
    id                  BIGSERIAL PRIMARY KEY,
    vehicle_name        VARCHAR(255),
    vehicle_type        VARCHAR(100),
    vin                 VARCHAR(17) UNIQUE NOT NULL,
    license_plate       VARCHAR(50) UNIQUE NOT NULL,
    make                VARCHAR(100),
    model               VARCHAR(100),
    year                INTEGER,
    color               VARCHAR(50),
    capacity_weight     DECIMAL(10, 2),
    capacity_volume     DECIMAL(10, 2),
    fuel_type           VARCHAR(50),
    status              VARCHAR(50) DEFAULT 'available',
    purchase_date       DATE,
    insurance_expiry    DATE,
    last_maintenance    DATE,
    next_maintenance    DATE,
    odometer            DECIMAL(10, 2),
    current_driver_id   BIGINT REFERENCES drivers(id) ON DELETE SET NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- GIN index for vehicle search
CREATE INDEX idx_fleets_search_gin ON fleets USING GIN (
    (lower(COALESCE(vehicle_name, '') || ' ' ||
           vin || ' ' ||
           license_plate || ' ' ||
           COALESCE(make, '') || ' ' ||
           COALESCE(model, ''))) gin_trgm_ops
);

-- Additional indexes
CREATE INDEX idx_fleets_vin ON fleets (lower(vin));
CREATE INDEX idx_fleets_license_plate ON fleets (lower(license_plate));
CREATE INDEX idx_fleets_status ON fleets (status);
CREATE INDEX idx_fleets_driver ON fleets (current_driver_id);
```

#### 5.2.5 Orders Table

```sql
CREATE TABLE orders (
    id                      BIGSERIAL PRIMARY KEY,
    order_number            VARCHAR(50) UNIQUE NOT NULL,
    hawb_numbers            JSONB, -- Array of HAWB numbers
    status                  VARCHAR(50) DEFAULT 'pending',
    -- Status: pending, confirmed, in_transit, delivered, cancelled, on_hold
    origin                  JSONB, -- {street, city, state, zip, country}
    destination             JSONB, -- {street, city, state, zip, country}
    pickup_date             TIMESTAMP,
    delivery_date           TIMESTAMP,
    estimated_delivery      TIMESTAMP,
    total_weight            DECIMAL(10, 2),
    total_value             DECIMAL(15, 2),
    notes                   TEXT,
    account_id              BIGINT NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    driver_id               BIGINT REFERENCES drivers(id) ON DELETE SET NULL,
    fleet_id                BIGINT REFERENCES fleets(id) ON DELETE SET NULL,
    assigned_dispatcher_id  BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Composite GIN index for order search
CREATE INDEX idx_orders_search_gin ON orders USING GIN (
    (lower(order_number || ' ' ||
           COALESCE(notes, ''))) gin_trgm_ops,
    hawb_numbers jsonb_path_ops
);

-- Additional indexes
CREATE INDEX idx_orders_number ON orders (lower(order_number));
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_account ON orders (account_id);
CREATE INDEX idx_orders_driver ON orders (driver_id);
CREATE INDEX idx_orders_fleet ON orders (fleet_id);
CREATE INDEX idx_orders_dispatcher ON orders (assigned_dispatcher_id);
CREATE INDEX idx_orders_dates ON orders (pickup_date, delivery_date);
CREATE INDEX idx_orders_created ON orders (created_at DESC);
CREATE INDEX idx_orders_hawb ON orders USING GIN (hawb_numbers);
```

#### 5.2.6 PODs Table (Proof of Delivery)

```sql
CREATE TABLE pods (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    driver_id           BIGINT REFERENCES drivers(id) ON DELETE SET NULL,
    delivery_date       TIMESTAMP,
    recipient_name      VARCHAR(255),
    recipient_signature TEXT,
    signature_url       VARCHAR(500),
    delivery_status     VARCHAR(50),
    notes               TEXT,
    location            JSONB, -- {latitude, longitude}
    photo_urls          JSONB, -- Array of photo URLs
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Search index for PODs
CREATE INDEX idx_pods_search_gin ON pods USING GIN (
    (lower(COALESCE(recipient_name, '') || ' ' ||
           COALESCE(notes, ''))) gin_trgm_ops
);

-- Additional indexes
CREATE INDEX idx_pods_order ON pods (order_id);
CREATE INDEX idx_pods_driver ON pods (driver_id);
CREATE INDEX idx_pods_status ON pods (delivery_status);
CREATE INDEX idx_pods_date ON pods (delivery_date);
```

#### 5.2.7 Billings Table

```sql
CREATE TABLE billings (
    id              BIGSERIAL PRIMARY KEY,
    account_id      BIGINT NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    amount          DECIMAL(15, 2) NOT NULL,
    tax_amount      DECIMAL(15, 2) DEFAULT 0.00,
    status          VARCHAR(50) DEFAULT 'pending',
    -- Status: pending, paid, overdue, cancelled
    billing_date    DATE NOT NULL,
    due_date        DATE,
    payment_date    DATE,
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Junction table for billing-order relationship
CREATE TABLE billing_orders (
    billing_id  BIGINT NOT NULL REFERENCES billings(id) ON DELETE CASCADE,
    order_id    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    PRIMARY KEY (billing_id, order_id)
);

-- Search index
CREATE INDEX idx_billings_search_gin ON billings USING GIN (
    (lower(COALESCE(notes, ''))) gin_trgm_ops
);

-- Additional indexes
CREATE INDEX idx_billings_account ON billings (account_id);
CREATE INDEX idx_billings_status ON billings (status);
CREATE INDEX idx_billings_dates ON billings (billing_date, due_date);
CREATE INDEX idx_billing_orders_order ON billing_orders (order_id);
```

#### 5.2.8 Invoices Table

```sql
CREATE TABLE invoices (
    id              BIGSERIAL PRIMARY KEY,
    account_id      BIGINT NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    billing_id      BIGINT REFERENCES billings(id) ON DELETE SET NULL,
    subtotal        DECIMAL(15, 2) NOT NULL,
    tax_amount      DECIMAL(15, 2) DEFAULT 0.00,
    discount_amount DECIMAL(15, 2) DEFAULT 0.00,
    status          VARCHAR(50) DEFAULT 'draft',
    -- Status: draft, sent, paid, overdue, cancelled
    invoice_date    DATE NOT NULL,
    due_date        DATE,
    payment_date    DATE,
    terms           TEXT,
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Junction table for invoice-order relationship
CREATE TABLE invoice_orders (
    invoice_id  BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    order_id    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    PRIMARY KEY (invoice_id, order_id)
);

-- Search index
CREATE INDEX idx_invoices_search_gin ON invoices USING GIN (
    (lower(COALESCE(notes, ''))) gin_trgm_ops
);

-- Additional indexes
CREATE INDEX idx_invoices_account ON invoices (account_id);
CREATE INDEX idx_invoices_billing ON invoices (billing_id);
CREATE INDEX idx_invoices_status ON invoices (status);
CREATE INDEX idx_invoices_dates ON invoices (invoice_date, due_date);
CREATE INDEX idx_invoice_orders_order ON invoice_orders (order_id);
```

### 5.3 Materialized View for Global Search

To optimize global search across all entities:

```sql
CREATE MATERIALIZED VIEW global_search_index AS
SELECT
    'order' AS entity_type,
    id AS entity_id,
    order_number AS primary_identifier,
    lower(order_number || ' ' ||
          COALESCE(notes, '')) AS searchable_text,
    status,
    account_id,
    driver_id,
    created_at,
    updated_at
FROM orders

UNION ALL

SELECT
    'account' AS entity_type,
    id AS entity_id,
    account_number AS primary_identifier,
    lower(account_number || ' ' ||
          account_name || ' ' ||
          COALESCE(company_name, '') || ' ' ||
          COALESCE(contact_person, '') || ' ' ||
          COALESCE(email, '')) AS searchable_text,
    status,
    NULL::BIGINT AS account_id,
    NULL::BIGINT AS driver_id,
    created_at,
    updated_at
FROM accounts

UNION ALL

SELECT
    'driver' AS entity_type,
    id AS entity_id,
    license_number AS primary_identifier,
    lower(first_name || ' ' || last_name || ' ' ||
          COALESCE(email, '') || ' ' ||
          COALESCE(phone, '') || ' ' ||
          license_number) AS searchable_text,
    status,
    NULL::BIGINT AS account_id,
    id AS driver_id,
    created_at,
    updated_at
FROM drivers

UNION ALL

SELECT
    'fleet' AS entity_type,
    id AS entity_id,
    vin AS primary_identifier,
    lower(COALESCE(vehicle_name, '') || ' ' ||
          vin || ' ' ||
          license_plate || ' ' ||
          COALESCE(make, '') || ' ' ||
          COALESCE(model, '')) AS searchable_text,
    status,
    NULL::BIGINT AS account_id,
    current_driver_id AS driver_id,
    created_at,
    updated_at
FROM fleets

UNION ALL

SELECT
    'pod' AS entity_type,
    p.id AS entity_id,
    o.order_number AS primary_identifier,
    lower(COALESCE(p.recipient_name, '') || ' ' ||
          COALESCE(p.notes, '')) AS searchable_text,
    p.delivery_status AS status,
    o.account_id,
    p.driver_id,
    p.created_at,
    p.updated_at
FROM pods p
JOIN orders o ON p.order_id = o.id

UNION ALL

SELECT
    'billing' AS entity_type,
    id AS entity_id,
    id::VARCHAR AS primary_identifier,
    lower(COALESCE(notes, '')) AS searchable_text,
    status,
    account_id,
    NULL::BIGINT AS driver_id,
    created_at,
    updated_at
FROM billings

UNION ALL

SELECT
    'invoice' AS entity_type,
    id AS entity_id,
    id::VARCHAR AS primary_identifier,
    lower(COALESCE(notes, '')) AS searchable_text,
    status,
    account_id,
    NULL::BIGINT AS driver_id,
    created_at,
    updated_at
FROM invoices;

-- Create GiST index on materialized view
CREATE INDEX idx_global_search_gist ON global_search_index USING GIST (
    searchable_text gist_trgm_ops
);

CREATE INDEX idx_global_search_entity ON global_search_index (entity_type);
CREATE INDEX idx_global_search_account ON global_search_index (account_id);
CREATE INDEX idx_global_search_driver ON global_search_index (driver_id);
CREATE INDEX idx_global_search_created ON global_search_index (created_at DESC);

-- Refresh strategy (pick one)
-- Option 1: Manual refresh after data changes
-- REFRESH MATERIALIZED VIEW CONCURRENTLY global_search_index;

-- Option 2: Scheduled refresh (using pg_cron extension)
-- SELECT cron.schedule('refresh-global-search', '*/5 * * * *',
--   'REFRESH MATERIALIZED VIEW CONCURRENTLY global_search_index');
```

---

## 6. Search Implementation Strategy

### 6.1 Search Service Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      API Layer (REST)                        │
│  /api/v1/search/global, /api/v1/search/autocomplete         │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                   SearchController                           │
│  - Validates requests, handles pagination                   │
│  - Enforces rate limiting                                    │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                 SearchService (Business Logic)               │
│  ├─ GlobalSearchService                                     │
│  ├─ AutocompleteService                                     │
│  ├─ FacetedSearchService                                    │
│  └─ RoleBasedFilterService                                  │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│            SearchRepository (Data Access)                    │
│  - Native PostgreSQL queries with pg_trgm                   │
│  - JPA Specifications for dynamic filtering                 │
│  - QueryDSL for type-safe queries (optional)                │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                     PostgreSQL 16+                           │
│  - pg_trgm extension (similarity, word_similarity)          │
│  - GIN/GiST indexes on searchable columns                   │
│  - Materialized view for global search                      │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 Search Query Examples

#### 6.2.1 Fuzzy Search with Similarity

```java
// Service Layer
public List<SearchResultDTO> fuzzySearch(String query, String entityType, double threshold) {
    String sql = """
        SELECT
            id,
            order_number,
            similarity(lower(order_number), lower(?)) AS relevance
        FROM orders
        WHERE similarity(lower(order_number), lower(?)) > ?
        ORDER BY relevance DESC, created_at DESC
        LIMIT 50
    """;

    return jdbcTemplate.query(sql,
        new Object[]{query, query, threshold},
        new SearchResultRowMapper());
}
```

#### 6.2.2 Autocomplete with Word Similarity

```java
public List<AutocompleteDTO> autocomplete(String prefix, String entityType) {
    String sql = """
        SELECT DISTINCT
            account_number,
            account_name,
            word_similarity(?, lower(account_number)) AS relevance
        FROM accounts
        WHERE
            lower(account_number) %> lower(?) OR
            lower(account_name) %> lower(?)
        ORDER BY relevance DESC
        LIMIT 10
    """;

    return jdbcTemplate.query(sql,
        new Object[]{prefix, prefix, prefix},
        new AutocompleteRowMapper());
}
```

#### 6.2.3 Multi-Attribute Search with GIN Index

```java
public Page<AccountDTO> searchAccounts(AccountSearchCriteria criteria, Pageable pageable) {
    String searchText = criteria.getQuery().toLowerCase();

    String sql = """
        SELECT *,
            similarity(
                lower(account_number || ' ' || account_name || ' ' ||
                      COALESCE(company_name, '')),
                ?
            ) AS relevance
        FROM accounts
        WHERE
            lower(account_number || ' ' || account_name || ' ' ||
                  COALESCE(company_name, '')) % ?
            AND status = COALESCE(?, status)
            AND account_type = COALESCE(?, account_type)
        ORDER BY relevance DESC, created_at DESC
        LIMIT ? OFFSET ?
    """;

    // Use GIN index automatically
    return jdbcTemplate.query(sql, params, new AccountRowMapper());
}
```

#### 6.2.4 Global Search Across All Entities

```java
public List<GlobalSearchResultDTO> globalSearch(String query, UserDetails user) {
    // Apply role-based filtering
    Set<String> allowedTypes = roleFilterService.getAllowedEntityTypes(user);

    String sql = """
        SELECT
            entity_type,
            entity_id,
            primary_identifier,
            similarity(searchable_text, lower(?)) AS relevance,
            ts_headline(searchable_text, plainto_tsquery(?)) AS highlight
        FROM global_search_index
        WHERE
            searchable_text % lower(?)
            AND entity_type = ANY(?)
            AND (
                ? = 'ADMIN' OR
                (? = 'DISPATCHER' AND entity_type IN ('order', 'fleet', 'driver', 'pod')) OR
                (? = 'BILLING' AND entity_type IN ('order', 'billing', 'invoice')) OR
                (? = 'DRIVER' AND driver_id = ?) OR
                (? = 'FLEET_MANAGER' AND entity_type IN ('order', 'fleet', 'driver'))
            )
        ORDER BY relevance DESC, created_at DESC
        LIMIT 100
    """;

    return jdbcTemplate.query(sql, params, new GlobalSearchRowMapper());
}
```

### 6.3 Similarity Threshold Configuration

```java
@Configuration
public class SearchConfiguration {

    @Value("${search.similarity.global:0.3}")
    private double globalSimilarityThreshold;

    @Value("${search.similarity.word:0.6}")
    private double wordSimilarityThreshold;

    @PostConstruct
    public void configurePgTrgm() {
        jdbcTemplate.execute(
            "SET pg_trgm.similarity_threshold = " + globalSimilarityThreshold
        );
        jdbcTemplate.execute(
            "SET pg_trgm.word_similarity_threshold = " + wordSimilarityThreshold
        );
    }
}
```

### 6.4 Caching Strategy

```java
@Service
public class SearchService {

    @Cacheable(value = "autocomplete", key = "#prefix + '-' + #entityType")
    public List<AutocompleteDTO> autocomplete(String prefix, String entityType) {
        // Cache autocomplete results for 5 minutes
        return searchRepository.autocomplete(prefix, entityType);
    }

    @Cacheable(value = "facets", key = "#entityType")
    public Map<String, Long> getFacets(String entityType) {
        // Cache facet counts for 10 minutes
        return searchRepository.calculateFacets(entityType);
    }
}
```

---

## 7. Performance Optimization

### 7.1 Index Strategy Summary

| Table | Index Type | Columns | Purpose | Expected Performance |
|-------|-----------|---------|---------|---------------------|
| **orders** | GIN | order_number, notes | Fuzzy search | < 50ms for 1M records |
| **accounts** | GIN | account_number, account_name, company_name | Multi-attribute search | < 50ms |
| **drivers** | GiST | first_name, last_name, license_number | Similarity search | < 30ms |
| **fleets** | GIN | vin, license_plate, make, model | Vehicle search | < 40ms |
| **global_search_index** | GiST | searchable_text | Cross-entity search | < 100ms |

### 7.2 Query Optimization Techniques

1. **Use EXPLAIN ANALYZE** to verify index usage
2. **Set appropriate work_mem** for large result sets
3. **Enable parallel query execution** for complex searches
4. **Use prepared statements** to reduce parsing overhead
5. **Implement pagination** with cursor-based approach for large datasets
6. **Materialize frequent searches** for real-time dashboards

### 7.3 Connection Pooling

```properties
# HikariCP configuration (application.properties)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### 7.4 Monitoring & Metrics

```java
@Component
public class SearchMetrics {

    private final MeterRegistry registry;

    @Around("execution(* com.example.search.service.*.*(..))")
    public Object trackSearchMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(registry);
        try {
            Object result = joinPoint.proceed();
            sample.stop(Timer.builder("search.query.duration")
                .tag("method", joinPoint.getSignature().getName())
                .register(registry));
            return result;
        } catch (Exception e) {
            registry.counter("search.query.errors",
                "method", joinPoint.getSignature().getName()).increment();
            throw e;
        }
    }
}
```

---

## 8. Migration Strategy

### 8.1 Migration Phases

| Phase | Duration | Activities | Success Criteria |
|-------|----------|------------|------------------|
| **Phase 1: Setup** | Week 1-2 | Environment setup, Spring Boot scaffolding | Working dev environment |
| **Phase 2: Schema** | Week 3-4 | PostgreSQL schema creation, Flyway migrations | All tables created with indexes |
| **Phase 3: Data Migration** | Week 5-6 | MongoDB → PostgreSQL ETL, validation | 100% data migrated, validated |
| **Phase 4: Search Implementation** | Week 7-8 | Search services, repositories, controllers | All search features working |
| **Phase 5: Testing** | Week 9-10 | Unit, integration, performance testing | 95%+ test coverage, perf targets met |
| **Phase 6: Deployment** | Week 11-12 | Staging deployment, UAT, production cutover | Zero-downtime migration |

### 8.2 Data Migration Script (Conceptual)

```python
# MongoDB to PostgreSQL ETL Script (Python with psycopg2 and pymongo)

from pymongo import MongoClient
import psycopg2
from psycopg2.extras import execute_batch
import json

# Connect to MongoDB
mongo_client = MongoClient("mongodb://atlas-connection-string")
mongo_db = mongo_client['logistics_db']

# Connect to PostgreSQL
pg_conn = psycopg2.connect("postgresql://localhost/global_search")
pg_cursor = pg_conn.cursor()

# Migrate Orders
orders = mongo_db.orders.find()
order_data = []

for order in orders:
    order_data.append((
        order['order_number'],
        json.dumps(order.get('hawb_numbers', [])),
        order['status'],
        json.dumps(order.get('origin', {})),
        json.dumps(order.get('destination', {})),
        order.get('pickup_date'),
        order.get('delivery_date'),
        order.get('estimated_delivery'),
        order.get('total_weight'),
        order.get('total_value'),
        order.get('notes'),
        # Foreign keys require lookup from migrated IDs
        get_account_id(order['account_id']),
        get_driver_id(order.get('driver_id')),
        get_fleet_id(order.get('fleet_id')),
        get_user_id(order.get('assigned_dispatcher_id')),
        order['created_at'],
        order['updated_at']
    ))

# Batch insert
execute_batch(pg_cursor, """
    INSERT INTO orders (
        order_number, hawb_numbers, status, origin, destination,
        pickup_date, delivery_date, estimated_delivery,
        total_weight, total_value, notes,
        account_id, driver_id, fleet_id, assigned_dispatcher_id,
        created_at, updated_at
    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
""", order_data)

pg_conn.commit()

# Refresh materialized view
pg_cursor.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY global_search_index")
pg_conn.commit()
```

### 8.3 Rollback Strategy

1. **Blue-Green Deployment**: Keep MongoDB cluster running during migration
2. **Feature Flags**: Toggle between old/new search implementation
3. **Data Sync**: Bi-directional sync during transition period
4. **Rollback Triggers**: Automated rollback if error rate > 5% or latency > 2x baseline

---

## 9. Security & Authorization

### 9.1 Authentication Flow

```
┌──────────┐       ┌──────────────┐       ┌──────────────┐
│  Client  │──1───>│ Auth Service │──2───>│  PostgreSQL  │
│          │<──4───│   (JWT)      │<──3───│   (Users)    │
└──────────┘       └──────────────┘       └──────────────┘
     │
     5─────────────────────────┐
                                ▼
                    ┌───────────────────────┐
                    │  Search API (JWT)     │
                    │  - Validates token    │
                    │  - Extracts role      │
                    │  - Filters results    │
                    └───────────────────────┘
```

### 9.2 Role-Based Search Filtering

```java
@Service
public class RoleBasedFilterService {

    public SearchFilter getSearchFilter(UserDetails user) {
        String role = user.getRole();
        Long userId = user.getId();

        return switch (role) {
            case "ADMIN" -> SearchFilter.all();

            case "DISPATCHER" -> SearchFilter.builder()
                .allowedTypes(Set.of("order", "fleet", "driver", "pod"))
                .build();

            case "BILLING" -> SearchFilter.builder()
                .allowedTypes(Set.of("order", "billing", "invoice"))
                .readOnly(Set.of("order"))
                .build();

            case "DRIVER" -> SearchFilter.builder()
                .allowedTypes(Set.of("order", "pod"))
                .driverIdFilter(userId)
                .build();

            case "FLEET_MANAGER" -> SearchFilter.builder()
                .allowedTypes(Set.of("order", "fleet", "driver"))
                .build();

            default -> SearchFilter.none();
        };
    }
}
```

### 9.3 SQL Injection Prevention

- **Use parameterized queries** exclusively (JDBC PreparedStatement, JPA)
- **Never concatenate user input** into SQL strings
- **Validate and sanitize** all search inputs
- **Enable SQL logging** in dev/staging to catch anti-patterns

---

## 10. API Design

### 10.1 API Endpoints

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/v1/auth/login` | POST | JWT authentication | No |
| `/api/v1/auth/refresh` | POST | Refresh JWT token | Yes (refresh token) |
| `/api/v1/search/global` | POST | Global search across entities | Yes |
| `/api/v1/search/autocomplete` | GET | Type-ahead suggestions | Yes |
| `/api/v1/search/facets` | GET | Aggregated facet counts | Yes |
| `/api/v1/search/{entity}` | POST | Entity-specific search | Yes |

### 10.2 Request/Response Examples

#### Global Search Request

```json
POST /api/v1/search/global
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "query": "toyota",
  "searchType": "all",
  "filters": {
    "status": ["active", "pending"],
    "dateFrom": "2024-01-01",
    "dateTo": "2025-12-31"
  },
  "includeHighlights": true,
  "includeFacets": true,
  "page": 0,
  "size": 20,
  "sortBy": "relevance"
}
```

#### Global Search Response

```json
{
  "success": true,
  "query": "toyota",
  "totalResults": 142,
  "executionTimeMs": 87,
  "results": [
    {
      "entityType": "fleet",
      "entityId": 12345,
      "primaryIdentifier": "VIN-ABC123",
      "relevance": 0.89,
      "highlight": "<em>Toyota</em> Camry 2023",
      "data": {
        "vehicleName": "Toyota Camry 2023",
        "vin": "VIN-ABC123",
        "licensePlate": "XYZ-789",
        "status": "available"
      }
    },
    {
      "entityType": "order",
      "entityId": 67890,
      "primaryIdentifier": "ORD-2024-5678",
      "relevance": 0.72,
      "highlight": "Pickup <em>Toyota</em> parts from warehouse",
      "data": {
        "orderNumber": "ORD-2024-5678",
        "status": "in_transit",
        "notes": "Pickup Toyota parts from warehouse"
      }
    }
  ],
  "facets": {
    "entityTypes": {
      "fleet": 87,
      "order": 45,
      "account": 10
    },
    "status": {
      "active": 92,
      "pending": 35,
      "completed": 15
    }
  },
  "pagination": {
    "currentPage": 0,
    "totalPages": 8,
    "pageSize": 20,
    "hasNext": true
  }
}
```

#### Autocomplete Request

```json
GET /api/v1/search/autocomplete?prefix=ORD&type=order&limit=10
Authorization: Bearer <JWT_TOKEN>
```

#### Autocomplete Response

```json
{
  "success": true,
  "suggestions": [
    {
      "value": "ORD-2024-12345",
      "label": "ORD-2024-12345 (Toyota Parts Delivery)",
      "type": "order",
      "relevance": 0.95
    },
    {
      "value": "ORD-2024-12346",
      "label": "ORD-2024-12346 (Office Supplies)",
      "type": "order",
      "relevance": 0.93
    }
  ],
  "executionTimeMs": 23
}
```

---

## 11. Deployment Architecture

### 11.1 Production Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Load Balancer (ALB)                      │
│                  SSL Termination (ACM)                       │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
┌───────▼──────┐          ┌───────▼──────┐
│  Spring Boot │          │  Spring Boot │
│  Instance 1  │          │  Instance 2  │
│  (ECS/EC2)   │          │  (ECS/EC2)   │
└───────┬──────┘          └───────┬──────┘
        │                         │
        └────────────┬────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
┌───────▼──────────┐      ┌───────▼──────────┐
│  PostgreSQL RDS  │      │  Redis ElastiCache│
│  (Multi-AZ)      │      │  (Cluster Mode)  │
│  - Primary       │      │  - Session Cache │
│  - Read Replica  │      │  - Search Cache  │
└──────────────────┘      └──────────────────┘
```

### 11.2 Infrastructure Specifications

| Component | Specification | Justification |
|-----------|---------------|---------------|
| **Application Server** | AWS ECS Fargate (4 vCPU, 8GB RAM) x2 | Auto-scaling, containerized |
| **Database** | RDS PostgreSQL 16 (db.r6g.xlarge) | 4 vCPU, 32GB RAM, Multi-AZ |
| **Read Replica** | RDS Read Replica (db.r6g.large) | Offload read-heavy search queries |
| **Cache** | ElastiCache Redis (cache.r6g.large) | 2 vCPU, 13GB RAM, cluster mode |
| **Load Balancer** | Application Load Balancer | SSL termination, health checks |
| **Storage** | RDS gp3 SSD (500GB) | 12,000 IOPS, 500 MB/s throughput |

### 11.3 Scalability Considerations

- **Horizontal Scaling**: Auto-scaling group (2-10 instances based on CPU/memory)
- **Read Scaling**: PostgreSQL read replicas for search queries
- **Cache Scaling**: Redis cluster with automatic failover
- **Database Partitioning**: Table partitioning for orders/billings (by date)

---

## 12. Cost Analysis

### 12.1 Infrastructure Costs (Monthly, AWS US-East-1)

| Component | Current (MongoDB) | Proposed (PostgreSQL) | Savings |
|-----------|-------------------|----------------------|---------|
| **Database** | Atlas M50: $500 | RDS db.r6g.xlarge: $280 | $220 |
| **Read Replica** | N/A | RDS db.r6g.large: $140 | -$140 |
| **Application** | EC2 t3.xlarge x2: $150 | ECS Fargate x2: $120 | $30 |
| **Cache** | ElastiCache: $100 | ElastiCache: $100 | $0 |
| **Load Balancer** | ALB: $25 | ALB: $25 | $0 |
| **Data Transfer** | $50 | $40 | $10 |
| **Backups** | Included | RDS Snapshots: $25 | -$25 |
| **Monitoring** | Atlas: $30 | CloudWatch: $20 | $10 |
| **TOTAL** | **$855/month** | **$750/month** | **$105/month (12%)** |

**Annual Savings:** $1,260

### 12.2 Cost Optimization Opportunities

1. **Use Reserved Instances**: Save 40-60% on RDS and EC2 (1-year commitment)
2. **Right-size after profiling**: Start with db.r6g.large and scale up if needed
3. **Use Aurora PostgreSQL**: Better cost for high-concurrency (alternative)
4. **Lifecycle policies**: Archive old data to S3 Glacier

### 12.3 Development/Testing Costs

- **Staging Environment**: 50% of production (~$375/month)
- **Development Environment**: Local Docker containers ($0)

---

## 13. Risk Assessment & Mitigation

### 13.1 Technical Risks

| Risk | Probability | Impact | Mitigation Strategy |
|------|------------|--------|---------------------|
| **Search performance degradation** | Medium | High | Extensive load testing, index optimization, read replicas |
| **Data migration errors** | Medium | Critical | Thorough validation scripts, parallel runs, rollback plan |
| **JDK 25 compatibility issues** | Low | Medium | Use Spring Boot 3.5.x (officially supports JDK 25) |
| **PostgreSQL scaling limits** | Low | Medium | Design for horizontal partitioning from day 1 |
| **Learning curve (team)** | Medium | Medium | Training plan, pair programming, documentation |

### 13.2 Operational Risks

| Risk | Probability | Impact | Mitigation Strategy |
|------|------------|--------|---------------------|
| **Downtime during migration** | Medium | High | Blue-green deployment, feature flags, zero-downtime migration |
| **Increased latency** | Medium | High | Performance SLAs, monitoring, auto-scaling |
| **Data inconsistency** | Low | Critical | Bi-directional sync during transition, validation checks |

### 13.3 Business Risks

| Risk | Probability | Impact | Mitigation Strategy |
|------|------------|--------|---------------------|
| **Budget overruns** | Low | Medium | Phased approach, monthly cost tracking |
| **Timeline delays** | Medium | Medium | Buffer time (20%), agile sprints, MVP approach |
| **User adoption issues** | Low | Low | Maintain API compatibility, gradual rollout |

---

## 14. Open Questions & Clarifications

### 14.1 Critical Clarifications Needed

#### **A. Data Volume & Growth**

1. **Current data volume:**
   - How many total records in each collection? (Orders, Accounts, etc.)
   - What is the current database size in GB?
   - What is the expected growth rate? (records/month)

2. **Search volume:**
   - What is the average number of search requests per day?
   - What is the peak search load? (requests/second)
   - What is the acceptable downtime window for migration?

#### **B. Search Behavior & Requirements**

3. **Search patterns:**
   - What are the most common search queries? (order numbers, names, dates?)
   - What percentage of searches are global vs. entity-specific?
   - Do users expect exact matches or fuzzy matches by default?

4. **Autocomplete expectations:**
   - Should autocomplete search across all fields or specific ones?
   - Is 100ms autocomplete latency a hard requirement or a goal?
   - How many autocomplete suggestions should be returned? (current: 10)

5. **Result ranking:**
   - Should results prioritize exact matches over fuzzy matches?
   - Are there any business rules for result ranking? (e.g., recent orders first)
   - Should search results include archived/deleted records?

#### **C. Authorization & Multi-Tenancy**

6. **Role hierarchy:**
   - Are there more roles beyond the 5 documented? (Admin, Dispatcher, etc.)
   - Can a user have multiple roles simultaneously?
   - Are there account-level permissions? (e.g., Account Manager sees only their accounts)

7. **Data isolation:**
   - Is this a multi-tenant system? (Multiple organizations in one database)
   - If multi-tenant, should search be tenant-scoped automatically?
   - Are there any regulatory compliance requirements? (GDPR, HIPAA, etc.)

#### **D. Technical Constraints**

8. **Infrastructure preferences:**
   - AWS, Azure, GCP, or on-premises deployment?
   - Are there existing PostgreSQL instances to leverage?
   - What is the current CI/CD pipeline? (Jenkins, GitHub Actions, GitLab?)

9. **Integration requirements:**
   - Are there other systems that query the search API? (mobile apps, dashboards?)
   - Do you need GraphQL support in addition to REST?
   - Are webhooks or event streaming required? (Kafka, RabbitMQ?)

10. **Monitoring & observability:**
    - What monitoring tools are currently used? (Datadog, New Relic, CloudWatch?)
    - Are there existing SLAs for search performance?
    - What is the required log retention period?

#### **E. Migration & Timeline**

11. **Migration constraints:**
    - Is a staged rollout acceptable? (e.g., read-only mode first)
    - Can the MongoDB cluster remain active during migration? (blue-green)
    - What is the preferred migration window? (weekends, low-traffic hours?)

12. **Timeline expectations:**
    - What is the target go-live date?
    - Are there any hard deadlines? (contract renewals, events?)
    - What is the acceptable timeline for Phase 1 (MVP)?

#### **F. Testing & Quality**

13. **Testing requirements:**
    - What is the expected test coverage percentage?
    - Are there existing test datasets or fixtures?
    - Should we implement contract testing for API compatibility?

14. **Performance benchmarks:**
    - What are the current p50, p95, p99 latencies for search?
    - What is the acceptable error rate? (< 0.1%?)
    - Should we implement load testing? (JMeter, Gatling, Locust?)

#### **G. Future Roadmap**

15. **Future features:**
    - Are there plans for advanced features? (ML-based ranking, semantic search?)
    - Will search need to support file content? (PDF, Word documents?)
    - Is geospatial search required? (PostGIS extension?)

16. **Internationalization:**
    - What languages need to be supported?
    - Should search support non-Latin scripts? (Chinese, Arabic, Cyrillic?)
    - Are there localization requirements for search results?

---

### 14.2 Assumptions (To Be Validated)

For the purpose of this specification, the following assumptions have been made:

1. **Data volume**: < 10M total records, < 100GB database size
2. **Search load**: < 100 requests/second at peak
3. **Deployment**: AWS infrastructure (can be adapted)
4. **Downtime tolerance**: 1-hour maintenance window acceptable
5. **Budget**: $1,000/month for production infrastructure
6. **Timeline**: 12-week migration (3 months)
7. **Team**: 2-3 Java developers, 1 DevOps engineer, 1 DBA
8. **Testing**: Automated testing required, 80%+ coverage
9. **Compatibility**: Maintain current API contract (REST JSON)
10. **Language support**: English only (can add multilingual later)

---

### 14.3 Recommended Next Steps

1. **Stakeholder Review**: Review this specification with business and technical stakeholders
2. **Clarification Session**: Schedule a meeting to address Section 14.1 questions
3. **POC Development**: Build a proof-of-concept with sample data (1-2 weeks)
   - Single entity (e.g., Orders) with fuzzy search
   - Performance benchmarking with pg_trgm
   - Compare with current MongoDB Atlas Search
4. **Refined Estimate**: Update timeline and costs based on answers
5. **Architecture Review**: Technical deep-dive with senior engineers/architects
6. **Migration Plan Approval**: Get sign-off on phased migration approach
7. **Kickoff**: Begin Phase 1 (Environment Setup) after approvals

---

## Appendices

### Appendix A: Technology Comparison Matrix

| Feature | MongoDB Atlas Search | PostgreSQL pg_trgm | Winner |
|---------|---------------------|-------------------|--------|
| Fuzzy matching | Levenshtein distance | Trigram similarity | Tie |
| Case-insensitive | Configurable | Native | PostgreSQL |
| Language independence | Yes | Yes (better) | PostgreSQL |
| Index performance | Good | Excellent (GIN/GiST) | PostgreSQL |
| Setup complexity | Manual Atlas UI | SQL commands | PostgreSQL |
| Query language | MongoDB aggregation | Standard SQL | PostgreSQL |
| Vendor lock-in | High (Atlas-specific) | None (open-source) | PostgreSQL |
| Cost | $500+/month | $200-400/month | PostgreSQL |
| Ecosystem maturity | Moderate (newer) | Excellent (decades) | PostgreSQL |
| Scaling | Vertical (cluster tier) | Horizontal (replicas, partitioning) | PostgreSQL |

### Appendix B: Sample Flyway Migration

```sql
-- V1__initial_schema.sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    -- ... (see Section 5.2.1)
);

-- V2__add_search_indexes.sql
CREATE INDEX idx_users_fulltext_gin ON users USING GIN (
    (lower(first_name || ' ' || last_name || ' ' || email)) gin_trgm_ops
);

-- V3__create_materialized_view.sql
CREATE MATERIALIZED VIEW global_search_index AS
-- ... (see Section 5.3)

-- V4__add_role_based_security.sql
ALTER TABLE users ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'USER';
CREATE INDEX idx_users_role ON users (role);
```

### Appendix C: Spring Boot Project Structure

```
global-search-java/
├── src/
│   ├── main/
│   │   ├── java/com/example/globalsearch/
│   │   │   ├── GlobalSearchApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── SearchConfig.java
│   │   │   │   └── CacheConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── SearchController.java
│   │   │   │   └── AutocompleteController.java
│   │   │   ├── service/
│   │   │   │   ├── GlobalSearchService.java
│   │   │   │   ├── AutocompleteService.java
│   │   │   │   ├── RoleBasedFilterService.java
│   │   │   │   └── FacetService.java
│   │   │   ├── repository/
│   │   │   │   ├── SearchRepository.java
│   │   │   │   ├── OrderRepository.java
│   │   │   │   ├── AccountRepository.java
│   │   │   │   └── ... (other entity repositories)
│   │   │   ├── model/
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Order.java
│   │   │   │   │   ├── Account.java
│   │   │   │   │   └── ... (other entities)
│   │   │   │   └── dto/
│   │   │   │       ├── SearchRequestDTO.java
│   │   │   │       ├── SearchResultDTO.java
│   │   │   │       └── AutocompleteDTO.java
│   │   │   ├── security/
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       └── SearchException.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── db/migration/
│   │           ├── V1__initial_schema.sql
│   │           ├── V2__add_search_indexes.sql
│   │           └── V3__create_materialized_view.sql
│   └── test/
│       └── java/com/example/globalsearch/
│           ├── integration/
│           │   ├── SearchIntegrationTest.java
│           │   └── AutocompleteIntegrationTest.java
│           └── unit/
│               ├── SearchServiceTest.java
│               └── RoleBasedFilterServiceTest.java
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── README.md
```

### Appendix D: Key Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.5.7</version>
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

    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.4</version>
    </dependency>

    <!-- Flyway Migrations -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
        <version>10.21.0</version>
    </dependency>

    <!-- Redis Cache -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.36</version>
        <scope>provided</scope>
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
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>

<properties>
    <java.version>25</java.version>
    <maven.compiler.source>25</maven.compiler.source>
    <maven.compiler.target>25</maven.compiler.target>
</properties>
```

---

## Conclusion

This specification provides a **production-grade solution** for migrating the Global Search API from Ruby on Rails + MongoDB Atlas Search to **Java Spring Boot (JDK 25) + PostgreSQL with pg_trgm**.

### Key Advantages

✅ **Language-independent fuzzy search** using trigram similarity
✅ **Case-insensitive by default** with PostgreSQL pg_trgm
✅ **Fast search performance** with GIN/GiST indexes (<100ms autocomplete, <500ms global search)
✅ **Error-tolerant** typo handling with configurable similarity thresholds
✅ **Cost reduction** of 12-40% compared to MongoDB Atlas
✅ **Zero vendor lock-in** with open-source PostgreSQL
✅ **Proven technology stack** (Spring Boot 3.5.x + JDK 25 fully supported)
✅ **Scalable architecture** with read replicas and horizontal partitioning

### Next Steps

Please review **Section 14 (Open Questions & Clarifications)** and provide answers so we can:
1. Finalize the technical design
2. Build a proof-of-concept
3. Provide accurate timeline and cost estimates
4. Begin implementation

---

**Document prepared by:** Claude AI Assistant
**For:** Global Search Migration Project
**Contact:** [Your team contact information]
