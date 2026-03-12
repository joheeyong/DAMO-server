# 배포 가이드

## 서비스 구성

| 서비스 | 배포 위치 | 방식 |
|--------|----------|------|
| Backend (Spring Boot) | AWS EC2 | 수동 SCP + 재시작 |
| Web (React) | Vercel | Git push 자동 배포 |
| App (Flutter) | Firebase App Distribution | 수동 빌드 + 업로드 |

## Backend 배포

### 1. 빌드

```bash
cd demo-api
./gradlew build -x test
```

### 2. EC2 업로드

```bash
scp -i ~/.ssh/dadoc-key.pem \
  build/libs/demo-api-0.0.1-SNAPSHOT.jar \
  ec2-user@54.180.179.231:/home/ec2-user/
```

### 3. 재시작

```bash
ssh -i ~/.ssh/dadoc-key.pem ec2-user@54.180.179.231

# 프로세스 종료 후 재시작
pkill -f demo-api
nohup bash /home/ec2-user/start-app.sh > /tmp/app.log 2>&1 &

# 헬스체크
curl http://localhost:8080/health
```

### EC2 파일 구조

```
/home/ec2-user/
├── demo-api-0.0.1-SNAPSHOT.jar    # 실행 파일
├── start-app.sh                    # 시작 스크립트 (env 로딩 + java -jar)
├── env.sh                          # 환경변수 모음
├── firebase-credentials.json       # Firebase 서비스 계정
└── app.log                         # 애플리케이션 로그
```

### env.sh 구성

```bash
export DB_URL=jdbc:mysql://...
export DB_USERNAME=admin
export DB_PASSWORD=***
export FIREBASE_CREDENTIALS=/home/ec2-user/firebase-credentials.json
export NAVER_SEARCH_CLIENT_ID=***
export NAVER_SEARCH_CLIENT_SECRET=***
export YOUTUBE_API_KEY=***
export GOOGLE_CLIENT_ID=***
export GOOGLE_CLIENT_SECRET=***
export NAVER_LOGIN_CLIENT_ID=***
export NAVER_LOGIN_CLIENT_SECRET=***
export JWT_SECRET=***
```

## Web 배포

Vercel에 GitHub 레포가 연결되어 있어 `main` push 시 자동 배포됩니다.

```bash
cd damo-web
git push origin main
# → Vercel이 자동으로 빌드 + 배포
# → https://damo-web.vercel.app 에서 확인
```

### Vercel 환경변수

| 변수 | 값 |
|------|-----|
| `REACT_APP_API_BASE_URL` | `http://54.180.179.231:8080` |

## Flutter 앱 배포

### Android APK 빌드

```bash
cd damo_flutter
flutter build apk --release
```

### Firebase App Distribution 배포

```bash
firebase appdistribution:distribute \
  build/app/outputs/flutter-apk/app-release.apk \
  --project damo-app-2026 \
  --app 1:961127696213:android:7b2c493c2458ecc9f0d1dc
```

## 인프라 정보

| 리소스 | 스펙 | 비고 |
|--------|------|------|
| EC2 | t4g.micro, Amazon Linux | IP: 54.180.179.231 |
| RDS | db.t4g.micro, MySQL 8.0, 20GB | 자동 백업, 서울 리전 |
| Vercel | Hobby Plan | 자동 HTTPS |
| Firebase | Spark (무료) | FCM, Analytics |
| 도메인 | damo-web.vercel.app | Vercel 기본 |

## SSH 접속

```bash
ssh -i ~/.ssh/dadoc-key.pem ec2-user@54.180.179.231
```
