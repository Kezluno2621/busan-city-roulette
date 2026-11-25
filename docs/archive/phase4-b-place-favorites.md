# Phase 4-B 회원별 관광 장소 즐겨찾기

검증일: 2026-10-29 (Asia/Seoul)

## 구현 결과

로그인 회원이 장소를 즐겨찾기에 추가·조회·해제하고 `/my/favorites`에서 다시 볼 수 있다. 장소 목록과 상세 화면은 현재 회원의 저장 상태를 표시하며, 비회원에게는 로그인 안내를 제공한다. 기존 인증, 추천, 룰렛, 저장 코스, 공공데이터 수집 기능은 변경하지 않았다.

## DB 구조

Flyway `V11__create_place_favorites.sql`이 다음 테이블을 생성한다.

| 컬럼 | 형식 | 조건 |
|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT |
| `member_id` | BIGINT | `members.id` FK, NOT NULL |
| `place_id` | BIGINT | `places.id` FK, NOT NULL |
| `created_at` | DATETIME(6) | NOT NULL |

- `UNIQUE(member_id, place_id)`로 같은 회원의 중복 저장을 DB에서도 차단한다.
- 회원이나 장소가 정상적인 도메인 절차로 삭제될 때 고아 즐겨찾기가 남지 않도록 FK에 `ON DELETE CASCADE`를 적용했다.
- 회원별 최신순 조회 인덱스: `idx_place_favorites_member_created(member_id, created_at)`
- 장소 역참조 인덱스: `idx_place_favorites_place(place_id)`
- 기존 V1~V10 파일은 수정하지 않았다.

## REST API

모든 API는 로그인과 기존 CSRF 보호가 필요하며, 회원 ID는 요청에서 받지 않고 `Principal`에서만 결정한다.

| Method | URL | 동작 | 성공 응답 |
|---|---|---|---|
| POST | `/api/favorites` | `{ "placeId": 1 }` 저장 | 최초 201, 이미 저장됨 200 |
| GET | `/api/favorites?page=0&size=20` | 현재 회원 목록 | 200, 최대 50건 페이징 |
| DELETE | `/api/favorites/{placeId}` | 현재 회원의 항목만 해제 | 204 |

존재하지 않는 Place는 404, 잘못된 요청은 400, 동시 삽입 UNIQUE 충돌은 409로 응답한다. 다른 회원이 같은 `placeId`를 삭제해도 현재 인증 회원의 행만 삭제 조건에 포함되므로 소유권을 침범하지 않는다.

## 서비스와 프론트엔드

- 계층: `PlaceFavoriteController` → `PlaceFavoriteService` → `PlaceFavoriteRepository`
- 목록은 `PlaceFavorite`와 `Place`를 EntityGraph로 함께 읽고 Entity를 JSON에 직접 노출하지 않는다.
- 장소 목록은 현재 페이지의 Place ID만 일괄 조회해 카드별 N+1 쿼리를 피한다.
- `/places`, `/places/{id}`에 즐겨찾기 추가·해제 버튼과 처리 결과 피드백을 추가했다.
- `/my/favorites`에 회원 전용 카드 목록, 빈 상태, 페이지네이션을 추가했다.
- 기존 Thymeleaf, CSS, JavaScript와 CSRF meta 값을 재사용했으며 새 프론트엔드 프레임워크는 도입하지 않았다.

## 자동 테스트

- 추가, 순차 중복 추가의 멱등 응답
- 삭제 후 원본 Place 보존
- 회원별 목록과 다른 회원 삭제 격리
- 존재하지 않는 Place 거부
- 페이지네이션 입력 검증
- 비회원 API·페이지 차단
- CSRF 없는 변경 요청 차단
- 6개 동시 요청 후 DB 행 1개 유지
- H2 V1~V11 및 Hibernate schema validation
- 기존 기능 전체 회귀 테스트

최종 Java 17 `gradlew clean build` 결과는 아래 실제 실행 결과 절에 기록한다.

## 격리 MySQL V11 검증

원본 MySQL에는 연결하거나 쓰지 않았다. Phase 4-A-2의 V10 격리 사본을 새 디렉터리 `.runtime-backups/phase4b-restore-proof/data`로 복제하고 localhost 전용 새 포트 `33310`에서만 실행했다.

적용 전 복구 수단:

- 전체 데이터 디렉터리 복제: 180 files, 192,660,764 bytes
- 논리 백업: `.runtime-backups/phase4b-restore-proof/phase4b-pre-v11.sql.bak`
- 백업 크기: 29,883 bytes
- SHA-256: `DFC0F548B9D2136616DF84D792265924C440D28D54511E1B19322D690942FEAD`

적용 결과:

| 항목 | 결과 |
|---|---|
| MySQL | 8.0.46 |
| 적용 전 Flyway | V10 |
| Flyway 검증 | 12 migrations 성공 |
| V11 적용 | 성공 |
| Hibernate validation | 성공 |
| FK | member, place 2개 확인 |
| UNIQUE | `(member_id, place_id)` 확인 |
| 보조 인덱스 | 2개 확인 |

실제 dev 앱 HTTP 검증:

| 검증 | 결과 |
|---|---|
| 회원 A 최초 등록 | HTTP 201 |
| 회원 A 같은 장소 재등록 | HTTP 200, 총 1건 |
| 회원 B 목록 | 0건 |
| 회원 B가 같은 Place ID 해제 | 회원 A 1건 유지 |
| 인증 세션 CSRF 누락 DELETE | HTTP 403 |
| 회원 A 정상 해제 | HTTP 204, 최종 0건 |
| `/my/favorites` | HTTP 200 |
| 비회원 장소 목록 안내 | HTTP 200, 로그인 저장 안내 표시 |

임시 회원 두 명만 정리한 최종 보존 결과:

| 테이블 | 적용/검증 전 | 정리 후 |
|---|---:|---:|
| `places` | 10 | 10 |
| `members` | 1 | 1 |
| `trip_records` | 0 | 0 |
| `saved_courses` | 0 | 0 |
| `place_favorites` | 없음 | 0 |

Place ID `1~10`도 동일하게 유지됐다. 검증 종료 후 dev 앱과 격리 MySQL은 정상 종료했다.

## 미검증 및 운영 전 확인

- GUI 브라우저를 직접 조작한 반응형·접근성 시각 QA는 수행하지 않았다. 실제 HTTP 렌더링과 자동 테스트까지만 확인했다.
- 원본/운영 MySQL V11 적용은 금지 조건에 따라 수행하지 않았다.
- 여러 애플리케이션 인스턴스의 완전 동시 요청은 DB UNIQUE로 보호되며 충돌 요청은 409가 될 수 있다. 클라이언트는 현재 상태를 다시 조회하면 된다.
- 운영 반영 전 별도 승인된 백업·복원 절차와 V11 적용 점검이 필요하다.

Git commit/push는 수행하지 않았다.
