# 확인된 한계와 개선 후보

| 위치 | 확인 근거 | 개선 후보 |
| --- | --- | --- |
| `domain/recommendation/service/RecommendationCandidateFilter.java`의 `select`, `identity` | 이름에서 공백을 제거한 값을 후보 중복 키로 사용한다. 서로 다른 지점이 같은 이름을 쓰면 먼저 읽은 한 건만 남을 수 있다. | 주소·원천·좌표를 함께 고려하는 별도 후보 검증 및 동명이점 fixture |
| `domain/recommendation/service/RecommendationCandidateFilter.java`의 `select` | DB 검색에 상한을 먼저 적용하고 추천 서비스가 이후 거리를 평가한다. 상한 밖의 후보는 거리 평가 기회를 얻지 못한다. | 데이터가 커지면 DB 범위 조회나 다단계 후보 수집의 품질과 성능 비교 |
| `domain/course/service/CourseGenerationService.java`의 `buildCoursePlaces` | 장소별 시간·비용 상태를 UNKNOWN으로 반환한다. | 운영시간, 체류시간, 공개 요금 데이터를 별도로 검증·적재 |
| `domain/transport/service/TransitRoutePlannerService.java`의 `continuous` | 번호가 한 칸이라도 끊기는 구간은 현재 데이터에서 신뢰할 수 있는 구간으로 처리하지 않는다. | 공식 전체 정차 데이터 적재와 노선 방향 품질 점검 |
| `domain/course/service/CourseOrderingService.java`의 `permute` | 방문 순서를 완전 열거한다. 현재 5곳에서는 제한적이지만 장소 상한을 올리면 팩토리얼로 증가한다. | 상한 확대 요청이 있을 때 근사 최적화 또는 가지치기 비교 |
| `domain/course/service/GeneratedCourseRegistry.java`의 `consume` | 서버 메모리의 생성 결과를 기준으로 저장 토큰을 확인한다. | 멀티 인스턴스 배포가 필요할 때 만료·일회성 소비를 지원하는 공유 저장소 검토 |
| `global/security/SecurityConfig.java`의 URL 인가 규칙 | 로컬 폼 로그인·CSRF·회원 전용 설정은 확인할 수 있으나 공개 배포의 TLS와 세션 운영은 로컬 검증 범위 밖이다. | 배포가 필요할 때 HTTPS, 쿠키와 프록시 설정을 별도 검증 |

## 조치 완료

- 저장 코스 지도는 공백, 숫자 변환 실패, 허용 범위 밖 좌표를 제외한다. 출발지 없이도 유효한 방문 좌표가 있으면 지도를 표시하고, 표시할 좌표가 없으면 안내를 표시한다.
- 저장 코스 팝업의 장소명·방문 번호와 룰렛 출발지 팝업 이름은 텍스트가 채워진 DOM 요소로 렌더링한다.
- 저장 코스 지도의 누락 좌표, 정상 좌표, Leaflet 미로드 경로는 `node --test e2e/saved-course-map.test.cjs`로 확인할 수 있다. 실제 브라우저의 지도 타일 렌더링은 별도 검증 대상이다.
