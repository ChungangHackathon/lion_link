"use client";

import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { UserRole, UserStatus } from "@/lib/types";
import { ADMIN_ID, ME_ID, byId } from "@/lib/mock/users";

/**
 * Demo session. There is no backend auth yet, so the "logged-in" user lives in
 * localStorage. Role/status switches let you walk every flow in the wireframe:
 *  - MEMBER / APPROVED → 박중앙 (default)
 *  - ADMIN / APPROVED  → 정운영 (관리자 화면)
 *  - MEMBER / PENDING  → 승인 대기 (GNB disabled, /pending)
 */
export interface Session {
  userId: string;
  role: UserRole;
  status: UserStatus;
  loggedIn: boolean;
  firstVisit: boolean; // H-04 승인 후 첫 방문
}

interface SessionContextValue {
  session: Session;
  user: ReturnType<typeof byId>;
  login: (opts?: Partial<Session>) => void;
  logout: () => void;
  update: (patch: Partial<Session>) => void;
}

const DEFAULT: Session = { userId: ME_ID, role: "MEMBER", status: "APPROVED", loggedIn: true, firstVisit: false };
const KEY = "lionlink.session";

const SessionContext = createContext<SessionContextValue | null>(null);

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<Session>(DEFAULT);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(KEY);
      if (raw) setSession({ ...DEFAULT, ...JSON.parse(raw) });
    } catch {}
  }, []);

  const value = useMemo<SessionContextValue>(() => {
    const persist = (s: Session) => {
      setSession(s);
      try {
        localStorage.setItem(KEY, JSON.stringify(s));
      } catch {}
    };
    return {
      session,
      user: byId(session.userId),
      login: (opts) =>
        persist({
          ...DEFAULT,
          ...opts,
          userId: opts?.role === "ADMIN" ? ADMIN_ID : opts?.userId ?? ME_ID,
          loggedIn: true,
        }),
      logout: () => persist({ ...DEFAULT, loggedIn: false }),
      update: (patch) => persist({ ...session, ...patch }),
    };
  }, [session]);

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("useSession must be used inside SessionProvider");
  return ctx;
}
