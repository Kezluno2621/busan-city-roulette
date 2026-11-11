# 주요 소스 인덱스

우선순위는 `A`가 전체 흐름 이해에 필수, `B`가 기능 심화, `C`가 운영·검증 도구다.

## 애플리케이션과 설정

| 파일 | 역할 | 주요 입출력/연결 | 우선순위 |
|---|---|---|---|
| `CityRouletteApplication.java` | Spring Boot 시작점 | 전체 컴포넌트 스캔 | A |
| `config/SecurityConfig.java` | 인증·인가·CSRF·로그인 정책 | Controller URL, `CustomUserDetailsService` | A |
| `config/RecommendationWeights.java` | 추천 가중치 설정 바인딩 | `recommendation.weights.*` → Scorer | A |
| `config/ExternalApiProperties.java` | 외부 API 설정 집합 | 환경변수 → Client | B |
| `config/*Runner.java` | 명시적 수집·검증 진입점 | CLI 옵션 → 수집/검증 Service | C |

## 도메인과 Repository

| 파일 | 역할 | 주요 관계 | 우선순위 |
|---|---|---|---|
| `domain/Place.java` | 통합 대표 장소 | PlaceSource, Favorite, SavedCoursePlace | A |
| `domain/PlaceSource.java` | 공급자별 원천 식별·라이선스 | `(provider, externalId)` 멱등 키 | A |
| `domain/Member.java` | 로그인 회원 | Course, Favorite, TripRecord의 소유자 | A |
| `domain/SavedCourse.java` | 회원 저장 코스와 버전 | SavedCoursePlace, Member | A |
| `domain/BusStop.java` | 공식 버스 정류장 | BusRouteStop | B |
| `domain/BusRoute.java` | 외부 노선 ID 단위 노선 | BusRouteStop | B |
| `domain/BusRouteStop.java` | 노선·정류장·정차 순서 연결 | Route → Stop 그래프 간선 | B |
| `domain/ImportRun.java` | 수집 실행 통계 | 공급자별 성공/실패 건수 | B |
| `repository/PlaceRepository.java` | 장소 검색·후보 조회 | 페이지네이션, 필터, 좌표 | A |
| `repository/BusStopRepository.java` | bounding-box 정류장 조회 | 주변 검색 | B |
| `repository/BusRouteStopRepository.java` | 노선 관계 일괄 조회 | N+1 방지, 그래프 구성 | B |
| `repository/SavedCourseRepository.java` | 회원 코스 조회 | 소유권 범위 쿼리 | B |

## 장소 화면과 회원 기능

| 파일 | 역할 | 입력 → 출력 | 우선순위 |
|---|---|---|---|
| `controller/PlaceExplorerController.java` | `/places` 목록·상세 화면 | 쿼리/ID → Thymeleaf Model | A |
| `service/PlaceExplorerService.java` | 화면용 장소·출처 조합 | 검색 조건 → View DTO | A |
| `controller/PlaceFavoriteController.java` | 즐겨찾기 REST API | 인증 회원 + placeId → Favorite DTO | B |
| `service/PlaceFavoriteService.java` | 즐겨찾기 소유권·중복 처리 | Member/Place → DB 변경 | B |
| `controller/SavedCourseController.java` | 저장 코스 REST CRUD | 인증·요청 DTO → 응답 DTO | B |
| `service/SavedCourseService.java` | 토큰·소유권·버전·트랜잭션 | 코스 요청 → SavedCourse | A |
| `controller/TripRecordController.java` | 회원 여행 기록·파일 화면 | multipart/form → View | B |
| `service/FileStorageService.java` | 파일명 정규화·저장·조회 | MultipartFile → StoredFileInfo | B |

## 추천·룰렛

| 파일 | 역할 | 입력 → 출력 | 우선순위 |
|---|---|---|---|
| `dto/recommendation/RecommendationRequest.java` | 추천 API 입력 계약 | 출발지·조건·랜덤성 | A |
| `service/RecommendationCandidateFilter.java` | 입력 정규화와 DB 후보 제한 | 요청 → 제한 후보 | A |
| `service/RecommendationScorer.java` | 요소별 점수와 가중 평균 | Place/거리/선호 → 0~100 | A |
| `service/PersonalizedRecommendationService.java` | 추천 전체 오케스트레이션 | 요청 → 추천 목록·제한사항 | A |
| `service/CourseGenerationService.java` | 가중 선택·다양성·방문 순서 | 추천 후보 → 코스 DTO | A |
| `service/GeneratedCourseRegistry.java` | 저장 전 단기 토큰 | 생성 코스 ↔ 단일 사용 토큰 | B |
| `controller/PersonalizedRecommendationController.java` | `POST /api/recommendations` | JSON → JSON | A |
| `controller/CourseGenerationController.java` | `POST /api/courses/generate` | JSON → 코스 JSON | A |

## 버스와 경로 탐색

| 파일 | 역할 | 입력 → 출력 | 우선순위 |
|---|---|---|---|
| `service/BusQueryService.java` | 버스 조회·주변 검색·노선 일괄 조합 | 반경/검색어 → DTO | A |
| `util/DistanceCalculator.java` | Haversine 직선거리 | 두 좌표 → meter | A |
| `service/TransitGraphService.java` | 정차 순서를 보존한 그래프 | DB 관계 → TransitRouteGraph | B |
| `service/TransitRoutePlannerService.java` | 직통·최대 1회 환승 후보 | 출발/목적 Place → 경로 응답 | A |
| `service/BusanBusClient.java` | 부산 BIMS XML 호출 | 검색 조건 → XML | B |
| `service/BusanBusXmlParser.java` | XXE 방지 XML 파싱 | XML → Raw DTO | B |
| `service/BusCollectionService.java` | 정류장·노선 소량 수집 | API → Normalized DTO | C |
| `controller/BusController.java` | 정류장·노선 REST 조회 | page/search/id → DTO | B |
| `controller/NearbyBusStopController.java` | 장소 주변 정류장 API | placeId/radius → 거리순 목록 | A |

## 관광 공공데이터 수집

| 파일 | 역할 | 입력 → 출력 | 우선순위 |
|---|---|---|---|
| `service/PlaceDataProvider.java` | 공급자 공통 수집 계약 | page/limit → ProviderPage | A |
| `service/ProviderPlaceMapper.java` | 공급자 공통 변환 계약 | Raw DTO → NormalizedPlace | A |
| `dto/importing/NormalizedPlace.java` | 공급자 독립 표준 장소 | 검증·저장 입력 | A |
| `service/TourismApiClient.java` | 한국관광공사 KorService2 호출 | 요청 → Tourism DTO | B |
| `service/BusanJsonApiClient.java` | 부산 JSON API 공통 처리 | HTTP → JSON 노드 | B |
| `service/*DataProvider.java` | 공급자 페이지·상태 해석 | API 응답 → ProviderPage | B |
| `service/*PlaceMapper.java` | 실제 확인 필드 정규화 | 공급자 Item → NormalizedPlace | B |
| `service/PlaceImportService.java` | 멱등 저장·갱신·병합 후보 | NormalizedPlace → ImportResult | A |
| `service/PlaceDataQualityService.java` | 누락률·분포·중복 품질 통계 | DB → QualityReport | B |
| `service/PlaceImagePolicyService.java` | 이미지 라이선스 표시 정책 | 출처 정보 → 표시 가능 여부 | B |

## 화면·정적 자원

| 파일 | 역할 | 연결 | 우선순위 |
|---|---|---|---|
| `templates/index.html` | 메인 페이지 | 공개 탐색·추천 진입 | B |
| `templates/places/list.html` | 장소 카드·검색·페이지네이션 | PlaceExplorerController | A |
| `templates/places/detail.html` | 상세·지도·출처·주변 정류장 | PlaceDetailView | A |
| `templates/recommend.html` | 추천·룰렛 입력과 결과 영역 | 추천/코스 API | A |
| `templates/my/courses.html` | 회원 코스 목록 | SavedCoursePageController | B |
| `static/js/recommendation-engine.js` | 추천 요청·카드·Leaflet 지도 | `/api/recommendations` | A |
| `static/js/course-roulette.js` | 코스 요청·서버 일치 애니메이션·저장 | `/api/courses/generate`, `/api/courses` | A |
| `static/js/place-explorer.js` | 즐겨찾기와 장소 화면 상호작용 | `/api/favorites` | B |
| `static/css/style.css` | 공통 디자인·반응형·접근성 | 전체 템플릿 | B |

## DB·테스트·실행 자료

| 경로 | 역할 | 우선순위 |
|---|---|---|
| `src/main/resources/db/migration/V1__...V12__...sql` | 재현 가능한 스키마 변경 이력 | A |
| `src/test/java/...` | H2/Fixture 단위·통합·보안 회귀 테스트 | A |
| `scripts/demo/` | Windows 로컬 demo DB 검증·실행·종료 | C |
| `docs/phase2-*` | 데이터 수집·교통 기반 구축 기록 | C |
| `docs/phase3-*` | 경로·추천·룰렛 알고리즘 기록 | B |
| `docs/phase4-*` | 저장·즐겨찾기·실제 데이터 확장 기록 | C |
| `docs/phase5-*` | GUI QA·demo·제출 검증 기록 | C |

전체 파일을 처음부터 순서대로 읽기보다, 관심 기능의 Controller에서 시작해 Service → Repository/Entity → DTO → 테스트로 이동하는 것이 효율적이다.

