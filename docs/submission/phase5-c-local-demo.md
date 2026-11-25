# Phase 5-C 로컬 시연 환경

검증일: 2026-11-07 (Asia/Seoul)

## 1. 완료 상태

교수님 발표용 환경은 클라우드나 공개 URL 없이 다음 구성으로 실제 실행했다.

- Java: `C:\Program Files\Java\jdk-17` (`17.0.12`)
- Gradle Wrapper: 8.14.3
- Spring Boot 프로필: `demo`
- 애플리케이션: `http://127.0.0.1:18090/`
- 격리 MySQL: `127.0.0.1:33321/cityroulette_demo`
- MySQL/Flyway: MySQL 8.0.46, Flyway V12
- 원본 MySQL 및 기존 격리 DB: 접근하거나 변경하지 않음

`demo` 프로필은 Flyway, SQL 초기화, 공공데이터 수집기, 품질 분석 Runner,
버스 수집기 및 개발 진단 Runner를 모두 비활성화한다. 서버를 시작해도 DB 스키마나
관광 데이터를 자동으로 변경하지 않는다.

## 2. 시연 DB 구성

`.runtime-demo/mysql-data`는 기존 검증 DB의 별도 파일 복사본이며 Git에서 제외한다.
그 안에 발표용 논리 DB `cityroulette_demo`를 별도로 복원하고 기존 회원 관련 데이터는
제거한 후 비식별 시연 데이터만 생성했다.

| 항목 | 실제 검증 건수 |
| --- | ---: |
| places | 395 |
| place_sources | 395 |
| bus_stops | 30 |
| bus_routes | 1 |
| bus_route_stops | 20 |
| members | 1 (비식별 시연 계정) |
| place_favorites | 1 |
| saved_courses | 1 |
| saved_course_places | 3 |

시연 계정은 `demo` / `test1234`이며 이메일은 외부 발송에 사용할 수 없는
`demo@example.invalid`이다. 실제 서비스나 다른 계정에 이 비밀번호를 재사용하지 않는다.

복구용 백업:

- 파일: `.runtime-demo/backup/cityroulette-demo-sanitized.sql`
- 크기: 538,371 bytes
- SHA-256: `F14E6C7B80858E0A6F86EC05A0191E218BA0EB072CCC8A01EFFCFE424C363A1E`

백업과 `.runtime-demo` 전체는 소스 저장소에 포함하지 않고 발표 장비의 로컬 저장소나
암호화된 이동식 저장장치로 별도 보관한다.

## 3. 실행 방법

PowerShell을 프로젝트 루트에서 연다. 스크립트에는 DB 비밀번호나 API 키가 없다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo\start-demo-db.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\demo\verify-demo.ps1 -Username root -AllowEmptyPassword
powershell -ExecutionPolicy Bypass -File .\scripts\demo\start-demo.ps1 -Username root -AllowEmptyPassword
```

마지막 명령은 Java 17을 명시적으로 선택하고 안전 검증 후 Spring Boot를 전경에서
실행한다. 발표용 격리 MySQL 복사본이 빈 로컬 root 비밀번호를 사용하기 때문에 준비된
현재 환경에서만 `-AllowEmptyPassword`를 쓴다. 다른 MySQL 환경에서는 해당 옵션을 제거하고
`DEMO_DB_USERNAME`, `DEMO_DB_PASSWORD`를 프로세스 환경변수로 전달한다.

종료:

1. Spring Boot 창에서 `Ctrl+C` 후 종료 확인에 `Y` 입력
2. 다음 명령으로 이 작업이 시작한 demo MySQL PID만 종료

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo\stop-demo-db.ps1
```

MySQL을 중지해도 `.runtime-demo/mysql-data`의 저장 데이터는 유지된다.

## 4. 실행 스크립트 안전장치

- MySQL datadir가 프로젝트의 `.runtime-demo` 아래인지 절대 경로로 확인
- `auto.cnf`가 없는 미준비 디렉터리 실행 차단
- MySQL은 `127.0.0.1`에만 바인딩하고 포트 33321 사용
- PID 파일이 가리키는 프로세스 이름이 `mysqld`가 아니면 종료 거부
- `verify-demo.ps1`에서 Java 17, Flyway V12 및 기준 테이블 건수를 확인
- 기준 건수가 다르면 Spring Boot 시작 전 중단
- 기존 Gradle daemon의 오래된 DB 환경변수 재사용을 막기 위해 `--no-daemon` 사용

## 5. 외부 API 의존성

API 키 없이 DB만으로 동작하도록 실제 확인한 기능:

- 관광지 395건 목록, 검색, 상세
- 장소 DB 기반 출발지 검색
- 개인화 추천
- seed 기반 룰렛 코스 생성
- 로그인 및 세션
- 코스 저장·조회
- 즐겨찾기 저장·조회

`demo` 프로필은 Kakao Local REST 키와 ODsay 키를 비워 둔다. 출발지 검색은 Kakao 대신
저장된 `places`를 검색한다. 기존 ODsay 전용 기능을 호출하면 실제 호출 성공처럼 가장하지
않고 기존 오류 안내를 표시한다.

Leaflet JavaScript와 OpenStreetMap 타일은 인터넷 연결에 의존한다. 인터넷이 없으면 장소,
추천, 룰렛 및 저장 기능은 계속 사용할 수 있고 지도 영역에는 외부 지도 연결이 필요하다는
안내를 표시한다. 네트워크를 실제 차단한 브라우저 검증은 이번 작업에서 수행하지 못했으며,
인터넷 연결 상태에서 지도 마커 렌더링은 확인했다.

## 6. 실제 발표 시나리오 결과

인앱 Chromium에서 다음 순서로 검증했다.

1. 메인 페이지 접속
2. 여행지 목록에서 395건 및 33페이지 확인
3. `APEC` 검색 결과 2건 확인
4. APEC나루공원 상세, 실제 좌표 지도와 출처 확인
5. API 키 없이 DB 검색으로 APEC나루공원을 출발지로 선택
6. 관광지·음식점 조건 추천 10건과 추천 이유/점수 확인
7. random seed 42, 3곳 룰렛 결과 재현
8. `demo` 계정 로그인
9. APEC나루공원 즐겨찾기 등록 및 목록 확인
10. `교수님 시연 코스` 저장, 3개 장소 방문 순서와 지도 확인

검증된 seed 42 코스는 `수영 돼지국밥 → 다리집 본점 → 다온 한정식`이었다.
데이터가 바뀌지 않는 한 동일한 입력과 seed로 재현된다.

## 7. 재시작 및 데이터 보존

Spring Boot와 MySQL을 모두 종료한 뒤 같은 스크립트로 다시 시작했다.

- Java 17 + demo 프로필 재기동 성공
- 홈 `/` HTTP 200
- `/places` HTTP 200 및 395건 표시
- MySQL 재시작 후 회원 1, 즐겨찾기 1, 저장 코스 1, 코스 장소 3 유지
- 저장 전 룰렛 토큰은 메모리 기반이므로 서버 재시작 시 소멸하는 기존 정책 유지

## 8. 백업 복구

준비된 demo datadir가 손상된 경우 새 MySQL 8.0 인스턴스에서 빈 DB를 만든 뒤 백업을
복원한다. 아래 명령은 반드시 격리 인스턴스의 포트와 계정인지 먼저 확인한 뒤 실행한다.

```powershell
mysql.exe --host=127.0.0.1 --port=33321 --user=root `
  --execute="CREATE DATABASE IF NOT EXISTS cityroulette_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

mysql.exe --host=127.0.0.1 --port=33321 --user=root --database=cityroulette_demo `
  --execute="SOURCE C:/Projects/busan-city-roulette/.runtime-demo/backup/cityroulette-demo-sanitized.sql"
```

복원 후에는 반드시 `verify-demo.ps1`를 실행한다. 원본 개발 DB에는 이 백업을 복원하지 않는다.

## 9. 테스트 결과

- `node --check`: 추천, 룰렛, 저장 코스 지도 JavaScript 3개 통과
- `gradlew --no-daemon clean build`: 성공
- Java 테스트: 324개, 실패 0개
- MySQL 실제 연결 및 재시작: 성공
- GUI 핵심 시나리오: 성공

미검증 항목:

- 네트워크 어댑터를 실제 차단한 완전 오프라인 GUI 테스트
- 발표 장비가 현재 PC와 다른 경우의 하드웨어별 실행 검증
- 유효한 Kakao/ODsay 키를 사용한 실제 외부 호출
- demo 프로필에서 파일 업로드·다운로드의 수동 브라우저 재검증

파일 기능은 기존 Phase 5-A GUI 검증과 이번 324개 회귀 테스트 범위에서는 통과했지만,
이번 Phase 5-C 브라우저 시나리오에서는 다시 업로드하지 않았다.

## 10. 발표 자료용 증거

민감정보가 없는 기존 GUI QA 캡처를 발표 자료에 재사용할 수 있다.

- 메인/목록/상세/추천/룰렛/즐겨찾기/저장 코스: `docs/testing/qa/screenshots/`
- Phase 5-B 반응형 및 오류 상태: `docs/testing/qa/phase5-b/`
- DB 관계와 수집·정규화 흐름: `docs/archive/phase2-data-import.md`,
  `docs/archive/phase4-c3-tourism-data-scale-up.md`
- 자동 테스트 증거: `build/reports/tests/test/index.html` (clean build 후 생성)

발표 직전 새 캡처를 만들 때는 환경변수 설정 창, 콘솔의 연결 문자열, 실제 회원 정보가
화면에 들어가지 않도록 한다.

## 11. 발표 직전 체크리스트

- 전원 연결 및 절전 모드 해제
- Java 17 및 MySQL 8.0 경로 확인
- `verify-demo.ps1` 성공 확인
- `http://127.0.0.1:18090/` 접속 확인
- `demo` 계정 로그인 확인
- 유선/무선 인터넷 연결 확인(지도 타일용)
- 백업 SQL과 SHA-256을 별도 저장장치에도 보관
- 발표 중에는 공공데이터 수집 Runner를 실행하지 않음
- 브라우저 자동 완성이나 환경변수 화면에 민감정보가 보이지 않도록 정리

