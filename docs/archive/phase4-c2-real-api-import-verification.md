# Phase 4-C-2 STS 실제 API 수집 및 격리 DB 검증

후속 외부 ID 충돌 수정 및 멱등성 검증: [Phase 4-C-2-FIX](phase4-c2-external-id-constraint-fix.md)

작성일: 2026-10-31 (Asia/Seoul)

## 현재 결론

다중 공급자 수집기를 재작성하지 않고 `TourismImportVerificationRunner`를 추가했다. Runner는 `dev` 프로필에서 명시적으로 활성화했을 때만 실행되며 `INSPECT`, `DRY_RUN`, `WRITE_VERIFY`를 지원한다.

STS 워크스페이스의 기존 City Roulette 실행 설정에서 다섯 API 키가 모두 설정된 것을 값 노출 없이 확인했다. STS의 Eclipse `bin/main` 출력물이 오래되어 직접 Spring Boot 실행은 `PlaceResponse` 로딩에서 실패했지만, 독립 Gradle 빌드는 정상이다. 따라서 기존 STS `.launch`의 Environment를 새 파일에 복사하지 않고 실행 프로세스 메모리로만 상속한 Gradle `bootRun` 대체 경로로 실제 INSPECT, DRY_RUN, 승인된 WRITE_VERIFY를 실행했다.

WRITE_VERIFY는 TourAPI·부산명소·부산맛집까지 각각 20건을 커밋했고, 부산푸디투어 20건을 테마 콘텐츠로 건너뛴 뒤 부산갈맷길 저장 중 기존 `places.external_id` 전역 UNIQUE 제약조건 충돌로 중단됐다. 사용자 지정 중단 조건에 따라 두 번째 재수집을 수행하지 않았다. 따라서 결과는 **3개 공급자 부분 적재 성공, 갈맷길 적재 실패, 재수집 멱등성 미검증**이다.

## 구현한 검증 모드

### INSPECT

- DB 이름, 비밀 없는 JDBC URL SHA-256, Flyway 버전 확인
- Place, PlaceSource, 병합 후보, ImportRun 건수 확인
- 공급자별 API 키의 설정 여부만 `true/false`로 출력
- 키 값, JDBC URL, 비밀번호, 전체 요청 URL은 출력하지 않음
- API 호출과 DB 쓰기 없음

### DRY_RUN

- 요청한 공급자의 키가 모두 설정됐는지 확인
- 공급자 순서: TourAPI → 부산명소 → 부산맛집 → 부산푸디 → 갈맷길
- 공급자별 최대 20건만 기존 Client/Mapper/Importer로 수집·정규화
- 푸디 테마는 Place로 저장하지 않고 skipped로 집계
- 한 공급자라도 실패하거나 fetched=0/failed>0이면 비정상 종료
- Place, PlaceSource, 병합 후보, ImportRun 쓰기 없음

### WRITE_VERIFY

동일 프로세스에서 DRY_RUN이 먼저 성공한 뒤에만 진입한다. 다음 조건이 모두 맞아야 한다.

1. 격리 DB임을 사람이 명시적으로 확인
2. 예상 DB 이름 일치
3. Flyway V11
4. 실제 JDBC URL SHA-256이 예상 격리 URL 지문과 일치
5. 실제 URL 지문이 원본 DB URL 지문과 다름
6. 백업 파일 존재
7. 백업 파일 SHA-256 일치

첫 저장과 동일 조건 재수집을 연속 실행하고 Place·PlaceSource·병합 후보 건수와 Place 주요 필드 스냅샷이 두 번째 실행에서 안정적인지 확인한다. 기존 Place ID가 하나라도 사라지면 실패한다. 원천 데이터의 정상 갱신으로 기존 행의 주요 필드가 달라진 경우에는 변경 건수를 별도 보고한다.

## 준비된 격리 DB

- 데이터 디렉터리: `.runtime-backups/phase4c2-real-api-proof/data`
- MySQL: 8.0.46
- 로컬 검증 포트: 33312
- DB: `cityroulette`
- Flyway: V11
- Place: 10
- PlaceSource: 10
- 병합 후보: 0
- ImportRun: 5 (기존 버스 수집 이력)
- 기존 Place ID: 1~10
- 실제 Gradle 검증 경로 JDBC URL SHA-256: `E96DC2D9B2B432EC10DF5E5FB698224A0FB83947D03A46479AC3B74C94AF04AE`
- 원본 DB JDBC URL SHA-256: `8E71AF2848E58D2B69099F2BBDC5070B7DD2BC1852040DA18F261A757F84F4F8` (격리 지문과 불일치)

복구 수단:

- 논리 백업: `.runtime-backups/phase4c2-real-api-proof/phase4c2-pre-import.sql.bak`
- 백업 크기: 31,205 bytes
- 백업 SHA-256: `E918A67D43969BEFF2C7E664F17F0DA2EC82069A16C2AA69BA47422CC7848410`

이 지문들은 비밀번호나 API 키를 포함하지 않는다. 원본 MySQL은 연결하거나 변경하지 않았다.

## STS 실행 방법

기존 Spring Boot Run Configuration을 복제하여 Phase 4-C-2 전용 설정을 만든다. 기존 다섯 API 키 환경변수는 값 변경 없이 그대로 재사용한다.

실제 워크스페이스의 `.metadata/.plugins/org.eclipse.debug.core/.launches`에서 City Roulette 실행 설정 두 개를 확인했다. 이 중 프로젝트 전체 설정에는 다섯 API 키와 DB 환경변수 이름이 모두 존재했다. 키 값은 출력하지 않았다. STS 직접 실행은 오래된 Eclipse 출력물 때문에 실패하여, 아래 세션에서는 기존 `.launch`의 Environment를 프로세스 메모리에서만 읽는 임시 Gradle 실행 스크립트를 사용했다. API 키를 소스, 명령행, 새 설정 파일에 하드코딩하지 않았다.

전용 설정 생성 절차:

1. 기존 City Roulette Spring Boot App 설정을 선택하고 `Duplicate`한다.
2. Project는 `busan-city-roulette`, Main type은 `com.cityroulette.CityRouletteApplication`인지 확인한다.
3. Profile 입력란에 `dev`를 지정한다. Profile 입력란을 사용하지 않는 설정이라면 Program arguments에 `--spring.profiles.active=dev`를 추가한다. 두 방식을 중복 지정하지 않는다.
4. Program arguments에 `--spring.main.web-application-type=none`을 추가한다.
5. Environment에서 기존 다섯 API 키의 값은 열람하거나 복사하지 않고 변수명이 존재하는지만 확인한다.
6. 아래 공통 환경변수와 모드별 변수를 추가한 뒤 `Apply`한다.

필수 API 키 변수명:

```text
TOUR_API_KEY
BUSAN_ATTRACTION_API_KEY
BUSAN_RESTAURANT_API_KEY
BUSAN_FOODIE_API_KEY
BUSAN_GALMAETGIL_API_KEY
```

공통 조건:

- Active profile: `dev`
- Program arguments: `--spring.main.web-application-type=none`
- `RUNTIME_ALLOW_MIGRATIONS=false`
- `PLACE_IMPORT_ENABLED=false`
- `TOURISM_VERIFICATION_ENABLED=true`
- DB 환경변수는 위 격리 MySQL 사본을 가리켜야 함
- 전체 URL이나 키 값은 콘솔에 출력하지 않음

공통 환경변수의 실제 이름과 값:

```text
TOURISM_VERIFICATION_ENABLED=true
TOURISM_VERIFICATION_PROVIDERS=ALL
TOURISM_VERIFICATION_PAGE_SIZE=20
TOURISM_VERIFICATION_LIMIT=20
RUNTIME_ALLOW_MIGRATIONS=false
PLACE_IMPORT_ENABLED=false
```

DB 연결에는 기존 코드가 읽는 `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`를 사용한다. 이 중 값은 문서나 콘솔에 기록하지 않는다. `DB_PORT`와 `DB_NAME`을 포함한 연결 대상은 격리 DB와 일치해야 하며, 실행 전에 격리 MySQL 프로세스가 실제로 기동 중인지 확인한다.

### 1. 원본 DB INSPECT

원본 DB 설정에서 다음 모드만 실행하여 로그의 `jdbcUrlSha256`을 기록한다. 읽기 전용 검사이며 API 호출과 쓰기는 없다. 이 값을 이후 `TOURISM_VERIFICATION_ORIGINAL_JDBC_URL_SHA256`으로 사용한다. 원본 DB에서는 `DRY_RUN`이나 `WRITE_VERIFY`를 실행하지 않는다.

```text
TOURISM_VERIFICATION_MODE=INSPECT
```

### 2. 격리 DB INSPECT

격리 DB 연결로 변경한 뒤 다시 INSPECT한다. 다음이 보여야 한다.

```text
mode=INSPECT
flywayVersion=11
counts=places 10, sources 10, mergeCandidates 0, importRuns 5
keys={TOUR_API=true, BUSAN_ATTRACTION=true, BUSAN_RESTAURANT=true, BUSAN_FOODIE=true, BUSAN_GALMAETGIL=true}
```

2026-10-31 실제 실행에서는 다섯 keys가 모두 `true`였고 MySQL 8.0.46, Flyway V11, Place 10건, PlaceSource 10건을 확인했다.

다음 항목을 모두 확인하기 전에는 DRY_RUN으로 진행하지 않는다.

- `mode=INSPECT`
- 실제 DB 엔진 MySQL 8.0.46
- `flywayVersion=11`
- `keys`의 다섯 값이 모두 `true`
- `jdbcUrlSha256=E96DC2D9B2B432EC10DF5E5FB698224A0FB83947D03A46479AC3B74C94AF04AE`
- 원본 DB INSPECT 지문과 격리 DB 지문이 서로 다름
- 백업 파일 존재 및 SHA-256 일치

### 3. DRY_RUN

```text
TOURISM_VERIFICATION_MODE=DRY_RUN
TOURISM_VERIFICATION_PROVIDERS=ALL
TOURISM_VERIFICATION_PAGE_SIZE=20
TOURISM_VERIFICATION_LIMIT=20
```

각 공급자에 대해 `TOURISM_VERIFY_RESULT stage=DRY_RUN` 로그를 확인한다. 실제 저장은 없어야 한다.

Runner의 DRY_RUN 성공은 기존 Client가 HTTP/공급자 응답 오류 없이 파싱되고, Mapper/Importer가 적어도 한 건을 가져오며 `failed=0`인 경우를 뜻한다. 현재 Runner 로그에는 HTTP 상태와 공급자 자체 결과코드를 별도 필드로 출력하지 않으므로 실제 로그에 없는 값을 추측해 기록하지 않는다. 공급자별 `fetched`, `duplicates`, `skipped`, `failed`를 그대로 결과 표에 옮긴다.

### 4. WRITE_VERIFY

DRY_RUN 전체 성공 후에만 다음을 추가한다.

```text
TOURISM_VERIFICATION_MODE=WRITE_VERIFY
TOURISM_VERIFICATION_EXPECTED_DATABASE=cityroulette
TOURISM_VERIFICATION_EXPECTED_JDBC_URL_SHA256=E96DC2D9B2B432EC10DF5E5FB698224A0FB83947D03A46479AC3B74C94AF04AE
TOURISM_VERIFICATION_ORIGINAL_JDBC_URL_SHA256=<원본 INSPECT에서 얻은 서로 다른 지문>
TOURISM_VERIFICATION_ISOLATED_DATABASE_CONFIRMED=true
TOURISM_VERIFICATION_BACKUP_FILE=<phase4c2-pre-import.sql.bak의 절대 경로>
TOURISM_VERIFICATION_BACKUP_SHA256=E918A67D43969BEFF2C7E664F17F0DA2EC82069A16C2AA69BA47422CC7848410
```

`TOURISM_VERIFY_ASSERTIONS originalIdsPreserved=true reimportStable=true`가 아니면 검증 실패다.

WRITE_VERIFY를 실행하기 전에 사용자에게 다음 변경 범위를 제시하고 확인한다.

- 대상: 위 SHA-256으로 확인된 격리 MySQL `cityroulette`만
- 공급자별 최대 20건, 전체 API 호출은 5개 공급자의 소량 표본만
- 푸디투어 테마 콘텐츠는 Place 저장 대상에서 제외
- Place를 생성할 수 있는 나머지 네 공급자의 이론상 신규 Place/PlaceSource 상한은 각각 합계 80건이나, 기존 원천·누락·중복으로 실제 수는 더 적을 수 있음
- 기존 동일 원천은 갱신될 수 있으나 다른 공급자의 유사 장소를 자동 병합하지 않음
- 첫 저장과 동일 조건 재수집을 수행하므로 ImportRun은 최대 10건 증가할 수 있음
- 기존 Place ID 1~10, 회원, 코스, 즐겨찾기 데이터는 삭제하지 않음

DRY_RUN에서 한 공급자라도 실패하면 WRITE_VERIFY를 실행하지 않는다. 안전 승인 변수를 채우더라도 Runner 내부의 DRY_RUN 선행과 지문·백업 검증은 우회되지 않는다.

## STS를 사용할 수 없을 때의 대체 실행

대체 실행도 STS와 동일한 환경변수를 이미 전달받는 셸에서만 가능하다. 키나 DB 비밀번호를 명령행 인수로 붙이지 않는다. Codex 터미널처럼 키가 없는 셸에서는 실제 API 검증 경로가 될 수 없다.

Gradle 실행:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev --spring.main.web-application-type=none"
```

빌드된 실행 JAR 사용:

```powershell
java -jar build\libs\city-roulette-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --spring.main.web-application-type=none
```

`TOURISM_VERIFICATION_ENABLED`의 기본값은 `false`이므로 명시적으로 활성화하지 않은 일반 서버 시작에서는 Runner가 실행되지 않는다. `build.gradle`의 `verifyDevelopmentData`는 별도 읽기 전용 검증 Main을 실행하는 태스크이므로 이번 관광 수집 Runner 실행 명령으로 사용하지 않는다.

## 이번 세션 실행 결과

### INSPECT

| 항목 | 결과 |
|---|---|
| 격리 MySQL 연결 | 성공 |
| Flyway V11 | 확인 |
| Place/Source | 10/10 |
| Place ID | 1~10 보존 |
| API 키 | 기존 STS 설정에서 5개 모두 설정, Runner에서 모두 `true` 확인 |
| 비밀정보 로그 노출 | 없음 |

### DRY_RUN

기존 STS Environment를 프로세스 메모리로만 상속하여 다섯 공급자의 실제 API를 호출했다. 공급자별 최대 20건 제한을 지켰고 failed는 모두 0이었다. 부산푸디투어 20건은 미식 테마 콘텐츠로 분류되어 Place 저장 대상에서 전부 skipped 처리됐다. 실행 후 DB 건수는 Place 10, Source 10, 병합 후보 0, ImportRun 5로 동일했고 Place ID 1~10도 보존됐다.

| Provider | 실제 연결 | Fetched | 정규화 | 저장 |
|---|---|---:|---:|---:|
| TOUR_API | 성공 | 20 | 20 | 0 |
| BUSAN_ATTRACTION | 성공 | 20 | 20 | 0 |
| BUSAN_RESTAURANT | 성공 | 20 | 20 | 0 |
| BUSAN_FOODIE | 성공(테마 분류) | 20 | 0, skipped 20 | 0 |
| BUSAN_GALMAETGIL | 성공 | 20 | 20 | 0 |

### WRITE_VERIFY

사용자 승인 후 격리 MySQL `localhost:33312/cityroulette`에만 실행했다. 실행 직전 Flyway V11, 격리/원본 JDBC 지문 불일치, 백업 존재와 SHA-256 일치, Place/Source 10/10, Place ID 1~10을 다시 확인했다. WRITE_VERIFY 내부 선행 DRY_RUN도 다섯 공급자 모두 다시 성공했다.

첫 저장 결과:

| Provider | Fetched | Inserted | Updated | Skipped | Failed | 결과 |
|---|---:|---:|---:|---:|---:|---|
| TOUR_API | 20 | 20 | 0 | 0 | 0 | 커밋 |
| BUSAN_ATTRACTION | 20 | 20 | 0 | 0 | 0 | 커밋 |
| BUSAN_RESTAURANT | 20 | 20 | 0 | 0 | 0 | 커밋 |
| BUSAN_FOODIE | 20 | 0 | 0 | 20 | 0 | 테마 콘텐츠 제외, 커밋 |
| BUSAN_GALMAETGIL | 20 | 0 | 0 | 0 | 1 | 롤백 후 전체 실행 중단 |

- Place: 10 → 70 (신규 60)
- PlaceSource: 10 → 70 (신규 60)
- ImportRun: 5 → 10 (SUCCESS 4, FAILED 1 추가)
- 병합 후보: 0 → 0
- 공급자별 PlaceSource: TOUR_API 30(기존 10 포함), BUSAN_ATTRACTION 20, BUSAN_RESTAURANT 20
- 기존 Place ID 1~10: 모두 존재하며 이름·카테고리·주소·위도·경도 필드가 백업과 일치

갈맷길 실패 원인은 `Duplicate entry '258' for key 'places.uk_places_external_id'`이다. 통합 출처 키는 `place_sources(provider, external_id)` 조합으로 설계되어 있으나, 기존 `places.external_id`의 공급자 비구분 전역 UNIQUE가 다른 공급자의 같은 숫자 ID를 거부했다. FK 오류나 기존 Place 훼손은 없었다. 이미 공급자별 트랜잭션으로 커밋된 60건은 유지했고 백업을 보존했다.

오류 발생 즉시 추가 쓰기를 중단했으므로 동일 조건 두 번째 재수집은 **미수행**이며 중복 증가 없음과 `reimportStable=true`는 검증되지 않았다.

## 품질·추천 검증 상태

부분 적재된 70건을 읽기 전용으로 분석했다.

- 좌표 누락: 0/70 (0%)
- 주소 누락: 0/70 (0%)
- 이미지 누락: 10/70 (14.29%)
- 카테고리: 관광지 37, 음식점 30, 문화/공연/전시 3
- 공급자별 Source: TOUR_API 30, BUSAN_ATTRACTION 20, BUSAN_RESTAURANT 20
- 구·군: 16개 구·군에 분포(최대 해운대구 11, 다음 부산진구 10)
- 병합 후보: 0
- Source URL과 attribution: 70/70 보유
- content/image license: 신규 부산명소·부산맛집 40건은 모두 보유. TOUR_API 30건 중 기존 10건은 비어 있고, 이미지가 있는 TOUR_API 20건은 image license 보유

이미지 정책은 완화하지 않았으며 라이선스가 불명확한 이미지를 표시 가능으로 간주하지 않았다.

확장 데이터 기반 공개 API 검증:

- 개인화 추천: HTTP 200, 후보 69건, 결과 10건, 동일 seed(42) 결과 재현
- 룰렛 코스: HTTP 200, 요청/실제 5/5, 중복 Place 없음, 좌표 5/5, 동일 seed(42) 방문 장소 재현
- API 검증은 DB 자동 저장 없이 수행했다.
- 실제 브라우저 렌더링과 지도 마커 시각 확인은 이번 실행에서 미검증이다.

## 테스트

- WRITE_VERIFY 안전조건 단위 테스트 추가
- 격리 DB INSPECT 실제 실행 성공
- STS 키 5개 설정 여부와 Runner 주입 성공 확인
- 실제 다섯 공급자 DRY_RUN 성공 및 DB 무변경 확인
- 승인된 WRITE_VERIFY에서 3개 공급자 60건 적재, 푸디 20건 제외, 갈맷길 UNIQUE 오류 및 중단 확인
- 확장 데이터 추천·룰렛 REST API 읽기 전용 검증 성공
- Java 17 `gradlew clean build`: 성공
- 전체 자동 테스트: 기존 307개 + 신규 2개 = 309개 통과
- 실패·오류·무시: 0

## Phase 4-C-3 준비 상태

격리 DB 안전 게이트, 실제 API DRY_RUN, 3개 공급자 부분 적재, 기존 Place 보존, 품질·추천 검증까지 확인했다. 그러나 다음 조건이 남아 있어 Phase 4-C-3 확대 수집 준비 완료로 판정하지 않는다.

1. 기존 `places.external_id` 전역 UNIQUE와 공급자별 외부 ID 모델의 충돌 해소
2. 갈맷길 소량 저장 성공
3. 같은 조건 두 번째 재수집 및 건수 안정성 검증
4. 필요 시 부분 적재 상태를 백업으로 복구할지, 안전한 후속 마이그레이션 후 이어갈지 결정

원본 MySQL에는 접근하거나 쓰지 않았고 Git commit/push도 수행하지 않았다.

## 남은 수동 작업 체크리스트

1. `places.external_id` 레거시 제약조건과 데이터 이전 영향을 검토하고 별도 Flyway 마이그레이션 여부를 결정한다.
2. 격리 DB 백업/복구 상태를 다시 확인한 뒤 갈맷길 최대 20건만 재검증한다.
3. 첫 저장과 같은 조건으로 재수집하여 Place/PlaceSource 중복 증가가 없는지 확인한다.
4. 실제 브라우저에서 추천 카드와 Leaflet 지도 마커를 별도로 확인한다.
