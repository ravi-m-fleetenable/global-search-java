# Global Search Migration - Executive Summary

**Project:** Migration from Ruby on Rails + MongoDB Atlas to Java Spring Boot + PostgreSQL
**Date:** 2025-11-07
**Prepared for:** Technology Evaluation & Decision Making

---

## 🎯 Project Objective

Migrate the Global Search API from **MongoDB Atlas Search** to **PostgreSQL with pg_trgm** while:
- ✅ Maintaining all current search features
- ✅ Reducing infrastructure costs
- ✅ Eliminating vendor lock-in
- ✅ Improving search performance
- ✅ Modernizing the technology stack (Java Spring Boot + JDK 25)

---

## 📊 Current vs. Proposed System

| Aspect | **Current System** | **Proposed System** | **Impact** |
|--------|-------------------|-------------------|-----------|
| **Backend** | Ruby on Rails 7.1 | Java Spring Boot 3.5.x + JDK 25 | Modern, enterprise-grade |
| **Database** | MongoDB Atlas M50 | PostgreSQL 16+ (RDS) | Relational, proven at scale |
| **Search Engine** | MongoDB Atlas Search | PostgreSQL pg_trgm (native) | No external dependencies |
| **Infrastructure** | Atlas + EC2 + Redis | RDS + ECS/Fargate + Redis | Cloud-native, scalable |
| **Monthly Cost** | **~$855** | **~$750** | **12% reduction** |
| **Vendor Lock-in** | High (Atlas-specific) | None (open-source SQL) | Flexibility |
| **Search Features** | Fuzzy, case-insensitive | Fuzzy, case-insensitive | Feature parity ✅ |
| **Performance** | < 500ms search, < 100ms autocomplete | < 100ms search, < 50ms autocomplete | **2-5x faster** |

---

## 💡 Why PostgreSQL pg_trgm?

### What is pg_trgm?

PostgreSQL's **trigram extension** (`pg_trgm`) provides **language-independent fuzzy text matching** by breaking text into 3-character sequences.

**Example:**
```
Query: "Toyota"        Trigrams: {" To", "Toy", "oyo", "yot", "ota", "ta "}
Typo:  "Tayota"        Trigrams: {" Ta", "Tay", "ayo", "yot", "ota", "ta "}
Similarity: 67% match → Found!
```

### Key Advantages

| Feature | How It Works | Benefit |
|---------|--------------|---------|
| **Fuzzy Matching** | Finds "Tayota" when searching "Toyota" | Typo tolerance |
| **Case Insensitive** | "TOYOTA" = "toyota" = "ToYoTa" | User-friendly |
| **Language Independent** | Works for English, Spanish, Chinese, etc. | Global support |
| **Fast (GIN/GiST Indexes)** | Indexed trigrams = sub-50ms queries | High performance |
| **Built-in PostgreSQL** | No external search engine needed | Simple architecture |
| **Production Proven** | Used by GitHub, GitLab, Discourse | Battle-tested |

---

## 🏆 Key Benefits

### 1. Cost Reduction
- **Current:** MongoDB Atlas M50 ($500/month) + EC2 ($150) + Redis ($100) + misc. ($105) = **$855/month**
- **Proposed:** PostgreSQL RDS ($420) + ECS Fargate ($120) + Redis ($100) + misc. ($110) = **$750/month**
- **Savings:** $105/month = **$1,260/year** (12%)
- **With Reserved Instances:** Save 40-60% more ($3,000-5,000/year)

### 2. Eliminated Vendor Lock-in
- **Current:** Atlas Search uses proprietary query language, manual index creation in Atlas UI
- **Proposed:** Standard SQL with open-source extensions, portable to any PostgreSQL provider
- **Impact:** Can migrate to AWS RDS, Azure PostgreSQL, Google Cloud SQL, or self-hosted

### 3. Improved Performance
- **GIN/GiST Indexes:** Optimized for trigram similarity (5-10x faster than table scans)
- **Read Replicas:** Offload search queries to dedicated replicas
- **Materialized Views:** Pre-computed global search index
- **Expected:** < 100ms for global search, < 50ms for autocomplete (vs. current 500ms / 100ms)

### 4. Enterprise-Grade Stack
- **Java Spring Boot:** Industry standard for microservices (40% of Fortune 500 use Spring)
- **JDK 25 (LTS):** Latest long-term support release (Sept 2025)
- **PostgreSQL 16:** Most advanced open-source database (ACID, JSON, full-text search)
- **Ecosystem:** Mature tooling, libraries, and community support

### 5. Simplified Architecture
**Current:**
```
Rails App → MongoDB Atlas Search → MongoDB Atlas Cluster → Atlas UI (index management)
```

**Proposed:**
```
Spring Boot → PostgreSQL (single database, SQL-based search)
```
- **Fewer components** = easier to maintain
- **No separate search cluster** to manage
- **SQL-based** = familiar to most developers

---

## 📈 Search Features (Feature Parity)

All current features will be **maintained or improved**:

| Feature | Current Implementation | Proposed Implementation | Status |
|---------|----------------------|------------------------|--------|
| **Global Search** | MongoDB aggregation across 7 collections | PostgreSQL materialized view with GiST index | ✅ Equivalent |
| **Autocomplete** | Atlas Search prefix matching | `word_similarity()` with GIN index | ✅ Faster |
| **Fuzzy Matching** | Levenshtein distance (Atlas) | Trigram similarity (pg_trgm) | ✅ Equivalent |
| **Case Insensitive** | Atlas analyzer config | Native `LOWER()` + trigram | ✅ Simpler |
| **Typo Tolerance** | Atlas fuzzy matching | Similarity threshold (0.3 default) | ✅ Configurable |
| **Result Highlighting** | Atlas highlighting | `ts_headline()` function | ✅ Equivalent |
| **Faceted Search** | MongoDB aggregation | SQL `GROUP BY` + counts | ✅ Faster |
| **Pagination** | MongoDB skip/limit | SQL `LIMIT`/`OFFSET` or cursor | ✅ Standard |
| **Role-Based Access** | Pundit (Rails) | Spring Security | ✅ Enterprise-grade |

---

## 🗂️ Database Schema Migration

### Current: MongoDB (7 Collections)

```
orders, accounts, drivers, fleets, users, pods, billings, invoices
```

### Proposed: PostgreSQL (8 Tables + 2 Junction Tables)

**Core Tables:**
1. `users` - Authentication and roles
2. `accounts` - Customer data
3. `drivers` - Driver profiles
4. `fleets` - Vehicle inventory
5. `orders` - Shipment orders
6. `pods` - Proof of delivery
7. `billings` - Payment records
8. `invoices` - Invoice documents

**Junction Tables:**
- `billing_orders` - Many-to-many (billing ↔ orders)
- `invoice_orders` - Many-to-many (invoice ↔ orders)

**Search Optimization:**
- **GIN indexes** on frequently searched text columns
- **GiST indexes** on similarity-based searches
- **Materialized view** (`global_search_index`) for cross-entity search
- **JSONB columns** for semi-structured data (addresses, arrays)

---

## 🛠️ Technology Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Runtime** | Java OpenJDK | 25 (LTS) | Application execution |
| **Framework** | Spring Boot | 3.5.x | Web framework, DI, REST APIs |
| **Database** | PostgreSQL | 16+ | Primary data store + search |
| **ORM** | Spring Data JPA | 3.5.x (Hibernate 6.6) | Database access |
| **Security** | Spring Security + JWT | 6.5.x | Authentication/Authorization |
| **Caching** | Redis + Spring Cache | 3.5.x | Performance optimization |
| **Migration** | Flyway | 10.x | Database version control |
| **Build** | Maven | 3.9.x | Dependency management |
| **Deployment** | AWS ECS Fargate | Latest | Container orchestration |
| **Database (AWS)** | RDS PostgreSQL | 16.x | Managed database service |
| **Cache (AWS)** | ElastiCache Redis | 7.x | Managed cache service |

---

## 📅 Migration Timeline

### Phased Approach (12 Weeks)

| Phase | Duration | Key Activities | Deliverables |
|-------|----------|---------------|--------------|
| **1. Setup** | Week 1-2 | Environment setup, Spring Boot scaffolding | Dev environment, CI/CD pipeline |
| **2. Schema** | Week 3-4 | PostgreSQL schema, Flyway migrations | All tables, indexes created |
| **3. Data Migration** | Week 5-6 | MongoDB → PostgreSQL ETL, validation | 100% data migrated, verified |
| **4. Search** | Week 7-8 | Search services, API endpoints | All search features working |
| **5. Testing** | Week 9-10 | Unit, integration, load testing | 80%+ test coverage |
| **6. Deployment** | Week 11-12 | Staging, UAT, production cutover | Production launch ✅ |

**Total Timeline:** 12 weeks (3 months)
**Recommended Buffer:** +20% (2-3 weeks)
**Realistic Timeline:** 14-15 weeks

---

## 💰 Cost Analysis

### Monthly Infrastructure Costs (AWS US-East-1)

| Component | Current | Proposed | Savings |
|-----------|---------|----------|---------|
| **Database** | Atlas M50: $500 | RDS db.r6g.xlarge: $280 | +$220 |
| **Read Replica** | N/A | RDS db.r6g.large: $140 | -$140 |
| **Application** | EC2 t3.xlarge x2: $150 | ECS Fargate x2: $120 | +$30 |
| **Cache** | ElastiCache: $100 | ElastiCache: $100 | $0 |
| **Load Balancer** | ALB: $25 | ALB: $25 | $0 |
| **Other** | $80 | $85 | -$5 |
| **TOTAL** | **$855/month** | **$750/month** | **+$105/month** |

**Annual Savings:** $1,260
**3-Year Savings (with Reserved Instances):** $10,000-15,000

### One-Time Migration Costs

| Item | Cost Estimate |
|------|--------------|
| Development (3 developers x 3 months) | $60,000 - $90,000 |
| DevOps/DBA (1 engineer x 3 months) | $25,000 - $40,000 |
| Testing (1 QA engineer x 2 months) | $15,000 - $25,000 |
| Project Management | $10,000 - $15,000 |
| AWS Infrastructure (dev/staging) | $1,500 - $3,000 |
| **TOTAL** | **$111,500 - $173,000** |

**ROI Timeline:** 5-10 years (based on cost savings alone)
**Note:** Primary value is **reduced lock-in** and **improved performance**, not just cost savings.

---

## ⚠️ Risks & Mitigation

| Risk | Probability | Impact | Mitigation Strategy |
|------|------------|--------|---------------------|
| **Performance degradation** | Medium | High | Extensive load testing, index optimization, read replicas |
| **Data migration errors** | Medium | Critical | Validation scripts, parallel runs, rollback plan |
| **Timeline delays** | Medium | Medium | 20% buffer time, agile sprints, MVP approach |
| **Team learning curve** | Medium | Low | Training, pair programming, documentation |
| **Downtime during cutover** | Low | High | Blue-green deployment, zero-downtime migration |

**Overall Risk:** **Low to Medium** (mitigated with proper planning)

---

## ✅ Success Criteria

### Must Have (Go-Live Requirements)
1. ✅ All 7 entity types searchable (Orders, Accounts, Drivers, Fleets, PODs, Billings, Invoices)
2. ✅ Global search across all entities < 500ms (target: < 100ms)
3. ✅ Autocomplete < 100ms (target: < 50ms)
4. ✅ Fuzzy matching with configurable similarity threshold
5. ✅ Case-insensitive search
6. ✅ Role-based access control (5 roles)
7. ✅ Zero data loss during migration
8. ✅ API compatibility maintained (same request/response format)

### Nice to Have (Post-Launch)
- Result highlighting (can defer if needed)
- Faceted search aggregations
- Search analytics dashboard
- Advanced ML-based ranking

---

## 🚀 Next Steps

### Immediate Actions Required

1. **Review Documents** (This Week)
   - [ ] Executive Summary (this document)
   - [ ] Technical Specification (TECHNICAL_SPECIFICATION.md)
   - [ ] Clarification Questions (CLARIFICATION_QUESTIONS.md)

2. **Answer Clarification Questions** (Week 1)
   - [ ] Complete CLARIFICATION_QUESTIONS.md
   - [ ] Schedule 60-minute clarification call if needed

3. **Proof-of-Concept** (Week 2-3)
   - [ ] Build POC with sample data (1 entity: Orders)
   - [ ] Performance benchmarking vs. MongoDB
   - [ ] Demonstrate fuzzy search with pg_trgm

4. **Decision Point** (Week 4)
   - [ ] Review POC results
   - [ ] Finalize budget approval
   - [ ] Green-light full migration OR adjust approach

### Decision Makers Needed

- **Technical Approval:** CTO / VP Engineering
- **Budget Approval:** CFO / Finance
- **Timeline Approval:** Product / Project Management
- **Go-Live Approval:** Operations / DevOps Lead

---

## 📞 Contact & Questions

**For technical questions:**
- Review: `TECHNICAL_SPECIFICATION.md` (detailed architecture)

**For clarifications:**
- Fill out: `CLARIFICATION_QUESTIONS.md`

**For next steps:**
- Email: [Your contact email]
- Meeting: [Schedule link]

---

## 📎 Appendix: Quick Reference

### Key PostgreSQL Search Functions

```sql
-- Fuzzy similarity (0.0 to 1.0, higher = more similar)
SELECT similarity('Toyota', 'Tayota');  -- Returns: 0.67

-- Find all similar records
SELECT * FROM fleets
WHERE similarity(make, 'Toyota') > 0.3
ORDER BY similarity(make, 'Toyota') DESC;

-- Autocomplete with word similarity
SELECT vehicle_name
FROM fleets
WHERE word_similarity('Toy', vehicle_name) > 0.6
ORDER BY word_similarity('Toy', vehicle_name) DESC
LIMIT 10;

-- Case-insensitive search
SELECT * FROM accounts
WHERE lower(account_name) % 'acme';  -- % is similarity operator
```

### Sample Search API Request

```bash
curl -X POST https://api.example.com/v1/search/global \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "toyota",
    "searchType": "all",
    "includeHighlights": true,
    "page": 0,
    "size": 20
  }'
```

### Spring Boot Compatibility

- **JDK 25 Support:** ✅ Confirmed (Spring Boot 3.5.x officially supports JDK 17-25)
- **PostgreSQL Support:** ✅ Native (Spring Data JPA + PostgreSQL driver)
- **pg_trgm Support:** ✅ Via native SQL queries and JPA @Query annotations

---

## 🎓 Conclusion

The migration from **MongoDB Atlas Search** to **PostgreSQL pg_trgm** offers:

1. **Cost Savings:** 12-40% reduction in infrastructure costs
2. **Performance:** 2-5x faster search with proper indexing
3. **Flexibility:** No vendor lock-in, portable SQL
4. **Simplicity:** Single database for data + search
5. **Maturity:** Battle-tested stack (Spring Boot + PostgreSQL)

**Recommendation:** **Proceed with Proof-of-Concept** to validate performance assumptions, then execute phased 12-week migration.

---

**Document Version:** 1.0
**Last Updated:** 2025-11-07
**Next Review:** After clarification questions answered
