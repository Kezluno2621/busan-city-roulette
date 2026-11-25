# City Roulette Phase 2-D — 실제 DB 검증 및 버스 조회 최적화

검증일: 2026-10-29 (Asia/Seoul)

## 1. STS와 Codex 실행 환경

Codex 터미널의 `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`,
`BUSAN_BUS_API_KEY`는 모두 미설정 상태였다. STS의
`city-roulette-busan-city-roulette - CityRouletteApplication` 실행 구성에는 여섯 변수가
모두 있으며, 값·비밀번호·API 키는 출력하지 않았다.

STS 설정의 DB 호스트는 로컬이지만 DB 이름에 `dev`, `test`, `local`과 같은
명시적인 개발용 표식은 없었다. 따라서 쓰기 대상인 개발 DB로는 확정하지
않았고, 실제 적재나 Flyway 마이그레이션을 실행하지 않았다.

## 2. 실제 MySQL 연결과 스키마

STS launch 설정을 현재 프로세스에만 전달한 후 Flyway를 비활성화하고
Hibernate `validate`로 읽기 전용 연결을 시도했다.

- MySQL 연결: 성공
- MySQL 버전: 8.0.46
- Flyway 최종 적용: v4
- Spring/JPA 시작: 실패 (`bus_route_stops` 테이블 없음)
- DB 쓰기: 0건

JPA 시작과 독립적인 `verifyDevelopmentData` JDBC 태스크를 추가했다. 이 태스크는
JDBC 세션을 read-only로 설정하고 Flyway를 실행하지 않으며 트랜잭션을 롤백한다.

```powershell
.\gradlew.bat verifyDevelopmentData
```

## 3. 실제 DB 통계

| 항목 | 결과 | 상태 |
| --- | ---: | --- |
| `places` | 10 | 확인 |
| `place_sources` | 테이블 없음 | 미적용 |
| `bus_stops` | 테이블 없음 | 미적용 |
| `bus_routes` | 테이블 없음 | 미적용 |
| `bus_route_stops` | 테이블 없음 | 미적용 |
| 좌표가 있는 장소 | 10 | 100% |
| 카테고리 | 관광지 10 | 확인 |
| 구·군 | 산출 불가 | `district` 컬럼 없음 |
| 정류장/노선/정차 관계 | 산출 불가 | 테이블 없음 |
| 300m/500m/1km 매칭률 | 산출 불가 | `bus_stops` 없음 |

현재 DB는 V5~V9가 적용되지 않은 상태다. 따라서 Phase 2-C의 상세 출처,
구·군, 버스 조회 기능은 H2 테스트에서는 정상이지만 현재 MySQL 스키마로는
애플리케이션을 시작할 수 없다.

## 4. 부산버스 API 표본 연결

STS에 저장된 `BUSAN_BUS_API_KEY`를 출력하지 않고 `/busStopList`에
`pageNo=1`, `numOfRows=1`만 요청했다.

| 항목 | 결과 |
| --- | --- |
| HTTP | 200 |
| 공급자 코드 | `00` |
| XML 파싱 | 성공 |
| 표본 | 1건 |
| `totalCount` | 8,800 |

인증키가 이미 URL 인코딩된 형태여서 중복 인코딩하면 HTTP 403이었고, 값을
노출하지 않고 형태만 판별해 그대로 사용했을 때 성공했다. `/busInfo`는 전체
노선 목록으로 간주하지 않으며 이번 검증에서 호출하지 않았다.

## 5. 소량 적재

실행하지 않았다.

사유:

1. DB 이름으로 개발 DB를 명확히 확정할 수 없음
2. 백업/복구 수단이 확인되지 않음
3. Flyway v4로 `bus_*` 테이블이 없어 버스 수집기를 시작할 수 없음

V5~V9 적용은 기존 10건을 유지하는 증분 마이그레이션이지만, 실제 DB에 대한
쓰기이므로 현재 작업에서는 실행하지 않았다.

## 6. 주변 정류장 조회 최적화

기존에는 좌표가 있는 모든 정류장을 JVM으로 읽어 Haversine 계산을 했다.
변경 후 흐름은 다음과 같다.

```text
Place 좌표 + 반경
  → 보수적인 위도/경도 bounding box
  → DB BETWEEN 후보 조회
  → 후보에만 Haversine 정밀 거리
  → 반경 내 필터
  → 거리순 정렬
  → 최대 100건
```

V9에 `(latitude, longitude)` 복합 인덱스를 추가했다. 현재 좌표가 nullable인
두 DECIMAL 컬럼이므로, POINT 컬럼 추가와 전체 데이터 변환이 필요한 MySQL 공간
인덱스는 이번에 적용하지 않았다.

상세 화면의 최대 12개 정류장 노선은 기존 N번 개별 조회 대신 정류장 ID 목록을
사용한 한 번의 `JOIN FETCH` 조회로 변경했다. REST DTO와 URL은 변경하지 않았다.

## 7. 테스트

- bounding-box 후보 + Haversine 순서/반경 테스트
- 정류장 여러 개의 노선 일괄 조회 테스트
- 테이블 건수, 노선 미연결, 300m/500m/1km 매칭률 통계 테스트
- H2에 V9 적용 및 Hibernate 스키마 검증
- `gradlew.bat --no-daemon clean build`: **BUILD SUCCESSFUL**
- 전체 243 tests, failures 0, errors 0

## 8. Phase 3-A 진입 판정

| 조건 | 상태 |
| --- | --- |
| 실제 MySQL 연결 | 충족 |
| Place 좌표 | 10/10 확인 |
| BusStop 좌표 | 미충족 — 테이블 없음 |
| BusRouteStop 관계 | 미충족 — 테이블 없음 |
| 검증된 노선 정차 순서 | 미충족 |
| 실 DB 주변 정류장 검색 | 미충족 |
| 경로 그래프 생성 | 미충족 |

Phase 3-A의 그래프/탐색 코드는 H2 fixture로 개발할 수 있지만, 실제 부산 경로
탐색을 검증할 수 있는 진입 조건은 충족하지 못했다.

다음 순서가 필요하다.

1. 현재 DB가 개발용임을 명시적으로 확인
2. 스냅샷/백업 확보
3. V5~V9 적용 후 기존 10건 보존 확인
4. 정류장 10건과 명시적 검색 노선 1건 Dry-run
5. 소량 저장·재수집·중복 미증가 검증
6. 300m/500m/1km 매칭률을 다시 산출
7. 최소 1개 노선의 정차 순서를 원본 API와 비교
