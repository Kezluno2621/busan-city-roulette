CREATE TABLE IF NOT EXISTS places (
    id BIGINT NOT NULL AUTO_INCREMENT,
    external_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    address VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    indoor BOOLEAN,
    PRIMARY KEY (id)
);
