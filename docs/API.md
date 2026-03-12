# DAMO API Reference

**Base URL**: `http://54.180.179.231:8080`

---

## 인증 (Auth)

### POST `/api/auth/google`

Google OAuth 로그인

**Request Body:**
```json
{
  "code": "4/0AX4XfWh...",
  "redirectUri": "https://damo-web.vercel.app/auth/google/callback"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "name": "홍길동",
    "email": "hong@gmail.com",
    "profileImage": "https://lh3.googleusercontent.com/...",
    "provider": "google",
    "interests": "맛집,여행,IT"
  }
}
```

---

### POST `/api/auth/naver`

Naver OAuth 로그인

**Request Body:**
```json
{
  "code": "oAu3K...",
  "state": "random_state_string",
  "redirectUri": "https://damo-web.vercel.app/auth/naver/callback"
}
```

**Response:** 위 Google 응답과 동일한 형식

---

### POST `/api/auth/kakao`

Kakao OAuth 로그인

**Request Body:**
```json
{
  "code": "auth_code...",
  "redirectUri": "https://damo-web.vercel.app/auth/kakao/callback"
}
```

**Response:** 위 Google 응답과 동일한 형식 (`provider: "kakao"`)

---

### GET `/api/auth/me`

현재 로그인된 사용자 정보 조회

**Headers:** `Authorization: Bearer {JWT}`

**Response (인증됨):**
```json
{
  "id": 1,
  "name": "홍길동",
  "email": "hong@gmail.com",
  "profileImage": "https://...",
  "provider": "google",
  "interests": "맛집,여행,IT"
}
```

**Response (미인증):**
```json
{
  "error": "Unauthorized",
  "status": 403
}
```

---

### PUT `/api/auth/interests`

관심사 설정/변경

**Headers:** `Authorization: Bearer {JWT}`

**Request Body:**
```json
{
  "interests": ["맛집", "여행", "IT", "영화"]
}
```

**Response:**
```json
{
  "interests": "맛집,여행,IT,영화"
}
```

---

## 검색 (Search)

### GET `/api/search/all`

통합 검색 (네이버 + 카카오 + YouTube + Shorts + Reddit + Instagram)

**Parameters:**
| 이름 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `query` | string | (필수) | 검색어 |
| `display` | int | 5 | 카테고리당 결과 수 |

**Response:** 각 카테고리별 원본 JSON을 key-value로 반환
```json
{
  "blog": "{\"items\": [...]}",
  "news": "{\"items\": [...]}",
  "youtube": "{\"items\": [...]}",
  "shorts": "{\"items\": [...]}",
  "reddit": "{\"data\": {\"children\": [...]}}",
  "instagram": "{\"data\": [...]}",
  "kakao-blog": "{\"documents\": [...]}",
  "kakao-cafe": "{\"documents\": [...]}",
  "kakao-web": "{\"documents\": [...]}",
  "kakao-video": "{\"documents\": [...]}",
  "kakao-image": "{\"documents\": [...]}"
}
```

---

### GET `/api/search/trending`

추천/트렌딩 피드

**Parameters:**
| 이름 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `display` | int | 10 | 카테고리당 결과 수 |

**Response:** searchAll과 동일 형식 + `keyword` 필드 포함

---

### GET `/api/search/{category}`

카테고리별 검색

**카테고리:**
- 네이버: `blog`, `news`, `cafe`, `shop`, `image`, `kin`, `book`, `webkr`
- 카카오: `kakao-blog`, `kakao-cafe`, `kakao-web`, `kakao-video`, `kakao-image`
- 기타: `youtube`, `shorts`, `reddit`, `instagram`

**Parameters:**
| 이름 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `query` | string | (필수) | 검색어 |
| `display` | int | 10 | 결과 수 |
| `start` | int | 1 | 시작 위치 (네이버만) |
| `sort` | string | sim | 정렬 (sim/date, 네이버만) |

---

## 사용자 활동 / 추천 (Activity)

### POST `/api/activity/search`

검색 기록 저장

**Headers:** `Authorization: Bearer {JWT}`

**Request Body:**
```json
{
  "query": "축구 하이라이트"
}
```

**Response:**
```json
{
  "status": "ok"
}
```

---

### POST `/api/activity/click`

클릭 기록 저장

**Headers:** `Authorization: Bearer {JWT}`

**Request Body:**
```json
{
  "contentId": "youtube-abc123",
  "platform": "youtube",
  "sourceKeyword": "축구"
}
```

**Response:**
```json
{
  "status": "ok"
}
```

---

### POST `/api/activity/rank`

개인화 랭킹 요청 — 사용자 프로필 기반으로 콘텐츠 순서 최적화

**Headers:** `Authorization: Bearer {JWT}`

**Request Body:**
```json
{
  "items": [
    { "id": "youtube-abc", "platform": "youtube", "title": "축구 하이라이트", "sourceKeyword": "축구" },
    { "id": "blog-0-http://...", "platform": "blog", "title": "맛집 추천", "sourceKeyword": "맛집" }
  ]
}
```

**Response:**
```json
{
  "rankedIds": ["youtube-abc", "blog-0-http://..."]
}
```

---

## 푸시 알림 (FCM)

### POST `/api/fcm/register`

디바이스 토큰 등록

**Request Body:**
```json
{
  "token": "cK7Rq...",
  "platform": "android"
}
```

### POST `/api/fcm/send`

푸시 알림 전송

**Request Body:**
```json
{
  "title": "새 소식",
  "body": "새로운 콘텐츠가 있습니다"
}
```

### GET `/api/fcm/tokens`

등록된 디바이스 토큰 목록

---

## 공통

### GET `/health`

서버 상태 확인

```json
{
  "status": "UP",
  "timestamp": "2026-03-12T01:30:44.552"
}
```

---

## 에러 응답

```json
{
  "timestamp": "2026-03-12T01:30:37.410+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/auth/me"
}
```

## 인증 방식

- **공개 엔드포인트**: `/`, `/health`, `/api/auth/**`, `/api/search/**`, `/api/fcm/**`, `/api/users/**`
- **인증 필요**: `/api/me/**`, `/api/activity/**`
- **토큰 형식**: `Authorization: Bearer {JWT}`
- **JWT 유효기간**: 24시간
