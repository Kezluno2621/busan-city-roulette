# City Roulette 프로젝트 구조

City Roulette는 부산 공공 관광데이터를 검색하고, 즉흥 룰렛 또는 맞춤 조건으로 여행 코스를 만드는 웹 서비스다. 로그인 회원은 코스와 즐겨찾기를 저장하고 여행 기록에 이미지를 첨부할 수 있다.

Java 17, Spring Boot, Spring MVC, Spring Security, Spring Data JPA, MySQL 8, Flyway를 사용한다. 화면은 Thymeleaf, JavaScript, CSS로 구성하며 지도는 Leaflet을 사용한다. Gradle Wrapper로 빌드하고, JUnit과 Spring 테스트 및 H2 테스트 설정으로 동작을 검증한다.

| 경로 | 읽을 내용 |
| --- | --- |
| `src/main/java/com/cityroulette/controller` | URL, 요청 검증, 화면 모델과 REST 응답 |
| `service` | 회원 업무, 데이터 수집·정규화, 관광 검색, 추천, 버스 경로 |
| `repository`, `domain` | 쿼리, JPA 엔티티 및 테이블 관계 |
| `dto` | 요청·응답 계약, 공급자 원본과 내부 전달 모델 |
| `config`, `tools`, `util` | 인증·설정, 실행 옵션이 필요한 수집·검증, 거리 계산 |
| `src/main/resources` | Spring 설정, Flyway, 템플릿과 정적 자원 |
| `src/test` | 테스트와 모의 서버 응답, H2·브라우저 데이터 |
| `scripts/demo` | Windows 발표용 DB 및 앱 시작·검증·종료 |

웹 진입점은 `CityRouletteApplication`이다. 화면 Controller는 모델과 템플릿을 연결하고 REST Controller는 DTO를 반환한다. 일부 Service는 외부 HTTP 클라이언트를 사용하며, Repository는 JPA를 통해 DB에 접근한다. 수집 Runner는 일반 페이지를 열 때 실행되는 경로와 분리되어 있다.

## 스키마와 설정

| Flyway | 역할 |
| --- | --- |
| V1~V4 | 장소 및 원천 ID 제약, 회원, 여행 기록 |
| V5~V7 | 장소 검색 보강, 출처와 실행 이력, 다중 공급자 정규화와 병합 후보 |
| V8~V9 | 버스 정류장·노선·정차 관계, 좌표 검색 인덱스 |
| V10~V11 | 저장 코스와 방문 장소, 회원별 즐겨찾기 |
| V12 | 대표 장소의 외부 ID 유일성을 공급자 범위로 조정 |

기존 Flyway 파일은 적용 이력의 체크섬과 연결되므로 보존한다. 기본 설정은 프로필 공통값, dev는 개발 설정, demo는 준비된 시연용 DB 검증, test는 격리된 테스트 설정을 맡는다. JSON fixture는 공급자 응답을 재현하므로 주석을 넣지 않는다. [주요 파일 색인](code-study/08-source-index.md)에서 소스 위치를 찾아볼 수 있다.
