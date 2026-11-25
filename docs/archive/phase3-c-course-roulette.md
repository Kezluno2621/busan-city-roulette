# Phase 3-C 룰렛 기반 여행 코스 생성 엔진

## 구현 범위

Phase 3-C는 Phase 3-B가 계산한 개인화 추천 후보와 점수를 재사용하여 1~5개 장소를 선택하고 방문 순서를 구성한다. 생성 결과는 응답으로만 반환하며 DB에 저장하지 않는다. 기존 ODsay 랜덤 추천, 개별 장소 추천, 정적 버스 경로 탐색은 변경하지 않았다.

처리 흐름은 다음과 같다.

`CourseGenerationRequest` → Phase 3-B 후보/점수 최대 20건 → 중복 제거 → 가중치 기반 비복원 선택 → nearest-neighbor 방문 순서 → 구간별 정적 교통 검증 → 코스 DTO

## 룰렛 선택 알고리즘

- `randomness=0`: `추천 점수 + 미선택 카테고리 보너스`가 가장 높은 장소를 결정론적으로 선택한다.
- `randomness>0`: 랜덤성이 높을수록 상위 후보 풀을 넓힌다. 추천 점수가 높은 장소의 확률은 계속 높게 유지하면서 점수 지수의 차이를 완화한다.
- 아직 선택하지 않은 카테고리는 확률 가중치 1.35배를 적용한다.
- 장소는 Place ID 기준으로 먼저 중복 제거하고 비복원 추출한다.
- 같은 `randomSeed`, 요청, 후보 순서에서는 동일한 장소 선택 결과가 재현된다.
- 랜덤성 100에서도 Phase 3-B의 좌표·활성 상태·필터·데이터 품질 검증을 우회하지 않는다.

브라우저 룰렛은 장소를 선택하지 않는다. 서버 응답의 장소만 애니메이션에 사용하며 최종 카드와 지도 역시 동일 응답을 표시한다.

## 코스 순서

선택된 장소를 출발지에서 시작하는 nearest-neighbor 휴리스틱으로 정렬한다. 매 단계에서 현재 위치와 직선거리가 가장 가까운 미방문 장소를 선택한다. 동률이면 추천 점수와 Place ID로 순서를 안정화한다.

`distanceFromPreviousMeters`는 WGS84 좌표의 Haversine 직선거리다. 이 순서는 최적 경로, 도로 거리, 도보 거리 또는 실제 버스 운행 경로가 아니다.

## 교통 검증

- Place ID 출발 요청에서 Phase 3-B가 첫 구간을 이미 평가했다면 그 결과를 재사용한다.
- 그 외 구간은 인접 장소 쌍별로 `TransitRoutePlannerService`를 최대 한 번 호출한다.
- 현재 적재된 연속 정차 구간에서 후보가 확인된 경우에만 `VERIFIED`다.
- 부분 데이터에서 후보가 없으면 `PARTIAL_DATA`, 평가할 수 없거나 오류가 발생하면 `UNVERIFIED`다.
- 경로 후보가 없더라도 장소를 코스에서 제거하거나 실제 이동 불가능으로 판단하지 않는다.

현재 시간과 비용 근거 데이터가 없으므로 `estimatedTimeStatus`와 `estimatedCostStatus`는 `UNKNOWN`으로 반환한다. 실제 시간, 입장료, 교통요금을 생성하거나 추정하지 않는다.

## REST API

```http
POST /api/courses/generate
Content-Type: application/json
```

```json
{
  "originPlaceId": 1,
  "categories": ["ATTRACTION", "CAFE"],
  "district": "해운대구",
  "availableMinutes": 180,
  "budget": 30000,
  "randomness": 60,
  "placeCount": 3,
  "randomSeed": 42
}
```

`originPlaceId` 대신 `originLatitude`와 `originLongitude`를 함께 사용할 수 있다. ID와 좌표 동시 입력, 좌표 한쪽 누락, 범위 밖 좌표는 거부한다. `placeCount`는 1~5, `randomness`는 0~100, 여행 시간은 양수, 예산은 0 이상이다. 없는 Place ID는 404, 좌표 없는 출발 Place는 422다. Spring Security의 CSRF 보호는 유지된다.

응답 주요 구조:

```json
{
  "courseTitle": "부산 즉흥 여행 코스 · 3곳",
  "origin": {"placeId": 1, "name": "출발지", "latitude": 35.1, "longitude": 129.1},
  "generatedAt": "2026-10-29T00:00:00Z",
  "requestedPlaceCount": 3,
  "actualPlaceCount": 3,
  "places": [
    {
      "placeId": 2,
      "sequence": 1,
      "recommendationScore": 87.5,
      "distanceFromPreviousMeters": 620.4,
      "transitVerificationStatus": "PARTIAL_DATA",
      "estimatedTimeStatus": "UNKNOWN",
      "estimatedCostStatus": "UNKNOWN"
    }
  ],
  "insufficientCandidates": false,
  "dataLimitations": []
}
```

후보가 부족하면 가능한 장소만 반환하고 `actualPlaceCount`, `insufficientCandidates`, `message`로 부족 사유를 명시한다.

## UI와 지도

`GET /recommend`에 Phase 3-C 영역을 추가했다.

- 카테고리, 구군, 시간, 예산, 랜덤성, 장소 수, 선택적 seed 입력
- 룰렛 시작/다시 뽑기
- 요청 및 애니메이션 중 중복 제출 차단
- 운영체제의 애니메이션 감소 설정에서 즉시 결과 표시
- 방문 순서, 추천 점수, 추천 이유, 구간 교통 상태 표시
- Leaflet 출발지 및 번호 마커
- 장소를 잇는 점선과 “실제 이동 경로 아님” 안내

지도 점선은 장소 순서를 이해하기 위한 직선 연결일 뿐 도로나 버스 노선 형상이 아니다.

## 성능과 안전

- Phase 3-B DB 필터와 후보 제한을 그대로 사용한다.
- 코스 계층은 추천 점수를 다시 계산하지 않고 결과 DTO를 재사용한다.
- 추천 후보는 최대 20건, 결과는 최대 5건이다.
- 교통 조회는 실제 코스 인접 구간으로 제한한다.
- 읽기 전용 트랜잭션이며 코스를 DB에 저장하지 않는다.
- 원본 MySQL 마이그레이션이나 쓰기를 수행하지 않는다.

## 테스트

H2 Fixture에서 다음을 검증한다.

- randomness 0 결정론 및 카테고리 다양성
- randomness 100의 seed별 선택 변화
- 동일 seed 재현
- 중복 Place 제거
- 후보 부족 응답
- nearest-neighbor 순서와 구간 거리
- 시간/비용 UNKNOWN
- 부분 교통 데이터에서도 코스 유지
- 없는 Place, 좌표 누락, 입력 충돌
- REST 응답, Bean Validation, CSRF 403
- 기존 Phase 3-B, ODsay, 인증, 파일, 버스 경로 기능 회귀

Java 17 `gradlew clean build`가 성공했으며 전체 283개 테스트가 통과했다(실패 0, 오류 0). 이 중 Phase 3-C 신규 테스트는 서비스 6개와 Controller/CSRF 2개다.

## 실제 데이터 검증 및 한계

이번 단계에서는 원본 MySQL(Flyway V4)과 격리 MySQL에 연결하거나 쓰지 않았다. 실제 부산 100번 부분 구간을 사용한 코스 생성은 미검증이며 H2 Fixture 성공과 구분한다.

Phase 4에서 코스 저장을 추가하려면 회원 소유권, 코스/코스 장소 순서 테이블, 생성 당시 점수와 데이터 제한사항 스냅샷, 낙관적 잠금 또는 수정 시각 정책이 필요하다. 운영시간·체류시간·가격 데이터가 추가되기 전에는 코스 전체 시간과 예산 충족을 확정해서는 안 된다.
