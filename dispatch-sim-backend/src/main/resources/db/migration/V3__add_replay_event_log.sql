CREATE TABLE IF NOT EXISTS replay_event_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payload LONGTEXT NOT NULL
);

CREATE INDEX idx_replay_event_log_session_time ON replay_event_log (session_id, event_time);
CREATE INDEX idx_replay_event_log_time ON replay_event_log (event_time);
