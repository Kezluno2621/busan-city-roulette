# 웹 프로그래밍 기말 프로젝트 평가 기준 대응표

## 기본 점수

| 평가 항목 | City Roulette 구현 | 확인 위치 |
| --- | --- | --- |
| 참신성·현실성 | 부산 공공 관광데이터를 통합하고 즉흥 룰렛과 맞춤 코스를 제공 | `README.md`, 발표 2~3장 |
| 3계층 구조·Domain·DB | Controller → Service → Repository, JPA Domain, MySQL | `src/main/java`, 발표 4·6장 |
| Controller 제어 | 페이지·REST Controller와 DTO 응답 분리 | `src/main/java/com/cityroulette/controller` |
| 입력·검증·보안·예외 처리 | Bean Validation, Spring Security, CSRF, 소유권 검사, 예외 Handler | Controller·Service·SecurityConfig 및 테스트 |
| DB 구성·처리 | Flyway V1~V12, JPA 관계, 검색·페이징·멱등 upsert | `src/main/resources/db/migration` |
| HTML 페이지 | Thymeleaf·JavaScript·CSS 반응형 화면 | `src/main/resources/templates`, `static` |
| 회원·비회원 구분 | 회원가입·로그인, 회원 전용 코스·즐겨찾기·여행 기록 | Member·SavedCourse·PlaceFavorite 기능 |
| 파일 업로드·다운로드 | PNG 검증, 회원별 접근 제어, 재시작 후 파일 보존 | TripRecord·FileStorage 기능과 테스트 |
| 구두 발표 | 10장 PPT와 약 6분 발표 대본, 3~4분 시연 | `presentation`, `docs/presentation` |

## 추가점수 근거

| 평가 항목 | City Roulette 구현 |
| --- | --- |
| DB 데이터 생성 프로그램 | TourAPI·부산 공공데이터 수집, 정규화, 검증, 중복 방지, MySQL 저장 Runner |
| DB 관련 별도 프로그램 | 부산버스 XML 파싱과 정류장·노선·정차 순서 수집기 |
| 상업적·학술적 가치 | 실제 부산 여행 문제 해결, 데이터 출처·라이선스·검증 범위 표시 |
| 완성도 | 334개 자동화 테스트, 실제 Chromium QA, 반응형 화면, 예외·빈 상태 처리 |

발표에서는 화면을 나열하기보다 데이터가 API에서 DB로 들어가는 과정, 3계층 구조,
추천 점수와 seed 재현 원리, 회원별 소유권 검사와 파일 보안을 코드와 함께 설명합니다.
