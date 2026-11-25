# Phase 5-A 실제 GUI 브라우저 통합 QA

검증일: 2026-11-01 (Asia/Seoul)

## 1. 실행 환경과 격리 안전성

- 애플리케이션: Java 17.0.12, Spring Boot 3.5.16, `dev` 프로필
- DB: MySQL 8.0.46, `127.0.0.1:33317/cityroulette`
- 브라우저: ChatGPT in-app browser(IAB, Chromium 계열). 자동화 인터페이스가 상세 버전 문자열은 제공하지 않아 버전은 미기록이다.
- 서버: `127.0.0.1:18081`
- 대상 DB는 `.runtime-backups/phase4c3-scale-proof-v2/data`를 새 디렉터리
  `.runtime-backups/phase5a-qa-proof/data`로 복제한 QA 전용 사본이다.
- 복제 원본과 함께 보관된 논리 백업 SHA-256:
  `629E27D9C74A49F9EABEB8526874EBD09C8F9446BB29AD427BE3B91BCD549FE3`
- 원본 DB에는 연결하거나 쓰지 않았다. Flyway는 실행하지 않고 Hibernate `validate`만 사용했다.
- 수집 및 품질 Runner는 모두 비활성화했다.

시작 기준은 Flyway V12, Place 395건, PlaceSource 395건, 좌표 보유 Place
395건이었다. 테스트 회원/코스/즐겨찾기/여행 기록 쓰기는 QA 사본에서만
수행했다.

## 2. 브라우저와 해상도

실제 GUI 브라우저에서 다음 viewport를 적용했다.

| 구분 | 크기 | 가로 스크롤 | 결과 |
|---|---:|---:|---|
| Desktop | 1440x900 | 없음 | 정상 |
| Tablet | 768x1024 | 없음 | 정상 |
| Mobile | 390x844 | 없음 | 수정 후 정상 |
| Small mobile | 360x800 | 없음 | 정상 |

390px 추천 화면에서 카테고리 체크박스가 입력의 전역 `width: 100%` 규칙을
상속해 텍스트를 화면 밖으로 밀어내는 문제가 실제 스크린샷으로 재현됐다.
모바일에서 2열 flex 배치와 체크박스 고유 너비를 지정한 뒤 문서 폭 375px,
viewport 390px, 체크박스 13x13px로 재검증했다.

## 3. 실제 GUI 시나리오 결과

총 42개 브라우저/보안/데이터 확인 항목을 실행했다. 40개는 최초 실행부터
통과했고 2개는 결함을 재현한 뒤 수정 및 재검증하여 최종 통과했다.

### 공개 화면과 장소 탐색

- 메인 페이지, 내비게이션, CSS/JS 로딩 정상
- `/places`에서 실제 DB 395건 표시
- 페이지 2 이동, 키워드 `해운대`, 음식점+해운대구 복합 필터 정상
- 결과 없음 화면 정상
- 상세 `/places/1`과 존재하지 않는 ID의 404 화면 정상
- 장소명 한글 깨짐 없음
- 이용조건이 불명확한 이미지는 기존 정책을 유지했다.

### Leaflet

- 상세 화면에 지도 컨테이너 1개, 실제 Place 좌표 마커 1개 표시
- OpenStreetMap 타일 15개 로딩 확인
- 장소 팝업, 확대/축소 컨트롤, 출처 링크 표시
- 정류장 데이터가 없는 반경은 오류 대신 빈 상태 표시
- 개인화 추천 지도 마커 10개 표시
- 코스 지도에 출발점/장소 순서 마커와 점선 연결 표시
- `장소 간 직선 연결 — 실제 도로·도보·버스 이동 경로가 아닙니다.` 안내 확인

### 추천과 룰렛

실제 Kakao 키를 복사하지 않고 `e2e/kakao-local-stub.mjs`를 QA 서버에서만
사용해 부산역 좌표 선택 흐름을 재현했다. 장소 후보와 점수 계산은 격리
MySQL의 실제 395건을 사용했다.

- 해운대구 관광지: 후보 12곳 중 10곳 렌더링
- 추천 점수, 이유, 거리, `NOT_EVALUATED` 교통 상태 표시
- 룰렛 5곳 선택, 중복 0, 순서 1~5 확인
- seed 42 재실행 결과가 동일함을 확인
- 서버 결과와 카드 결과가 일치
- 실행 후 버튼이 `다시 뽑기`로 복구

기존 ODsay 추천은 키가 없는 환경에서 HTTP 500 Whitelabel 페이지와 예외
스택 트레이스를 노출했다. Controller에서 `OdsayApiException`을 처리하도록
수정한 뒤 HTTP 200 폼 화면과 안전한 안내 문구만 표시되는 것을 재검증했다.

### 회원·소유권·CSRF

- QA 전용 회원 2명 생성(기존 회원 미변경)
- 잘못된 비밀번호 안내 및 정상 로그인/로그아웃 확인
- 비회원 `/my/courses`, `/my/favorites` 접근은 `/login`으로 이동
- 회원 A 코스를 회원 B가 조회하면 내용 노출 없이 404
- 회원 B 즐겨찾기 목록은 비어 있어 회원 간 격리 확인
- 인증된 세션이라도 CSRF 없는 즐겨찾기 POST는 HTTP 403

회원가입 레코드 생성은 브라우저의 계정 생성 확인 동작을 피하기 위해 같은
localhost 앱의 HTTP 폼 엔드포인트로 수행했다. 로그인과 이후 기능은 실제
GUI 브라우저에서 검증했다.

### 코스·즐겨찾기·파일

- 카드 즐겨찾기 추가, 목록 표시, 상세 화면 해제/재등록 확인
- 룰렛 결과 3곳 저장, 목록/상세/순서/지도 확인
- 코스 제목 수정 확인
- 여행 기록 PNG 업로드(36,823 bytes), 상세 메타데이터 및 브라우저 다운로드 확인
- 버스 경로 API는 `NO_CANDIDATE`, `PARTIAL_DATA`와 부분 데이터 해석 문구를 정상 반환

## 4. 발견 결함과 수정

| 심각도 | 결함 | 수정 | 재검증 |
|---|---|---|---|
| High | ODsay 키/공급자 오류가 500 Whitelabel 및 스택 트레이스로 노출 | `RecommendationPageController`에서 `OdsayApiException`을 잡아 안전한 폼 오류로 변환 | HTTP 200, 키/스택 미노출 |
| Medium | 390px 추천 화면 카테고리 체크박스가 100% 폭을 가져 텍스트가 잘림 | 모바일 체크박스 2열 배치, input 너비/높이 자동화 | 문서 가로 오버플로 없음 |

## 5. 테스트 및 정적 검사

- `gradlew clean build`: 성공
- Java 테스트: 315개 통과, 실패 0, 스킵 0
- 신규 회귀 테스트: ODsay 공급자 불가 시 안전한 폼 오류 반환 1개
- `node --check`: `course-roulette.js`, `recommendation-engine.js`,
  `place-explorer.js`, `saved-course-map.js`, QA Kakao stub 모두 성공
- 주요 페이지 브라우저 console warning/error: 0건

별도 Playwright/npm 의존성은 추가하지 않았다. 사용 가능한 IAB Chromium의
Playwright locator 인터페이스로 실제 브라우저를 자동 조작했다. 로컬 검색
fixture의 반복 실행 방법은 `e2e/README.md`에 기록했다.

## 6. 스크린샷

개인정보와 비밀번호를 포함하지 않는 PNG를 `docs/testing/qa/screenshots/`에 저장했다.

- `01-home-desktop.png`
- `02-places-desktop.png`
- `03-place-detail-map-desktop.png`
- `04-recommend-desktop.png`
- `05-personalized-recommendation-desktop.png`
- `06-course-roulette-desktop.png`
- `07-favorites-desktop.png`
- `08-saved-course-detail-desktop.png`
- `09-places-tablet.png`
- `10-recommend-mobile.png` (결함 재현)
- `11-places-small-mobile.png`
- `12-place-map-mobile.png`
- `13-recommend-mobile-fixed.png`
- `14-odsay-unavailable-safe-error.png`

## 7. 데이터 정리와 보존

QA 종료 시 다음 임시 데이터를 정확한 회원 ID 범위로 삭제했다.

- 회원 2
- SavedCourse 1 / SavedCoursePlace 3
- PlaceFavorite 1
- TripRecord 1
- 업로드 PNG 1

최종 상태:

- Flyway V12
- places 395 / place_sources 395
- 좌표 보유 places 395
- PlaceSource `(provider, external_id)` 중복 0, orphan 0
- bus_stops 30 / bus_routes 1 / bus_route_stops 20
- 기존 회원 ID 1 보존
- saved_courses, favorites, trip_records 각 0

## 8. 미검증과 Phase 5-B 준비

다음 항목은 성공으로 간주하지 않는다.

- 실제 Kakao API 키를 사용한 외부 검색: localhost fixture만 사용
- 실제 ODsay 정상 경로 응답: 키 미설정 오류 처리만 검증
- `prefers-reduced-motion` 브라우저 미디어 에뮬레이션: 구현/단위 회귀는 유지되나 이번 GUI 도구에서 미실행
- 오래된 course version의 브라우저 409와 저장 토큰 재사용: 기존 자동 테스트 범위이며 이번 GUI에서 재실행하지 않음
- 코스 삭제 버튼의 실제 GUI 클릭: QA 정리는 안전한 DB 트랜잭션으로 수행
- 전체 부산 버스망 경로: 현재 30/1/20 표본 범위만 사용

Critical 미해결 결함은 없고, 발견된 High/Medium 결함은 수정 및 재검증했다.
실제 외부 Kakao/ODsay 정상 호출과 reduced-motion 전용 브라우저 실행을 제외하면
Phase 5-B 진입이 가능하다.
