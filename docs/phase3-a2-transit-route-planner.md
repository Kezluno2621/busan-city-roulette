# City Roulette Phase 3-A-2 — 버스 경로 탐색 엔진

검증일: 2026-10-29 (Asia/Seoul)

## 구현 범위

정적 `BusRoute → BusRouteStop → BusStop` 데이터를 이용해 다음 기능을 구현했다.

- 검증된 연속 정차 구간의 직통 경로 탐색
- 동일 BusStop을 이용하는 최대 1회 환승 탐색
- Place 좌표와 주변 정류장 연결
- 방향성 있는 정차 순서 검사
- 반복 정류장과 순환 노선 처리
- 후보 정렬과 개수 제한
- DTO 기반 공개 REST API

운행시간, 도착 예정 시간, 요금, 실제 도보 경로 및 도로상 버스 선형은 계산하지
않는다.

## 그래프 모델

```text
Place
  └─ Haversine 반경 검색 → BusStop
                              ↑
BusRoute → BusRouteStop(sequence, direction) ─┘
```

`TransitGraphService.routes(routeIds)`는 주변 정류장에 실제로 연결된 노선 ID만
받아 정차 관계를 batch fetch한다. 매 요청마다 전체 노선과 전체 정류장을 로딩하지
않으며 최대 200개 노선 그래프로 제한한다.

각 그래프는 다음 정보를 보존한다.

- 내부 및 외부 노선 ID
- 노선 번호
- 공식 정류장 ID와 좌표
- 정차 순서
- 방향/분기 정보
- 연속 순서 여부
- 반복 정류장 여부

같은 노선 번호라도 외부 노선 ID가 다르면 서로 다른 방향 그래프로 취급한다.

## 직통 탐색

1. 출발·목적 Place에서 지정 반경 내 정류장을 bounding-box와 Haversine으로 찾는다.
2. 두 정류장 집합에 연결된 노선 ID를 일괄 조회한다.
3. 공통 노선의 모든 정류장 출현 순번을 비교한다.
4. 출발 정류장 순번보다 목적 정류장 순번이 뒤에 있어야 한다.
5. 선택 구간의 모든 인접 정차 순번이 정확히 1씩 증가해야 한다.
6. 조건을 모두 만족한 구간만 `VERIFIED` 직통 후보로 반환한다.

반대 방향은 반환하지 않는다. 노선 전체가 아닌 저장된 일부 구간도 선택 구간의
순서가 연속되면 후보가 될 수 있지만 `dataCoverageStatus`는 `PARTIAL_DATA`다.

## 1회 환승 탐색

1. 출발 주변 정류장에 연결된 첫 번째 노선을 선택한다.
2. 목적 주변 정류장에 연결된 두 번째 노선을 선택한다.
3. 두 노선이 같은 BusStop 엔티티를 공유하는 지점을 찾는다.
4. 첫 구간은 승차 순번보다 환승 순번이 뒤에 있어야 한다.
5. 두 번째 구간은 환승 순번보다 하차 순번이 뒤에 있어야 한다.
6. 두 구간 모두 정차 순서가 연속되어야 한다.

이름만 같거나 좌표만 가까운 서로 다른 정류장 사이의 도보 환승은 확정 경로로
반환하지 않는다. 환승 횟수는 최대 1회다.

## 반복 정류장과 순환 노선

정류장을 `stopId → 단일 순번`으로 축약하지 않고 그래프 안의 각 출현 위치를
별도로 평가한다. 같은 정류장이 순번 1과 20에 다시 등장하면 두 위치가 각각
승차·하차 후보가 된다. 따라서 순환 노선을 무방향 그래프로 잘못 단순화하지 않는다.

## 후보 정렬

후보는 다음 순서로 정렬한다.

1. 환승 횟수
2. 출발·목적 정류장까지의 Haversine 직선거리 합
3. 경유 정차 수
4. 외부 노선 ID

직선거리 합은 실제 도보 거리나 버스 이동거리가 아니다. 정차 수가 적다는 이유로
실제 이동시간이 짧다고 해석하지 않는다.

## REST API

### Place 기반 정적 경로 탐색

```http
GET /api/transit/routes?originPlaceId=1&destinationPlaceId=2&radius=1000&limit=10
```

| 파라미터 | 기본값 | 제한 |
| --- | ---: | --- |
| `originPlaceId` | 필수 | 양수 |
| `destinationPlaceId` | 필수 | 양수, 출발지와 달라야 함 |
| `radius` | 1000 | 50~3000m |
| `limit` | 10 | 1~20 |

응답 예시:

```json
{
  "originPlaceId": 1,
  "destinationPlaceId": 2,
  "nearbyRadiusMeters": 1000,
  "verificationStatus": "VERIFIED",
  "dataCoverageStatus": "PARTIAL_DATA",
  "walkingDistanceType": "HAVERSINE_STRAIGHT_LINE_ACCESS_AND_EGRESS_NOT_WALKING_ROUTE",
  "candidates": [
    {
      "routeType": "DIRECT",
      "transferCount": 0,
      "boardingStop": {"id": 10, "name": "승차 정류장", "latitude": 35.1, "longitude": 129.1},
      "alightingStop": {"id": 20, "name": "하차 정류장", "latitude": 35.2, "longitude": 129.2},
      "transferStop": null,
      "busRouteNumbers": ["100"],
      "busRouteExternalIds": ["verified-external-route-id"],
      "stopCount": 5,
      "walkingDistanceMeters": 312.4,
      "verificationStatus": "VERIFIED",
      "dataCoverageStatus": "PARTIAL_DATA"
    }
  ]
}
```

후보가 없으면 HTTP 200과 `verificationStatus=NO_CANDIDATE`를 반환한다. 이는 현재
적재 데이터 안에 후보가 없다는 의미이며 부산 전체 버스망에서 이동할 수 없다는
뜻이 아니다.

오류 상태:

- Place 없음: 404
- 좌표 없는 Place: 422
- 동일 출발·목적 또는 잘못된 파라미터: 400

기존 좌표 기반 `/api/transit/routes?sx=...&sy=...&ex=...&ey=...`도 유지한다.
Spring mapping의 필수 query parameter 조건으로 두 API를 구분한다.

## 지도 연동 준비

`TransitStopSummary`는 승차·하차·환승 정류장의 ID, 공식 ID, ARS, 명칭,
위도·경도를 제공한다. 지도에서는 이 세 종류의 정류장 마커를 구분해 표시할 수
있다. 실제 도로 형상이 없으므로 정류장 사이의 직선을 실제 버스 운행 경로로
표현해서는 안 된다.

## H2 Fixture 테스트

다음 시나리오를 독립 fixture로 검증했다.

- 정상 직통 경로
- 반대 방향 직통 차단
- 동일 공식 정류장의 1회 환승
- 이름만 같은 서로 다른 정류장 환승 차단
- 동일 정류장 반복 출현
- 순환 노선의 정방향 구간
- 정차 순서 누락 차단
- 10~11처럼 중간부터 저장된 연속 부분 구간
- 좌표 누락 Place
- 동일 출발지·목적지
- 존재하지 않는 Place
- 후보 수 제한
- 데이터 부족 시 `NO_CANDIDATE + PARTIAL_DATA`
- 비회원 REST API 접근 및 파라미터 검증

전체 결과:

- Java 17 / Gradle 8.14.3 `clean build` 성공
- 50개 테스트 스위트, 266개 테스트 성공
- 실패 0, 오류 0, 건너뜀 0
- 기존 인증, 파일, 장소, 버스, 좌표 기반 transit API 회귀 테스트 포함

## 격리 MySQL 실제 데이터 검증

원본 DB에는 연결하거나 쓰지 않았다. Phase 3-A-1 격리 사본과 최신 애플리케이션을
로컬 전용 포트로 실행해 읽기 전용 REST 검증을 수행했다.

| 항목 | 결과 |
| --- | ---: |
| Place | 10 |
| 서로 다른 방향 Place 쌍 | 90 |
| HTTP 200 응답 | 90 |
| HTTP 오류 | 0 |
| 후보가 있는 Place 쌍 | 0 |
| 직통 후보 | 0 |
| 환승 후보 | 0 |

현재 실제 데이터는 100번 노선 한 개의 130개 정차 중 상위 20개뿐이다. 500m
이내 Place 매칭은 0건이고 1km 매칭도 Place의 20%뿐이므로 후보 0건은 예상 가능한
부분 데이터 결과다. 실제 부산 경로 부재로 해석하지 않는다.

## 구현 클래스

- `TransitRoutePlannerService`: 직통·1회 환승 탐색과 정렬
- `TransitRouteController`: Place ID 기반 REST API
- `TransitRoutePlanResponse`, `TransitRouteCandidate`: API 결과
- `TransitStopSummary`: 지도 마커용 정류장 정보
- `TransitRouteType`, `TransitVerificationStatus`, `TransitDataCoverageStatus`: 상태 모델
- `TransitGraphService.routes`: 필요한 노선만 batch graph 조회

## 미완료 및 Phase 3-B 준비

알고리즘과 API 계약, 지도용 DTO는 Phase 3-B 연동이 가능한 상태다. 실제 부산 전역
경로를 검증하려면 다음 데이터가 더 필요하다.

1. 검증된 여러 노선의 전체 정차 순서
2. 서로 다른 노선이 공유하는 공식 환승 정류장
3. 관광 Place와 300~500m 이내에 있는 노선 연결 정류장
4. 노선 방향 또는 운행 계통을 구분하는 충분한 외부 노선 ID
5. 부분/전체 적재 여부를 노선별로 명시하는 coverage metadata

Phase 3-B에서는 Place 상세 지도에서 후보 API를 호출하고 승차·하차·환승 마커를
표시할 수 있다. 실제 시간·비용·도로 선형은 별도 검증 데이터가 생기기 전까지
표시하지 않는다.

