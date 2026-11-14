# 멋사동네 (LION:LINK) Frontend

Next.js 16 · React 19 · Tailwind v4. Figma 「먼크강 멋사동네」 **수정 화면** 페이지(디자인 1차 완성본, v2.11 · node 906:16294)를 기준으로 해요. 화면 흐름은 *05 Desktop v2*의 GD Guide Flows를 그대로 따르고, 수정 화면에서 지운 화면은 뺐어요.

```bash
npm install
npm run dev     # http://localhost:3000
npm run build
```

## 백엔드 연결

```bash
cp .env.example .env.local   # NEXT_PUBLIC_API_URL=http://localhost:8080
```

- `NEXT_PUBLIC_API_URL`이 있으면 **API 모드**예요. 로그인·가입 신청·승인 대기·반려 화면이 실제 API(`docs/api.md` 1. 계정·인증)로 동작해요.
- 비워 두면 **프로토타입 모드**예요. 모든 데이터가 `lib/mock/*`의 더미이고, `/login` 하단의 ‘프로토타입’ 버튼으로 **회원 / 승인 대기 / 관리자 / 가입 신청** 상태를 바꿔 모든 흐름을 볼 수 있어요.
- 로컬 백엔드의 `FRONTEND_URL`은 기본값 `http://localhost:3000`이라 따로 설정할 게 없어요. 구글 로그인을 하려면 백엔드에 `GOOGLE_CLIENT_ID`·`GOOGLE_CLIENT_SECRET`이 있어야 해요.

API 호출은 `lib/api/client.ts`의 `api()`를 써요. 토큰 헤더, 401일 때 재발급 1회, 에러(`ApiError`의 `code`·`message`)를 공통으로 처리해요.

```ts
import { api, type Page } from "@/lib/api/client";

const posts = await api<Page<Post>>("/posts", { query: { page: 0, size: 20 } });
await api(`/posts/${id}/like`, { method: "PUT" });
```

영역별 API 함수는 `lib/api/{영역}.ts`에 모아요 (예: `lib/api/auth.ts`). 아직 연결하지 않은 화면은 API 모드에서도 `lib/mock/*`와 `lib/store.tsx`(메모리)를 그대로 써요.

## 구조

```
app/
  (auth)/      login(A-10) · signup(A-01 가입 신청) · onboarding(O-01·02) · pending(A-13·15) · rejected(A-14)
               invite/[token] → /signup 으로 이동 (초대 링크 가입은 디자인에서 삭제)
  (main)/      GNB + 회원 영역 (승인 대기면 /pending, 로그아웃이면 /login으로)
    home/            H-01 · H-02 · H-04 · H-05 · H-06 · CF-21
    alumni/          S-01 · S-02 · S-03 · S-04 /[id] P-01 · P-03~06 · CF-10
    coffee/          M-01 · M-02 · M-03 · M-04 (상세는 오른쪽 영역, /coffee?id=…&action=accept|reject)  /[id] → /coffee?id= 로 이동
    chat/            T-01 · /[id] T-17 · T-27 · T-05 · T-23 · CF-9/9b · /requests T-12 · T-14 · T-15
    community/       B-01 · B-02 · B-03  /new B-05 · B-06 · B-07 · B-09 · CF-14 · L-11  /[id] B-12 · B-12b · B-13 · K-01 · L-06 · K-03
    me/              P-02 · E-04 · E-07 · E-09~E-15 · CF-23  /coffee-settings CS-01 · CS-03 · CF-5
                     /notifications N-01 · N-02 · N-04  /bookmarks X-01  /settings X-03 · CF-3
    admin/           AD-10 (이름·기수 연필 · 참고 열) · AD-11 · AD-12 · CF-18  /posts
  403, not-found, error        C-03 · C-04 · C-05
components/
  ui/        Button · Chip · Input · Textarea · Select · Checkbox · Radio · Switch · Badge(Status/Count/Availability/PartTag/Tag)
             Avatar(Avatar/Part 파트 색 + 이니셜) · Modal · ConfirmModal · Popover · Toast · EmptyState · Banner · ProgressBar · Tabs · Tooltip · Card
             Icon — Figma Icon/* 39종(public/icons, CSS mask라 text 색을 따라감). Glyph는 Icon의 옛 이름
  layout/    Gnb · Logo · SideNav · PageHeader · NotificationDropdown(H-07) · ProfileMenu(C-01/C-01b) · AuthShell
  domain/    AlumniCard · AlumniPicker(C-20) · CoffeeRequestModal(R-01/R-01b/R-02/R-05/C-08) · MessageRequestModal(T-04)
             CoffeeChatDetail(D-01~07 + D-10/D-11/D-15/D-16/D-17/CF-7) · ProfileSections · PostRow
  brand/     VillageHero (A-01 로고·마을 일러스트, public/brand)
lib/
  types.ts   백엔드 enum과 같은 이름 (UserStatus, CoffeeChatStatus, LionPart …)
  store.tsx  신청·수락·거절·일정·채팅·글·댓글·승인·초대 액션 — API 붙일 때 여기만 바꾸면 돼요
```

## 디자인 토큰

`app/globals.css`의 `@theme`에 Figma 변수를 그대로 옮겼어요: `primary`(#4F6FA8 – 버튼·링크), `primary-strong`(#2155A4 – 진행 바), `cau`(CAU BLUE #004C97 – 로고 "멋사"·강조 숫자·체크), `blue-100/50`, `orange`(LION ORANGE #FF6000 – 숫자 뱃지·에러만), `navy`(#00245E – GNB), `green`(#22A45D – 커피챗 가능), 회색 `ink-900/700/500/400`, `line-300/200`, `bg-100/50`, 파트 색 `part-fe/be/design/plan-*`. 타이포는 `t-title`, `t-title3`, `t-body-bold`, `t-body2`, `t-label1/2`, `t-caption1/2` 유틸리티. 폰트는 Pretendard(CDN). GNB 로고는 OkDanDan(옥단단) 워드마크를 Figma에서 윤곽선 SVG로 내보낸 `public/brand/logo.svg`예요(폰트 파일은 재배포 금지라 넣지 않음).

## 흐름 (GD Guide Flows F1~F19)

| 흐름 | 경로 |
| --- | --- |
| F1 가입 신청 → 승인 | `/signup` 이름·기수·동의 → `/onboarding` → `/pending` → (프로토타입 버튼) 승인 → `/home` (첫 커피챗 가이드) |
| F2 로그인 | `/login` → 구글 → `/home` |
| F3 선배 찾고 함께 신청 | `/home` → `/alumni` → 필터 → `/alumni/u-1` → 커피챗 신청 모달 → 동문 추가 → `/coffee?tab=sent` |
| F4 받은 신청 수락 → 만남 | `/coffee` 오른쪽 상세 → 수락(일정 후보) → `/chat/[room]` → 일정 확정 → 만남 완료 |
| F5 거절 / F6 신청 취소 | `/coffee` 거절 모달(사유 + 쉬기) / 보낸 신청 → 신청 취소 |
| F7·F8 메시지 요청 | 프로필 → 메시지 보내기 → `/chat/[id]` / `/chat/requests` → 수락·거절 |
| F9·F10 단체방 · 방 관리 | `/chat/r-3` 참여자 · 나가기 확인 |
| F11~F13 커뮤니티 · 모집 · 번개 | `/community` → `/community/new` (카테고리 번개/커피챗) → `/community/[id]` |
| F14·F15 프로필 편집 · 커피챗 설정 | `/me` 연필 아이콘 → 팝업 / `/me/coffee-settings` |
| F16 알림 메일 | `/me/notifications` → 메일 미리보기 → `/coffee?id=c-1` |
| F17 관리자 가입 승인 | `/login` ‘관리자로’ → `/admin` → 반려 사유 → 확인 |
| F18 알림 | GNB 🔔 → 수락하기/거절하기 → `/coffee?id=…&action=…` |
| F19 탈퇴 | `/me/settings` → 확인창 → `/login` |

## 디자인 1차(수정 화면)에서 바뀐 것

- 삭제: 초대 링크 가입(A-02·A-05~07), 관리자 초대 관리(AD-02~07), 커피챗 전체 화면 상세(D-01~07), 차단·차단 목록(X-02, CF-10)
- 용어: 선배/후배 → **사자**, 여는 기간 → 커피챗 받는 기간, 기술·키워드 → 대표 기술, 프로필 링크 → 외부 링크
- 홈 H-01 왼쪽 내 정보 패널(파트 커버 · 커피챗 카드 5상태 · 연락처 · 활동 요약), H-04 첫 커피챗 가이드, H-06 쉬기 기간 선택
- 동문 찾기 4열 카드 · 빠른 필터 · 적용된 필터 줄 · 정렬 드롭다운, 마이페이지 P-02 · 커피챗 설정 CS-01(달력 · 미리보기) · AD-10 표 개편
- Figma에 🛠/💬 메모로 남은 화면(O-01~03, M-03, T-17·T-18 카드, B-06·B-07 레이아웃 등)은 디자인이 확정되면 다시 맞춰야 해요
