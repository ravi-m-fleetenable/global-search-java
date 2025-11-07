-- ============================================================================
-- Global Search - Database Schema Migration V3
-- Create pg_trgm Search Indexes (GIN and GiST)
-- ============================================================================

-- =======================
-- Users Table Indexes
-- =======================

-- Full-text search index (GIN for fast fuzzy search)
CREATE INDEX idx_users_fulltext_gin ON users USING GIN (
    (lower(first_name || ' ' || last_name || ' ' || email)) gin_trgm_ops
);

CREATE INDEX idx_users_email ON users (lower(email));
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_status ON users (status);

-- =======================
-- Accounts Table Indexes
-- =======================

-- Composite GIN index for multi-attribute search
CREATE INDEX idx_accounts_search_gin ON accounts USING GIN (
    (lower(account_number || ' ' ||
           account_name || ' ' ||
           COALESCE(company_name, '') || ' ' ||
           COALESCE(contact_person, '') || ' ' ||
           COALESCE(email, ''))) gin_trgm_ops
);

-- Individual field indexes for exact lookups
CREATE INDEX idx_accounts_number ON accounts (lower(account_number));
CREATE INDEX idx_accounts_type ON accounts (account_type);
CREATE INDEX idx_accounts_status ON accounts (status);
CREATE INDEX idx_accounts_created ON accounts (created_at DESC);

-- JSONB index for address queries
CREATE INDEX idx_accounts_address_gin ON accounts USING GIN (address);

-- =======================
-- Drivers Table Indexes
-- =======================

-- GiST index for similarity-based search (frequently queried)
CREATE INDEX idx_drivers_search_gist ON drivers USING GIST (
    (lower(first_name || ' ' || last_name || ' ' ||
           COALESCE(email, '') || ' ' ||
           COALESCE(phone, '') || ' ' ||
           license_number)) gist_trgm_ops
);

-- Individual indexes
CREATE INDEX idx_drivers_license ON drivers (lower(license_number));
CREATE INDEX idx_drivers_email ON drivers (lower(email));
CREATE INDEX idx_drivers_status ON drivers (status);
CREATE INDEX idx_drivers_created ON drivers (created_at DESC);

-- =======================
-- Fleets Table Indexes
-- =======================

-- GIN index for vehicle search
CREATE INDEX idx_fleets_search_gin ON fleets USING GIN (
    (lower(COALESCE(vehicle_name, '') || ' ' ||
           vin || ' ' ||
           license_plate || ' ' ||
           COALESCE(make, '') || ' ' ||
           COALESCE(model, ''))) gin_trgm_ops
);

-- Exact match indexes
CREATE INDEX idx_fleets_vin ON fleets (lower(vin));
CREATE INDEX idx_fleets_license_plate ON fleets (lower(license_plate));
CREATE INDEX idx_fleets_status ON fleets (status);
CREATE INDEX idx_fleets_driver ON fleets (current_driver_id);
CREATE INDEX idx_fleets_created ON fleets (created_at DESC);

-- =======================
-- Orders Table Indexes (on parent, inherited by partitions)
-- =======================

-- Composite GIN index for order search
-- Note: Create on parent table to be inherited by all partitions
CREATE INDEX idx_orders_search_gin ON orders USING GIN (
    (lower(order_number || ' ' || COALESCE(notes, ''))) gin_trgm_ops
);

-- JSONB index for HAWB numbers array
CREATE INDEX idx_orders_hawb_gin ON orders USING GIN (hawb_numbers);

-- Standard B-tree indexes
CREATE INDEX idx_orders_number ON orders (lower(order_number));
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_account ON orders (account_id);
CREATE INDEX idx_orders_driver ON orders (driver_id);
CREATE INDEX idx_orders_fleet ON orders (fleet_id);
CREATE INDEX idx_orders_dispatcher ON orders (assigned_dispatcher_id);
CREATE INDEX idx_orders_dates ON orders (pickup_date, delivery_date);
CREATE INDEX idx_orders_created ON orders (created_at DESC);

-- JSONB indexes for origin/destination
CREATE INDEX idx_orders_origin_gin ON orders USING GIN (origin);
CREATE INDEX idx_orders_destination_gin ON orders USING GIN (destination);

-- =======================
-- Partial Indexes (Index only recent data)
-- =======================

-- Index only last 12 months of orders for faster queries
-- This significantly reduces index size and improves performance
CREATE INDEX idx_orders_search_recent ON orders USING GIN (
    (lower(order_number || ' ' || COALESCE(notes, ''))) gin_trgm_ops
)
WHERE created_at >= CURRENT_DATE - INTERVAL '12 months';

-- =======================
-- Analyze Tables for Query Planner
-- =======================

ANALYZE users;
ANALYZE accounts;
ANALYZE drivers;
ANALYZE fleets;
ANALYZE orders;
