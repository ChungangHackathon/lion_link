"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { UserRole, UserStatus } from "@/lib/types";
import { ADMIN_ID, ME_ID, byId } from "@/lib/mock/users";
import { API_ENABLED, refreshAccessToken, setUnauthenticatedHandler, tokenStore } from "@/lib/api/client";
import { getMe, logout as logoutApi, type Me } from "@/lib/api/auth";

/**
 * 로그인 상태.
 *
 * - API 모드(NEXT_PUBLIC_API_URL 있음): 앱을 열면 refreshToken 쿠키로 accessToken을 받고
 *   GET /me로 회원 상태를 가져온다. role·status는 서버 값이다.
 * - 프로토타입 모드(NEXT_PUBLIC_API_URL 없음): 예전처럼 localStorage에 데모 세션을 둔다.
 *   로그인 화면의 데모 버튼으로 역할·상태를 바꿔 볼 수 있다.
 *
 * 아직 API를 붙이지 않은 화면은 mock 동문 데이터(`user`)를 쓰므로, API 모드에서도
 * `userId`는 mock 사용자 ID를 가리키고 이름·기수만 서버 값으로 덮어쓴다.
 * 프로필 API를 연결하면 `user` 대신 `me`/프로필 응답을 쓰도록 바꾼다.
 */
export interface Session {
  userId: string;
  role: UserRole;
  status: UserStatus;
  loggedIn: boolean;
  firstVisit: boolean; // H-04 승인 후 첫 방문
  profileCompleted: boolean;
}

interface SessionContextValue {
  session: Session;
  /** mock 동문 데이터 기반 사용자 (API 모드에서는 이름·기수만 서버 값) */
  user: ReturnType<typeof byId>;
  /** GET /me 응답. 프로토타입 모드이거나 로그인 전이면 null */
  me: Me | null;
  /** 첫 로그인 확인이 끝났는지. false인 동안에는 화면 분기를 하지 않는다 */
  ready: boolean;
  /** 프로토타입 모드 전용 데모 로그인 */
  login: (opts?: Partial<Session>) => void;
  logout: () => Promise<void>;
  update: (patch: Partial<Session>) => void;
  /** 가입 신청·정보 수정처럼 서버가 내 정보를 돌려줬을 때 반영한다 */
  setMe: (me: Me) => void;
  /** GET /me를 다시 불러온다. 로그인이 풀렸으면 null */
  refreshMe: () => Promise<Me | null>;
}

const DEFAULT: Session = { userId: ME_ID, role: "MEMBER", status: "APPROVED", loggedIn: true, firstVisit: false, profileCompleted: true };
const LOGGED_OUT: Session = { ...DEFAULT, loggedIn: false };
const KEY = "lionlink.session";

const SessionContext = createContext<SessionContextValue | null>(null);

function sessionFromMe(me: Me, firstVisit: boolean): Session {
  return {
    userId: me.role === "ADMIN" ? ADMIN_ID : ME_ID,
    role: me.role,
    status: me.status,
    loggedIn: true,
    firstVisit,
    profileCompleted: me.profileCompleted,
  };
}

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<Session>(API_ENABLED ? LOGGED_OUT : DEFAULT);
  const [me, setMeState] = useState<Me | null>(null);
  const [ready, setReady] = useState(false);

  const applyMe = useCallback((next: Me | null) => {
    setMeState(next);
    setSession((prev) => (next ? sessionFromMe(next, prev.firstVisit) : LOGGED_OUT));
  }, []);

  const refreshMe = useCallback(async () => {
    try {
      if (!tokenStore.get()) {
        const token = await refreshAccessToken();
        if (!token) {
          applyMe(null);
          return null;
        }
      }
      const next = await getMe();
      applyMe(next);
      return next;
    } catch {
      // 서버에 닿지 못했거나 재발급이 실패함 → 로그인 전 상태로 본다
      applyMe(null);
      return null;
    }
  }, [applyMe]);

  useEffect(() => {
    if (!API_ENABLED) {
      try {
        const raw = localStorage.getItem(KEY);
        if (raw) setSession({ ...DEFAULT, ...JSON.parse(raw) });
      } catch {}
      setReady(true);
      return;
    }

    // 재발급까지 실패하면(로그인 만료) 로그인 전 상태로 돌린다. 화면 이동은 각 레이아웃이 한다.
    setUnauthenticatedHandler(() => applyMe(null));
    let alive = true;
    refreshMe().finally(() => {
      if (alive) setReady(true);
    });
    return () => {
      alive = false;
      setUnauthenticatedHandler(null);
    };
  }, [applyMe, refreshMe]);

  const value = useMemo<SessionContextValue>(() => {
    const persist = (s: Session) => {
      setSession(s);
      if (API_ENABLED) return;
      try {
        localStorage.setItem(KEY, JSON.stringify(s));
      } catch {}
    };
    const mockUser = byId(session.userId);
    return {
      session,
      user: me && mockUser ? { ...mockUser, name: me.name, generation: me.generation } : mockUser,
      me,
      ready,
      login: (opts) =>
        persist({
          ...DEFAULT,
          ...opts,
          userId: opts?.role === "ADMIN" ? ADMIN_ID : opts?.userId ?? ME_ID,
          loggedIn: true,
        }),
      logout: async () => {
        if (!API_ENABLED) {
          persist(LOGGED_OUT);
          return;
        }
        applyMe(null);
        await logoutApi();
      },
      update: (patch) => persist({ ...session, ...patch }),
      setMe: applyMe,
      refreshMe,
    };
  }, [session, me, ready, applyMe, refreshMe]);

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("useSession must be used inside SessionProvider");
  return ctx;
}
