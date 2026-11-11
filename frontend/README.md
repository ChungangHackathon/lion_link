# 멋사동네 (LION:LINK) Frontend

Next.js 16 · React 19 · Tailwind v4. Figma 「먼크강 멋사동네」 *05 Desktop v2 · 데스크톱 와이어프레임* (오른쪽 310번대 세트, GD Guide 00~16)을 기준으로 구성했어요.

```bash
npm install
npm run dev     # http://localhost:3000
npm run build
```

백엔드 연동 전이라 모든 데이터는 `lib/mock/*`의 더미이고, 상태는 `lib/store.tsx`(메모리)와 `lib/session.tsx`(localStorage)에 들어 있어요. `/login` 하단의 ‘프로토타입’ 버튼으로 **회원 / 승인 대기 / 관리자 / 초대 가입** 상태를 바꿔 모든 흐름을 볼 수 있어요.

## 구조

```
app/
  (auth)/      login(A-10) · invite/[token](A-01~07) · onboarding(O-01·02) · pending(A-13·15) · rejected(A-14)
  (main)/      GNB + 회원 영역 (승인 대기면 /pending, 로그아웃이면 /login으로)
    home/            H-01 · H-02 · H-04 · H-05 · H-06 · CF-21
    alumni/          S-01 · S-02 · S-03 · S-04 /[id] P-01 · P-03~06 · CF-10
    coffee/          M-01 · M-02 · M-03  /[id] D-01~D-07 · D-06b
    chat/            T-01 · /[id] T-17 · T-27 · T-05 · T-23 · CF-9/9b · /requests T-12 · T-14 · T-15
    community/       B-01 · B-02 · B-03  /new B-05 · B-06 · B-07 · B-09 · CF-14 · L-11  /[id] B-12 · B-12b · B-13 · K-01 · L-06 · K-03
    me/              P-02 · E-04 · E-07 · E-09~E-15 · CF-23  /coffee-settings CS-01 · CS-03 · CF-5
                     /notifications N-01 · N-02 · N-04  /bookmarks X-01  /blocked X-02  /settings X-03 · CF-3
    admin/           AD-10 · AD-11 · AD-12 · CF-18  /invites AD-02 · AD-03 · AD-04 · AD-07 · CF-17 · CF-19  /posts
  403, not-found, error        C-03 · C-04 · C-05
components/
  ui/        Button · Chip · Input · Textarea · Select · Checkbox · Radio · Switch · Badge(Status/Count/Availability/PartTag/Tag)
             Avatar · Modal · ConfirmModal · Popover · Toast · EmptyState · Banner · ProgressBar · Tabs · Tooltip · Card · Icon/Glyph
  layout/    Gnb · Logo · SideNav · PageHeader · NotificationDropdown(H-07) · ProfileMenu(C-01/C-01b) · AuthShell
  domain/    AlumniCard · AlumniPicker(C-20) · CoffeeRequestModal(R-01/R-01b/R-02/R-05/C-08) · MessageRequestModal(T-04)
             CoffeeChatDetail(D-01~07 + D-10/D-11/D-15/D-16/D-17/CF-7) · ProfileSections · PostRow
  brand/     VillageHero (A-01 로고·마을 일러스트, public/brand)
lib/
  types.ts   백엔드 enum과 같은 이름 (UserStatus, CoffeeChatStatus, LionPart …)
  store.tsx  신청·수락·거절·일정·채팅·글·댓글·승인·초대 액션 — API 붙일 때 여기만 바꾸면 돼요
```

## 디자인 토큰

`app/globals.css`의 `@theme`에 Figma 변수를 그대로 옮겼어요: `primary`(CAU BLUE #2155A4), `orange`(LION ORANGE #FF6000 – 숫자 뱃지·에러만), `navy`(#00245E – GNB), `green`(#22A45D – 커피챗 가능), 회색 `ink-900/700/500/400`, `line-300/200`, `bg-100/50`, 파트 색 `part-fe/be/design/plan-*`. 타이포는 `t-title`, `t-title3`, `t-body-bold`, `t-body2`, `t-label1/2`, `t-caption1/2` 유틸리티. 폰트는 Pretendard(CDN) + 로고용 Nanum Gothic.

## 흐름 (GD Guide Flows F1~F19)

| 흐름 | 경로 |
| --- | --- |
| F1 초대 가입 → 승인 | `/invite/abc123` → 동의 → `/onboarding` → `/pending` → (프로토타입 버튼) 승인 → `/home` |
| F2 로그인 | `/login` → 구글 → `/home` |
| F3 선배 찾고 함께 신청 | `/home` → `/alumni` → 필터 → `/alumni/u-1` → 커피챗 신청 모달 → 동문 추가 → `/coffee?tab=sent` |
| F4 받은 신청 수락 → 만남 | `/coffee` → 수락(일정 후보) → `/chat/[room]` → 일정 확정 → 만남 완료 |
| F5 거절 / F6 신청 취소 | `/coffee` 거절 모달(사유 + 쉬기) / 보낸 신청 → 신청 취소 |
| F7·F8 메시지 요청 | 프로필 → 메시지 보내기 → `/chat/[id]` / `/chat/requests` → 수락·거절·차단 |
| F9·F10 단체방 · 방 관리 | `/chat/r-3` 참여자 · 나가기 확인 |
| F11~F13 커뮤니티 · 모집 · 번개 | `/community` → `/community/new` (카테고리 번개/커피챗) → `/community/[id]` |
| F14·F15 프로필 편집 · 커피챗 설정 | `/me` 연필 아이콘 → 팝업 / `/me/coffee-settings` |
| F16 알림 메일 | `/me/notifications` → 메일 미리보기 → `/coffee/c-1` |
| F17 관리자 가입 승인 | `/login` ‘관리자로’ → `/admin` → 반려 사유 → 확인 |
| F18 알림 | GNB 🔔 → 수락하기/거절하기 → `/coffee/[id]?action=…` |
| F19 탈퇴 | `/me/settings` → 확인창 → `/login` |
