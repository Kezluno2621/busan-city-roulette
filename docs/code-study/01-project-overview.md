# 01. 프로젝트 개요

## 한 문장 소개

City Roulette는 부산 공공 관광 데이터를 통합하고, 사용자의 출발지·카테고리·지역·랜덤성 조건으로 장소와 즉흥 여행 코스를 추천하는 Spring Boot 웹 서비스다.

## 주요 사용자 흐름

1. 비회원은 관광지 목록과 상세 정보를 검색한다.
2. 출발지와 선호 조건을 입력해 개인화 추천을 받는다.
3. 서버가 확정한 장소들로 룰렛 애니메이션과 방문 순서를 확인한다.
4. 로그인 회원은 코스·즐겨찾기·여행 기록을 저장한다.
5. 지도에서 장소와 주변 버스 정류소를 확인한다.

## 기술 스택

| 영역 | 기술 | 프로젝트에서의 역할 |
|---|---|---|
| 언어 | Java 17 | 백엔드와 알고리즘 |
| 프레임워크 | Spring Boot 3.5 | 설정, DI, 웹 서버 |
| 웹 | Spring MVC, Thymeleaf | REST API와 서버 렌더링 화면 |
| 보안 | Spring Security | 로그인, 권한, CSRF |
| DB | MySQL 8, Spring Data JPA | 영속 데이터와 조회 |
| 마이그레이션 | Flyway V1~V12 | 스키마 버전 관리 |
| 프론트 | HTML, CSS, JavaScript, Leaflet | 반응형 UI와 지도 |
| 테스트 | JUnit 5, MockMvc, H2 | 단위·통합·보안 테스트 |

## 디렉터리 지도

```text
src/main/java/com/cityroulette
├─ config       실행 설정, 보안, 수동 Runner
├─ controller   MVC 화면과 REST API
├─ domain       JPA Entity
├─ dto          요청·응답·정규화 DTO
├─ repository   Spring Data JPA
├─ service      유스케이스와 알고리즘
├─ tools        독립 실행 점검 도구
└─ util         순수 계산 유틸리티

src/main/resources
├─ db/migration Flyway V1~V12
├─ templates    Thymeleaf 화면
└─ static       CSS, JavaScript, 이미지
```

## 핵심 기능과 시작 클래스

| 기능 | 시작점 | 핵심 Service |
|---|---|---|
| 장소 탐색 | `PlaceExplorerController`, `PlaceController` | `PlaceExplorerService`, `PlaceService` |
| 개인화 추천 | `PersonalizedRecommendationController` | `PersonalizedRecommendationService` |
| 룰렛 코스 | `CourseGenerationController` | `CourseGenerationService` |
| 코스 저장 | `SavedCourseController` | `SavedCourseService` |
| 즐겨찾기 | `PlaceFavoriteController` | `PlaceFavoriteService` |
| 버스 조회 | `BusController`, `NearbyBusStopController` | `BusQueryService` |
| 정적 경로 | `TransitRouteController` | `TransitRoutePlannerService` |
| 공공데이터 수집 | 조건부 Runner | `PlaceCollectionService`, `PlaceImportService` |

## 프로필과 DB

- 기본 개발 설정과 발표용 `demo` 프로필은 서로 다른 DB를 바라본다.
- demo 프로필은 `127.0.0.1:33321/cityroulette_demo`와 포트 `18090`을 사용한다.
- demo DB에는 장소 395건과 부분 버스 데이터만 있으며 전체 부산 교통망이 아니다.
- 일반 서버 시작은 수집 작업을 자동 실행하지 않는다.

## 처음 읽을 때의 기준

Controller에서 URL을 찾고, 주입된 Service의 동일 동작을 따라간 뒤 Repository와 Entity를 확인한다. 외부 API 수집은 화면 요청 흐름과 분리되어 있으므로 마지막에 읽는 편이 이해하기 쉽다.
