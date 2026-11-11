# City Roulette Phase 3-A-1 — 버스 정류장 좌표 보강 및 연결 검증

검증일: 2026-10-29 (Asia/Seoul)

## 결론

Phase 2-F 격리 MySQL 사본의 100번 노선 상위 20개 정류장을 실제
부산버스정보시스템 `busStopList` 응답과 비교했다. 20건 모두 노선 응답의
`nodeid`와 일반 정류장 응답의 `bstopid`가 정확히 일치했다. 이름이나 ARS 번호에
의존한 자동 병합 없이 공식 ID로 좌표를 보강했다.

보강 후 정류장 수와 정차 관계 수는 증가하지 않았고 정차 순서 1~20도 유지됐다.
원본 MySQL은 변경하지 않았다.

## 백업 및 대상 DB 안전성

쓰기 전에 Phase 2-F 격리 사본을 다시 백업했다.

- 파일: `.runtime-backups/phase3a1-pre-enrichment-20261029.sql.bak`
- 크기: 25,645 bytes
- SHA-256: `0B66C25D14CD270B5B8F451851F93A6723C457EA864F2BD669A41CC7A3AB5E05`
- 별도 임시 MySQL 복원 결과: Place 10, BusStop 30, BusRoute 1,
  BusRouteStop 20, Flyway v9
- 복원 검증: 성공

백업과 검증 로그는 `.runtime-backups/` 아래에 있으며 Git에서 제외된다.

## 실제 API 식별자 비교

### 일반 정류장: `busStopList`

| 의미 | 원본 필드 | 표준 필드 |
| --- | --- | --- |
| 공식 정류장 ID | `bstopid` | `externalStopId` |
| ARS 번호 | `arsno` | `arsNumber` |
| 정류장명 | `bstopnm` | `name` |
| 경도 | `gpsx` | `longitude` |
| 위도 | `gpsy` | `latitude` |
| 정류장 유형 | `stoptype` | `stopType` |

### 노선 정류장: `busInfoByRouteId`

100번 노선 실제 응답은 총 130개 정차 항목이었다. 확인한 필드는 다음과 같다.

| 의미 | 원본 필드 | 처리 |
| --- | --- | --- |
| 노선 정류장 ID | `nodeid` | 공식 ID 비교 키 |
| 정차 순서 | `bstopidx` | `stopSequence` |
| 정류장명 | `bstopnm` | 표시 및 ARS 보조 검증 |
| ARS 번호 | `arsno` | 고유한 경우에만 보조 키 |
| 노선 번호 | `lineno` | 노선 확인 |
| 방향/분기 정보 | `rpoint` | `direction` fallback |
| 일부 위도 | `lat` | 유효 범위인 경우만 사용 |
| 일부 경도 | `lin` | 유효 범위인 경우만 사용 |
| 일부 방향 | `direction` | 존재하면 `rpoint`보다 우선 |

`nodeid`는 130건 모두 존재했고 `arsno`는 128건 존재했다. `lat`, `lin`,
`direction`은 각각 14건만 값이 있어 전체 좌표 공급원으로 사용할 수 없었다.
따라서 좌표가 없는 정류장은 `busStopList`의 같은 공식 ID를 기준으로 보강한다.

첫 표본에서는 `nodeid == bstopid`, ARS 번호, 정류장명까지 모두 일치했고 일반
정류장 응답에 좌표가 존재했다. 실제 보강 대상 20건 전체도 공식 ID가 일치했다.

## 좌표 보강 알고리즘

`BusStopEnrichmentService`는 다음 순서를 적용한다.

1. 선택한 노선에서 좌표가 하나라도 없는 정류장만 조회한다.
2. ARS 검색 또는 이름 검색으로 일반 정류장 API 후보를 가져온다.
3. 후보 중 `nodeid/bstopid`가 같은 항목을 최우선으로 선택한다.
4. 공식 ID가 다르면 API 후보와 현재 DB 양쪽에서 ARS가 유일하고 정규화된 동일
   이름까지 확인될 때만 좌표 증거로 사용할 수 있다. 두 ID 자체는 합치거나
   바꾸지 않는다.
5. 이름만 같은 후보 또는 중복 ARS는 검토 후보로 남기고 자동 병합하지 않는다.
6. 부산 범위 검사를 통과한 위도·경도만 기존 BusStop에 추가한다.
7. 좌표가 이미 있는 정류장은 재실행 대상에서 제외한다.

`BusRouteStopMapper`도 실제로 확인된 `lat/lin`을 유효한 경우 직접 사용하도록
보완했다. `direction`이 없으면 실제 응답의 `rpoint`를 방향 정보로 사용한다.

## 실제 보강 결과

### Dry-run

| 항목 | 건수 |
| --- | ---: |
| 보강 대상 | 20 |
| 공식 ID 일치 | 20 |
| ARS 보조 일치 | 0 |
| 좌표 보강 가능 | 20 |
| 검토 후보 | 0 |
| 미연결 | 0 |
| 실패 | 0 |

### 저장 및 재실행

| 항목 | 보강 전 | 1차 보강 | 2차 실행 |
| --- | ---: | ---: | ---: |
| BusStop | 30 | 30 | 30 |
| 좌표 보유 BusStop | 10 | 30 | 30 |
| BusRoute | 1 | 1 | 1 |
| BusRouteStop | 20 | 20 | 20 |
| 보강 대상 | 20 | 20 | 0 |

두 번째 실행은 대상이 0건이었으며 중복 INSERT나 정차 관계 변경이 없었다.
기존 Place 10건과 ImportRun 4건도 좌표 보강 과정에서 변경되지 않았다.

확인된 필드 매핑으로 노선을 한 번 재수집한 결과:

- 노선: 1건 update, 신규 노선 0
- 정차 관계: 20건 유지
- 좌표 보유: 20/20
- 방향 정보: 20/20
- 정차 순서: 1~20, 누락 0, distinct 20
- ImportRun: 재수집 이력 1건 추가, 총 5건

## 교통 그래프 준비 구조

`TransitGraphService`는 다음 관계를 한 번에 조회한다.

```text
BusRoute → BusRouteStop(stopSequence, direction) → BusStop(coordinates)
```

`TransitRouteGraph`와 `TransitGraphStop` DTO는 다음 품질 정보를 함께 제공한다.

- 정차 순서 연속 여부
- 모든 정류장 좌표 보유 여부
- 동일 정류장 재방문 여부
- 정차별 방향 정보

100번 상위 20건의 결과는 다음과 같다.

- 순서 연속: true
- 좌표 완전성: 20/20
- 반복 정류장: false
- FK 누락: 0

반복 정류장을 허용하는 테스트도 추가해 순환 노선 가능성을 차단하지 않는다.

## 주변 정류장 매칭률

Place 10건과 표본 정류장 30건을 기준으로 기존 bounding-box/Haversine 조회를
재실행했다.

| 반경 | 보강 전 | 보강 후 |
| --- | ---: | ---: |
| 300m | 0% | 0% |
| 500m | 0% | 0% |
| 1km | 10% | 20% |

부분 정류장과 100번 노선 일부만 사용한 결과이므로 부산 전체 대중교통 접근성을
나타내지 않는다.

## 수동 실행 설정

보강 Runner는 `dev` profile과 명시적 활성화가 모두 필요하며 HTTP로 노출되지
않는다.

```text
BUS_STOP_ENRICHMENT_ENABLED=true
BUS_STOP_ENRICHMENT_DRY_RUN=true
BUS_STOP_ENRICHMENT_ROUTE_NUMBER=100
BUS_STOP_ENRICHMENT_LIMIT=20
RUNTIME_EXPECTED_DATABASE=<확인한 격리/개발 DB>
RUNTIME_BACKUP_CONFIRMED=true
```

쓰기 시에만 `BUS_STOP_ENRICHMENT_DRY_RUN=false`로 변경한다. 예상 DB 불일치 또는
백업 미확인 상태에서는 Runner가 쓰기를 차단한다.

## 테스트 결과

- 공식 ID 일치 보강
- 고유 ARS + 동일 명칭 보조 보강
- 서로 다른 식별자 보존
- 중복 ARS 자동 병합 차단
- 이름 단독 자동 병합 차단
- 재실행 멱등성
- 정차 순서, 방향 및 반복 정류장 참조
- 실제 확인된 `rpoint`, `lat`, `lin` 파싱
- Java 17 / Gradle 8.14.3 `clean build` 성공
- 48개 테스트 스위트, 251개 테스트 성공
- 실패 0, 오류 0, 건너뜀 0

## Phase 3-A-2 진행 가능 여부

경로 그래프 DTO와 단일 노선의 좌표·순서·방향 데이터가 준비되어 알고리즘 및
fixture 기반 Phase 3-A-2 개발은 가능하다.

실제 부산 경로 탐색을 검증하기에는 아직 부족하다.

1. 100번 노선은 실제 130개 정차 중 상위 20개만 저장되어 있다.
2. 노선도 1개뿐이므로 환승 관계를 검증할 수 없다.
3. 500m 이내 Place 연결이 현재 표본에서 0건이다.
4. 원본 MySQL은 계속 Flyway v4이며 변경하지 않았다.

따라서 다음 단계는 격리/명확한 개발 DB에서 검증된 소수 노선을 추가하고,
각 노선의 전체 정차 순서를 소량 단위로 검증한 뒤 직통·환승 탐색을 시작하는
것이 안전하다.

