# Phase 4-A-2 격리 MySQL 코스 저장 검증

검증일: 2026-10-29 (Asia/Seoul)

## 결론

Phase 2-F의 V9 격리 MySQL 사본을 새 데이터 디렉터리로 복제하고, 그 복제본에만 V10을 적용했다. 원본 STS MySQL과 기존 Phase 2-F 격리 사본은 변경하지 않았다.

수정된 V10은 MySQL 8.0.46에서 적용됐고 Hibernate schema validation도 통과했다. 실제 HTTP 세션과 CSRF를 사용한 생성·저장·목록·상세·제목 수정·소유권 차단·삭제가 성공했다.

## 환경과 복구 수단

- Codex 터미널 DB 관련 환경변수: 모두 미설정
- 검증 원본: Phase 2-F에서 V9와 버스 표본이 적용된 격리 사본
- 작업 사본: `.runtime-backups/phase4a2-restore-proof-v2/data`
- MySQL: 8.0.46
- 작업 전 Flyway: V9
- 작업 전 데이터: Place 10, Member 1, TripRecord 0, BusStop 30, BusRoute 1, BusRouteStop 20
- 논리 백업: `.runtime-backups/phase4a2-pre-v10-20261029.sql.bak`
- 백업 크기: 26,066 bytes
- SHA-256: `15916BCE67E4A2330630DF540A98C751DAB465D2F964FEDAF507A94AB413E415`

전체 데이터 디렉터리 복제본과 논리 백업이 모두 있어 실패 시 새 검증 사본을 폐기하고 다시 복제·복원할 수 있다.

## 최초 MySQL 적용에서 발견한 문제

H2에서 통과했던 `saved_course_places.recommendation_reason`과 `data_limitations`의 `VARCHAR(10000)` 조합이 MySQL `utf8mb4`의 최대 행 크기를 초과했다.

- MySQL 오류: 1118, `Row size too large`
- 최초 V10 결과: 실패
- 실패한 작업 사본: 보존
- 원본/기존 격리 사본 영향: 없음

V10이 실제 DB에 성공 적용된 적이 없는 상태에서 두 스냅샷 컬럼을 `VARCHAR(5000)`으로 조정했다. 새 V9 복제본을 만들고 H2 전체 빌드를 다시 통과시킨 다음 MySQL 적용을 재시도했다.

## V10 적용 결과

| 항목 | 결과 |
|---|---|
| Flyway 검증 | 11 migrations 성공 |
| V10 적용 | 성공 |
| 최종 버전 | 10 |
| Hibernate schema validation | 성공 |
| `saved_courses` | 생성 |
| `saved_course_places` | 생성 |
| FK | 3개 |
| UNIQUE | 2개 |
| `version` 컬럼 | 확인 |
| 명시적 보조 인덱스 | 2개 |

확인한 제약조건:

- `fk_saved_courses_member`
- `fk_saved_course_places_course`
- `fk_saved_course_places_place`
- `uk_saved_course_visit_order`
- `uk_saved_course_place`

확인한 인덱스:

- `idx_saved_courses_member_created`
- `idx_saved_course_places_course`

## 실제 CRUD 및 보안 검증

격리 DB에만 임시 회원 A/B를 공개 회원가입 흐름으로 만들고, 회원 A의 실제 로그인 세션에서 코스를 생성했다.

| 검증 | 결과 |
|---|---|
| 코스 생성 | 2개 장소, 저장 토큰 발급 |
| 코스 저장 | 2개 장소 스냅샷 저장 |
| 회원 A 목록 | 저장 코스 포함 |
| 회원 A 상세 | 장소 2개와 순서 확인 |
| 제목 수정 | 성공, version 0 → 1 |
| 오래된 version 재수정 | HTTP 409 |
| 저장 토큰 재사용 | HTTP 400 |
| 새 인증 세션의 CSRF 누락 | HTTP 403 |
| 비회원 저장 | 로그인으로 차단, HTTP 302 |
| 회원 B 상세 조회 | HTTP 404 |
| 회원 B 제목 수정 | HTTP 404 |
| 회원 B 삭제 | HTTP 404 |
| 회원 A 삭제 | HTTP 204 |
| 삭제 후 상세 | HTTP 404 |
| 삭제 후 자식 정리 | `saved_course_places` 0 |
| `/my/courses` | 인증 세션 HTTP 200 |
| `/my/courses/{id}` | 인증 세션 HTTP 200 |

30분 만료 동작은 실제로 기다리지 않고 주입 가능한 Clock을 사용한 자동 테스트로 검증했다. 실제 MySQL HTTP 흐름에서는 일회용 토큰 재사용 차단을 확인했다.

## 데이터 보존과 정리

테스트 코스를 먼저 삭제한 뒤 임시 회원 A/B만 명시적으로 삭제했다. 최종 결과:

| 테이블 | 작업 전 | 작업 후 |
|---|---:|---:|
| places | 10 | 10 |
| members | 1 | 1 |
| trip_records | 0 | 0 |
| bus_stops | 30 | 30 |
| bus_routes | 1 | 1 |
| bus_route_stops | 20 | 20 |
| saved_courses | 없음 | 0 |
| saved_course_places | 없음 | 0 |

Place ID `1~10`도 유지됐다. 테스트 과정에서 기존 회원 비밀번호, 기존 Place, TripRecord 또는 버스 표본을 수정·삭제하지 않았다.

## 웹 화면 검증 범위

실제 격리 MySQL에 연결한 dev 애플리케이션에서 인증 세션으로 `/recommend`, `/my/courses`, `/my/courses/{id}`의 HTTP 응답과 렌더링 마크업을 확인했다. 별도 GUI 브라우저에서 애니메이션과 지도 타일을 시각적으로 조작하는 검증은 수행하지 않았으므로 브라우저 시각 QA는 미검증이다.

## 자동 테스트

- Java 17 `gradlew clean build`: 성공
- 전체 테스트: 296개 통과
- 실패/오류: 0
- H2 V1~V10 및 Hibernate validation: 성공
- 만료 토큰 자동 테스트 추가
- 기존 인증, 파일, 추천, ODsay, 버스 경로 회귀 포함

## 변경 파일

실제 MySQL 호환성 수정:

- `V10__create_saved_courses.sql`: 자식 스냅샷 JSON 컬럼 10,000 → 5,000자
- `SavedCoursePlace.java`: 동일 길이 매핑 반영
- `GeneratedCourseRegistry.java`: 만료 테스트를 위한 Clock 주입 가능 구조
- `GeneratedCourseRegistryTest.java`: 30분 TTL 만료 검증

## 미완료 사항

- 원본 MySQL V10 적용: 금지 조건에 따라 미수행
- GUI 브라우저 시각 QA: 미검증
- 다중 서버 환경의 공유 토큰 저장소: 미구현
- 실제 운영 배포 전 백업/복원 훈련과 V10 적용 승인: 필요

검증 종료 후 dev 애플리케이션과 격리 MySQL은 정상 종료했다. Git commit/push는 수행하지 않았다.
