CREATE TABLE IF NOT EXISTS road_nodes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    position_x DOUBLE NOT NULL,
    position_y DOUBLE NOT NULL,
    type VARCHAR(32) NOT NULL,
    metadata TEXT NULL
);

CREATE TABLE IF NOT EXISTS road_edges (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_node_id BIGINT NOT NULL,
    to_node_id BIGINT NOT NULL,
    bidirectional BOOLEAN NOT NULL DEFAULT TRUE,
    weight DOUBLE NOT NULL,
    metadata TEXT NULL
);

CREATE TABLE IF NOT EXISTS road_network_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_no INT NOT NULL,
    snapshot LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_road_nodes_type ON road_nodes(type);
CREATE INDEX idx_road_edges_nodes ON road_edges(from_node_id, to_node_id);
CREATE INDEX idx_road_network_versions_version_no ON road_network_versions(version_no);
