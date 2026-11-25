# Package Structure

City Roulette는 수업에서 설명하기 쉬운 `Controller → Service → Repository` 3계층을 유지한다.
DTO는 파일 수를 줄이기 위해 합치는 것이 아니라, 외부 API 계약과 기능별 요청·응답 경계를 분명히 하기 위해 기능별 패키지로 나눈다.

## 조사 결과 요약

- production Java 파일: 251개 (`package-info.java` 제외)
- DTO 파일: 95개 (`package-info.java` 제외)
- production과 test 참조를 모두 검색한 결과 미사용 DTO는 없었다.
- 외부 API 응답 DTO는 Jackson 매핑 계약을 보호하기 위해 유지했다.
- 최상위 `dto`에는 여러 기능에서 함께 쓰는 공통 응답만 둔다.

## 최상위 패키지

| 패키지 | production 파일 수 | 역할 | 주요 클래스 | 판단 |
|---|---:|---|---|---|
| `config` | 16 | 보안, 외부 API 설정, 명시적으로 실행하는 수집·검증 Runner | `SecurityConfig`, `TourismApiConfig`, `RuntimeVerificationRunner` | 유지 |
| `controller` | 29 | MVC 화면과 REST 요청 처리, 입력 검증 및 HTTP 응답 변환 | `PlaceController`, `CourseGenerationController`, `SavedCourseController` | 유지 |
| `domain` | 13 | JPA Entity와 도메인 상태 | `Member`, `Place`, `BusStop`, `SavedCourse` | 유지 |
| `repository` | 12 | Spring Data JPA 데이터 접근 | `PlaceRepository`, `BusStopRepository`, `SavedCourseRepository` | 유지 |
| `service` | 83 | 추천, 코스, 수집, 버스, 회원 등 비즈니스 로직 | `CourseGenerationService`, `PlaceImportService`, `TransitRoutePlannerService` | 유지 |
| `tools` | 1 | 웹 실행과 분리된 읽기 전용 데이터 검증 진입점 | `ReadOnlyDataVerificationMain` | 유지 |
| `util` | 1 | 여러 서비스에서 재사용하는 순수 계산 | `DistanceCalculator` | 유지 |

`controller`, `service`, `repository`, `domain`은 수업의 3계층 구조와 JPA 설명에 직접 연결되므로 feature 기반 구조로 재배치하지 않았다.

## DTO 패키지 역할표

| 패키지 | 파일 수 | 역할 | 주요 클래스 | 실제 사용 및 판단 |
|---|---:|---|---|---|
| `dto` | 3 | 여러 기능에서 공유하는 공통 응답·검증 결과 | `ApiErrorResponse`, `ExternalApiProbeResult`, `DataVerificationSnapshot` | 유지. 기존 기능 전용 패키지에서 공통 위치로 이동 |
| `dto.bus` | 23 | 부산버스 정적 DB, XML 원본, 정규화, 자체 교통 그래프 DTO | `BusStopRaw`, `NormalizedBusStop`, `TransitRouteCandidate` | 유지 |
| `dto.course` | 6 | 저장 전 코스 생성 요청과 생성 결과 | `CourseGenerationRequest`, `CourseGenerationResponse`, `CoursePlaceDto` | 유지 |
| `dto.favorite` | 4 | 회원별 장소 즐겨찾기 요청·응답 | `FavoriteCreateRequest`, `FavoriteResponse` | Controller/Service/UI에서 사용하므로 유지 |
| `dto.importing` | 10 | 부산 관광 공급자 원본 항목, 정규화 결과, 수집·품질 통계 | `NormalizedPlace`, `ProviderPage`, `PlaceImportResult` | 유지 |
| `dto.location` | 3 | Kakao Local 응답과 화면용 출발지 검색 결과 | `KakaoPlaceSearchResponse`, `LocationSearchResultDto` | 외부 계약과 화면 DTO가 명확해 유지 |
| `dto.member` | 2 | 회원가입 입력과 로그인 회원 표시 정보 | `MemberSignupRequest`, `MemberProfile` | 유지 |
| `dto.place` | 7 | 장소 목록·상세·주변 정류장 화면 및 REST 응답 | `PlaceResponse`, `PlaceDetailView`, `PlacePageResponse` | 유지 |
| `dto.recommendation` | 8 | 개인화 추천 입력, 점수 내역, 추천 결과 | `RecommendationRequest`, `RecommendationResultDto` | 유지 |
| `dto.savedcourse` | 6 | DB에 저장된 회원 코스의 생성·조회·수정 응답 | `SavedCourseCreateRequest`, `SavedCourseResponse` | 유지 |
| `dto.tourism` | 15 | 한국관광공사 KorService2 JSON 계층과 수집 결과 | `TourismApiResponse`, `TourismBody`, `TourismPlaceDto` | 외부 API 계약이므로 유지 |
| `dto.transit` | 5 | ODsay 외부 대중교통 API 응답 | `OdsayResponse`, `OdsayPathDto`, `TransitRouteDto` | 외부 API 계약이므로 유지 |
| `dto.trip` | 3 | 여행 기록 입력과 업로드·다운로드 파일 정보 | `TripRecordRequest`, `StoredFileInfo`, `TripAttachment` | 유지 |

production 참조 없이 테스트에서만 쓰이는 DTO는 없었다. 테스트 코드는 production DTO의 JSON 파싱, validation, Controller 계약, Service 변환을 검증한다.

## 이번에 정리한 위치

### 공통 DTO

- `dto.place.ApiErrorResponse` → `dto.ApiErrorResponse`
  - 장소뿐 아니라 버스 API 예외 처리에서도 사용하므로 장소 전용이 아니다.
- `dto.external.ExternalApiProbeResult` → `dto.ExternalApiProbeResult`
  - 다섯 관광 API와 부산버스 등 여러 공급자의 공통 진단 결과이다.
- `dto.bus.DataVerificationSnapshot` → `dto.DataVerificationSnapshot`
  - Place와 Bus 통계를 함께 포함하므로 버스 전용이 아니다.

이 이동으로 모호했던 `dto.external` 하위 패키지는 제거되고, 최상위 `dto`는 실제 공통 DTO만 보유하게 되었다. Java 타입 이름, JSON 필드, endpoint, DB 스키마는 변경하지 않았다.

## 합치지 않은 DTO의 이유

### `dto.bus`와 `dto.transit`

- `dto.bus`: 부산버스정보시스템의 XML, MySQL 정적 버스 데이터, 자체 정차 순서 그래프를 표현한다.
- `dto.transit`: ODsay 외부 API의 JSON 응답을 표현한다.

둘 다 교통을 다루지만 데이터 공급자와 책임이 다르다. `TransitRouteCandidate`는 자체 버스 그래프의 경로 후보이고, `OdsayPathDto`는 ODsay 응답 계약이므로 통합하지 않았다.

### `dto.course`와 `dto.savedcourse`

- `dto.course`: 사용자가 방금 생성한 저장 전 코스와 일회성 저장 토큰의 원본이다.
- `dto.savedcourse`: 회원 소유권, 낙관적 잠금 version, 생성·수정 시각을 포함한 영구 저장 스냅샷이다.

장소 필드 일부가 비슷해도 수명 주기와 API 계약이 다르므로 `CoursePlaceDto`와 `SavedCoursePlaceResponse`를 통합하지 않았다.

### `dto.trip`과 `dto.savedcourse`

- `dto.trip`: 사용자가 작성한 여행 메모와 첨부 파일을 다룬다.
- `dto.savedcourse`: 추천 엔진이 만든 방문 순서와 점수 스냅샷을 다룬다.

두 기능은 저장 대상과 사용자 목적이 다르므로 유지한다.

### `dto.favorite`

즐겨찾기 REST API, 서비스, Thymeleaf 회원 화면에서 실제 사용된다. 회원별 데이터 격리와 중복 등록 결과를 Entity 노출 없이 전달하므로 유지한다.

### `dto.importing`

부산명소·맛집·푸디투어·갈맷길의 원본 항목과 공통 정규화 결과가 데이터 수집 파이프라인에서 함께 사용된다. Provider 전용 원본은 외부 계약이므로 합치지 않았다.

## 최종 트리

```text
com.cityroulette
├─ config
├─ controller
├─ domain
├─ repository
├─ service
├─ tools
├─ util
└─ dto
   ├─ ApiErrorResponse
   ├─ DataVerificationSnapshot
   ├─ ExternalApiProbeResult
   ├─ bus
   ├─ course
   ├─ favorite
   ├─ importing
   ├─ location
   ├─ member
   ├─ place
   ├─ recommendation
   ├─ savedcourse
   ├─ tourism
   ├─ transit
   └─ trip
```

## 발표용 설명

> Controller, Service, Repository는 수업에서 배운 3계층 구조를 그대로 유지했습니다. Entity를 JSON으로 직접 노출하지 않고 DTO로 경계를 만들었으며, DTO는 기능별로 분리했습니다. `tourism`과 `transit`은 각각 한국관광공사와 ODsay의 외부 응답 계약을 보호하고, `bus`는 부산버스 정적 데이터와 자체 경로 그래프를 담당합니다. `course`는 생성 직후 코스, `savedcourse`는 회원이 DB에 저장한 코스, `trip`은 메모와 사진이 있는 여행 기록이라 서로 합치지 않았습니다. 여러 기능에서 공통으로 쓰는 오류·진단 DTO만 최상위 `dto`에 두었습니다.

