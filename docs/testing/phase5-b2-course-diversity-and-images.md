# Phase 5-B-2 여행 코스 다양성 및 장소 이미지 개선

검증일: 2026-11-06 (Asia/Seoul)

## 1. 식당 연속 발생 원인

- 후보 선택은 원시 카테고리 문자열의 중복 여부만 보정했다. 같은 방문 목적을 가진 다른 표기나 역할 단위의 과도한 중복을 알지 못했다.
- 선택 후 `nearestNeighbor`가 오직 현재 지점과의 직선거리만 최소화해, 선택된 음식점이 가까이 있으면 음식점 → 음식점 순서가 다시 만들어졌다.
- 점수·사용자 카테고리 조건·seed 선택 자체가 잘못된 것은 아니므로 이 부분은 유지했다.

## 2. 코스 순서 개선 알고리즘

실제 카테고리와 확인 가능한 동의어만 다음 역할로 매핑한다.

- `MEAL`: 음식점, 식사, restaurant
- `CAFE`: 카페, 디저트, cafe
- `SIGHTSEEING`: 관광지, 명소, attraction
- `CULTURE`: 문화시설, 박물관, 전시, culture
- `WALKING`: 공원, 산책, 갈맷길, walking
- 나머지: `OTHER`

후보 선택은 기존 점수와 seed 기반 가중 랜덤을 유지하면서 새 역할에 12점의 novelty bonus를 준다. 최종 순서는 최대 5곳이므로 최대 120개 순열을 비교한다.

1. 기존 nearest-neighbor의 총 직선거리를 기준선으로 계산한다.
2. `기준선 × 1.35 + 1,000m` 이하인 순열만 허용한다.
3. 허용 순열을 음식점 연속 횟수 → 동일 역할 연속 횟수 → 총 직선거리 → Place ID 순으로 비교한다.
4. 음식점만 있거나 이동거리 상한 안에서 분리할 수 없으면 연속을 허용하고 제한사항으로 설명한다.

같은 입력과 같은 `randomSeed`는 같은 장소 및 순서를 반환한다. 실제 운영시간·식사시간·이동시간은 생성하지 않는다.

## 3. 기존 대비 실제 결과

- Fixture `음식점 2 + 관광지 2`: 연속 음식점 0회.
- Fixture 음식점 3곳뿐인 경우: 3곳을 정상 반환하고 후보 구성상 연속 가능 제한사항 표시.
- 실제 격리 DB, `관광지 + 음식점`, 5곳, seed 42: 음식점 4곳과 관광지 1곳이 선택되어 수학적으로 가능한 최소인 음식점 연속 2회로 정렬했다.
- 실제 격리 DB, 3곳, seed 43: `음식점 → 관광지 → 음식점`으로 연속 음식점 0회.
- seed 42를 같은 조건으로 재실행한 장소 ID와 순서는 완전히 일치했다.
- 다양성 순열은 nearest-neighbor 기준 대비 거리 상한을 넘지 않는다.

## 4. 이미지 공급자 선정 근거

우선순위는 기존 공공데이터 이미지 → 검증된 외부 이미지 → placeholder다. 이번 단계에서는 이미 적재된 TourAPI/부산 공공데이터 이미지만 사용했다.

Wikimedia Commons는 MediaWiki `imageinfo`의 `url`과 `extmetadata`로 원본 페이지, 저작자, 라이선스, 라이선스 URL 등의 메타데이터를 조회할 수 있다. 다만 이름 검색만으로 부산의 동일 장소를 확정할 수 없고, 현재 DB에는 매칭 근거와 `VERIFIED/REVIEW_REQUIRED/REJECTED` 상태를 보존할 구조가 없다. 따라서 자동 연결은 하지 않고 향후 수동 검토형 공급자로만 검토한다.

- MediaWiki Imageinfo: https://www.mediawiki.org/wiki/API:Imageinfo/en
- CommonsMetadata: https://www.mediawiki.org/wiki/Extension:CommonsMetadata/en
- Commons 외부 재사용 안내: https://commons.wikimedia.org/wiki/Commons:Reusing_content_outside_Wikimedia/en

## 5. 라이선스·매칭·URL 정책

- `PlaceSource.imageLicense`가 비어 있으면 `Place.imageUrl`이 있어도 표시하지 않는다.
- 허가된 기존 이미지가 있으면 추천 DTO, 룰렛 DTO와 저장 코스 조회 DTO까지 전달한다.
- 저장 코스는 이미지 URL을 스냅샷 DB 컬럼에 복제하지 않고 현재 Place와 라이선스 정책을 다시 조회한다.
- URL은 `http`/`https`와 정상 host만 허용한다. localhost, `.local`, `.internal`, loopback, link-local, 사설 IPv4 및 로컬 IPv6 주소를 차단한다.
- 외부 URL을 서버가 프록시하거나 다운로드하지 않는다. 브라우저 로드 실패는 로컬 placeholder로 대체한다.
- 장소명만 유사하거나 이미지 라이선스/실제 장소 일치가 불명확한 외부 이미지는 표시하지 않는다.

## 6. 기존 이미지 데이터 및 실제 로딩 결과

격리 MySQL V12 기준:

- Place: 395건
- PlaceSource: 395건
- 이미지 URL 보유: 384건
- 알려진 이미지 이용조건 보유: 384개 Place
- 이미지 URL 없음: 11건
- 신규 외부 이미지 연결: 0건
- DB 마이그레이션: 없음

실제 Chromium 표본:

- 장소 목록 첫 페이지 12개: 실제 이미지 11개 정상 로드, 이미지가 없는 1개 placeholder.
- 장소 상세: 실제 1200px 이미지, 원천과 콘텐츠/이미지 이용조건, Leaflet 지도 정상.
- 개인화 추천 10개: viewport에 들어온 8개 중 실제 이미지 7개와 placeholder 1개 정상, 나머지 2개는 lazy-load 대기 상태.
- 룰렛 5곳: viewport에 들어온 4개 실제 이미지 정상, 나머지 1개 lazy-load 대기 상태.
- 저장 코스 3곳: 실제 이미지 3개 정상.
- 즐겨찾기 1곳: 실제 이미지 정상.

전체 384개 URL에 대한 장시간 네트워크 가용성·403·타임아웃 전수 검사는 하지 않았다. 표시 여부와 현재 화면 표본의 브라우저 로딩을 검증했으며 공급자 상태에 따른 향후 실패는 fallback으로 처리한다.

## 7. UI 변경

- 추천 결과와 룰렛 코스 카드에 16:9/4:3 반응형 이미지 영역을 추가했다.
- 저장 코스 상세는 현재 Place 이미지 정책을 적용해 실제 이미지를 표시한다.
- 모든 동적 이미지에 lazy loading, 장소명 기반 alt, `object-fit: cover`, 로드 실패 fallback을 적용했다.
- 목록·상세·즐겨찾기의 기존 이미지 정책과 디자인을 유지했다.

## 8. Chromium 반응형·접근성 QA

| viewport | 화면 | 결과 |
|---:|---|---|
| 1440×900 | 목록·상세·추천·룰렛·저장 코스·즐겨찾기 | 실제 이미지/placeholder 정상, overflow 없음 |
| 768×1024 | 룰렛 | 619px 카드, 128px 이미지, overflow 없음 |
| 390×844 | 룰렛 | 313px 카드, 232×131px 이미지, 52px 버튼, overflow 없음 |
| 360×800 | 장소 목록 | 1열 317px 카드, 315×197px 이미지, overflow 없음 |

- alt는 모두 `<장소명> 대표 이미지` 형식으로 확인했다.
- 서버가 확정한 순서와 애니메이션/카드 결과가 일치했다.
- console warning/error: 0건.
- QA 증적 인덱스: `docs/testing/qa/phase5-b2/README.md`
- 기존 변경 전/Phase 5-B 캡처: `docs/testing/qa/screenshots/`, `docs/testing/qa/phase5-b/`
- 이번 도구는 캡처를 대화 내 표시했지만 로컬 저장 API가 없어 신규 이미지 바이너리 저장은 미완료다.

## 9. 자동화 테스트

- `gradlew clean build`: 성공
- Java 테스트: 323개 통과, 실패 0
- 기존 317개 + 신규 6개
- `node --check`: recommendation-engine, course-roulette, place-explorer, saved-course-map 4개 통과
- 신규 검증: 혼합 역할 순서, 음식점 전용 후보, 이미지 DTO 전달, 라이선스 미확인 차단, 위험 URL 차단, 이미지 없음 fallback.

## 10. 데이터 안전과 정리

- 기존 `phase5b-qa-proof`를 `phase5b2-qa-proof`로 복제했다.
- 백업 SHA-256 `629E27D9C74A49F9EABEB8526874EBD09C8F9446BB29AD427BE3B91BCD549FE3` 일치를 확인했다.
- QA 대상은 `127.0.0.1:33320/cityroulette`, 애플리케이션은 `127.0.0.1:18086`이었다.
- QA 회원 ID 9와 그 회원의 코스·즐겨찾기만 정리했다.
- 최종 격리 DB: 기존 회원 1명, Place 395, PlaceSource 395, SavedCourse 0, Favorite 0.
- 원본 MySQL 접근·변경, Flyway 변경, 공공데이터 재수집, Git commit/push는 수행하지 않았다.

## 11. 미검증 사항과 Phase 5-C

- 전체 384개 외부 이미지 URL의 장기 가용성 전수 측정은 미검증이다.
- Wikimedia 등 외부 이미지 자동 검색·매칭·저장은 구현하지 않았다. 도입 시 V13+에 이미지 원본/저작자/라이선스 URL/매칭 근거/검증 상태를 저장하고 관리자 수동 검토가 필요하다.
- 신규 Chromium 캡처의 로컬 바이너리 저장은 도구 제한으로 미완료다.

코스 다양성, 기존 공공이미지의 안전한 화면 확장, seed 재현성, 반응형 UI와 회귀 테스트는 완료되어 Phase 5-C 진행이 가능하다.
