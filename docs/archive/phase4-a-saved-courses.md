# Phase 4-A 회원별 여행 코스 저장 및 관리

## 역할 구분

`TripRecord`는 사용자가 실제로 다녀온 장소의 방문일, 메모, 사진을 기록한다. `SavedCourse`는 Phase 3-C 룰렛이 만든 여행 계획과 당시 추천 근거를 보존한다. 두 기능의 생명주기와 데이터 의미가 달라 별도 테이블과 서비스를 사용한다.

## ERD

```text
members 1 ─── N saved_courses 1 ─── N saved_course_places N ─── 0..1 places
```

- `saved_courses.member_id`: 소유 회원. 요청의 회원 ID가 아니라 인증 Principal로 결정한다.
- `saved_course_places.saved_course_id`: 코스 삭제 시 함께 삭제한다.
- `saved_course_places.place_id`: 최신 Place 상세 링크용 nullable FK다. 원본 삭제 시 `SET NULL`이지만 스냅샷은 유지된다.
- `(saved_course_id, visit_order)`와 `(saved_course_id, place_id)`는 유일하다.
- `saved_courses.version`은 JPA `@Version` 낙관적 잠금에 사용한다.

## 스냅샷 정책

저장 시점의 장소명, 카테고리, 주소, 좌표, 방문 순서, 추천 점수와 이유, 이전 지점 직선거리, 교통 검증 상태, 데이터 제한사항을 `saved_course_places`에 복사한다. 이후 공공데이터가 변경되어도 저장된 계획의 표시 내용은 바뀌지 않는다. Place FK는 최신 상세 페이지 이동에만 사용한다.

시간과 비용 근거 데이터가 없으므로 코스 상태는 `UNKNOWN`을 저장한다. 실제 이동시간이나 요금을 생성하지 않는다.

## 서버 검증 저장 토큰

클라이언트가 장소 목록, 순서, 점수 또는 추천 이유를 POST하도록 허용하지 않는다.

1. `POST /api/courses/generate`가 서버 메모리에 검증된 코스 스냅샷을 등록한다.
2. 암호학적으로 안전한 256비트 랜덤 저장 토큰을 응답의 `saveToken`으로 반환한다.
3. 토큰은 30분 유효하고 한 번만 사용할 수 있다.
4. `POST /api/courses`는 제목과 토큰만 받는다.
5. 저장 서비스가 서버 스냅샷의 장소 수, Place ID 중복, 1부터 시작하는 연속 순서, 좌표, 점수와 거리 범위를 재검증한다.

애플리케이션 재시작 후 토큰은 무효가 된다. 이는 미저장 생성 결과를 영속 데이터로 취급하지 않기 위한 의도된 동작이다.

## DB 마이그레이션

신규 파일: `V10__create_saved_courses.sql`

- `saved_courses` 생성
- `saved_course_places` 생성
- 회원, 코스, Place FK 설정
- 방문 순서와 Place 중복 방지 UNIQUE 설정
- 회원별 최신 조회와 코스 장소 조회 인덱스 설정
- 기존 V1~V9 파일 및 기존 테이블은 변경하지 않는다.

H2 MySQL 호환 모드에서 V1~V10 적용과 Hibernate schema validation이 성공했다. 원본 MySQL과 기존 Phase 2-F 격리 MySQL에는 이번 작업에서 적용하지 않았다.

실제 MySQL 적용 전 절차:

1. 대상 DB가 격리 개발 사본인지 확인한다.
2. `members`, `places`, `trip_records`와 Flyway 이력의 백업 및 복원을 검증한다.
3. 격리 사본에 V10을 적용한다.
4. 신규 테이블, FK, UNIQUE, 인덱스와 기존 테이블 건수를 비교한다.
5. 실패 시 격리 DB를 폐기하고 검증된 백업에서 복원한다.

원본 DB에는 별도 승인과 백업 확인 없이 적용하지 않는다.

## REST API

| Method | URL | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/courses/generate` | 공개 | 코스 생성 및 일회용 저장 토큰 발급 |
| POST | `/api/courses` | 회원 | `{title, generationToken}`으로 저장 |
| GET | `/api/courses?page=0&size=20&title=` | 회원 | 자신의 코스만 페이지 조회 |
| GET | `/api/courses/{id}` | 회원 | 자신의 코스 상세 |
| PATCH | `/api/courses/{id}` | 회원 | `{title, version}` 제목 수정 |
| DELETE | `/api/courses/{id}` | 회원 | 자신의 코스 삭제 |

저장 성공은 201과 `Location` 헤더, 삭제 성공은 204를 반환한다. 없는 코스와 다른 회원 코스는 모두 404로 처리해 존재 여부를 노출하지 않는다. 오래된 version 수정은 409다. 모든 상태 변경 요청은 기존 CSRF 보호를 유지한다.

## 소유권 검증

Controller는 클라이언트의 memberId를 받지 않고 `Principal.getName()`만 Service에 전달한다. Service는 username으로 Member를 조회한 뒤 항상 `findByIdAndMember`로 상세, 수정, 삭제 대상을 찾는다. 목록도 Member 조건을 쿼리에 포함한다.

## UI

- `/recommend`: 로그인 회원에게 코스 제목과 저장 버튼을 표시한다.
- 비회원에게는 로그인 후 저장 안내를 표시한다.
- `/my/courses`: 제목 검색, 최신순 페이지 목록, 대표 장소와 장소 수 표시.
- `/my/courses/{id}`: 스냅샷 방문 순서, 추천 이유, 직선거리, 교통 상태, 제한사항과 Leaflet 지도 표시.
- 상세 페이지에서 제목 수정과 삭제를 지원한다.
- 지도 점선은 실제 이동 경로가 아니라는 설명을 함께 표시한다.

## 테스트 범위

H2 Fixture에서 다음을 검증한다.

- V10 적용 및 JPA schema validation
- 인증 회원 저장과 Principal 소유자 결정
- 비회원 API 차단 및 CSRF 403
- 순서와 추천 스냅샷 보존
- 다른 회원 목록/상세/수정/삭제 차단
- 제목 수정과 오래된 version 충돌
- 중복 Place ID 및 가짜/재사용 토큰 차단
- 유효하지 않은 스냅샷에서 DB 부분 저장 없음
- 코스 삭제 시 자식 정리 및 원본 Place 보존
- 목록 및 상세 Thymeleaf 렌더링과 지도 데이터
- 기존 인증, 파일, 추천, ODsay, 버스 경로 회귀

Java 17 `gradlew clean build`가 성공했으며 전체 295개 테스트가 통과했다(실패 0, 오류 0). Phase 4-A 신규 테스트는 Service 6개, REST/보안 4개, Thymeleaf 페이지 2개다.

## 한계 및 다음 단계

- 저장 토큰 저장소는 단일 애플리케이션 인스턴스 메모리다. 다중 인스턴스에서는 Redis 같은 TTL 공유 저장소가 필요하다.
- 격리 MySQL V10 적용은 미검증이다.
- 장소 추가/삭제와 방문 순서 편집은 지원하지 않는다.
- 공유 링크, 공개/비공개 설정, 즐겨찾기와 협업은 Phase 4-B 후보 기능이다.
