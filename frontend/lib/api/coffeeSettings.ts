/** 커피챗 받기 설정 (docs/api.md PROFILE-12 · 13 · 15) */

import type { CoffeeTopic, Availability, TimeSlot, Weekday } from "./alumni";
import { api } from "./client";

export type DefaultMode = "ON" | "OFF";
export type MeetingWay = "ONLINE" | "OFFLINE";

export interface CoffeePeriod {
  id: number | string;
  startDate: string;
  endDate: string;
}

export interface CoffeeSettings {
  /** ON 받기, OFF 안 받기 */
  defaultMode: DefaultMode;
  acceptGroup: boolean;
  topics: CoffeeTopic[];
  modes: MeetingWay[];
  availableSlots: { day: Weekday; times: TimeSlot[] }[];
  preferredArea: string | null;
  /** 기본이 ON이면 REST(쉬는 기간), OFF면 OPEN(받는 기간) */
  periodKind: "REST" | "OPEN";
  periods: CoffeePeriod[];
  /** 오늘 기준 상태 */
  status: { availability: Availability; nextAvailableDate: string | null };
}

export interface CoffeeSettingsInput {
  defaultMode: DefaultMode;
  acceptGroup: boolean;
  topics?: CoffeeTopic[];
  modes: MeetingWay[];
  availableSlots?: { day: Weekday; times: TimeSlot[] }[];
  preferredArea?: string;
  /** 기본 상태를 바꾸는데 등록된 기간이 있으면 true여야 한다 (기간이 모두 지워진다) */
  confirmClearPeriods?: boolean;
}

/** PROFILE-12 조회 */
export function getCoffeeSettings() {
  return api<CoffeeSettings>("/me/coffee-settings");
}

/** PROFILE-13 저장. 기간이 있는데 기본 상태를 바꾸면 409 PERIODS_WILL_BE_CLEARED */
export function saveCoffeeSettings(body: CoffeeSettingsInput) {
  return api<CoffeeSettings>("/me/coffee-settings", { method: "PUT", body });
}

/** PROFILE-15 기간 추가 */
export function addPeriod(body: { startDate: string; endDate: string }) {
  return api<CoffeePeriod>("/me/coffee-periods", { method: "POST", body });
}

/**
 * PROFILE-15 기간 수정. 진행 중인 기간은 종료일만 바꿀 수 있고 시작일은 원래 값을 그대로 보낸다.
 * 이미 끝난 기간은 404, 다른 기간과 겹치면 409 PERIOD_OVERLAP
 */
export function updatePeriod(id: CoffeePeriod["id"], body: { startDate: string; endDate: string }) {
  return api<CoffeePeriod>(`/me/coffee-periods/${id}`, { method: "PUT", body });
}

/** PROFILE-15 기간 삭제 */
export function removePeriod(id: CoffeePeriod["id"]) {
  return api(`/me/coffee-periods/${id}`, { method: "DELETE" });
}

export const PERIOD_LIMIT = 5;

/** "YYYY-MM-DD" 날짜 계산 (KST 기준 오늘) */
export const todayKst = () => new Date(Date.now() + 9 * 3_600_000).toISOString().slice(0, 10);
export function addDays(date: string, days: number): string {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

/** 이미 시작한(진행 중인) 기간인지. 이런 기간은 종료일만 바꿀 수 있다 */
export const periodStarted = (p: CoffeePeriod) => p.startDate <= todayKst();

/** PROFILE-15 검증과 같은 규칙. 문제가 없으면 null. editing이면 그 기간은 겹침 검사에서 빼고, 진행 중이면 시작일 검사 대신 종료일만 본다 */
export function periodError(start: string, end: string, existing: CoffeePeriod[], editing?: CoffeePeriod): string | null {
  const today = todayKst();
  if (editing) existing = existing.filter((e) => String(e.id) !== String(editing.id));
  if (editing && periodStarted(editing)) {
    if (end < today) return "종료일은 오늘부터 고를 수 있어요";
  } else if (start < today) return "시작일은 오늘 이후여야 해요";
  if (start > addDays(today, 365)) return "1년 이내로 정해주세요";
  if (end < start) return "종료일이 시작일보다 빨라요";
  if (end > addDays(start, 179)) return "한 번에 최대 180일까지 정할 수 있어요";
  if (existing.some((e) => start <= e.endDate && e.startDate <= end)) return "이미 등록한 기간과 겹쳐요";
  return null;
}
