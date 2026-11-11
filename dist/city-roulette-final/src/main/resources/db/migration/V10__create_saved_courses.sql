CREATE TABLE saved_courses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    origin_name VARCHAR(255) NOT NULL,
    origin_latitude DECIMAL(10,7) NOT NULL,
    origin_longitude DECIMAL(10,7) NOT NULL,
    requested_place_count INT NOT NULL,
    actual_place_count INT NOT NULL,
    randomness INT NOT NULL,
    available_minutes INT NULL,
    budget BIGINT NULL,
    estimate_time_status VARCHAR(20) NOT NULL,
    estimate_cost_status VARCHAR(20) NOT NULL,
    data_limitations VARCHAR(10000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_saved_courses_member FOREIGN KEY (member_id) REFERENCES members(id)
);

CREATE INDEX idx_saved_courses_member_created ON saved_courses(member_id, created_at);

CREATE TABLE saved_course_places (
    id BIGINT NOT NULL AUTO_INCREMENT,
    saved_course_id BIGINT NOT NULL,
    place_id BIGINT NULL,
    visit_order INT NOT NULL,
    place_name_snapshot VARCHAR(255) NOT NULL,
    category_snapshot VARCHAR(100) NULL,
    address_snapshot VARCHAR(500) NULL,
    latitude_snapshot DECIMAL(10,7) NOT NULL,
    longitude_snapshot DECIMAL(10,7) NOT NULL,
    recommendation_score DECIMAL(6,2) NOT NULL,
    recommendation_reason VARCHAR(5000) NULL,
    distance_from_previous_meters DECIMAL(12,1) NOT NULL,
    transit_verification_status VARCHAR(30) NOT NULL,
    data_limitations VARCHAR(5000) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_saved_course_places_course FOREIGN KEY (saved_course_id) REFERENCES saved_courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_course_places_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE SET NULL,
    CONSTRAINT uk_saved_course_visit_order UNIQUE (saved_course_id, visit_order),
    CONSTRAINT uk_saved_course_place UNIQUE (saved_course_id, place_id)
);

CREATE INDEX idx_saved_course_places_course ON saved_course_places(saved_course_id);
