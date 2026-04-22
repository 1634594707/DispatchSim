CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    pickup_x DOUBLE NOT NULL,
    pickup_y DOUBLE NOT NULL,
    delivery_x DOUBLE NOT NULL,
    delivery_y DOUBLE NOT NULL,
    priority INT NOT NULL,
    strategy VARCHAR(32) NULL,
    assigned_vehicle_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    cancellation_reason VARCHAR(255) NULL,
    dispatch_attempts INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS vehicles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    status VARCHAR(32) NOT NULL,
    current_position_x DOUBLE NULL,
    current_position_y DOUBLE NULL,
    battery INT NOT NULL,
    speed DOUBLE NOT NULL,
    max_speed DOUBLE NOT NULL,
    current_load DOUBLE NOT NULL,
    capacity DOUBLE NOT NULL,
    heading DOUBLE NULL,
    total_tasks INT NULL,
    total_distance DOUBLE NULL,
    current_order_id BIGINT NULL
);

CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload LONGTEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL
);
