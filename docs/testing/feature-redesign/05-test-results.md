# 테스트 결과와 알려진 한계

## 추가 검증 항목

- 선호 카테고리가 다른 카테고리를 배제하지 않음
- ONLY 모드의 DB 카테고리 제한
- REQUIRED 모드의 선택 카테고리 포함 시도
- 즉흥 모드에서 출발지 생략 가능
- 최근 결과 제외
- 고정 seed 사용 시 최근 기록과 무관한 재현성
- 음식점과 다른 역할이 함께 있을 때 연속 음식점 최소화
- 기존 `/recommend` POST 및 코스 저장 토큰 호환

## 실행 결과

- 표적 테스트: `CourseGenerationServiceTest`, `PersonalizedRecommendationServiceTest`, `RecommendationPageControllerTest` 통과
- 전체 `gradlew.bat clean build`: 성공
- 전체 테스트: 333개 통과, 실패 0개
- JavaScript 구문 검사: `node --check course-roulette.js` 통과

## 알려진 한계

- 최근 추천 기록은 현재 브라우저의 localStorage에만 저장되며 다른 기기와 동기화되지 않는다.
- 데이터에 특정 카테고리 후보가 부족하면 REQUIRED 모드를 모두 만족하지 못할 수 있다.
- 시간·비용 데이터가 없는 장소는 정확하게 평가하지 않는다.
- 버스망이 부분 데이터이므로 경로 후보 없음은 부산 전역의 이동 불가를 의미하지 않는다.
- 지도 타일은 외부 인터넷 연결에 의존한다.

## 실제 demo 브라우저 검증

- `demo` 프로필, MySQL 8.0.46, `http://127.0.0.1:18091/recommend`에서 확인
- 즉흥 룰렛 연속 2회 결과가 서로 다른 장소로 구성됨
- 음식점 선호 4곳 요청에서 음식점과 관광지·문화시설이 함께 구성됨
- demo DB에는 카페 카테고리가 0건이므로 카페 포함은 불가능하며 화면 제한사항으로 안내
- 모바일 폭에서 카테고리 칩이 글자 단위로 줄바꿈되지 않고 한 줄 단어로 표시됨
- 브라우저 콘솔 오류 없음
