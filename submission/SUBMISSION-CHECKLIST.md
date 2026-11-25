# City Roulette 최종 제출 체크리스트

## ZIP 안에 반드시 있어야 하는 자료

- [x] Spring Boot 웹사이트 소스와 Gradle Wrapper
- [x] Flyway V1~V12 마이그레이션
- [x] 비식별 MySQL 데이터 재현 SQL과 복원 설명
- [x] 10장 발표 PPTX
- [x] 최종 프로젝트 보고서
- [x] 발표 대본과 로컬 시연 가이드
- [x] 실제 화면 캡처

## 제출 직전에 직접 입력할 정보

- [ ] PPT 표지의 학번과 이름
- [ ] 교수님이 지정한 ZIP 파일명 규칙
- [ ] LMS 제출 마감과 업로드 용량 제한

## 발표 PC 확인

- [ ] PowerPoint에서 16:9 PPTX 열기
- [ ] Java 17과 MySQL 8 실행 확인
- [ ] `scripts/demo/run-demo.ps1` 실행 확인
- [ ] `http://127.0.0.1:18090/` 접속 확인
- [ ] 발표용 로컬 계정 준비

## 보안 확인

- [x] 실제 회원과 비밀번호 해시 제외
- [x] API 키와 DB 비밀번호 제외
- [x] `.git`, Gradle 캐시, 빌드 결과, 런타임 DB 제외
- [x] 업로드 파일과 로그 제외

제출 ZIP의 DB 파일은 Flyway가 적용된 빈 MySQL 8 DB에 가져오는 data-only SQL입니다.
MySQL 서버 자체 데이터 디렉터리나 원본 개발 DB 백업을 제출하지 않습니다.
