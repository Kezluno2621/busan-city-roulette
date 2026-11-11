# Local demo scripts

프로젝트 루트 PowerShell에서 다음 한 명령으로 실행하는 것이 가장 간단하다.

```powershell
.\scripts\demo\run-demo.ps1 -Username root -AllowEmptyPassword
```

이 명령은 시연용 MySQL 시작, 백업/데이터 검증, Spring Boot 실행을 순서대로 수행한다.
Spring Boot를 `Ctrl+C`로 종료하면 시연용 MySQL도 함께 종료한다.

문제를 단계별로 확인해야 할 때는 다음 순서로 실행한다.

```powershell
.\scripts\demo\start-demo-db.ps1
.\scripts\demo\verify-demo.ps1 -Username root -AllowEmptyPassword
.\scripts\demo\start-demo.ps1 -Username root -AllowEmptyPassword
```

Spring Boot는 `Ctrl+C`로 종료하고, MySQL은 다음 명령으로 종료한다.

```powershell
.\scripts\demo\stop-demo-db.ps1
```

이 스크립트는 `.runtime-demo/mysql-data`에 준비된 격리 MySQL 복사본만 사용한다.
다른 DB를 대상으로 실행하지 않으며, DB 비밀번호와 API 키를 파일에 저장하지 않는다.
`start-demo-db.ps1`과 `start-demo.ps1`은 포트가 이미 사용 중이면 점유 프로세스와 PID를
표시하고 실행을 중단한다. `verify-demo.ps1`은 Java 17, 백업 SHA-256, Flyway V12,
기준 데이터 건수를 확인하며 MySQL 연결 오류에 비밀번호나 연결 문자열을 노출하지 않는다.

상세 절차와 복구 방법은 `docs/phase5-c-local-demo.md`, 최종 리허설 결과와 장애 대응은
`docs/phase5-d-final-demo-rehearsal.md`를 참고한다.
