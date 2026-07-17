CREATE TABLE reservations (
    reservation_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    vehicle_id VARCHAR(255) NOT NULL,
    station_id VARCHAR(255) NOT NULL,
    status VARCHAR(100) NOT NULL,
    created_at_epoch_ms BIGINT NOT NULL,
    scheduled_start_epoch_ms BIGINT NOT NULL,
    pickup_deadline_epoch_ms BIGINT NOT NULL,
    total_kilometers DOUBLE PRECISION,
    payment_amount NUMERIC(12, 2),
    completed_at_epoch_ms BIGINT
);
