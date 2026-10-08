# Tourism API Research

## 1. 조사 목적

- 조사일: 2026-10-07
- 목적: City Roulette MVP의 관광지 데이터 소스, 요청 방식, Place 매핑과 저장 전략 결정.
- 사전 확인: README의 부산 관광·날씨·대중교통 활용 계획과 개발 단계, Issue 양식, 현재 Place/PlaceRepository를 읽었다. 별도 계획서는 없었다.
- 조사 당시 Place: `places` 테이블, `Long id` 자동 증가, `String name/category/address`, `Double latitude/longitude`, `boolean indoor`. `name`과 `indoor`는 NOT NULL이다.
- 이번 작업은 문서 조사이다. 인증키 발급·인증된 API 호출·실제 데이터 품질 표본 검증은 하지 않았다. 공식 제공 페이지와 공개 명세가 있다는 사실을 서비스의 실시간 정상 응답 보장으로 해석하지 않는다.
- Entity, Controller, Service, DTO, 설정, DB 데이터는 변경하지 않는다.

주요 근거는 [한국관광공사 서비스 공식 상세페이지 및 Swagger](https://www.data.go.kr/data/15101578/openapi.do)와 그 페이지의 [공식 참고문서 ZIP](https://www.data.go.kr/cmm/cmm/fileDownload.do?atchFileId=FILE_000000003603931&fileDetailSn=1)이다. ZIP의 `한국관광공사_개방데이터_활용매뉴얼(국문)_v4.4.docx`(2026-02-26)와 `한국관광공사_개방데이터_활용신청방법_매뉴얼_v3.3.docx`를 직접 확인했다. 아래에서 각각 **활용매뉴얼**, **신청매뉴얼**로 부른다. 첨부 URL이 변경되면 상세페이지의 참고문서에서 최신본을 받는다.

## 2. 후보 데이터 소스

| 후보 | 제공·사용 조건 | 필요한 데이터 | 부산 조회 / 판단 |
| --- | --- | --- | --- |
| 한국관광공사 국문 관광정보 서비스_GW (`KorService2`) | 공식 제공 목록·현행 명세 확인. 무료, 활용신청·서비스키 필요, 개발 자동승인. REST JSON/XML | 이름·주소·좌표·관광타입·신분류코드·대표이미지. 상세 개요는 별도 조회. 실내 여부 직접 필드 없음 | 법정동 시도코드로 필터. 분류 및 원천 ID가 있어 **우선 선정** |
| 부산광역시 부산명소정보 서비스 | 공식 공공데이터포털 명세 확인. 무료, 활용신청·서비스키 필요, 개발 자동승인. REST JSON/XML | `MAIN_TITLE`, `ADDR1`, `LAT`, `LNG`, 설명·이미지. 분류·실내 여부 직접 필드 없음 | 부산 전용 데이터로 별도 시도 필터 불필요. **API 대체안** |
| 전국관광지정보표준데이터 | 공식 제공 목록과 CSV 등 다운로드 안내 확인. 파일 사용 시 API 호출량은 해당 없음. 파일 다운로드 절차·개별 이용조건은 사용 전 확인 필요 | 관광지명·관광지구분·주소·위도·경도·관광지소개. 공개 컬럼에 이미지·실내 여부 없음 | 공개 표에 부산 소재 행 확인. 주소/제공기관으로 부산 추출 가능. 지정 관광지·관광단지 중심이므로 다양한 장소 후보 확보에는 제한 |

출처: [한국관광공사](https://www.data.go.kr/data/15101578/openapi.do), [부산명소정보](https://www.data.go.kr/data/15063481/openapi.do), [전국관광지정보표준데이터](https://www.data.go.kr/data/15021141/standard.do).

부산시 데이터는 [부산 데이터 포털](https://data.busan.go.kr/bdip/opendata/detail.do?publicdatapk=15063481)에도 등재되어 있다. 공공데이터포털과 부산 포털의 같은 서비스이므로 별개의 API 두 개로 세지 않는다. 조사 중 부산 포털 직접 접속은 오류가 발생하여 세부 명세는 공공데이터포털을 기준으로 확인했다.

데이터 품질 평가는 문서상 필드와 제공기관을 기준으로 한 판단이다. 실제 부산 건수, 중복률, 폐업·폐쇄 반영 상태, 좌표 누락률과 사진 유효성은 **확인 필요**이다. 전국 표준데이터는 갱신 주기 연간, 기관별 자료 병합은 매월 초라고 안내되어 있어 등록 시점과 실제 데이터 기준일을 구별해야 한다.

## 3. 최종 선정 API

**한국관광공사 국문 관광정보 서비스_GW의 `KorService2`를 우선 사용한다.**

선정 이유는 무료 개발계정, 부산 필터, 장소명·주소·WGS84 좌표, 기본 분류와 콘텐츠 ID가 모두 문서화되어 있고 Java/Spring에서 다루기 쉬운 GET 방식 JSON을 지원하기 때문이다. 개발 자동승인으로 신청 절차도 비교적 단순하다. 이는 문서에 근거한 적합성 판단이며, 실제 키 승인과 표본 응답 검증을 전제로 한다. [공식 상세페이지](https://www.data.go.kr/data/15101578/openapi.do), [활용·신청매뉴얼](https://www.data.go.kr/cmm/cmm/fileDownload.do?atchFileId=FILE_000000003603931&fileDetailSn=1)

MVP에서는 `contentTypeId=12`(관광지), `14`(문화시설)를 각각 조회해 후보를 모으는 방식을 제안한다. 문화시설을 모두 실내로 간주하지 않는다. 숙박·음식점·행사·여행코스까지 한꺼번에 수집하지 않는다.

**버전 주의:** 현행 Swagger에서 `areaCode`, `sigunguCode`, `cat1~3`은 미사용·삭제 예정으로 표시된다. 예전 `areaCode=6` 예제를 새 구현의 기준으로 삼지 않는다. 지역은 `lDongRegnCd`/`lDongSignguCd`, 상세 분류는 `lclsSystm1~3`과 해당 코드조회 기능을 사용한다. `contentTypeId`는 현행 명세에 남아 있다. [현행 Swagger](https://www.data.go.kr/data/15101578/openapi.do)

## 4. 인증 및 요청 방식

### 기본 요청

| 항목 | 확인 내용 |
| --- | --- |
| Base URL | `https://apis.data.go.kr/B551011/KorService2` |
| 목록 Endpoint | `/areaBasedList2` |
| HTTP Method | `GET` |
| 인증 | 공공데이터포털 활용신청 후 발급된 키를 `serviceKey` 쿼리로 전달 |
| 응답 | XML 기본값, `_type=json` 지정 시 JSON |
| 필수 파라미터 | `serviceKey`, `MobileOS`, `MobileApp` |

| 파라미터 | 필수 여부 | 이 프로젝트의 사용 방법 |
| --- | --- | --- |
| `serviceKey` | 필수 | `{SERVICE_KEY}` 자리표시자. 실제 키는 향후 서버 환경변수로 관리 |
| `MobileOS` | 필수 | 웹 서비스이므로 `WEB` |
| `MobileApp` | 필수 | `CityRoulette` |
| `_type` | 선택 | `json` |
| `numOfRows` | 선택 | 처음에는 `10`. 최대 허용값은 확인 필요 |
| `pageNo` | 선택 | `1`부터 순차 조회 |
| `arrange` | 선택 | `A` 제목순. `C` 수정일순, `D` 생성일순도 제공 |
| `contentTypeId` | 선택 | `12` 또는 `14`를 별도 요청. 쉼표 다중 입력은 명세에 없으므로 사용하지 않음 |
| `lDongRegnCd` | 선택 | 부산 `26` |
| `lDongSignguCd` | 선택 | 부산 전체는 생략. 구·군 한정 시 코드조회 후 지정하며 `lDongRegnCd`도 필요 |
| `lclsSystm1~3` | 선택 | 세부 분류 필터. 2단계는 1단계, 3단계는 1·2단계 코드도 필요 |
| `modifiedtime` | 선택 | `YYYYMMDD` 형식. 변경분 수집의 경계 동작은 실제 확인 후 사용 |

파라미터 이름의 대소문자를 지킨다. `arrange=O/Q/R`는 대표이미지가 있는 항목만 대상으로 하는 정렬이므로 이미지가 없는 장소까지 확보할 초기 수집에서는 사용하지 않는다. 근거: [활용매뉴얼의 지역기반 관광정보 조회 명세](https://www.data.go.kr/cmm/cmm/fileDownload.do?atchFileId=FILE_000000003603931&fileDetailSn=1).

키가 없는 문서용 요청 예시이며 이번에 실행한 URL은 아니다.

```text
https://apis.data.go.kr/B551011/KorService2/areaBasedList2?serviceKey={SERVICE_KEY}&MobileOS=WEB&MobileApp=CityRoulette&_type=json&numOfRows=10&pageNo=1&arrange=A&lDongRegnCd=26&contentTypeId=12
```

### 부산 코드 확인과 페이지 처리

활용매뉴얼의 지역기반 요청·응답 예시에 `lDongRegnCd=26`과 부산 사하구 주소가 함께 나온다. 구현 시 아래 요청으로 시도 목록을 가져와 `name`이 부산광역시인 항목의 `code`를 다시 확인한다. `lDongRegnCd`를 생략하면 전체 시도 목록이며 `lDongListYn=N`일 때 응답 필드는 `code`, `name`이다. [공식 지역·법정동코드 명세](https://www.data.go.kr/data/15101578/openapi.do)

```text
https://apis.data.go.kr/B551011/KorService2/ldongCode2?serviceKey={SERVICE_KEY}&MobileOS=WEB&MobileApp=CityRoulette&_type=json&lDongListYn=N&numOfRows=100&pageNo=1
```

목록 응답의 `response.body.numOfRows`, `pageNo`, `totalCount`로 페이지 수를 계산한다. 한 페이지씩 순차 처리하고 마지막 페이지 또는 빈 결과에서 종료한다. `totalCount`와 페이지 내용은 수집 도중 달라질 수 있으므로 `contentid`로 중복을 막는다. 큰 `numOfRows`가 항상 허용된다고 가정하지 않는다.

### 키 발급과 관리

공공데이터포털 로그인 → 해당 서비스의 활용신청 → 목적·사용 기능 입력 → 개발계정 승인 상태와 키 확인 순서이다. 신청매뉴얼은 개발 자동승인 후 약 **10~30분**, 운영 심의는 약 **1~3일**을 안내한다. 이는 2025-05-30 신청매뉴얼의 안내값이며 현재 실제 처리시간은 **확인 필요**, 즉시 사용 보장은 아니다. [신청매뉴얼](https://www.data.go.kr/cmm/cmm/fileDownload.do?atchFileId=FILE_000000003603931&fileDetailSn=1)

향후 키는 예를 들어 `TOUR_API_SERVICE_KEY` 환경변수로 받는다. 이번에는 설정을 추가하지 않는다. URL 조립 시 키를 한 번만 인코딩하고, 포털의 Encoding/Decoding 키와 클라이언트의 자동 인코딩을 중복 적용하지 않는다. 전체 요청 URL을 로그에 남겨 키를 노출하지 않는다.

## 5. 주요 응답 필드

활용매뉴얼의 공식 JSON 예시는 `response.header`와 `response.body.items.item` 배열 구조이다. 아래는 필드 설명이며 임의로 만든 관광지 응답이 아니다. [활용매뉴얼](https://www.data.go.kr/cmm/cmm/fileDownload.do?atchFileId=FILE_000000003603931&fileDetailSn=1)

| 위치/필드 | 의미 | 사용 |
| --- | --- | --- |
| `response.header.resultCode`, `resultMsg` | 처리 결과 | 매뉴얼의 정상 예시는 `0000`, `OK`. HTTP 200만으로 성공 판단하지 않음 |
| `response.body.items.item` | 장소 목록 | 각 항목을 검사한 뒤 변환 |
| `contentid` | 원천 콘텐츠 ID | 중복 방지·상세 조회 |
| `title` | 콘텐츠 제목 | 장소명 |
| `contenttypeid` | 관광타입 ID | MVP의 간단한 category 분류 |
| `lclsSystm1`, `lclsSystm2`, `lclsSystm3` | 새 대·중·소 분류코드 | 세부 분류가 필요할 때 사용 |
| `addr1`, `addr2` | 주소·상세주소 | 존재하는 값만 공백으로 연결 |
| `mapx` | WGS84 경도 | longitude. 매뉴얼 JSON에는 문자열로 표현 |
| `mapy` | WGS84 위도 | latitude. 매뉴얼 JSON에는 문자열로 표현 |
| `lDongRegnCd` | 법정동 시도코드 | 부산 필터 결과 검증 |
| `firstimage`, `firstimage2` | 원본·썸네일 대표이미지 URL | 향후 이미지 표시 시 검토, 누락 가능 |
| `cpyrhtDivCd` | 저작권 유형 | 이미지 등 콘텐츠 이용조건 확인 |
| `modifiedtime` | 콘텐츠 수정일 | 향후 갱신 판단에 사용 검토 |

목록에 상세 설명이 항상 있다고 가정하지 않는다. 설명은 `GET /detailCommon2`의 `overview`로 확인하며, 필수 `contentId`와 공통 필수 파라미터가 필요하다. 추가 사진은 `GET /detailImage2`의 `originimgurl`, `smallimageurl`, `cpyrhtDivCd`를 확인할 수 있다. 현행 `detailCommon2` 명세에 없는 과거의 `overviewYN` 등의 옵션을 임의로 추가하지 않는다. [공식 상세조회 명세](https://www.data.go.kr/data/15101578/openapi.do)

새 분류코드 이름은 `GET /lclsSystmCode2`로 조회한다. 목록 응답의 `lclsSystm1~3`은 이름이 아닌 코드다. MVP category는 우선 `contenttypeid`의 문서화된 이름(12→관광지, 14→문화시설)을 저장하고, 서로 다른 분류 단계나 코드를 한 컬럼에 혼합하지 않는다.

## 6. Place Entity 매핑

O는 대응 필드가 있다는 뜻이며 모든 레코드의 값이 존재한다는 보장은 아니다.

| Place 필드 | API 필드 | 사용 가능 여부 | 비고 |
| --- | --- | --- | --- |
| `id` | 직접 매핑하지 않음 | O | 내부 DB 자동 증가 ID 유지. `contentid`로 덮어쓰지 않음 |
| `name` | `title` | O | 공백·빈 이름은 저장 대상에서 제외하고 오류로 기록 |
| `category` | `contenttypeid` | O | 코드→관광지/문화시설 이름 변환. 세부 분류는 `lclsSystm1~3`로 향후 확장 |
| `address` | `addr1` + `addr2` | O | 빈 부분을 제외해 연결. 둘 다 없으면 NULL |
| `latitude` | `mapy` | O | 문자열을 Double로 변환. 누락·빈 문자열·잘못된 수치는 NULL |
| `longitude` | `mapx` | O | 위도와 뒤바꾸지 않음. 누락·잘못된 수치는 NULL |
| `indoor` | 대응 필드 없음 | X | 현재 조사한 일반 관광 목록·상세 명세에서 직접 제공하지 않음 |

근거: [지역기반·공통정보조회 공식 명세](https://www.data.go.kr/data/15101578/openapi.do), [활용매뉴얼의 WGS84 좌표 정의](https://www.data.go.kr/cmm/cmm/fileDownload.do?atchFileId=FILE_000000003603931&fileDetailSn=1).

### 유지할 필드

`id`, `name`, `category`, `address`, `latitude`, `longitude`의 기본 역할과 타입은 유지한다. 좌표는 유한한 값과 위도 -90~90, 경도 -180~180 범위를 검사하고, 0 등 부산 장소 좌표로 의심스러운 값은 그대로 추천에 사용하지 않는다. 좌표가 없는 장소는 저장 가능하더라도 향후 위치 기반 후보에서는 제외한다. 문자열은 현재 컬럼 길이 255를 넘는지 실제 응답으로 확인하고, 초과값을 조용히 잘라 저장하지 않는다.

### 후속 모델 보완 구현 상태

- `externalId` (`String`, `external_id`) 추가 완료. 수동 등록 장소를 위해 NULL을 허용한다.
- UNIQUE 제약은 실제 수집 단계의 중복 처리 정책과 함께 도입하도록 보류했다. `existsByExternalId`를 추가했지만 조회만으로 동시 저장의 중복을 방지하지는 못한다.
- `indoor`를 nullable `Boolean`으로 변경하고 생성자, getter(`getIndoor`), 저장·조회 테스트를 수정했다. true=실내, false=실외, null=미확인이며 기존 DB 값은 변경하지 않는다.
- 기존 MySQL의 `indoor` NOT NULL 제거 여부는 직접 확인해야 한다. 아래 내용은 조사 당시 제안이다.

### 수정이 필요한 필드

`indoor`는 **`Boolean` + NULL 허용**으로 바꾸는 것을 권장한다. true=실내, false=실외, null=미확인으로 구분한다. 현재 `boolean`/NOT NULL에 미확인을 false로 넣으면 ‘실외라는 사실’을 만들어내게 된다. 초기 MVP에서는 실내·실외 필터를 제외한다.

| 처리 방식 | 판단 |
| --- | --- |
| category 기반 후처리 | 문화시설에도 야외·복합 공간이 있어 확정 근거로 부족. 추후 검수할 후보 분류에만 사용 |
| 별도 데이터 | 장소별 실내 여부를 명시하는 공식 근거가 확인될 때 검토. 이번 조사에서 그런 데이터 소스는 확보하지 못함 |
| nullable 처리 | 추천. 알 수 없는 정보를 사실대로 보존 |
| MVP에서 제외 | 실내·실외 필터를 보류하는 방식으로 nullable 처리와 함께 적용 |

기존 false 값이 이미 저장돼 있다면 실제 실외인지 미확인인지 알 수 없으므로 일괄 확정하지 않는다. 스키마·기존값 처리와 getter/생성자/테스트 변경은 다음 Issue에서 진행한다. **이번에는 Place를 수정하지 않았다.**

### 추가를 검토할 필드

- **`externalId` (`String`) 권장:** `contentid` 보관, 같은 데이터를 다시 가져올 때 중복 생성 방지. 내부 `id`와 분리한다. 하나의 소스만 쓰면 externalId 유일성, 복수 소스를 병합할 때는 아래 조합의 유일성을 검토한다.
- **`source` (`String`) 조건부 권장:** 부산시 대체 소스도 같은 테이블에 저장할 때 필요. 원천 ID는 기관 간에 충돌할 수 있으므로 `(source, externalId)`로 식별한다.
- `sourceModifiedAt`, `syncedAt`: 원천 수정일과 마지막 수집 시점을 구분할 필요가 생길 때 검토한다.
- 설명·이미지·저작권 유형 필드는 해당 화면 개발 시 함께 검토한다. 지금 전부 추가하지 않는다.

## 7. 데이터 저장 전략

| 방식 | 장점 | 단점 |
| --- | --- | --- |
| A. 사용자 요청마다 관광 API 호출 | 저장·갱신 로직이 적음, 제공기관의 최신 응답 사용 | 사용자 수에 따라 호출량 증가, 응답 지연·장애에 직접 영향, 여러 페이지 후보를 매번 모으기 어려움 |
| B. API에서 가져와 MySQL에 저장 후 조회 | 빠른 조회, 호출량 절약, 외부 장애 시 기존 데이터 사용, JPA 학습에 적합 | 중복 방지·누락값 처리·갱신 관리 필요, 데이터가 오래될 수 있음 |

**B를 추천한다.** 앞으로 승인된 키로 소량을 수집해 검증한 뒤 부산 관광지·문화시설을 페이지 단위로 저장한다. 첫 단계에서는 개발자가 필요할 때 실행하는 수집 작업이면 충분하며 Redis, 메시지 큐, 복잡한 스케줄러는 필요 없다.

동일 원천 ID는 갱신하고, 실패한 페이지 때문에 기존 정상 데이터를 지우지 않는다. 전체 수집 중 일부 누락을 즉시 삭제 신호로 해석하지 않는다. 수집시각을 기록하고 오래된 데이터임을 알 수 있도록 한다. API 장애 시 기존 DB를 사용하고, DB가 비어 있으면 데이터 준비 중임을 알린다. 구체적인 실행 방식은 다음 구현 Issue에서 정한다. 이번에는 저장이나 수집을 수행하지 않았다.

## 8. 제한사항

| 항목 | 확인 결과 / 남은 확인 |
| --- | --- |
| 사용 신청·인증 | 개발계정 신청과 서비스키 필요. 승인·실제 인증 응답은 미검증 |
| 비용 | 공식 상세페이지에서 무료 |
| 호출량 | 신청매뉴얼은 **오퍼레이션별 일일 1,000건** 안내. 현행 상세페이지도 개발 트래픽 1,000 표기. 실제 발급 계정의 한도·초당 제한·리셋 시각은 확인 필요 |
| 승인 시간 | 개발 약 10~30분, 운영 약 1~3일이라는 신청매뉴얼 안내. 현재 소요시간은 확인 필요 |
| 갱신 | 포털 메타데이터는 실시간. 개별 장소의 사실관계가 즉시 수정된다는 보장은 아님. `modifiedtime` 참고 |
| 좌표·주소 누락 | 활용매뉴얼에서 선택 항목이므로 값이 없을 가능성을 처리해야 함. 실제 누락률은 확인 필요 |
| 이미지 | URL 제공 확인. 누락·만료·깨진 링크 비율과 개별 저작권 유형은 확인 필요 |
| 장애 | 포털은 타임아웃, 인증, 일일·초당 제한 오류를 안내. 상태코드뿐 아니라 응답 결과를 확인하고 무한 재시도하지 않음 |
| JSON 오류 응답 | 정상 예제 구조는 확인. 인증 오류·빈 목록의 실제 본문/Content-Type은 키 발급 후 확인 필요 |
| 데이터 품질 | 실제 부산 건수, 중복, 폐쇄 장소 반영, 주소 길이, 좌표 정확성은 표본 검증 필요 |

호출량·승인: [공식 신청매뉴얼](https://www.data.go.kr/cmm/cmm/fileDownload.do?atchFileId=FILE_000000003603931&fileDetailSn=1). 무료·갱신·오류: [서비스 상세페이지](https://www.data.go.kr/data/15101578/openapi.do).

**이용조건:** 포털 전체 이용허락은 제한 없음으로 표시되지만 사진은 공공누리 1·3유형 및 별도 사용 제한이 안내되어 있다. `cpyrhtDivCd`를 확인해야 하며, 유형을 모르면 무조건 자유이용 가능하다고 해석하지 않는다. 명세는 Type1의 출처 표시를 ‘권장’으로 기술하지만, [공공누리 제1유형](https://www.kogl.or.kr/info/licenseType1.do)은 출처 표시 조건을, [제3유형](https://www.kogl.or.kr/info/licenseType3.do)은 출처 표시와 변경금지 조건을 명시한다. 프로젝트에서는 제공기관·원출처 링크와 해당 저작물의 유형을 표시하는 쪽으로 설계하고, 상충하는 개별 조건은 제공기관에 확인한다. 사진의 기업 CI/BI 사용 및 인격권 침해 용도 금지는 [관광공사 서비스 설명](https://www.data.go.kr/data/15101578/openapi.do)에 명시돼 있다. 이미지 표시 기능은 별도 Issue에서 권리 확인 후 진행한다.

## 9. 대체 데이터 소스

Primary는 한국관광공사, **Fallback은 부산광역시 부산명소정보 서비스**로 정한다. [공식 상세페이지](https://www.data.go.kr/data/15063481/openapi.do)

- 요청: `GET https://apis.data.go.kr/6260000/AttractionService/getAttractionKr`
- 필수: `ServiceKey`, `pageNo`, `numOfRows`. 선택: `resultType=json`, `UC_SEQ`. 관광공사와 키·형식 파라미터 이름이 다르므로 대소문자까지 해당 명세를 따른다.
- 부산 전용 목록이므로 부산 시도코드 필터는 필요 없다.
- `MAIN_TITLE`→name, `ADDR1`→address, `LAT`→latitude, `LNG`→longitude, `UC_SEQ`→externalId로 검토한다. `TITLE`은 소개 제목이므로 장소명 대신 무조건 사용하지 않는다.
- 이름·주소·좌표도 명세상 선택 항목이므로 누락 가능성을 처리한다. 필드 존재와 값의 완전성은 구분한다.
- 설명 `ITEMCNTNTS`, 이미지 `MAIN_IMG_NORMAL`/`MAIN_IMG_THUMB`가 문서화돼 있다. category와 indoor의 직접 필드는 없으므로 미확인을 보존해야 한다.
- 무료, 개발 자동승인·운영 심의승인, 개발 트래픽 10,000 표기. 한도의 기간 단위·초당 제한·승인 소요시간은 **확인 필요**. 포털 갱신 주기는 실시간 표기이다.
- 공공데이터포털은 이용허락 제한 없음으로 표기하지만 부산 포털의 검색 노출 내용에는 다른 권리 표기가 있었고 직접 접속은 오류였다. 이미지·설명 이용조건은 부산시 제공부서에 **재확인 필요**이다.
- 독립적으로 이 서비스의 활용신청이 필요하다. 주 소스의 인증키가 별도 신청 없이 그대로 통한다고 가정하지 않는다. 두 서비스가 공공데이터포털 인프라를 공유하므로 모든 장애에 독립적인 대안도 아니다.

Fallback은 자동 전환을 구현하겠다는 뜻이 아니라, 주 소스를 사용할 수 없을 때 다시 검증해 채택할 후보이다. 이번 조사에서는 이 API의 실제 인증 응답과 JSON 중첩 경로까지 검증하지 않았다.

## 10. 다음 개발 단계

추천 Issue: **[Feature] 관광정보 수집을 위한 Place 원천 ID 및 미확인 실내 여부 모델 보완**.

1. `externalId`와 nullable `Boolean indoor`를 반영하고, 현재 데이터 유무에 따라 제약·기존값 처리를 결정한다. MVP category는 관광타입의 이름으로 통일한다.
2. 별도 준비 작업으로 공공데이터포털 개발계정을 신청하고, 로컬에서 부산 코드와 관광지/문화시설 각 1페이지를 확인한다. 실제 키·키 포함 URL·원본 인증 로그는 Git에 남기지 않는다.
3. 이후 **[Feature] 부산 관광정보 조회 및 MySQL 수집** Issue에서만 요청 DTO/응답 변환, 중복 방지, 페이지 처리, 누락값·오류 처리를 구현한다.
4. 저장된 Place를 조회하는 화면을 구현한 뒤 추천 후보 선정으로 진행한다. 날씨·교통·지도는 그 이후 별도 Issue로 진행한다.

완료 판단: 후보·공식 요청/응답·매핑·저장 전략·대체안을 문서화했다. 미확인 항목은 위에 남겼다. API 사용 가능 여부는 **조건부 사용 가능**, 현재 Place는 **일부 수정 필요**이다. 이번 조사에서는 실행 코드를 변경하거나 DB에 데이터를 넣지 않았다.
