# DAMO Server

Spring Boot 기반 백엔드 API 서버

## 기술 스택

- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Database**: MySQL 8.0 (AWS RDS)
- **Server**: AWS EC2 (t4g.micro)
- **CI/CD**: GitHub Actions

## API 목록

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/` | Welcome 메시지 |
| `GET` | `/health` | 서버 상태 확인 |
| `POST` | `/api/users` | 유저 생성 |
| `GET` | `/api/users` | 전체 유저 조회 |
| `GET` | `/api/users/{id}` | 특정 유저 조회 |
| `DELETE` | `/api/users/{id}` | 유저 삭제 |

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

## 프로젝트 구조

```
src/main/java/com/luxrobo/demoapi/
├── DemoApiApplication.java          # 메인 애플리케이션
├── controller/
│   ├── HealthController.java        # 헬스체크, 홈
│   └── UserController.java          # 유저 CRUD
├── entity/
│   └── User.java                    # 유저 엔티티
└── repository/
    └── UserRepository.java          # 유저 레포지토리
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
코드 push → 빌드 → EC2 업로드 → 서버 재시작
```

## 인프라

| 서비스 | 리소스 | 비고 |
|--------|--------|------|
| EC2 | t4g.micro | Elastic IP 할당 |
| RDS | db.t4g.micro, MySQL 8.0 | 20GB |
| 리전 | ap-northeast-2 (서울) | - |
