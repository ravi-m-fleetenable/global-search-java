-- ============================================================================
-- Global Search - Database Schema Migration V2
-- Create Base Tables with JSONB Support
-- ============================================================================

-- Users Table (Authentication & Authorization)
CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) UNIQUE NOT NULL,
    encrypted_password  VARCHAR(255) NOT NULL,
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    role                VARCHAR(50) NOT NULL,
    -- Role: ADMIN, DISPATCHER, BILLING, DRIVER, FLEET_MANAGER
    status              VARCHAR(50) DEFAULT 'ACTIVE',
    driver_id           BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_role CHECK (role IN ('ADMIN', 'DISPATCHER', 'BILLING', 'DRIVER', 'FLEET_MANAGER')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE'))
);

-- Accounts Table
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
    -- Type: SHIPPER, CONSIGNEE, BROKER, FREIGHT_FORWARDER
    credit_limit        DECIMAL(15, 2) DEFAULT 0.00,
    current_balance     DECIMAL(15, 2) DEFAULT 0.00,
    status              VARCHAR(50) DEFAULT 'ACTIVE',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_account_type CHECK (account_type IN ('SHIPPER', 'CONSIGNEE', 'BROKER', 'FREIGHT_FORWARDER')),
    CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

-- Drivers Table
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
    status              VARCHAR(50) DEFAULT 'ACTIVE',
    address             JSONB, -- {street, city, state, zip, country}
    emergency_contact   JSONB, -- {name, phone, relationship}
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_driver_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ON_LEAVE', 'TERMINATED'))
);

-- Fleets Table
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
    status              VARCHAR(50) DEFAULT 'AVAILABLE',
    purchase_date       DATE,
    insurance_expiry    DATE,
    last_maintenance    DATE,
    next_maintenance    DATE,
    odometer            DECIMAL(10, 2),
    current_driver_id   BIGINT REFERENCES drivers(id) ON DELETE SET NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_fleet_status CHECK (status IN ('AVAILABLE', 'IN_USE', 'MAINTENANCE', 'OUT_OF_SERVICE'))
);

-- Orders Table (Partitioned by created_at)
CREATE TABLE orders (
    id                      BIGSERIAL NOT NULL,
    order_number            VARCHAR(50) NOT NULL,
    hawb_numbers            JSONB, -- Array of HAWB numbers
    status                  VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    -- Status: PENDING, CONFIRMED, IN_TRANSIT, DELIVERED, CANCELLED, ON_HOLD
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
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id, created_at),
    CONSTRAINT chk_order_status CHECK (status IN ('PENDING', 'CONFIRMED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED', 'ON_HOLD')),
    CONSTRAINT uq_order_number_created UNIQUE (order_number, created_at)
) PARTITION BY RANGE (created_at);

-- Create initial partitions for orders (last 12 months + 3 future months)
-- Note: In production, use pg_partman for automatic partition management

-- Default partition (catch-all for dates outside range)
CREATE TABLE orders_default PARTITION OF orders DEFAULT;

-- Current year partitions (example for 2024)
CREATE TABLE orders_2024_01 PARTITION OF orders
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE orders_2024_02 PARTITION OF orders
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE orders_2024_03 PARTITION OF orders
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');

CREATE TABLE orders_2024_04 PARTITION OF orders
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');

CREATE TABLE orders_2024_05 PARTITION OF orders
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');

CREATE TABLE orders_2024_06 PARTITION OF orders
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');

CREATE TABLE orders_2024_07 PARTITION OF orders
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');

CREATE TABLE orders_2024_08 PARTITION OF orders
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');

CREATE TABLE orders_2024_09 PARTITION OF orders
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');

CREATE TABLE orders_2024_10 PARTITION OF orders
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');

CREATE TABLE orders_2024_11 PARTITION OF orders
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE TABLE orders_2024_12 PARTITION OF orders
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- Future partitions (2025)
CREATE TABLE orders_2025_01 PARTITION OF orders
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE orders_2025_02 PARTITION OF orders
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE orders_2025_03 PARTITION OF orders
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

-- Add foreign key constraint to users.driver_id
ALTER TABLE users
    ADD CONSTRAINT fk_users_driver
    FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE SET NULL;

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at triggers to all tables
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_drivers_updated_at BEFORE UPDATE ON drivers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_fleets_updated_at BEFORE UPDATE ON fleets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Note: Triggers on partitioned tables need to be added to each partition or use BEFORE INSERT trigger on parent
