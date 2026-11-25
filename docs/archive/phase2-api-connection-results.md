# City Roulette Phase 2 — 외부 API 연결 검증 결과

- 검증일: 2026-10-28 (Asia/Seoul)
- 실행 방식: STS launch 설정의 API 환경변수 여섯 개를 값 출력 없이 H2 테스트 프로필에 전달
- 호출 범위: 공급자별 첫 페이지 1건
- 운영 DB: 연결하지 않음
- 원본 응답 및 인증키: 저장·출력하지 않음

## 1. 결론

여섯 API 모두 인증, HTTP 상태, 공급자 결과 코드와 표본 응답 파싱에 성공했다.

| 공급자 | 환경변수 인식 | HTTP | 결과 코드 | 상태 | 전체 건수 | 표본 파싱 |
| --- | --- | ---: | --- | --- | ---: | --- |
| TourAPI | O | 200 | `0000` | `CONNECTED` | 2,226 | 성공 |
| 부산명소 | O | 200 | `00` | `CONNECTED` | 213 | 성공 |
| 부산맛집 | O | 200 | `00` | `CONNECTED` | 471 | 성공 |
| 부산푸디투어 | O | 200 | `00` | `CONNECTED` | 95 | 성공 |
| 부산갈맷길 | O | 200 | `00` | `CONNECTED` | 85 | 성공 |
| 부산버스 정류소 | O | 200 | `00` | `CONNECTED` | 8,800 | 성공 |

건수는 검증 시점 응답의 `totalCount`이며 향후 달라질 수 있다. 전체 데이터를 수집한 결과가 아니다.

## 2. 실행 환경 구분

Codex 터미널 프로세스에는 여섯 API 환경변수가 없었지만, STS의 현재 프로젝트 launch 설정에는 여섯 변수명이
모두 등록되어 있었다. STS와 터미널의 프로세스 환경은 서로 상속되지 않으므로 이를 설정 실패로 판단하지 않았다.

실제 검증은 launch 파일에서 허용된 여섯 변수만 자식 프로세스 환경에 전달했다. 키 값은 명령 인수, 로그, 문서,
테스트 결과에 출력하지 않았다. 실제 MySQL 설정은 전달하지 않고 `test` 프로필의 H2 메모리 DB를 사용했다.

## 3. 사용한 오퍼레이션

| 공급자 | 오퍼레이션 | 응답 형식 |
| --- | --- | --- |
| TourAPI | `KorService2/areaBasedList2`, `lDongRegnCd=26` | JSON |
| 부산명소 | `AttractionService/getAttractionKr` | JSON |
| 부산맛집 | `FoodService/getFoodKr` | JSON |
| 부산푸디투어 | `FoodieService/getFoodieKr` | JSON |
| 부산갈맷길 | `fbusangmgtourinfo/getgmgtourinfo` | JSON |
| 부산버스 | `BusanBIMS/busStopList` | XML |

공식 명세: [TourAPI](https://www.data.go.kr/data/15101578/openapi.do),
[부산명소](https://www.data.go.kr/data/15063481/openapi.do),
[부산맛집](https://www.data.go.kr/data/15063472/openapi.do),
[부산푸디](https://www.data.go.kr/data/15063507/openapi.do),
[갈맷길](https://www.data.go.kr/data/15077606/openapi.do),
[부산버스](https://www.data.go.kr/data/15092750/openapi.do).

## 4. 실제 표본 응답 필드

아래 목록은 각 첫 번째 표본에서 실제 존재한 필드다. 다른 행에는 일부 필드가 없을 수 있다.

### TourAPI

```text
contentid, contenttypeid, title, addr1, addr2, mapx, mapy,
firstimage, firstimage2, cpyrhtDivCd, lDongRegnCd, lDongSignguCd,
lclsSystm1, lclsSystm2, lclsSystm3, tel, zipcode,
createdtime, modifiedtime
```

목록 응답에는 긴 상세 설명이 없었다. 설명은 향후 `detailCommon2`를 선택적으로 호출할 때만 보강한다.

### 부산명소

```text
UC_SEQ, MAIN_TITLE, TITLE, SUBTITLE, PLACE, ADDR1, GUGUN_NM,
LAT, LNG, CNTCT_TEL, HOMEPAGE_URL, TRFC_INFO,
USAGE_DAY, USAGE_AMOUNT, USAGE_DAY_WEEK_AND_TIME, HLDY_INFO,
MAIN_IMG_NORMAL, MAIN_IMG_THUMB, ITEMCNTNTS, MIDDLE_SIZE_RM1
```

### 부산맛집

```text
UC_SEQ, MAIN_TITLE, TITLE, SUBTITLE, PLACE, ADDR1, ADDR2,
GUGUN_NM, LAT, LNG, CNTCT_TEL, HOMEPAGE_URL,
USAGE_DAY_WEEK_AND_TIME, RPRSNTV_MENU,
MAIN_IMG_NORMAL, MAIN_IMG_THUMB, ITEMCNTNTS
```

### 부산푸디투어

```text
UC_SEQ, MAIN_TITLE, PLACE, TITLE, SUBTITLE,
MAIN_IMG_NORMAL, MAIN_IMG_THUMB, ITEMCNTNTS
```

표본에는 주소, 행정구역, 좌표, 전화번호가 없었다. `PLACE`, `TITLE`, `SUBTITLE`과 설명을 가진 미식 테마 콘텐츠로
보며 개별 음식점이라고 자동 판단하지 않는다. 실제 개별 장소 연결은 콘텐츠 설명 분석이나 부산맛집 원천 연결을
별도 검토해야 한다.

### 부산갈맷길

```text
uc_seq, name, title, place, cate1_nm, lat, lng,
course, gugan, main_img_n, main_img_t, itemcntnts
```

`course`와 `gugan`은 장소의 대표 주소가 아니라 갈맷길 코스·구간 관계다. Place 본문에 합치지 않고 원천 메타데이터
또는 향후 코스 관계 테이블에 보존한다.

### 부산버스 정류소

```text
bstopid, bstopnm, arsno, gpsx, gpsy, stoptype
```

버스 정류소는 관광 장소가 아니며 `places`에 넣지 않는다. `gpsx`는 경도, `gpsy`는 위도 후보지만 저장 구현 전
공식 명세와 다수 표본으로 좌표 범위를 다시 검증한다.

## 5. 표준 Place 매핑 초안

`—`는 실제 표본에 필드가 없었다는 뜻이며 값을 임의 생성하지 않는다.

| 표준 필드 | TourAPI | 부산명소 | 부산맛집 | 부산푸디 | 갈맷길 |
| --- | --- | --- | --- | --- | --- |
| 원천 ID | `contentid` | `UC_SEQ` | `UC_SEQ` | `UC_SEQ` | `uc_seq` |
| 이름 | `title` | `MAIN_TITLE` 우선 | `MAIN_TITLE` 우선 | 콘텐츠: `MAIN_TITLE` | `title` 또는 `place` 검증 |
| 주소 | `addr1` + `addr2` | `ADDR1` | `ADDR1` + `ADDR2` | — | — |
| 행정구역 | `lDongSignguCd` | `GUGUN_NM` | `GUGUN_NM` | — | — |
| 위도 | `mapy` 문자열→숫자 | `LAT` 문자열→숫자 | `LAT` 문자열→숫자 | — | `lat` 문자열→숫자 |
| 경도 | `mapx` 문자열→숫자 | `LNG` 문자열→숫자 | `LNG` 문자열→숫자 | — | `lng` 문자열→숫자 |
| 카테고리 | `contenttypeid`, `lclsSystm1~3` | 공급자 고정 `ATTRACTION` | 공급자 고정 `FOOD` | `FOOD_THEME` 후보 | `cate1_nm` |
| 설명 | 목록에는 없음 | `ITEMCNTNTS` | `ITEMCNTNTS` | `ITEMCNTNTS` | `itemcntnts` |
| 대표 이미지 | `firstimage` | `MAIN_IMG_NORMAL` | `MAIN_IMG_NORMAL` | `MAIN_IMG_NORMAL` | `main_img_n` |
| 전화번호 | `tel` | `CNTCT_TEL` | `CNTCT_TEL` | — | — |
| 원천 수정 시각 | `modifiedtime` | — | — | — | — |

원본 제목 보조 필드, 운영시간, 대표메뉴, 교통정보, 코스·구간은 `places`의 공통 컬럼으로 억지로 합치지 않는다.
공급자 원본 메타데이터 또는 향후 목적별 테이블에 둔다.

## 6. 기존 프로젝트 구조 분석

현재 Flyway v6 기준으로 다음 구조가 이미 있다.

- `places`: 대표 장소 및 이미지 URL
- `place_sources`: 공급자, 외부 ID, 원천 URL·카테고리·수정/동기화 시각·권리 정보
- `import_runs`: 공급자별 실행 상태와 수집 통계
- `place_merge_candidates`: 유사 장소의 수동 검토 후보
- `UNIQUE(place_sources.provider, place_sources.external_id)`

TourAPI는 이미 페이지네이션, 부산 코드 검증, dry-run과 멱등 저장 구조가 구현돼 있다. 회원, 인증, 여행 기록과
파일 업로드는 수집 구조와 직접 의존하지 않는다. 이번 작업에서는 migration이나 운영 DB를 변경하지 않았다.

## 7. DB 마이그레이션 계획

다음 migration은 별도 작업에서 실제 수집기와 함께 작성한다.

### 장소 데이터

1. 기존 `places`, `place_sources`, `import_runs`를 재사용한다.
2. 부산명소와 부산맛집은 각각 `BUSAN_ATTRACTION`, `BUSAN_RESTAURANT` provider로 수집한다.
3. 부산푸디는 기본적으로 장소가 아니라 테마 콘텐츠로 분리한다. 필요하면 `content_collections`와
   `content_collection_places`를 먼저 설계한다.
4. 갈맷길 장소는 Place가 될 수 있지만 `course`, `gugan`은 향후 `walking_courses`, `walking_course_places`
   또는 원천 JSON 메타데이터에 분리한다.
5. `source_modified_at`이 없는 공급자는 NULL로 유지하고 `last_synced_at`만 갱신한다.
6. 공급자가 다른 유사 장소는 `place_merge_candidates`에만 기록하고 자동 병합하지 않는다.
7. 원본 메타데이터 보존이 필요하면 `place_sources`에 크기 제한이 있는 JSON 컬럼, payload hash, last-seen/active
   컬럼을 추가하는 migration을 별도로 검토한다.

### 교통 데이터

관광 Place와 분리된 migration으로 다음을 계획한다.

- `bus_stops`: 내부 ID, provider, 외부 정류소 ID, ARS 번호, 정류소명, 좌표, 정류소 유형, 동기화 시각
- `bus_routes`: 내부 ID, provider, 외부 노선 ID, 노선 번호·유형과 검증된 운행 메타데이터
- `bus_route_stops`: 노선 FK, 정류소 FK, 정차 순서, `UNIQUE(route_id, stop_sequence)`

이번 표본으로 검증된 것은 정류소 필드뿐이다. 노선·노선 정류소 필드는 해당 공식 오퍼레이션을 1건 호출해 실제
응답을 확인한 뒤 컬럼을 확정한다. 버스 실시간 도착 데이터는 영구 마스터 테이블이 아니라 단기 캐시 또는 조회 시
응답으로 취급한다.

## 8. 개발 전용 진단 기능

- `GET /dev/api-status`
- `local` 또는 `dev` 프로필에서만 Controller Bean 등록
- `ROLE_ADMIN`만 접근 가능
- 비회원은 로그인으로 이동, 일반 회원은 403
- 운영/`test` 단독 프로필에서는 엔드포인트 자체가 없음
- GET 요청을 명시적으로 보낼 때만 공급자별 1건 호출
- 키, 키 일부, 원본 URL, 원본 응답 본문은 반환하지 않음

관리자 계정이 없는 개발 DB에서는 권한을 우회하지 않고 CLI 진단을 사용한다.

## 9. Mock 테스트와 실제 호출 구분

### Mock/H2 테스트

- 설정 바인딩과 키 누락
- JSON의 `body.items.item` 및 직접 `item` 래퍼
- XML 파싱
- 인증·HTTP·파싱 상태 구분
- 관리자/일반회원/비회원 접근
- local/dev 프로필과 운영 프로필 비활성화
- 기존 인증·파일 기능 회귀

### 실제 호출

여섯 공급자 모두 첫 페이지 1건만 호출했다. 실제 MySQL 연결, 데이터 INSERT/UPDATE, 전체 페이지 순회는 하지 않았다.

## 10. Phase 2-B 구현 순서

1. 부산명소와 부산맛집 raw DTO/정규화 Mapper 작성
2. 공급자별 fixture를 실제 필드 구조로 비식별 저장하고 계약 테스트 추가
3. 기존 `TourismPageImporter`를 provider adapter 방식으로 일반화
4. dry-run 10건으로 주소·좌표·이미지 누락률과 중복 후보 측정
5. 부산명소와 부산맛집만 `place_sources` 멱등 upsert 구현
6. 푸디 테마 및 갈맷길 코스 모델을 장소 수집과 분리해 설계
7. 버스 정류소/노선/노선-정류소 실제 응답을 각각 검증한 뒤 별도 migration·수집기 구현
8. 운영 전 호출 제한, 라이선스 표시, 비활성 원천 처리 정책 확정
