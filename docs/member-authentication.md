# 회원가입 및 세션 인증

## 구현 범위와 구조

Java 17 / Spring Boot 3.5.16의 기존 MVC·JPA 구조를 유지합니다.
회원가입은 `MemberController → MemberService → MemberRepository → members` 순서로 처리합니다.
로그인은 Spring Security Filter → CustomUserDetailsService → MemberRepository 순서로 처리하며,
성공한 인증 상태를 HTTP Session에 저장합니다. JWT, 소셜 로그인, 별도 인증 서버는 사용하지 않습니다.

기존 구현은 HomeController, 홈 화면, Place 및 PlaceRepository입니다.
관광정보·교통·추천 API와 추천 로직은 아직 구현되지 않았습니다.
`/recommend`는 기존 홈의 추천 준비 화면을 재사용합니다.

## 의존성과 데이터

기존 Validation, JPA, Thymeleaf, H2를 재사용하고 Spring Security와 테스트용 security-test만 추가합니다.
화면의 로그인 정보는 MVC 모델로 제공하므로 Thymeleaf Security extras는 필요하지 않습니다.

`Member`는 Flyway `V3__create_members_table` migration으로 생성한 `members` 테이블에
id, username, password(hash), email, displayName, role, createdAt을 저장합니다.
username과 email에는 DB unique 제약을 둡니다. 새 회원의 role은 USER이며 Role enum에는 USER와 ADMIN만 둡니다.
비밀번호 확인은 MemberSignupRequest에만 존재하며 DB에 저장하지 않습니다.
화면에는 password 필드가 없는 MemberProfile을 사용합니다.
기존 places 스키마와 DB 접속 환경변수는 변경하지 않습니다.

## 화면과 접근 정책

| 요청 | 동작 |
| --- | --- |
| GET /signup | DTO를 사용하는 회원가입 폼 |
| POST /signup | Validation 및 서비스 검증 후 등록, /login으로 이동 |
| GET /login | 직접 만든 로그인 화면 |
| POST /login | Spring Security가 인증, 기본 /로 이동 또는 저장된 요청 복원 |
| POST /logout | Spring Security가 세션 무효화 후 /로 이동 |
| GET /my | 인증 필수, 아이디·닉네임·이메일과 여행 기록 준비 안내 |

`/`, `/places`, `/recommend`, `/recommend/search-location`, `/signup`, `/login`, 정적 리소스와
`/api/places/**`, `/api/tourism/**`, `/api/transit/**`, `/api/recommendations/**`는 비회원 접근을 허용합니다.
공개 설정이 미구현 API를 만드는 것은 아닙니다. 미구현 주소는 404입니다.
`/my`, `/my/**`는 인증이 필요합니다. 비회원은 로그인 화면으로 이동합니다.

CSRF 보호를 유지합니다. Thymeleaf `th:action`의 POST 폼에는 CSRF 토큰이 자동 포함됩니다.
현재 JSON POST API가 없어 CSRF 예외는 추가하지 않습니다.
향후 공개 API에 POST를 추가할 때에도 permitAll은 CSRF 면제를 의미하지 않으므로 별도 검토가 필요합니다.

## 입력과 오류 처리

아이디는 필수, 공백 없이 4~30자이며 이메일은 필수·형식 검증·최대 254자입니다.
닉네임은 필수 2~30자, 비밀번호는 필수 8자 이상입니다.
BCrypt의 입력 한계에 맞춰 비밀번호는 UTF-8 기준 72바이트 이내여야 합니다.
비밀번호와 확인이 다르면 등록을 거부합니다.
아이디/이메일 중복은 사전 검사와 DB unique 제약으로 막고, 사용자에게 해당 필드 오류를 표시합니다.
잘못된 입력은 signup 화면에서 수정할 수 있으며 비밀번호를 HTML에 다시 출력하지 않습니다.
로그인 실패 메시지는 '아이디 또는 비밀번호가 올바르지 않습니다.'로 통일합니다.

평문 비밀번호는 저장하거나 로그로 출력하지 않습니다. BCryptPasswordEncoder가 hash를 만들며
Spring Security는 PasswordEncoder.matches로 인증합니다. BCrypt는 복호화하는 암호화가 아닌 단방향 해시입니다.
DB 비밀번호는 기존 DB_PASSWORD 환경변수로만 전달합니다.

## STS 수동 확인

1. 프로젝트 JDK와 Gradle JVM을 17로 지정하고 Gradle > Refresh Gradle Project를 실행합니다.
2. MySQL을 실행하고 README의 DB 생성 절차와 DB_USERNAME, DB_PASSWORD 등 실행 환경변수를 준비합니다.
3. CityRouletteApplication을 Run As > Spring Boot App으로 실행합니다.
4. http://localhost:8080/signup 에서 다음 테스트 전용 정보를 입력합니다.
   아이디 testuser, 비밀번호와 확인 test1234, 이메일 test@example.com, 닉네임 테스트유저.
   이미 등록했다면 다른 아이디와 이메일을 사용합니다. 예시 비밀번호는 실제 계정에 재사용하지 않습니다.
5. /login으로 이동한 뒤 같은 아이디와 비밀번호로 로그인합니다.
6. 홈에서 '테스트유저님', '내 여행', 로그아웃 버튼을 확인합니다.
7. /my에서 아이디·닉네임·이메일을 확인합니다.
8. 로그아웃 버튼을 누른 뒤 /my에 다시 접근하면 /login으로 이동하는지 확인합니다.
9. 비회원 상태에서 /my를 먼저 요청한 뒤 로그인하면 원래 페이지로 돌아가는지도 확인합니다.
10. 잘못된 비밀번호, 중복 아이디/이메일, 짧은 입력, 비밀번호 확인 불일치를 각각 확인합니다.

MySQL Workbench에서 애플리케이션과 같은 DB를 선택하고 확인합니다.

```sql
SELECT id, username, email, display_name, password
FROM members;
```

password가 test1234 같은 평문이 아니라 `$2a$...` 등으로 시작하는 60자 BCrypt hash인지 확인합니다.
hash도 인증 관련 정보이므로 결과를 공개 게시하거나 로그에 복사하지 않습니다.
실제 MySQL/STS GUI 확인은 사용자의 로컬 환경에서 수행합니다. H2 자동 테스트는 MySQL 연결 검증과 다릅니다.

## 평가 기준 및 후속 범위

Controller 제어, HTML Form 입출력, Jakarta Validation, JPA DB 처리, Thymeleaf HTML,
회원가입, 로그인, 회원/비회원 접근 구분을 구현합니다.
실제 여행 기록 저장과 파일 업로드/다운로드는 후속 Issue입니다.
이메일 인증, 비밀번호 재설정, JWT/OAuth, 관리자 페이지 등은 이번 범위에 포함하지 않습니다.

## 개념 설명

- 회원가입은 새 회원 정보를 DB에 만드는 일이고, 로그인은 기존 회원이 맞는지 확인하는 일입니다.
- BCrypt는 비밀번호 원문을 복구하기 어려운 hash로 바꿉니다. 매번 salt를 사용하고 계산 비용이 있어
  DB가 유출되더라도 단순 평문 저장보다 비밀번호 추측에 더 많은 비용이 듭니다.
- Spring Security는 로그인 POST를 받아 DB 회원 조회, 비밀번호 검증, 인증 상태 저장과 접근 제한을 처리합니다.
- Session은 서버에 로그인 상태를 보관합니다. 브라우저가 JSESSIONID 쿠키를 보내면 서버가 해당 세션을 찾습니다.
  로그아웃하면 세션이 무효화되어 이후 요청은 다시 인증해야 합니다.
- Member Entity는 DB의 회원 정보이고 UserDetails는 Security가 인증에 사용하는 아이디·해시·권한 형태입니다.
  DB 모델을 인증 프레임워크에 직접 결합하지 않고 UserDetailsService에서 변환합니다.
- permitAll은 비회원도 접근할 수 있다는 뜻이고 authenticated는 로그인한 사용자만 접근할 수 있다는 뜻입니다.
  두 설정 모두 CSRF와 같은 다른 보안 검사를 자동으로 끄지는 않습니다. 브라우저 폼에는 CSRF 보호를 유지하고,
  기존 외부 호출 호환성을 위해 `POST /api/tourism/sync`만 CSRF 검사에서 제외합니다.
- Controller는 HTTP 입력과 화면, Service는 회원가입 규칙·해시 처리, Repository는 DB 저장·조회에 집중합니다.
  이렇게 나누면 화면을 바꾸거나 규칙을 수정할 때 영향을 줄이고 각 부분을 테스트하기 쉽습니다.

공식 동작 참고: [Spring Security 6.5 Form Login](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/form.html),
[CSRF](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html).

## 자동 검증 결과

`./gradlew clean test`와 `./gradlew clean build` 성공, 전체 165건 통과(실패/오류/건너뜀 0)했습니다.
회원가입·로그인·로그아웃·접근 제어와 기존 관광지·추천·대중교통 기능 회귀 테스트를 포함합니다.
`git diff --check`도 통과했습니다.
MySQL 계정 없이 기존 H2 test 프로필로 실행했습니다.
