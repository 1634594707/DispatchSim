CREATE TABLE IF NOT EXISTS depots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    position_x DOUBLE NOT NULL,
    position_y DOUBLE NOT NULL,
    icon VARCHAR(32) NULL,
    metadata TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_depots_name ON depots(name);
