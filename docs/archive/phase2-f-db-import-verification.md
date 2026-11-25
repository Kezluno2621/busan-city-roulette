# City Roulette Phase 2-F — MySQL 마이그레이션 및 버스 표본 적재 검증

검증일: 2026-10-29 (Asia/Seoul)

## 결론

원본 STS 연결 DB는 이름에 `dev/test/local` 표식이 없고 STS launch 설정에도
`dev` profile, expected database, backup-confirmed 설정이 없다. 따라서 원본 DB가
개발 전용이라고 객관적으로 확인할 수 없어 원본에는 V5~V9 및 버스 데이터를
적용하지 않았다.

대신 원본의 읽기 전용 백업을 별도의 격리 MySQL 인스턴스에 복원한 뒤, 그
사본에서 V5~V9, Dry-run, 1차 저장, 동일 조건 재수집 및 데이터 무결성을
검증했다. 아래의 v9 및 버스 적재 결과는 모두 **격리 사본의 결과**다.

## 상태 구분

| 항목 | 상태 | 결과 |
| --- | --- | --- |
| 원본 MySQL 읽기 연결 | 완료 | MySQL 8.0.46, Flyway v4 |
| 원본 DB의 개발 전용 여부 | 미검증 | 이름/STS 설정에 개발 환경 표식 없음 |
| 백업 생성 | 완료 | 전체 스키마와 데이터 덤프 성공 |
| 격리 복원 | 완료 | 4개 테이블, Place 10건, Flyway v4 복원 |
| V5~V9 SQL 검토 | 완료 | 삭제/초기화 문장 없음, 적용 순서 및 FK 확인 |
| V5~V9 적용 | 부분 완료 | 격리 사본 성공, 원본 미적용 |
| 버스 Dry-run | 완료 | 정류장 10건, 100번 노선 1건, 정차 20건 |
| 버스 저장/재수집 | 완료 | 격리 사본에서 멱등성 확인 |
| 원본 DB 버스 적재 | 미검증 | 개발 DB 확인 전 쓰기 금지로 미수행 |
| 정차 순서 비교 | 완료 | 원본 API 상위 20건과 DB 순서 일치 |
| 전체 부산 노선망 | 미완료 | 작업 범위에서 제외 |

## STS 실행 환경 확인

두 개의 City Roulette STS launch configuration을 확인했다.

- DB 연결 변수는 존재한다.
- 부산버스 API 키는 주 실행 설정에 존재한다.
- 키와 DB 자격증명 값은 출력하지 않았다.
- `SPRING_PROFILES_ACTIVE`, `RUNTIME_EXPECTED_DATABASE`,
  `RUNTIME_BACKUP_CONFIRMED`, `RUNTIME_ALLOW_MIGRATIONS`는 설정되어 있지 않다.
- DB 이름은 개발 환경임을 나타내는 표식을 갖고 있지 않다.

따라서 `RuntimeVerificationRunner`의 원본 DB 쓰기 조건은 충족되지 않는다.

## 백업과 복원 검증

`mysqldump --single-transaction` 방식으로 스키마, 데이터, trigger, routine 및
event를 포함한 백업을 만들었다.

- 파일: `.runtime-backups/phase2f-pre-migration-20261029-111022.sql.bak`
- 크기: 8,382 bytes
- SHA-256: `A888420A9FDCFDB0F959DBB58C021F791BB5EB710B63104EFC8E7A3BD7D2BAF2`
- 포함 확인: `places`, `members`, `trip_records`, `flyway_schema_history`
- Git 제외: `.runtime-backups/`를 `.gitignore`에 추가

별도 포트의 격리 MySQL 8.0.46에 백업을 복원했다. 복원 후 결과는 다음과 같다.

| 항목 | 복원 결과 |
| --- | ---: |
| 테이블 | 4 |
| Place | 10 |
| Member | 1 |
| TripRecord | 0 |
| Flyway | v4 |

격리 서버는 각 검증 종료 후 정상 종료했다. 백업에는 실제 사용자 데이터가
포함되므로 Git에 추가하거나 외부로 전송해서는 안 된다.

## V5~V9 검토 및 격리 적용

- V5: `places` 검색 필드, 출처, 시간 필드 및 일반 인덱스 추가
- V6: `place_sources`, `import_runs`, `place_merge_candidates` 생성 및 기존
  TourAPI 원천 연결
- V7: 정규화 필드와 source 활성 상태 추가
- V8: 정류장, 노선, 정차 순서 테이블과 FK/UNIQUE 생성
- V9: 정류장 위도·경도 복합 인덱스 생성

테이블 삭제나 기존 행 삭제 문장은 없다. Flyway migration 자체의 down script는
없으므로 실패 또는 원복 시 검증된 전체 백업으로 복원하는 것이 복구 절차다.

격리 사본 적용 결과:

| 항목 | 적용 전 | 적용 후 |
| --- | ---: | ---: |
| Flyway | v4 | v9 |
| 테이블 | 4 | 10 |
| Place | 10 | 10 |
| PlaceSource | 없음 | 10 |
| BusStop | 없음 | 0 |
| BusRoute | 없음 | 0 |
| BusRouteStop | 없음 | 0 |

V5~V9 5개 migration이 성공했다. 원본과 사본의 Place ID 및 주요 필드
`id/name/address/category/latitude/longitude/external_id` 해시는 일치했다.

## API Dry-run

`/busInfo`를 전체 노선 목록으로 취급하지 않고 입력 노선번호와 정확히 일치하는
결과만 사용했다. 검증 노선은 `100`이다.

| 대상 | Fetched | Inserted 예상 | Updated | Skipped | Failed | 정차 관계 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 정류장 | 10 | 10 | 0 | 0 | 0 | 0 |
| 100번 노선 | 1 | 1 | 0 | 0 | 0 | 20 |

좌표, 외부 정류장 ID, 외부 노선 ID 및 정차 순서 매핑이 완료됐으며 Dry-run은
DB를 변경하지 않았다.

## 격리 사본 저장 및 재수집

| 실행 | 정류장 결과 | 노선 결과 | DB BusStop | DB BusRoute | DB RouteStop |
| --- | --- | --- | ---: | ---: | ---: |
| 저장 전 | - | - | 0 | 0 | 0 |
| 1차 | 10 insert | 1 insert / 정차 20 | 30 | 1 | 20 |
| 2차 | 10 update | 1 update / 정차 20 | 30 | 1 | 20 |

BusStop 30건은 독립 정류장 표본 10건과 100번 노선의 정차 정류장 20건이다.
재수집 후 `bus_stops`, `bus_routes`, `bus_route_stops` 건수는 증가하지 않았다.

- Place: 10, 기존 ID 및 주요 필드 보존
- PlaceSource: 10
- 좌표가 유효한 BusStop: 10/30
- BusRoute: 1
- BusRouteStop: 20
- 정차 순서: 1~20, 누락 0, distinct 20
- 정차 관계가 없는 노선: 0
- ImportRun: 성공 4건(1차 정류장/노선, 2차 정류장/노선)
- 원본 API와 저장된 상위 20개 정차 순서: 일치

노선 API가 반환한 정차 정류장 20건에는 현재 매핑 가능한 좌표가 없었다.
따라서 좌표 보유 정류장 10건과 노선 관계가 연결되어 있지 않다.

## 주변 정류장 매칭률

격리 표본 기준 결과다.

| 반경 | 좌표가 있는 Place 중 매칭 비율 |
| --- | ---: |
| 300m | 0% |
| 500m | 0% |
| 1km | 10% |

이는 정류장 10건만 좌표를 보유한 부분 표본이므로 부산 전체 접근성을 나타내지
않는다. 특히 좌표가 있는 노선 연결 정류장은 0/20이므로 이 데이터만으로 실제
장소 간 버스 경로를 탐색할 수 없다.

## 원본 DB 최종 재확인

모든 격리 검증 이후 원본을 읽기 전용으로 다시 확인했다.

- Flyway: v4
- 테이블: 4
- Place: 10
- Place 주요 필드 스냅샷: 백업 및 격리 사본과 일치
- 원본 DB에 V5~V9 및 버스 데이터 쓰기 없음

## 테스트와 Phase 3-A 판단

코드 테스트 성공과 실제 원본 DB 적용 성공은 별개다. Gradle 전체 테스트 결과는
아래에 기록하며, 격리 MySQL에서는 실제 migration, API Dry-run, 저장, 재수집 및
순서 비교가 성공했다.

- Java 17 / Gradle 8.14.3 `clean build` 성공
- 47개 테스트 스위트, 244개 테스트 성공
- 실패 0, 오류 0, 건너뜀 0
- 기존 인증, 파일, 관광지 및 버스 단위 테스트 포함
- `git diff --check` 오류 없음(기존 파일 줄바꿈 변환 경고만 존재)

Phase 3-A의 그래프/알고리즘 코드는 fixture로 개발할 수 있다. 그러나 실제 부산
데이터 기반 경로 탐색 진입 조건은 아직 충족되지 않았다.

남은 조건:

1. 원본 DB가 운영이 아닌 개발 전용이라는 사용자 확인 또는 명확한 환경 표식
2. 원본 STS 설정에 `dev`, expected database, backup-confirmed를 명시
3. 확인된 개발 DB에 V5~V9 적용 및 같은 검증 반복
4. 100번 노선 정차 정류장의 좌표를 정류장 상세/목록 데이터와 external ID로 보강
5. 좌표가 있는 Place → 노선 연결 정류장 → 정차 순서 조회 가능 여부 재검증

