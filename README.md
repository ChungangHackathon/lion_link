# 멋사동네

> 활동은 한 기수, 연결은 모든 기수.

멋쟁이사자처럼 중앙대 동문을 기수 너머로 잇는 서비스입니다. 프로필을 올리고, 동문을 찾고,
커피챗을 신청하고, 채팅으로 약속을 잡는 흐름까지 한곳에서 됩니다.


---

## 기술

| 영역 | 스택 |
| --- | --- |
| 백엔드 | Java 21 · Spring Boot 4.1.2 · Spring Data JPA · Spring Security(OAuth2 Client) · WebSocket(STOMP) |
| DB | MySQL 8.4 · Flyway (테스트는 H2) |
| 인프라 | AWS S3(파일) · SMTP(메일) · EC2 |
| 프론트 | Next.js 16 · React 19 · TypeScript · Tailwind v4 · @stomp/stompjs · Vercel |



## 로컬에서 실행하기

### 1. DB

```bash
cd backend
docker compose up -d
```

MySQL 8.4가 `localhost:3306`에 뜨고, 계정과 DB 이름은 모두 `lionlink`입니다.
테이블은 서버가 뜰 때 Flyway가 만듭니다.

### 2. 백엔드

```bash
cd backend
cp src/main/resources/application-secret.example.yml src/main/resources/application-secret.yml
./gradlew bootRun
```

`application-secret.yml`은 git에 올라가지 않습니다. 값을 비워 두면 `application.yml`의 로컬
기본값으로 뜨고, 구글 로그인·파일 업로드·메일처럼 외부 서비스가 필요한 기능만 막힙니다.
설정 우선순위는 **환경변수 > `application-secret.yml` > `application.yml` 기본값**입니다.

서버는 `http://localhost:8080`입니다.

| 키 | 쓰는 곳 |
| --- | --- |
| `GOOGLE_CLIENT_ID` · `GOOGLE_CLIENT_SECRET` | 구글 로그인 |
| `JWT_SECRET` | 토큰 서명 (32바이트 이상) |
| `S3_BUCKET` · `AWS_ACCESS_KEY_ID` · `AWS_SECRET_ACCESS_KEY` | 사진·첨부 업로드 |
| `MAIL_*` | 메일 알림 (`MAIL_ENABLED=false`면 보내지 않고 기록만) |
| `ADMIN_EMAILS` | 서버가 뜰 때 관리자로 올릴 계정 |

### 3. 프론트

```bash
cd frontend
npm install
cp .env.example .env.local
npm run dev
```

`http://localhost:3000`입니다. `NEXT_PUBLIC_API_URL`을 비우면 **백엔드 없이** mock 데이터로
모든 화면을 볼 수 있습니다(프로토타입 모드). 
