# Phase 4-C 부산 관광 공공데이터 확장 및 품질 검증

검증일: 2026-10-29 (Asia/Seoul)

## 결론

기존 다중 공급자 수집기는 TourAPI, 부산명소, 부산맛집, 부산푸디투어, 갈맷길의 페이지네이션·정규화·멱등 저장·병합 후보 기록을 이미 지원한다. 이번 작업에서는 공급자별 이미지 이용조건 보존을 보완하고, 실제 DB에서 누락률과 분포를 DB 집계로 측정하는 읽기 전용 품질 Runner를 추가했다.

현재 Codex 프로세스에는 다섯 API 키와 DB 환경변수가 없고 Windows 앱 목록에도 STS가 없어, 신규 실제 API 호출과 확대 적재는 실행하지 못했다. 키 없이 실행한 공급자별 20건 제한 Dry-run은 HTTP 호출 전에 안전하게 실패했으며 DB를 변경하지 않았다. 따라서 이번 실행의 실제 신규 수집·저장 건수는 0이며, 성공으로 간주하지 않는다.

## 수집·정제 처리 흐름

```text
Provider Client (최대 건수·페이지 제한)
  → 공급자 결과 코드 및 JSON 검증
  → 공급자별 Mapper
  → NormalizedPlace
  → 필수 필드·부산 좌표 검증
  → (provider, externalId) 원천 조회
  → 신규 Place/PlaceSource 또는 기존 원천 갱신
  → 다른 공급자 유사 장소는 PlaceMergeCandidate만 기록
  → ImportRun 통계
  → PlaceDataQualityRunner 품질 집계
```

- 푸디투어 표본은 좌표·주소가 없는 미식 테마이므로 `THEME_CONTENT`로 분류하고 Place 저장에서 제외한다.
- 갈맷길 `course`, `gugan`은 주소로 만들지 않고 `source_category`에 보존한다.
- 공급자가 다른 유사 장소는 자동 병합하지 않는다.
- API 키, 원본 요청 URL, 응답 본문은 로그에 출력하지 않는다.

## 이번 실행 환경과 API 결과

| Provider | 키 상태 | 요청 제한 | 실제 HTTP 호출 | Fetched | Inserted | Updated | Skipped | Failed |
|---|---|---:|---|---:|---:|---:|---:|---:|
| TOUR_API | Codex 미설정 | 20 | 미수행 | 0 | 0 | 0 | 0 | 실행 실패 |
| BUSAN_ATTRACTION | Codex 미설정 | 20 | 미수행 | 0 | 0 | 0 | 0 | 실행 실패 |
| BUSAN_RESTAURANT | Codex 미설정 | 20 | 미수행 | 0 | 0 | 0 | 0 | 실행 실패 |
| BUSAN_FOODIE | Codex 미설정 | 20 | 미수행 | 0 | 0 | 0 | 0 | 실행 실패 |
| BUSAN_GALMAETGIL | Codex 미설정 | 20 | 미수행 | 0 | 0 | 0 | 0 | 실행 실패 |

실패는 인증 실패가 아니라 현재 프로세스에 키가 전달되지 않은 `NOT_CONFIGURED` 상태다. 기존 `phase2-api-connection-results.md`에는 2026-10-28 STS 환경에서 다섯 API의 HTTP 200, 공급자 결과 코드, 1건 파싱 성공과 공급자별 최대 10건 Dry-run 성공이 별도 기록돼 있다. 그 과거 결과를 이번 신규 호출로 재해석하지 않는다.

## 격리 MySQL 품질 기준선

Phase 4-B의 V11 격리 사본을 `.runtime-backups/phase4c-quality-proof/data`로 다시 복제하고 localhost 전용 포트 33311에서 읽기 검증했다. 원본 MySQL과 기존 격리 사본은 변경하지 않았다.

| 항목 | 결과 |
|---|---:|
| Place | 10 |
| PlaceSource | 10 (`TOUR_API`) |
| 병합 후보 | 0 |
| 좌표 보유 | 10 (100%) |
| 좌표 누락 | 0 (0%) |
| 주소 누락 | 0 (0%) |
| 이미지 누락 | 10 (100%) |
| 신규 Place | 0 |
| 갱신 Place | 0 |
| 이번 실행의 ImportRun | 0 |

카테고리 분포는 관광지 10건이다. 구·군 분포는 강서구 2, 금정구 2, 사하구 1, 서구 1, 영도구 3, 해운대구 1이다. 현재 10건 표본은 특정 공급자·카테고리에 편중되어 있으므로 부산 전체 데이터 품질이나 추천 다양성을 대표하지 않는다.

## 품질 보고 기능

`PlaceDataQualityService`는 전체 Place를 JVM으로 읽지 않고 SQL 집계로 다음을 계산한다.

- 좌표·주소·이미지 누락 건수와 비율
- 공급자별 원천 수, 연결된 고유 Place 수, 이미지 라이선스 미확인 수
- 카테고리와 구·군 분포
- 병합 후보 수

수동 실행 예시:

```powershell
.\gradlew.bat bootRun --args="--spring.main.web-application-type=none --place.quality.enabled=true"
```

`place.quality.enabled`의 기본값은 `false`이며 API 호출과 DB 쓰기를 하지 않는다.

## 추천·룰렛 실제 DB 검증

격리 DB의 10개 Place를 사용해 실제 HTTP API를 검증했다.

| 항목 | 결과 |
|---|---|
| 카테고리 기반 추천 | 관광지 조건, 5건 반환 |
| 추천 후보 | 출발지 제외 9건 |
| 동일 seed 재현 | 동일 Place 순서 확인 |
| 강서구 필터 | 출발지 제외 후보 1건, 부족 상태 표시 |
| 룰렛 코스 | 요청 5건, 실제 5건 |
| 코스 중복 | 0건 |
| 지도 좌표 | 5/5 보유 |
| 방문 순서 | 5개 장소에 순서 생성 |

추천 가중치는 변경하지 않았다. 강서구 결과 부족은 DB에 강서구 Place가 2건뿐이고 그중 출발지 1건이 후보에서 제외되기 때문이다. 이미지가 모두 누락되어 카드에는 기본 이미지가 사용된다.

## 성능 기준선

MySQL 8.0.46, Place 10건, localhost dev 앱에서 워밍업 후 각 5회 측정했다.

| 요청 | 평균 | 최소 | 최대 |
|---|---:|---:|---:|
| 장소 전체 첫 페이지 | 10.6ms | 7ms | 24ms |
| 카테고리+구·군 복합 필터 | 8.2ms | 6ms | 13ms |
| 장소명 검색 | 6.6ms | 5ms | 9ms |

품질 집계는 37ms, JPA 첫 페이지 최초 측정은 119ms였다. 추천 최초 측정은 339ms였다. 표본이 10건뿐이므로 부하 성능이나 대량 데이터 성능으로 일반화할 수 없다. 장소 목록은 서버 페이징, 추천 후보는 최대 200건 제한을 유지한다. 목록 DTO 변환에는 lazy 연관을 순회하지 않고, 로그인 회원의 즐겨찾기 상태도 현재 페이지 ID를 일괄 조회하므로 새 N+1은 확인되지 않았다. SQL 쿼리 카운터 기반의 부하 테스트는 미수행이다.

## 출처 및 이용조건

- [한국관광공사 국문 관광정보 서비스](https://www.data.go.kr/data/15101578/openapi.do): 활용 가능한 관광정보를 선별 제공하며 사진은 공공누리 1유형·3유형을 포함한다. `cpyrhtDivCd` 원문을 `image_license`에 보존하고 출처를 표시해야 한다.
- [부산명소정보](https://www.data.go.kr/data/15063481/openapi.do): 공공데이터포털 메타데이터의 이용허락범위는 제한 없음이다.
- [부산맛집정보](https://www.data.go.kr/data/15063472/openapi.do): 공공데이터포털 메타데이터의 이용허락범위는 제한 없음이다.
- [부산푸디투어](https://www.data.go.kr/data/15063507/openapi.do): Place가 아닌 테마 콘텐츠로 분리한다.
- [부산 갈맷길 관광정보](https://www.data.go.kr/data/15077606/openapi.do): 이용허락범위 제한 없음이며 이미지 상대 URL에는 비짓부산 호스트가 필요하다는 안내가 있다.

부산 공급자는 확인된 포털 이용조건을 `content_license`와 `image_license` 양쪽에 보존하도록 수정했다. TourAPI는 공급자별 사진 코드 원문을 유지한다. 신규 수집은 `image_license`가 비어 있으면 Place에 이미지 URL을 저장하지 않고, 기존 데이터도 알려진 이미지 이용조건이 연결된 Place만 목록·상세·즐겨찾기에 표시한다. 그 외에는 기본 이미지를 사용하며 운영 배포 전 개별 원천의 최신 조건을 다시 확인해야 한다.

## 단계적 확대 실행 절차

STS에서 키가 전달되는 격리 DB 실행 설정을 사용할 때 다음 순서를 지킨다.

1. 격리 DB 이름, Flyway V11, 백업·복구 가능성을 확인한다.
2. 공급자별 `limit=20`, `dry-run=true`로 개별 실행한다.
3. 응답 필드와 누락률을 확인하고 푸디 테마가 Place로 저장되지 않는지 확인한다.
4. 같은 조건으로 실제 저장 후 한 번 재수집해 Place/PlaceSource 건수가 증가하지 않는지 확인한다.
5. 품질 Runner와 추천 검증을 실행한다.
6. 호출 한도와 품질을 확인한 공급자만 20 → 50 → 100처럼 단계적으로 확대한다.

예시:

```powershell
.\gradlew.bat bootRun --args="--spring.main.web-application-type=none --place.import.enabled=true --place.import.providers=TOUR_API --place.import.dry-run=true --place.import.page-size=20 --place.import.limit=20"
```

키 값은 STS 환경변수에만 두며 명령 인수나 문서에 넣지 않는다. 자동 수집은 활성화하지 않는다.

## 테스트와 미완료 사항

- 공급자 fixture 파싱, 정규화, 부산 좌표 검증, 원천 멱등 처리, 재수집, Dry-run 무변경 테스트를 유지한다.
- 부산 공급자 이미지 라이선스 매핑 테스트와 품질 집계 테스트를 추가했다.
- Java 17 `gradlew clean build`: 성공
- 전체 자동 테스트: 307개 통과, 실패·오류 0
- H2 V1~V11 및 Hibernate schema validation: 성공

미완료:

- 이번 세션의 실제 API 표본 20건 호출
- 공급자별 실제 확대 적재와 재수집 통계
- 확대 DB 기준 누락률·중복 후보·추천 편중 분석
- 200건 후보 규모의 성능/쿼리 카운터 검증
- 원본 MySQL 반영

따라서 Phase 5 코드 개발은 가능하지만, “실제 확장 데이터로 검증 완료”를 진입 조건으로 삼는 단계는 STS 키 환경에서 위 절차를 수행한 뒤 진행해야 한다. Git commit/push는 수행하지 않았다.
