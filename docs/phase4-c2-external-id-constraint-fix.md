# Phase 4-C-2-FIX 외부 ID 제약조건 및 멱등성 검증

작성일: 2026-10-31 (Asia/Seoul)

## 결론

부산갈맷길 적재 실패는 수집기의 공급자 구분 누락이 아니라 V2에서 생성된 `places.external_id` 전역 UNIQUE 인덱스가 V5 이후에도 남아 있었기 때문에 발생했다. V12에서 이 레거시 인덱스만 제거했다. V5의 `UNIQUE(source, external_id)`와 V6의 `UNIQUE(provider, external_id)`는 그대로 유지한다.

최종 V12를 70건 기준선에서 만든 별도 MySQL 8.0.46 격리 복제본에 적용했다. 갈맷길 20건 저장과 즉시 재수집, 이어서 다섯 공급자 전체 재수집을 완료했으며 Place와 PlaceSource 중복 증가는 없었다.

## 기존 구조 조사

| 질문 | 확인 결과 |
|---|---|
| `places.external_id` 사용처 | 응답 DTO와 레거시 TourAPI 호환 조회에 남아 있음. 즉시 삭제할 수 없음 |
| `places` 공급자 정보 | `source` 컬럼이 존재하며 다중 공급자 신규 Place에는 provider 값이 저장됨 |
| Place 복합 고유성 | V5의 `uk_places_source_external_id(source, external_id)`가 이미 존재 |
| PlaceSource 고유성 | `uk_place_sources_provider_external(provider, external_id)`가 이미 존재 |
| 수집기 중복 조회 | `PlaceImportService`는 먼저 `PlaceSourceRepository.findByProviderAndExternalId` 사용 |
| 레거시 TourAPI 처리 | `TourismPageImporter`도 PlaceSource를 우선 조회하고, 기존 `KOR_SERVICE` Place만 호환 조회 |
| 다른 공급자의 같은 ID | V12 이후 별도 Place/PlaceSource 생성 가능. 유사성만으로 자동 병합하지 않음 |

V2의 `uk_places_external_id(external_id)`와 V5의 복합 UNIQUE가 동시에 존재해, 더 좁은 V2 제약이 공급자가 다른 같은 ID를 먼저 거부하고 있었다. 실제 충돌 ID는 `258`이었다.

## V12 마이그레이션

파일: `src/main/resources/db/migration/V12__scope_legacy_place_external_id_by_source.sql`

```sql
DROP INDEX uk_places_external_id ON places;
```

변경 전:

- `UNIQUE(external_id)` — 제거 대상
- `UNIQUE(source, external_id)` — 유지
- `place_sources UNIQUE(provider, external_id)` — 유지

변경 후:

- Place 내부 PK `id`와 모든 Place FK 유지
- 레거시 `external_id` 컬럼 유지
- 같은 공급자 안의 Place 외부 ID 중복 차단
- 다른 공급자의 동일 외부 ID 허용
- PlaceSource의 공급자별 중복 차단 유지

MySQL DDL은 일반 트랜잭션 롤백에 의존하지 않았다. V12 적용 전 70건 격리 사본을 별도로 복제하고 논리 백업을 생성했다.

## 격리 DB 및 백업

- 원본 DB: 접근·변경하지 않음
- 기준선: Place 70, PlaceSource 70, ImportRun 10, Flyway V11
- 최종 검증 복제본: 로컬 격리 포트 33314
- 적용 DB: MySQL 8.0.46, Flyway V12
- V12 전 백업 SHA-256: `D4B5696DC1BAC4F12A55E10A468BC25AD5A0A41118ABFB09A80B99AAB3A8B25D`
- V12 적용 후 Hibernate schema validation: 성공
- 기존 Place ID 1~70: 모두 보존
- V12 직후 Place 1~70 주요 데이터: 변경 없음
- Member 1건 보존, TripRecord/SavedCourse/Favorite는 기준선에서 각각 0건

실제 최종 인덱스:

- `places.uk_places_source_external_id(source, external_id)` UNIQUE
- `place_sources.uk_place_sources_provider_external(provider, external_id)` UNIQUE
- 기존 PK, 검색 인덱스, PlaceSource FK 유지

## 수집기 변경 여부

`PlaceImportService`의 중복 판단 순서는 이미 요구 정책과 일치해 운영 로직을 변경하지 않았다.

1. `(provider, externalId)`로 PlaceSource 조회
2. 존재하면 연결 Place와 Source 갱신
3. 없으면 공급자별 신규 Place와 PlaceSource 생성
4. 정규화 이름·주소/좌표가 유사하면 자동 병합하지 않고 후보만 기록

V12에 맞춰 `TourismImportVerificationRunner`의 쓰기 안전조건만 Flyway V12 필수로 갱신했다.

## 갈맷길 저장 및 재수집

| 단계 | Fetched | Inserted | Updated | Skipped | Failed |
|---|---:|---:|---:|---:|---:|
| DRY_RUN | 20 | 20(예상) | 0 | 0 | 0 |
| 첫 저장 | 20 | 20 | 0 | 0 | 0 |
| 동일 조건 재수집 | 20 | 0 | 20 | 0 | 0 |

- Place/Source: 70/70 → 90/90 → 90/90
- 기존 ID 보존: true
- 기존 70건 주요 필드 변경: 0
- `reimportStable`: true
- 기존 충돌 ID `258`: 공급자별 Source로 정상 공존

## 전체 공급자 재수집

갈맷길 검증 후 동일 표본 조건(공급자별 최대 20건)으로 두 번 실행했다.

| Provider | 각 실행 Fetched | 각 실행 Inserted | 각 실행 Updated | 각 실행 Skipped | Failed |
|---|---:|---:|---:|---:|---:|
| TOUR_API | 20 | 0 | 20 | 0 | 0 |
| BUSAN_ATTRACTION | 20 | 0 | 20 | 0 | 0 |
| BUSAN_RESTAURANT | 20 | 0 | 20 | 0 | 0 |
| BUSAN_FOODIE | 20 | 0 | 0 | 20 | 0 |
| BUSAN_GALMAETGIL | 20 | 0 | 20 | 0 | 0 |

- 첫 전체 재수집 후 Place/Source: 90/90
- 두 번째 전체 재수집 후 Place/Source: 90/90
- `(provider, external_id)` 중복: 0
- PlaceSource FK 고아 행: 0
- 공급자가 다른 동일 external_id: 4개 존재하며 정상 공존
- 최종 ImportRun: 22 (SUCCESS 21, 과거 FIX 전 갈맷길 실패 1)
- 푸디투어 테마 콘텐츠는 계속 Place 저장에서 제외

## 데이터 품질

- Place: 90
- PlaceSource: 90
- 공급자: TOUR_API 30, 부산명소 20, 부산맛집 20, 부산갈맷길 20
- 좌표 누락: 0 (0%)
- 주소 누락: 20 (22.22%, 갈맷길 원천 표본)
- 이미지 누락: 10 (11.11%)
- 병합 후보: 0
- 카테고리: 관광지 50, 음식점 37, 행사/공연/축제 3
- 구·군 미확인: 20 (갈맷길 주소 누락과 동일 범위)
- 신규 3개 부산 공급자의 attribution/content/image license 누락: 0
- 기존 TourAPI 10건의 content/image license 미확인 상태는 유지

주소나 좌표를 임의 생성하지 않았고 이미지 라이선스 정책도 완화하지 않았다.

## 추천 및 룰렛 검증

최종 90건 격리 DB에서 공개 REST API를 DB 쓰기 없이 호출했다.

- 추천 후보 89, 결과 10
- 동일 seed 추천 재현: 성공
- 룰렛 코스 5개 생성
- 룰렛 중복 Place: 0
- 룰렛 좌표: 5/5
- 동일 seed 코스 재현: 성공
- 저장 코스·즐겨찾기: 자동 회귀 테스트로 검증, 실제 회원 계정 UI 조작은 미수행
- 실제 브라우저 Leaflet 렌더링: 미검증

## 테스트

- 서로 다른 공급자의 동일 external ID 저장 테스트 추가
- 동일 공급자의 같은 ID 재수집 시 갱신·중복 방지 테스트 추가
- H2 MySQL 모드에서 V1~V12 적용 검증
- 실제 MySQL 8.0.46에서 최종 V12 적용 및 Hibernate validation 성공
- Java 17 `gradlew clean build`: 성공
- 전체 자동 테스트: 311개 통과, 실패 0, 무시 0 (기존 309 + 신규 2)

## 미완료 및 다음 단계

- 원본 MySQL에는 V12를 적용하지 않았다.
- 대량 수집은 수행하지 않았다.
- 실제 브라우저 지도와 회원 코스/즐겨찾기 UI는 수동 미검증이다.
- Phase 4-C-3의 단계적 확대 수집은 V12를 적용할 대상 DB의 별도 백업과 명시적 승인 후 진행해야 한다.

소량 다중 공급자 적재와 재수집 멱등성 기준은 충족했다.

