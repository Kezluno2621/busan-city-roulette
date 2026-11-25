# 코드 읽기 가이드

한 기능의 Controller에서 시작해 Service, Repository, DTO와 테스트 순서로 읽으면 입력이 어떻게 결과로 바뀌는지 따라갈 수 있다.

접근 정책은 `SecurityConfig`, 회원 인증 정보의 구성은 `CustomUserDetailsService`에서 확인할 수 있다. 거리 계산은 `DistanceCalculator`와 단위 테스트를 함께 보고, 점수 산식과 코스 선택은 [추천 알고리즘](../features/RECOMMENDATION_ALGORITHM.md)을 참고한다.

| 기능 | 먼저 볼 클래스와 메서드 | 설명 |
| --- | --- | --- |
| 장소 탐색 | `PlaceExplorerController`, `PlaceExplorerService`, `PlaceRepository` | 검색 조건을 전달하고 DB 페이지 및 출처를 화면 모델로 조합한다. |
| 관광 수집 | `PlaceImportRunner`, `PlaceCollectionService`, `PlaceImportService.importItems` | 공급자 페이지 조회, 표준화, DB 반영을 나눈다. `NormalizedPlace`에 원천과 라이선스를 남긴다. |
| 회원 | `SecurityConfig`, `MemberController`, `MemberService`, `CustomUserDetailsService` | 가입 검증과 해시 저장, 폼 로그인과 URL 접근 정책을 분리한다. |
| 저장 코스 | `CourseGenerationController`, `GeneratedCourseRegistry`, `SavedCourseService` | 생성 응답의 토큰을 저장 단계에 사용한다. 저장 코스와 방문 순서 스냅샷을 보관한다. |
| 여행 기록 | `TripRecordController`, `TripRecordService`, `FileStorageService` | multipart 입력과 소유권을 처리한다. 파일은 저장 경로에, 기록과 파일 메타데이터는 DB에 둔다. |
| 교통 검색 | `BusQueryService`, `TransitGraphService`, `TransitRoutePlannerService.plan` | 주변 정류장을 좁히고 저장된 정차 순서에서 직통 또는 한 번 환승을 검토한다. |
| 추천 | `PersonalizedRecommendationService.recommend`, `RecommendationCandidateFilter.select`, `RecommendationScorer` | 입력과 DB 후보, 점수 계산, 응답 구성을 분리한다. |
| 코스 | `CourseGenerationService`, `CourseCandidateSelector.select`, `CourseOrderingService.order` | 코스 후보 선택과 방문 순서는 별도 규칙으로 계산한다. |

Repository의 `findBy...` 선언은 Spring Data JPA가 메서드 이름을 해석한다. `@Query`를 붙인 쿼리는 선언된 내용을 사용하며, `nativeQuery`가 설정된 경우 실제 SQL이다. `Optional`은 조회 결과가 없을 수 있음을 표현한다. 서비스에서 빈 결과를 화면의 빈 상태로 반환할지, 예외로 변환할지 결정한다. `@Transactional`이 붙은 쓰기 메서드는 관련 DB 작업을 한 트랜잭션으로 묶는다.

`templates/recommendation/index.html`은 추천 입력과 결과 영역을 제공한다. `js/common/main.js`는 출발지 검색에, `js/course/roulette.js`는 코스 생성 요청·결과 카드·지도에 사용한다. 코스 애니메이션은 서버가 정한 방문 장소를 표시한다. `style.css`의 반응형 규칙은 화면 폭에 따라 폼과 카드 배치를 조정한다.

더 자세한 파일 목록은 [소스 색인](08-source-index.md), 발표 질문은 [코드 답변 가이드](../submission/presentation/evaluation-based-code-defense-guide.md)를 참고한다.
