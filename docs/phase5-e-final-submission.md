# Phase 5-E 최종 제출 패키지

검증일: 2026-11-24 (Asia/Seoul)

## 1. 제출 결과물

| 결과물 | 역할 | 상태 |
| --- | --- | --- |
| `README.md` | 프로젝트 소개, 아키텍처, 실행, 테스트, 제한사항 | 완료 |
| `docs/final-project-report.md` | 요구사항·설계·DB·알고리즘·구현·검증 최종 보고서 | 완료 |
| `submission/presentation/city-roulette-final-presentation-v3.pptx` | 10장 발표 자료 | 완료·검증 |
| `docs/final-demo-guide.md` | 5~7분 대본, 실행·오류·종료 절차 | 완료 |
| `submission/db/cityroulette-demo-public-data.sql` | 회원 없는 공공 정적 데이터 | 완료·복원 검증 |
| `submission/db/README.md` | Flyway 이후 데이터 복원 절차 | 완료 |
| `dist/city-roulette-final-ready/` | 압축 전 제출용 디렉터리 | 완료 |
| 최종 ZIP | 제출용 디렉터리 확인 후 생성 | 사용자 요청에 따라 미생성 |

## 2. README와 보고서

기존 README의 초기 계획, 미구현 설명, 배포 예정 내용을 실제 완료 상태로 교체했다.
최종 README에는 프로젝트 목적, 핵심 기능, 기술 스택, 아키텍처, ERD, 추천·룰렛 알고리즘,
4개 공공데이터 공급자, 로컬 실행, 334개 테스트, 주요 화면과 제한사항을 포함한다.

최종 보고서는 다음 9개 장으로 작성했다.

1. 프로젝트 개요
2. 요구사항 분석
3. 시스템 설계
4. DB 설계와 ERD
5. 핵심 알고리즘
6. 구현 결과
7. 자동화·MySQL·GUI·시연 테스트
8. 결과 및 한계
9. 결론과 향후 개선

문서의 수치와 기능은 실제 코드, Flyway V1~V12, 기존 Phase 보고서와 QA 결과에서만
가져왔다. 시간·가격·부산 전체 버스망 등 미검증 항목은 성공으로 표현하지 않았다.

## 3. 발표 자료

- 총 10장, 16:9
- 실제 Chromium QA 스크린샷만 사용
- Malgun Gothic 글꼴
- 발표 순서: 문제 → 서비스 → 탐색 → 아키텍처 → 데이터 → DB → 알고리즘 → 화면 → 검증 → 한계
- speaker notes에 근거 문서 경로 기록
- PPTX SHA-256: `734029711F981717922A8790B2AC9E59FB347A5382E9587066C797A6EF2EF2DD`

검증 결과:

- 10장 확인
- 패키지 무결성 finding 0
- 레이아웃 finding 0, warning 0
- first-party Artifact Tool 재가져오기 성공
- 실제 스크린샷 바이트 임베드 확인
- 전체 몽타주와 10개 슬라이드를 개별 렌더링해 시각 확인

PowerPoint 데스크톱 앱에서 직접 연 검증은 수행하지 않았다. 제출 전 발표 PC의 PowerPoint
또는 LibreOffice에서 한 번 열어 글꼴 대체와 영상 출력 비율을 확인한다.

## 4. DB 재현 자료

제출 SQL은 schema dump가 아니라 Flyway V1~V12 이후에 적용하는 data-only dump다.

포함:

- places 395
- place_sources 395
- bus_stops 30
- bus_routes 1
- bus_route_stops 20

제외:

- members와 비밀번호 해시·이메일
- saved_courses·saved_course_places
- place_favorites
- trip_records와 업로드 파일
- 세션·토큰·수집 실행 로그

SQL SHA-256:
`AD308E870F464C45F911962B14F4B880465B0720B7E913ECE3B290D201172AFC`

새 임시 MySQL DB에서 Flyway 12개 마이그레이션을 적용하고 제출 덤프를 복원했다. 복원 후
395/395/30/1/20, PlaceSource FK 고아 0, 동일 공급자·외부 ID 중복 0, 사용자 관련 행 0을
확인했다. 임시 DB는 검증 후 삭제했다.

## 5. 제출 패키지 포함·제외

포함:

- Java·resource·test 소스
- Gradle Wrapper와 빌드 설정
- Flyway V1~V12
- README, 최종 보고서, 시연 가이드
- 발표 PPTX
- 비식별 DB 재현 자료
- 핵심 GUI 스크린샷
- demo 실행 스크립트

제외:

- `.git`, `.gradle`, `build`, `.runtime-demo`, `.build`
- IDE 개인 설정과 launch 파일
- 로그와 PID 파일
- API 키, DB 비밀번호, 원본·격리 DB 백업
- 실제 회원 및 로컬 시연 계정 데이터
- 브라우저 다운로드와 업로드 임시 파일

## 6. 민감정보 검사

소스와 최종 패키지에서 다음 패턴을 검사했다.

- PEM/private key header
- AWS access key 형식
- Google API key 형식
- GitHub·Slack token 형식
- password·secret·api key에 직접 할당된 값
- 제출 SQL의 members/password/email/session/token 및 사용자 테이블명

실제 비밀값은 발견되지 않았다. `.env.example`에는 변수명과 빈 placeholder만 포함한다.
문서의 `root`와 `-AllowEmptyPassword`는 준비된 로컬 복제본 실행 절차이며 실제 비밀번호를
포함하지 않는다.

## 7. 테스트 결과

- Java 17 `gradlew --no-daemon clean build`: 성공
- 테스트 334개, 실패 0, 오류 0, 건너뜀 0
- demo PowerShell 5개 구문 오류 0
- 로컬 demo URL HTTP 200 및 Chromium 핵심 흐름 검증
- Spring Boot·MySQL 재시작 후 데이터 보존
- PNG 업로드·다운로드·권한·SHA-256 일치
- 제출용 DB 새 V12 스키마 복원 성공
- ZIP central directory 검사 및 압축 해제 무결성 검사 성공

## 8. 압축 전 제출 폴더 결과

- 폴더: `dist/city-roulette-final-ready/`
- 포함 파일 수: 405
- 전체 파일 크기: 5,230,618 bytes
- PPTX: 10장, SHA-256 `734029711F981717922A8790B2AC9E59FB347A5382E9587066C797A6EF2EF2DD`
- DB SQL SHA-256: `AD308E870F464C45F911962B14F4B880465B0720B7E913ECE3B290D201172AFC`
- `.git`, 빌드 결과, Gradle 캐시, 런타임 DB, 업로드 파일: 0건
- 비밀키 형식 탐지: 0건
- 사용자 테이블 INSERT: 0건
- ZIP: 사용자 요청에 따라 아직 생성하지 않음

## 9. 남은 수동 작업

- 교수님 양식에 학번·이름·과목명·제출일 입력
- PPT 표지에 발표자 정보가 필요하면 직접 추가
- 발표 PC PowerPoint에서 PPTX 최종 열기
- 발표 PC의 Java 17·MySQL 8 경로 확인
- 제출 사이트의 허용 ZIP 크기와 파일명 규칙 확인
- 공개 저장소 제출 시 공공 이미지와 데이터의 이용 조건 재확인

현재 소스, 최종 보고서, 10장 발표 자료, 시연 가이드와 비식별 DB 재현 자료가 하나의
압축 전 제출 폴더로 준비되어 있다. 위 수동 항목을 확인한 뒤 이 폴더만 ZIP으로 압축하면
최종 제출할 수 있다.
