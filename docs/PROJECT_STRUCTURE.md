# City Roulette 프로젝트 구조

City Roulette는 부산 공공 관광데이터를 검색하고, 즉흥 룰렛 또는 맞춤 조건으로 여행 코스를 만드는 웹 서비스다. 로그인 회원은 코스와 즐겨찾기를 저장하고 여행 기록에 이미지를 첨부할 수 있다.

Java 17, Spring Boot, Spring MVC, Spring Security, Spring Data JPA, MySQL 8, Flyway를 사용한다. 화면은 Thymeleaf, JavaScript, CSS로 구성하며 지도는 Leaflet을 사용한다. Gradle Wrapper로 빌드하고, JUnit과 Spring 테스트 및 H2 테스트 설정으로 동작을 검증한다.

| 경로 | 읽을 내용 |
| --- | --- |
| `src/main/java/com/cityroulette/domain` | `member`, `place`, `transport`, `recommendation`, `course`, `favorite`, `trip` 기능의 MVC·JPA 코드 |
| `integration` | TourAPI, 부산시 관광·버스 API, Kakao, ODsay 클라이언트와 원본 응답 DTO |
| `ingestion` | 관광·버스 데이터 수집, 변환, 검증, 적재, 수집 이력 및 수동 Runner |
| `global` | 로그인과 접근 정책, 공통 오류 응답, 거리 계산 |
| `web`, `tools` | 첫 화면·공통 모델 데이터, 읽기 전용 검증 진입점 |
| `src/main/resources` | Spring 설정, Flyway, 템플릿과 정적 자원 |
| `src/test` | 테스트와 모의 서버 응답, H2·브라우저 데이터 |
| `scripts/demo` | Windows 발표용 DB 및 앱 시작·검증·종료 |

웹 진입점은 루트 패키지의 `CityRouletteApplication`이다. 새 패키지는 모두 그 아래에 있어 Spring Boot의 기본 컴포넌트·엔티티·저장소 검색 범위에 들어간다. 각 기능에서 Controller는 요청을 받고, Service는 업무 규칙을 처리하며, Repository는 JPA로 저장소에 접근한다. 작게 유지할 수 있는 패키지는 역할을 더 쪼개지 않았다.

### 주요 클래스의 위치

| 클래스 | 위치와 역할 |
| --- | --- |
| `SecurityConfig` | `global.security`에서 공용·회원·관리자 경로와 인증 설정 관리 |
| `PlaceExplorerService` | `domain.place.service`에서 장소 검색과 상세 모델 구성 |
| `TransitRoutePlannerService` | `domain.transport.service`에서 적재 버스 경로 후보 탐색 |
| `RecommendationScorer`, `CourseGenerationService` | 점수 계산은 `domain.recommendation.service`, 코스 선택·방문 순서는 `domain.course.service` |
| `BusanJsonApiClient`, `ProviderPage` | `integration.busan`의 외부 통신과 원본 페이지 |
| `PlaceImportService` | `ingestion.place`에서 표준화된 장소 적재 |

공급자의 응답 DTO는 `integration`, 변환 후 적재에 필요한 모델은 `ingestion`, 화면과 API 요청·응답은 해당 `domain`의 DTO에 둔다. 수집 Runner 실행은 설정으로 제어한다.

### 화면과 정적 자원

`templates`에는 `home`, `member`, `place`, `recommendation`, `course`, `favorite`, `trip` 화면과 공통 `fragments`, 오류 화면 `error`가 있다. 코스 결과 fragment는 `course/fragments`에 둔다. `static/js`에는 `common`, `place`, `recommendation`, `course` 스크립트가 있고, 공통 스타일과 이미지는 `static/css`, `static/images`에 있다. 템플릿 이름은 내부 경로이며 기존 페이지와 API URL은 그대로 사용한다.

## 스키마와 설정

| Flyway | 역할 |
| --- | --- |
| V1~V4 | 장소 및 원천 ID 제약, 회원, 여행 기록 |
| V5~V7 | 장소 검색 보강, 출처와 실행 이력, 다중 공급자 정규화와 병합 후보 |
| V8~V9 | 버스 정류장·노선·정차 관계, 좌표 검색 인덱스 |
| V10~V11 | 저장 코스와 방문 장소, 회원별 즐겨찾기 |
| V12 | 대표 장소의 외부 ID 유일성을 공급자 범위로 조정 |

기존 Flyway 파일은 적용 이력의 체크섬과 연결되므로 보존한다. 기본 설정은 프로필 공통값, dev는 개발 설정, demo는 준비된 시연용 DB 검증, test는 격리된 테스트 설정을 맡는다. JSON fixture는 공급자 응답을 재현하므로 주석을 넣지 않는다. [주요 파일 색인](code-study/08-source-index.md)에서 소스 위치를 찾아볼 수 있다.

추천·버스 조회·경로 탐색 테스트 중 테이블 전체를 비우는 클래스는 각각 별도의 H2 데이터베이스를 사용한다. 테스트 클래스 순서가 바뀌어도 다른 기능의 외래 키나 데이터에 영향을 주지 않는다.
