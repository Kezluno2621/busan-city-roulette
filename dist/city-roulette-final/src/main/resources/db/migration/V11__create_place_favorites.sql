CREATE TABLE place_favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_place_favorites_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    CONSTRAINT fk_place_favorites_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE,
    CONSTRAINT uk_place_favorites_member_place UNIQUE (member_id, place_id)
);

CREATE INDEX idx_place_favorites_member_created ON place_favorites(member_id, created_at);
CREATE INDEX idx_place_favorites_place ON place_favorites(place_id);
