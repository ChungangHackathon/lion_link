# LION LINK 백엔드 배포 설정

> 서버 주소: `https://lionlink.n-e.kr`. 배포 담당자가 서버·DB·S3를 설정할 때 보는 문서야. API는 `docs/api.md`, 테이블은 `docs/erd.md`를 봐.

## 1. 실행 프로필

운영 서버는 반드시 `SPRING_PROFILES_ACTIVE=prod`로 띄워.

- `prod`가 아니면 개발용 `X-User-Id` 헤더 로그인이 열려서, 헤더 하나로 아무 회원인 척할 수 있어 (REST, WebSocket 둘 다)
- `prod`에서는 Swagger(`/swagger-ui`, `/v3/api-docs`)가 기본으로 꺼져. FE 연동 기간에만 `SWAGGER_ENABLED=true`로 켜

## 2. 환경 변수

| 변수 | 필수 | 설명 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | O | `prod` |
| `SPRING_DATASOURCE_URL` | O | `jdbc:mysql://{DB 주소}:3306/lionlink?serverTimezone=Asia/Seoul&characterEncoding=UTF-8`. `serverTimezone`을 꼭 넣어 |
| `DB_USERNAME`, `DB_PASSWORD` | O | DB 계정 |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | O | 구글 로그인 |
| `JWT_SECRET` | O | 32바이트 이상 임의 문자열 |
| `FRONTEND_URL` | O | 프론트 주소 (`https://...`, 끝에 `/` 없이). CORS, 로그인 뒤 이동, 메일 링크, WebSocket 허용 주소에 같이 쓰여 |
| `COOKIE_SECURE` | O | `true` (HTTPS) |
| `COOKIE_SAME_SITE` | O | 프론트와 백엔드 도메인이 다르면 `None`, 같으면 `Lax` |
| `S3_BUCKET`, `S3_REGION` | O | 업로드 버킷. 지역 기본값 `ap-northeast-2` |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | △ | 서버에 IAM 역할을 붙였으면 비워 둬 |
| `S3_PUBLIC_URL` | X | 파일 주소 앞부분. CloudFront를 쓸 때만. 비우면 `https://{버킷}.s3.{지역}.amazonaws.com` |
| `MAIL_ENABLED` | X | `true`일 때만 메일을 실제로 보내 (기본 `false`, 로그만 남김) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | 메일을 켜면 O | SMTP (Gmail, AWS SES 등). 포트 기본 587 |
| `MAIL_FROM` | 메일을 켜면 O | 보내는 사람. 예: `"LION:LINK" <no-reply@도메인>`. 표시 이름에 콜론이 있어 따옴표로 감싸야 한다. 빼면 메일을 만들 때 실패한다 |
| `SWAGGER_ENABLED` | X | 운영에서 Swagger를 켤 때만 `true` |

## 3. 구글 로그인

- 구글 콘솔의 승인된 리디렉션 URI: `https://lionlink.n-e.kr/login/oauth2/code/google`
- 로그인 시작 주소: `https://lionlink.n-e.kr/oauth2/authorization/google`

## 4. S3

게시글 파일은 브라우저가 업로드 주소(FILE-01)로 S3에 바로 `PUT`해서 버킷 CORS가 필요해. 프로필 사진은 서버를 거쳐 올라가.

**버킷 CORS** (S3 → 버킷 → 권한 → CORS)

```json
[
  {
    "AllowedOrigins": ["https://{프론트 주소}", "http://localhost:3000"],
    "AllowedMethods": ["PUT", "GET", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }
]
```

**공개 읽기** (버킷 정책). 이미지·첨부파일을 주소로 바로 보여줘서 필요해. "퍼블릭 액세스 차단"에서 버킷 정책 관련 두 항목은 꺼야 적용돼.

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": "*",
    "Action": "s3:GetObject",
    "Resource": ["arn:aws:s3:::{버킷}/uploads/*", "arn:aws:s3:::{버킷}/profile-photos/*"]
  }]
}
```

**서버 권한** (IAM): `arn:aws:s3:::{버킷}/*`에 `s3:PutObject`, `s3:DeleteObject`. 업로드 주소 서명, 프로필 사진 올리기, 글에서 빠진 파일 지우기에 써.

## 5. WebSocket (채팅)

- 주소: `wss://lionlink.n-e.kr/ws` (STOMP)
- 앞에 프록시(nginx, 로드밸런서)가 있으면 WebSocket 업그레이드를 넘겨줘야 해. nginx라면:

```nginx
location /ws {
    proxy_pass http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 3600s;
}
```

- 허용 주소는 `FRONTEND_URL`이야. 다르면 연결이 403으로 막혀

## 6. DB (Flyway)

테이블은 서버가 뜰 때 Flyway(`backend/src/main/resources/db/migration`, `backend/src/main/java/db/migration`)가 만들고 바꿔. 서버는 엔티티와 테이블이 다르면 뜨지 않아(`ddl-auto: validate`).

| 버전 | 내용 |
| --- | --- |
| V1 | 전체 테이블 (2026-11-14 기준) |
| V2 | 첫 배포(2026-11-13) DB에 없던 컬럼·테이블 추가 (`chat_rooms.meetup_id`, `closed_at`, `post_search_keywords`). 이미 있으면 건너뜀 |
| V3 | enum 컬럼의 CHECK 제약 삭제. enum 값은 서버가 검증해서, 값을 추가해도 DB를 바꿀 필요가 없어 |

**지금 운영 DB에 처음 올릴 때**: 따로 할 일은 없어. 이미 테이블이 있으니 V1을 적용한 것으로 표시(baseline)하고 V2, V3만 실행해. 데이터는 그대로 남아. 혹시 모르니 올리기 전에 DB 스냅샷을 떠 둬.

**테이블을 바꿀 때**: `V4__설명.sql`처럼 다음 번호로 파일을 추가해. 이미 적용된 파일은 고치지 마.

**로컬 DB**: 기존 로컬 DB도 그대로 돼. 서버가 스키마 검증에서 멈추면 `docker compose down -v`로 DB를 지우고 다시 띄워.

## 7. 배포 후 확인

1. `GET /actuator/health` → `{"status":"UP"}` (DB 연결까지 확인함)
2. 운영에서 `/swagger-ui/index.html`이 404
3. 구글 로그인 → 프론트로 돌아오는지
4. 글쓰기에서 이미지 업로드 → 글 상세에서 이미지가 보이는지
5. 채팅방을 열어서 실시간으로 메시지가 오는지 (`wss://.../ws`)
6. 메일을 켰다면 커피챗 신청 메일이 오는지

## 8. 서버를 여러 대로 늘릴 때

지금은 1대 기준이야. 늘리기 전에 아래를 먼저 바꿔야 해.

- 스케줄러(메일 발송 10초마다, 커피챗 만료·완료 1시간마다, 검색어 정리 매일)가 서버마다 돌아. 메일이 두 번 갈 수 있어서 ShedLock 같은 잠금이 필요해
- 채팅 실시간 전송이 서버 메모리(simple broker)를 써서, 다른 서버에 붙은 사람에게는 안 가. 외부 브로커(RabbitMQ 등)가 필요해
