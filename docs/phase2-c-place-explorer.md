# City Roulette Phase 2-C — 통합 관광지 탐색기

## 구현 범위

Phase 2-B에서 적재한 `places`, `place_sources`, `bus_stops`, `bus_routes`,
`bus_route_stops`를 읽어 비회원도 이용할 수 있는 목록·상세 화면과 REST API를
구현했다. 이 단계는 수집기나 추천 알고리즘을 실행하지 않고, 기존 DB를
조회하는 기능만 추가한다.

## 화면 URL

| URL | 설명 | 접근 |
| --- | --- | --- |
| `GET /places` | 키워드·구/군·카테고리 복합 필터, 결과 건수, 서버 페이징 | 공개 |
| `GET /places/{id}` | 장소, 출처, 지도, 주변 정류장과 경유 노선 | 공개 |

`/places`는 `keyword`, `district`, `category`, `page`, `size`를 쿼리 파라미터로
받는다. 페이지 이동 시에도 필터를 URL에 유지한다. 상세의 `radius`는
50~3,000m이고 기본값은 500m이다.

## REST API

| API | 기능 |
| --- | --- |
| `GET /api/places` | `keyword`, `district`, `category`, `page`, `size` 기반 장소 페이징 |
| `GET /api/places/{id}` | 장소 DTO 조회 |
| `GET /api/places/{id}/nearby-bus-stops?radius=500` | Haversine 직선거리 기반 주변 정류장 |
| `GET /api/bus/stops/{id}/routes` | 정류장을 경유하는 중복 제거 노선 DTO 목록 |

기존 버스 API인 `/api/bus/stops`, `/api/bus/stops/{id}`, `/api/bus/routes`,
`/api/bus/routes/{id}`, `/api/bus/routes/{id}/stops`도 그대로 유지한다. Entity를
직접 반환하지 않고 응답 DTO를 사용한다. 존재하지 않는 대상은 404, 반경과
페이지 크기 범위 위반은 400으로 처리한다.

## Controller → Service → Repository

```text
PlaceExplorerController
  └─ PlaceExplorerService
       ├─ PlaceService → PlaceRepository
       ├─ PlaceSourceRepository
       └─ BusQueryService
            ├─ BusStopRepository
            └─ BusRouteStopRepository → BusRoute
```

`PlaceExplorerService`는 상세 화면에 필요한 장소, 출처, 최종 동기화 시각,
주변 정류장, 경유 노선을 조합한다. 좌표가 없거나 버스 데이터가 없으면
예외 대신 빈 결과를 화면에 전달한다. 연결 거리는 도보 거리나 소요 시간이
아닌 WGS84 좌표의 Haversine 직선거리다.

## 지도 설정과 이용 조건

- Leaflet 1.9.4를 브라우저에서 로드하고 OpenStreetMap 표준 타일을 사용한다.
- 별도의 지도 API 키나 환경변수는 필요하지 않다.
- 타일 URL은 `https://tile.openstreetmap.org/{z}/{x}/{y}.png`이고 지도에
  `© OpenStreetMap contributors` 출처를 노출한다.
- 타일을 사전 수집·일괄 다운로드·프록시하지 않고 일반 브라우저 화면에서만
  필요한 타일을 요청한다.
- Leaflet/CDN/타일 로드가 실패해도 상세 정보와 정류장 목록은 유지하고
  지도 영역에 안내를 표시한다.

참고: [Leaflet Quick Start](https://leafletjs.com/examples/quick-start/),
[OpenStreetMap Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/)

## 이미지·출처 처리

원천 `image_url`이 비어 있거나 로드가 실패하면 로컬 SVG 기본 이미지를
표시한다. 상세 화면은 연결된 모든 `PlaceSource`의 provider, attribution,
content/image license, source URL, 동기화 시각을 개별로 표시한다. 실제
이미지의 재이용 가능 여부는 DB에 저장된 각 원천의 `image_license`를 따른다.

## 테스트

- `PlaceExplorerServiceTest`: 복합 필터/페이징, 상세·출처·정류장·노선 조합,
  좌표/교통 데이터 없음
- `PlaceExplorerControllerTest`: 비회원 목록/상세 접근, 빈 결과, 404
- `BusControllerTest`: `GET /api/bus/stops/{id}/routes`
- 전체 테스트와 `build`는 H2 `test` 프로필로 수행한다. MySQL 실행 데이터는
  삭제·재수집하지 않는다.

2026-10-28 검증 결과:

- `gradlew.bat build`: **BUILD SUCCESSFUL**, 241 tests, 0 failures
- 인앱 브라우저: 비회원 `/places` 200, 카드·필터·모바일 배치 정상
- 인앱 브라우저: `/places/1`에서 OSM 타일, 장소 마커 1개, 정류장
  마커 2개, 66m/92m 직선거리, 경유 노선, 출처 표시 정상
- 상세 화면 브라우저 콘솔 warning/error 0건

브라우저 검증은 실제 MySQL 대신 `phase2c-browser-demo.sql`을 명시적으로
로드한 H2 일회성 서버에서만 수행했다. 이 SQL은 일반 테스트나 운영
실행 시 자동 로드되지 않는다.

```powershell
.\gradlew.bat bootTestRun --args="--spring.profiles.active=test --server.port=18080 --server.address=127.0.0.1 --spring.sql.init.mode=always --spring.sql.init.data-locations=classpath:phase2c-browser-demo.sql"
```

## 환경변수

Phase 2-C 화면 자체에 추가된 비밀 환경변수는 없다. 애플리케이션 실행에는
기존 MySQL 환경변수(`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`,
`DB_PASSWORD`)를 그대로 사용한다. 외부 API 키는 조회 화면에서 사용하거나
브라우저에 전달하지 않는다.

## 한계와 후속 작업

- 지도 타일은 외부 네트워크가 필요하다. 서비스 규모가 커지면 OSM 정책에
  맞는 전용/상용 타일 공급자를 재검토한다.
- 도보 경로·실시간 도착·예상 소요 시간은 제공하지 않는다.
- 즐겨찾기·개인 코스는 Phase 3의 회원 전용 기능으로 남긴다.
- 실제 표시 품질은 Phase 2-B 수집 데이터의 좌표, 이미지, 라이선스,
  노선-정류장 연결 완전성에 의존한다.
- 이 환경에서는 개발 MySQL의 실제 Phase 2-B 적재 데이터로 화면을 별도
  검증하지 않았다.
