# DAMO 시스템 아키텍처

## 전체 구성

```
┌──────────────────────────────────────────────────────────────┐
│                        클라이언트                              │
│                                                              │
│  ┌─────────────┐     ┌──────────────────────────────────┐   │
│  │ Flutter App  │     │          React Web                │   │
│  │ (WebView)   │────>│    damo-web.vercel.app            │   │
│  │ + FCM 푸시   │     │                                  │   │
│  └─────────────┘     └───────────────┬──────────────────┘   │
│                                       │                      │
└───────────────────────────────────────┼──────────────────────┘
                                        │ HTTPS (Vercel Rewrites)
                                        ▼
┌───────────────────────────────────────────────────────────────┐
│                    Spring Boot API Server                      │
│                    54.180.179.231:8080                         │
│                                                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐ │
│  │  Auth    │  │  Search  │  │   FCM    │  │    User      │ │
│  │ Controller│  │Controller│  │Controller│  │  Controller  │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘ │
│       │              │              │               │         │
│  ┌────┴─────┐  ┌────┴──────────────┴───┐    ┌─────┴───────┐ │
│  │  OAuth   │  │    Search Services    │    │   User      │ │
│  │ Service  │  │ Naver│YouTube│Reddit  │    │  Service    │ │
│  └────┬─────┘  └──────────────────────┘    └──────┬──────┘ │
│       │                                            │         │
│  ┌────┴────────────────────────────────────────────┴───────┐ │
│  │                    MySQL (AWS RDS)                       │ │
│  │                  users, device_tokens                    │ │
│  └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
   ┌────────────┐  ┌────────────┐  ┌────────────┐
   │Google OAuth│  │Naver APIs  │  │YouTube API │
   │            │  │Search+Login│  │  Data v3   │
   └────────────┘  └────────────┘  └────────────┘
```

## 인증 플로우

```
1. 사용자가 Google/Naver 로그인 클릭
2. OAuth 제공자의 로그인 페이지로 리다이렉트
3. 사용자 인증 후 Authorization Code와 함께 콜백 URL로 리다이렉트
4. 프론트엔드가 code를 백엔드 /api/auth/{provider}로 전송
5. 백엔드가 code → access_token 교환
6. access_token으로 사용자 정보 조회 (이름, 이메일, 프로필)
7. DB에 사용자 upsert (최초 → INSERT, 재로그인 → UPDATE)
8. JWT 토큰 발급 (24시간 유효)
9. 프론트엔드가 JWT를 localStorage에 저장
10. 이후 모든 인증 요청에 Authorization: Bearer {JWT} 헤더 포함
```

## 추천 피드 로직

```
초기 로딩 (fetchTrending):
├── 트렌딩 API 호출 (YouTube 인기 + 네이버 뉴스/블로그/쇼핑)
├── 사용자 관심사 중 랜덤 2개 키워드로 검색 (병렬)
├── 결과 병합 (트렌딩 3개마다 관심사 콘텐츠 삽입)
└── 중복 제거 후 렌더링

인피니티 스크롤 (fetchMoreTrending):
├── 관심사 키워드 70% / 바이럴 키워드 30% 확률로 선택
├── 선택된 키워드로 searchAll API 호출
├── 기존 아이템과 중복 제거 후 추가
└── 8개마다 개인화 배너 삽입 ("XXX님에게 딱 맞는 YYY")
```

## 데이터 모델

### users 테이블
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK, AUTO) | |
| name | VARCHAR | 사용자 이름 |
| email | VARCHAR | 이메일 |
| provider | VARCHAR | `google` / `naver` |
| providerId | VARCHAR | OAuth 제공자 사용자 ID |
| profileImage | VARCHAR | 프로필 이미지 URL |
| interests | VARCHAR(1000) | 관심사 (콤마 구분) |

> UNIQUE 제약: (provider, providerId)

### device_tokens 테이블
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK, AUTO) | |
| token | VARCHAR | FCM 디바이스 토큰 |
| platform | VARCHAR | `android` / `ios` |

## 외부 API 연동

| API | 용도 | 인증 |
|-----|------|------|
| Naver Search API | 블로그/뉴스/카페/쇼핑/이미지/지식iN/도서/웹문서 검색 | Client ID + Secret |
| YouTube Data API v3 | 동영상 검색, 인기 동영상, Shorts | API Key |
| Reddit JSON API | 인기글, 검색 (공개 엔드포인트) | User-Agent |
| Google OAuth 2.0 | 사용자 인증 | Client ID + Secret |
| Naver Login API | 사용자 인증 | Client ID + Secret |
| Firebase Admin SDK | FCM 푸시 알림 전송 | Service Account JSON |
