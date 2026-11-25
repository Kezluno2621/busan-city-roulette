CREATE TABLE bus_stops (
    id BIGINT NOT NULL AUTO_INCREMENT,
    external_stop_id VARCHAR(30) NOT NULL,
    ars_number VARCHAR(20),
    name VARCHAR(255) NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    stop_type VARCHAR(50),
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bus_stops_external_id UNIQUE (external_stop_id)
);

CREATE INDEX idx_bus_stops_name ON bus_stops (name);
CREATE INDEX idx_bus_stops_ars_number ON bus_stops (ars_number);

CREATE TABLE bus_routes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    external_route_id VARCHAR(30) NOT NULL,
    route_number VARCHAR(50) NOT NULL,
    route_type VARCHAR(50),
    start_stop_name VARCHAR(255),
    end_stop_name VARCHAR(255),
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bus_routes_external_id UNIQUE (external_route_id)
);

CREATE INDEX idx_bus_routes_number ON bus_routes (route_number);

CREATE TABLE bus_route_stops (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bus_route_id BIGINT NOT NULL,
    bus_stop_id BIGINT NOT NULL,
    stop_sequence INT NOT NULL,
    direction VARCHAR(30),
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_bus_route_stops_route FOREIGN KEY (bus_route_id) REFERENCES bus_routes (id),
    CONSTRAINT fk_bus_route_stops_stop FOREIGN KEY (bus_stop_id) REFERENCES bus_stops (id),
    CONSTRAINT uk_bus_route_stops_sequence UNIQUE (bus_route_id, stop_sequence)
);

CREATE INDEX idx_bus_route_stops_stop ON bus_route_stops (bus_stop_id);
