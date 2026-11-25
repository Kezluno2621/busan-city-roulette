ALTER TABLE places ADD COLUMN normalized_name VARCHAR(255);
ALTER TABLE places ADD COLUMN normalized_address VARCHAR(500);
ALTER TABLE places ADD COLUMN merge_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE places
SET normalized_name = LOWER(TRIM(name))
WHERE normalized_name IS NULL;

UPDATE places
SET normalized_address = LOWER(TRIM(address))
WHERE address IS NOT NULL AND normalized_address IS NULL;

CREATE INDEX idx_places_normalized_name ON places (normalized_name);

ALTER TABLE place_sources ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
