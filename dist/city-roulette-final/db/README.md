# 비식별 DB 재현 자료

`cityroulette-demo-public-data.sql`은 검증된 demo MySQL에서 다음 공공 정적 데이터만
내보낸 data-only 덤프입니다.

- `places`: 395
- `place_sources`: 395
- `bus_stops`: 30
- `bus_routes`: 1
- `bus_route_stops`: 20

회원, 비밀번호 해시, 이메일, 세션, 저장 코스, 즐겨찾기, 여행기록, 업로드 파일,
수집 실행 로그는 포함하지 않습니다.

## 복원 순서

1. 빈 MySQL 8 데이터베이스를 생성한다.
2. 프로젝트를 해당 DB에 연결하여 Flyway V1~V12를 적용한다.
3. 애플리케이션을 종료한다.
4. 아래처럼 public data 덤프를 가져온다.

```powershell
mysql.exe --host=127.0.0.1 --port=<port> --user=<user> `
  --database=<database> `
  --execute="SOURCE C:/path/cityroulette-demo-public-data.sql"
```

복원 후 기대 건수는 395 / 395 / 30 / 1 / 20입니다. `members` 등 사용자 테이블은
비어 있는 것이 정상입니다. 시연 계정은 해당 PC에서 회원가입 화면으로 새로 만드세요.

## 검증 결과

- MySQL 8.0.46의 새 임시 DB에서 Flyway 12개 마이그레이션 적용 성공
- data-only 덤프 복원 성공
- PlaceSource FK 고아 0
- 동일 `(provider, external_id)` 중복 0
- 회원·코스·즐겨찾기·여행기록 0
- SHA-256: `AD308E870F464C45F911962B14F4B880465B0720B7E913ECE3B290D201172AFC`

덤프는 원본 테이블과 새 Flyway DB의 열 순서 차이를 안전하게 처리하도록 모든 INSERT에
열 이름을 포함합니다. 데이터의 텍스트와 이미지 사용은 `place_sources`의 attribution,
content license, image license를 따릅니다.
