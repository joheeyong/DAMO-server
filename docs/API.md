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

통합 검색 (네이버 + YouTube + Shorts + Reddit)

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
  "reddit": "{\"data\": {\"children\": [...]}}"
}
```

---

### GET `/api/search/trending`

추천/트렌딩 피드

**Parameters:**
| 이름 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `display` | int | 10 | 카테고리당 결과 수 |

---

### GET `/api/search/{category}`

카테고리별 검색

**카테고리:** `blog`, `news`, `cafe`, `shop`, `image`, `kin`, `book`, `webkr`, `youtube`, `shorts`, `reddit`

**Parameters:**
| 이름 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `query` | string | (필수) | 검색어 |
| `display` | int | 10 | 결과 수 |
| `start` | int | 1 | 시작 위치 |
| `sort` | string | sim | 정렬 (sim/date) |

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
- **인증 필요**: `/api/me/**`
- **토큰 형식**: `Authorization: Bearer {JWT}`
- **JWT 유효기간**: 24시간
