# Global Search Migration - Clarification Questions

**Project:** Ruby on Rails + MongoDB → Java Spring Boot + PostgreSQL
**Date:** 2025-11-07
**Status:** Awaiting Stakeholder Input

---

## Instructions

Please review and provide answers to the questions below. Your responses will help us:
- Finalize the technical design
- Provide accurate cost and timeline estimates
- Identify potential risks early
- Ensure the solution meets your exact requirements

**Priority Legend:**
- 🔴 **Critical** - Must answer before proceeding
- 🟡 **Important** - Needed for accurate estimates
- 🟢 **Nice to have** - Can be decided later

---

## Section A: Data Volume & Growth 🔴 CRITICAL

### A1. Current Data Volume
**Question:** How many total records exist in each collection?

- [ ] Orders: _____________ records
- [ ] Accounts: _____________ records
- [ ] Drivers: _____________ records
- [ ] Fleets: _____________ records
- [ ] Users: _____________ records
- [ ] PODs: _____________ records
- [ ] Billings: _____________ records
- [ ] Invoices: _____________ records

**Current database size:** _____________ GB

### A2. Growth Projections
**Question:** What is your expected growth rate?

- [ ] Records added per month: _____________
- [ ] Expected database size in 1 year: _____________ GB
- [ ] Expected database size in 3 years: _____________ GB

### A3. Migration Window
**Question:** What is the acceptable downtime window for migration?

- [ ] Zero downtime required (blue-green deployment)
- [ ] Up to 1 hour during off-peak hours
- [ ] Up to 4 hours on a weekend
- [ ] Up to 8 hours with advance notice
- [ ] Other: _____________

**Preferred migration date/window:** _____________

---

## Section B: Search Performance & Behavior 🔴 CRITICAL

### B1. Current Search Metrics
**Question:** What are your current search performance metrics?

**Daily search volume:**
- [ ] Average searches per day: _____________
- [ ] Peak searches per second: _____________
- [ ] Autocomplete requests per day: _____________

**Current latency (if known):**
- [ ] Average search latency (p50): _____________ ms
- [ ] 95th percentile latency (p95): _____________ ms
- [ ] 99th percentile latency (p99): _____________ ms

### B2. Search Expectations
**Question:** What should be the default search behavior?

- [ ] Exact match first, then fuzzy matches
- [ ] Fuzzy matching by default (typo tolerance)
- [ ] User can toggle between exact/fuzzy
- [ ] Other: _____________

### B3. Autocomplete Requirements
**Question:** Autocomplete configuration preferences?

- [ ] Search across all fields
- [ ] Search only primary identifiers (order numbers, account numbers, etc.)
- [ ] Configurable per entity type
- [ ] Number of suggestions to return: _____________ (default: 10)

**Is <100ms autocomplete a hard requirement or a goal?**
- [ ] Hard requirement (SLA)
- [ ] Goal (best effort)
- [ ] Not critical

### B4. Result Ranking
**Question:** How should search results be ranked?

Priority order (1 = highest priority):
- [ ] ___ Exact matches
- [ ] ___ Similarity score (relevance)
- [ ] ___ Most recent records first
- [ ] ___ Most frequently accessed
- [ ] ___ Business priority (orders > accounts > etc.)
- [ ] Other: _____________

### B5. Archived/Deleted Records
**Question:** Should search include archived or soft-deleted records?

- [ ] Yes, include all records
- [ ] No, exclude archived/deleted
- [ ] Admin only can see archived
- [ ] Configurable per search request

---

## Section C: Authorization & Access Control 🟡 IMPORTANT

### C1. Role Hierarchy
**Question:** Are there additional roles beyond these 5?

**Current documented roles:**
- Admin (full access)
- Dispatcher (Orders, Fleets, Drivers, PODs)
- Billing (Orders read-only, Billings, Invoices)
- Driver (own orders and PODs only)
- Fleet Manager (Orders, Fleets, Drivers)

**Additional roles (if any):**
- [ ] _____________
- [ ] _____________

### C2. Multiple Roles
**Question:** Can a user have multiple roles simultaneously?

- [ ] Yes, users can have multiple roles
- [ ] No, one role per user
- [ ] Roles are hierarchical (e.g., Admin inherits all)

### C3. Multi-Tenancy
**Question:** Is this a multi-tenant system?

- [ ] Yes - multiple organizations share the database
- [ ] No - single organization
- [ ] Planned for future

**If multi-tenant:**
- [ ] Search should be automatically scoped to user's tenant
- [ ] Admins can search across all tenants
- [ ] Tenant ID field name: _____________

### C4. Compliance Requirements
**Question:** Are there regulatory compliance requirements?

- [ ] GDPR (EU data protection)
- [ ] HIPAA (healthcare)
- [ ] SOC 2
- [ ] PCI-DSS (payment card industry)
- [ ] None
- [ ] Other: _____________

---

## Section D: Infrastructure & Deployment 🟡 IMPORTANT

### D1. Cloud Provider
**Question:** Which cloud provider or infrastructure?

- [ ] AWS
- [ ] Azure
- [ ] Google Cloud Platform (GCP)
- [ ] On-premises
- [ ] Hybrid
- [ ] Other: _____________

**Region/Location:** _____________

### D2. Existing Infrastructure
**Question:** Are there existing resources we can leverage?

- [ ] Yes, we have existing PostgreSQL instances
- [ ] Yes, we have existing Redis clusters
- [ ] Yes, we have existing Kubernetes clusters
- [ ] No, we need new infrastructure
- [ ] Other: _____________

**Details:** _____________

### D3. CI/CD Pipeline
**Question:** What is your current CI/CD setup?

- [ ] Jenkins
- [ ] GitHub Actions
- [ ] GitLab CI
- [ ] AWS CodePipeline
- [ ] Azure DevOps
- [ ] None (manual deployment)
- [ ] Other: _____________

### D4. Container Strategy
**Question:** Containerization preference?

- [ ] Docker + Kubernetes (EKS, AKS, GKE)
- [ ] Docker + ECS (AWS)
- [ ] Docker + EC2 (traditional VMs)
- [ ] No containers (bare metal / VMs)
- [ ] Other: _____________

---

## Section E: Integration & APIs 🟡 IMPORTANT

### E1. API Consumers
**Question:** What systems/clients consume the search API?

- [ ] Web application (SPA: React, Angular, Vue)
- [ ] Mobile apps (iOS, Android)
- [ ] Third-party integrations
- [ ] Internal dashboards/BI tools
- [ ] Microservices
- [ ] Other: _____________

**Number of concurrent API clients:** _____________

### E2. API Format
**Question:** API requirements beyond REST?

- [ ] REST JSON only (current)
- [ ] GraphQL required
- [ ] gRPC for internal services
- [ ] WebSockets for real-time updates
- [ ] Other: _____________

### E3. Event Streaming
**Question:** Are webhooks or event streaming required?

- [ ] Yes - Kafka
- [ ] Yes - RabbitMQ
- [ ] Yes - AWS SNS/SQS
- [ ] Yes - Other: _____________
- [ ] No - not required

**Use case:** _____________

---

## Section F: Monitoring & Operations 🟢 NICE TO HAVE

### F1. Monitoring Tools
**Question:** What monitoring/observability tools do you currently use?

- [ ] Datadog
- [ ] New Relic
- [ ] AWS CloudWatch
- [ ] Prometheus + Grafana
- [ ] ELK Stack (Elasticsearch, Logstash, Kibana)
- [ ] Splunk
- [ ] None
- [ ] Other: _____________

### F2. Performance SLAs
**Question:** Are there existing SLAs for search performance?

- [ ] Yes - Document available
- [ ] Yes - See below:
  - Search latency SLA: _____________ ms (p95)
  - Autocomplete SLA: _____________ ms (p95)
  - Uptime SLA: _____________ %
  - Error rate SLA: < _____________ %
- [ ] No - to be defined

### F3. Log Retention
**Question:** What is the required log retention period?

- [ ] 7 days
- [ ] 30 days
- [ ] 90 days
- [ ] 1 year
- [ ] Other: _____________

---

## Section G: Migration & Timeline 🔴 CRITICAL

### G1. Migration Strategy
**Question:** What migration approach is preferred?

- [ ] **Big Bang:** Full cutover on a specific date (fastest, higher risk)
- [ ] **Phased Rollout:** Gradual migration by feature (slower, lower risk)
  - Phase 1: Read-only search (PostgreSQL reads, MongoDB writes)
  - Phase 2: Dual-write (both databases)
  - Phase 3: Full cutover (PostgreSQL only)
- [ ] **Blue-Green:** MongoDB runs alongside PostgreSQL, instant rollback
- [ ] **Other:** _____________

### G2. MongoDB Availability
**Question:** Can the MongoDB cluster remain active during migration?

- [ ] Yes - we can keep it running for 1-2 months
- [ ] Yes - but only for 2-4 weeks
- [ ] No - cost constraints require immediate shutdown
- [ ] Other: _____________

### G3. Timeline Constraints
**Question:** What are your timeline expectations?

**Target go-live date:** _____________

**Hard deadlines (if any):**
- [ ] Contract renewal: _____________
- [ ] Budget year-end: _____________
- [ ] Product launch: _____________
- [ ] Other: _____________

**Acceptable timeline:**
- [ ] 6-8 weeks (MVP, minimal features)
- [ ] 10-12 weeks (full feature parity) ← **RECOMMENDED**
- [ ] 16-20 weeks (full features + extras)
- [ ] Other: _____________

---

## Section H: Testing & Quality 🟡 IMPORTANT

### H1. Test Coverage
**Question:** What is the expected test coverage percentage?

- [ ] 80%+ (industry standard)
- [ ] 90%+ (high quality)
- [ ] 95%+ (critical systems)
- [ ] No specific requirement

### H2. Test Data
**Question:** Are there existing test datasets or fixtures?

- [ ] Yes - we have production-like test data
- [ ] Yes - we can generate synthetic data
- [ ] No - needs to be created
- [ ] Can use anonymized production data

**Test data volume:** _____________

### H3. Load Testing
**Question:** Should we implement load/performance testing?

- [ ] Yes - critical requirement
- [ ] Yes - nice to have
- [ ] No - not required

**If yes, preferred tool:**
- [ ] JMeter
- [ ] Gatling
- [ ] Locust
- [ ] k6
- [ ] Other: _____________

### H4. UAT (User Acceptance Testing)
**Question:** Will there be a UAT phase?

- [ ] Yes - 1 week UAT
- [ ] Yes - 2 weeks UAT
- [ ] Yes - 4 weeks UAT
- [ ] No - direct to production
- [ ] Other: _____________

**UAT participants:** _____________

---

## Section I: Future Roadmap 🟢 NICE TO HAVE

### I1. Advanced Features
**Question:** Are any of these features planned?

- [ ] Machine learning-based ranking
- [ ] Semantic search (natural language)
- [ ] Search analytics dashboard
- [ ] Saved searches / search history
- [ ] Search suggestions based on popularity
- [ ] Other: _____________

**Timeline for advanced features:** _____________

### I2. File Content Search
**Question:** Will search need to support file content?

- [ ] Yes - PDF files
- [ ] Yes - Microsoft Office documents (Word, Excel)
- [ ] Yes - Images (OCR)
- [ ] No - not required
- [ ] Other: _____________

### I3. Geospatial Search
**Question:** Is location-based search required?

- [ ] Yes - search by radius (e.g., "drivers within 50km")
- [ ] Yes - geofencing
- [ ] No - not required
- [ ] Future requirement

**If yes, we recommend adding PostGIS extension to PostgreSQL.**

### I4. Internationalization
**Question:** What languages need to be supported?

**Current:**
- [ ] English only

**Planned:**
- [ ] Spanish
- [ ] French
- [ ] German
- [ ] Chinese (Simplified / Traditional)
- [ ] Arabic
- [ ] Other: _____________

**Search requirements:**
- [ ] Language-specific stemming (e.g., "running" → "run")
- [ ] Language detection (auto-detect query language)
- [ ] Multilingual fuzzy matching (e.g., accents: "café" = "cafe")
- [ ] Not required

---

## Section J: Budget & Resources 🔴 CRITICAL

### J1. Infrastructure Budget
**Question:** What is the monthly infrastructure budget?

- [ ] < $500/month
- [ ] $500 - $1,000/month
- [ ] $1,000 - $2,000/month
- [ ] $2,000 - $5,000/month
- [ ] > $5,000/month
- [ ] No hard limit

**Current MongoDB Atlas cost:** $_____________ /month

### J2. Team Resources
**Question:** What is the available team composition?

- [ ] ___ Java developers (Spring Boot experience)
- [ ] ___ DevOps engineers
- [ ] ___ Database administrators (PostgreSQL)
- [ ] ___ QA engineers
- [ ] ___ Project manager

**Team availability:**
- [ ] Full-time (100%)
- [ ] Part-time (50%)
- [ ] Part-time (25%)
- [ ] Other: _____________

### J3. External Support
**Question:** Is external consulting/contracting acceptable?

- [ ] Yes - for specialized tasks (DBA, performance tuning)
- [ ] Yes - for full implementation
- [ ] No - internal team only
- [ ] Other: _____________

---

## Section K: Risk Tolerance 🟡 IMPORTANT

### K1. Performance Degradation
**Question:** What happens if search performance is slower than MongoDB initially?

- [ ] Acceptable if < 20% slower (can optimize later)
- [ ] Acceptable if < 10% slower
- [ ] Not acceptable - must match or exceed current performance
- [ ] Other: _____________

### K2. Feature Parity
**Question:** Can we launch with fewer features initially (MVP)?

**Minimum required features (Phase 1):**
- [ ] Global search (all entities)
- [ ] Autocomplete
- [ ] Fuzzy matching
- [ ] Role-based filtering
- [ ] Faceted search
- [ ] Result highlighting
- [ ] Pagination

**Can defer to Phase 2:**
- [ ] _____________
- [ ] _____________

### K3. Rollback Criteria
**Question:** What triggers a rollback to MongoDB?

- [ ] Error rate > 5%
- [ ] Latency > 2x baseline
- [ ] Downtime > 1 hour
- [ ] Data integrity issues (any)
- [ ] Other: _____________

---

## Summary & Next Steps

### Completed ✅
- Comprehensive technical specification created
- PostgreSQL search architecture designed
- Database schema migration plan outlined
- Cost analysis and risk assessment completed

### Pending Your Input 📝
1. **Review this document** and provide answers to all questions
2. **Mark priority** for any questions we missed
3. **Schedule a 60-minute clarification call** if needed

### Timeline After Receiving Answers
- **Week 1:** Finalize specification based on your answers
- **Week 2:** Build proof-of-concept (POC) with real data
- **Week 3:** Performance benchmarking & comparison
- **Week 4:** Final design review & kickoff approval

---

## How to Submit Answers

**Option 1:** Fill out this document and email to: _____________

**Option 2:** Schedule a clarification call: _____________

**Option 3:** Create a shared document (Google Docs, Confluence): _____________

**Deadline for answers:** _____________ (recommended: within 1 week)

---

**Document version:** 1.0
**Last updated:** 2025-11-07
**Contact:** [Your team contact information]
