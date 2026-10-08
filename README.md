# 🎲 City Roulette

> 부산의 대중교통과 공공데이터를 활용하여  
> 사용자의 현재 상황에 맞는 즉흥 여행 목적지를 추천하는 웹 서비스

---

## 📌 Basic Info

### 프로젝트명

**City Roulette**

Repository: `busan-city-roulette`

### 개요

사용자의 출발 위치와 이용 가능한 시간, 날씨 등의 정보를 바탕으로  
부산에서 현재 방문할 수 있는 여행지를 탐색하고 즉흥적으로 추천하는 웹 서비스입니다.

### 동기

외출하거나 여행을 하고 싶어도 어디를 갈지 쉽게 결정하지 못하거나,
항상 익숙한 장소만 반복해서 방문하는 경우가 많습니다.

또한 새로운 장소를 찾으려면 관광지 검색, 이동 시간 확인,
대중교통 검색, 날씨 확인 등을 각각 따로 해야 하는 불편함이 있습니다.

기존 여행 서비스는 대부분 사용자가 목적지를 먼저 정한 뒤
경로나 주변 정보를 제공하는 방식입니다.

City Roulette는 이러한 과정을 반대로 구성하여,
**사용자가 목적지를 정하지 않아도 현재 상황을 기반으로
갈 수 있는 장소를 먼저 추천하는 서비스**를 만드는 것을 목표로 합니다.

### 예상 결과물

사용자가 출발지와 이용 가능한 시간을 입력하면
부산의 관광지, 날씨, 대중교통 등의 데이터를 분석하여
현재 방문할 수 있는 목적지를 추천합니다.

최종적으로 다음과 같은 정보를 하나의 웹 화면에서 확인할 수 있도록 구현합니다.

- 추천 목적지
- 관광지 기본 정보
- 예상 이동 시간
- 현재 날씨
- 추천 이유
- 간단한 여행 코스

---

## ✨ Key Features

### 1. 🎲 즉흥 목적지 추천

사용자의 출발 위치와 이용 가능한 시간을 기반으로
방문 가능한 장소들을 탐색한 뒤 목적지를 추천합니다.

단순 랜덤 선택이 아니라 이동 가능 여부 등의 조건을 적용한 후
후보 장소 중 하나를 선택하도록 구현합니다.

### 2. 🚌 대중교통 기반 목적지 필터링

부산의 버스 및 대중교통 데이터를 활용하여
실제로 이동 가능한 장소를 목적지 후보로 선정합니다.

사용 가능한 시간보다 이동 시간이 오래 걸리는 장소는
추천 대상에서 제외합니다.

### 3. 🌤 상황 기반 여행 추천

날씨와 관광지 정보를 결합하여
현재 상황에 적합한 장소를 우선적으로 추천합니다.

예를 들어 비가 오는 경우 실내 관광지를,
날씨가 좋은 경우 공원이나 해변과 같은 야외 장소를
추천할 수 있도록 구현합니다.

---

## 🛠 Tech Stack

### Frontend

- HTML
- CSS
- JavaScript
- Thymeleaf

### Backend

- Java
- Spring Boot
- Spring MVC
- Spring Data JPA

### Database

- MySQL

### Deployment

- Render
- AWS

프로젝트 구현 및 배포 단계에서 Spring Boot 애플리케이션의
배포 환경과 비용, 설정 난이도 등을 비교하여 최종 배포 플랫폼을 결정할 예정입니다.

---

## 🗂 Data / API

프로젝트에서는 다음과 같은 공공데이터 및 외부 API 활용을 계획하고 있습니다.

- 부산광역시 버스정보 API
- 한국관광공사 관광정보 API
- 기상청 날씨 API
- 공공데이터포털 Open API

사용할 API는 구현 과정에서 데이터 제공 범위와 사용 가능 여부에 따라
추가 또는 변경될 수 있습니다.

관광정보 데이터 소스와 Place 매핑 조사: [Tourism API Research](docs/tourism-api-research.md)

---

## 🗓 Milestones

### Week 1-4 — 기획 · 설계 · 초기 세팅

- 프로젝트 주제 및 요구사항 정의
- Git/GitHub 개발 환경 구축
- GitHub Repository 생성
- 공공데이터 및 API 조사
- 주요 화면 구성 설계
- 데이터 구조 설계
- Spring Boot 프로젝트 초기 설정

### Week 5-8 — 핵심 기능 구현 · MVP

- 관광지 API 연동
- 관광지 데이터 조회 기능 구현
- 목적지 후보 생성 기능 구현
- 랜덤 목적지 추천 기능 구현
- 기본 웹 UI 구현
- MVP 완성

### Week 9-12 — 고도화 · 추가 기능 · 테스트

- 날씨 API 연동
- 부산 대중교통 데이터 연동
- 이동 가능 시간 기반 필터링
- 상황 기반 추천 로직 구현
- 추천 결과 화면 개선
- 예외 처리 및 기능 테스트

### Week 13-16 : 최적화, 문서화, 최종 발표

- UI/UX 개선
- 코드 리팩토링
- 기능 및 오류 테스트
- Vercel, Render, AWS 등 배포 환경 비교
- 최종 배포 환경 선정 및 웹 서비스 배포
- README 및 프로젝트 문서 작성
- 최종 발표 준비

---

## 📄 License

This project is licensed under the MIT License.

---

## Development

현재 구현 범위는 **Spring Boot 기본 개발 환경, MySQL 연결 설정과 Place 기본 스키마**입니다.
`GET /`에서 Thymeleaf 홈 화면을 표시하며, 여행 조건 입력과 추천 버튼은 아직 비활성화되어 있습니다.
외부 API, 추천 로직, 회원 기능은 구현하지 않았습니다.

### Requirements

- JDK 17 (IDE 실행용 Java와 별도로 프로젝트 JDK 및 Gradle JVM을 17로 설정)
- Spring Boot 3.5.16
- Gradle 8.14.3: 저장소의 Wrapper 사용, 별도 Gradle 설치 불필요
- MySQL 8 이상 및 사용자가 준비한 개발 DB/계정
- Spring Tool Suite(STS), 또는 IntelliJ IDEA / VS Code

Java와 Gradle 조합은 [Spring Boot 공식 요구사항](https://docs.spring.io/spring-boot/3.5/system-requirements.html)을 기준으로 선택했습니다.
최초 Import와 빌드에는 Gradle 및 의존성을 다운로드할 인터넷 연결이 필요합니다.

### MySQL 설정

MySQL 서버를 실행하고, 개발 DB(기본 이름 `cityroulette`)와 해당 DB에 접근할 계정을 직접 준비합니다.
애플리케이션은 DB를 생성하거나 삭제하지 않습니다.

Windows에서는 서비스 앱에서 설치한 MySQL 서비스를 시작하고, MySQL Workbench 또는 MySQL 클라이언트에서
DB 생성 권한이 있는 계정으로 다음 명령을 최초 한 번 실행합니다.

```sql
CREATE DATABASE IF NOT EXISTS cityroulette
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

이미 같은 이름의 DB가 있다면 이 명령은 기존 DB의 설정을 변경하지 않습니다.
다른 DB 이름을 사용하려면 위 SQL의 이름과 `DB_NAME`을 함께 변경하세요.
애플리케이션용 계정에는 해당 개발 DB의 조회·추가·수정·삭제 권한과,
`ddl-auto=update`가 테이블을 준비하는 데 필요한 CREATE·ALTER·INDEX 권한이 필요합니다.
실제 계정명과 비밀번호는 로컬에서만 관리합니다.

| 환경변수 | 기본값 | 설명 |
| --- | --- | --- |
| `DB_HOST` | `localhost` | MySQL 서버 주소 |
| `DB_PORT` | `3306` | MySQL 포트 |
| `DB_NAME` | `cityroulette` | 이미 준비된 개발 DB 이름 |
| `DB_USERNAME` | 없음, 필수 | 본인의 DB 계정 |
| `DB_PASSWORD` | 없음, 필수 | 본인의 DB 비밀번호 |

`.env.example`은 환경변수 목록을 보여주는 참고 파일입니다.
**Spring Boot는 `.env`를 자동으로 읽지 않습니다.** STS 실행 설정의 Environment에 입력하거나,
터미널에서 실행하기 전에 OS 환경변수로 설정하세요. 실제 비밀번호나 API Key를 파일에 작성해 Git에 올리지 마세요.
`.env`, 개인 IDE 설정, `*.launch`, `application-local.*`는 Git에서 제외합니다.
공유 가능한 Eclipse 프로젝트 설정과 Java 17/UTF-8 설정은 저장소에 포함합니다.

JDBC URL에는 `connectionTimeZone=Asia/Seoul&characterEncoding=UTF-8`을 사용합니다.
`connectionTimeZone`은 Connector/J의 시간 변환 기준이며 MySQL 서버 시간대 자체를 변경하지 않습니다.
`characterEncoding=UTF-8`과 DB의 `utf8mb4`로 한글 등을 저장할 수 있도록 구성했습니다.

개발용 `spring.jpa.hibernate.ddl-auto=update`는 Entity를 기준으로 없는 테이블을 만들고 필요한 스키마 변경을 시도합니다.
기존 테이블을 매번 삭제하는 설정은 아니지만, 모든 변경을 안전하게 처리하는 마이그레이션 도구는 아닙니다.
현재는 개인 개발 DB에 사용하고, 중요한 데이터가 생기면 백업과 명시적인 스키마 변경 방식을 검토하세요.
`spring.jpa.open-in-view=false`는 화면 렌더링 중 DB 접근을 막습니다.
애플리케이션 시작에는 MySQL 연결이 필요하며, 홈 화면 자체는 Repository를 호출하지 않습니다.

SQL 로그는 기본적으로 끕니다. 학습할 때만 STS 환경변수에 `SPRING_JPA_SHOW_SQL=true`를 추가하면
Hibernate가 실행한 SQL을 확인할 수 있습니다. 파라미터 값 상세 로그는 켜지 않습니다.

설정 근거: [Connector/J 시간대](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-datetime-types-processing.html),
[문자 인코딩](https://dev.mysql.com/doc/connector-j/en/connector-j-reference-charsets.html),
[MySQL DB 생성](https://dev.mysql.com/doc/refman/8.4/en/create-database.html),
[Spring Boot 스키마 초기화](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html).

### Place 기본 스키마

`Place`는 `places` 테이블에 대응합니다. 실행 시 JPA가 스키마를 준비하며 실제 관광지 데이터는 넣지 않습니다.

| 필드 | Java 타입 | 의미 / 제약 |
| --- | --- | --- |
| `id` | `Long` | 기본키, MySQL AUTO_INCREMENT |
| `externalId` | `String` | 원천 콘텐츠 ID (`external_id`), NULL 가능 |
| `name` | `String` | 장소 이름, NULL 불가 |
| `category` | `String` | 장소 분류, 미확인 시 NULL 가능 |
| `address` | `String` | 주소, 미확인 시 NULL 가능 |
| `latitude` | `Double` | 위도, 미확인 시 NULL 가능 |
| `longitude` | `Double` | 경도, 미확인 시 NULL 가능 |
| `indoor` | `Boolean` | 실내 true, 실외 false, 미확인 NULL |

문자열 컬럼 길이는 JPA 기본값 255입니다. 좌표는 위치 표현에 사용할 부동소수점 값으로 저장합니다.
`name`의 NULL 제약과 빈 문자열 검증은 다릅니다. 향후 입력 DTO를 만들 때 빈 이름 등의 검증을 추가합니다.
`PlaceRepository extends JpaRepository<Place, Long>`이 기본 저장·조회·삭제 메서드를 제공합니다.
아직 Place용 Service, Controller, 관계 매핑은 없습니다.

### Run

1. MySQL Server를 시작하고 위 SQL로 `cityroulette` DB를 준비합니다.
2. STS에서 `File > Import > Gradle > Existing Gradle Project`로 저장소를 가져옵니다.
   Gradle Wrapper를 선택하고 프로젝트 JDK 및 Gradle JVM을 JDK 17로 설정합니다. 이미 Import했다면 기존 프로젝트를 사용합니다.
3. `Run > Run Configurations > Spring Boot App`에서 `CityRouletteApplication` 실행 구성을 선택하거나 만들고,
   `Environment` 탭에 위 MySQL 환경변수를 설정합니다. 이 실행 설정은 개인 workspace에만 보관합니다.
4. `CityRouletteApplication`을 `Run As > Spring Boot App`으로 실행합니다.
5. Console에서 아래 연결 확인 항목을 확인합니다.
6. 브라우저에서 `http://localhost:8080`에 접속해 기존 홈 화면을 확인합니다.

터미널에서는 동일한 환경변수를 설정한 뒤 실행합니다.

```bash
./gradlew bootRun
```

Windows에서는 `gradlew.bat bootRun`을 사용합니다.
DB 연결 오류가 발생하면 서버 실행 여부, DB 이름, 계정 권한, 환경변수를 확인하세요.

### 실제 MySQL 연결 확인

- Console에 HikariPool의 `Start completed`와 `Started CityRouletteApplication`이 출력되는지 확인합니다.
- `Access denied`, `Communications link failure`, DDL 실행 오류가 없는지 확인합니다.
  시작 완료 로그만으로 테이블 생성 성공까지 단정하지 말고 아래 SQL도 실행하세요.
- MySQL Workbench에서 애플리케이션과 같은 서버·DB에 연결한 뒤 확인합니다.

```sql
SELECT DATABASE(), VERSION();
SHOW CREATE DATABASE cityroulette;
USE cityroulette;
SHOW TABLES LIKE 'places';
SHOW CREATE TABLE places;
SELECT COUNT(*) FROM places;
```

`places` 테이블, `id` 기본키와 AUTO_INCREMENT, `name`의 NOT NULL, `external_id`와 `indoor`의 NULL 허용을 확인합니다.
기존 테이블의 `indoor` NOT NULL 제거는 `ddl-auto=update`만으로 적용되지 않을 수 있으므로 실제 스키마를 확인하세요.
처음 만든 DB라면 데이터 건수는 0입니다. DB 이름을 바꿨다면 확인 SQL도 맞춰 수정하세요.
`Unknown database`는 DB 이름/생성 여부, `Access denied`는 계정/비밀번호/권한,
`Communications link failure`는 서버/호스트/포트를 먼저 확인합니다.

### Build / Test

```bash
./gradlew clean build
```

Windows에서는 `gradlew.bat clean build`를 사용합니다.
`contextLoads()`와 홈 화면의 HTTP 200, 뷰 이름, Thymeleaf 렌더링을 테스트합니다.
추가로 Place Entity 인식과 Repository Bean 주입, ID 자동 생성, 저장 후 DB 재조회,
외부 ID 저장·조회와 존재 여부, 실내 여부 true/false/NULL 저장, 필수 이름의 NULL 거부를 테스트합니다. 테스트 데이터는 트랜잭션 종료 시 롤백합니다.
테스트에만 H2 메모리 DB와 `test` 프로필을 사용하므로 MySQL 서버나 비밀번호 없이 실행할 수 있습니다.
H2는 실행용 JAR에 포함되지 않으며, 이 테스트의 통과가 실제 MySQL 연결 성공을 의미하지는 않습니다.
테스트 보고서는 `build/reports/tests/test/index.html`에서 확인할 수 있습니다.

### 프로젝트 구조

```text
src/main/java/com/cityroulette/
├── CityRouletteApplication.java
├── controller/HomeController.java
├── service/package-info.java
├── repository/PlaceRepository.java
├── domain/Place.java
├── dto/package-info.java
└── config/package-info.java

src/main/resources/
├── application.properties
├── templates/index.html
└── static/
    ├── css/style.css
    └── js/main.js
```

- `controller`: HTTP 요청을 받아 화면을 연결합니다.
- `service`: 향후 여행 추천 등의 비즈니스 로직을 담당합니다.
- `repository`: DB 저장·조회 작업을 담당합니다.
- `domain`: DB 테이블에 대응하는 Entity를 둡니다. 현재는 장소의 기본 정보인 Place만 정의합니다.
- `dto`: 향후 화면/API와 주고받는 데이터를 담습니다.
- `config`: 향후 외부 API 등 설정을 둡니다.

사용하지 않는 패키지는 `package-info.java`에 역할만 설명했습니다.
현재 HomeController는 화면만 반환하며, 기능 개발 시 Controller → Service → Repository로 책임을 나눕니다.
Trip/Recommendation, 외부 API 데이터 저장, 샘플 데이터, 별도 예외 처리 클래스는 아직 만들지 않았습니다.
오류 응답은 Spring Boot 기본 처리를 사용합니다.

### Dependencies

- Spring Web: Spring MVC와 내장 Tomcat
- Thymeleaf: 서버에서 HTML 화면 렌더링
- Spring Data JPA: Entity와 Repository를 통한 DB 접근
- MySQL Driver: MySQL 연결
- Validation: 향후 입력 DTO 검증을 위한 기본 의존성
- Spring Boot Test / JUnit Platform Launcher: 기본 테스트 실행
- H2: 테스트 전용 메모리 DB

- Spring Boot DevTools: 개발 실행 시 재시작 지원 (`developmentOnly`, 배포 JAR에서 제외)

Lombok, Spring Security는 추가하지 않았습니다.

### 다음 GitHub Issue 제안

1. 관광정보 API 명세 조사 및 Place 필드 매핑 정의
2. 관광지 조회 기능을 Controller → Service → Repository로 구현
3. 여행 조건 DTO와 입력 검증 구현
4. 목적지 후보 선정 및 기본 추천 기능 구현


### Eclipse(STS) / VS Code / Docker code-server

기존 `com.cityroulette` 패키지, `CityRouletteApplication` 및 Gradle 프로젝트 이름
`city-roulette`을 유지합니다. 저장소 폴더 이름은 `busan-city-roulette`입니다.
Gradle Groovy DSL의 `build.gradle`을 공통 빌드 설정으로 사용합니다.

```text
busan-city-roulette/
├── .settings/
│   ├── org.eclipse.buildship.core.prefs
│   ├── org.eclipse.core.resources.prefs
│   └── org.eclipse.jdt.core.prefs
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/com/cityroulette/
│   │   │   ├── CityRouletteApplication.java
│   │   │   └── controller/, service/, repository/, domain/, dto/, config/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/css/style.css
│   │       ├── static/js/main.js
│   │       └── templates/index.html
│   └── test/
│       ├── java/com/cityroulette/
│       └── resources/application-test.properties
├── docs/
├── .classpath
├── .project
├── .gitattributes
├── .gitignore
├── .env.example
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── LICENSE
└── README.md
```

기존 `.git/`, `.github/` 및 문서는 그대로 유지합니다.
`.gradle/`, `build/`는 Gradle 실행 시 생성되며, `bin/`은 Eclipse 빌드 시 생성될 수 있습니다.
이 출력/캐시 디렉터리는 Git에서 제외하며 수동 생성할 필요가 없습니다.

**Eclipse(STS)**

1. Buildship이 설치된 Eclipse 또는 STS에서 `File > Import > Gradle > Existing Gradle Project` 선택.
2. `busan-city-roulette` 루트를 선택하고 Gradle distribution은 **Gradle Wrapper** 사용.
3. 프로젝트 JRE 및 Gradle JVM에 JDK 17 지정. IDE 자체의 실행 JDK 요구사항은 별도로 충족합니다.
4. Import 후 `Gradle > Refresh Gradle Project` 실행.
5. 위 MySQL 환경변수를 Run Configurations에 설정하고 `CityRouletteApplication` 실행.
   일반 Eclipse는 `Run As > Java Application`, STS는 `Run As > Spring Boot App` 사용.

공유 `.classpath`에는 JavaSE-17과 Buildship 의존성 컨테이너를 사용하며 절대 JAR 경로는 넣지 않습니다.
소스/의존성 변경은 `build.gradle`에 반영한 뒤 Refresh합니다.
메타데이터 재생성이 필요한 경우 `./gradlew eclipse`를 사용합니다.
설정 방식: [Gradle Eclipse 플러그인 공식 문서](https://docs.gradle.org/current/userguide/eclipse_plugin.html).

**VS Code / Docker code-server**

- 프로젝트 루트 폴더를 열고, 컨테이너 내부에도 JDK 17을 설치합니다.
  `java -version`과 `./gradlew --version`으로 확인합니다.
- Java 언어 지원 및 Gradle 확장을 사용할 수 있습니다. 확장의 언어 서버 실행용 JDK 요구사항과
  프로젝트 빌드용 JDK 17은 별개입니다. 설치 경로는 각 환경의 사용자 설정에서 지정합니다.
  확장 사용 여부와 무관하게 아래 터미널 명령으로 빌드/실행할 수 있습니다.
- Docker에는 쓰기 가능한 프로젝트 볼륨과 Gradle 캐시를 사용하고 컨테이너 사용자에게 접근 권한을 줍니다.
- MySQL이 다른 컨테이너라면 같은 Docker 네트워크의 서비스 이름을 `DB_HOST`로 사용합니다.
  컨테이너의 `localhost`는 해당 컨테이너 자체입니다.
- MySQL 환경변수는 Docker 환경 설정이나 터미널에서 전달합니다. `.env`는 Spring Boot가 자동 로드하지 않습니다.
- 웹 화면은 컨테이너의 8080 포트를 게시하거나 code-server 프록시/포트 전달 기능으로 확인합니다.
  개발 컨테이너를 실행할 때 포트 게시가 필요하면 `-p 127.0.0.1:8080:8080`을 지정합니다.

```bash
./gradlew tasks
./gradlew clean build
# MySQL 서버 및 DB 환경변수 준비 후
./gradlew bootRun
# 또는 빌드한 실행 JAR
java -jar build/libs/city-roulette-0.0.1-SNAPSHOT.jar
```

Windows에서는 `./gradlew` 대신 `gradlew.bat`을 사용합니다.
DevTools는 컴파일된 클래스 변경을 감지하므로 Java 파일 수정 후 IDE 자동 빌드 또는
별도 터미널의 `./gradlew classes`로 컴파일합니다.


### 구성 검증 결과

- `./gradlew tasks`, `./gradlew eclipse`, `./gradlew clean build` 성공.
- 기존 테스트 8건 통과(실패/오류 0).
- 아래 명령으로 테스트 전용 H2를 사용해 실제 서버를 실행하고 `/` 및 `/css/style.css`의 HTTP 200 확인.

```bash
./gradlew bootTestRun --args='--spring.profiles.active=test --server.port=18080 --server.address=127.0.0.1'
```

이 명령은 테스트 클래스패스를 사용한 일시적인 실행 확인용이며 MySQL 실행 설정은 변경하지 않습니다.
실행 JAR에서 H2와 DevTools가 제외되는 것도 확인했습니다.
Eclipse 메타데이터의 JavaSE-17, Buildship 컨테이너, main/test 소스 및 상대 출력 경로를 검토했습니다.
실제 MySQL 연결, Eclipse/STS GUI Import 및 Docker code-server GUI 동작은 이 환경에서 검증하지 않았습니다.
