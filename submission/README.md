# City Roulette 제출 자료 안내

이 디렉터리는 웹 프로그래밍 기말 프로젝트 평가 기준의 제출 형식에 맞춘 원본 자료입니다.
최종 제출 ZIP에서는 이 자료와 실행 가능한 소스코드를 하나의 폴더로 묶습니다.

## 제출 항목 대응

| 평가 기준 제출 항목 | 포함 자료 |
| --- | --- |
| 개발한 웹사이트 소스 | 프로젝트 루트의 `src`, Gradle Wrapper, 빌드 설정, Flyway V1~V12 |
| DB 재현 파일 | `db/cityroulette-demo-public-data.sql`과 `db/README.md` |
| 10분 이내 발표 PPT | `presentation/city-roulette-final-presentation-v3.pptx` |
| 발표 설명 자료 | `../docs/presentation/final-presentation-script.md` |

## 발표 자료 구성

발표 자료는 총 10장입니다. 3계층 구조, 회원 기능, 파일 업로드·다운로드, DB 처리,
공공데이터 수집 프로그램, 추천 알고리즘과 검증 결과를 평가 기준에 맞춰 설명합니다.
발표 대본은 약 6분이며, 이후 3~4분 동안 실제 로컬 웹서비스를 시연할 수 있습니다.

## DB 재현 방식

1. 빈 MySQL 8 데이터베이스를 생성합니다.
2. 애플리케이션을 연결해 Flyway V1~V12를 적용합니다.
3. 애플리케이션을 종료합니다.
4. `db/cityroulette-demo-public-data.sql`을 가져옵니다.

덤프에는 장소·출처·부분 버스 정적 데이터만 포함합니다. 실제 회원, 비밀번호 해시,
이메일, 저장 코스, 즐겨찾기, 여행 기록, 세션과 API 키는 포함하지 않습니다.

## 제출 전 확인

- PPT 표지에 학번과 이름을 입력합니다.
- 발표 PC에서 PPTX를 한 번 열어 글꼴과 화면 비율을 확인합니다.
- ZIP 내부에 `.git`, `build`, `.gradle`, `.runtime-demo`, 업로드 파일이 없는지 확인합니다.
- API 키와 DB 비밀번호를 제출 파일에 직접 입력하지 않습니다.
