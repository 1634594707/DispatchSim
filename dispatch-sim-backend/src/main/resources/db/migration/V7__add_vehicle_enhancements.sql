ALTER TABLE vehicles
    ADD COLUMN order_queue VARCHAR(1024) NOT NULL DEFAULT '[]',
    ADD COLUMN loading_time_remaining INT NOT NULL DEFAULT 0;

UPDATE vehicles
SET order_queue = '[]',
    loading_time_remaining = 0
WHERE order_queue IS NULL
   OR loading_time_remaining IS NULL;
