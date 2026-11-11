ALTER TABLE places ADD COLUMN image_url VARCHAR(1000);

CREATE TABLE place_sources (
    id BIGINT NOT NULL AUTO_INCREMENT,
    place_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    source_url VARCHAR(1000),
    source_category VARCHAR(255),
    source_modified_at TIMESTAMP NULL,
    last_synced_at TIMESTAMP NOT NULL,
    attribution VARCHAR(500),
    content_license VARCHAR(255),
    image_license VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_place_sources_place FOREIGN KEY (place_id) REFERENCES places (id),
    CONSTRAINT uk_place_sources_provider_external UNIQUE (provider, external_id)
);

CREATE INDEX idx_place_sources_place_id ON place_sources (place_id);

INSERT INTO place_sources (
    place_id, provider, external_id, source_url, source_category,
    source_modified_at, last_synced_at, attribution, content_license, image_license
)
SELECT
    id, 'TOUR_API', external_id,
    CONCAT('https://korean.visitkorea.or.kr/detail/rem_detail.do?cotid=', external_id),
    category, NULL, updated_at, '한국관광공사 TourAPI', NULL, NULL
FROM places
WHERE source = 'KOR_SERVICE' AND external_id IS NOT NULL;

CREATE TABLE import_runs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    dry_run BOOLEAN NOT NULL,
    record_limit INT,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NULL,
    pages_requested INT NOT NULL DEFAULT 0,
    received_count INT NOT NULL DEFAULT 0,
    inserted_count INT NOT NULL DEFAULT 0,
    updated_count INT NOT NULL DEFAULT 0,
    duplicate_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    PRIMARY KEY (id)
);

CREATE INDEX idx_import_runs_provider_started ON import_runs (provider, started_at);

CREATE TABLE place_merge_candidates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    place_id BIGINT NOT NULL,
    candidate_place_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_merge_candidate_place FOREIGN KEY (place_id) REFERENCES places (id),
    CONSTRAINT fk_merge_candidate_other FOREIGN KEY (candidate_place_id) REFERENCES places (id),
    CONSTRAINT uk_merge_candidate_pair UNIQUE (place_id, candidate_place_id)
);
