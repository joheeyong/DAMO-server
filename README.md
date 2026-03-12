# DAMO Server

**다모(DAMO)** - 통합 콘텐츠 검색 플랫폼 백엔드 API 서버

## 기술 스택

| 분류 | 기술 |
|------|------|
| Framework | Spring Boot 3.2.5 |
| Language | Java 17 |
| Database | MySQL 8.0 (AWS RDS) |
| Auth | Spring Security + JWT (JJWT 0.12.5) |
| OAuth | Google, Naver, Kakao (Authorization Code Flow) |
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
│   ├── SearchController.java        # 통합 검색
│   └── ActivityController.java      # 사용자 활동 추적 + 추천 랭킹
├── service/
│   ├── OAuthService.java            # Google/Naver/Kakao OAuth 토큰 교환
│   ├── NotificationScheduler.java   # 관심사 알림 스케줄러 (1시간 간격)
│   ├── NaverSearchService.java      # 네이버 검색 API (8개 카테고리)
│   ├── YouTubeSearchService.java    # YouTube Data API v3
│   ├── RedditSearchService.java     # Reddit 공개 JSON API
│   ├── KakaoSearchService.java      # 카카오 Daum 검색 API (5개 카테고리)
│   ├── InstagramSearchService.java  # Instagram Graph API (해시태그 검색)
│   ├── RecommendationService.java   # 개인화 추천 알고리즘
│   └── FcmService.java
├── security/
│   ├── JwtProvider.java             # JWT 생성/검증
│   └── JwtAuthenticationFilter.java # Bearer 토큰 필터
├── entity/
│   ├── User.java                    # 사용자 (OAuth 연동)
│   ├── DeviceToken.java             # FCM 디바이스 토큰
│   ├── UserSearchHistory.java       # 사용자 검색 기록
│   └── UserClickHistory.java        # 사용자 클릭 기록
└── repository/
    ├── UserRepository.java
    ├── DeviceTokenRepository.java
    ├── UserSearchHistoryRepository.java
    └── UserClickHistoryRepository.java
```

## API 엔드포인트

### 인증 (Auth)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| `POST` | `/api/auth/google` | Google OAuth 로그인 | - |
| `POST` | `/api/auth/naver` | Naver OAuth 로그인 | - |
| `POST` | `/api/auth/kakao` | Kakao OAuth 로그인 | - |
| `GET` | `/api/auth/me` | 내 정보 조회 | Bearer |
| `PUT` | `/api/auth/interests` | 관심사 설정 | Bearer |

### 검색 (Search)

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/search/all?query={q}&display={n}` | 통합 검색 (네이버+카카오+유튜브+Shorts+Reddit+Instagram) |
| `GET` | `/api/search/trending?display={n}` | 추천/트렌딩 피드 |
| `GET` | `/api/search/{category}?query={q}` | 카테고리별 검색 |

> 카테고리: `blog`, `news`, `cafe`, `shop`, `image`, `kin`, `book`, `webkr`, `youtube`, `shorts`, `reddit`, `instagram`, `kakao-blog`, `kakao-cafe`, `kakao-web`, `kakao-video`, `kakao-image`

### 사용자 활동 / 추천 (Activity)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| `POST` | `/api/activity/search` | 검색 기록 저장 | Bearer |
| `POST` | `/api/activity/click` | 클릭 기록 저장 | Bearer |
| `POST` | `/api/activity/rank` | 개인화 랭킹 요청 | Bearer |

### 사용자 / 푸시

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/fcm/register` | FCM 디바이스 토큰 등록 |
| `POST` | `/api/fcm/send` | 푸시 알림 전송 |
| `GET` | `/health` | 서버 상태 확인 |

## 추천 알고리즘

사용자의 관심사, 검색 기록, 클릭 기록을 기반으로 콘텐츠를 개인화 랭킹합니다.

### 데이터 소스

| 소스 | 데이터 | 수집 |
|------|--------|------|
| 관심사 | `users.interests` | 사용자 직접 설정 |
| 검색 기록 | `user_search_history` (최근 50건) | 검색 시 자동 저장 |
| 클릭 기록 | `user_click_history` (최근 100건) | 클릭 시 자동 저장 |

### 스코어링

| 신호 | 가중치 | 설명 |
|------|--------|------|
| 관심사 매칭 | +10 | 관심사 키워드와 콘텐츠 매칭 |
| 검색 기록 매칭 | +5 (시간감쇠) | 최근 검색어와 콘텐츠 매칭 |
| 클릭 키워드 유사도 | +7 (시간감쇠) | 이전 클릭 콘텐츠의 키워드 매칭 |
| 플랫폼 선호도 | +0.3/클릭 | 자주 클릭하는 플랫폼 우선 |
| 이미 본 콘텐츠 | -15 | 중복 방지 |

- **시간 감쇠**: 반감기 72시간 (`decay = 1 / (1 + hoursAgo / 72)`)
- **비로그인**: 기본 셔플 순서 유지
- **콜드 스타트**: 관심사 설정만으로 동작, 사용할수록 정교해짐

## 인증 플로우

```
[클라이언트]                    [백엔드]                     [Google/Naver/Kakao]
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
| `KAKAO_REST_API_KEY` | 카카오 REST API 키 (검색 + OAuth Client ID) |
| `KAKAO_CLIENT_SECRET` | 카카오 OAuth Client Secret |
| `INSTAGRAM_ACCESS_TOKEN` | Instagram Graph API 액세스 토큰 |
| `INSTAGRAM_USER_ID` | Instagram 비즈니스 사용자 ID |

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
- **인증 엔드포인트**: `/api/activity/**`, `/api/me/**` (Bearer 토큰 필수)

## 관련 레포지토리

| 서비스 | 레포 |
|--------|------|
| 웹 (React) | [DAMO-web](https://github.com/joheeyong/DAMO-web) |
| 앱 (Flutter) | [DAMO-flutter](https://github.com/joheeyong/DAMO-flutter) |
