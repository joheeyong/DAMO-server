# DAMO Server

**다모(DAMO)** - 통합 콘텐츠 검색 플랫폼 백엔드 API 서버

## 기술 스택

| 분류 | 기술 |
|------|------|
| Framework | Spring Boot 3.2.5 |
| Language | Java 17 |
| Database | MySQL 8.0 (AWS RDS) |
| Auth | Spring Security + JWT (JJWT 0.12.5) |
| OAuth | Google, Naver (Authorization Code Flow) |
| HTTP Client | Spring WebFlux WebClient |
| Push | Firebase Admin SDK (FCM) |
| Server | AWS EC2 (t4g.micro) |
| CI/CD | GitHub Actions |

## 프로젝트 구조

```
src/main/java/com/luxrobo/demoapi/
├── DemoApiApplication.java
├── config/
│   ├── SecurityConfig.java          # Spring Security + CORS + JWT 필터
│   ├── WebConfig.java               # 웹 설정
│   ├── FirebaseConfig.java          # Firebase Admin SDK
│   ├── RateLimitFilter.java         # IP 기반 요청 제한
│   └── SecurityHeaderFilter.java    # 보안 헤더
├── controller/
│   ├── HealthController.java        # 헬스 체크
│   ├── AuthController.java          # OAuth 로그인 + 사용자 정보
│   ├── UserController.java          # 사용자 CRUD
│   ├── FcmController.java           # 푸시 알림
│   └── SearchController.java        # 통합 검색
├── service/
│   ├── OAuthService.java            # Google/Naver OAuth 토큰 교환
│   ├── NaverSearchService.java      # 네이버 검색 API
│   ├── YouTubeSearchService.java    # YouTube Data API v3
│   ├── RedditSearchService.java     # Reddit 공개 JSON API
│   ├── UserService.java
│   └── FcmService.java
├── security/
│   ├── JwtProvider.java             # JWT 생성/검증
│   └── JwtAuthenticationFilter.java # Bearer 토큰 필터
├── entity/
│   ├── User.java                    # 사용자 (OAuth 연동)
│   └── DeviceToken.java             # FCM 디바이스 토큰
└── repository/
    ├── UserRepository.java
    └── DeviceTokenRepository.java
```

## API 엔드포인트

### 인증 (Auth)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| `POST` | `/api/auth/google` | Google OAuth 로그인 | - |
| `POST` | `/api/auth/naver` | Naver OAuth 로그인 | - |
| `GET` | `/api/auth/me` | 내 정보 조회 | Bearer |
| `PUT` | `/api/auth/interests` | 관심사 설정 | Bearer |

### 검색 (Search)

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/search/all?query={q}&display={n}` | 통합 검색 (네이버+유튜브+Shorts+Reddit) |
| `GET` | `/api/search/trending?display={n}` | 추천/트렌딩 피드 |
| `GET` | `/api/search/{category}?query={q}` | 카테고리별 검색 |

> 카테고리: `blog`, `news`, `cafe`, `shop`, `image`, `kin`, `book`, `webkr`, `youtube`, `shorts`, `reddit`

### 사용자 / 푸시

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/fcm/register` | FCM 디바이스 토큰 등록 |
| `POST` | `/api/fcm/send` | 푸시 알림 전송 |
| `GET` | `/health` | 서버 상태 확인 |

## 인증 플로우

```
[클라이언트]                    [백엔드]                     [Google/Naver]
    │                            │                              │
    │── OAuth 로그인 요청 ──────>│                              │
    │                            │── Authorization Code ──────>│
    │                            │<── Access Token ────────────│
    │                            │── 사용자 정보 요청 ────────>│
    │                            │<── 이름/이메일/사진 ────────│
    │                            │                              │
    │                            │ DB Upsert + JWT 생성         │
    │<── { token, user } ────────│                              │
    │                            │                              │
    │── Bearer JWT ────────────>│ (이후 모든 인증 요청)         │
```

## 환경변수

| 변수 | 설명 |
|------|------|
| `DB_URL` | MySQL 접속 URL |
| `DB_USERNAME` | DB 사용자명 |
| `DB_PASSWORD` | DB 비밀번호 |
| `NAVER_SEARCH_CLIENT_ID` | 네이버 검색 API Client ID |
| `NAVER_SEARCH_CLIENT_SECRET` | 네이버 검색 API Client Secret |
| `YOUTUBE_API_KEY` | YouTube Data API Key |
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth Client Secret |
| `NAVER_LOGIN_CLIENT_ID` | 네이버 로그인 Client ID |
| `NAVER_LOGIN_CLIENT_SECRET` | 네이버 로그인 Client Secret |
| `JWT_SECRET` | JWT 서명 키 (256bit+) |
| `FIREBASE_CREDENTIALS` | Firebase 서비스 계정 JSON 경로 |

## 로컬 실행

```bash
export DB_URL=jdbc:mysql://localhost:3306/dadoc
export DB_USERNAME=root
export DB_PASSWORD=password
# ... 기타 환경변수

./gradlew bootRun
```

## 배포

```bash
# 빌드
./gradlew build -x test

# EC2 배포
scp -i ~/.ssh/dadoc-key.pem build/libs/demo-api-0.0.1-SNAPSHOT.jar ec2-user@54.180.179.231:/home/ec2-user/

# 재시작
ssh -i ~/.ssh/dadoc-key.pem ec2-user@54.180.179.231 'pkill -f demo-api; nohup bash /home/ec2-user/start-app.sh > /tmp/app.log 2>&1 &'
```

## 인프라

| 서비스 | 스펙 | 비고 |
|--------|------|------|
| EC2 | t4g.micro | IP: 54.180.179.231 |
| RDS | db.t4g.micro, MySQL 8.0 | 20GB, 자동 백업 |
| 리전 | ap-northeast-2 (서울) | |

## 보안

- **JWT**: Stateless 세션, 24시간 유효기간
- **CORS**: `damo-web.vercel.app`, `localhost:3000`
- **Rate Limit**: IP당 분당 100회
- **Security Headers**: XSS, 클릭재킹, MIME 스니핑 방지
- **민감정보**: 환경변수 관리

## 관련 레포지토리

| 서비스 | 레포 |
|--------|------|
| 웹 (React) | [DAMO-web](https://github.com/joheeyong/DAMO-web) |
| 앱 (Flutter) | [DAMO-flutter](https://github.com/joheeyong/DAMO-flutter) |
