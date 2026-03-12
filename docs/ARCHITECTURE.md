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
│  │ + 뒤로가기   │     │                                  │   │
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
│  │  Auth    │  │  Search  │  │ Activity │  │    FCM       │ │
│  │Controller│  │Controller│  │Controller│  │  Controller  │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘ │
│       │              │              │               │         │
│  ┌────┴─────┐  ┌────┴─────────────┐  ┌────┴──────┐          │
│  │  OAuth   │  │  Search Services │  │Recommend- │          │
│  │ Service  │  │ Naver │YouTube   │  │ation      │          │
│  │          │  │ Kakao │Reddit    │  │ Service   │          │
│  │          │  │ Instagram        │  │           │          │
│  └────┬─────┘  └─────────────────┘  └────┬──────┘          │
│       │                                    │                 │
│  ┌────┴────────────────────────────────────┴───────────────┐ │
│  │                    MySQL (AWS RDS)                       │ │
│  │  users, device_tokens,                                  │ │
│  │  user_search_history, user_click_history                │ │
│  └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
                          │
     ┌────────────────────┼────────────────────┐
     ▼                    ▼                    ▼
┌──────────┐  ┌──────────────────┐  ┌──────────────────┐
│Google    │  │Naver APIs        │  │YouTube API       │
│OAuth     │  │Search + Login    │  │Data v3           │
└──────────┘  └──────────────────┘  └──────────────────┘
     ▼                    ▼                    ▼
┌──────────┐  ┌──────────────────┐  ┌──────────────────┐
│Kakao     │  │Reddit            │  │Instagram         │
│Daum API  │  │JSON API          │  │Graph API         │
└──────────┘  └──────────────────┘  └──────────────────┘
```

## 검색 데이터 흐름

```
사용자 검색 "축구"
    │
    ▼
SearchController.searchAll("축구", 5)
    │
    ├── NaverSearchService.searchAll()      → 8개 카테고리 병렬 검색
    │   ├── blog, news, cafe, shop
    │   ├── image, kin, book, webkr
    │
    ├── KakaoSearchService.searchAll()      → 5개 카테고리 병렬 검색
    │   ├── kakao-blog, kakao-cafe, kakao-web
    │   ├── kakao-video, kakao-image
    │
    ├── YouTubeSearchService.search()       → 동영상 검색
    ├── YouTubeSearchService.searchShorts() → Shorts 검색
    ├── RedditSearchService.search()        → Reddit 검색
    ├── InstagramSearchService.searchByHashtag() → 해시태그 검색
    │
    ▼
프론트엔드 normalizeItems() → 통합 포맷으로 변환
    │
    ▼
ActivityController.rank() → 개인화 랭킹 (로그인 시)
    │
    ▼
피드 렌더링 (17개 소스 통합)
```

## 추천 알고리즘 흐름

```
사용자 피드 요청
    │
    ▼
RecommendationService.rankItems(userId, items)
    │
    ├── 1. DB 조회 (3 쿼리)
    │   ├── User.interests        → "축구,영화,IT"
    │   ├── 최근 검색 50건        → ["맛집", "여행", ...]
    │   └── 최근 클릭 100건       → [{contentId, platform, sourceKeyword}, ...]
    │
    ├── 2. 프로필 구성
    │   ├── interests[]           → 명시적 관심사
    │   ├── searchAffinity{}      → 검색 키워드별 가중치 (시간감쇠)
    │   ├── platformAffinity{}    → 플랫폼별 클릭 가중치
    │   ├── keywordAffinity{}     → 클릭 키워드별 가중치
    │   └── clickedContentIds{}   → 이미 본 콘텐츠 ID
    │
    ├── 3. 스코어링 (각 콘텐츠)
    │   ├── 관심사 매칭     → +10점
    │   ├── 검색 기록 매칭  → +5점 × decay
    │   ├── 플랫폼 선호도   → +0.3점 × 클릭수
    │   ├── 키워드 유사도   → +7점 × decay
    │   ├── 이미 본 콘텐츠  → -15점
    │   └── 랜덤 지터       → +0~0.5점
    │
    └── 4. 점수 내림차순 정렬 → rankedIds 반환
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
├── 트렌딩 API 호출 (YouTube 인기 + 네이버 뉴스/블로그/쇼핑 + 카카오 블로그/카페 + Reddit + Instagram)
├── 사용자 관심사 중 랜덤 2개 키워드로 searchAll 병렬 검색
├── 결과 병합 (트렌딩 → 관심사 블록 삽입)
├── 개인화 랭킹 적용 (로그인 시)
└── 중복 제거 후 렌더링

인피니티 스크롤 (fetchMoreTrending):
├── 관심사 키워드 70% / 바이럴 키워드 30% 확률로 선택
├── 선택된 키워드로 searchAll API 호출
├── 개인화 랭킹 적용 (로그인 시)
├── 기존 아이템과 중복 제거 후 추가
└── sourceKeyword 기반 개인화 배너 삽입
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

### user_search_history 테이블
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK, AUTO) | |
| userId | BIGINT | 사용자 ID |
| query | VARCHAR | 검색어 |
| searchedAt | DATETIME | 검색 시간 |

> INDEX: (userId, searchedAt)

### user_click_history 테이블
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK, AUTO) | |
| userId | BIGINT | 사용자 ID |
| contentId | VARCHAR | 콘텐츠 ID (e.g. `youtube-abc123`) |
| platform | VARCHAR | 플랫폼 (e.g. `youtube`, `blog`) |
| sourceKeyword | VARCHAR | 검색 키워드 출처 |
| clickedAt | DATETIME | 클릭 시간 |

> INDEX: (userId, clickedAt)

## 외부 API 연동

| API | 용도 | 인증 | 카테고리 |
|-----|------|------|----------|
| Naver Search API | 블로그/뉴스/카페/쇼핑/이미지/지식iN/도서/웹문서 | Client ID + Secret | 8개 |
| Kakao Daum Search API | 블로그/카페/웹/영상/이미지 | REST API Key | 5개 |
| YouTube Data API v3 | 동영상 검색, 인기 동영상, Shorts | API Key | 2개 |
| Reddit JSON API | 인기글, 검색 (공개 엔드포인트) | User-Agent | 1개 |
| Instagram Graph API | 해시태그 기반 게시물 검색 | Access Token | 1개 |
| Google OAuth 2.0 | 사용자 인증 | Client ID + Secret | - |
| Naver Login API | 사용자 인증 | Client ID + Secret | - |
| Firebase Admin SDK | FCM 푸시 알림 전송 | Service Account JSON | - |

> 총 17개 검색 카테고리 통합
