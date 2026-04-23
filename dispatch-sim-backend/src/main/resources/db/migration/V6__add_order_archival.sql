ALTER TABLE orders
    ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN archived_at TIMESTAMP NULL,
    ADD COLUMN archival_reason VARCHAR(255) NULL,
    ADD COLUMN depot_id BIGINT NULL;

UPDATE orders
SET archived = FALSE
WHERE archived IS NULL;

CREATE INDEX idx_orders_archived_status ON orders(archived, status);
CREATE INDEX idx_orders_depot_id ON orders(depot_id);
