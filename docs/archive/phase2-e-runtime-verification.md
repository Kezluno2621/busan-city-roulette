# City Roulette Phase 2-E — STS 기반 실제 MySQL 및 버스 데이터 검증

작성일: 2026-10-29 (Asia/Seoul)

## 현재 실행 결과

Phase 2-D/2-E 읽기 전용 검증으로 STS launch 설정의 MySQL에 연결했다.
비밀번호와 API 키는 출력하지 않았다.

| 항목 | 결과 |
| --- | --- |
| MySQL | 연결 성공, 8.0.46 |
| Flyway | v4 |
| `places` | 10건, 좌표 10건, 관광지 10건 |
| `place_sources` | 없음 |
| `bus_stops` | 없음 |
| `bus_routes` | 없음 |
| `bus_route_stops` | 없음 |
| `import_runs` | V6 대상이므로 현재 스키마에 없음 |
| `/busStopList` | HTTP 200, resultCode `00`, totalCount 8,800, 표본 1건 파싱 성공 |

현재 DB의 이름은 명시적인 `dev/test/local` 표식을 갖고 있지 않고, 백업도
확인되지 않았다. 따라서 V5~V9, 소량 적재, 재수집은 실행하지 않았다.

## STS 수동 검증 도구

`RuntimeVerificationRunner`는 다음 두 조건이 모두 맞을 때만 등록된다.

1. Spring profile이 `dev`
2. `runtime.verification.enabled=true`

일반 서버 시작에서는 실행되지 않고, HTTP Controller로 노출되지 않는
`ApplicationRunner`다. 지원 모드는 다음과 같다.

| 모드 | DB 쓰기 | 기능 |
| --- | --- | --- |
| `INSPECT` | 없음 | DB/Flyway/테이블/좌표/매칭률 통계 |
| `DRY_RUN` | 없음 | INSPECT + 정류장 10건 + 정확 일치 노선 1건/20건 Dry-run |
| `WRITE_VERIFY` | 있음 | Dry-run, 1차 저장, 동일 조건 2차 저장, 중복/순서/장소 보존 검증 |

`WRITE_VERIFY`는 다음을 모두 검증한다.

- 연결된 DB 이름과 `runtime.verification.expected-database` 정확 일치
- `runtime.verification.backup-confirmed=true`
- Dry-run 정류장 10건, 실패 0건
- `/busInfo` 검색 결과 중 입력 노선번호와 정확히 일치하는 노선 1건
- 재수집 후 `bus_stops`, `bus_routes`, `bus_route_stops` 건수 미증가
- 기존 Place ID 목록과 건수 유지
- API 상위 20개 정차 순서와 DB 순서 일치

## dev 프로필 안전 기본값

`application-dev.properties`는 pending Flyway가 STS 시작만으로 자동 적용되지 않도록
다음을 기본값으로 둔다.

```properties
spring.flyway.enabled=${RUNTIME_ALLOW_MIGRATIONS:false}
runtime.verification.enabled=${RUNTIME_VERIFICATION_ENABLED:false}
runtime.verification.mode=${RUNTIME_VERIFICATION_MODE:INSPECT}
runtime.verification.expected-database=${RUNTIME_EXPECTED_DATABASE:}
runtime.verification.backup-confirmed=${RUNTIME_BACKUP_CONFIRMED:false}
runtime.verification.route-number=${RUNTIME_ROUTE_NUMBER:}
```

## STS 실행 순서

### 1. 마이그레이션 전 읽기 전용 사전 점검

Spring Boot는 현재 v4 스키마로 JPA를 시작할 수 없으므로 STS의 Gradle Task에서
`verifyDevelopmentData`를 실행한다. 이 태스크는 Flyway를 실행하지 않고 read-only
JDBC만 사용한다.

### 2. 백업과 DB 정체 확인

다음을 사용자가 먼저 확인한다.

- 이 DB가 운영이 아닌 개발 DB인지
- 백업/스냅샷에서 복구할 수 있는지
- `RUNTIME_EXPECTED_DATABASE`가 실제 DB 이름과 정확히 같은지

### 3. V5~V9 적용

백업 후 한 번의 STS 실행에서만 다음을 사용한다.

```text
Profile: dev
RUNTIME_ALLOW_MIGRATIONS=true
RUNTIME_VERIFICATION_ENABLED=true
RUNTIME_VERIFICATION_MODE=INSPECT
RUNTIME_EXPECTED_DATABASE=<확인한 개발 DB 이름>
RUNTIME_BACKUP_CONFIRMED=true
```

시작 후 Flyway v9, Place ID/건수, 신규 테이블을 확인하고 즉시 종료한다.

### 4. Dry-run

`RUNTIME_ALLOW_MIGRATIONS=false`, `RUNTIME_VERIFICATION_MODE=DRY_RUN`,
`RUNTIME_ROUTE_NUMBER=<정확한 검증 노선번호>`로 실행한다.

### 5. 소량 저장과 재수집

Dry-run 통계를 확인한 뒤 `RUNTIME_VERIFICATION_MODE=WRITE_VERIFY`로 변경한다.
`RUNTIME_BACKUP_CONFIRMED=true` 및 expected DB 일치가 없으면 Runner가 저장 전에
중단한다.

## 실제 적재 및 품질 통계

현재는 미수행이다. 버스 테이블이 없으므로 아래 항목은 아직 미검증이다.

- 정류장 10건 저장/재수집
- 노선 1건과 정차 순서 20건
- 중복 미증가
- 300m/500m/1km 주변 정류장 매칭률
- API와 DB 정차 순서 일치

소량 데이터로 산출한 매칭률은 전체 부산의 대중교통 접근성을 대표하지 않는다.

## 코드 및 회귀 테스트 결과

- Java 17, Gradle 8.14.3 기준 `clean build` 성공
- 전체 47개 테스트 스위트, 244개 테스트 성공
- 실패 0건, 오류 0건, 건너뜀 0건
- `/busInfo`가 검색형 API라는 전제에 맞춰 요청한 노선번호와 정확히 일치하는
  결과만 수집하는 테스트 포함
- 주변 정류장 bounding-box/Haversine 조회와 일괄 노선 조회 테스트 통과
- `git diff --check` 오류 없음(기존 파일의 줄바꿈 변환 경고만 존재)

실제 MySQL 쓰기와 API 기반 소량 적재는 위 코드 테스트에 포함되지 않는다.
개발 DB 및 백업 확인 전 쓰기 금지 조건에 따라 의도적으로 실행하지 않았다.

## Phase 3-A 진입 상태

경로 탐색 엔진의 코드는 H2 fixture로 개발할 수 있다. 실제 부산 데이터로
검증하려면 다음이 추가로 필요하다.

1. 개발 DB와 백업을 사용자가 확인
2. V5~V9 적용 후 Place 10건 보존 확인
3. `DRY_RUN` 성공
4. `WRITE_VERIFY` 성공
5. 최소 1개 노선의 API/DB 정차 순서 일치

이 조건이 충족되기 전에는 실제 부산 버스 경로 탐색이 검증됐다고 판단하지 않는다.
