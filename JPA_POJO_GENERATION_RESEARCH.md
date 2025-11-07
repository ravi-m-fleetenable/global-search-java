# JPA POJO Generation from Schema - Production-Grade Solution

**Status:** ✅ Research Complete
**Date:** 2025-11-07
**Context:** MongoDB → PostgreSQL migration with schema-first approach
**JPA Version:** Jakarta Persistence 3.2 (2024)

---

## Executive Summary

**Your Challenge:**
- Migrating from MongoDB to PostgreSQL
- Need to standardize schema first (JSON Schema)
- Want to generate JPA POJOs from that schema
- Need build-time validation to ensure POJOs match schema
- Considering hexagonal architecture
- Wondering if MapStruct is needed

**My Recommendation:**
✅ **Use JSON Schema + Hibernate JPA Static Metamodel Generator + Schema Validation**
✅ **Implement Hexagonal Architecture with separate domain and persistence models**
✅ **Use MapStruct for domain ↔ persistence entity mapping**
✅ **Validate schema compliance at build time with custom Maven plugin**

---

## 📊 Research Findings

### 1. Latest JPA Version: Jakarta Persistence 3.2 (2024)

**Key Features Relevant to Your Use Case:**

#### A. Java Records as Embeddables (NEW in 3.2)
```java
// Perfect for value objects in your domain model
public record Address(
    String street,
    String city,
    String state,
    String zip,
    String country
) {}

@Entity
public class Account {
    @Id
    private Long id;

    @Embedded
    private Address address; // Records work as embeddables!
}
```

#### B. Enhanced Date/Time Support
- Full support for `java.time.Instant`, `java.time.Year`
- Perfect for your `created_at`, `updated_at` fields

#### C. JPQL Enhancements
- `UNION`, `INTERSECT`, `EXCEPT` operations
- Null precedence in ordering (`NULLS FIRST`, `NULLS LAST`)
- `getSingleResultOrNull()` - no more try-catch for optional queries

#### D. Type-Safe Options API (NEW in 3.2)
```java
// Old way (error-prone)
em.find(Order.class, id, Map.of("jakarta.persistence.lock.timeout", 1000));

// New way (type-safe)
em.find(Order.class, id, Timeout.seconds(1));
```

#### E. Schema Manager API (NEW in 3.2)
```java
// Programmatically validate schema at runtime or in tests
SchemaManager schemaManager = emf.unwrap(SchemaManager.class);
schemaManager.validate(); // Ensures JPA entities match DB schema
```

**Verdict:** ✅ JPA 3.2 is production-ready (released April 2024) and perfect for your use case.

---

### 2. jsonschema2pojo for JPA Annotation Generation

**Capabilities:**

#### ✅ What It Does Well:
- Generates POJOs from JSON Schema
- Supports Jackson annotations out-of-the-box
- Supports JSR-303 validation annotations
- Configurable via Maven/Gradle plugin
- Active maintenance (latest: 1.2.1, Nov 2023)

#### ❌ What It Lacks:
- **No native JPA annotation support** (Entity, Id, Column, etc.)
- **No relationship mapping** (ManyToOne, OneToMany)
- **No index definition support**
- **No JSONB column type support**

#### 🔧 Workarounds Available:

**Option 1: Custom Annotator**
```xml
<plugin>
    <groupId>org.jsonschema2pojo</groupId>
    <artifactId>jsonschema2pojo-maven-plugin</artifactId>
    <configuration>
        <customAnnotator>com.yourcompany.JpaAnnotator</customAnnotator>
    </configuration>
</plugin>
```

**Option 2: Third-Party Library**
- `helperjs2pojo` - adds Lombok + JPA support
- Requires schema extensions: `"entity": true`, `"id": "fieldName"`

**Option 3: Post-Processing with MapStruct**
- Generate basic POJOs
- Manually create JPA entities
- Use MapStruct to convert between them

#### 📋 JSON Schema Hints Required (Minimum):

For `helperjs2pojo` custom annotator approach:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Order",
  "type": "object",
  "entity": true,  // ← Marks as JPA entity
  "table": "orders",
  "properties": {
    "id": {
      "type": "integer",
      "format": "int64",
      "id": true,  // ← Marks as @Id
      "generatedValue": "IDENTITY"
    },
    "orderNumber": {
      "type": "string",
      "maxLength": 50,
      "unique": true,
      "column": "order_number"  // ← Column name
    },
    "accountId": {
      "type": "integer",
      "format": "int64",
      "manyToOne": "Account",  // ← Relationship hint
      "joinColumn": "account_id"
    },
    "status": {
      "type": "string",
      "enum": ["PENDING", "CONFIRMED", "IN_TRANSIT", "DELIVERED"],
      "enumerated": "STRING"
    }
  },
  "required": ["orderNumber", "accountId"]
}
```

**Verdict:** ⚠️ jsonschema2pojo requires significant custom annotator work for JPA. Not ideal for production.

---

### 3. Alternative POJO Generation Approaches

#### Option A: Hibernate JPA Static Metamodel Generator (RECOMMENDED)

**How It Works:**
1. You manually write JPA entities (or generate basic POJOs)
2. Hibernate generates type-safe metamodel classes at compile time
3. Use metamodel for type-safe queries

**Benefits:**
- ✅ **Compile-time query validation** - no runtime query errors
- ✅ **IDE autocomplete** for all entity fields
- ✅ **Refactoring safety** - renaming fields updates all queries
- ✅ **Zero runtime overhead**
- ✅ **Official Hibernate tool** (well-maintained)

**Example:**
```java
// Your entity (manually created or template-generated)
@Entity
public class Order {
    @Id private Long id;
    private String orderNumber;
    private OrderStatus status;
}

// Hibernate generates Order_.java automatically
@StaticMetamodel(Order.class)
public class Order_ {
    public static volatile SingularAttribute<Order, Long> id;
    public static volatile SingularAttribute<Order, String> orderNumber;
    public static volatile SingularAttribute<Order, OrderStatus> status;
}

// Type-safe query (compile-time checked!)
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Order> query = cb.createQuery(Order.class);
Root<Order> order = query.from(Order.class);
query.where(cb.equal(order.get(Order_.status), OrderStatus.PENDING));
```

**Setup:**
```xml
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-jpamodelgen</artifactId>
    <version>6.6.4.Final</version>
    <scope>provided</scope>
</dependency>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.hibernate.orm</groupId>
                <artifactId>hibernate-jpamodelgen</artifactId>
                <version>6.6.4.Final</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

#### Option B: Database-First (Hibernate Tools)

**Reverse engineer from PostgreSQL schema:**
- JBoss Tools (Eclipse plugin)
- IntelliJ IDEA Ultimate (built-in)
- Hibernate Tools CLI

**Pros:**
- ✅ Fast initial generation
- ✅ Accurately reflects database

**Cons:**
- ❌ Requires database to exist first
- ❌ Not "schema-first" approach
- ❌ Hard to version control the schema definition
- ❌ Generated code can be messy

**Verdict:** ❌ Not suitable for your schema-first migration approach.

#### Option C: jOOQ (Alternative to JPA)

**What It Is:**
- Type-safe SQL query builder
- Generates POJOs from database schema
- Works alongside or instead of JPA

**Pros:**
- ✅ Extreme type safety
- ✅ Write SQL directly (no ORM abstraction)
- ✅ Great for complex queries

**Cons:**
- ❌ Database-first only
- ❌ Learning curve
- ❌ Doesn't fit "schema-first" approach

**Verdict:** ❌ Not suitable for your use case.

---

### 4. Schema Validation at Build Time

**Challenge:** Ensure generated POJOs match your standardized JSON Schema

**Solution: Custom Maven Plugin + json-schema-validator**

#### Implementation:

**Step 1: Define JSON Schemas**
```
src/main/resources/schemas/
├── order.schema.json
├── account.schema.json
├── driver.schema.json
├── fleet.schema.json
├── user.schema.json
├── pod.schema.json
└── billing.schema.json
```

**Step 2: Maven Plugin Configuration**
```xml
<plugin>
    <groupId>com.networknt</groupId>
    <artifactId>json-schema-validator-maven-plugin</artifactId>
    <version>1.0.57</version>
    <executions>
        <execution>
            <phase>validate</phase>
            <goals>
                <goal>validate-schema</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <schemaDirectory>${project.basedir}/src/main/resources/schemas</schemaDirectory>
        <validateOnBuild>true</validateOnBuild>
    </configuration>
</plugin>
```

**Step 3: Custom Validation Test**
```java
@SpringBootTest
public class SchemaComplianceTest {

    @Test
    public void testOrderEntityMatchesSchema() throws Exception {
        // Load JSON schema
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
            .getSchema(getClass().getResourceAsStream("/schemas/order.schema.json"));

        // Convert JPA entity to JSON (using Jackson)
        Order order = Order.builder()
            .orderNumber("ORD-001")
            .status(OrderStatus.PENDING)
            .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        JsonNode orderJson = mapper.valueToTree(order);

        // Validate
        Set<ValidationMessage> errors = schema.validate(orderJson);
        assertTrue(errors.isEmpty(),
            "Order entity does not match schema: " + errors);
    }
}
```

**Step 4: Gradle Alternative**
```groovy
plugins {
    id 'com.github.alenkacz.gradle-json-validator' version '1.0.0'
}

validateJson {
    targetJsonDirectory = file('src/test/resources/test-data')
    jsonSchemaFile = file('src/main/resources/schemas/order.schema.json')
}

build.dependsOn validateJson
```

**Available Libraries:**
- `com.networknt:json-schema-validator` (most active, supports draft V7/V2019-09/V2020-12)
- `com.github.fge:json-schema-validator` (older but stable)
- `io.rest-assured:json-schema-validator` (for API tests)

**Verdict:** ✅ Build-time schema validation is achievable with existing Maven/Gradle plugins.

---

### 5. Hexagonal Architecture with JPA

**Core Principle:** Keep domain logic independent of persistence mechanism

#### Recommended Structure:

```
src/main/java/com/fleetenable/globalsearch/
│
├── domain/                          # Core business logic (no JPA!)
│   ├── model/
│   │   ├── Order.java              # Pure domain entity (no @Entity)
│   │   ├── Account.java
│   │   └── ...
│   ├── port/
│   │   ├── OrderRepository.java    # Interface (no Spring Data)
│   │   └── AccountRepository.java
│   └── service/
│       ├── OrderService.java       # Business logic
│       └── GlobalSearchService.java
│
├── application/                     # Use cases / application services
│   ├── dto/
│   │   ├── CreateOrderRequest.java
│   │   └── SearchRequest.java
│   └── usecase/
│       ├── CreateOrderUseCase.java
│       └── GlobalSearchUseCase.java
│
└── infrastructure/                  # External concerns (JPA, REST, etc.)
    ├── persistence/
    │   ├── entity/                  # JPA entities (with @Entity)
    │   │   ├── OrderEntity.java
    │   │   ├── AccountEntity.java
    │   │   └── ...
    │   ├── repository/              # Spring Data JPA repositories
    │   │   ├── JpaOrderRepository.java
    │   │   └── OrderRepositoryAdapter.java  # Implements domain port
    │   └── mapper/                  # MapStruct mappers
    │       ├── OrderMapper.java     # Order ↔ OrderEntity
    │       └── AccountMapper.java
    ├── web/
    │   ├── controller/
    │   │   └── OrderController.java
    │   └── dto/
    │       └── OrderResponse.java
    └── config/
        └── PersistenceConfig.java
```

#### Example Code:

**Domain Model (Pure Java):**
```java
package com.fleetenable.globalsearch.domain.model;

import java.time.Instant;
import java.util.List;

public class Order {
    private final Long id;
    private final String orderNumber;
    private final OrderStatus status;
    private final Account account;
    private final Instant createdAt;

    // Constructor, getters, business methods

    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING ||
               status == OrderStatus.CONFIRMED;
    }

    public Order cancel() {
        if (!canBeCancelled()) {
            throw new IllegalStateException("Order cannot be cancelled");
        }
        return new Order(id, orderNumber, OrderStatus.CANCELLED,
                        account, createdAt);
    }
}
```

**Domain Repository Interface (Port):**
```java
package com.fleetenable.globalsearch.domain.port;

import com.fleetenable.globalsearch.domain.model.Order;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long id);
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByStatus(OrderStatus status);
    void delete(Order order);
}
```

**JPA Entity (Infrastructure):**
```java
package com.fleetenable.globalsearch.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // JPA-specific annotations, no business logic
}
```

**Repository Adapter (Infrastructure):**
```java
package com.fleetenable.globalsearch.infrastructure.persistence.repository;

import com.fleetenable.globalsearch.domain.model.Order;
import com.fleetenable.globalsearch.domain.port.OrderRepository;
import com.fleetenable.globalsearch.infrastructure.persistence.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final JpaOrderRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    // ... other methods
}
```

**Spring Data JPA Repository:**
```java
package com.fleetenable.globalsearch.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaOrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);
}
```

#### Benefits of Hexagonal Architecture for Your Use Case:

✅ **Schema Standardization:** Domain models define the "standard" schema
✅ **Migration Flexibility:** Easy to support MongoDB and PostgreSQL simultaneously
✅ **Testing:** Domain logic tests don't need database
✅ **Future-Proof:** Swap PostgreSQL for another DB without changing domain
✅ **Clear Boundaries:** Schema changes happen in infrastructure, not domain

#### Challenges:

⚠️ **More Boilerplate:** Need mappers between domain and persistence models
⚠️ **Learning Curve:** Team needs to understand hexagonal architecture
⚠️ **Performance:** Extra mapping layer (mitigated by MapStruct)

**Verdict:** ✅ **HIGHLY RECOMMENDED** for your MongoDB → PostgreSQL migration.

---

### 6. MapStruct for Entity Mapping

**What It Is:**
- Compile-time bean mapper (no reflection!)
- Generates mapping code during compilation
- Zero runtime overhead

**Why You Need It:**

With hexagonal architecture, you'll have:
- **Domain Models** (pure Java, no JPA)
- **JPA Entities** (infrastructure layer)
- Need efficient mapping between them

#### Example:

**MapStruct Mapper Interface:**
```java
package com.fleetenable.globalsearch.infrastructure.persistence.mapper;

import com.fleetenable.globalsearch.domain.model.Order;
import com.fleetenable.globalsearch.infrastructure.persistence.entity.OrderEntity;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface OrderMapper {

    @Mapping(target = "account", ignore = true) // Handle separately
    Order toDomain(OrderEntity entity);

    @Mapping(target = "account", ignore = true)
    OrderEntity toEntity(Order domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDomain(Order domain, @MappingTarget OrderEntity entity);
}
```

**Generated Code (by MapStruct):**
```java
@Component
public class OrderMapperImpl implements OrderMapper {

    @Override
    public Order toDomain(OrderEntity entity) {
        if (entity == null) return null;

        return Order.builder()
            .id(entity.getId())
            .orderNumber(entity.getOrderNumber())
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    @Override
    public OrderEntity toEntity(Order domain) {
        if (domain == null) return null;

        OrderEntity entity = new OrderEntity();
        entity.setId(domain.getId());
        entity.setOrderNumber(domain.getOrderNumber());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
```

**Maven Configuration:**
```xml
<properties>
    <mapstruct.version>1.6.3</mapstruct.version>
    <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${mapstruct.version}</version>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.36</version>
                    </path>
                    <!-- Lombok + MapStruct compatibility -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>${lombok-mapstruct-binding.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Benefits:**
- ✅ **Compile-time generation** - no reflection overhead
- ✅ **Type-safe** - compiler catches field mismatches
- ✅ **Fast** - as fast as hand-written code
- ✅ **Maintainable** - auto-updates when fields change
- ✅ **Lombok compatible** - works with @Builder, @Data, etc.

**Verdict:** ✅ **ESSENTIAL** for hexagonal architecture with domain/persistence separation.

---

## 🎯 Recommended Production-Grade Solution

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│  JSON Schema (Source of Truth)                             │
│  src/main/resources/schemas/*.schema.json                   │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ├──► Build-Time Validation (Maven Plugin)
                 │    ✓ Ensures consistency
                 │
                 ├──► Domain Model Generation (Template/Manual)
                 │    → Pure Java domain entities
                 │
                 └──► JPA Entity Generation (Template + Manual)
                      → @Entity annotated persistence models

┌─────────────────────────────────────────────────────────────┐
│  Hexagonal Architecture Layers                             │
├─────────────────────────────────────────────────────────────┤
│  domain/                                                    │
│  ├── model/          ← Pure domain entities (no JPA)       │
│  ├── port/           ← Repository interfaces               │
│  └── service/        ← Business logic                      │
├─────────────────────────────────────────────────────────────┤
│  infrastructure/persistence/                                │
│  ├── entity/         ← JPA entities (@Entity)              │
│  ├── repository/     ← Spring Data JPA + Adapters          │
│  └── mapper/         ← MapStruct mappers                   │
└─────────────────────────────────────────────────────────────┘
         │
         └──► Hibernate JPA Static Metamodel Generator
              ✓ Compile-time query validation
              ✓ Type-safe Criteria API
```

### Step-by-Step Implementation

#### Phase 1: Schema Standardization (Week 1-2)

**1.1. Define JSON Schemas for All Entities**

Create `src/main/resources/schemas/order.schema.json`:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "https://fleetenable.com/schemas/order.json",
  "title": "Order",
  "description": "Shipment order entity",
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "format": "int64",
      "description": "Primary key"
    },
    "orderNumber": {
      "type": "string",
      "maxLength": 50,
      "pattern": "^ORD-[0-9]{6}$",
      "description": "Unique order number"
    },
    "status": {
      "type": "string",
      "enum": ["PENDING", "CONFIRMED", "IN_TRANSIT", "DELIVERED", "CANCELLED"]
    },
    "accountId": {
      "type": "integer",
      "format": "int64"
    },
    "hawbNumbers": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "origin": {
      "$ref": "#/definitions/Address"
    },
    "destination": {
      "$ref": "#/definitions/Address"
    },
    "createdAt": {
      "type": "string",
      "format": "date-time"
    }
  },
  "required": ["orderNumber", "status", "accountId", "createdAt"],
  "definitions": {
    "Address": {
      "type": "object",
      "properties": {
        "street": { "type": "string" },
        "city": { "type": "string" },
        "state": { "type": "string", "maxLength": 2 },
        "zip": { "type": "string", "pattern": "^[0-9]{5}$" },
        "country": { "type": "string", "maxLength": 3 }
      },
      "required": ["city", "state", "country"]
    }
  }
}
```

**1.2. Version Control Schemas**
- Store in Git
- Use semantic versioning (v1.0.0, v1.1.0)
- Create schema changelog

#### Phase 2: Build-Time Validation (Week 2)

**2.1. Add Schema Validation Maven Plugin**

Create custom Maven plugin or use existing:
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>validate-schemas</id>
            <phase>validate</phase>
            <goals>
                <goal>java</goal>
            </goals>
            <configuration>
                <mainClass>com.fleetenable.schema.SchemaValidator</mainClass>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**2.2. Write Schema Validator**
```java
public class SchemaValidator {
    public static void main(String[] args) throws Exception {
        File schemaDir = new File("src/main/resources/schemas");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

        for (File schemaFile : schemaDir.listFiles(f -> f.getName().endsWith(".json"))) {
            try {
                JsonSchema schema = factory.getSchema(new FileInputStream(schemaFile));
                System.out.println("✓ Valid schema: " + schemaFile.getName());
            } catch (Exception e) {
                System.err.println("✗ Invalid schema: " + schemaFile.getName());
                throw e;
            }
        }
    }
}
```

#### Phase 3: Domain Model Creation (Week 3-4)

**3.1. Create Domain Entities (Manual or Template-Based)**

Option A: **Manual Creation** (Recommended for control)
```java
package com.fleetenable.globalsearch.domain.model;

import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class Order {
    Long id;
    String orderNumber;
    OrderStatus status;
    Long accountId;
    List<String> hawbNumbers;
    Address origin;
    Address destination;
    Instant createdAt;

    // Business methods
    public Order markAsDelivered() {
        return toBuilder()
            .status(OrderStatus.DELIVERED)
            .build();
    }
}

@Value
@Builder
public class Address {
    String street;
    String city;
    String state;
    String zip;
    String country;
}
```

Option B: **Template-Based Generation** (For speed)

Create Freemarker/Velocity template: `domain-entity.ftl`
```java
package com.fleetenable.globalsearch.domain.model;

import lombok.Builder;
import lombok.Value;
<#list imports as import>
import ${import};
</#list>

@Value
@Builder(toBuilder = true)
public class ${entityName} {
    <#list fields as field>
    ${field.type} ${field.name};
    </#list>
}
```

**3.2. Add Validation Tests**
```java
@Test
public void testOrderDomainModelMatchesSchema() throws Exception {
    JsonSchema schema = loadSchema("/schemas/order.schema.json");

    Order order = Order.builder()
        .orderNumber("ORD-123456")
        .status(OrderStatus.PENDING)
        .accountId(1L)
        .createdAt(Instant.now())
        .build();

    JsonNode json = objectMapper.valueToTree(order);
    Set<ValidationMessage> errors = schema.validate(json);

    assertThat(errors).isEmpty();
}
```

#### Phase 4: JPA Entity Creation (Week 4-5)

**4.1. Create JPA Entities**
```java
package com.fleetenable.globalsearch.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_order_number", columnList = "order_number"),
    @Index(name = "idx_orders_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Type(JsonBinaryType.class)
    @Column(name = "hawb_numbers", columnDefinition = "jsonb")
    private List<String> hawbNumbers;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private AddressDTO origin;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private AddressDTO destination;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

**4.2. Enable Hibernate Static Metamodel Generation**

Already configured in your `pom.xml` via annotation processor!

Generates `OrderEntity_.java`:
```java
@StaticMetamodel(OrderEntity.class)
public class OrderEntity_ {
    public static volatile SingularAttribute<OrderEntity, Long> id;
    public static volatile SingularAttribute<OrderEntity, String> orderNumber;
    public static volatile SingularAttribute<OrderEntity, OrderStatus> status;
    // ... all fields
}
```

#### Phase 5: MapStruct Mappers (Week 5)

**5.1. Add MapStruct Dependency**
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>
```

**5.2. Create Mappers**
```java
@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "account.id", target = "accountId")
    Order toDomain(OrderEntity entity);

    @Mapping(target = "account", ignore = true) // Set via service
    OrderEntity toEntity(Order domain);

    List<Order> toDomainList(List<OrderEntity> entities);
}
```

#### Phase 6: Repository Adapters (Week 6)

**6.1. Domain Port**
```java
package com.fleetenable.globalsearch.domain.port;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findByStatus(OrderStatus status);
}
```

**6.2. Spring Data JPA Repository**
```java
package com.fleetenable.globalsearch.infrastructure.persistence.repository;

public interface JpaOrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);
    List<OrderEntity> findByStatus(OrderStatus status);
}
```

**6.3. Adapter Implementation**
```java
@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {
    private final JpaOrderRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return mapper.toDomainList(jpaRepository.findByStatus(status));
    }
}
```

---

## 📦 Complete Maven Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <properties>
        <java.version>21</java.version>
        <spring-boot.version>3.4.1</spring-boot.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <lombok.version>1.18.36</lombok.version>
        <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
        <hibernate-jpamodelgen.version>6.6.4.Final</hibernate-jpamodelgen.version>
        <json-schema-validator.version>1.5.3</json-schema-validator.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <!-- JSONB Support -->
        <dependency>
            <groupId>io.hypersistence</groupId>
            <artifactId>hypersistence-utils-hibernate-63</artifactId>
            <version>3.9.0</version>
        </dependency>

        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Hibernate JPA Static Metamodel Generator -->
        <dependency>
            <groupId>org.hibernate.orm</groupId>
            <artifactId>hibernate-jpamodelgen</artifactId>
            <version>${hibernate-jpamodelgen.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- JSON Schema Validator (for tests) -->
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>json-schema-validator</artifactId>
            <version>${json-schema-validator.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <annotationProcessorPaths>
                        <!-- Order matters! Lombok must be first -->
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <!-- Lombok + MapStruct binding -->
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>${lombok-mapstruct-binding.version}</version>
                        </path>
                        <!-- MapStruct processor -->
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                        <!-- Hibernate JPA Metamodel Generator -->
                        <path>
                            <groupId>org.hibernate.orm</groupId>
                            <artifactId>hibernate-jpamodelgen</artifactId>
                            <version>${hibernate-jpamodelgen.version}</version>
                        </path>
                    </annotationProcessorPaths>
                    <compilerArgs>
                        <arg>-Amapstruct.defaultComponentModel=spring</arg>
                        <arg>-Amapstruct.unmappedTargetPolicy=WARN</arg>
                    </compilerArgs>
                </configuration>
            </plugin>

            <!-- Schema Validation Plugin -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <id>validate-json-schemas</id>
                        <phase>validate</phase>
                        <goals>
                            <goal>java</goal>
                        </goals>
                        <configuration>
                            <mainClass>com.fleetenable.globalsearch.schema.SchemaValidator</mainClass>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 🧪 Testing Strategy

### 1. Schema Validation Tests
```java
@SpringBootTest
public class SchemaComplianceTest {

    private ObjectMapper objectMapper;
    private JsonSchemaFactory schemaFactory;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    @Test
    void domainModelShouldMatchSchema() throws Exception {
        JsonSchema schema = loadSchema("/schemas/order.schema.json");

        Order order = Order.builder()
            .orderNumber("ORD-123456")
            .status(OrderStatus.PENDING)
            .accountId(1L)
            .createdAt(Instant.now())
            .build();

        JsonNode json = objectMapper.valueToTree(order);
        Set<ValidationMessage> errors = schema.validate(json);

        assertThat(errors).isEmpty();
    }

    @Test
    void jpaEntityShouldMatchSchema() throws Exception {
        // Similar test for JPA entities
    }
}
```

### 2. Mapper Tests
```java
@SpringBootTest
public class OrderMapperTest {

    @Autowired
    private OrderMapper mapper;

    @Test
    void shouldMapEntityToDomain() {
        OrderEntity entity = OrderEntity.builder()
            .id(1L)
            .orderNumber("ORD-123456")
            .status(OrderStatus.PENDING)
            .createdAt(Instant.now())
            .build();

        Order domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getOrderNumber()).isEqualTo("ORD-123456");
        assertThat(domain.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
```

### 3. Repository Tests
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class OrderRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private JpaOrderRepository jpaRepository;

    @Autowired
    private OrderMapper mapper;

    private OrderRepositoryAdapter adapter;

    @BeforeEach
    void setup() {
        adapter = new OrderRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    void shouldSaveAndRetrieveOrder() {
        Order order = Order.builder()
            .orderNumber("ORD-123456")
            .status(OrderStatus.PENDING)
            .accountId(1L)
            .createdAt(Instant.now())
            .build();

        Order saved = adapter.save(order);
        Optional<Order> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getOrderNumber()).isEqualTo("ORD-123456");
    }
}
```

---

## 📊 Pros & Cons Analysis

### Recommended Approach: Hexagonal + MapStruct + JPA

| Aspect | Pros | Cons |
|--------|------|------|
| **Schema First** | ✅ JSON Schema as single source of truth | ⚠️ Requires discipline to keep in sync |
| **Domain Purity** | ✅ No JPA pollution in domain model | ❌ More boilerplate (2 entity types) |
| **Testability** | ✅ Domain tests need no database | ❌ Need to test mappers separately |
| **Migration** | ✅ Easy to support Mongo + Postgres | ⚠️ Dual persistence during migration |
| **Type Safety** | ✅ Compile-time validation everywhere | ❌ Learning curve for team |
| **Performance** | ✅ MapStruct = zero overhead | ⚠️ Extra mapping step |
| **Maintainability** | ✅ Clear separation of concerns | ❌ More files to manage |
| **Future-Proof** | ✅ Easy to swap persistence | ⚠️ Upfront architecture effort |

---

## 🚀 Migration Roadmap

### Phase 1: Foundation (Weeks 1-2)
- [ ] Define JSON Schemas for all 7 entities
- [ ] Setup schema validation in build pipeline
- [ ] Configure Maven with all annotation processors
- [ ] Create domain model package structure

### Phase 2: Domain Layer (Weeks 3-4)
- [ ] Create domain entities (pure Java)
- [ ] Create domain repository interfaces (ports)
- [ ] Create domain services with business logic
- [ ] Write domain model tests with schema validation

### Phase 3: Persistence Layer (Weeks 5-6)
- [ ] Create JPA entities with annotations
- [ ] Setup Flyway migrations for PostgreSQL
- [ ] Create Spring Data JPA repositories
- [ ] Generate Hibernate static metamodel

### Phase 4: Integration (Weeks 7-8)
- [ ] Create MapStruct mappers
- [ ] Implement repository adapters
- [ ] Wire up dependency injection
- [ ] Write integration tests

### Phase 5: Dual-Write (Weeks 9-10)
- [ ] Implement dual-write to MongoDB + PostgreSQL
- [ ] Add consistency checks
- [ ] Monitor for discrepancies
- [ ] Validate data integrity

### Phase 6: Cutover (Weeks 11-12)
- [ ] Migrate historical data
- [ ] Switch reads to PostgreSQL
- [ ] Disable MongoDB writes
- [ ] Decommission MongoDB

---

## 🎓 Learning Resources

### JSON Schema
- [JSON Schema Official Site](https://json-schema.org/)
- [Understanding JSON Schema](https://json-schema.org/understanding-json-schema/)

### JPA 3.2
- [Jakarta Persistence 3.2 Spec](https://jakarta.ee/specifications/persistence/3.2/)
- [Baeldung: Jakarta Persistence 3.2](https://www.baeldung.com/jakarta-persistence-3-2)

### Hexagonal Architecture
- [Baeldung: Hexagonal Architecture with Spring](https://www.baeldung.com/hexagonal-architecture-ddd-spring)
- [Alistair Cockburn: Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

### MapStruct
- [MapStruct Reference Guide](https://mapstruct.org/documentation/stable/reference/html/)
- [Baeldung: Quick Guide to MapStruct](https://www.baeldung.com/mapstruct)

### Hibernate Static Metamodel
- [Hibernate JPA Metamodel Generator](https://docs.jboss.org/hibernate/orm/6.6/topical/html_single/metamodelgen/MetamodelGenerator.html)

---

## ✅ Final Recommendations

### 1. Do NOT use jsonschema2pojo for JPA generation
- **Reason:** No native JPA support, requires too much custom work
- **Alternative:** Manual JPA entities + Hibernate metamodel generator

### 2. DO use JSON Schema as source of truth
- Store in version control
- Validate at build time
- Use for API documentation (OpenAPI integration)

### 3. DO implement Hexagonal Architecture
- **Critical for your migration:** Allows MongoDB + PostgreSQL coexistence
- Clearer separation between domain and infrastructure
- Easier to test

### 4. DO use MapStruct
- **Essential** for domain ↔ persistence mapping
- Zero runtime overhead
- Compile-time safety

### 5. DO use Hibernate JPA Static Metamodel Generator
- **Free type safety** for queries
- No additional runtime dependencies
- Catches errors at compile time

### 6. Schema Validation Approach
Implement at **three levels**:
1. **Build time:** Validate JSON schemas are well-formed
2. **Test time:** Validate entities match schemas
3. **Runtime (optional):** Validate incoming API requests

---

## 📝 Summary

**Your Question:** How many hints do I need in JSON Schema for JPA annotations with jsonschema2pojo?

**My Answer:** **Don't use jsonschema2pojo for JPA** - it's not the right tool. Instead:

1. **JSON Schema** → Source of truth for data structure
2. **Manual/Template JPA entities** → With full control over annotations
3. **Hibernate JPA Metamodel Generator** → Compile-time type safety
4. **MapStruct** → Efficient domain ↔ persistence mapping
5. **Hexagonal Architecture** → Clean separation for migration
6. **Build-time schema validation** → Ensure consistency

This gives you:
- ✅ Full control over JPA annotations
- ✅ Type-safe queries (compile-time)
- ✅ Clean architecture for migration
- ✅ Zero runtime overhead
- ✅ Easy testing
- ✅ Production-grade solution

**Next Steps:**
1. Review this document
2. Decide on hexagonal architecture (recommended: YES)
3. Start with Phase 1: JSON Schema definition
4. I can help implement any of these components

Want me to create a concrete implementation example for one of your entities (e.g., Order)?
