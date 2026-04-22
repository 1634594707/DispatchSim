CREATE INDEX idx_orders_status_created_at ON orders (status, created_at);
CREATE INDEX idx_orders_assigned_vehicle_status ON orders (assigned_vehicle_id, status);
CREATE INDEX idx_orders_strategy ON orders (strategy);

CREATE INDEX idx_vehicles_status ON vehicles (status);
CREATE INDEX idx_vehicles_current_order_id ON vehicles (current_order_id);

CREATE INDEX idx_outbox_status_created_at ON outbox_event (status, created_at);
CREATE INDEX idx_outbox_aggregate_type_id ON outbox_event (aggregate_type, aggregate_id);
