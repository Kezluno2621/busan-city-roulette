# 패키지 배치 기준

전체 트리와 화면 위치는 [프로젝트 구조](PROJECT_STRUCTURE.md)를 참고한다.

## 변경 전과 변경 후

| 변경 전 | 변경 후 |
| --- | --- |
| 루트의 `controller`, `service`, `repository`, `domain`, `dto` | 관련 클래스를 `domain/<기능>/<역할>`에 모음 |
| 외부 API 클라이언트·응답과 추천·수집 서비스가 같은 패키지에 위치 | 전송과 원본 응답은 `integration`, 변환·검증·적재는 `ingestion` |
| `config`에 인증·외부 API 설정·작업 Runner가 공존 | 보안은 `global.security`, API 설정은 각 공급자, Runner는 수집 기능에 위치 |
| 공통 `my` 템플릿 및 일렬로 놓인 JS 파일 | 회원·저장 코스·즐겨찾기·여행 기록별 템플릿 및 화면별 스크립트 |

## 경계를 사용하는 방법

- 외부 응답은 공급자 계약에 맞춘 타입으로 수신한다. 예를 들어 부산 관광 페이지의 `ProviderPage`는 `integration.busan.dto`에 둔다.
- `ingestion.place.provider`는 공급자별 클라이언트와 매퍼를 연결한다. `ingestion.history`는 장소·버스 수집이 함께 사용하는 `ImportRun`과 이력 조회·저장을 담당한다.
- 수집기는 원본을 `NormalizedPlace`로 변환하고 적재 서비스를 호출한다. `PlaceImportService`는 중복·동기화 규칙을 관리하고 `PlaceRepository`와 `PlaceSourceRepository`로 저장한다.
- 임시 코스 생성과 회원별 코스 저장은 하나의 기능에 둔다. 요청, 생성 결과, 저장 결과 DTO는 서로 다른 계약을 유지한다.
- 장소 상세의 주변 정류장과 즐겨찾기 상태는 각각 교통·즐겨찾기 조회를 조합한다. DB 매핑과 접근 제어는 기존 서비스에서 처리한다.
- 테스트는 대응하는 기능 패키지 아래에 둔다. 신규 패키지는 모두 애플리케이션 루트인 `com.cityroulette` 아래에 위치한다.

