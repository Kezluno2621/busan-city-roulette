# Phase 5-B UI·UX 개선 및 반응형 검증

검증일: 2026-11-02 (Asia/Seoul)

## 1. 검증 환경과 데이터 안전

- Java 17, Spring Boot `dev`, ChatGPT in-app Chromium 브라우저
- 격리 MySQL 8.0.46: `127.0.0.1:33319/cityroulette`
- 애플리케이션: `127.0.0.1:18083`
- Flyway V12, Place 395건, PlaceSource 395건
- Phase 5-A의 정리된 QA 사본을 `phase5b-qa-proof`로 다시 복제했다.
- 복제 백업 SHA-256은 `629E27D9C74A49F9EABEB8526874EBD09C8F9446BB29AD427BE3B91BCD549FE3`로 일치했다.
- Flyway 실행과 공공데이터 수집 Runner는 비활성화하고 Hibernate `validate`만 사용했다.
- 원본 MySQL에는 연결하거나 쓰지 않았다.

## 2. 기존 UI 문제점

1. 메인 화면이 룰렛 한 가지 행동만 강조해 여행지 탐색·추천·저장 흐름을 파악하기 어려웠다.
2. 추천 화면만 공통 내비게이션을 사용하지 않아 화면 간 이동과 브랜드 일관성이 끊겼다.
3. 장소가 395건으로 늘면서 모바일 페이지네이션에 33개 페이지가 모두 표시됐다.
4. 추천과 룰렛의 처리 중 상태가 시각적으로만 표시되고 폼 `aria-busy`, 결과 포커스가 없었다.
5. 코스 삭제가 브라우저 기본 `confirm`에 의존했고, 오래된 version 수정 시 409 Whitelabel 페이지가 노출됐다.
6. 공통 푸터, 화면 간 다음 행동, 데이터 한계 안내의 표현이 일관되지 않았다.

## 3. 디자인 개선 방향

- 기존 navy/blue/mint 색상을 유지하고 surface, radius, shadow, focus 규칙을 디자인 토큰으로 보강했다.
- 모든 공개 핵심 화면에 같은 브랜드 내비게이션과 하단 메뉴를 사용했다.
- 44px 이상 터치 영역, 명확한 주·보조 버튼, 카드와 빈 상태의 공통 계층을 적용했다.
- 과도한 장식을 피하고 여행지 사진, 조건 입력, 결과 지도 순으로 시선이 흐르도록 구성했다.
- `prefers-reduced-motion`에서는 애니메이션·전환과 smooth scroll을 사실상 제거한다.

## 4. 수정한 화면과 코드

### 메인

- 추천 시작/여행지 둘러보기 두 개의 핵심 행동을 제공한다.
- 여행지 탐색 → 취향 추천 → 코스 룰렛의 3단계 기능 카드를 추가했다.
- 로그인 회원의 즐겨찾기·코스 저장 기능을 별도 배너로 안내한다.

### 관광지 목록·상세

- 카드 이미지 비율, hover/focus, 긴 장소명 줄바꿈과 하단 행동 정렬을 통일했다.
- 전체 페이지 번호 대신 현재 페이지 주변 최대 5개만 표시한다.
- 결과 수, 현재 페이지, 검색어를 한 영역에서 확인할 수 있다.
- 상세 화면에 추천으로 이어지는 CTA와 이미지/콘텐츠 이용조건 안내를 추가했다.
- 주소가 없는 데이터는 기존과 같이 `주소 정보가 없습니다.`로 표시하며 값을 생성하지 않는다.

### 추천·룰렛

- 공통 내비게이션과 `출발지 선택 → 장소 추천 → 코스 룰렛` 바로가기를 추가했다.
- 시간·예산 데이터 한계와 자동 저장되지 않는다는 점을 눈에 띄는 안내로 분리했다.
- 요청 중 버튼 잠금, `aria-busy`, 로딩 spinner, 오류/결과 포커스를 적용했다.
- 카테고리 선택을 44px 이상 선택 영역으로 만들고 모바일에서 1열로 표시했다.
- 서버 결과만 애니메이션과 카드에 표시하는 기존 동작 및 중복 요청 차단을 유지했다.

### 회원 화면

- 즐겨찾기와 저장 코스의 카드·빈 상태·공통 푸터를 정리했다.
- 코스 상세 관리 영역을 분리하고 삭제 전 명시적인 확인 패널을 표시한다.
- 오래된 version 수정은 HTTP 409를 유지하면서 최신 제목과 재시도 안내를 같은 상세 화면에 렌더링한다.

주요 수정 파일:

- `src/main/resources/static/css/style.css`
- `src/main/resources/static/js/recommendation-engine.js`
- `src/main/resources/static/js/course-roulette.js`
- `src/main/resources/templates/fragments/navigation.html`
- `src/main/resources/templates/fragments/footer.html`
- `src/main/resources/templates/index.html`
- `src/main/resources/templates/places/list.html`
- `src/main/resources/templates/places/detail.html`
- `src/main/resources/templates/recommend.html`
- `src/main/resources/templates/my/courses.html`
- `src/main/resources/templates/my/course-detail.html`
- `src/main/resources/templates/my/favorites.html`
- `src/main/java/com/cityroulette/controller/PlaceExplorerController.java`
- `src/main/java/com/cityroulette/controller/SavedCoursePageController.java`

## 5. 변경 전후 스크린샷

변경 전 자료는 `docs/testing/qa/screenshots/`, 변경 후 자료는 `docs/testing/qa/phase5-b/`에 있다.

| 화면 | 변경 전 | 변경 후 |
|---|---|---|
| 메인 데스크톱 | `screenshots/01-home-desktop.png` | `phase5-b/01-home-desktop.jpg` |
| 장소 목록 모바일 | `screenshots/11-places-small-mobile.png` | `phase5-b/02-places-small-mobile.jpg` |
| 추천 모바일 | `screenshots/13-recommend-mobile-fixed.png` | `phase5-b/07-recommend-mobile.jpg` |
| 저장 코스 상세 | `screenshots/08-saved-course-detail-desktop.png` | `phase5-b/13-saved-course-detail.jpg` |

추가 증적:

- 장소 상세/Leaflet: `03-place-detail-desktop.jpg`, `04-place-detail-mobile.jpg`
- 빈 검색 결과: `05-places-empty-mobile.jpg`
- 실제 10개 개인화 추천/지도: `08-personalized-results-desktop.jpg`
- reduced-motion 룰렛 5개 결과: `09-course-mobile.jpg`
- 즐겨찾기 빈 상태/목록: `10-favorites-empty-mobile.jpg`, `12-favorites-list.jpg`
- 409 결함/수정: `14-stale-version-conflict.jpg`, `15-stale-version-fixed.jpg`
- 삭제 확인 패널: `16-delete-dialog.jpg`

## 6. 반응형 실제 브라우저 결과

장소 목록에서 모든 지정 viewport를 실제 적용했다.

| viewport | 문서 가로 overflow | 카드 | 페이지 링크 | 결과 |
|---:|---:|---:|---:|---|
| 1440×900 | 없음 | 12 | 6(번호 5 + 다음) | 정상 |
| 1024×768 | 없음 | 12 | 6 | 정상 |
| 768×1024 | 없음 | 12 | 6 | 정상 |
| 390×844 | 없음 | 12 | 6 | 정상 |
| 360×800 | 없음 | 12 | 6 | 정상 |

- 360px에서는 카드가 1열이며 긴 장소명이 카드 밖으로 넘치지 않았다.
- 390px 추천 화면에서 카테고리 checkbox는 13px 고유 폭, 선택 영역은 한 줄 전체를 사용했다.
- 지도 폭은 390px viewport에서 콘텐츠 폭 319px와 일치했다.
- 버튼 높이는 모바일에서 50~52px로 터치 목표를 충족했다.
- 회원 내비게이션은 모바일에서 3단 행으로 재배치되며 가로 스크롤이 없었다.

## 7. 접근성 검증

- 공통 `nav`와 하단 `nav`에 접근 가능한 이름을 제공했다.
- 입력 요소는 연결된 label 또는 감싸는 label을 유지했다.
- 모든 주요 링크/버튼에 `:focus-visible` outline과 44px 이상 목표 크기를 적용했다.
- 추천 로딩은 `role=status`, 오류는 `role=alert`, 결과 요약은 포커스 가능한 상태 영역으로 제공한다.
- 코스 삭제 확인은 checkbox/label 기반으로 키보드에서 열고 닫을 수 있으며 확정 버튼과 분리했다.
- 브라우저의 `prefers-reduced-motion: reduce`가 실제 `true`인 환경에서 룰렛 재실행은 567ms에 완료됐고 spinning class가 남지 않았다. 동일 seed 결과도 일치했다.
- 전역 reduced-motion CSS가 smooth scroll, transition, animation을 제거한다.

## 8. 실제 기능 및 GUI QA

- 실제 격리 DB 395건 장소 목록, 복합 레이아웃, 빈 결과 상태 정상
- 실제 Place 좌표로 상세 Leaflet 지도와 마커 표시 정상
- 로컬 Kakao fixture의 부산역 선택 후 해운대 관광지 후보 12건 중 10건 카드/마커 표시
- 룰렛 5건, 중복 0, 방문 순서 1~5, seed 재현, 모바일 지도 정상
- 즐겨찾기 추가 후 회원 목록에 1건 표시
- 코스 3곳 저장, 상세 순서와 지도 표시, 제목 수정 정상
- 브라우저 두 화면의 오래된 version 수정으로 409 Whitelabel 결함 재현 후 수정: 최신 제목과 안내 렌더링 정상
- 삭제 확인 패널 표시와 취소 상태를 확인했다. 초기 native-confirm 검증 중 생성 코스가 삭제되어 DB `saved_courses` 1→0 및 연결 데이터 정리를 확인했으나, 브라우저 도구의 대화상자 처리 제한으로 삭제 후 목록 화면 증적은 남기지 못했다.
- 주요 검증 탭 console error/warning 0건

## 9. 자동화 테스트

- `node --check`: recommendation, roulette, place explorer, saved course map 모두 통과
- `gradlew clean build`: 성공
- Java 테스트: **317개 통과**, 실패 0, 스킵 0
- 신규 테스트:
  - 페이지네이션이 첫/중간/마지막 페이지에서 최대 5개로 제한되는지 검증
  - stale version 페이지 수정이 409와 사용자 안내 화면을 함께 반환하는지 검증
- 기존 인증, 파일, 장소 검색, 추천, 룰렛, 저장 코스, 즐겨찾기, 버스 경로 테스트가 모두 통과했다.

## 10. 정리, 미완료 및 Phase 5-C

QA 종료 전 테스트 회원 ID 8과 그 회원의 즐겨찾기·코스·여행 기록만 트랜잭션으로 정리했다. 최종 격리 DB는 기존 회원 1명, Place 395건, PlaceSource 395건, saved course/favorite 각 0건으로 시작 상태를 보존한다.

미완료/제한:

- Kakao 검색은 로컬 fixture이며 실제 키 외부 호출은 미검증이다.
- ODsay 정상 경로는 키가 없어 기존 안전 오류 처리만 유지한다.
- 이미지 원본의 실제 네트워크 응답속도는 공급자 상태에 따라 달라질 수 있다.
- 삭제 후 목록 리디렉션의 최종 화면 캡처는 남기지 못했지만 서비스/컨트롤러 자동 테스트와 격리 DB 삭제·연결 정리는 확인했다.

Critical/High 미해결 UI 결함은 없다. 실제 외부 지도·교통 키 검증과 더 정교한 이미지 성능 측정을 제외하면 Phase 5-C 진행이 가능하다.
