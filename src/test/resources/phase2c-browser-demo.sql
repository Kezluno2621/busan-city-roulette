INSERT INTO places (
    external_id, name, normalized_name, category, address, normalized_address,
    district, latitude, longitude, description, image_url, indoor, merge_status,
    source, created_at, updated_at
) VALUES (
    'PHASE2C-DEMO-PLACE', '감천문화마을', '감천문화마을', '관광지',
    '부산광역시 사하구 감내이2로 203', '부산광역시사하구감내이2로203', '사하구',
    35.0975000, 129.0106000, '산복도로와 예술 공간을 함께 볼 수 있는 Phase 2-C 브라우저 검증용 데모 장소입니다.',
    NULL, FALSE, 'ACTIVE', 'MANUAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO place_sources (
    place_id, provider, external_id, source_url, source_category,
    source_modified_at, last_synced_at, attribution, content_license, image_license, active
) SELECT id, 'TOUR_API', 'PHASE2C-DEMO-SOURCE', 'https://korean.visitkorea.or.kr/', '관광지',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '한국관광공사 TourAPI 데모', '검증용 데이터', NULL, TRUE
  FROM places WHERE external_id = 'PHASE2C-DEMO-PLACE';

INSERT INTO bus_stops (
    external_stop_id, ars_number, name, latitude, longitude, stop_type, updated_at
) VALUES
    ('PHASE2C-DEMO-STOP-1', '10001', '감천문화마을입구', 35.0980000, 129.0110000, '일반', CURRENT_TIMESTAMP),
    ('PHASE2C-DEMO-STOP-2', '10002', '감정초등학교', 35.0969000, 129.0099000, '일반', CURRENT_TIMESTAMP);

INSERT INTO bus_routes (
    external_route_id, route_number, route_type, start_stop_name, end_stop_name, updated_at
) VALUES ('PHASE2C-DEMO-ROUTE', '사하구1-1', '마을버스', '감천문화마을', '괴정역', CURRENT_TIMESTAMP);

INSERT INTO bus_route_stops (
    bus_route_id, bus_stop_id, stop_sequence, direction, updated_at
) SELECT r.id, s.id, 1, '괴정역', CURRENT_TIMESTAMP
  FROM bus_routes r, bus_stops s
 WHERE r.external_route_id = 'PHASE2C-DEMO-ROUTE'
   AND s.external_stop_id = 'PHASE2C-DEMO-STOP-1';

INSERT INTO bus_route_stops (
    bus_route_id, bus_stop_id, stop_sequence, direction, updated_at
) SELECT r.id, s.id, 2, '괴정역', CURRENT_TIMESTAMP
  FROM bus_routes r, bus_stops s
 WHERE r.external_route_id = 'PHASE2C-DEMO-ROUTE'
   AND s.external_stop_id = 'PHASE2C-DEMO-STOP-2';
