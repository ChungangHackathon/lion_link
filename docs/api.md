# LION LINK API 명세서

> 전체 API 목록(87개)은 아래 "전체 API 목록". 상세 명세는 계정·인증(AUTH)·프로필(PROFILE)·관리자(ADMIN) — 담당 이시형, 동문 찾기(ALUMNI)·커뮤니티(POST·COMMENT·MEETUP·RECRUIT)·홈(HOME)·파일(FILE)·커피챗(COFFEE)·알림(NOTI)·채팅(CHAT) — 담당 조상원.
> 마지막 갱신: 2026-11-13 (번개 단체 채팅방 추가). 기준: 기능명세서 v1.17, `docs/erd.md`

## 공통 규칙

모든 API는 `/api/v1` 아래의 JSON REST이고, 로그인 후에는 `Authorization: Bearer {accessToken}` 헤더로 인증해. 기준은 기능명세서 v1.17과 DB 스키마 v2야.

| 항목 | 규칙 |
| --- | --- |
| Base URL | `/api/v1` |
| 형식 | 요청·응답 모두 `application/json; charset=utf-8`. 필드 이름은 camelCase |
| 인증 | Access Token 1시간(헤더), Refresh Token 14일(HttpOnly 쿠키 `refreshToken`). 1.5 |
| 시간 | 한국 시간(KST) ISO-8601, 예) `2026-11-08T12:00:00+09:00`. 날짜만 쓰는 값은 `2026-11-08`, 년월은 `2026-11` |
| ID | 숫자(BIGINT) |
| 삭제 | 소프트 삭제. 삭제·탈퇴한 데이터는 404로 응답해 |
| 열거값 | 영어 대문자 코드(`APPROVED`, `ADMIN` 등)로 주고받고, 한글 문구는 FE가 붙여 |
| 목록 | 페이지 방식은 `page`(0부터), `size`로 요청하고 `{ items, page, size, totalElements, hasNext }`로 응답해. 전체 개수가 필요 없는 더보기 목록(댓글, 관심 동문)은 `{ items, page, size, hasNext }` |
| 성공 응답 | 감싸지 않고 데이터 객체를 그대로 반환해. 본문이 없으면 `204 No Content` |

### 회원 상태별 접근

서버는 모든 요청에서 토큰의 회원 상태를 확인해. 상태는 `users.status`(`PENDING`/`APPROVED`/`REJECTED`)와 `users.role`(`MEMBER`/`ADMIN`) 조합이야.

| 대상 | 가능한 API | 막힐 때 |
| --- | --- | --- |
| 비회원(토큰 없음) | AUTH-01 구글 로그인, AUTH-02 가입 신청, AUTH-09 가입 화면 구글 정보, AUTH-03 토큰 재발급 | `401 UNAUTHENTICATED` |
| 승인 대기(`PENDING`) | 내 정보, 가입 정보 수정, 내 프로필 조회·편집, 공개 범위, 커피챗 설정, 알림 설정, 로그아웃, 탈퇴 | `403 NOT_APPROVED` → FE는 `GET /me`의 `status`를 보고 승인 대기 화면으로 이동 |
| 반려(`REJECTED`) | 내 정보, 다시 신청, 로그아웃, 탈퇴 | `403 NOT_APPROVED` → FE는 `GET /me`의 `status`를 보고 반려 안내 화면으로 이동 |
| 승인(`APPROVED`) | 위 승인 대기 API 전부와 동문 찾기·커피챗·채팅·커뮤니티·알림 |  |
| 관리자(`ADMIN`) | 승인 회원 API 전부와 `/admin/**` | 일반 회원이 `/admin/**` 호출 시 `403 FORBIDDEN` |

프로필 기본 정보 입력(2.1)을 끝내지 않은 회원(`profileCompleted=false`)은 서버가 막지 않고, FE가 `GET /me`의 `profileCompleted`를 보고 2.1 화면으로 보내.

### 에러 응답

실패 응답은 모두 `{ "code": "오류 종류", "message": "사용자에게 보여줄 문구" }` 형식이야. FE는 `code`로 분기하고 `message`는 그대로 보여줘. 입력 검증 실패에만 `fieldErrors: [{ field, message }]`가 추가돼.

| HTTP | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 입력값 검증 실패(글자 수, 형식, 필수값) |
| 401 | `UNAUTHENTICATED` | 토큰이 없거나 만료·위조. FE는 재발급을 1회 시도하고, 실패하면 로그인 화면으로 보내 |
| 401 | `GOOGLE_AUTH_FAILED` | 구글 로그인 실패 (JSON 응답이 아니라 {FE}/login?error=로 전달) |
| 403 | `FORBIDDEN` | 권한 없음(관리자 아님, 남의 데이터) |
| 403 | `NOT_APPROVED` | 승인 대기·반려 회원이 막힌 기능을 호출. 둘의 구분은 `GET /me`의 `status`로 한다 |
| 403 | `PROFILE_NOT_COMPLETED` | 기본 프로필(2.1)을 입력하지 않은 회원이 프로필 완료가 필요한 기능(홈 등)을 호출 |
| 404 | `NOT_FOUND` | 없는 데이터, 삭제된 데이터, 탈퇴 회원, 승인 전 회원의 프로필 |
| 409 | `CONFLICT` 계열 | 상태 충돌. 이 문서의 각 API에 `ALREADY_PROCESSED` 같은 구체 코드를 적었어. FE는 `message`를 토스트로 띄우고 데이터를 다시 불러와 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 오류 |

## 전체 API 목록 (팀 노션 기준)

팀 노션 API 명세서의 86행을 영역별로 모았다(2026-11-12 기준). `GET /users/{userId}`가 두 영역에 중복돼 있어 동문 찾기에만 두었고, 그래서 85개였다. 커뮤니티를 구현하면서 번개 참여자 목록·번개 취소 2개를 빼고 카테고리별 글 수·좋아요·좋아요 취소 3개, 커피챗 모집 참여 1개(4.11)를 더해 87개다. 상세 명세는 계정·인증·프로필·관리자·동문 찾기·커뮤니티·홈이 이 문서에 있고, 커피챗·채팅·알림은 담당자의 노션 페이지가 원본이다.

| 영역 | API 수 | 담당 (노션 기준) | 상세 명세 |
| --- | --- | --- | --- |
| 1. 계정·인증 | 9 | 이시형 | 이 문서 아래 |
| 2. 프로필 | 18 | 이시형 | 이 문서 아래 |
| 3. 관리자 | 6 | 이시형 | 이 문서 아래 |
| 4. 커피챗 | 10 | 차예은 (조상원이 함께 맡음) | 이 문서 8장 |
| 5. 동문 찾기 | 5 | 조상원 | 이 문서 아래 |
| 6. 커뮤니티 | 17 | 조상원 | 이 문서 아래 |
| 7. 홈 | 2 | 조상원 | 이 문서 아래 |
| 8. 채팅 | 14 | 차예은 (조상원이 함께 맡음) | 이 문서 10장 |
| 9. 알림 | 6 | 차예은 | 이 문서 9장 |

커피챗·채팅·알림 30개는 노션 표에 행만 있고, 확인한 페이지(커피챗 신청, 첫 메시지 요청, 알림 목록)의 본문은 빈 템플릿이었다.

| 영역 | Method | 경로 (`/api/v1` 생략) | 설명 | 담당 |
| --- | --- | --- | --- | --- |
| 1. 계정·인증 | `POST` | `/auth/logout` | 로그아웃 | 이시형 |
| 1. 계정·인증 | `POST` | `/auth/refresh` | 토큰 재발급 | 이시형 |
| 1. 계정·인증 | `POST` | `/auth/signup` | 가입 신청 | 이시형 |
| 1. 계정·인증 | `GET` | `/auth/signup/google-info` | 가입 화면 구글 정보 조회 | 이시형 |
| 1. 계정·인증 | `GET` | `/me` | 내 정보 조회 | 이시형 |
| 1. 계정·인증 | `POST` | `/me/reapply` | 다시 신청 | 이시형 |
| 1. 계정·인증 | `PATCH` | `/me/signup-info` | 가입 정보 수정 | 이시형 |
| 1. 계정·인증 | `POST` | `/me/withdraw` | 회원 탈퇴 | 이시형 |
| 1. 계정·인증 | `GET` | `/oauth2/authorization/google` | 구글 로그인 시작 (`/api/v1` 아님) | 이시형 |
| 2. 프로필 | `POST` | `/me/coffee-periods` | 쉬는·받는 기간 추가 | 이시형 |
| 2. 프로필 | `DELETE` | `/me/coffee-periods/{id}` | 쉬는·받는 기간 삭제 | 이시형 |
| 2. 프로필 | `GET` | `/me/coffee-settings` | 커피챗 받기 설정 조회 | 이시형 |
| 2. 프로필 | `PUT` | `/me/coffee-settings` | 커피챗 받기 설정 저장 | 이시형 |
| 2. 프로필 | `POST` | `/me/coffee-settings/switch` | 커피챗 받기 ON/OFF 스위치 | 이시형 |
| 2. 프로필 | `GET` | `/me/profile` | 내 프로필 조회 | 이시형 |
| 2. 프로필 | `PUT` | `/me/profile/basic` | 기본 정보 수정 | 이시형 |
| 2. 프로필 | `PUT` | `/me/profile/bio` | 소개 수정 | 이시형 |
| 2. 프로필 | `PUT` | `/me/profile/contact` | 연락처 수정 | 이시형 |
| 2. 프로필 | `PUT` | `/me/profile/links` | 외부 링크 수정 | 이시형 |
| 2. 프로필 | `POST` | `/me/profile/onboarding` | 기본 정보 입력(온보딩) | 이시형 |
| 2. 프로필 | `DELETE` | `/me/profile/photo` | 프로필 사진 삭제 | 이시형 |
| 2. 프로필 | `PUT` | `/me/profile/photo` | 프로필 사진 변경 | 이시형 |
| 2. 프로필 | `PUT` | `/me/profile/skills` | 대표 기술 수정 | 이시형 |
| 2. 프로필 | `PATCH` | `/me/profile/visibility` | 공개 범위 설정 | 이시형 |
| 2. 프로필 | `POST` | `/me/{section}` | 목록형 섹션 항목 추가 | 이시형 |
| 2. 프로필 | `PUT` | `/me/{section}/{itemId}` | 목록형 섹션 항목 수정 | 이시형 |
| 2. 프로필 | `DELETE` | `/me/{section}/{itemId}` | 목록형 섹션 항목 삭제 | 이시형 |
| 3. 관리자 | `GET` | `/admin/signups` | 가입 신청 목록 조회 | 이시형 |
| 3. 관리자 | `GET` | `/admin/signups/pending-count` | 승인 대기 수 조회 | 이시형 |
| 3. 관리자 | `POST` | `/admin/signups/{userId}/approve` | 가입 승인 | 이시형 |
| 3. 관리자 | `POST` | `/admin/signups/{userId}/reject` | 가입 반려 | 이시형 |
| 3. 관리자 | `POST` | `/admin/signups/approve-batch` | 가입 일괄 승인 | 이시형 |
| 3. 관리자 | `PATCH` | `/admin/signups/{userId}/signup-info` | 가입 정보 수정 (이름·기수) | 이시형 |
| 4. 커피챗 | `GET` | `/coffee-chats` | 받은·보낸 신청 보기 | 차예은 |
| 4. 커피챗 | `POST` | `/coffee-chats` | 커피챗 신청 | 차예은 |
| 4. 커피챗 | `GET` | `/coffee-chats/{coffeeChatId}` | 신청 내용과 가능한 버튼 보기 | 차예은 |
| 4. 커피챗 | `POST` | `/coffee-chats/{coffeeChatId}/accept` | 신청 수락하기 | 차예은 |
| 4. 커피챗 | `POST` | `/coffee-chats/{coffeeChatId}/reject` | 신청 거절하기 | 차예은 |
| 4. 커피챗 | `POST` | `/coffee-chats/{coffeeChatId}/withdraw` | 상대가 답하기 전 신청 거두기 | 차예은 |
| 4. 커피챗 | `PUT` | `/coffee-chats/{coffeeChatId}/schedule` | 만날 시간·장소 정하기 또는 바꾸기 | 차예은 |
| 4. 커피챗 | `POST` | `/coffee-chats/{coffeeChatId}/cancel` | 수락한 커피챗 취소하기 | 차예은 |
| 4. 커피챗 | `POST` | `/coffee-chats/{coffeeChatId}/complete` | 만남 완료 기록하기 | 차예은 |
| 4. 커피챗 | `DELETE` | `/coffee-chats/{coffeeChatId}/members/me` | 함께 신청한 커피챗에서 빠지기 | 차예은 |
| 5. 동문 찾기 | `GET` | `/users` | 동문 목록 조회 (검색·필터) | 조상원 |
| 5. 동문 찾기 | `GET` | `/users/{userId}` | 동문 프로필 상세 조회 (= PROFILE-03) | 조상원 |
| 5. 동문 찾기 | `GET` | `/users/me/bookmarks` | 관심 동문 목록 조회 | 조상원 |
| 5. 동문 찾기 | `PUT` | `/users/{userId}/bookmark` | 관심 동문 저장 | 조상원 |
| 5. 동문 찾기 | `DELETE` | `/users/{userId}/bookmark` | 관심 동문 저장 취소 | 조상원 |
| 6. 커뮤니티 | `GET` | `/posts` | 게시글 목록·검색·정렬 (카테고리 여러 개 가능) | 조상원 |
| 6. 커뮤니티 | `POST` | `/posts` | 게시글 작성 | 조상원 |
| 6. 커뮤니티 | `GET` | `/posts/category-counts` | 카테고리별 게시글 수 | 조상원 |
| 6. 커뮤니티 | `GET` | `/posts/popular-keywords` | 인기 키워드 조회 | 조상원 |
| 6. 커뮤니티 | `GET` | `/posts/{postId}` | 게시글 상세 조회 | 조상원 |
| 6. 커뮤니티 | `PATCH` | `/posts/{postId}` | 게시글 수정 | 조상원 |
| 6. 커뮤니티 | `DELETE` | `/posts/{postId}` | 게시글 삭제 (번개 글이면 번개 취소, 참여자 알림) | 조상원 |
| 6. 커뮤니티 | `PUT` | `/posts/{postId}/like` | 게시글 좋아요 | 조상원 |
| 6. 커뮤니티 | `DELETE` | `/posts/{postId}/like` | 게시글 좋아요 취소 | 조상원 |
| 6. 커뮤니티 | `GET` | `/posts/{postId}/comments` | 댓글 목록 조회 | 조상원 |
| 6. 커뮤니티 | `POST` | `/posts/{postId}/comments` | 댓글 작성 | 조상원 |
| 6. 커뮤니티 | `DELETE` | `/posts/{postId}/comments/{commentId}` | 댓글 삭제 | 조상원 |
| 6. 커뮤니티 | `PUT` | `/posts/{postId}/meetup/members/me` | 번개모임 참여 | 조상원 |
| 6. 커뮤니티 | `DELETE` | `/posts/{postId}/meetup/members/me` | 번개모임 참여 취소 | 조상원 |
| 6. 커뮤니티 | `POST` | `/posts/{postId}/meetup/invites` | 번개 초대 알림 발송 | 조상원 |
| 6. 커뮤니티 | `PUT` | `/posts/{postId}/recruit/members/me` | 커피챗 모집 참여 (참여 취소 `DELETE`는 결정 후 추가) | 조상원 |
| 6. 커뮤니티 | `POST` | `/files` | 이미지·첨부파일 업로드 주소 발급 | 조상원 |
| 7. 홈 | `GET` | `/home` | 홈 화면 통합 정보 조회 | 조상원 |
| 7. 홈 | `POST` | `/home/coffee-guide/dismiss` | 첫 커피챗 가이드 카드 닫기 | 조상원 |
| 8. 채팅 | `POST` | `/chat-requests` | 첫 메시지 요청 보내기 | 차예은 |
| 8. 채팅 | `GET` | `/chat-requests` | 받은 요청함 열기 | 차예은 |
| 8. 채팅 | `GET` | `/chat-requests/{roomId}` | 첫 메시지 내용 보기 | 차예은 |
| 8. 채팅 | `POST` | `/chat-requests/{roomId}/accept` | 메시지 요청 수락 | 차예은 |
| 8. 채팅 | `POST` | `/chat-requests/{roomId}/decline` | 메시지 요청 거절 | 차예은 |
| 8. 채팅 | `PATCH` | `/chat-requests/{roomId}/messages/{messageId}` | 수락 전 첫 메시지 수정 | 차예은 |
| 8. 채팅 | `DELETE` | `/chat-requests/{roomId}/messages/{messageId}` | 수락 전 첫 메시지 삭제·요청 취소 | 차예은 |
| 8. 채팅 | `GET` | `/chat-rooms` | 내 채팅방 목록 | 차예은 |
| 8. 채팅 | `GET` | `/chat-rooms/{roomId}` | 채팅방 정보와 참여자 | 차예은 |
| 8. 채팅 | `GET` | `/chat-rooms/{roomId}/messages` | 이전 메시지·끊긴 동안 온 메시지 | 차예은 |
| 8. 채팅 | `POST` | `/chat-rooms/{roomId}/messages` | 일반 메시지 보내기 | 차예은 |
| 8. 채팅 | `PUT` | `/chat-rooms/{roomId}/read` | 읽음 처리 | 차예은 |
| 8. 채팅 | `PATCH` | `/chat-rooms/{roomId}/preferences` | 방 고정·알림 끄기 | 차예은 |
| 8. 채팅 | `DELETE` | `/chat-rooms/{roomId}/members/me` | 방 나가기·종료된 요청 숨기기 | 차예은 |
| 9. 알림 | `GET` | `/notifications` | 최근 알림 20개 | 차예은 |
| 9. 알림 | `PUT` | `/notifications/{notificationId}/read` | 알림 하나 읽음 처리 | 차예은 |
| 9. 알림 | `PUT` | `/notifications/read-all` | 알림 모두 읽음 처리 | 차예은 |
| 9. 알림 | `GET` | `/me/notification-settings` | 이메일 알림 설정 조회 | 차예은 |
| 9. 알림 | `PATCH` | `/me/notification-settings` | 이메일 알림 종류별 켜기·끄기 | 차예은 |
| 9. 알림 | `GET` | `/email-notifications/unsubscribe` | 메일 링크로 해당 종류 수신 거부 | 차예은 |

### 노션 표와 이 문서를 맞추며 찾은 것

| 번호 | 내용 | 제안 |
| --- | --- | --- |
| 1 | `GET /users/{userId}`가 '2. 프로필'(PROFILE-03)과 '5. 동문 찾기' 두 행에 있다 | 동문 찾기(`AlumniController`)에 이미 구현돼 있으니 프로필 쪽 행은 "동문 찾기와 같음"으로 표시 |
| 2 | 다른 영역 명세(홈 등)와 공용 코드(`CommonErrorCode`)는 승인 전 403을 `NOT_APPROVED`, 프로필 미완료를 `PROFILE_NOT_COMPLETED`로 쓴다 | **확정(2026-11-12)**: `NOT_APPROVED`로 통일. 이 문서의 `APPROVAL_PENDING`·`ACCOUNT_REJECTED`는 없앴다 |
| 3 | 커뮤니티가 `POST /files`(업로드 주소 발급)를 쓴다 | **반영(2026-11-13)**: S3 업로드 주소 발급으로 구현(FILE-01). 프로필 사진(PROFILE-05)도 같은 API를 쓰면 된다 |
| 4 | 커피챗·채팅·알림 상세 명세가 비어 있다 | 커피챗 신청(4.1)·수락은 프로필의 커피챗 설정(PROFILE-12~15)과 맞물린다. 담당자에게 작성 일정 확인 |
| 5 | 실시간 채팅(5.3)의 WebSocket 연결·구독 경로, 커피챗 함께 신청 모집(4.11) API가 목록에 없다 | 채팅 담당 확인. 커피챗 함께 신청 모집은 모집 참여를 커뮤니티가, 모인 인원으로 신청을 커피챗 신청(`fromRecruitId`)이 맡는다 ('커뮤니티 확정 필요' 2번) |
| 6 | PROFILE-03(`GET /users/{userId}`)의 응답 형식과 지금 구현(ALUMNI-02)이 다르다 | **반영(2026-11-12)**: PROFILE-03 형식으로 맞췄다. `COOLDOWN`은 커피챗 구현(8장)과 함께 반영 (ALUMNI-02 참고) |

## 1. 계정·인증

가입은 구글 계정 하나로만 하고, 운영진 승인 전에는 프로필 설정만 할 수 있어. 초대 링크는 v1.17에서 없어졌어. API는 9개야. 구글 로그인은 Authorization Code 방식이라 로그인 시작은 브라우저 이동이고, 결과는 FE로 리다이렉트하면서 HttpOnly 쿠키로 넘겨.

| ID | 기능 | 메서드·경로 | 호출 가능한 대상 | 명세 |
| --- | --- | --- | --- | --- |
| AUTH-01 | 구글 로그인 | `GET /oauth2/authorization/google` | 누구나 | 1.5 |
| AUTH-09 | 가입 화면 구글 정보 | `GET /auth/signup/google-info` | 구글 로그인을 마친 비회원 | 1.4 |
| AUTH-02 | 가입 신청 | `POST /auth/signup` | 구글 로그인을 마친 비회원 | 1.4 |
| AUTH-03 | 토큰 재발급 | `POST /auth/refresh` | Refresh 쿠키 보유자 | 1.5 |
| AUTH-04 | 로그아웃 | `POST /auth/logout` | 누구나 | 1.6 |
| AUTH-05 | 내 정보 조회 | `GET /me` | 로그인한 모든 상태 | 1.4.1 |
| AUTH-06 | 가입 정보 수정 | `PATCH /me/signup-info` | 승인 대기 | 1.4.1 |
| AUTH-07 | 다시 신청 | `POST /me/reapply` | 반려 | 1.4.1 |
| AUTH-08 | 회원 탈퇴 | `POST /me/withdraw` | 로그인한 모든 상태 | 1.8 |

구글 계정 변경·분실(1.7, P2)은 MVP에서 운영진이 DB로 처리하므로 API가 없어.

### AUTH-01 구글 로그인

`GET /oauth2/authorization/google` · 인증 불필요 · `/api/v1` 아님

Authorization Code 방식이야. FE는 API를 부르지 않고 브라우저를 이 주소로 이동시켜. 서버(Spring Security)가 구글 로그인 화면으로 보내고, 구글이 콜백 `GET /login/oauth2/code/google`로 돌려준 code를 클라이언트 ID·시크릿으로 교환해 구글 계정(고유 ID·이메일·이름)을 확인해. 그다음 결과에 따라 FE로 리다이렉트해. 토큰은 URL에 싣지 않고 HttpOnly 쿠키로만 줘. 가입 신청(AUTH-02)을 누르기 전에는 계정을 만들지 않아.

| 결과 | 이동 위치 | 쿠키 | FE가 이어서 할 일 |
| --- | --- | --- | --- |
| 가입된 계정(승인 대기·반려 포함) | `{FE}/auth/callback` | `refreshToken` (HttpOnly, 14일) | AUTH-03으로 accessToken 발급 → AUTH-05로 화면 분기 |
| 가입하지 않았거나 탈퇴한 계정 | `{FE}/signup` | `signupToken` (HttpOnly, 10분) | AUTH-09로 이메일·이름 미리 채움 → AUTH-02 |
| 실패 | `{FE}/login?error={code}` | 없음 | 아래 code로 안내 |

| code | 언제 |
| --- | --- |
| `GOOGLE_AUTH_FAILED` | 로그인 취소, 이메일 인증이 안 된 구글 계정, 그 밖의 인증 실패 |
| `GOOGLE_UNAVAILABLE` | 구글과 토큰·사용자 정보를 주고받다 실패 |

로그인할 때마다 구글 계정 선택 화면이 떠(`prompt=select_account`). 쿠키 경로는 `/api/v1/auth`라서 FE는 이 경로를 부를 때 `credentials: 'include'`를 꼭 붙여야 해. Google Console의 승인된 리디렉션 URI에는 `{서버 주소}/login/oauth2/code/google`을 등록하고, 서버에는 `GOOGLE_CLIENT_ID`·`GOOGLE_CLIENT_SECRET`·`FRONTEND_URL`을 환경변수로 넣어. FE와 서버 도메인이 다르면 쿠키에 `SameSite=None; Secure`가 필요해.

### AUTH-09 가입 화면 구글 정보

`GET /auth/signup/google-info` · `signupToken` 쿠키 필요

구글 로그인 뒤 `{FE}/signup`으로 돌아온 FE가 이메일·이름 칸을 미리 채울 때 불러. `name`은 구글 계정 이름이고 없으면 `null`이야.

```json
{ "email": "lion@gmail.com", "name": "김사자" }
```

| 상태 | code | 언제 |
| --- | --- | --- |
| 401 | `UNAUTHENTICATED` | `signupToken` 쿠키가 없거나 만료(10분)·위조됨. 구글 로그인부터 다시 |

### AUTH-02 가입 신청

`POST /auth/signup` · `signupToken` 쿠키 필요

이름·기수를 입력하고 개인정보 동의를 하면 계정을 만들어. 상태는 `PENDING`이고 바로 로그인돼. 이어서 프로필 기본 정보 입력(PROFILE-01)으로 가.

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `name` | string | O | 2\~20자 |
| `generation` | number | O | 1\~14 (현재 기수까지) |
| `privacyAgreed` | boolean | O | `true`여야 함 |

`201`이고 `refreshToken` 쿠키가 함께 내려가고 signupToken 쿠키는 지워져.

```json
{
  "accessToken": "eyJ...",
  "expiresIn": 3600,
  "user": { "...": "AUTH-05의 응답과 같음" }
}
```

서버가 하는 일은 이래.

- 구글 이메일, 구글 고유 ID, 구글 계정 이름(관리자 확인용)을 저장해.
- 관리자 전원에게 '새 가입 승인 요청' 서비스 안 알림을 보내.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 글자 수·범위 오류, `privacyAgreed`가 `false` |
| 401 | `UNAUTHENTICATED` | `signupToken` 쿠키가 없거나 만료·위조됨 |
| 409 | `ALREADY_REGISTERED` | 이미 가입된 구글 계정. 창 두 개에서 동시에 신청하면 하나만 성공해 |

### AUTH-03 토큰 재발급

`POST /auth/refresh` · `refreshToken` 쿠키 필요

요청 본문은 없어. 구글 로그인 직후 {FE}/auth/callback에서 accessToken을 처음 받을 때도 이걸 불러. Access Token이 만료돼 401을 받으면 FE가 1번만 호출해. 성공하면 Refresh Token도 새로 발급해서 쿠키를 교체해(회전).

```json
{ "accessToken": "eyJ...", "expiresIn": 3600 }
```

| 상태 | code | 언제 |
| --- | --- | --- |
| 401 | `UNAUTHENTICATED` | 쿠키가 없거나 만료·폐기됨, 탈퇴한 회원. FE는 로그인 화면으로 이동 |

이미 회전돼 폐기된 Refresh Token이 다시 오면 탈취로 보고 그 회원의 Refresh Token을 모두 폐기해(모든 기기 로그아웃). 그래서 FE는 재발급을 동시에 여러 번 부르지 말고, 진행 중인 재발급 요청 하나를 같이 기다려야 해.

### AUTH-04 로그아웃

`POST /auth/logout` · 인증 불필요

Refresh Token을 폐기하고 쿠키를 지워. 응답은 항상 `204`이고, 토큰이 없거나 이미 만료돼 있어도 오류를 내지 않아. 서버 호출이 실패해도 FE는 저장된 토큰을 지우고 로그인 화면으로 보내(1.6).

### AUTH-05 내 정보 조회

`GET /me` · 로그인한 모든 상태(승인 대기·반려 포함)

FE가 앱을 열 때 부르고, `status`와 `profileCompleted`로 어느 화면으로 보낼지 정해. AUTH-01, 02, 06, 07의 `user`도 이 형식이야.

| 응답 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | number | 회원 ID |
| `name` | string | 가입 때 입력한 이름 |
| `generation` | number | 가입 때 입력한 기수 |
| `email` | string | 구글 가입 이메일 |
| `role` | string | `MEMBER` / `ADMIN` |
| `status` | string | `PENDING` / `APPROVED` / `REJECTED`. FE 화면 분기 기준 |
| `profileCompleted` | boolean | `false`면 FE가 프로필 기본 정보 입력(2.1)으로 보냄 |
| `rejectReason` | string / null | `REJECTED`일 때만 값이 있어. 반려 안내 화면에 그대로 보여줘 |
| `signupInfoEditable` | boolean | 이름·기수를 고칠 수 있으면 `true`. `PENDING`일 때만 |
| `photoUrl` | string / null | 프로필 사진. 없으면 FE가 기본 사자 이미지를 써 |

```json
{
  "id": 12, "name": "김사자", "generation": 13, "email": "lion@gmail.com",
  "role": "MEMBER", "status": "PENDING", "profileCompleted": true,
  "rejectReason": null, "signupInfoEditable": true, "photoUrl": null
}
```

### AUTH-06 가입 정보 수정

`PATCH /me/signup-info` · 승인 대기(`PENDING`)만

승인 대기 화면의 \[가입 정보 고치기\]야. 승인 된 뒤에는 회원이 고칠 수 없고 관리자가 고쳐(ADMIN-04).

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `name` | string | O | 2\~20자 |
| `generation` | number | O | 1\~14 |

`200`이고 응답은 AUTH-05 형식이야.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 글자 수·범위 오류 |
| 409 | `NOT_PENDING` | 반려된 회원. 이름·기수는 다시 신청(AUTH-07)으로 고쳐야 해 |
| 409 | `ALREADY_APPROVED` | 고치는 사이 승인됨. 메시지 '이미 승인됐어요. 이름·기수는 운영진에게 고쳐달라고 해주세요' |
| 409 | `DUPLICATE_ITEM` | 기수를 바꿨는데 바뀔 기수에 이미 다른 멋사 활동(운영진 등)이 있음. 자동 생성 항목(`isDefault`)의 기수도 같이 바뀌기 때문이야(확정이 필요한 것들 3) |

### AUTH-07 다시 신청

`POST /me/reapply` · 반려(`REJECTED`)만

반려 안내 화면의 \[다시 신청하기\]야. 이름·기수를 고쳐 다시 신청하면 상태가 `PENDING`으로 돌아가고 재신청 시각이 기록돼. 이전 반려 사유는 관리자 목록에 보여주려고 지우지 않아. 관리자에게는 새 가입 승인 요청 알림이 가.

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `name` | string | O | 2\~20자 |
| `generation` | number | O | 1\~14 |

`200`이고 응답은 AUTH-05 형식이야.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 글자 수·범위 오류 |
| 409 | `NOT_REJECTED` | 반려 상태가 아님(이미 다시 신청했거나 승인됨) |
| 409 | `DUPLICATE_ITEM` | AUTH-06과 같음. 바뀔 기수에 이미 다른 멋사 활동이 있음 |

### AUTH-08 회원 탈퇴

`POST /me/withdraw` · 로그인한 모든 상태

> **변경 예정:** 구글 로그인을 Authorization Code 방식으로 바꿔서 FE가 구글 ID 토큰을 받지 않아. 본인 확인 방식(구글 재로그인 리다이렉트 등)은 회원 탈퇴 이슈에서 정하고 고칠게.

탈퇴할 때 구글로 한 번 더 로그인해 본인을 확인해. `DELETE`는 본문을 떨굴 수 있어서 `POST`로 했어.

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `idToken` | string | O | 방금 다시 로그인해 받은 구글 ID 토큰. 현재 계정의 구글 고유 ID와 같아야 함 |

`204`이고, 서버가 하는 일은 이래.

- `deleted_at`을 기록하고 구글 고유 ID 연결을 풀어. 같은 구글 계정으로 다시 가입 신청할 수 있고, 새 회원 행이 만들어져.
- Refresh Token을 모두 폐기해.
- 진행 중인 커피챗은 취소(사유 탈퇴)하고 상대에게 알려. 여러 명 커피챗에서 받는 사자나 대표 신청자면 전체를 취소하고, 함께 신청자면 그 사람만 빠진다.
- 대기 중인 메시지 요청을 삭제해.
- 모집 중인 번개·커피챗 모집글은 종료하고 참여자에게 알려. 참여 중이던 모집글에서는 빠진다.
- 작성한 글·댓글은 남기고 작성자를 '알 수 없음'으로 표시해. 동문 찾기·프로필에서는 바로 빠진다.

| 상태 | code | 언제 |
| --- | --- | --- |
| 401 | `GOOGLE_AUTH_FAILED` | ID 토큰 검증 실패, 또는 다른 구글 계정으로 확인함 |
| 403 | `ADMIN_CANNOT_WITHDRAW` | 관리자 계정. 운영진이 DB에서 관리자 역할을 해제한 뒤에 탈퇴할 수 있어 |

## 2. 프로필

프로필은 승인 대기 회원도 편집할 수 있고, 다른 동문이 볼 때는 공개 범위를 서버가 적용한 결과만 내려줘. 크게 온보딩, 조회, 섹션 편집, 커피챗 받기 설정으로 나뉘고 API는 총 15개야. 프로필 완성도(2.6)는 FE가 계산하므로 API가 없어.

| ID | 기능 | 메서드·경로 | 명세 |
| --- | --- | --- | --- |
| PROFILE-01 | 기본 정보 입력(온보딩) | `POST /me/profile/onboarding` | 2.1 |
| PROFILE-02 | 내 프로필 조회 | `GET /me/profile` | 2.5 |
| PROFILE-03 | 다른 동문 프로필 조회 | `GET /users/{userId}` | 2.5 |
| PROFILE-04 | 기본 정보 수정 | `PUT /me/profile/basic` | 2.2 |
| PROFILE-05 | 프로필 사진 변경·삭제 | `PUT` `DELETE /me/profile/photo` | 2.2 |
| PROFILE-06 | 소개 수정 | `PUT /me/profile/bio` | 2.2 |
| PROFILE-07 | 대표 기술 수정 | `PUT /me/profile/skills` | 2.2 |
| PROFILE-08 | 외부 링크 수정 | `PUT /me/profile/links` | 2.2 |
| PROFILE-09 | 연락처 수정 | `PUT /me/profile/contact` | 2.2 |
| PROFILE-10 | 공개 범위 수정 | `PATCH /me/profile/visibility` | 2.3 |
| PROFILE-11 | 목록형 섹션 항목 추가·수정·삭제 | `POST` `PUT` `DELETE /me/{section}` | 2.2.1\~2.2.5 |
| PROFILE-12 | 커피챗 받기 설정 조회 | `GET /me/coffee-settings` | 2.4 |
| PROFILE-13 | 커피챗 받기 설정 저장 | `PUT /me/coffee-settings` | 2.4 |
| PROFILE-14 | 홈 스위치(받기·쉼기) | `POST /me/coffee-settings/switch` | 2.4, 8.1 |
| PROFILE-15 | 쉼는·받는 기간 추가·삭제 | `POST /me/coffee-periods`, `DELETE /me/coffee-periods/{id}` | 2.4.1 |

호출 가능한 대상은 PROFILE-03만 승인 회원(`APPROVED`)이고, 나머지는 승인 대기도 쓸 수 있어(반려 회원은 모두 `403 NOT_APPROVED`).

### 열거값

| 이름 | 값 |
| --- | --- |
| `lionPart` 멋사 파트 | `PLANNING` `DESIGN` `FRONTEND` `BACKEND` (기타 없음) |
| `status` 현재 상태 | `STUDENT` 재학, `LEAVE` 휴학, `GRADUATED` 졸업, `EMPLOYED` 재직, `FOUNDER` 창업, `GRAD_SCHOOL` 대학원, `ETC` 기타 |
| `interest` 관심 분야 | `PLANNING_PM` `DESIGN` `FRONTEND` `BACKEND` `APP` `DATA_AI` `INFRA_DEVOPS` `MARKETING` `STARTUP` `ETC` |
| `linkType` 외부 링크 종류 | `LINKEDIN` `GITHUB` `PORTFOLIO` `BLOG` `REMEMBER` `NOTION` `ETC` |
| `scope` 공개 범위 | `PUBLIC` 전체 공개, `CONNECTED` 연결된 동문만, `PRIVATE` 비공개 |
| `yearMonth` | `"2026-11"` 형식의 문자열. DB에는 해당 월 1일로 저장해 |

### PROFILE-01 기본 정보 입력(온보딩)

`POST /me/profile/onboarding` · 승인 대기·승인 회원, 처음 1번만

가입 직후 화면(2.1)의 \[시작하기\]야. 건너뛰기는 없어. 이름·기수는 여기서 받지 않고 가입 정보를 그대로 써.

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `lionParts` | string\[\] | O | `lionPart` 1\~2개, 중복 불가 |
| `status` | string | O | `status` 1개 |
| `interests` | string\[\] | O | `interest` 1\~3개 |
| `company` | string | `EMPLOYED`일 때 O | 1\~30자 |
| `jobTitle` | string | `EMPLOYED`일 때 O | 1\~30자 |
| `link` | string | X | http(s) URL 1개. 종류는 주소를 보고 자동 지정(linkedin.com은 `LINKEDIN`, github.com은 `GITHUB`, notion.site·notion.so는 `NOTION`, 그 밖은 `ETC`) |
| `headline` | string | X | 0\~100자 |
| `staffActivities` | object\[\] | X | 운영진 활동 최대 5줄. `{ generation(1~14, 중복 불가, 가입 기수 포함), parts(1~2개) }` |

`200`이고 응답은 AUTH-05 형식(`profileCompleted: true`)이야. 서버가 하는 일은 이래.

- 프로필을 만들고 `users.profile_completed`를 `true`로 바꿔.
- `EMPLOYED`면 경력에 '현재 재직 중' 항목 1개를 자동 등록해. 고용 형태와 시작 년월은 비어 있어.
- 학력에 '중앙대학교' 1개를 자동 등록해.
- 멋사 활동 이력에 가입 기수 항목 1개를 자동 등록해. 역할은 `LION`, 파트는 `lionParts` 그대로(파트별로 나눠 만들지 않아), 삭제할 수 없는 항목(`isDefault`)이야. `staffActivities`에 가입 기수가 있으면 그 항목의 역할을 `STAFF`로 바꿔. 나머지 기수는 `STAFF` 항목으로 추가해.
- `link`가 있으면 외부 링크의 첫 항목으로 넣어.
- 커피챗 받기 설정(기본 받기, 방식 둘 다)과 알림 설정(메일 둘 다 켜짐) 기본값을 만들어. 공개 범위는 기본값으로 시작해(이메일·전화번호 `CONNECTED`, 외부 링크·회사명 `PUBLIC`).

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 필수값 누락, 파트 3개 이상, 중복 기수, URL 형식 오류 |
| 409 | `ALREADY_COMPLETED` | 이미 기본 정보를 입력했음 |

### PROFILE-02 내 프로필 조회

`GET /me/profile` · 승인 대기·승인 회원

마이페이지 첫 화면이야. 내 프로필은 공개 범위를 적용하지 않고 모든 내용과 현재 공개 범위 설정을 함께 내려줘. 내용이 없는 섹션은 빈 배열이나 `null`로 오고 FE가 숨겨. 목록형 섹션은 날짜 최신순으로 정렬해서 주고, 진행 중인 항목이 맨 위야.

```json
{
  "userId": 12, "name": "김사자", "generation": 13,
  "photoUrl": null, "headline": "핀테크 백엔드 개발자",
  "bio": "...", "major": "컴퓨터공학", "minor": null, "region": "서울 강남",
  "status": "EMPLOYED", "lionParts": ["FRONTEND", "BACKEND"], "interests": ["BACKEND", "DATA_AI"],
  "skills": ["Spring", "MySQL"],
  "links": [{ "type": "GITHUB", "url": "https://github.com/lion" }],
  "contact": { "email": "lion@gmail.com", "phone": null },
  "visibility": { "email": "CONNECTED", "phone": "CONNECTED", "links": "PUBLIC", "company": "PUBLIC" },
  "careers": [], "educations": [], "lionActivities": [], "lionProjects": [],
  "projects": [], "certifications": [], "awards": [], "activities": [], "languages": []
}
```

목록형 섹션의 항목 형식은 PROFILE-11에 적었어. 항목마다 `id`가 들어 있어.

### PROFILE-03 다른 동문 프로필 조회

`GET /users/{userId}` · 승인 회원(`APPROVED`)만

동문 목록·검색·커피챗 화면에서 카드를 누르면 불러. 응답 구조는 PROFILE-02와 같은데, 다음이 달라.

- 공개 범위(2.3)로 가려진 항목(`contact.email`, `contact.phone`, `links`)은 키째 빼고 보내. FE가 가리는 방식은 쓰지 않아.
- 회사명이 비공개면 `careers[].company`를 빼고 `companyHidden: true`를 넣어. FE는 '비공개 회사'로 표시해. 직무는 그대로 보여.
- `visibility`와 내 전용 설정은 포함하지 않아.
- 아래 필드가 추가돼.

| 추가 필드 | 타입 | 설명 |
| --- | --- | --- |
| `coffeeChat` | object | 남의 커피챗 설정 요약 `{ availability, nextAvailableDate, acceptGroup, topics, modes, availableSlots, preferredArea }`. 오른쪽 고정 카드에 쓰여 |
| `coffeeChat.availability` | string | `AVAILABLE` 신청 가능, `RESTING` 쉼는 중, `NOT_ACCEPTING` 안 받는 중, `IN_PROGRESS` 이미 진행 중(내가 함께 신청자인 건 포함), `COOLDOWN` 재신청 대기 중 |
| `coffeeChat.nextAvailableDate` | date / null | `RESTING`·`COOLDOWN`일 때 '날짜부터 신청할 수 있어요'에 쓰는 날짜 |
| `chat` | object | `{ status, requestAvailableDate }`. `status`는 `NONE` `REQUESTED` `ACTIVE`, 거절된 날짜 + 7일 전이면 `requestAvailableDate`에 다시 보낼 수 있는 날짜 |
| `commonalities` | object\[\] | 나와의 공통점 `{ type, label }`이 정해진 순서대로 오고 FE가 최대 3개를 보여줘. `type`은 `SAME_LION_PROJECT` `SAME_COMPANY` `SAME_GENERATION` `SAME_PART` `SHARED_INTEREST`. 상대의 비공개 회사명은 계산에서 빼 |
| `mutualCount` | number | 함께 아는 동문 수. 누구인지는 내려주지 않고 숫자만 내려줘. 탈퇴한 회원은 세지 않아 |
| `bookmarked` | boolean | 내가 관심 동문으로 저장했는지 |

'연결된 동문'은 커피챗이 한 번이라도 수락된 적이 있거나(취소·완료돼도 유지, 여러 명이면 받는 사자와 각 신청자 사이, 수락 전에 빠진 사람은 제외) 메시지 요청을 수락해 대화 중인 사이야.

| 상태 | code | 언제 |
| --- | --- | --- |
| 404 | `NOT_FOUND` | 없는 회원, 탈퇴 회원, 승인 대기·반려 회원, 프로필 미완성 회원 |

### PROFILE-04 기본 정보 수정

`PUT /me/profile/basic` · 승인 대기·승인 회원

상단 소개 카드의 연필(✎) 팝업이야. 이름·기수는 받지 않아(승인 전에는 AUTH-06, 승인 후에는 관리자가 ADMIN-06으로 고쳐). 보낸 값으로 통째로 바꾸니까 안 바꾼 칸도 현재 값을 그대로 보내줘.

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `headline` | string | X | 한 줄 소개 0\~100자. 비우면 `null` |
| `major` | string | X | 주전공 0\~30자 |
| `minor` | string | X | 복수·부전공 0\~30자 |
| `region` | string | X | 활동 지역 0\~20자 예) 서울 강남 |
| `status` | string | O | `status` 1개 |
| `lionParts` | string\[\] | O | `lionPart` 1\~2개, 중복 불가 |
| `interests` | string\[\] | O | `interest` 1\~3개, 중복 불가 |

`200`이고 응답은 위 필드를 그대로 돌려줘. 현재 상태를 `EMPLOYED`에서 다른 값으로 바꿔도 경력은 그대로 두고, `EMPLOYED`로 바꿀 때도 회사·직무는 받지 않아(경력 PROFILE-11에서 채워).

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 글자 수 초과, 파트·관심 분야 개수 오류, 없는 열거값 |

### PROFILE-05 프로필 사진 변경·삭제

`PUT /me/profile/photo` · `DELETE /me/profile/photo` · 승인 대기·승인 회원

`PUT`은 `multipart/form-data`로 `file` 한 개를 받아. jpg·png·webp, 5MB 이하만 되고 새 사진이 이전 사진을 대체해. `200`에 `{ "photoUrl": "https://..." }`를 돌려줘. `DELETE`는 `204`이고 기본 사자 이미지로 돌아가서 이후 조회에서 `photoUrl`이 `null`이야.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `FILE_TOO_LARGE` | 5MB 초과 |
| 400 | `UNSUPPORTED_FILE_TYPE` | jpg·png·webp가 아님 |

### PROFILE-06\~09 한 칸짜리 섹션 수정 (소개·대표 기술·외부 링크·연락처)

모두 승인 대기·승인 회원이 쓰고, 보낸 값으로 통째로 바꿔. 응답은 `200`과 바뀐 값이야. 소개·대표 기술·외부 링크는 비워서 보내면(`null`·빈 배열) 그 섹션이 프로필에서 사라져. 연락처는 항상 보이는 섹션이라 비울 수 없어.

| ID | 경로 | 요청 필드 | 규칙 |
| --- | --- | --- | --- |
| PROFILE-06 | `PUT /me/profile/bio` | `bio` string | 0\~1000자, 줄바꿈 유지 |
| PROFILE-07 | `PUT /me/profile/skills` | `skills` string\[\] | 0\~5개, 태그당 1\~20자. 서버가 중복을 지우고(앞쪽 유지) 보낸 순서를 그대로 저장해. 6개 이상이면 400 |
| PROFILE-08 | `PUT /me/profile/links` | `links` object\[\] | 0\~5개 `{ type, url }`. `type`은 `linkType`, `url`은 http(s)로 시작. 보낸 순서 유지 |
| PROFILE-09 | `PUT /me/profile/contact` | `email` string, `phone` string | `email` 필수·이메일 형식(가입 때는 구글 이메일로 채워져 있고 다른 주소로 바꿀 수 있어). `phone` 선택, `010-0000-0000` 형식 |

연락처 이메일은 다른 동문에게 보여주는 용도야. 서비스 알림 메일은 이 값과 상관없이 항상 가입한 구글 이메일로 가.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 글자 수·개수 초과, URL·이메일·전화번호 형식 오류 (`fieldErrors`에 칸별로) |

### PROFILE-10 공개 범위 수정

`PATCH /me/profile/visibility` · 승인 대기·승인 회원

연락처·링크·경력 편집 팝업 안의 자물쇠 선택이야. 보낸 키만 바꾸고 안 보낸 키는 그대로야.

| 요청 필드 | 타입 | 허용값 |
| --- | --- | --- |
| `email` | string | `PUBLIC` `CONNECTED` `PRIVATE` |
| `phone` | string | `PUBLIC` `CONNECTED` `PRIVATE` |
| `links` | string | `PUBLIC` `CONNECTED` `PRIVATE` |
| `company` | string | `PUBLIC` `PRIVATE` (`CONNECTED`는 없어) |

`200`에 네 값을 모두 담은 `{ "visibility": { "email": "CONNECTED", "phone": "CONNECTED", "links": "PUBLIC", "company": "PUBLIC" } }`를 돌려줘. 조회 때마다 범위를 계산하니까 이미 연결된 동문에게도 바로 반영돼. 기본값은 이메일·전화번호 `CONNECTED`, 외부 링크·회사명 `PUBLIC`이야.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 없는 값, `company`에 `CONNECTED` |

### PROFILE-11 목록형 섹션 항목 추가·수정·삭제

경력·학력·멋사 활동·멋사 프로젝트·프로젝트·자격증·수상·대외활동·언어는 경로 모양이 모두 같아. 승인 대기·승인 회원이 쓸 수 있어.

| 메서드·경로 | 동작 | 성공 응답 |
| --- | --- | --- |
| `POST /me/{section}` | 항목 1개 추가 | `201`과 만들어진 항목(`id` 포함) |
| `PUT /me/{section}/{itemId}` | 항목 1개를 통째로 수정 | `200`과 바뀐 항목 |
| `DELETE /me/{section}/{itemId}` | 항목 1개 삭제 | `204` |

마지막 항목을 지우면 그 섹션은 프로필에서 사라지고, 따로 API는 없어. 조회할 때는 날짜 최신순으로 정렬하고 진행 중인 항목을 맨 위에 둬. 기간은 `"2024-03"` 형식의 `yearMonth` 문자열이야.

| 섹션 | `{section}` | 최대 | 요청 필드 (필수는 O 표시) |
| --- | --- | --- | --- |
| 경력 | `careers` | 10 | `company` O 1\~30자, `jobTitle` O 1\~30자, `employmentType` O (`FULL_TIME` `CONTRACT` `INTERN` `FREELANCE` `FOUNDER` `ETC`), `startYm` O, `endYm` 또는 `isCurrent: true` 중 하나 O, `description` 0\~300자 |
| 학력 | `educations` | 5 | `school` O 1\~30자, `major` 0\~30자, `degree` (`BACHELOR` `MASTER` `DOCTOR` `EXCHANGE_ETC`), `startYm`, `endYm` 또는 `isCurrent: true`, `description` 0\~200자 |
| 멋사 활동 | `lion-activities` | 6 | `generation` O 1\~14, `role` O (`LION` 아기사자, `STAFF` 운영진, `PRESIDENT` 대표·부대표, `PART_LEAD` 파트장, `ETC`), `parts` O `lionPart` 1\~2개 |
| 멋사 프로젝트·해커톤 | `lion-projects` | 5 | `name` O 1\~30자, `eventType` O (`REGULAR` 정기 프로젝트, `JUNGKATHON` 중커톤, `UNION_HACKATHON` 연합 해커톤, `ETC`), `generation` O 1\~14, `myRole` 0\~30자, `description` 0\~200자, `link` URL |
| 프로젝트 | `projects` | 10 | `name` O 1\~30자, `startYm`, `endYm` 또는 `isCurrent: true`, `role` 0\~30자, `description` 0\~300자, `link` URL, `tags` string\[\] 0\~5개 |
| 자격증 | `certifications` | 10 | `name` O 1\~30자, `issuer` 0\~30자, `acquiredYm` |
| 수상 | `awards` | 10 | `name` O 1\~30자, `host` 0\~30자, `awardedYm`, `description` 0\~100자 |
| 대외활동 | `activities` | 10 | `name` O 1\~30자, `organization` 0\~30자, `startYm`, `endYm`, `description` 0\~200자 |
| 언어 | `languages` | 5 | `language` O 1\~20자, `level` O (`BASIC` 기초, `DAILY` 일상 회화, `BUSINESS` 비즈니스, `NATIVE` 원어민) |

공통 검증 규칙이야.

- `startYm`은 이번 달까지만 되고, `endYm`은 `startYm`보다 빠를 수 없어. `isCurrent: true`면 `endYm`을 같이 보내지 않아(보내면 400).
- 경력은 `startYm`이 필수지만, 가입·온보딩에서 자동으로 만든 항목은 비어 있을 수 있어. 그 항목을 수정해서 저장할 때 채우면 돼.
- 멋사 활동에서 가입 기수 항목은 `isDefault: true`로 내려가. 이 항목은 `role`과 `parts`만 바꿀 수 있고 `generation`은 못 바꾸고 지울 수도 없어.
- 같은 기수의 멋사 활동, 같은 언어는 두 번 추가할 수 없어.

항목 모양 예시(경력)야.

```json
{
  "id": 31, "company": "핀테크A", "jobTitle": "백엔드 개발자",
  "employmentType": "FULL_TIME", "startYm": "2024-03", "endYm": null,
  "isCurrent": true, "description": "결제 API 개발"
}
```

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 필수값 누락, 글자 수 초과, 종료가 시작보다 빠름, 시작이 미래, `isCurrent`와 `endYm` 동시 입력 |
| 403 | `DEFAULT_ITEM_NOT_DELETABLE` | 멋사 활동의 가입 기수 항목을 지우려 함 |
| 404 | `NOT_FOUND` | 없는 항목이거나 남의 항목 |
| 409 | `LIMIT_EXCEEDED` | 섹션 최대 개수 초과 |
| 409 | `DUPLICATE_ITEM` | 같은 기수·같은 언어를 또 추가 |

### PROFILE-12 커피챗 받기 설정 조회

`GET /me/coffee-settings` · 승인 대기·승인 회원

마이페이지 > 커피챗 설정 화면이야. 지난 기간은 목록에서 빼고 내려줘.

```json
{
  "defaultMode": "ON",
  "acceptGroup": true,
  "topics": ["CAREER", "PROJECT_DEV"],
  "modes": ["ONLINE", "OFFLINE"],
  "availableSlots": [{ "day": "MON", "times": ["EVENING"] }],
  "preferredArea": "강남",
  "periodKind": "REST",
  "periods": [{ "id": 5, "startDate": "2026-11-20", "endDate": "2026-11-27" }],
  "status": { "availability": "AVAILABLE", "nextAvailableDate": null }
}
```

| 필드 | 설명 |
| --- | --- |
| `defaultMode` | `ON` 받기, `OFF` 안 받기 |
| `topics` | `JOB_PREP` 취업 준비, `CAREER` 커리어·이직, `STUDY` 학업·전공, `PROJECT_DEV` 프로젝트·개발, `STARTUP` 창업, `LION_ACTIVITY` 멋사 활동, `ETC` 기타. 비어 있어도 돼 |
| `modes` | `ONLINE` `OFFLINE`, 1개 이상 |
| `availableSlots` | 요일 `day`(`MON`\~`SUN`)별 `times`(`MORNING` `AFTERNOON` `EVENING`) |
| `periodKind` | 기본이 `ON`이면 `REST`(쉬는 기간), `OFF`면 `OPEN`(커피챗 받는 기간). 기간의 의미를 알려주려고 내려줘 |
| `status.availability` | 오늘 기준 `AVAILABLE` 받는 중, `RESTING` 쉬는 중, `NOT_ACCEPTING` 안 받는 중 |
| `status.nextAvailableDate` | `RESTING`이면 쉬는 기간 종료일 다음 날, `NOT_ACCEPTING`이면 곧 시작하는 받는 기간의 시작일, 그 밖에는 `null` |

'지금 받는 중'은 `defaultMode`와 오늘이 기간에 들어 있는지를 합쳐서 서버가 정해. `ON`이고 기간 밖이거나, `OFF`이고 `OPEN` 기간 안이면 받는 중이야.

### PROFILE-13 커피챗 받기 설정 저장

`PUT /me/coffee-settings` · 승인 대기·승인 회원

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `defaultMode` | string | O | `ON` `OFF` |
| `acceptGroup` | boolean | O | 여러 명 커피챗 받기 |
| `topics` | string\[\] | X | `topics` 값. 비워도 돼(신청자는 기타만 고를 수 있어) |
| `modes` | string\[\] | O | 1개 이상 |
| `availableSlots` | object\[\] | X | 중복 요일 불가 |
| `preferredArea` | string | X | 0\~30자 |
| `confirmClearPeriods` | boolean | X | `defaultMode`를 바꿀 때 등록된 기간이 있으면 `true`여야 해 |

`defaultMode`를 바꾸면 기간의 의미가 반대가 되니까 등록된 기간을 모두 지워. 그래서 기간이 있는 상태에서 `confirmClearPeriods` 없이 바꾸려고 하면 `409`로 막고, FE가 '등록한 기간이 모두 지워져요. 바꿀까요?'를 띄운 뒤 `true`로 다시 보내. `200`이고 응답은 PROFILE-12 형식이야.

`acceptGroup`을 끄면 이미 받은 여러 명 신청과 진행 중인 커피챗은 그대로 두고, 나를 신청할 사자로 고른 모집 중인 커피챗 모집글(4.11)은 신청할 수 없게 되고 작성자에게 알림이 가. 모집글 쪽 처리는 커뮤니티 API 담당 범위야.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `modes`가 비어 있음('가능한 방식을 1개 이상 골라주세요'), 없는 열거값, 글자 수 초과 |
| 409 | `PERIODS_WILL_BE_CLEARED` | 기간이 있는데 `confirmClearPeriods` 없이 `defaultMode`를 바꾸려 함 |

### PROFILE-14 홈 스위치 (받기·쉬기)

`POST /me/coffee-settings/switch` · 승인 대기·승인 회원

홈 화면 '커피챗 받기' 카드(8.1)의 스위치야. 지금 상태에 따라 서버가 알아서 처리해.

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `on` | boolean | O | `true` 켜기, `false` 끄기 |
| `restFor` | string | 끌 때 조건부 | `WEEK_1` `WEEK_2` `MONTH_1` `CUSTOM` `UNTIL_RESUME`. 쉬는 중이 아닌데 `on: false`일 때만 의미가 있어 |
| `startDate`, `endDate` | date | `CUSTOM`일 때 | PROFILE-15와 같은 검증 |
| `confirmClearPeriods` | boolean | X | 기본 상태가 바뀌는데 등록된 기간이 있을 때 `true` |

| 지금 상태 | 요청 | 서버가 하는 일 |
| --- | --- | --- |
| 받는 중 (`ON`, 기간 밖) | 끄기 | `WEEK_1`·`WEEK_2`·`MONTH_1`은 오늘부터 기간을 만들어 쉬는 기간으로 등록. `CUSTOM`은 받은 날짜로 등록. `UNTIL_RESUME`은 `defaultMode`를 `OFF`로 바꿔(기간이 있으면 지워지니 `confirmClearPeriods` 필요) |
| 받는 중 (`OFF`, 받는 기간 안) | 끄기 | 진행 중인 받는 기간을 어제까지로 줄여. 오늘 시작한 기간이면 지워 |
| 쉬는 중 (`ON`, 쉬는 기간 안) | 켜기 | 진행 중인 쉬는 기간을 어제까지로 줄여. 오늘 시작한 기간이면 지워 |
| 안 받는 중 (`OFF`, 기간 밖) | 켜기 | `defaultMode`를 `ON`으로 바꿔(기간이 있으면 지워지니 `confirmClearPeriods` 필요) |
| 이미 원하는 상태 | 같은 요청 | 아무것도 안 바꾸고 `200` |

`200`이고 응답은 PROFILE-12 형식이야. 쉬는 기간이 이미 5개면 기간 종류(`WEEK_1` 등)는 `409 PERIOD_LIMIT`로 막고 `UNTIL_RESUME`만 받아. 연달아 눌러서 중복 요청이 와도 결과가 같게 처리해(이미 원하는 상태면 그대로).

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 끌 때 `restFor` 누락, `CUSTOM`인데 날짜 누락·형식 오류 |
| 409 | `PERIOD_LIMIT` | 기간이 이미 5개인데 `UNTIL_RESUME`이 아닌 기간으로 끄려 함 |
| 409 | `PERIODS_WILL_BE_CLEARED` | 기본 상태가 바뀌는데 기간이 있고 `confirmClearPeriods`가 없음 |

### PROFILE-15 기간 추가·삭제

`POST /me/coffee-periods` · `DELETE /me/coffee-periods/{id}` · 승인 대기·승인 회원

기본 상태가 `ON`이면 쉬는 기간을, `OFF`면 커피챗 받는 기간을 등록해. 날짜 단위로 동작하고 시작일 00:00부터 종료일 23:59까지야. 기간이 시작·종료되면 상태가 자동으로 바뀌니까 따로 호출할 필요가 없어.

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `startDate` | date | O | 오늘부터 1년 이내 |
| `endDate` | date | O | 시작일 이후, 시작일부터 최대 180일 |
| `mergeOverlap` | boolean | X | 기본 `false`. `true`면 겹치는 기간을 하나로 합쳐서 저장해(거절 팝업의 빠른 쉬기 4.5용) |

`201`이고 응답은 `{ "id": 6, "startDate": "2026-11-20", "endDate": "2026-11-27" }`야. 합쳐서 저장했다면 합쳐진 기간이 돌아와. `DELETE`는 `204`야. 기간은 최대 5개이고, 쉬는 중에도 이미 받은 신청과 진행 중인 커피챗은 그대로 진행돼. 대기 중 신청의 14일 만료도 멈추지 않아.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 종료일이 시작일보다 빠름('종료일이 시작일보다 빨라요'), 시작일이 과거·1년 초과, 180일 초과 |
| 404 | `NOT_FOUND` | 없는 기간이거나 남의 기간 |
| 409 | `PERIOD_OVERLAP` | 이미 등록한 기간과 겹침('이미 등록한 기간과 겹쳐요') |
| 409 | `PERIOD_LIMIT` | 이미 5개 |

## 3. 관리자

관리자 메뉴는 가입 승인(1.2.1)이 전부야. 모든 `/admin/**` API는 서버가 `role = ADMIN`인지 확인하고, 일반 회원이 부르면 `403 FORBIDDEN`을 줘(승인 대기·반려 회원은 그 앞에서 `403 NOT_APPROVED`로 막혀). 관리자 지정·해제는 DB에서 직접 하고(MVP), 관리자 계정은 승인 절차 없이 `APPROVED`로 시작해. 프로필 메뉴의 \[관리자 페이지\] 노출(10.1)은 AUTH-05 응답의 `role`로 FE가 판단하니까 따로 API가 없어.

| ID | 기능 | 메서드·경로 | 명세 |
| --- | --- | --- | --- |
| ADMIN-01 | 가입 목록 조회(탭별) | `GET /admin/signups` | 1.2.1 |
| ADMIN-02 | 승인 대기 건수 | `GET /admin/signups/pending-count` | 1.2.1 |
| ADMIN-03 | 가입 승인 | `POST /admin/signups/{userId}/approve` | 1.2.1 |
| ADMIN-04 | 가입 반려 | `POST /admin/signups/{userId}/reject` | 1.2.1 |
| ADMIN-05 | 선택 승인 | `POST /admin/signups/approve-batch` | 1.2.1 |
| ADMIN-06 | 이름·기수 수정 | `PATCH /admin/signups/{userId}/signup-info` | 1.2.1 |

이 문서에 없는 관리자 기능도 있어.

- 게시글·댓글 삭제(6.4\~6.6)는 작성자 본인과 같은 삭제 API를 쓰고, 서버가 관리자는 남의 글·댓글도 삭제할 수 있게 허용해(수정은 불가). 커뮤니티 API 담당 범위야.
- 인기 키워드 제외 목록(6.1)은 MVP에서 DB(`keyword_blocklist`)에 직접 추가하니까 API가 없어. 여기 있는 단어가 들어간 키워드는 인기 키워드(POST-08)에서 빠져.
- 새 가입이 들어올 때 관리자 전원에게 가는 서비스 안 알림은 AUTH-02가 만들어.

### ADMIN-01 가입 목록 조회

`GET /admin/signups?status=PENDING&page=0&size=20` · 관리자

관리자 > 가입 승인 화면의 탭(승인 대기 / 승인됨 / 반려됨) 하나를 불러와. 탈퇴한 회원은 어느 탭에도 나오지 않아(승인 대기 중에 탈퇴하면 목록에서 사라짐).

| 쿼리 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `status` | string | X | `PENDING` 승인 대기(기본), `APPROVED` 승인됨, `REJECTED` 반려됨 |
| `page`, `size` | number | X | 기본 0, 20. `size` 최대 100 |

정렬은 승인 대기가 가입이 오래된 순, 승인됨·반려됨은 처리 시각이 최신인 순이야.

```json
{
  "items": [
    {
      "userId": 31, "name": "김사자", "generation": 13,
      "email": "lion@gmail.com", "googleName": "Lion Kim",
      "signedUpAt": "2026-11-07T14:20:00+09:00",
      "lionParts": ["BACKEND"], "currentStatus": "EMPLOYED", "company": "핀테크A",
      "sameNameGeneration": true,
      "reapplied": true, "reapplyAt": "2026-11-08T09:10:00+09:00",
      "rejectReason": "이름·기수가 맞지 않아요",
      "reviewedAt": null, "reviewedBy": null
    }
  ],
  "page": 0, "size": 20, "totalElements": 1, "hasNext": false
}
```

| 필드 | 설명 |
| --- | --- |
| `name`, `generation` | 가입할 때 본인이 입력한 값 |
| `googleName` | 구글 계정 이름. 입력 이름과 대조하는 용도 |
| `lionParts`, `currentStatus`, `company` | 온보딩(PROFILE-01)에서 입력한 값. 아직 입력 전이면 각각 `[]`, `null`, `null`. `company`는 현재 재직 중 경력의 회사명이고 비공개 설정과 상관없이 관리자에게 보여줘 |
| `sameNameGeneration` | 이미 승인된 회원 중 이름·기수가 같은 사람이 있으면 `true`(주황 '같은 이름·기수 있음') |
| `reapplied` | 반려된 뒤 다시 신청했으면 `true`(회색 '재신청'). `reapplyAt`은 그 시각 |
| `rejectReason` | 반려됨 탭이거나 재신청한 사람이면 이전 반려 사유, 아니면 `null` |
| `reviewedAt`, `reviewedBy` | 승인됨·반려됨 탭에서 처리한 시각과 관리자 이름. 승인 대기면 `null`(재신청자도 마찬가지). 승인 절차 없이 `APPROVED`로 시작한 계정(관리자 등)도 `null`이고, 승인됨 탭에서 맨 뒤에 나와 |

자동 표시(`sameNameGeneration`, `reapplied`)는 참고용이야. 자동 승인은 없고 최종 판단은 관리자가 해.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 없는 `status`, `size` 100 초과 |
| 403 | `FORBIDDEN` | 관리자가 아님 |

### ADMIN-02 승인 대기 건수

`GET /admin/signups/pending-count` · 관리자

관리자 메뉴 \[가입 승인\] 옆 빨간 숫자야. 응답은 `200`에 `{ "pending": 3 }`이고, 0이면 FE가 숫자를 숨겨. 탈퇴한 회원은 세지 않아.

| 상태 | code | 언제 |
| --- | --- | --- |
| 403 | `FORBIDDEN` | 관리자가 아님 |

### ADMIN-03 가입 승인

`POST /admin/signups/{userId}/approve` · 관리자 · 요청 본문 없음

승인 대기 회원을 승인하고, 반려됨 탭의 \[승인으로 바꾸기\]도 같은 API를 써. 서버가 하는 일은 이래.

- `status`를 `APPROVED`로 바꾸고 처리한 관리자와 시각(`reviewed_by`, `reviewed_at`)을 기록해. 반려됐던 회원이면 반려 사유는 비워.
- 본인에게 서비스 안 알림(계정 탭, 가입 승인됨)과 승인 메일(9.2)을 만들어. 승인 메일은 알림 설정으로 끌 수 없는 메일이라 항상 보내.
- 이 시점부터 다른 회원에게 노출되고 모든 기능이 열려. 따로 처리할 건 없고 `status` 기준이야.

`200`이고 응답은 `{ "userId": 31, "name": "김사자", "status": "APPROVED", "reviewedAt": "2026-11-08T10:00:00+09:00" }`야. '{이름}님을 승인했어요' 토스트는 이 `name`으로 FE가 만들어.

두 관리자가 같은 사람을 동시에 처리하면 먼저 처리한 쪽만 반영돼. 서버는 `status`가 `PENDING` 또는 `REJECTED`일 때만 바꾸는 조건부 갱신으로 처리하고, 나중 사람은 `409`를 받아 목록을 새로고침해.

| 상태 | code | 언제 |
| --- | --- | --- |
| 403 | `FORBIDDEN` | 관리자가 아님 |
| 404 | `NOT_FOUND` | 없는 회원이거나 탈퇴한 회원 |
| 409 | `ALREADY_PROCESSED` | 이미 승인됨('이미 처리된 가입이에요') |

### ADMIN-04 가입 반려

`POST /admin/signups/{userId}/reject` · 관리자

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `reasonType` | string | O | `UNVERIFIED` 동문인지 확인할 수 없어요, `MISMATCH` 이름·기수가 맞지 않아요, `ETC` 기타 |
| `reasonText` | string | `ETC`일 때 O | 5\~100자. `ETC`가 아니면 무시해 |

승인 대기 회원만 반려할 수 있어. 서버는 `status`를 `REJECTED`로 바꾸고 처리자·시각과 사유를 기록해. `UNVERIFIED`·`MISMATCH`는 위 고정 문구가, `ETC`는 입력한 `reasonText`가 사유로 저장돼. 반려는 알림도 메일도 보내지 않고, 본인이 다음에 접속하면 AUTH-05 응답의 `status`와 `rejectReason`으로 반려 안내 화면(1.4.1)을 보여줘.

`200`이고 응답은 `{ "userId": 31, "status": "REJECTED", "rejectReason": "이름·기수가 맞지 않아요" }`야.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `ETC`인데 `reasonText`가 5자 미만이거나 100자 초과, 없는 `reasonType` |
| 403 | `FORBIDDEN` | 관리자가 아님 |
| 404 | `NOT_FOUND` | 없는 회원이거나 탈퇴한 회원 |
| 409 | `ALREADY_PROCESSED` | 승인 대기가 아님(이미 승인·반려됨) |

### ADMIN-05 선택 승인

`POST /admin/signups/approve-batch` · 관리자

여러 명을 체크해서 \[선택 승인\]으로 한 번에 승인해.

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `userIds` | number\[\] | O | 1\~50개, 중복 불가(중복이면 `400`) |

한 명씩 ADMIN-03과 같은 규칙으로 따로 처리하고, 일부가 실패해도 나머지는 승인돼. 그래서 `200`에 성공한 사람과 못 한 사람을 나눠서 줘. `failed.code`는 `ALREADY_PROCESSED`(이미 승인됨) 또는 `NOT_FOUND`(없는 회원·탈퇴한 회원)야.

```json
{
  "succeeded": [
    { "userId": 31, "name": "김사자", "status": "APPROVED" },
    { "userId": 32, "name": "이사자", "status": "APPROVED" }
  ],
  "failed": [
    { "userId": 33, "code": "ALREADY_PROCESSED" },
    { "userId": 34, "code": "NOT_FOUND" }
  ]
}
```

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `userIds`가 비었거나 50개 초과 |
| 403 | `FORBIDDEN` | 관리자가 아님 |

### ADMIN-06 이름·기수 수정

`PATCH /admin/signups/{userId}/signup-info` · 관리자

가입자가 이름·기수를 잘못 입력했을 때 목록에서 연필(✎)로 바로 고쳐. 승인된 뒤에는 회원이 직접 못 고치니까 승인됨 탭의 회원도 이 API로 고쳐.

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `name` | string | O | 2\~20자 |
| `generation` | number | O | 1\~14 |

`200`이고 응답은 `{ "userId": 31, "name": "김사자", "generation": 12 }`야. 기수를 바꾸면 가입 때 자동으로 만들어진 멋사 활동 항목(`isDefault`)의 기수도 같이 바꿔(확정이 필요한 것들 참고). 같은 이름·기수 표시(`sameNameGeneration`)는 목록을 볼 때마다 다시 계산하니까 따로 처리할 게 없어.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 이름 길이·기수 범위 오류 |
| 403 | `FORBIDDEN` | 관리자가 아님 |
| 404 | `NOT_FOUND` | 없는 회원이거나 탈퇴한 회원 |
| 409 | `DUPLICATE_ITEM` | 바뀔 기수에 이미 다른 멋사 활동이 있음(확정이 필요한 것들 3번) |

## 4. 확정이 필요한 것들

명세에 적혀 있지 않아서 제가 정해서 쓴 부분이야. 팀과 맞추고 나면 그대로 확정해도 돼.

| 번호 | 무엇을 | 이 문서의 제안 | 맞출 사람 |
| --- | --- | --- | --- |
| 1 | 열거값의 영문 이름(`lionPart`, `status`, `interest`, `topics`, 각 `scope` 등) | 이 문서의 이름을 그대로 쓴다. DB 컬럼에 들어가는 값이라 SQL의 주석과도 맞춘다 | 커피챗·채팅·커뮤니티 API 담당, FE |
| 2 | 반려 후 재신청(AUTH-07) 때 이전 반려 사유 | 지우지 않고 남겨둔다. 관리자 목록이 재신청 사람에게 이전 사유를 보여주려면 필요하다. 승인할 때(ADMIN-03)만 비운다 | BE |
| 3 | 관리자가 기수를 고칠 때(ADMIN-06)·회원이 승인 전에 고칠 때(AUTH-06) 자동 생성된 멋사 활동 항목의 기수 | 같이 바꾼다. 바뀔 기수에 이미 다른 멋사 활동(운영진 등)이 있으면 자동 생성 항목을 그 항목과 합치지 않고 `409 DUPLICATE_ITEM`으로 막는다 | BE, 기획 |
| 4 | 기본 정보에서 멋사 파트를 수정하면(PROFILE-04) 자동 생성 멋사 활동 항목의 파트 | 바꾸지 않는다. 두 값은 각자 따로 수정하고 세 화면(2.2 소개 카드, 멋사 활동 섹션)에 각각 보인다. 같이 바꾸고 싶으면 알려줘 | 기획, BE |
| 5 | 프로필 사진 저장소(S3 등), 원본 보관·리사이즈, 사진을 바꾸거나 지웠을 때 이전 파일 삭제 | 저장소는 팀이 정한 것을 쓰고, 이전 파일은 바로 지운다. `photoUrl`은 공개 주소로 내려준다(만료 없음) | BE |
| 6 | 연락처 이메일(PROFILE-09)을 다른 주소로 바꿀 때 본인 확인 | 확인 메일 없이 형식만 검사한다(알림 메일은 가입 이메일로만 나가서 위험하지 않다) | BE |
| 7 | 승인 메일(ADMIN-03)이 알림 설정의 영향을 받는지 | **변경(2026-11-13)**: 받는다. 알림 설정 화면(N-01)에 맞춰 가입 승인 메일 받기를 설정에 넣었다(NOTI-04) | 완료 |
| 8 | 관리자 계정이 승인됨 탭에 나오는지 | 나온다(`role`과 관계없이 `status` 기준). 필요하면 제외한다 | BE, FE |
| 9 | 목록 조회의 정렬·페이지 크기 | 승인 대기는 오래된 순, 나머지 탭은 처리 시각 최신순, 기본 20개(최대 100) | FE |
| 10 | 클라이언트 입력 기간 형식 | 연월은 `"2024-03"`, 날짜는 `"2026-11-20"`, 시각은 KST ISO-8601 | FE |
| 11 | 승인 전 회원 차단 코드 | **확정(2026-11-12)**: 공용 코드·다른 영역 명세와 같은 `403 NOT_APPROVED` 하나로 통일. 승인 대기·반려 구분은 FE가 `GET /me`의 `status`로 한다. 가입 정보 수정(AUTH-06)을 반려 회원이 부르면 상태 충돌이라 `409 NOT_PENDING` | 완료 |
| 12 | 탈퇴(AUTH-08) 본인 재확인 | 구글 로그인을 Authorization Code 방식으로 바꿔 FE가 ID 토큰을 받지 않는다. 구글 재로그인 리다이렉트 등으로 다시 설계한다 | BE, FE |

리프레시 토큰은 Redis 대신 DB `refresh_tokens` 테이블에 SHA-256 해시로 저장하기로 정했다(2026-11-12).

## 5. 동문 찾기

담당: 조상원(동문·커뮤니티·홈). 노션 초안(3-1~3-5)을 프론트 화면에 맞춰 구현한 기준이야. 모두 승인 회원(`APPROVED`)만 쓸 수 있어.

> 5~7장에서 **(구현 예정)**이 붙은 필드·쿼리·API는 프론트 화면에 필요해서 형식을 먼저 정해 둔 거야. 서버에는 아직 없으니 FE는 구현 완료 표시가 바뀐 뒤 써 줘.

| ID | 기능 | 메서드·경로 | 노션 |
| --- | --- | --- | --- |
| ALUMNI-01 | 동문 목록·검색 | `GET /users` | 3-1 |
| ALUMNI-02 | 동문 프로필 상세 | `GET /users/{userId}` | 3-2 (= PROFILE-03) |
| ALUMNI-03 | 관심 동문 목록 | `GET /users/me/bookmarks` | 3-3 |
| ALUMNI-04 | 관심 동문 저장 | `PUT /users/{userId}/bookmark` | 3-4 |
| ALUMNI-05 | 관심 동문 저장 취소 | `DELETE /users/{userId}/bookmark` | 3-5 |

### ALUMNI-01 동문 목록·검색

`GET /users` · 승인 회원

승인되고 탈퇴하지 않았고 기본 프로필이 있는 회원만 내려줘. 나 자신은 목록에서 빠져.

| 쿼리 | 타입 | 설명 |
| --- | --- | --- |
| `q` | string | 0\~50자. 이름, 한 줄 소개, 대표 기술, 현재 경력의 직무·회사(회사 공개인 사람만)에서 대소문자 없이 찾아 |
| `generations` | number\[\] | 기수. 여러 개는 쉼표로 (`13,14`) |
| `parts` | string\[\] | `lionPart`. 파트 1·2 둘 다 봐 |
| `statuses` | string\[\] | 현재 상태(`status` 열거값) |
| `interests` | string\[\] | 관심 분야(`interest` 열거값 코드, 예: `PLANNING_PM,DATA_AI`). 값이 정확히 같아야 걸려 |
| `available` | boolean | `true`면 오늘 기준 커피챗을 받는(`AVAILABLE`) 사자만 |
| `sort` | string | `RECOMMEND` 추천순(받는 중 먼저, 그다음 나와 같은 파트), `GENERATION` 기수 오름차순, `LATEST` 최근 가입순. 기본 `RECOMMEND`. 순서가 같으면 최근 가입순 |
| `page`, `size` | number | 기본 0, 20. `size`는 1\~100 |

같은 조건 안의 값은 OR, 다른 조건끼리는 AND야. 열거값은 PROFILE의 열거값 표와 같아.

```json
{
  "items": [{
    "id": 25, "name": "김사자", "generation": 13, "lionParts": ["BACKEND"], "status": "EMPLOYED",
    "headline": "백엔드 개발자", "bio": "서버 개발 이야기를 좋아해요", "photoUrl": null, "company": "예시회사",
    "interests": ["BACKEND"], "skills": ["Spring"], "bookmarked": false,
    "coffeeChat": { "availability": "AVAILABLE", "nextAvailableDate": null, "acceptGroup": true }
  }],
  "page": 0, "size": 20, "totalElements": 1, "hasNext": false
}
```

| 필드 | 설명 |
| --- | --- |
| `company` | 현재 경력의 회사. 회사 공개 범위로 가려지면 `null` |
| `bio` | 카드에 2줄로 보여주는 소개 |
| `coffeeChat.availability` | PROFILE-12의 `status.availability`와 같은 규칙으로 오늘 기준으로 정해. `AVAILABLE` 받는 중, `RESTING` 쉬는 중, `NOT_ACCEPTING` 안 받는 중. 커피챗 설정이 없으면 `NOT_ACCEPTING` |
| `coffeeChat.nextAvailableDate` | 다시 받는 날. 카드의 '11/20부터 받아요'에 써. `RESTING`이면 쉬는 기간이 끝난 다음 날(바로 이어지는 쉬는 기간이 있으면 마지막 기간 기준), `NOT_ACCEPTING`이면 곧 시작하는 받는 기간의 시작일, 그 밖에는 `null` |
| `coffeeChat.acceptGroup` | 여러 명 신청을 받는 설정인지. 커피챗 모집 글의 사자는 `AVAILABLE`이면서 `true`인 사자만 고를 수 있어 |

이 항목 형식은 ALUMNI-03, HOME-01 `recommendedSeniors`, 동문 상세 오른쪽 목록에서도 같이 써.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `size` 범위, 검색어 50자 초과, 없는 열거값(`fieldErrors`에 파라미터 이름) |

### ALUMNI-02 동문 프로필 상세

`GET /users/{userId}` · 승인 회원

응답은 PROFILE-03 형식이야. 구현하면서 정한 세부 사항과, 프론트 상세 화면 때문에 더한 필드를 적어 둘게.

```json
{
  "userId": 12, "name": "김사자", "generation": 13, "photoUrl": null, "headline": "풀스택 개발자",
  "bio": "...", "major": null, "minor": null, "region": "서울", "status": "EMPLOYED",
  "lionParts": ["BACKEND", "FRONTEND"], "interests": ["DATA_AI"], "skills": ["Spring"],
  "links": [{ "type": "GITHUB", "url": "https://github.com/kim" }],
  "contact": { "email": "kim@test.com" },
  "careers": [{ "id": 31, "company": "라이온컴퍼니", "companyHidden": false, "jobTitle": "백엔드 개발자", "employmentType": "FULL_TIME", "startYm": "2024-03", "endYm": null, "isCurrent": true, "description": null }],
  "educations": [], "lionActivities": [], "lionProjects": [], "projects": [],
  "certifications": [], "awards": [], "activities": [], "languages": [],
  "isStaff": false,
  "coffeeChat": { "availability": "AVAILABLE", "nextAvailableDate": null, "acceptGroup": true, "topics": ["CAREER"], "modes": ["ONLINE"], "availableSlots": [], "preferredArea": "강남", "pendingCoffeeChatId": null },
  "chat": { "status": "NONE", "requestAvailableDate": null },
  "commonalities": [{ "type": "SAME_GENERATION", "label": "13기" }],
  "mutualCount": 0,
  "bookmarked": false
}
```

| 항목 | 정한 내용 |
| --- | --- |
| 가려진 값 | `contact.email`, `contact.phone`, `links`는 키째 빠져. 둘 다 가려지면 `contact`는 `{}` |
| `careers[].companyHidden` | 회사명이 비공개면 경력마다 `company` 키가 빠지고 `companyHidden: true`. 직무(`jobTitle`)는 그대로 |
| 섹션 항목 필드 | PROFILE-11 이름을 써. 자격증 `issuer`·`acquiredYm`, 수상 `host`·`awardedYm` |
| 섹션 순서 | 경력·학력·프로젝트는 진행 중 먼저 최신순, 멋사 활동·멋사 프로젝트는 기수 최신순, 자격증·수상·대외활동은 날짜 최신순, 어학은 추가한 순 |
| `isStaff` (추가) | 멋사 활동 중 운영진(`STAFF`)·대표(`PRESIDENT`)·파트장(`PART_LEAD`)이 있으면 `true`. 이름 옆 '운영진' 태그 |
| `coffeeChat.availability` | ALUMNI-01의 수신 상태에 `IN_PROGRESS`가 더해져. 나와 이 사자 사이에 진행 중(`PENDING` `ACCEPTED` `SCHEDULED`)인 커피챗이 있거나, 이 사자가 받는 여러 명 커피챗에 내가 함께 신청자로 남아 있으면 `IN_PROGRESS`. 거절되거나 만료된 지 7일이 안 됐으면 `COOLDOWN`이고 `nextAvailableDate`는 다시 신청할 수 있는 날이야(COFFEE-01과 같은 규칙) |
| `coffeeChat.pendingCoffeeChatId` (추가) | 내가 이 사자에게 보낸 대기 중(`PENDING`) 신청 ID. '신청 대기 중 · 내 신청 보기' 링크에 써 |
| `chat` | 둘 사이의 1:1 방 기준. `REQUESTED`는 누가 보냈든 대기 중인 요청이 있을 때야. 내가 보낸 요청이 거절된 날짜 + 7일 전이면 `status: NONE`과 `requestAvailableDate`(거절된 날짜 + 7일) |
| `commonalities` | `SAME_LION_PROJECT` 같은 기수에 이름이 같은 멋사 프로젝트(`13기 라이언링크`), `SAME_COMPANY` 둘 다 지금 다니는 회사(`라이온컴퍼니`), `SAME_GENERATION`(`13기`), `SAME_PART`(`백엔드 파트`), `SHARED_INTEREST`(`데이터·AI`) 순서야. 한 종류가 여러 개면 각각 들어가 |
| `mutualCount` | 나와 연결된 동문 중 이 사자와도 연결된 사람 수. 탈퇴 회원은 빼 |
| 내 프로필을 볼 때 | 공개 범위 없이 모두 보이고 `commonalities: []`, `mutualCount: 0`, `chat.status: NONE`, `pendingCoffeeChatId: null` |

'연결된 동문'은 PROFILE-03 정의 그대로야. 커피챗은 수락 시각(`accepted_at`)이 있는지로 보고, 여러 명 커피챗은 수락 뒤에 빠진 함께 신청자도 연결로 쳐. 동문 목록(ALUMNI-01)의 회사 공개 판단도 같은 기준을 써.

| 상태 | code | 언제 |
| --- | --- | --- |
| 404 | `USER_NOT_FOUND` | 없는 회원, 승인 전, 탈퇴, 프로필이 없는 회원 |

### ALUMNI-03 관심 동문 목록

`GET /users/me/bookmarks?page=0&size=20` · 승인 회원

최근 저장한 순서야. 화면이 동문 목록과 같은 카드에 '최근 저장순 · N명'을 보여줘.

- 항목은 ALUMNI-01 항목 형식에 `bookmarkedAt`을 더한 것
- 전체 수가 있는 페이지 형식(`totalElements` 포함)
- 저장한 뒤 탈퇴한 동문은 목록에서 빠짐(`totalElements`에도 안 셈)

```json
{
  "items": [{ "id": 25, "name": "김사자", "generation": 13, "lionParts": ["BACKEND"], "...": "ALUMNI-01 항목과 같음", "bookmarkedAt": "2026-11-08T10:00:00+09:00" }],
  "page": 0, "size": 20, "totalElements": 1, "hasNext": false
}
```

### ALUMNI-04 관심 동문 저장 · ALUMNI-05 저장 취소

`PUT /users/{userId}/bookmark` · `DELETE /users/{userId}/bookmark` · 승인 회원

`PUT`은 `200`에 `{ "userId": 25, "bookmarked": true }`를 돌려줘. 이미 저장했으면 그대로 성공해. `DELETE`는 저장하지 않았어도 `204`야.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `CANNOT_BOOKMARK_SELF` | 자기 자신을 저장 |
| 404 | `USER_NOT_FOUND` | 없는 회원, 승인 전, 탈퇴 회원 |

## 6. 커뮤니티

담당: 조상원(동문·커뮤니티·홈). 노션 초안(6-1~6-15)을 프론트 화면에 맞춰 바꾼 기준이야. 모두 승인 회원만 쓸 수 있어.

| ID | 기능 | 메서드·경로 | 노션 |
| --- | --- | --- | --- |
| POST-01 | 게시글 목록·검색 | `GET /posts` | 6-1 |
| POST-02 | 카테고리별 게시글 수 | `GET /posts/category-counts` | 신규 |
| POST-03 | 게시글 작성 | `POST /posts` | 6-3 |
| POST-04 | 게시글 상세 | `GET /posts/{postId}` | 6-4 |
| POST-05 | 게시글 수정 | `PATCH /posts/{postId}` | 6-5 |
| POST-06 | 게시글 삭제 | `DELETE /posts/{postId}` | 6-6 (+ 6-15 번개 취소) |
| POST-07 | 좋아요 · 좋아요 취소 | `PUT` `DELETE /posts/{postId}/like` | 신규 |
| COMMENT-01 | 댓글 목록 | `GET /posts/{postId}/comments` | 6-7 |
| COMMENT-02 | 댓글 작성 | `POST /posts/{postId}/comments` | 6-8 |
| COMMENT-03 | 댓글 삭제 | `DELETE /posts/{postId}/comments/{commentId}` | 6-9 |
| MEETUP-01 | 번개 참여 · 참여 취소 | `PUT` `DELETE /posts/{postId}/meetup/members/me` | 6-12, 6-13 |
| MEETUP-02 | 번개 초대 | `POST /posts/{postId}/meetup/invites` | 6-14 |
| RECRUIT-01 | 커피챗 모집 참여 · 참여 취소 | `PUT` `DELETE /posts/{postId}/recruit/members/me` | 신규 (4.11). 참여 취소는 결정 후 추가 |
| POST-08 | 인기 키워드 | `GET /posts/popular-keywords` | 6-2 |
| FILE-01 | 업로드 주소 발급 | `POST /files` | 6-10 |

노션의 6-11 참여자 목록은 POST-04에 들어가서, 6-15 번개 취소는 POST-06이 대신해서 없앴어.

### 열거값

| 이름 | 값 |
| --- | --- |
| `category` | `CONTEST` 공모전, `JOB` 채용·협업, `MATERIAL` 족보·자료, `EVENT` 세미나·행사, `NOW` 지금 사자는, `FREE` 자유, `MEETUP` 번개, `RECRUIT` 커피챗 모집 |
| `meetup.status` | `OPEN` 모집 중, `FULL` 정원 마감, `ENDED` 모임 시각 지남, `CANCELED` 취소 |
| `recruit.status` | `OPEN` 모집 중, `APPLIED` 커피챗 신청 완료, `ENDED` 모집 종료. 마감일이 지난 `OPEN` 모집은 응답에서 `ENDED`로 보여줘. 마감일 당일까지는 `OPEN` |

### 공통 객체

- `author`: `{ id, name, generation, lionParts, photoUrl, headline, isDeleted }`. 익명 글·댓글은 `null`, 탈퇴한 작성자는 `isDeleted: true`이고 나머지는 `null`(FE는 '알 수 없음')
- `members[]`: `{ id, name, generation, lionParts, photoUrl, isHost, isDeleted }`. 작성자가 맨 앞

### POST-01 게시글 목록·검색

`GET /posts` · 승인 회원

| 쿼리 | 타입 | 설명 |
| --- | --- | --- |
| `category` | string\[\] | 없으면 전체. 여러 개는 쉼표로 (`CONTEST,JOB`). 홈 새 소식 탭에서 써 |
| `q` | string | 0\~50자. 제목·본문에서 대소문자 없이 찾아. 첫 페이지에서 결과가 있으면 인기 키워드(POST-08)에 남아 |
| `sort` | string | `LATEST` 최신순(기본), `VIEWS` 조회순 |
| `recruiting` | boolean | `true`면 모집 중인 번개(취소·종료 전, 정원 남음)와 커피챗 모집(`OPEN`, 마감일 전, 정원 남음)만 |
| `period` | string | `WEEK`면 최근 7일 글만 ('이번 주 많이 본 글'은 `period=WEEK&sort=VIEWS`) |
| `page`, `size` | number | 기본 0, 20. `size`는 1\~100 |

```json
{
  "items": [{
    "id": 101, "category": "MEETUP", "title": "금요일 저녁 번개", "isAnonymous": false,
    "author": { "id": 12, "name": "김사자", "generation": 13, "lionParts": ["BACKEND"], "photoUrl": null, "headline": null, "isDeleted": false },
    "viewCount": 42, "commentCount": 3, "hasFile": false, "thumbnailUrl": null, "deadline": null,
    "meetup": { "meetAt": "2026-11-20T19:00:00+09:00", "place": "흑석역 2번 출구", "capacity": 6, "participantCount": 3, "status": "OPEN" },
    "recruit": null,
    "createdAt": "2026-11-10T14:00:00+09:00"
  }],
  "page": 0, "size": 20, "totalElements": 1, "hasNext": false
}
```

- 커피챗 모집 글은 `recruit: { senior: { id, name, generation, isDeleted }, capacity, memberCount, deadline, status }`
- `deadline`은 공모전·채용 글의 마감일(D-n 태그). `thumbnailUrl`은 첫 이미지(없으면 `null`), `hasFile`은 첨부파일이나 이미지가 있으면 `true`
- 삭제된 글은 빠지고, `commentCount`는 삭제된 댓글을 빼고 세

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 없는 카테고리·정렬값, `size` 범위 (`fieldErrors`에 파라미터 이름) |

### POST-02 카테고리별 게시글 수

`GET /posts/category-counts` · 승인 회원

커뮤니티 왼쪽 카테고리 목록의 숫자야. 글이 없는 카테고리도 `0`으로 와.

```json
{ "total": 42, "counts": { "CONTEST": 5, "JOB": 3, "MATERIAL": 8, "EVENT": 2, "NOW": 6, "FREE": 12, "MEETUP": 4, "RECRUIT": 2 } }
```

### POST-03 게시글 작성

`POST /posts` · 승인 회원

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `category` | string | O | `category` |
| `title` | string | O | 2\~50자 |
| `content` | string | O | 1\~5000자 |
| `links` | string\[\] | X | 0\~3개, http(s) URL |
| `isAnonymous` | boolean | X | `FREE`에서만 `true` 가능 |
| `deadline` | date | X | `CONTEST`·`JOB`만. 오늘 이후 |
| `meetup` | object | `MEETUP`이면 O | `{ meetAt, place, capacity, companionIds }` |
| `meetup.meetAt` | datetime | O | 지금부터 1시간 뒤 \~ 14일 이내 |
| `meetup.place` | string | O | 1\~50자 |
| `meetup.capacity` | number | O | 2\~30명(작성자 포함) |
| `meetup.companionIds` | number\[\] | X | 함께하는 동문. 등록하면 바로 참여자가 돼. 승인 회원, 본인·중복 불가, `capacity - 1`명 이하 |
| `recruit` | object | `RECRUIT`이면 O | `{ seniorId, capacity, deadline }` |
| `recruit.seniorId` | number | O | 신청받을 사자. 본인 불가, 커피챗을 받고 여러 명 신청을 받는 사자만 |
| `recruit.capacity` | number | O | 2\~5명(작성자 포함) |
| `recruit.deadline` | date | O | 오늘 이후 |
| `files` | object\[\] | X | FILE-01로 올린 파일 `{ type(IMAGE·ATTACHMENT), url, name, size }`. 이미지 5장, 첨부파일 3개까지. `url`은 내가 발급받은 `fileUrl`만 돼. 보낸 순서대로 저장해 |

`201`에 `{ "id": 101, "category": "MEETUP", "createdAt": "..." }`를 돌려줘. 번개는 작성자와 함께하는 동문을, 커피챗 모집은 작성자를 참여자로 같이 저장해. 번개는 이 사람들이 들어간 단체 채팅방도 같이 열어(10장 "번개 단체방").

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 글자 수, 링크 형식·개수, 정원 범위 |
| 400 | `ANONYMOUS_NOT_ALLOWED` | `FREE`가 아닌데 익명 |
| 400 | `INVALID_CATEGORY_DETAIL` | 카테고리와 맞지 않는 `meetup`·`recruit`·`deadline` |
| 400 | `INVALID_MEETUP_TIME` | 번개 일정이 1시간 이내이거나 14일 넘음 |
| 400 | `INVALID_COMPANIONS` | 함께하는 동문에 본인·중복이 있거나 정원 초과 |
| 400 | `INVALID_RECRUIT_SENIOR` | 신청받을 사자로 본인을 고름 |
| 400 | `FILE_LIMIT_EXCEEDED` | 이미지 6장 이상, 첨부파일 4개 이상 |
| 400 | `INVALID_FILE_URL` | 내가 FILE-01로 발급받지 않은 파일 주소('올린 파일을 다시 확인해 주세요.') |
| 404 | `USER_NOT_FOUND` | 함께하는 동문이나 신청받을 사자가 없거나 승인 전·탈퇴 |
| 409 | `GROUP_COFFEE_CHAT_NOT_ALLOWED` | 신청받을 사자가 커피챗을 안 받거나 1:1만 받음 |

### POST-04 게시글 상세

`GET /posts/{postId}` · 승인 회원

같은 회원이 24시간 안에 다시 보면 조회수를 올리지 않아(`post_views`).

```json
{
  "id": 101, "category": "MEETUP", "title": "금요일 저녁 번개", "content": "...",
  "isAnonymous": false, "isMine": false,
  "author": { "id": 12, "name": "김사자", "generation": 13, "lionParts": ["BACKEND"], "photoUrl": null, "headline": null, "bio": null, "status": "EMPLOYED", "isDeleted": false },
  "links": [], "files": [], "deadline": null,
  "viewCount": 43, "commentCount": 3, "likeCount": 8, "likedByMe": false,
  "meetup": {
    "meetAt": "2026-11-20T19:00:00+09:00", "place": "흑석역 2번 출구", "capacity": 6,
    "participantCount": 3, "status": "OPEN", "joinedByMe": false, "members": []
  },
  "recruit": null,
  "createdAt": "2026-11-10T14:00:00+09:00", "updatedAt": null
}
```

- 커피챗 모집 글은 `recruit: { senior: { id, name, generation, lionParts, photoUrl, acceptGroup, isDeleted }, capacity, memberCount, deadline, status, joinedByMe, isSenior, hasOpenCoffeeChatWithSenior, members }`
- `author.bio`(소개)는 일반 글 오른쪽 작성자 카드에 써
- `recruit.senior.acceptGroup`은 그 사자가 오늘 커피챗을 받는 중(`AVAILABLE`)이면서 여러 명 신청을 받을 때만 `true`
- `isMine`은 익명 글이어도 내 글이면 `true`. `updatedAt`은 수정한 적이 있을 때만 값이 있어('수정됨' 표시)
- `files[]`는 `{ id, type(IMAGE·ATTACHMENT), url, name, size }`

| 상태 | code | 언제 |
| --- | --- | --- |
| 404 | `POST_NOT_FOUND` | 없거나 삭제된 글 |

### POST-05 게시글 수정

`PATCH /posts/{postId}` · 작성자만

보낸 필드만 바꿔. 필드는 POST-03과 같고 규칙도 같아(`isAnonymous`, `meetup.companionIds`는 없음).

- 일반 카테고리(`CONTEST` `JOB` `MATERIAL` `EVENT` `NOW` `FREE`)끼리는 자유롭게 바꿀 수 있어. 익명 글은 `FREE`에서 못 바꿔
- 번개·커피챗 모집 글은 작성자 말고 참여자가 없을 때만 다른 카테고리로 바꿀 수 있어. 바꾸면 모집 정보(번개는 초대 기록과 단체 채팅방까지)는 지워져
- 일반 글을 번개·커피챗 모집으로 바꿀 때는 `meetup`(`meetAt` `place` `capacity`) 또는 `recruit`(`seniorId` `capacity` `deadline`)를 모두 보내야 해. POST-03처럼 검증하고 작성자가 첫 참여자가 돼
- 마감일을 쓸 수 없는 카테고리로 바꾸면 `deadline`은 지워져
- `files`를 보내면 파일 목록을 통째로 바꿔(남길 파일도 다시 보내). 안 보내면 그대로, 빈 배열이면 모두 지워
- 번개: 시작되거나 취소된 번개는 수정할 수 없고, 정원은 지금 참여 인원보다 줄일 수 없어
- 커피챗 모집: 커피챗을 신청한 뒤(`APPLIED`)에는 모집 정보를 못 바꾸고, 참여자가 있으면 신청받을 사자를 못 바꿔

`200`에 `{ "id": 101, "title": "...", "updatedAt": "..." }`를 돌려줘.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `ANONYMOUS_NOT_ALLOWED` | 익명 글을 `FREE` 밖으로 옮김 |
| 400 | `INVALID_CATEGORY_DETAIL`, `INVALID_MEETUP_TIME` | POST-03과 같음 |
| 400 | `INVALID_RECRUIT_SENIOR` | 신청받을 사자를 본인으로 바꿈 |
| 403 | `NOT_OWNER` | 작성자가 아님 |
| 404 | `POST_NOT_FOUND` | 없거나 삭제된 글 |
| 404 | `USER_NOT_FOUND` | 바꾸려는 신청받을 사자가 없거나 승인 전·탈퇴 |
| 409 | `CATEGORY_CHANGE_NOT_ALLOWED` | 참여자가 있는 번개·커피챗 모집 글의 카테고리를 바꿈('참여자가 있어 카테고리를 바꿀 수 없어요.') |
| 409 | `MEETUP_CLOSED` | 시작되거나 취소된 번개 |
| 409 | `GROUP_COFFEE_CHAT_NOT_ALLOWED` | 바꾸려는 사자가 커피챗을 안 받거나 1:1만 받음 |
| 409 | `RECRUIT_CLOSED` | 신청 완료된 모집의 모집 정보를 바꿈 |
| 409 | `RECRUIT_SENIOR_LOCKED` | 참여자가 있는데 신청받을 사자를 바꿈 |
| 409 | `CAPACITY_BELOW_MEMBERS` | 정원을 참여 인원보다 적게 |

### POST-06 게시글 삭제

`DELETE /posts/{postId}` · 작성자 또는 관리자

소프트 삭제(`deleted_at`)이고 `204`야. 댓글도 화면에서 같이 사라져.

- 번개 글이면 번개도 취소(`meetups.canceled_at`)하고 작성자를 뺀 참여자에게 취소 알림을 저장해. 단체 채팅방에는 '번개가 취소됐어요'(이미 끝난 번개를 관리자가 지우면 '번개 글이 삭제됐어요')를 남기고 닫아. 프론트의 [번개 취소]가 이 API야
- 커피챗 모집 글은 신청 전(`OPEN`)에만 지울 수 있어. 프론트의 [모집 취소]가 이 API야. 마감 전이면 작성자를 뺀 참여자에게 취소 알림을 저장해
- 관리자는 위 상태 제한 없이 지울 수 있어

| 상태 | code | 언제 |
| --- | --- | --- |
| 403 | `NOT_OWNER` | 작성자도 관리자도 아님 |
| 404 | `POST_NOT_FOUND` | 없거나 이미 삭제된 글 |
| 409 | `MEETUP_CLOSED` | 이미 시작된 번개 |
| 409 | `RECRUIT_CLOSED` | 커피챗을 신청한 모집 |

### POST-07 좋아요 · 좋아요 취소

`PUT /posts/{postId}/like` · `DELETE /posts/{postId}/like` · 승인 회원

둘 다 `200`에 `{ "postId": 101, "liked": true, "likeCount": 9 }`를 돌려줘. 여러 번 눌러도 결과가 같아.

| 상태 | code | 언제 |
| --- | --- | --- |
| 404 | `POST_NOT_FOUND` | 없거나 삭제된 글 |

### POST-08 인기 키워드

`GET /posts/popular-keywords?size=10` · 승인 회원

커뮤니티 오른쪽 '인기 키워드' 카드야. PRD에 없어서 프론트 화면 기준으로 정했어. 최근 7일 동안 커뮤니티 검색(POST-01)에 많이 쓰인 검색어를 검색한 사람이 많은 순서로 줘.

| 쿼리 | 타입 | 설명 |
| --- | --- | --- |
| `size` | number | 1\~20, 기본 10 |

```json
{ "items": [{ "keyword": "알고리즘", "count": 12 }, { "keyword": "인턴", "count": 8 }] }
```

- 쌓이는 검색: POST-01의 첫 페이지(`page=0`)에서 결과가 1개 이상인 `q`만 남겨. 앞뒤 공백을 지우고 가운데 공백은 하나로, 영어는 소문자로 바꿔서 2\~20자일 때만 남겨
- `count`는 최근 7일 안에 그 키워드를 검색한 회원 수야. 같은 회원이 여러 번 검색해도 1이고, 같으면 최근에 검색된 키워드가 먼저 와
- 금칙어(`keyword_blocklist`)가 들어간 키워드는 빼. 금칙어는 DB에 직접 넣어(3. 관리자 참고). 넣으면 바로 빠져
- 검색 기록은 7일이 지나면 매일 새벽 4시 30분에 지워
- 아직 검색이 없으면 `items`가 `[]`야. FE는 카드를 숨기거나 '아직 인기 키워드가 없어요'를 보여줘
- 키워드를 누르면 FE는 그 키워드를 `q`에 넣고 전체 카테고리로 POST-01을 불러. 글자를 칠 때마다 검색하면 중간 글자('알고')도 남으니, 검색은 Enter를 누르거나 입력이 멈춘 뒤(300ms 정도)에 불러줘

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `size` 범위 |

### COMMENT-01 댓글 목록

`GET /posts/{postId}/comments?page=0&size=20` · 승인 회원

오래된 순이야. 삭제된 댓글도 남고 `deleted: true`, 본문은 '삭제된 댓글이에요.', `author`는 `null`이야.

```json
{
  "items": [{
    "id": 801, "postId": 101, "content": "좋은 정보 감사합니다!", "isAnonymous": false,
    "author": { "id": 25, "name": "이사자", "generation": 14, "lionParts": ["BACKEND"], "photoUrl": null, "headline": null, "isDeleted": false },
    "deleted": false, "isMine": false, "createdAt": "2026-11-10T15:00:00+09:00"
  }],
  "page": 0, "size": 20, "hasNext": false
}
```

| 상태 | code | 언제 |
| --- | --- | --- |
| 404 | `POST_NOT_FOUND` | 없거나 삭제된 글 |

### COMMENT-02 댓글 작성

`POST /posts/{postId}/comments` · 승인 회원

`{ "content": "..." }` 1\~500자. 익명은 고르지 않고 서버가 정해. 익명 글의 작성자가 쓴 댓글만 익명이야. `201`에 COMMENT-01의 항목 하나를 돌려줘. 대댓글은 프론트 화면이 없어서 아직 받지 않아.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 비었거나 500자 초과 |
| 404 | `POST_NOT_FOUND` | 없거나 삭제된 글 |

### COMMENT-03 댓글 삭제

`DELETE /posts/{postId}/comments/{commentId}` · 작성자 또는 관리자

소프트 삭제이고 `204`야.

| 상태 | code | 언제 |
| --- | --- | --- |
| 403 | `NOT_OWNER` | 작성자도 관리자도 아님 |
| 404 | `POST_NOT_FOUND`, `COMMENT_NOT_FOUND` | 없거나 삭제된 글·댓글, 다른 글의 댓글 |

### MEETUP-01 번개 참여 · 참여 취소

`PUT /posts/{postId}/meetup/members/me` · `DELETE /posts/{postId}/meetup/members/me` · 승인 회원

둘 다 `200`에 지금 참여 상태를 돌려줘. 화면이 다시 조회하지 않고 버튼·인원을 바꿀 수 있어.

```json
{ "postId": 101, "joined": true, "participantCount": 4, "capacity": 6, "status": "OPEN", "members": [] }
```

- 이미 참여 중이면 참여는 그대로 성공해
- 마지막 한 자리에 여러 명이 동시에 눌러도 한 명만 들어가(번개 행 잠금 + `READ COMMITTED`)
- 작성자는 참여 취소 대신 POST-06으로 번개를 취소해
- 참여하면 번개 단체 채팅방에 들어가고, 참여를 취소하면 방에서 나가(10장 "번개 단체방"). FE의 '단체 채팅방에 초대될 거예요' 안내를 그대로 써도 돼

| 상태 | code | 언제 |
| --- | --- | --- |
| 403 | `HOST_CANNOT_LEAVE` | 작성자가 참여 취소 |
| 404 | `POST_NOT_FOUND` | 없거나 삭제된 글 |
| 404 | `MEETUP_NOT_FOUND` | 번개 글이 아님 |
| 404 | `NOT_MEETUP_MEMBER` | 참여하지 않았는데 참여 취소 |
| 409 | `MEETUP_CLOSED` | 시작되거나 취소된 번개 |
| 409 | `MEETUP_FULL` | 정원 마감 |

### MEETUP-02 번개 초대

`POST /posts/{postId}/meetup/invites` · 번개 작성자만

`{ "userIds": [25, 31] }` 1\~20명. 본인, 이미 참여했거나 초대한 회원, 승인 전·탈퇴·없는 회원은 건너뛰어. 초대 기록(`meetup_invites`)과 받는 사람의 알림(커뮤니티 탭, `MEETUP_INVITED`)을 저장해. 자동 참여는 아니야.

`200`에 `{ "sentCount": 1, "skippedUserIds": [31] }`를 돌려줘.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 비었거나 20명 초과 |
| 403 | `NOT_OWNER` | 번개 작성자가 아님 |
| 404 | `POST_NOT_FOUND`, `MEETUP_NOT_FOUND` | MEETUP-01과 같음 |
| 409 | `MEETUP_CLOSED` | 시작되거나 취소된 번개 |

FE는 '{N}명에게 초대 알림을 보냈어요'에 고른 인원 수가 아니라 `sentCount`를 써 줘.

### RECRUIT-01 커피챗 모집 참여 · 참여 취소

`PUT /posts/{postId}/recruit/members/me` · 승인 회원 (참여 취소 `DELETE`는 구현 예정, 아래 참고)

커피챗 모집 글(4.11) 상세의 [참여하기]야. 모인 인원으로 커피챗을 신청하는 [이 인원으로 커피챗 신청하기]는 커피챗 신청 API(4.1)가 `fromRecruitId`를 받아 처리하고, 신청하면 모집 상태가 `APPLIED`가 돼. 그 부분은 커피챗 담당 범위야.

`200`에 지금 참여 상태를 돌려줘. `members`는 작성자가 맨 앞이고 그다음 참여한 순서야.

```json
{ "postId": 102, "joined": true, "memberCount": 2, "capacity": 4, "status": "OPEN", "members": [] }
```

- 이미 참여 중이면 참여는 그대로 성공해(알림은 다시 안 보내)
- 마감일 당일까지 참여할 수 있어
- 신청받을 사자 본인은 참여할 수 없어
- 그 사자와 이미 진행 중인 커피챗(`PENDING` `ACCEPTED` `SCHEDULED`)이 있으면 참여할 수 없어. 내가 신청자, 받는 사람, 함께 신청자인 커피챗을 모두 봐
- 마지막 한 자리에 여러 명이 동시에 눌러도 한 명만 들어가(MEETUP-01과 같은 방식)
- 참여하면 모집 작성자에게 `RECRUIT_JOINED` 알림을 저장해
- 참여 취소(`DELETE`)는 프론트에 버튼이 없어서 만들지 팀에서 정한다. 만든다면 작성자와 신청 완료(`APPLIED`) 뒤에는 취소할 수 없어

| 상태 | code | 언제 |
| --- | --- | --- |
| 403 | `SENIOR_CANNOT_JOIN` | 신청받을 사자 본인이 참여 |
| 404 | `POST_NOT_FOUND` | 없거나 삭제된 글 |
| 404 | `RECRUIT_NOT_FOUND` | 커피챗 모집 글이 아님 |
| 409 | `RECRUIT_CLOSED` | 신청 완료·종료됐거나 마감일이 지남 |
| 409 | `RECRUIT_FULL` | 정원 마감 |
| 409 | `OPEN_COFFEE_CHAT_EXISTS` | 그 사자와 진행 중인 커피챗이 있음 |

### FILE-01 업로드 주소 발급

`POST /files` · 승인 회원

이미지·첨부파일은 서버를 거치지 않고 S3에 바로 올려. 순서는 이래.

1. 이 API로 업로드 주소를 받아
2. `uploadUrl`에 파일을 `PUT`으로 올려. 헤더 `Content-Type`은 응답의 `contentType` 그대로 보내고, 파일 크기는 요청한 `size`와 같아야 해(다르면 S3가 거절해)
3. 글을 쓰거나 고칠 때 `fileUrl`을 `files[].url`로 보내

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `type` | string | O | `IMAGE` 이미지, `ATTACHMENT` 첨부파일 |
| `fileName` | string | O | 원래 파일 이름. 확장자로 형식을 확인해 |
| `size` | number | O | 바이트. 이미지 10MB, 첨부파일 20MB 이하 |

| 종류 | 확장자 |
| --- | --- |
| `IMAGE` | jpg, jpeg, png, webp |
| `ATTACHMENT` | pdf, hwp, docx, pptx, xlsx, zip |

```json
{
  "uploadUrl": "https://{버킷}.s3.ap-northeast-2.amazonaws.com/uploads/12/3f2a....png?X-Amz-Algorithm=...",
  "fileUrl": "https://{버킷}.s3.ap-northeast-2.amazonaws.com/uploads/12/3f2a....png",
  "contentType": "image/png",
  "expiresAt": "2026-11-13T17:40:00+09:00"
}
```

`uploadUrl`은 10분 동안만 쓸 수 있어. 올린 파일은 공개 읽기라서 `fileUrl`을 그대로 `<img src>`에 쓰면 돼.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 필수값 누락, 0바이트 |
| 400 | `INVALID_FILE_TYPE` | 종류에 맞지 않는 확장자('지원하지 않는 파일 형식이에요.') |
| 400 | `FILE_TOO_LARGE` | 크기 초과('업로드할 수 있는 파일 크기를 넘었어요.') |

### 커뮤니티가 저장하는 알림

알림 담당 테이블(`notifications`)에 커뮤니티 탭·`POST` 대상으로 저장해. 알림 조회 API는 알림 담당 범위야.

| `type` | 받는 사람 | 문구 |
| --- | --- | --- |
| `MEETUP_INVITED` | 초대받은 회원 | `님이 번개에 초대했어요. {제목}` (보낸 사람: 작성자) |
| `MEETUP_CANCELED` | 작성자를 뺀 참여자 | `참여한 번개가 취소됐어요. {제목}` |
| `COMMENT_CREATED` | 글 작성자 (본인 댓글 제외) | `님이 내 글에 댓글을 남겼어요. {제목}` (보낸 사람: 댓글 작성자) |
| `RECRUIT_JOINED` | 커피챗 모집 작성자 | `님이 내 모집글에 참여했어요. {제목}` (보낸 사람: 참여자) |
| `RECRUIT_CANCELED` | 작성자를 뺀 모집 참여자 | `참여한 커피챗 모집이 취소됐어요. {제목}` |

보낸 사람이 있는 알림은 `actor_id`에 그 회원을 남기고 문구에는 이름 뒤 부분만 저장해. 화면은 이름을 굵게 쓰고 문구를 이어 붙인다(9. 알림의 "보낸 사람과 문구"). 익명 댓글이면 보낸 사람을 남기지 않고 문구 앞에 '익명'을 붙여(지금 규칙에선 익명 댓글은 익명 글 작성자만 쓰므로 알림이 가지 않아).

### 커뮤니티 확정 필요

| 번호 | 무엇을 | 지금 구현 | 맞출 사람 |
| --- | --- | --- | --- |
| 1 | 익명 허용 범위 | 글은 `FREE`만, 댓글은 익명 글 작성자만 자동 익명. PRD(6.3·6.6)는 번개·커피챗 모집 빼고 모두 익명 가능 | 기획, FE |
| 2 | 커피챗 함께 신청 모집(4.11) | RECRUIT-01 참여 구현. 모인 인원으로 신청(`APPLIED`로 변경, `coffee_chat_id` 기록)은 커피챗 신청이 맡는다. 참여 취소를 만들지, 신청 후 커피챗이 거절·만료돼도 '신청 완료'로 남길지 정해야 한다 | 커피챗 담당, 기획 |
| 3 | 인기 키워드(6-2) | **반영**: POST-08. PRD에 없어서 최근 7일 검색어 기준으로 정했다 | 기획, FE |
| 4 | 업로드 주소 발급(6-10) | **반영**: FILE-01. 글을 지우거나 파일을 바꿀 때 S3의 이전 파일은 아직 지우지 않는다 | BE |
| 5 | 탈퇴(AUTH-08)·여러 명 커피챗 끄기(PROFILE-13) 때 모집글 처리 | 미구현. 회원·프로필 쪽에서 이벤트를 보내면 커뮤니티가 모집글 종료·참여자 정리·알림을 맡는다 | 계정·프로필 담당 |

## 7. 홈

담당: 조상원(동문·커뮤니티·홈). 커뮤니티 새 소식과 모집 중인 글은 POST-01로 받고, 커피챗 받기 카드는 PROFILE-12·14, 프로필 완성도는 FE가 계산해.

| ID | 기능 | 메서드·경로 | 노션 |
| --- | --- | --- | --- |
| HOME-01 | 홈 화면 | `GET /home` | 8-1 |
| HOME-02 | 환영 배너 닫기 | `POST /home/coffee-guide/dismiss` | 8-2 |

### HOME-01 홈 화면

`GET /home` · 승인 회원 + 기본 프로필 입력 완료

```json
{
  "me": { "id": 12, "name": "김사자", "generation": 14, "lionParts": ["BACKEND"], "headline": "백엔드 지망생", "bio": "...", "photoUrl": null },
  "welcomeBannerVisible": true,
  "counts": { "pendingCoffeeChats": 1, "activeCoffeeChats": 2, "unreadMessages": 3, "messageRequests": 1, "bookmarks": 5 },
  "upcomingCoffeeChat": {
    "id": 41, "scheduledAt": "2026-11-20T19:00:00+09:00", "meetingMode": "ONLINE", "roomId": 7,
    "partner": { "id": 25, "name": "박사자", "generation": 11, "lionParts": ["BACKEND"], "photoUrl": null, "isDeleted": false }
  },
  "recommendedSeniors": [],
  "onThisDay": { "id": 501, "category": "NOW", "title": "13기 중커톤 시상식 후기", "thumbnailUrl": null, "createdAt": "2025-11-12T15:00:00+09:00" }
}
```

| 필드 | 기준 |
| --- | --- |
| `welcomeBannerVisible` | `users.guide_dismissed_at`이 없으면 `true` |
| `counts.pendingCoffeeChats` | 내가 받은 `PENDING` 커피챗 |
| `counts.activeCoffeeChats` | 내가 신청자·받는 사자·함께 신청자인 `ACCEPTED`·`SCHEDULED` 커피챗 |
| `counts.unreadMessages` | 나가지 않은 `ACTIVE` 방에서 마지막으로 읽은 메시지 이후 다른 사람이 보낸 메시지 |
| `counts.messageRequests` | 내가 받은 `REQUESTED` 메시지 요청 |
| `counts.bookmarks` | 관심 동문 목록(ALUMNI-03)에 나오는 사자 수. 탈퇴한 사자는 빼 |
| `upcomingCoffeeChat` | 지금 이후 가장 가까운 `SCHEDULED` 커피챗 1건. 없으면 `null` |
| `recommendedSeniors` | 나보다 앞 기수이면서 오늘 커피챗을 받는(`AVAILABLE`) 사자 2명. 같은 파트 먼저, 그다음 가까운 기수 순. ALUMNI-01 항목 형식 |
| `onThisDay` | 1년 전 같은 날 쓴 글 중 조회수가 가장 많은 글. 없으면 `null` |

커피챗·채팅 개수는 커피챗·채팅 담당 테이블을 읽기만 해. '커피챗을 받는 중'은 PROFILE-12 규칙(받기 설정 + 쉬는 기간·받는 기간)으로 오늘 기준으로 판단해.

| 상태 | code | 언제 |
| --- | --- | --- |
| 403 | `NOT_APPROVED` | 승인 전 |
| 403 | `PROFILE_NOT_COMPLETED` | 기본 프로필(2.1)을 입력하지 않음 |

### HOME-02 환영 배너 닫기

`POST /home/coffee-guide/dismiss` · 승인 회원

승인 후 첫 방문 환영 배너의 [닫기]야. `users.guide_dismissed_at`을 기록하고 `200`에 `{ "visible": false, "dismissedAt": "..." }`를 돌려줘. 여러 번 불러도 처음 닫은 시각을 유지해.

## 8. 커피챗

담당: 차예은(커피챗·채팅·알림). 지금은 조상원이 함께 맡음. 노션 명세가 비어 있어서 PRD 4장·상태 흐름(0.2)과 프론트 화면(M-01·M-02, 신청·수락·거절·일정·취소 팝업)을 기준으로 정했어.

| ID | 기능 | 메서드·경로 | PRD |
| --- | --- | --- | --- |
| COFFEE-01 | 커피챗 신청 | `POST /coffee-chats` | 4.1, 4.11 |
| COFFEE-02 | 내 커피챗 목록 | `GET /coffee-chats` | 4.2 |
| COFFEE-03 | 커피챗 상세 | `GET /coffee-chats/{coffeeChatId}` | 4.3 |
| COFFEE-04 | 수락 | `POST /coffee-chats/{coffeeChatId}/accept` | 4.4 |
| COFFEE-05 | 거절 | `POST /coffee-chats/{coffeeChatId}/reject` | 4.5 |
| COFFEE-06 | 신청 취소 (대기 중) | `POST /coffee-chats/{coffeeChatId}/withdraw` | 4.6 |
| COFFEE-07 | 일정 확정·변경 | `PUT /coffee-chats/{coffeeChatId}/schedule` | 4.8 |
| COFFEE-08 | 커피챗 취소 (수락 이후) | `POST /coffee-chats/{coffeeChatId}/cancel` | 4.9 |
| COFFEE-09 | 만남 완료 | `POST /coffee-chats/{coffeeChatId}/complete` | 4.10 |
| COFFEE-10 | 함께 신청자 빠지기 | `DELETE /coffee-chats/{coffeeChatId}/members/me` | 4.9 |

모두 승인 회원만 쓸 수 있어.

### 역할과 상태

| 역할 (`myRole`) | 누구 |
| --- | --- |
| `RECEIVER` | 받는 사자 |
| `REQUESTER` | 대표 신청자 (신청한 사람) |
| `MEMBER` | 함께 신청자 (여러 명 커피챗) |

상태는 PRD 흐름 그대로야. 진행 중은 `PENDING`(대기 중)·`ACCEPTED`(수락됨)·`SCHEDULED`(일정 확정), 종료는 `COMPLETED`·`REJECTED`·`CANCELED`·`EXPIRED`. 아래 표 밖의 변경은 `409 INVALID_STATUS`야.

| 지금 상태 | 할 수 있는 동작 (`actions`) |
| --- | --- |
| `PENDING` | 받는 사자 `ACCEPT` `REJECT`, 대표 신청자 `WITHDRAW`, 함께 신청자 `LEAVE` |
| `ACCEPTED` | 받는 사자·대표 신청자 `SCHEDULE` `CANCEL`, 함께 신청자 `LEAVE` |
| `SCHEDULED` | 받는 사자·대표 신청자 `SCHEDULE`(변경) `CANCEL`, 만남 시각이 지나면 `COMPLETE`. 함께 신청자 `LEAVE` |
| 종료 | 없음 |

자동으로 바뀌는 경우도 있어(1시간마다 확인).

- `PENDING`이 14일 동안 답이 없으면 `EXPIRED`
- `SCHEDULED`는 만남 시각 7일 뒤 `COMPLETED`
- `ACCEPTED`는 수락 30일 동안 일정이 없으면 `COMPLETED`

### 사용자 요약 객체

목록·상세의 사람 정보는 `{ id, name, generation, lionParts, photoUrl, isDeleted }`야. 탈퇴한 사람은 `isDeleted: true`이고 나머지는 `null`이야.

### COFFEE-01 커피챗 신청

`POST /coffee-chats`

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `receiverId` | number | 모집글이 아니면 O | 받는 사자 |
| `topic` | string | O | 받는 사자의 커피챗 주제(PROFILE-12 `topics`) 중 하나, 또는 `ETC` |
| `topicCustom` | string | `ETC`면 O | 1\~30자 |
| `reason` | string | O | 이 사자에게 신청하는 이유 10\~500자 |
| `questions` | string\[\] | X | 0\~3개, 각 5\~200자 |
| `preferredMode` | string | O | `ONLINE` `OFFLINE` `ANY`(상관없음) |
| `scheduleNote` | string | X | 희망 일정 메모 0\~100자 |
| `memberIds` | number\[\] | X | 함께 신청할 동문 0\~4명. 승인 회원, 본인·받는 사자·중복 불가 |
| `fromRecruitId` | number | X | 커뮤니티 커피챗 모집글(4.11)에서 신청할 때 모집글 ID |

- 받는 사자가 오늘 커피챗을 받는 중(`AVAILABLE`)이어야 해. 함께 신청자가 있으면 여러 명 신청도 받아야 해
- 나와 함께 신청자 중 누구라도 그 사자와 진행 중인 커피챗이 있으면 막아
- 거절되거나 만료된 지 7일이 안 됐으면 막아(`COOLDOWN`). 대표 신청자 기준이야
- `fromRecruitId`가 있으면 모집글 작성자만 신청할 수 있고, 받는 사자는 모집글의 사자, 함께 신청자는 모집 참여자(작성자 제외)로 정해져(`receiverId`·`memberIds`는 무시). 참여자가 작성자 외 1명 이상이어야 하고, 신청하면 모집은 `APPLIED`가 돼
- 신청 후 14일이 지나면 만료돼(`expiresAt`)
- 받는 사자에게 `COFFEE_CHAT_REQUESTED` 알림을 저장해

`201`에 `{ "id": 31, "status": "PENDING", "createdAt": "...", "expiresAt": "..." }`를 돌려줘.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 글자 수, 질문 개수, 함께 신청자 5명 이상 |
| 400 | `CANNOT_REQUEST_SELF` | 나에게 신청 |
| 400 | `INVALID_TOPIC` | 사자가 받지 않는 주제, `ETC`인데 `topicCustom` 없음 |
| 400 | `INVALID_MEMBERS` | 함께 신청자에 본인·받는 사자·중복·없는 회원, 모집 참여자가 작성자뿐 |
| 403 | `NOT_OWNER` | 모집글 작성자가 아님 |
| 404 | `USER_NOT_FOUND` | 받는 사자가 없거나 승인 전·탈퇴 |
| 404 | `RECRUIT_NOT_FOUND` | 없는 모집글 |
| 409 | `NOT_ACCEPTING` | 받는 사자가 지금 커피챗을 받지 않음 |
| 409 | `GROUP_COFFEE_CHAT_NOT_ALLOWED` | 함께 신청자가 있는데 여러 명 신청을 안 받음 |
| 409 | `OPEN_COFFEE_CHAT_EXISTS` | 그 사자와 진행 중인 커피챗이 있음 |
| 409 | `COOLDOWN` | 거절·만료 후 7일 안. 문구에 다시 신청할 수 있는 날짜가 들어가('11월 20일부터 다시 신청할 수 있어요.') |
| 409 | `RECRUIT_CLOSED` | 이미 신청했거나 끝난 모집 |

### COFFEE-02 내 커피챗 목록

`GET /coffee-chats?box=RECEIVED&filter=ALL&page=0&size=20`

| 쿼리 | 설명 |
| --- | --- |
| `box` | `RECEIVED` 받은 신청(내가 받는 사자), `SENT` 보낸 신청(내가 대표 신청자이거나 함께 신청자). 기본 `RECEIVED` |
| `filter` | `ALL`, `PENDING` 대기 중, `ACTIVE` 만남 준비 중(`ACCEPTED`·`SCHEDULED`), `ENDED` 종료. 기본 `ALL` |

최근 신청순이야.

```json
{
  "items": [{
    "id": 31, "status": "PENDING", "myRole": "RECEIVER",
    "counterpart": { "id": 12, "name": "김사자", "generation": 14, "lionParts": ["BACKEND"], "photoUrl": null, "isDeleted": false },
    "members": [],
    "topic": "CAREER", "topicCustom": null, "reason": "...",
    "createdAt": "...", "expiresAt": "...", "scheduledAt": null
  }],
  "page": 0, "size": 20, "hasNext": false,
  "pendingReceivedCount": 1, "activeCount": 2
}
```

- `counterpart`는 받은 신청이면 대표 신청자, 보낸 신청이면 받는 사자
- `members`는 함께 신청자(빠진 사람 제외)
- `pendingReceivedCount`·`activeCount`는 상단 '받은 대기 N · 만남 준비 중 N'이야

### COFFEE-03 커피챗 상세

`GET /coffee-chats/{coffeeChatId}`

받는 사자·대표 신청자·함께 신청자(빠지지 않은)만 볼 수 있고, 그 밖에는 `404`야.

```json
{
  "id": 31, "status": "ACCEPTED", "myRole": "REQUESTER",
  "receiver": { "id": 7, "name": "선배사자", "...": "사용자 요약" },
  "requester": { "id": 12, "name": "김사자", "...": "사용자 요약" },
  "members": [],
  "topic": "CAREER", "topicCustom": null, "reason": "...", "questions": ["..."],
  "preferredMode": "ANY", "scheduleNote": "평일 저녁 좋아요",
  "proposedSlots": ["2026-11-20T19:00:00+09:00"], "responseMessage": "반가워요!",
  "rejectReason": null,
  "meetingMode": null, "scheduledAt": null, "meetingPlace": null, "meetingLink": null,
  "cancelReason": null, "canceledBy": null,
  "roomId": 55, "recruitPostId": null,
  "createdAt": "...", "expiresAt": "...", "acceptedAt": "...", "rejectedAt": null, "canceledAt": null, "completedAt": null,
  "actions": ["SCHEDULE", "CANCEL"]
}
```

- `responseMessage`는 수락하거나 거절할 때 받는 사자가 남긴 말이야
- `roomId`는 수락 후 열린 채팅방, `recruitPostId`는 모집글에서 신청했을 때 그 글이야
- `actions`는 위 '역할과 상태' 표대로 지금 내가 할 수 있는 동작이야. FE는 이걸로 버튼을 보여줘

### COFFEE-04 수락

`POST /coffee-chats/{coffeeChatId}/accept` · 받는 사자, `PENDING`

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `proposedSlots` | datetime\[\] | O | 가능한 시간 후보 1\~3개. 지금 이후 |
| `responseMessage` | string | X | 한마디 0\~200자 |

- 수락하면 채팅방이 열려. 1:1이면 두 사람의 1:1 방(이미 있으면 그 방을 다시 열어), 여러 명이면 받는 사자와 신청자 전원의 단체방이야
- 대표 신청자와 함께 신청자에게 `COFFEE_CHAT_ACCEPTED` 알림을 저장해
- `200`에 COFFEE-03 형식을 돌려줘

### COFFEE-05 거절

`POST /coffee-chats/{coffeeChatId}/reject` · 받는 사자, `PENDING`

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `rejectReason` | string | O | `SCHEDULE` 일정이 맞지 않아요, `HARD_TOPIC` 답하기 어려운 주제예요, `NO_TIME` 당분간 여유가 없어요, `ETC` 기타 |
| `responseMessage` | string | X | 0\~200자 |

신청자들에게 `COFFEE_CHAT_REJECTED` 알림을 저장하고 `200`에 COFFEE-03 형식을 돌려줘. 거절 팝업의 '당분간 커피챗 쉬기'는 FE가 PROFILE-15를 따로 불러.

### COFFEE-06 신청 취소

`POST /coffee-chats/{coffeeChatId}/withdraw` · 대표 신청자, `PENDING`

`CANCELED`로 바꾸고 `200`에 COFFEE-03 형식을 돌려줘.

### COFFEE-07 일정 확정·변경

`PUT /coffee-chats/{coffeeChatId}/schedule` · 받는 사자·대표 신청자, `ACCEPTED`·`SCHEDULED`

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `scheduledAt` | datetime | O | 지금 이후 |
| `meetingMode` | string | O | `ONLINE` `OFFLINE` |
| `meetingPlace` | string | `OFFLINE`이면 O | 1\~50자 |
| `meetingLink` | string | X | `ONLINE`일 때 화상 링크, http(s) URL |

`SCHEDULED`로 바꾸고 채팅방에 '일정이 확정됐어요' 안내를 남겨. 나 말고 다른 참여자에게 `COFFEE_CHAT_SCHEDULED` 알림을 저장하고 `200`에 COFFEE-03 형식을 돌려줘.

### COFFEE-08 커피챗 취소

`POST /coffee-chats/{coffeeChatId}/cancel` · 받는 사자·대표 신청자, `ACCEPTED`·`SCHEDULED`

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `cancelReason` | string | O | 5\~200자. 상대에게 보여 |

`CANCELED`로 바꾸고 채팅방에 안내를 남겨. 다른 참여자에게 `COFFEE_CHAT_CANCELED` 알림을 저장하고 `200`에 COFFEE-03 형식을 돌려줘. 채팅방은 그대로 남아.

### COFFEE-09 만남 완료

`POST /coffee-chats/{coffeeChatId}/complete` · 받는 사자·대표 신청자, `SCHEDULED`이고 만남 시각이 지난 뒤

`COMPLETED`로 바꾸고 `200`에 COFFEE-03 형식을 돌려줘. 완료된 사이도 채팅은 계속할 수 있어.

### COFFEE-10 함께 신청자 빠지기

`DELETE /coffee-chats/{coffeeChatId}/members/me` · 함께 신청자, 진행 중일 때

빠지면 단체 채팅방에서도 나가고 `204`야. 커피챗은 남은 사람끼리 그대로 진행돼.

### COFFEE-04\~10 공통 에러

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 필수값 누락, 글자 수, 지난 시각, 후보 개수 |
| 403 | `FORBIDDEN_ACTION` | 내 역할로는 할 수 없는 동작 |
| 404 | `COFFEE_CHAT_NOT_FOUND` | 없거나 내가 참여하지 않은 커피챗 |
| 409 | `INVALID_STATUS` | 지금 상태에서는 할 수 없음, 대기 시간이 지나 만료된 신청 |
| 409 | `MEETING_NOT_PASSED` | 만남 시각 전에 완료 |

### 커피챗이 저장하는 알림

`notifications`에 커피챗 탭(`COFFEE_CHAT`)·`COFFEE_CHAT` 대상으로 저장해. 메일(9.2)은 알림 이슈에서 만들어.

`message`는 이름 뒤 문구야. 화면은 `actor.name` + `message`로 보여줘.

| `type` | 받는 사람 | 보낸 사람(`actor`) | `message` |
| --- | --- | --- | --- |
| `COFFEE_CHAT_REQUESTED` | 받는 사자 | 신청자 | `님이 커피챗을 신청했어요.` (여러 명이면 `님 외 {N}명이 커피챗을 신청했어요.`) |
| `COFFEE_CHAT_ACCEPTED` | 신청자 전원 | 받는 사자 | `님이 커피챗을 수락했어요. 채팅방에서 일정을 정해보세요.` |
| `COFFEE_CHAT_REJECTED` | 신청자 전원 | 받는 사자 | `님이 이번 커피챗은 어렵다고 했어요.` |
| `COFFEE_CHAT_SCHEDULED` | 나를 뺀 참여자 | 일정을 정한 회원 | `님이 커피챗 일정을 정했어요. {M월 d일 HH:mm}` (변경이면 `바꿨어요`) |
| `COFFEE_CHAT_CANCELED` | 나를 뺀 참여자 | 취소한 회원 | `님이 커피챗을 취소했어요.` |
| `COFFEE_CHAT_EXPIRED` | 신청자 전원 | 받는 사자 | `님에게 보낸 커피챗 신청이 만료됐어요.` |

## 9. 알림

담당: 커피챗, 채팅, 알림. PRD 9.1 인앱 알림과 9.2 이메일 알림이야. 노션 상세가 비어 있어서 프론트 화면(H-07 알림 드롭다운, N-01 알림 설정, N-04 수신 거부 완료)을 기준으로 정했어.

| ID | 기능 | 메서드, 경로 | 쓸 수 있는 회원 |
| --- | --- | --- | --- |
| NOTI-01 | 최근 알림 20개 | `GET /notifications` | 승인 회원 |
| NOTI-02 | 알림 하나 읽음 처리 | `PUT /notifications/{notificationId}/read` | 승인 회원 |
| NOTI-03 | 알림 모두 읽음 처리 | `PUT /notifications/read-all` | 승인 회원 |
| NOTI-04 | 이메일 알림 설정 조회 | `GET /me/notification-settings` | 승인 대기, 승인 회원 |
| NOTI-05 | 이메일 알림 켜기, 끄기 | `PATCH /me/notification-settings` | 승인 대기, 승인 회원 |
| NOTI-06 | 메일 링크로 수신 거부 | `GET /email-notifications/unsubscribe` | 로그인 없이 |

### 열거값

| 이름 | 값 |
| --- | --- |
| `tab` | `COFFEE_CHAT` 커피챗, `CHAT` 채팅, `COMMUNITY` 커뮤니티, `ACCOUNT` 계정(가입 신청, 가입 승인). 드롭다운에 계정 탭이 없어서 `ACCOUNT`는 전체 탭에서만 보여 |
| `targetType` | `COFFEE_CHAT`, `ROOM`, `POST`, `USER`. FE는 `targetType`과 `targetId`로 이동할 화면을 정해 |
| `type` | 알림 종류 문자열. 지금 저장되는 값은 아래 "지금 저장되는 알림" 표 |
| 메일 종류 | `COFFEE_CHAT_REQUEST` 커피챗 신청, `MESSAGE_REQUEST` 메시지 요청, `SIGNUP_APPROVED` 가입 승인 |

### 보낸 사람과 문구

회원이 일으킨 알림은 `notifications.actor_id`에 그 회원을 남기고, `message`에는 이름 뒤에 붙는 문구만 저장해. FE는 `actor.name`을 굵게 쓰고 바로 뒤에 `message`를 이어 붙이면 돼(예: **김사자** + `님이 내 글에 댓글을 남겼어요. 자유 글`). 이름은 조회할 때 `users`에서 읽어서, 이름이 바뀌어도 지금 이름으로 보여.

- `actor`가 `null`이면 시스템 알림이고 `message`가 문장 전체야(예: `가입이 승인됐어요. 이제 모든 기능을 이용할 수 있어요.`).
- 보낸 사람이 탈퇴했으면 `actor.isDeleted`가 `true`이고 `name`, `photoUrl`은 `null`이야. FE는 '탈퇴한 회원'처럼 따로 표시해.

### NOTI-01 최근 알림 20개

`GET /notifications?tab=ALL` (승인 회원)

`tab`은 `ALL`(기본), `COFFEE_CHAT`, `CHAT`, `COMMUNITY` 중 하나야. 고른 탭의 최근 알림을 최신순으로 20개까지 주고, 탭별 안 읽은 개수는 고른 탭과 상관없이 같이 줘.

```json
{
  "items": [
    {
      "id": 31,
      "tab": "COMMUNITY",
      "type": "COMMENT_CREATED",
      "targetType": "POST",
      "targetId": 501,
      "actor": { "id": 25, "name": "김사자", "photoUrl": null, "isDeleted": false },
      "message": "님이 내 글에 댓글을 남겼어요. 자유 글",
      "read": false,
      "createdAt": "2026-11-13T12:00:00+09:00"
    },
    {
      "id": 30,
      "tab": "ACCOUNT",
      "type": "SIGNUP_APPROVED",
      "targetType": "USER",
      "targetId": 12,
      "actor": null,
      "message": "가입이 승인됐어요. 이제 모든 기능을 이용할 수 있어요.",
      "read": true,
      "createdAt": "2026-11-12T09:00:00+09:00"
    }
  ],
  "unreadCounts": { "all": 3, "coffeeChat": 1, "chat": 0, "community": 2 }
}
```

| 필드 | 기준 |
| --- | --- |
| `read` | `notifications.read_at`이 있으면 `true` |
| `unreadCounts.all` | 안 읽은 내 알림 전부. 계정 알림도 들어가서 상단 종 배지에 쓰면 돼 |
| `unreadCounts.coffeeChat`, `chat`, `community` | 탭별 안 읽은 알림 수 |

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `tab`이 위 네 값이 아님 |
| 403 | `NOT_APPROVED` | 승인 전 |

### NOTI-02 알림 하나 읽음 처리

`PUT /notifications/{notificationId}/read` (승인 회원)

알림을 눌러 이동할 때 불러. `read_at`을 기록하고 `204`를 줘. 이미 읽은 알림이면 처음 읽은 시각을 그대로 두고 `204`를 줘.

| 상태 | code | 언제 |
| --- | --- | --- |
| 404 | `NOTIFICATION_NOT_FOUND` | 없는 알림이거나 내 알림이 아님('알림을 찾을 수 없어요.') |

### NOTI-03 알림 모두 읽음 처리

`PUT /notifications/read-all` (승인 회원)

드롭다운의 [모두 읽음]이야. 탭과 상관없이 안 읽은 내 알림을 모두 읽음으로 바꾸고 `204`를 줘.

### NOTI-04 이메일 알림 설정 조회

`GET /me/notification-settings` (승인 대기, 승인 회원)

```json
{ "emailCoffeeChat": true, "emailMessageRequest": true, "emailSignupApproved": true }
```

설정이 아직 없으면 모두 켜진 기본값으로 만들어서 돌려줘. 수신 거부 토큰도 이때 같이 만들어지고, 응답에는 넣지 않아.

| 필드 | 화면(N-01) |
| --- | --- |
| `emailCoffeeChat` | 커피챗 신청 메일 받기 |
| `emailMessageRequest` | 메시지 요청 메일 받기 |
| `emailSignupApproved` | 가입 승인 메일 받기. 승인 대기 중에 끄면 승인 메일을 보내지 않아 |

| 상태 | code | 언제 |
| --- | --- | --- |
| 403 | `NOT_APPROVED` | 반려된 회원 |

### NOTI-05 이메일 알림 켜기, 끄기

`PATCH /me/notification-settings` (승인 대기, 승인 회원)

```json
{ "emailCoffeeChat": false }
```

보낸 항목만 바꾸고, 빠진 항목은 그대로 둬. 응답은 NOTI-04와 같은 형식의 바뀐 설정 전체야.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 값이 `true`, `false`가 아님 |
| 403 | `NOT_APPROVED` | 반려된 회원 |

### NOTI-06 메일 링크로 수신 거부

`GET /email-notifications/unsubscribe?token={unsubscribeToken}&type=COFFEE_CHAT_REQUEST` (로그인 없이)

메일 아래의 [그만 받기] 링크는 FE 수신 거부 페이지 `{FE}/email-notifications/unsubscribe?token=...&type=...`로 가고, FE가 주소의 `token`, `type`으로 이 API를 부른 뒤 완료 화면(N-04)을 보여줘. `type`에 해당하는 메일만 끄고 `204`를 줘. 이미 꺼져 있어도 `204`야. 다시 켜는 건 NOTI-05로 해.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `INVALID_UNSUBSCRIBE_TOKEN` | 토큰에 맞는 설정이 없음('수신 거부 링크가 올바르지 않아요.') |
| 400 | `VALIDATION_ERROR` | `token`이나 `type`이 없거나 `type`이 메일 종류가 아님 |

### 지금 저장되는 알림

| `type` | `tab` | 저장하는 곳 | 보낸 사람 |
| --- | --- | --- | --- |
| `SIGNUP_REQUESTED` | `ACCOUNT` | 가입 신청(AUTH-02), 다시 신청(AUTH-07) | 없음 |
| `SIGNUP_APPROVED` | `ACCOUNT` | 가입 승인(ADMIN-03, 05) | 없음 |
| `MEETUP_INVITED`, `RECRUIT_JOINED`, `COMMENT_CREATED` | `COMMUNITY` | 커뮤니티 | 초대한 회원, 참여한 회원, 댓글 쓴 회원 |
| `MEETUP_CANCELED`, `RECRUIT_CANCELED` | `COMMUNITY` | 커뮤니티 | 없음 |
| `COFFEE_CHAT_REQUESTED`, `ACCEPTED`, `REJECTED`, `SCHEDULED`, `CANCELED`, `EXPIRED` | `COFFEE_CHAT` | 커피챗(8장 "커피챗이 저장하는 알림") | 신청자, 받는 사자, 일정을 정한 회원, 취소한 회원 |
| `MESSAGE_REQUESTED`, `MESSAGE_REQUEST_ACCEPTED` | `CHAT` | 채팅(10장 "채팅이 저장하는 알림") | 요청한 회원, 수락한 회원 |

회원이 일으킨 알림은 `actorId`를 채우고 `message`는 이름 뒤 문구로 저장해.

### 이메일 발송

PRD 9.2의 메일은 커피챗 신청, 메시지 요청, 가입 승인 세 가지뿐이야. API가 따로 없고 서버가 이벤트를 받아서 보내.

| 메일 종류 | 언제 | 받는 사람 | 제목 | 버튼 |
| --- | --- | --- | --- | --- |
| `COFFEE_CHAT_REQUEST` | 커피챗 신청(COFFEE-01) | 받는 사자 | `[LION:LINK] {이름}({기수}기)님이 커피챗을 신청했어요` (여러 명이면 `님 외 N명이`) | `{FE}/coffee/{coffeeChatId}` |
| `MESSAGE_REQUEST` | 메시지 요청(CHAT-01) | 요청받은 회원 | `[LION:LINK] 새 메시지 요청이 왔어요` | `{FE}/chat/requests` |
| `SIGNUP_APPROVED` | 가입 승인(ADMIN-03, 05) | 승인된 회원 | `[LION:LINK] 가입이 승인됐어요` | `{FE}/home` |

- **쌓기**: 요청과 같은 트랜잭션에서 `email_logs`에 `PENDING`으로 저장해. 요청이 롤백되면 메일도 남지 않고, 메일 서버가 느려도 API 응답은 기다리지 않아.
- **보내기**: 스케줄러가 10초마다 보낼 시각(`scheduled_at`)이 된 메일을 꺼내 보내고 `SENT`로 바꿔.
- **건너뛰기**: 보내는 시점에 받는 사람이 탈퇴했거나, 그 종류를 꺼 뒀거나(NOTI-05, NOTI-06), 커피챗 신청이나 메시지 요청이 이미 대기 중이 아니면 보내지 않고 `SKIPPED`로 남겨.
- **다시 보내기**: 실패하면 10분 뒤에 다시 보내고, 3번 실패하면 `FAILED`로 멈춰(`attempt_count`).
- **본문**: 보내는 시점의 데이터로 만들고, 아래에 해당 종류의 수신 거부 링크를 넣어.
- **설정**: SMTP 접속 정보는 `spring.mail.*`(환경변수 `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`), 보내는 사람은 `MAIL_FROM`이야. `MAIL_ENABLED`가 `false`(기본값)면 실제로 보내지 않고 로그만 남겨.

### 알림 남은 일

| 번호 | 무엇을 | 지금 | 맞출 사람 |
| --- | --- | --- | --- |
| 1 | 실제 메일 발송(발송, 재시도, `email_logs` 기록) | **반영(2026-11-13)**: 커피챗 신청, 메시지 요청, 가입 승인 메일 발송. 위 "이메일 발송" 참고 | 완료 |
| 2 | FE 탭 값 | 목업은 `COFFEE`를 쓰는데 API는 `COFFEE_CHAT`이야. FE가 API 값에 맞춘다 | FE |
| 3 | 알림을 누를 때 이동할 주소 | API는 `targetType`, `targetId`만 준다. 주소는 FE가 만든다 | FE |
| 4 | 커피챗 알림의 보낸 사람 | 해결. 커피챗 알림도 `actorId`를 남기고 `message`는 이름 뒤 문구로 저장한다(8장 표) | - |
| 5 | 메시지 요청 메일 | 해결. 메시지 요청(CHAT-01)을 만들면 받는 사람에게 메일을 쌓는다 | - |
| 6 | 발송 수단과 보내는 주소 | Gmail SMTP와 AWS SES(SMTP) 어느 쪽이든 설정만 바꾸면 된다. 운영에서 쓸 계정과 주소를 정한다 | BE |
| 7 | 묶어 보내기 | 지금은 한 건에 한 통을 바로 보낸다. 짧은 시간에 신청이 여러 건 오면 한 통으로 묶을지 정한다(`target_ids`, `scheduled_at`으로 확장 가능) | 기획, BE |
| 8 | FE 수신 거부 페이지 | `{FE}/email-notifications/unsubscribe?token=...&type=...` 페이지에서 NOTI-06을 부르고 N-04 완료 화면을 보여준다 | FE |
| 9 | 반송(`BOUNCED`) 처리 | 미구현. 메일 서비스의 반송 알림 연동이 필요하다 | BE |

## 10. 채팅

담당: 커피챗, 채팅, 알림. PRD 5장(5.1~5.8)이야. 노션 상세가 비어 있어서 프론트 화면(T-17 1:1 채팅방, T-27 단체 채팅방, 메시지 요청함, 메시지 요청 팝업)을 기준으로 정했어. 보내기와 조회는 REST이고, 새 메시지와 읽음은 WebSocket(STOMP)으로 바로 알려줘.

| ID | 기능 | 메서드, 경로 |
| --- | --- | --- |
| CHAT-01 | 메시지 요청 보내기 | `POST /chat-requests` |
| CHAT-02 | 받은 요청함 | `GET /chat-requests` |
| CHAT-03 | 요청 보기 | `GET /chat-requests/{roomId}` |
| CHAT-04 | 요청 수락 | `POST /chat-requests/{roomId}/accept` |
| CHAT-05 | 요청 거절 | `POST /chat-requests/{roomId}/decline` |
| CHAT-06 | 수락 전 첫 메시지 수정 | `PATCH /chat-requests/{roomId}/messages/{messageId}` |
| CHAT-07 | 수락 전 요청 취소 | `DELETE /chat-requests/{roomId}/messages/{messageId}` |
| CHAT-08 | 내 채팅방 목록 | `GET /chat-rooms` |
| CHAT-09 | 채팅방 정보 | `GET /chat-rooms/{roomId}` |
| CHAT-10 | 메시지 목록 | `GET /chat-rooms/{roomId}/messages` |
| CHAT-11 | 메시지 보내기 | `POST /chat-rooms/{roomId}/messages` |
| CHAT-12 | 읽음 처리 | `PUT /chat-rooms/{roomId}/read` |
| CHAT-13 | 상단 고정, 알림 끄기 | `PATCH /chat-rooms/{roomId}/preferences` |
| CHAT-14 | 나가기, 끝난 요청 숨기기 | `DELETE /chat-rooms/{roomId}/members/me` |

모두 승인 회원만 쓸 수 있어. 방에 참여하지 않았거나 나간 방은 `404 ROOM_NOT_FOUND`야.

### 방의 종류와 상태

| 종류 (`type`) | 언제 생기나 |
| --- | --- |
| `DIRECT` 1:1 | 메시지 요청(CHAT-01) 또는 1:1 커피챗 수락(COFFEE-04). 두 사람 사이에 1개만 있고 다시 쓰여 |
| `GROUP` 단체 | 여러 명 커피챗 수락(COFFEE-04). 받는 사자와 신청자 전원 |
| `GROUP` 번개 | 번개 글 작성(POST-03). 번개 참여자 전원(아래 "번개 단체방") |

| 상태 (`status`) | 뜻 | 메시지 보내기 |
| --- | --- | --- |
| `REQUESTED` | 메시지 요청을 보냈고 아직 답이 없음 | 안 돼 (첫 메시지만 있음) |
| `ACTIVE` | 대화 중 | 돼 |
| `DECLINED` | 요청이 거절됨 | 안 돼 |
| `CANCELED` | 보낸 사람이 요청을 취소함 | 안 돼 |

### 번개 단체방

번개(MEETUP-01)의 단체 채팅방이야. 방 이름은 번개 글 제목이고, CHAT-08의 `title`, CHAT-09의 `meetup`으로 알 수 있어.

- 번개 글을 쓰면(POST-03) 작성자와 함께하는 동문이 들어간 방이 열리고 '번개 채팅방이 열렸어요. 모임 전에 인사 나눠보세요.'가 남아
- 번개에 참여하면 방에 들어오고 '{이름}님이 들어왔어요', 참여를 취소하면 방에서 나가고 '{이름}님이 나갔어요'가 남아. 다시 참여하면 다시 들어와
- 번개가 끝나기 전에는 채팅방만 나갈 수 없어(CHAT-14). 번개 참여는 그대로인데 방만 없어지는 걸 막으려고 그래
- 번개가 취소되면(POST-06) '번개가 취소됐어요'를 남기고 방을 닫아. 목록에는 남지만 `canSend: false`이고 보내면 `409 INVALID_ROOM_STATUS`야
- 번개 글을 다른 카테고리로 바꾸면(POST-05, 참여자가 작성자뿐일 때만 가능) 방도 지워
- 이 기능 전에 만든 번개는 방이 없어서, 누가 처음 참여할 때 그때 참여자 전원으로 방을 열어

### 사용자 요약 객체

`{ id, name, generation, lionParts, photoUrl, isDeleted }`야. 탈퇴한 사람은 `isDeleted: true`이고 나머지는 `null`이야.

### 메시지 객체

```json
{ "id": 501, "type": "TEXT", "senderId": 12, "content": "안녕하세요!", "deleted": false,
  "createdAt": "2026-11-13T19:00:00+09:00", "editedAt": null, "unreadCount": 1 }
```

- `type`은 `TEXT` 일반 메시지, `SYSTEM` 안내(커피챗 수락·일정 확정 등). `SYSTEM`은 `senderId`가 `null`이야
- `unreadCount`는 보낸 사람을 뺀 참여자 중 아직 이 메시지를 안 읽은 사람 수야. 단체방에서 내 메시지 옆에 숫자로 보여줘. `SYSTEM`은 `0`

### CHAT-01 메시지 요청 보내기

`POST /chat-requests`

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `receiverId` | number | O | 받을 동문. 본인 불가 |
| `content` | string | O | 첫 메시지 5\~300자 |

- 두 사람의 1:1 방이 이미 대화 중(`ACTIVE`)이거나 요청 중(`REQUESTED`, 누가 보냈든)이면 새로 만들지 않고 그 방을 `200`으로 돌려줘(`created: false`). FE는 그 방으로 이동하거나 '이미 요청이 와 있어요'를 보여줘
- 내가 보낸 요청이 거절된 날짜 + 7일 전이면 `409 REQUEST_COOLDOWN`('11월 20일부터 다시 요청할 수 있어요.'). 동문 상세(ALUMNI-02)의 `chat.requestAvailableDate`와 같은 규칙이야
- 그 밖에는 요청을 보내고 `201`이야. 받는 사람에게 `MESSAGE_REQUESTED` 알림과 메시지 요청 메일(9장 "이메일 발송")을 쌓고, 받는 사람의 개인 채널에 `REQUEST_RECEIVED`를 보내

```json
{ "roomId": 55, "status": "REQUESTED", "requestedByMe": true, "created": true }
```

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 글자 수 |
| 400 | `CANNOT_CHAT_SELF` | 나에게 요청 |
| 404 | `USER_NOT_FOUND` | 없는 회원, 승인 전, 탈퇴 |
| 409 | `REQUEST_COOLDOWN` | 거절된 지 7일 안 |

### CHAT-02 받은 요청함

`GET /chat-requests?page=0&size=20`

나에게 온 대기 중 요청을 최근 순서로 줘. 형식은 `{ items, page, size, hasNext }`이고 항목은 `{ roomId, requester(사용자 요약), firstMessage(메시지 객체), requestedAt }`야.

### CHAT-03 요청 보기

`GET /chat-requests/{roomId}` · 보낸 사람, 받는 사람

`{ roomId, status, requester, receiver, firstMessage, requestedAt, declinedAt, declineReason, declineMessage }`를 줘.

### CHAT-04 요청 수락

`POST /chat-requests/{roomId}/accept` · 받는 사람, `REQUESTED`

방이 `ACTIVE`가 되고 안내 메시지가 남아. 받는 사람은 요청 메시지까지 읽은 것으로 처리해. 보낸 사람에게 `MESSAGE_REQUEST_ACCEPTED` 알림을 저장하고 `200`에 CHAT-09 형식을 돌려줘.

### CHAT-05 요청 거절

`POST /chat-requests/{roomId}/decline` · 받는 사람, `REQUESTED`

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `reason` | string | O | `BUSY` 요즘 바빠서 대화가 어려워요, `NOT_MY_FIELD` 제가 도움드리기 어려운 분야예요, `PREFER_COFFEE_CHAT` 커피챗으로 신청해주세요, `ETC` 기타 |
| `message` | string | `ETC`면 O | 5\~100자 |

방이 `DECLINED`가 되고 `204`야. 받는 사람의 요청함에서 사라지고, 보낸 사람 목록에는 '거절됨'으로 남아(숨기기는 CHAT-14).

### CHAT-06 수락 전 첫 메시지 수정

`PATCH /chat-requests/{roomId}/messages/{messageId}` · 보낸 사람, `REQUESTED`

`{ "content": "5~300자" }`를 받고 `200`에 메시지 객체(`editedAt` 채워짐)를 돌려줘.

### CHAT-07 수락 전 요청 취소

`DELETE /chat-requests/{roomId}/messages/{messageId}` · 보낸 사람, `REQUESTED`

첫 메시지를 지우고 방을 `CANCELED`로 바꿔. `204`야.

### CHAT-04~07 공통 에러

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 글자 수, 기타인데 사유 없음 |
| 403 | `FORBIDDEN_ACTION` | 보낸 사람이 수락·거절하거나, 받는 사람이 수정·취소 |
| 404 | `ROOM_NOT_FOUND`, `MESSAGE_NOT_FOUND` | 없는 방이나 메시지, 메시지 요청 방이 아님 |
| 409 | `INVALID_ROOM_STATUS` | 이미 답한 요청 |

### CHAT-08 내 채팅방 목록

`GET /chat-rooms?page=0&size=20`

대화 중인 방, 내가 보낸 요청(대기 중, 거절됨)을 보여줘. 상단 고정한 방이 먼저고, 그다음 최근 대화순이야. 받은 요청은 요청함(CHAT-02)에 있어.

```json
{
  "items": [{
    "roomId": 55, "type": "DIRECT", "status": "ACTIVE", "requestedByMe": false,
    "partner": { "id": 12, "name": "김사자", "...": "사용자 요약" },
    "members": [], "memberCount": 2, "coffeeChatId": 31, "meetupId": null, "title": null,
    "lastMessage": { "...": "메시지 객체" }, "unreadCount": 3,
    "pinned": false, "muted": false
  }],
  "page": 0, "size": 20, "hasNext": false
}
```

- `partner`는 1:1 방의 상대, `members`는 단체방의 나를 뺀 참여자야
- `unreadCount`는 내가 마지막으로 읽은 뒤에 다른 사람이 보낸 메시지 수야
- `meetupId`, `title`은 번개 단체방일 때 번개 글 ID와 제목이야. 다른 방은 `null`

### CHAT-09 채팅방 정보

`GET /chat-rooms/{roomId}`

`{ roomId, type, status, requestedByMe, members(나를 뺀 참여자), coffeeChat({ id, status, scheduledAt } 또는 null), meetup({ postId, title, meetAt, place, status } 또는 null), pinned, muted, canSend }`를 줘. `meetup.status`는 MEETUP-01과 같은 `OPEN` `FULL` `ENDED` `CANCELED`야. `canSend`는 `ACTIVE`이고 닫히지 않은 방(취소된 번개 방이 아님)일 때만 `true`야.

### CHAT-10 메시지 목록

`GET /chat-rooms/{roomId}/messages?before={messageId}&size=30` · `GET …/messages?after={messageId}`

- `before`: 그 메시지보다 오래된 메시지(처음 열 때는 비워서 최신 메시지). 더 위로 올릴 때 써
- `after`: 그 메시지 다음부터. 연결이 끊긴 동안 온 메시지를 이어 받을 때 써
- 항상 오래된 것부터 정렬해서 `{ items, hasMore }`로 줘. `size`는 1\~100, 기본 30
- 내가 나간 뒤 다시 들어온 1:1 방은 나가기 전 메시지를 안 보여줘

### CHAT-11 메시지 보내기

`POST /chat-rooms/{roomId}/messages` · `ACTIVE` 방의 참여자

| 요청 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `content` | string | O | 1\~1000자 |
| `clientMsgId` | string | X | FE가 만든 UUID. 같은 값으로 다시 보내면 새로 저장하지 않고 처음 메시지를 돌려줘(재전송 중복 방지) |

`201`에 메시지 객체를 돌려주고, 방 채널에 `MESSAGE`를 보내. 1:1 방에서 상대가 방을 나가 있었으면 다시 목록에 나타나(이전 대화는 안 보임). 보낸 메시지는 내가 읽은 것으로 쳐.

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 글자 수 |
| 404 | `ROOM_NOT_FOUND` | 없거나 내가 없는 방 |
| 409 | `INVALID_ROOM_STATUS` | 대화 중이 아니거나 닫힌 방(취소된 번개) |

### CHAT-12 읽음 처리

`PUT /chat-rooms/{roomId}/read`

`{ "lastMessageId": 501 }`(비우면 방의 최신 메시지)까지 읽은 것으로 기록하고 방 채널에 `READ`를 보내. 이전보다 앞으로 되돌리지는 않아. `200`에 `{ "roomId": 55, "lastReadMessageId": 501 }`이야.

### CHAT-13 상단 고정, 알림 끄기

`PATCH /chat-rooms/{roomId}/preferences`

`{ "pinned": true, "muted": false }` 중 보낸 값만 바꾸고 `200`에 `{ roomId, pinned, muted }`를 돌려줘. 알림을 끄면 개인 채널의 `ROOM_UPDATED`에 `muted: true`가 실려서 FE가 소리·토스트를 안 띄우면 돼.

### CHAT-14 나가기, 끝난 요청 숨기기

`DELETE /chat-rooms/{roomId}/members/me` · `204`

- 1:1 방: 내 목록에서 숨기고 지금까지의 대화를 내 화면에서 지워. 상대가 새 메시지를 보내면 다시 나타나
- 단체방: 방을 나가고 '{이름}님이 나갔어요' 안내가 남아. 다시 들어올 수 없어
- 번개 단체방: 번개가 끝나기 전에는 `409 INVALID_ROOM_STATUS`('번개가 끝나기 전에는 나갈 수 없어요. 번개 참여를 취소하면 방에서도 나가요.'). 끝난 뒤에는 단체방처럼 나가고, 취소된 번개 방은 내 목록에서 숨겨
- 거절·취소된 요청: 내 목록에서 숨겨
- 대기 중인 요청은 나가기 대신 취소(CHAT-07)나 거절(CHAT-05)을 써(`409 INVALID_ROOM_STATUS`)

### 실시간 (WebSocket STOMP)

| 항목 | 값 |
| --- | --- |
| 연결 주소 | `ws(s)://{서버}/ws` (SockJS 없이 순수 WebSocket) |
| 로그인 | STOMP `CONNECT` 프레임 헤더 `Authorization: Bearer {accessToken}`. 개발 서버는 REST처럼 `X-User-Id`도 받아 |
| 방 채널 | `/topic/chat-rooms/{roomId}` 구독. 참여 중인 방만 구독할 수 있어 |
| 개인 채널 | `/user/queue/chat` 구독 |
| 보내기 | STOMP로 보내지 않고 REST(CHAT-11)로 보내 |

| 채널 | `type` | 내용 |
| --- | --- | --- |
| 방 | `MESSAGE` | `{ type, roomId, message(메시지 객체) }` |
| 방 | `READ` | `{ type, roomId, userId, lastReadMessageId }` |
| 개인 | `ROOM_UPDATED` | `{ type, roomId, muted }`. 내 방에 새 메시지가 오거나 상태가 바뀜. 목록·안 읽은 수를 다시 불러 |
| 개인 | `REQUEST_RECEIVED` | `{ type, roomId }`. 새 메시지 요청. 대기 중인 요청은 받는 사람에게 `ROOM_UPDATED` 없이 이것만 가 |

연결이 끊겼다가 다시 붙으면 CHAT-10의 `after`로 놓친 메시지를 받아.

### 채팅이 저장하는 알림

채팅 탭(`CHAT`)·`ROOM` 대상으로 저장해. 일반 메시지는 알림을 만들지 않고, 안 읽은 수와 개인 채널로 알려.

| `type` | 받는 사람 | 보낸 사람(`actor`) | `message` |
| --- | --- | --- | --- |
| `MESSAGE_REQUESTED` | 받는 사람 | 요청한 회원 | `님이 메시지를 보냈어요.` |
| `MESSAGE_REQUEST_ACCEPTED` | 보낸 사람 | 수락한 회원 | `님이 메시지 요청을 수락했어요.` |
