# LION LINK API 명세서

> 전체 API 목록(85개)은 아래 "전체 API 목록". 상세 명세는 계정·인증(AUTH)·프로필(PROFILE)·관리자(ADMIN) — 담당 이시형. 나머지 영역 상세는 팀 노션 API 명세서가 원본이다.
> 마지막 갱신: 2026-11-12 (구글 로그인 Authorization Code 방식 반영). 기준: 기능명세서 v1.17, `docs/erd.md`

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
| 목록 | 페이지 방식은 `page`(0부터), `size`로 요청하고 `{ items, page, size, totalElements }`로 응답해 |
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

팀 노션 API 명세서의 86행을 영역별로 모았다(2026-11-12 기준). `GET /users/{userId}`가 두 영역에 중복돼 있어 동문 찾기에만 두었고, 그래서 85개다. 상세 명세는 계정·인증·프로필·관리자만 이 문서에 있고, 나머지는 각 담당자의 노션 페이지가 원본이다.

| 영역 | API 수 | 담당 (노션 기준) | 상세 명세 |
| --- | --- | --- | --- |
| 1. 계정·인증 | 9 | 이시형 | 이 문서 아래 |
| 2. 프로필 | 18 | 이시형 | 이 문서 아래 |
| 3. 관리자 | 6 | 이시형 | 이 문서 아래 |
| 4. 커피챗 | 10 | 커피챗·채팅·알림 담당 | **미작성** |
| 5. 동문 찾기 | 5 | 동문·커뮤니티·홈 담당 | 노션 |
| 6. 커뮤니티 | 15 | 동문·커뮤니티·홈 담당 | 노션 |
| 7. 홈 | 2 | 동문·커뮤니티·홈 담당 | 노션 |
| 8. 채팅 | 14 | 커피챗·채팅·알림 담당 | **미작성** |
| 9. 알림 | 6 | 커피챗·채팅·알림 담당 | **미작성** |

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
| 4. 커피챗 | `GET` | `/coffee-chats` | 받은·보낸 신청 보기 | 커피챗·채팅·알림 담당 |
| 4. 커피챗 | `POST` | `/coffee-chats` | 커피챗 신청 | 커피챗·채팅·알림 담당 |
| 4. 커피챗 | `GET` | `/coffee-chats/{coffeeChatId}` | 신청 내용과 가능한 버튼 보기 | 커피챗·채팅·알림 담당 |
| 4. 커피챗 | `POST` | `/coffee-chats/{coffeeChatId}/accept` | 신청 수락하기 | 커피챗·채팅·알림 담당 |
| 4. 커피챗 | `POST` | `/coffee-chats/{coffeeChatId}/reject` | 신청 거절하기 | 커피챗·채팅·알림 담당 |
| 4. 커피챗 | `POST` | `/coffee-chats/{coffeeChatId}/withdraw` | 상대가 답하기 전 신청 거두기 | 커피챗·채팅·알림 담당 |
| 4. 커피챗 | `PUT` | `/coffee-chats/{coffeeChatId}/schedule` | 만날 시간·장소 정하기 또는 바꾸기 | 커피챗·채팅·알림 담당 |
| 4. 커피챗 | `POST` | `/coffee-chats/{coffeeChatId}/cancel` | 수락한 커피챗 취소하기 | 커피챗·채팅·알림 담당 |
| 4. 커피챗 | `POST` | `/coffee-chats/{coffeeChatId}/complete` | 만남 완료 기록하기 | 커피챗·채팅·알림 담당 |
| 4. 커피챗 | `DELETE` | `/coffee-chats/{coffeeChatId}/members/me` | 함께 신청한 커피챗에서 빠지기 | 커피챗·채팅·알림 담당 |
| 5. 동문 찾기 | `GET` | `/users` | 동문 목록 조회 (검색·필터) | 동문·커뮤니티·홈 담당 |
| 5. 동문 찾기 | `GET` | `/users/{userId}` | 동문 프로필 상세 조회 (= PROFILE-03) | 동문·커뮤니티·홈 담당 |
| 5. 동문 찾기 | `GET` | `/users/me/bookmarks` | 관심 동문 목록 조회 | 동문·커뮤니티·홈 담당 |
| 5. 동문 찾기 | `PUT` | `/users/{userId}/bookmark` | 관심 동문 저장 | 동문·커뮤니티·홈 담당 |
| 5. 동문 찾기 | `DELETE` | `/users/{userId}/bookmark` | 관심 동문 저장 취소 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `GET` | `/posts` | 게시글 목록·검색·정렬 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `POST` | `/posts` | 게시글 작성 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `GET` | `/posts/popular-keywords` | 인기 키워드 조회 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `GET` | `/posts/{postId}` | 게시글 상세 조회 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `PATCH` | `/posts/{postId}` | 게시글 수정 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `DELETE` | `/posts/{postId}` | 게시글 삭제 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `GET` | `/posts/{postId}/comments` | 댓글·대댓글 목록 조회 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `POST` | `/posts/{postId}/comments` | 댓글·대댓글 작성 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `DELETE` | `/posts/{postId}/comments/{commentId}` | 댓글 삭제 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `POST` | `/posts/{postId}/meetup/join` | 번개모임 참여 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `GET` | `/posts/{postId}/meetup/members` | 번개모임 참여자 목록 조회 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `DELETE` | `/posts/{postId}/meetup/members/me` | 번개모임 참여 취소 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `POST` | `/posts/{postId}/meetup/invites` | 관심 동문에게 번개 초대 알림 발송 | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `POST` | `/posts/{postId}/meetup/cancel` | 번개모임 취소 (참여자 알림) | 동문·커뮤니티·홈 담당 |
| 6. 커뮤니티 | `POST` | `/files` | 이미지·첨부파일 업로드 URL 발급 | 동문·커뮤니티·홈 담당 |
| 7. 홈 | `GET` | `/home` | 홈 화면 통합 정보 조회 | 동문·커뮤니티·홈 담당 |
| 7. 홈 | `POST` | `/home/coffee-guide/dismiss` | 첫 커피챗 가이드 카드 닫기 | 동문·커뮤니티·홈 담당 |
| 8. 채팅 | `POST` | `/chat-requests` | 첫 메시지 요청 보내기 | 커피챗·채팅·알림 담당 |
| 8. 채팅 | `GET` | `/chat-requests` | 받은 요청함 열기 | 커피챗·채팅·알림 담당 |
| 8. 채팅 | `GET` | `/chat-requests/{roomId}` | 첫 메시지 내용 보기 | 커피챗·채팅·알림 담당 |
| 8. 채팅 | `POST` | `/chat-requests/{roomId}/accept` | 메시지 요청 수락 | 커피챗·채팅·알림 담당 |
| 8. 채팅 | `POST` | `/chat-requests/{roomId}/decline` | 메시지 요청 거절 | 커피챗·채팅·알림 담당 |
| 8. 채팅 | `PATCH` | `/chat-requests/{roomId}/messages/{messageId}` | 수락 전 첫 메시지 수정 | 커피챗·채팅·알림 담당 |
| 8. 채팅 | `DELETE` | `/chat-requests/{roomId}/messages/{messageId}` | 수락 전 첫 메시지 삭제·요청 취소 | 커피챗·채팅·알림 담당 |
| 8. 채팅 | `GET` | `/chat-rooms` | 내 채팅방 목록 | 커피챗·채팅·알림 담당 |
| 8. 채팅 | `GET` | `/chat-rooms/{roomId}` | 채팅방 정보와 참여자 | 커피챗·채팅·알림 담당 |
| 8. 채팅 | `GET` | `/chat-rooms/{roomId}/messages` | 이전 메시지·끊긴 동안 온 메시지 | 커피챗·채팅·알림 담당 |
| 8. 채팅 | `POST` | `/chat-rooms/{roomId}/messages` | 일반 메시지 보내기 | 커피챗·채팅·알림 담당 |
| 8. 채팅 | `PUT` | `/chat-rooms/{roomId}/read` | 읽음 처리 | 커피챗·채팅·알림 담당 |
| 8. 채팅 | `PATCH` | `/chat-rooms/{roomId}/preferences` | 방 고정·알림 끄기 | 커피챗·채팅·알림 담당 |
| 8. 채팅 | `DELETE` | `/chat-rooms/{roomId}/members/me` | 방 나가기·종료된 요청 숨기기 | 커피챗·채팅·알림 담당 |
| 9. 알림 | `GET` | `/notifications` | 최근 알림 20개 | 커피챗·채팅·알림 담당 |
| 9. 알림 | `PUT` | `/notifications/{notificationId}/read` | 알림 하나 읽음 처리 | 커피챗·채팅·알림 담당 |
| 9. 알림 | `PUT` | `/notifications/read-all` | 알림 모두 읽음 처리 | 커피챗·채팅·알림 담당 |
| 9. 알림 | `GET` | `/me/notification-settings` | 이메일 알림 설정 조회 | 커피챗·채팅·알림 담당 |
| 9. 알림 | `PATCH` | `/me/notification-settings` | 이메일 알림 종류별 켜기·끄기 | 커피챗·채팅·알림 담당 |
| 9. 알림 | `GET` | `/email-notifications/unsubscribe` | 메일 링크로 해당 종류 수신 거부 | 커피챗·채팅·알림 담당 |

### 노션 표와 이 문서를 맞추며 찾은 것

| 번호 | 내용 | 제안 |
| --- | --- | --- |
| 1 | `GET /users/{userId}`가 '2. 프로필'(PROFILE-03)과 '5. 동문 찾기' 두 행에 있다 | 동문 찾기(`AlumniController`)에 이미 구현돼 있으니 프로필 쪽 행은 "동문 찾기와 같음"으로 표시 |
| 2 | 다른 영역 명세(홈 등)와 공용 코드(`CommonErrorCode`)는 승인 전 403을 `NOT_APPROVED`, 프로필 미완료를 `PROFILE_NOT_COMPLETED`로 쓴다 | **확정(2026-11-12)**: `NOT_APPROVED`로 통일. 이 문서의 `APPROVAL_PENDING`·`ACCOUNT_REJECTED`는 없앴다 |
| 3 | 커뮤니티가 `POST /files`(업로드 URL 발급, 설계 제안 단계)를 쓴다 | 프로필 사진(PROFILE-05)도 같은 저장소·발급 방식을 쓸지 커뮤니티 담당과 맞춘다 (확정 필요 5번) |
| 4 | 커피챗·채팅·알림 상세 명세가 비어 있다 | 커피챗 신청(4.1)·수락은 프로필의 커피챗 설정(PROFILE-12~15)과 맞물린다. 담당자에게 작성 일정 확인 |
| 5 | 실시간 채팅(5.3)의 WebSocket 연결·구독 경로, 커피챗 함께 신청 모집(4.11) API가 목록에 없다 | 채팅·커뮤니티 담당 확인 |

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
| `chat` | object | `{ status, requestAvailableDate }`. `status`는 `NONE` `REQUESTED` `ACTIVE`, 거절된 지 7일 안이면 `requestAvailableDate`에 다시 보낼 수 있는 날짜 |
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
| 멋사 활동 | `lion-activities` | 6 | `generation` O 1\~14, `role` O (`LION` 아기사자, `STAFF` 운영진, `LEADER` 대표·부대표, `PART_LEADER` 파트장, `ETC`), `parts` O `lionPart` 1\~2개 |
| 멋사 프로젝트·해커톤 | `lion-projects` | 5 | `name` O 1\~30자, `eventType` O (`REGULAR` 정기 프로젝트, `JUNGKATHON` 중커톤, `JOINT_HACKATHON` 연합 해커톤, `ETC`), `generation` O 1\~14, `myRole` 0\~30자, `description` 0\~200자, `link` URL |
| 프로젝트 | `projects` | 10 | `name` O 1\~30자, `startYm`, `endYm` 또는 `isCurrent: true`, `role` 0\~30자, `description` 0\~300자, `link` URL, `tags` string\[\] 0\~5개 |
| 자격증 | `certifications` | 10 | `name` O 1\~30자, `issuer` 0\~30자, `acquiredYm` |
| 수상 | `awards` | 10 | `name` O 1\~30자, `host` 0\~30자, `awardedYm`, `description` 0\~100자 |
| 대외활동 | `activities` | 10 | `name` O 1\~30자, `organization` 0\~30자, `startYm`, `endYm`, `description` 0\~200자 |
| 언어 | `languages` | 5 | `language` O 1\~20자, `level` O (`BASIC` 기초, `CONVERSATIONAL` 일상 회화, `BUSINESS` 비즈니스, `NATIVE` 원어민) |

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
- 인기 키워드 제외 목록(6.1)은 MVP에서 DB(`keyword_blocklist`)에 직접 추가하니까 API가 없어.
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
  "page": 0, "size": 20, "totalElements": 1
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
| `reviewedAt`, `reviewedBy` | 승인됨·반려됨 탭에서 처리한 시각과 관리자 이름. 승인 대기면 `null` |

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
| `userIds` | number\[\] | O | 1\~50개, 중복 불가 |

한 명씩 ADMIN-03과 같은 규칙으로 따로 처리하고, 일부가 실패해도 나머지는 승인돼. 그래서 `200`에 성공한 사람과 못 한 사람을 나눠서 줘.

```json
{
  "approved": [31, 32],
  "skipped": [{ "userId": 33, "code": "ALREADY_PROCESSED" }, { "userId": 34, "code": "NOT_FOUND" }]
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
| 7 | 승인 메일(ADMIN-03)이 알림 설정의 영향을 받는지 | 받지 않는다. 알림 설정에는 커피챗 신청·메시지 요청 두 개뿐이고, 승인 메일은 항상 보낸다 | BE, 알림 담당 |
| 8 | 관리자 계정이 승인됨 탭에 나오는지 | 나온다(`role`과 관계없이 `status` 기준). 필요하면 제외한다 | BE, FE |
| 9 | 목록 조회의 정렬·페이지 크기 | 승인 대기는 오래된 순, 나머지 탭은 처리 시각 최신순, 기본 20개(최대 100) | FE |
| 10 | 클라이언트 입력 기간 형식 | 연월은 `"2024-03"`, 날짜는 `"2026-11-20"`, 시각은 KST ISO-8601 | FE |
| 11 | 승인 전 회원 차단 코드 | **확정(2026-11-12)**: 공용 코드·다른 영역 명세와 같은 `403 NOT_APPROVED` 하나로 통일. 승인 대기·반려 구분은 FE가 `GET /me`의 `status`로 한다. 가입 정보 수정(AUTH-06)을 반려 회원이 부르면 상태 충돌이라 `409 NOT_PENDING` | 완료 |
| 12 | 탈퇴(AUTH-08) 본인 재확인 | 구글 로그인을 Authorization Code 방식으로 바꿔 FE가 ID 토큰을 받지 않는다. 구글 재로그인 리다이렉트 등으로 다시 설계한다 | BE, FE |

리프레시 토큰은 Redis 대신 DB `refresh_tokens` 테이블에 SHA-256 해시로 저장하기로 정했다(2026-11-12).
