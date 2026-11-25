# 외부 공공데이터 API 설정 및 연결 진단

이 문서는 API 키를 저장소에 기록하지 않고 STS 실행 환경에서 City Roulette에 연결하는 방법을 설명한다.
기본 웹 서버는 키가 없어도 시작되며 외부 API 진단은 명시적으로 실행할 때만 호출된다.

## 환경변수

| 공급자 | 환경변수 | 진단 오퍼레이션 |
| --- | --- | --- |
| 한국관광공사 TourAPI | `TOUR_API_KEY` | `KorService2/areaBasedList2`, 부산 1건 |
| 부산명소정보 | `BUSAN_ATTRACTION_API_KEY` | `AttractionService/getAttractionKr`, 1건 |
| 부산맛집정보 | `BUSAN_RESTAURANT_API_KEY` | `FoodService/getFoodKr`, 1건 |
| 부산푸디투어 | `BUSAN_FOODIE_API_KEY` | `FoodieService/getFoodieKr`, 1건 |
| 부산 갈맷길 | `BUSAN_GALMAETGIL_API_KEY` | `fbusangmgtourinfo/getgmgtourinfo`, 1건 |
| 부산버스정보시스템 | `BUSAN_BUS_API_KEY` | `BusanBIMS/busStopList`, 정류소 1건 |

공공데이터포털 서비스별 활용신청이 필요하다. 여러 환경변수에 같은 포털 키를 등록할 수 있지만,
키가 같더라도 각 데이터셋의 활용 승인은 별개일 수 있다.

## STS 등록

1. `Run > Run Configurations...`를 연다.
2. `Spring Boot App`에서 `CityRouletteApplication` 실행 구성을 선택한다.
3. `Environment` 탭에서 `New...`를 눌러 위 환경변수를 필요한 만큼 등록한다.
4. `Apply` 후 실행 구성을 다시 시작한다. 이미 실행 중인 프로세스에는 변경값이 반영되지 않는다.

`.env.example`은 변수 이름을 보여주는 참고 파일일 뿐이며 Spring Boot가 자동으로 읽지 않는다.
STS의 개인 실행 설정, `.env`, `application-local.properties`, 실제 키는 Git에 추가하지 않는다.

## 설정 매핑

`application.properties`는 환경변수를 다음 계층에 바인딩한다.

```properties
external.api.tour.key=${TOUR_API_KEY:}
external.api.busan.attraction.key=${BUSAN_ATTRACTION_API_KEY:}
external.api.busan.restaurant.key=${BUSAN_RESTAURANT_API_KEY:}
external.api.busan.foodie.key=${BUSAN_FOODIE_API_KEY:}
external.api.busan.galmaetgil.key=${BUSAN_GALMAETGIL_API_KEY:}
external.api.busan.bus.key=${BUSAN_BUS_API_KEY:}
```

`ExternalApiProperties`가 이를 한 번만 바인딩하고 각 Client에는 해당 공급자의 키만 전달한다.
기존 `tour.api.service-key` 설정도 TourAPI에 한해 호환된다. 빈 기본값 덕분에 키가 없는 공급자가 있어도
웹 서버 시작은 실패하지 않으며, 호출 시 `NOT_CONFIGURED`로 보고된다.

## 연결 진단 실행

진단은 DB 연결이 필요한 Spring Boot 애플리케이션 컨텍스트에서 실행한다. STS의 `Arguments > Program arguments`에
다음을 추가하고 실행한다.

```text
--spring.main.web-application-type=none --external.api.diagnostics.enabled=true
```

터미널에서는 DB 환경변수와 API 환경변수를 현재 세션에 설정한 뒤 실행한다.

```powershell
.\gradlew.bat bootRun --args="--spring.main.web-application-type=none --external.api.diagnostics.enabled=true"
```

각 공급자는 다음 상태 중 하나로 출력된다.

- `CONNECTED`: HTTP 응답, 본문 결과 코드, 응답 파싱을 모두 통과함
- `NOT_CONFIGURED`: 해당 환경변수가 비어 있음
- `AUTH_FAILED`: HTTP 401/403 또는 공급자 인증·권한 오류 코드
- `HTTP_ERROR`: 인증 외 HTTP 상태 또는 전송 실패
- `PARSE_ERROR`: 성공 응답 본문을 JSON/XML로 해석하지 못함
- `UNAVAILABLE`: 공급자 오류나 현재 지원하지 않는 응답으로 완료하지 못함

결과에는 공급자, 상태, 전체 데이터 건수와 안전한 오류 요약만 포함한다. API 키와 인증키가 포함된 전체 URL은
출력하지 않는다. 기본 실행에서는 `external.api.diagnostics.enabled=false`이므로 외부 API를 호출하지 않는다.

`local` 또는 `dev` 프로필에서는 관리자만 `GET /dev/api-status`를 호출할 수 있다. 운영 프로필에는 Controller가
등록되지 않는다. 관리자 계정이 없는 개발 DB에서는 권한을 우회하지 말고 위 CLI 진단을 사용한다.

## 문제 해결

- `NOT_CONFIGURED`: 환경변수 철자, STS 실행 구성 선택, `Apply`, 프로세스 재시작을 확인한다.
- 인증·권한 오류: 공공데이터포털의 해당 서비스 활용신청 및 승인 상태를 확인한다.
- 키 형식 오류: 포털의 일반 인증키 또는 URL 인코딩 인증키를 그대로 사용한다. 코드는 중복 인코딩을 방지한다.
- 호출 한도 오류: 반복 진단하지 말고 포털에서 일일·초당 한도를 확인한다.
- HTTP 200이지만 `UNAVAILABLE`: 본문의 `resultCode`/`code`가 성공값인지 확인한다.
- 버스 XML 오류: 부산버스정보시스템은 JSON이 아닌 XML 응답이며 `busStopList` 권한을 확인한다.

진단을 위해 키 값을 로그 레벨 변경, 테스트 출력, 스크린샷 또는 이슈 본문에 복사하지 않는다.

## 공식 명세

- [한국관광공사 국문 관광정보 서비스](https://www.data.go.kr/data/15101578/openapi.do)
- [부산명소정보 서비스](https://www.data.go.kr/data/15063481/openapi.do)
- [부산맛집정보 서비스](https://www.data.go.kr/data/15063472/openapi.do)
- [부산푸디투어정보 서비스](https://www.data.go.kr/data/15063507/openapi.do)
- [갈맷길 관광 정보 안내](https://www.data.go.kr/data/15077606/openapi.do)
- [부산버스정보시스템](https://www.data.go.kr/data/15092750/openapi.do)
