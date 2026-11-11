# City Roulette Phase 2-A — 부산 관광 장소 공공데이터 API 조사

- 조사일: 2026-10-27
- 범위: 공식 문서 조사, 인증키 없는 연결 확인, 통합 데이터 모델과 수집기 설계
- 제외: 운영 DB 변경, API 키 발급, 인증된 전체 수집, Place Entity 변경

## 1. 결론

권장 데이터 전략은 다음과 같다.

1. **기본 장소 DB:** 한국관광공사 `KorService2`를 중심으로 관광지·문화시설·음식점·쇼핑·행사·숙박을 부산 법정동 코드로 수집한다.
2. **부산 특화 보강:** 부산 푸디투어, 갈맷길, 부산명소, 전시 API를 별도 원천으로 수집한다.
3. **실시간 검색 보강:** 카카오 로컬 API는 사용자 검색 시 후보와 최신 위치 정보를 보강하는 용도로 사용한다.
4. **카카오 영구 저장 보류:** 공식 개발 문서에서 장소 검색 결과의 장기 저장·재배포 허용 범위를 명확히 확인하기 전에는 카카오 결과를 영구 장소 DB의 원천으로 사용하지 않는다.
5. **원천과 정규화 분리:** `places`는 서비스가 사용하는 대표 장소, `place_sources`는 제공기관별 원본 식별자·동기화·권리 정보를 저장한다.

부산시 API는 지역 특화 설명과 이미지가 좋지만 데이터셋별 스키마가 다르고 서로 중복될 가능성이 높다. TourAPI는 전국 공통 식별자, 분류, 좌표, 이미지, 상세·변경 목록 API가 있어 중심 원천으로 더 적합하다. 카카오는 검색 품질은 높지만 전체 수집에 적합한 API가 아니며 페이지 상한과 이용정책 확인 문제가 있다.

## 2. 문서 확인과 실제 호출 확인 구분

### 공식 문서에서 확인한 내용

- 부산 푸디투어, 갈맷길, 부산 전시 API는 공공데이터포털에서 무료, JSON/XML, 활용신청 및 개발 트래픽을 확인했다.
- TourAPI는 부산 필터, 지역·위치·키워드·행사·숙박·상세·이미지·동기화 목록 등 15종 API와 약 26만 건의 전국 데이터를 제공한다고 안내한다.
- 카카오 로컬 API는 키워드·카테고리 장소 검색과 주소·좌표 변환을 제공한다.

### 실제 호출에서 확인한 내용

2026-10-27에 키를 넣지 않고 다음 네 URL을 실제 요청했다.

- `FoodieService/getFoodieKr`
- `fbusangmgtourinfo/getgmgtourinfo`
- `KorService2/areaBasedList2`
- `dapi.kakao.com/v2/local/search/keyword.json`

세 공공데이터 API는 모두 HTTP 401과 `SERVICE_KEY_IS_NULL`, 카카오는 HTTP 401과 `cannot find Authorization : KakaoAK header`를 반환했다. 따라서 엔드포인트 연결 및 인증 요구는 확인했지만, 이번 조사에서는 인증된 데이터 응답·실제 건수·누락률·중복률을 검증하지 못했다.

저장소의 기존 문서에는 과거 TourAPI 인증 호출에서 부산 관광지 응답을 받았다는 기록이 있으나, 이번 조사 시점의 키와 응답을 재검증한 것은 아니다. 운영 판단은 새 키로 표본 호출한 뒤 확정해야 한다.

## 3. API 비교 요약

| API | 주 용도 | 부산 필터 | 좌표 | 이미지/설명 | 전체 수집 | 개발 한도 | 영구 DB 후보 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 부산 푸디투어 | 부산 미식 콘텐츠 | 부산 전용 | 문서 제목상 제공, 실제 필드 재확인 필요 | 이미지·상세 설명 | `totalCount` 페이지 순회 | 10,000 | 적합 |
| 부산 갈맷길 | 코스 주변 명소·맛집·숙박 | 부산 전용 | 명소에 위·경도 | 이미지·소개 | `totalCount` 페이지 순회 | 10,000 | 적합 |
| 한국관광공사 TourAPI | 통합 관광 장소 기본 DB | 법정동 부산 코드 | WGS84 | 대표/추가 이미지·상세 | 페이지 및 동기화 목록 | 1,000 | 가장 적합 |
| 카카오 로컬 | 실시간 장소 검색·좌표 보강 | 좌표/반경/사각형/검색어 | 제공 | 설명·이미지 없음, 장소 URL 제공 | 최대 45페이지라 전체 수집 부적합 | 장소 검색 100,000/일 | 정책 확인 전 보류 |

한도는 조사일 기준 공식 페이지 표시값이며 변경될 수 있다. 공공데이터 운영계정은 활용사례를 등록해 증설 신청할 수 있다. 카카오 무료 쿼터는 개발자 계정에서 처음 활성화한 카카오맵 앱에 적용된다는 조건이 있다.

## 4. 부산광역시 부산푸디투어정보 서비스

공식 문서: <https://www.data.go.kr/data/15063507/openapi.do>

### 엔드포인트와 인증

```text
GET https://apis.data.go.kr/6260000/FoodieService/getFoodieKr
```

- 공공데이터포털에서 활용신청 후 `ServiceKey`를 쿼리 파라미터로 전달한다.
- 개발단계 자동승인, 운영단계 심의승인이다.
- 무료, 개발계정 10,000건으로 표시되어 있다.

### 요청

| 파라미터 | 필수 | 설명 |
| --- | --- | --- |
| `ServiceKey` | O | 공공데이터포털 인증키 |
| `pageNo` | O | 1부터 시작하는 페이지 |
| `numOfRows` | O | 페이지 크기 |
| `resultType` | X | JSON 사용 시 `json` |
| `UC_SEQ` | X | 개별 콘텐츠 ID |

### 응답과 적합성

공식 명세는 `resultCode`, `resultMsg`, `numOfRows`, `pageNo`, `totalCount`와 다음 콘텐츠 필드를 안내한다.

- `UC_SEQ`: 원천 ID
- `MAIN_TITLE`, `PLACE`, `TITLE`, `SUBTITLE`: 장소·콘텐츠 제목
- `MAIN_IMG_NORMAL`, `MAIN_IMG_THUMB`: 이미지 경로
- `ITEMCNTNTS`: 상세 설명

공식 페이지는 이름·제목·좌표·상세·이미지 조회 기능으로 설명하지만, 현재 공개 표에서 좌표 필드명은 확인되지 않았다. 인증 응답으로 좌표 이름과 결측률을 재확인해야 한다. 부산 전용이므로 별도 지역 필터는 필요 없다.

### 품질과 중복 예상

- 개별 식당보다는 먹자골목·테마 미식 콘텐츠가 섞일 수 있다.
- `MAIN_TITLE`, `PLACE`, `TITLE` 중 무엇을 정규화 장소명으로 사용할지 표본 검증이 필요하다.
- TourAPI 음식점이나 카카오 음식점과 중복 가능성이 높다.
- `(provider=BUSAN_FOODIE, externalId=UC_SEQ)`만 자동 동일 원천으로 판단한다.

### 이용조건

포털은 이용허락범위 제한 없음으로 표시하지만 이미지 원저작권과 출처 표시는 별도로 확인하는 편이 안전하다. 원본 이미지 URL과 출처를 유지하고 이미지 파일을 임의 복제하지 않는 방식을 우선한다.

## 5. 부산광역시 갈맷길 관광 정보 안내

공식 문서: <https://www.data.go.kr/data/15077606/openapi.do>

이 서비스는 명소·맛집·숙박의 세 오퍼레이션으로 구성된다. 공개 페이지에서 확인한 명소 엔드포인트는 다음과 같다.

```text
GET https://apis.data.go.kr/6260000/fbusangmgtourinfo/getgmgtourinfo
```

### 요청과 페이지 처리

- `ServiceKey`, `numOfRows`, `pageNo` 필수
- `resultType=json` 선택
- `totalCount`를 이용해 전체 페이지 순회 가능
- 무료, 개발·운영 자동승인, 개발 트래픽 10,000건 표시

### 명소 응답

- `uc_seq`: 원천 ID
- `cate1_nm`: 종류
- `place`, `title`: 장소 및 제목
- `main_img_n`, `main_img_t`: 이미지와 썸네일 경로
- `itemcntnts`: 소개
- `course`, `gugan`: 갈맷길 코스·구간
- `lat`, `lng`: 위도·경도

이미지 값이 상대경로이면 공식 안내에 따라 `https://www.visitbusan.net/`을 앞에 붙인다. 맛집과 숙박은 이름, 주소, 전화번호, 이용시간, 썸네일, 소개 또는 홈페이지를 제공한다고 안내되지만 각 오퍼레이션의 정확한 필드와 엔드포인트는 활용가이드 및 인증 표본으로 다시 확인한다.

갈맷길과 물리적으로 관련된 장소라는 강한 특성이 있어 추천 태그에 유용하다. 다만 일반 관광지 DB의 대표 레코드에 `course`를 직접 넣기보다 `place_sources.source_metadata` 또는 별도 `place_tags`로 보존한다.

## 6. 한국관광공사 국문 관광정보 서비스

공식 문서: <https://www.data.go.kr/data/15101578/openapi.do>

### 엔드포인트

```text
Base URL: https://apis.data.go.kr/B551011/KorService2

GET /areaCode2
GET /categoryCode2
GET /areaBasedList2
GET /locationBasedList2
GET /searchKeyword2
GET /searchFestival2
GET /searchStay2
GET /detailCommon2
GET /detailIntro2
GET /detailInfo2
GET /detailImage2
GET /areaBasedSyncList2
GET /lclsSystmCode2
GET /ldongCode2
```

공식 설명에는 반려동물 동반여행정보를 포함해 총 15종이 안내되어 있다.

### 인증과 공통 요청

- 공공데이터포털 활용신청 후 `serviceKey` 사용
- `MobileOS`, `MobileApp` 필수
- `_type=json`, `pageNo`, `numOfRows` 사용
- 무료, 실시간 갱신 표기
- 개발 자동승인, 운영 심의승인
- 개발 트래픽 1,000건 표시

### 부산 및 유형 필터

현행 서비스는 법정동 코드 `lDongRegnCd`/`lDongSignguCd`와 신분류 `lclsSystm1~3` 사용을 우선 검토한다. 기존 구현의 부산 코드는 `26`이며, 배포 전 `ldongCode2` 응답으로 다시 확인한다.

관광타입의 대표적인 `contentTypeId`는 다음처럼 활용할 수 있다.

| 값 | 유형 |
| --- | --- |
| 12 | 관광지 |
| 14 | 문화시설 |
| 15 | 행사·공연·축제 |
| 25 | 여행코스 |
| 28 | 레포츠 |
| 32 | 숙박 |
| 38 | 쇼핑 |
| 39 | 음식점 |

### 주요 응답

- `contentid`: 원천 식별자
- `title`, `addr1`, `addr2`: 명칭과 주소
- `mapx`, `mapy`: WGS84 경도·위도
- `contenttypeid`, `lclsSystm1~3`: 유형·분류
- `firstimage`, `firstimage2`: 대표 이미지
- `modifiedtime`: 원천 수정 시각
- `cpyrhtDivCd`: 이미지 저작권 구분
- `overview`: `detailCommon2` 상세 설명
- `originimgurl`, `smallimageurl`: `detailImage2` 추가 이미지

목록, 상세, 이미지 호출을 모든 레코드에 무조건 수행하면 호출량이 급증한다. 먼저 목록을 저장하고, 실제 추천 노출 후보나 상세 화면이 필요한 장소만 상세·이미지를 보강한다. 이후 `areaBasedSyncList2`로 변경분 수집을 검토한다.

### 이용조건

포털은 이용허락 제한 없음으로 표시하지만 사진은 공공누리 1유형과 3유형이 섞여 있고, 일반 정서에 반하는 용도와 기업 CI·BI 이용 금지 안내가 있다. `cpyrhtDivCd`, 제공기관, 원문 URL, 출처표시 문구를 이미지별로 보존해야 한다. 변경금지 유형은 리사이징·크롭 같은 가공도 별도 검토한다.

## 7. 카카오 로컬 API

공식 문서: <https://developers.kakao.com/docs/ko/local/dev-guide>, <https://developers.kakao.com/docs/ko/getting-started/quota>

### 엔드포인트와 인증

```text
GET https://dapi.kakao.com/v2/local/search/keyword.json
GET https://dapi.kakao.com/v2/local/search/category.json
```

헤더:

```text
Authorization: KakaoAK ${REST_API_KEY}
```

키는 카카오디벨로퍼스 앱을 생성하고 발급된 REST API 키를 사용한다.

### 검색 요청

키워드 검색은 `query` 필수이며 `category_group_code`, `x`, `y`, `radius`, `rect`, `page`, `size`, `sort`를 지원한다. 카테고리 검색은 `category_group_code`가 필수다.

City Roulette에 유용한 그룹 코드는 `CT1` 문화시설, `AT4` 관광명소, `AD5` 숙박, `FD6` 음식점, `CE7` 카페다. 반경은 최대 20km, 페이지는 최대 45, 키워드 검색 페이지 크기는 최대 15다. 따라서 카테고리별 부산 전체를 누락 없이 덤프하는 용도로는 적합하지 않다.

응답은 `meta.total_count`, `pageable_count`, `is_end`와 다음 장소 정보를 제공한다.

- `id`, `place_name`
- `category_name`, `category_group_code`, `category_group_name`
- `phone`
- `address_name`, `road_address_name`
- `x`, `y`
- `place_url`
- 좌표 요청 시 `distance`

이미지와 긴 설명은 로컬 장소 검색 응답에 없다.

### 호출 한도와 저장 정책

공식 쿼터 문서의 조사일 표시값은 키워드 장소 검색과 카테고리 장소 검색 각각 일 100,000건이다. 첫 번째로 카카오맵을 활성화한 앱에 무료 쿼터가 적용되며 이후 앱 또는 초과분은 유료 설정이 필요할 수 있다.

공식 개발 가이드와 공개 운영정책 검색만으로 장소 결과를 별도 DB에 장기간 축적·재배포하는 권리를 확정할 수 없었다. 따라서 다음 원칙을 적용한다.

- 사용자 요청 시 실시간 호출한 결과를 짧게 캐시하는 용도로 우선 사용한다.
- 카카오 `id`를 공공데이터 원천의 영구 식별자로 간주하지 않는다.
- 카카오 응답을 대량 수집해 독립 장소 DB로 재배포하지 않는다.
- 영구 저장이 필요하면 카카오에 서면 확인하거나 당시 적용되는 약관·정책을 법무 관점에서 재검토한다.

## 8. 추가 부산시 후보

| 데이터 | 공식 URL | 활용 판단 |
| --- | --- | --- |
| 부산명소정보 서비스 | <https://www.data.go.kr/data/15063481/openapi.do> | `AttractionService/getAttractionKr`; 부산 명소의 이름·주소·좌표·설명·이미지 보강 후보 |
| 부산해양체험정보 | <https://www.data.go.kr/data/15063564/openapi.do> | 해양 체험 코스, 좌표, 사진, 리뷰를 제공하므로 테마 추천에 적합 |
| 부산 전시 목록 | <https://www.data.go.kr/data/15063737/openapi.do> | `BusanCultureExhibitService/getBusanCultureExhibit`; 기간성 문화 이벤트 원천 |
| 부산 구·군 모범음식점 | <https://www.data.go.kr/data/15034029/openapi.do> | 행정기관 지정 음식점과 대표메뉴 보강, 좌표 유무 재확인 필요 |
| 비짓부산패스 관광지·일정 파일 | <https://www.data.go.kr/data/15160440/fileData.do> | 소규모 관광지·일정 데이터. 파일 원천과 API 원천을 같은 수집 인터페이스로 처리 가능 |

전시는 영구 장소와 기간성 이벤트를 분리해야 한다. 전시 제목 자체를 `places`에 넣지 말고, 시설을 Place로 연결한 뒤 향후 `events`로 저장하는 방식이 적절하다.

## 9. 통합 DB 구조 초안

### places — 정규화된 대표 장소

| 필드 | 타입 예시 | 설명 |
| --- | --- | --- |
| `id` | BIGINT PK | 내부 식별자 |
| `name` | VARCHAR(255) | 대표 장소명 |
| `normalized_name` | VARCHAR(255) | 검색·중복 후보 생성용 정규화명 |
| `category` | VARCHAR(50) | 내부 표준 카테고리 |
| `address` | VARCHAR(500) | 대표 주소 |
| `normalized_address` | VARCHAR(500) | 비교용 주소 |
| `district` | VARCHAR(50) | 부산 구·군 |
| `latitude`, `longitude` | DECIMAL(10,7) | WGS84, 각각 NULL 허용 |
| `description` | TEXT | 대표 설명 |
| `image_url` | VARCHAR(1000) | 대표 이미지 URL. 권리 확인된 이미지만 |
| `indoor` | BOOLEAN NULL | 실내/실외/미확인 |
| `merge_status` | VARCHAR(20) | `ACTIVE`, `REVIEW`, `MERGED` 등 |
| `created_at`, `updated_at` | DATETIME | 내부 생성·수정 시각 |

현재 `Place`의 `source`와 `externalId`는 단일 원천 단계에는 충분하지만, 한 Place에 여러 원천을 연결하려면 아래 테이블로 분리하는 것이 낫다. 마이그레이션 전까지 기존 컬럼을 즉시 제거하지 않는다.

### place_sources — 제공기관별 원본 연결

| 필드 | 설명 |
| --- | --- |
| `id`, `place_id` | PK 및 Place FK |
| `provider` | `TOUR_API`, `BUSAN_FOODIE`, `BUSAN_GALMAET`, `BUSAN_ATTRACTION` 등 |
| `external_id` | 제공기관 원천 ID |
| `source_url` | 원문 또는 공식 상세 URL |
| `source_category` | 원천 분류명·코드 |
| `source_modified_at` | 제공기관 수정 시각 |
| `last_synced_at` | 마지막 정상 동기화 시각 |
| `raw_payload` | JSON 원본 선택 저장. 용량·보존기간 제한 필요 |
| `content_license`, `image_license` | 텍스트·이미지 이용조건 |
| `attribution` | 화면에 표시할 출처 문구 |
| `active` | 원천에서 현재 확인되는지 여부 |

`UNIQUE(provider, external_id)`를 적용한다. `place_id`는 여러 행에서 같을 수 있어 한 대표 장소가 여러 원천을 갖는다.

### 추가 권장 테이블

- `place_images`: 이미지 URL, 원천, 저작권 유형, 대표 여부, 마지막 확인 시각
- `place_tags`: 갈맷길 코스, 해양체험, 미식골목 같은 다중 태그
- `import_runs`: provider, 시작·종료, 상태, 요청·수신·삽입·갱신·건너뜀·오류 수
- `import_errors`: run ID, external ID, 단계, 안전한 오류 요약
- `place_merge_candidates`: 두 Place ID, 비교 점수, 근거, 검토 상태

## 10. 카테고리 정규화

내부 카테고리는 처음부터 너무 세분화하지 않는다.

```text
ATTRACTION
CULTURE
FOOD
CAFE
SHOPPING
ACCOMMODATION
LEISURE
EVENT_VENUE
OTHER
```

원천 코드는 `place_sources.source_category`에 그대로 보존한다. 예를 들어 TourAPI `contentTypeId=39`와 카카오 `FD6`는 내부 `FOOD`로 매핑하지만 원천 코드를 지우지 않는다. 행사 그 자체는 Place가 아니라 향후 Event로 분리한다.

## 11. 중복 판정

### 자동 연결 가능

1. 동일 `provider + externalId`
2. 기존에 사람이 확정한 원천 연결

### 병합 후보만 생성

다음 조건을 조합해 점수를 계산하되 자동 병합하지 않는다.

- 공백·특수문자·법인 표기 등을 정리한 장소명 유사도
- 도로명/지번 표기와 상세주소를 정리한 주소 일치
- 좌표 간 거리: 예를 들어 30m 이내 강한 후보, 100m 이내 약한 후보
- 전화번호, 홈페이지 도메인, 동일 구·군
- 카테고리 호환성

이름만 같은 체인점, 같은 건물의 여러 시설, 골목과 개별 식당은 서로 다른 장소일 수 있다. 이름·좌표가 비슷해도 확신이 없으면 `place_merge_candidates`에 두고 관리자 검토 대상으로 남긴다.

## 12. 데이터 수집 프로그램 설계

```text
수동/스케줄 실행
  → ImportRun 생성
  → ProviderClient로 페이지 요청
  → Raw DTO 파싱
  → 필수 필드·응답 결과코드 검증
  → 원천별 Mapper로 표준 후보 생성
  → 카테고리·주소·좌표 정규화
  → provider + externalId upsert
  → 기존 Place 또는 병합 후보 탐색
  → 확실한 경우 연결, 불확실하면 새 Place/검토 후보 생성
  → 이미지·라이선스·출처 저장
  → ImportRun 집계 및 오류 기록
```

### 실행 원칙

- 초기에는 관리자 수동 실행을 기본으로 한다.
- 재실행해도 같은 `(provider, externalId)`가 중복 삽입되지 않도록 멱등하게 만든다.
- 페이지 하나를 작은 트랜잭션으로 처리해 전체 수집 실패 시 기존 데이터를 보존한다.
- timeout, 429, 5xx만 제한 횟수로 지수 백오프 재시도한다.
- 인증 실패, 잘못된 파라미터, 파싱 오류는 무한 재시도하지 않는다.
- 수집 중 보이지 않은 레코드를 즉시 삭제하지 않는다. 연속된 정상 전체 수집에서도 사라진 경우 비활성 후보로 표시한다.
- API 키가 포함된 URL, 전체 응답, 개인정보 가능 필드는 로그에 남기지 않는다.
- 원본 JSON은 필요할 때만 보존하고 최대 크기와 보존기간을 설정한다.

### 수집 순서

1. TourAPI 부산 관광지·문화시설을 소량 표본 수집
2. 좌표·주소·분류·이미지 누락률 측정
3. TourAPI 음식점·쇼핑·숙박 확대
4. 부산 푸디·갈맷길·명소로 설명과 테마 보강
5. 전시·행사는 Event 모델을 만든 뒤 수집
6. 카카오는 사용자 검색 시 실시간 보강만 적용

## 13. 다음 구현 전 확인 체크리스트

- 각 공공데이터 서비스 활용신청과 실제 일일·초당 한도
- 부산 법정동 코드와 각 API의 실제 JSON 중첩 구조
- 푸디투어 좌표 필드명 및 좌표 누락률
- 갈맷길 맛집·숙박 오퍼레이션의 정확한 엔드포인트
- TourAPI 신규 법정동/신분류 코드 적용 여부
- 이미지 URL 응답 상태와 `cpyrhtDivCd`별 표시·가공 규칙
- 같은 장소 50~100건 표본의 원천 간 중복률
- 주소 없는 장소, 좌표가 부산 밖인 장소, 폐업 장소 처리
- 카카오 결과 장기 저장이 필요할 경우 공식 이용 허용 범위 재확인

## 14. Phase 2-B 권장 범위

다음 단계에서는 모든 원천을 한 번에 구현하지 않는다.

1. `PlaceSource`, `ImportRun` 스키마와 Flyway migration
2. TourAPI `areaBasedList2` 수집기를 기존 `TourismApiClient` 기반으로 보완
3. 1페이지 표본 수집과 검증 리포트
4. `(provider, externalId)` upsert 테스트
5. 좌표·필수 필드 검증과 실패 집계
6. 그 이후 부산 푸디 또는 갈맷길 어댑터 한 개 추가

이 순서가 호출량, 중복, 저작권, 실패 복구 문제를 가장 작은 범위에서 검증할 수 있다.

## 공식 출처

- [부산광역시 부산푸디투어정보 서비스](https://www.data.go.kr/data/15063507/openapi.do)
- [부산광역시 갈맷길 관광 정보 안내](https://www.data.go.kr/data/15077606/openapi.do)
- [한국관광공사 국문 관광정보 서비스](https://www.data.go.kr/data/15101578/openapi.do)
- [카카오 로컬 REST API](https://developers.kakao.com/docs/ko/local/dev-guide)
- [카카오 API 쿼터](https://developers.kakao.com/docs/ko/getting-started/quota)
- [부산광역시 부산해양체험정보](https://www.data.go.kr/data/15063564/openapi.do)
- [부산광역시 전시 목록 서비스](https://www.data.go.kr/data/15063737/openapi.do)
- [부산광역시 구·군 모범음식점 현황](https://www.data.go.kr/data/15034029/openapi.do)
