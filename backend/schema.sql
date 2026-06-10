-- ============================================================================
-- ECOTRACK AI – ENTERPRISE POSTGRESQL DATABASE SCHEMA
-- Purpose: Holds user logs, active challenges, goal metrics, and gamified actions.
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    current_level INT DEFAULT 1,
    carbon_points INT DEFAULT 0,
    login_streak INT DEFAULT 1,
    last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS carbon_logs (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL, -- TRANSPORT, ENERGY, FOOD, CONSUMPTION
    description VARCHAR(255) NOT NULL,
    raw_quantity DOUBLE PRECISION NOT NULL,
    unit VARCHAR(20) NOT NULL, -- km, kWh, meals, items
    carbon_co2_kg DOUBLE PRECISION NOT NULL,
    logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS goals (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    target_reduction_kg DOUBLE PRECISION NOT NULL,
    accumulated_reduction_kg DOUBLE PRECISION DEFAULT 0.0,
    category_restriction VARCHAR(50) NOT NULL, -- ALL, TRANSPORT, etc.
    deadline_at TIMESTAMP NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_carbon_logs_user_date ON carbon_logs(user_id, logged_at);
CREATE INDEX IF NOT EXISTS idx_goals_user_deadline ON goals(user_id, deadline_at);
