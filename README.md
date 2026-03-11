# DAMO Server

Spring Boot 기반 백엔드 API 서버

## 기술 스택

- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Database**: MySQL 8.0 (AWS RDS)
- **Server**: AWS EC2 (t4g.micro)
- **CI/CD**: GitHub Actions
- **IDE**: IntelliJ IDEA

## 아키텍처 및 패턴

- **Architecture**: Layered Architecture (Controller → Service → Repository)
- **인증/인가**: Spring Security + JWT
- **API 문서**: Swagger (SpringDoc OpenAPI)
- **유효성 검증**: Bean Validation (`@Valid`)
- **에러 처리**: Global Exception Handler (`@RestControllerAdvice`)
- **데이터 전달**: DTO 패턴 (Request/Response 분리)
- **환경 분리**: Spring Profile (dev / prod)
- **보안**: Rate Limiting (Bucket4j), Security Headers

## 프로젝트 구조

```
src/main/java/com/luxrobo/demoapi/
├── DemoApiApplication.java          # 메인 애플리케이션
├── config/                          # 설정
│   ├── WebConfig.java               # CORS 설정
│   ├── FirebaseConfig.java          # Firebase Admin SDK 초기화
│   ├── SecurityConfig.java          # Spring Security 설정 (예정)
│   ├── SwaggerConfig.java           # Swagger 설정 (예정)
│   ├── RateLimitFilter.java         # 요청 제한 필터
│   └── SecurityHeaderFilter.java    # 보안 헤더 필터
├── controller/                      # API 엔드포인트
│   ├── HealthController.java
│   ├── UserController.java
│   └── FcmController.java
├── service/                         # 비즈니스 로직
│   ├── UserService.java
│   └── FcmService.java
├── repository/                      # DB 접근
│   ├── UserRepository.java
│   └── DeviceTokenRepository.java
├── entity/                          # DB 테이블 매핑
│   ├── User.java
│   └── DeviceToken.java
├── dto/                             # Request/Response 객체
│   ├── request/
│   └── response/
├── exception/                       # 에러 처리
│   ├── GlobalExceptionHandler.java
│   └── CustomException.java
└── security/                        # JWT, 인증/인가
    ├── JwtProvider.java
    └── JwtAuthenticationFilter.java
```

## API 목록

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/` | Welcome 메시지 |
| `GET` | `/health` | 서버 상태 확인 |
| `POST` | `/api/users` | 유저 생성 |
| `GET` | `/api/users` | 전체 유저 조회 |
| `GET` | `/api/users/{id}` | 특정 유저 조회 |
| `DELETE` | `/api/users/{id}` | 유저 삭제 |
| `POST` | `/api/fcm/register` | FCM 디바이스 토큰 등록 |
| `GET` | `/api/fcm/tokens` | 등록된 디바이스 목록 |
| `POST` | `/api/fcm/send` | 푸시 알림 전송 |

### 요청 예시

**유저 생성**
```
POST /api/users
Content-Type: application/json

{
  "name": "홍길동",
  "email": "hong@test.com"
}
```

## 로컬 실행

```bash
# 환경변수 설정
export DB_URL=jdbc:mysql://<DB_HOST>:3306/dadoc
export DB_USERNAME=admin
export DB_PASSWORD=<PASSWORD>

# 실행
./gradlew bootRun
```

## 배포

`main` 브랜치에 push하면 GitHub Actions를 통해 자동 배포됩니다.

```
코드 push → 빌드 → EC2 업로드 → systemd 재시작
```

## 인프라

| 서비스 | 리소스 | 비고 |
|--------|--------|------|
| EC2 | t4g.micro | Elastic IP: 54.180.179.231 |
| RDS | db.t4g.micro, MySQL 8.0 | 20GB, 자동 백업 활성화 |
| 리전 | ap-northeast-2 (서울) | - |
| 모니터링 | CloudWatch | CPU/상태 체크 알림 → 이메일 |

## 보안

- **Rate Limiting**: IP당 분당 100회, 시간당 1000회
- **Security Headers**: XSS 방어, 클릭재킹 방어, MIME 스니핑 방지
- **SQL Injection**: JPA 파라미터 바인딩으로 방어
- **DB 비밀번호**: 환경변수로 관리 (코드에 미포함)
- **CORS**: `damo-web.vercel.app`, `localhost:3000` 허용
- **EC2**: systemd 서비스로 자동 재시작 설정

## 관련 레포지토리

| 서비스 | 레포 |
|--------|------|
| 앱 (Flutter) | [DAMO-flutter](https://github.com/joheeyong/DAMO-flutter) |
| 웹 (React) | [DAMO-web](https://github.com/joheeyong/DAMO-web) |
