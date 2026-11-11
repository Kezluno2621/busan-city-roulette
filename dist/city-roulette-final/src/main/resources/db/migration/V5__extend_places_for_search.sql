ALTER TABLE places ADD COLUMN district VARCHAR(50);
ALTER TABLE places ADD COLUMN description VARCHAR(2000);
ALTER TABLE places ADD COLUMN source VARCHAR(50) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE places ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE places ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE places SET source = 'KOR_SERVICE' WHERE external_id IS NOT NULL;

UPDATE places
SET district = CASE
    WHEN address LIKE '%강서구%' THEN '강서구'
    WHEN address LIKE '%금정구%' THEN '금정구'
    WHEN address LIKE '%기장군%' THEN '기장군'
    WHEN address LIKE '%남구%' THEN '남구'
    WHEN address LIKE '%동구%' THEN '동구'
    WHEN address LIKE '%동래구%' THEN '동래구'
    WHEN address LIKE '%부산진구%' THEN '부산진구'
    WHEN address LIKE '%북구%' THEN '북구'
    WHEN address LIKE '%사상구%' THEN '사상구'
    WHEN address LIKE '%사하구%' THEN '사하구'
    WHEN address LIKE '%서구%' THEN '서구'
    WHEN address LIKE '%수영구%' THEN '수영구'
    WHEN address LIKE '%연제구%' THEN '연제구'
    WHEN address LIKE '%영도구%' THEN '영도구'
    WHEN address LIKE '%중구%' THEN '중구'
    WHEN address LIKE '%해운대구%' THEN '해운대구'
    ELSE NULL
END
WHERE district IS NULL;

CREATE UNIQUE INDEX uk_places_source_external_id ON places (source, external_id);
CREATE INDEX idx_places_category ON places (category);
CREATE INDEX idx_places_district ON places (district);
CREATE INDEX idx_places_name ON places (name);
