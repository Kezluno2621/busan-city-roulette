CREATE TABLE IF NOT EXISTS trip_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    place_name VARCHAR(200) NOT NULL,
    place_address VARCHAR(255),
    memo VARCHAR(2000),
    visit_date DATE NOT NULL,
    original_file_name VARCHAR(255),
    stored_file_name VARCHAR(255),
    file_content_type VARCHAR(100),
    file_size BIGINT,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_trip_records_member
        FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE
);

CREATE INDEX idx_trip_records_member_created_at
    ON trip_records (member_id, created_at);
