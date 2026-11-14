/** 홈 (docs/api.md HOME-01 · 02) + 홈 '커피챗 받기' 스위치 (PROFILE-14) */

import type { PostCategory } from "@/lib/types";
import type { AlumniSummary } from "./alumni";
import type { CoffeeSettings } from "./coffeeSettings";
import type { ApiLionPart } from "./profile";
import type { Id } from "./posts";
import { api } from "./client";

export interface HomeData {
  me: { id: Id; name: string; generation: number; lionParts: ApiLionPart[]; headline: string | null; bio: string | null; photoUrl: string | null };
  /** 승인 후 첫 방문 환영 배너(첫 커피챗 가이드)를 보여줄지 */
  welcomeBannerVisible: boolean;
  counts: { pendingCoffeeChats: number; activeCoffeeChats: number; unreadMessages: number; messageRequests: number; bookmarks: number };
  upcomingCoffeeChat: {
    id: Id;
    scheduledAt: string;
    meetingMode: "ONLINE" | "OFFLINE";
    roomId: Id | null;
    partner: { id: Id | null; name: string | null; generation: number | null; lionParts: ApiLionPart[] | null; photoUrl: string | null; isDeleted: boolean };
  } | null;
  /** 앞 기수 + 오늘 커피챗 받는 사자 2명 (ALUMNI-01 항목 형식) */
  recommendedSeniors: AlumniSummary[];
  /** 1년 전 같은 날 쓴 글 중 조회수가 가장 많은 글 */
  onThisDay: { id: Id; category: PostCategory; title: string; thumbnailUrl: string | null; createdAt: string } | null;
}

/** HOME-01 */
export function getHome() {
  return api<HomeData>("/home");
}

/** HOME-02 환영 배너 닫기 */
export function dismissWelcome() {
  return api<{ visible: false; dismissedAt: string }>("/home/coffee-guide/dismiss", { method: "POST" });
}

export type RestFor = "WEEK_1" | "WEEK_2" | "MONTH_1" | "CUSTOM" | "UNTIL_RESUME";

export interface SwitchRequest {
  on: boolean;
  restFor?: RestFor;
  startDate?: string;
  endDate?: string;
  /** 기본 상태가 바뀌는데 등록된 기간이 있으면 true (기간이 모두 지워진다) */
  confirmClearPeriods?: boolean;
}

/** PROFILE-14 홈 스위치. 응답은 PROFILE-12 형식. 409 PERIOD_LIMIT · PERIODS_WILL_BE_CLEARED */
export function switchCoffee(body: SwitchRequest) {
  return api<CoffeeSettings>("/me/coffee-settings/switch", { method: "POST", body });
}

/** 쉬기 기간 종류 → 오늘부터 며칠 (오늘 포함) */
export const REST_DAYS: Record<"WEEK_1" | "WEEK_2" | "MONTH_1", number> = { WEEK_1: 7, WEEK_2: 14, MONTH_1: 30 };
