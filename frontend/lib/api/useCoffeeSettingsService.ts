"use client";

import { useMemo } from "react";
import { useStore, type CoffeeSettings as MockSettings } from "@/lib/store";
import { API_ENABLED, ApiError } from "./client";
import type { CoffeeTopic, TimeSlot, Weekday } from "./alumni";
import { COFFEE_TOPIC_LABEL } from "./coffee";
import * as api from "./coffeeSettings";
import type { CoffeePeriod, CoffeeSettings, CoffeeSettingsInput } from "./coffeeSettings";
import { addDays, todayKst } from "./coffeeSettings";

/**
 * 커피챗 받기 설정 화면이 쓰는 API 묶음.
 * API 모드에서는 실제 서버, 프로토타입 모드에서는 store의 mock 설정을 같은 응답 모양으로 바꿔 준다.
 */
export interface CoffeeSettingsService {
  get: () => Promise<CoffeeSettings>;
  save: (body: CoffeeSettingsInput) => Promise<CoffeeSettings>;
  addPeriod: (body: { startDate: string; endDate: string }) => Promise<CoffeePeriod>;
  removePeriod: (id: CoffeePeriod["id"]) => Promise<void>;
}

const apiService: CoffeeSettingsService = {
  get: api.getCoffeeSettings,
  save: api.saveCoffeeSettings,
  addPeriod: api.addPeriod,
  removePeriod: api.removePeriod,
};

// ---------------------------------------------------------------------------
// 프로토타입 mock
// ---------------------------------------------------------------------------

const delay = <T,>(value: T) => new Promise<T>((resolve) => setTimeout(() => resolve(value), 120));
const LABEL_TO_TOPIC = Object.fromEntries(Object.entries(COFFEE_TOPIC_LABEL).map(([code, label]) => [label, code])) as Record<string, CoffeeTopic>;
const DAY_LABEL: Record<Weekday, string> = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금", SAT: "토", SUN: "일" };
const TIME_LABEL: Record<TimeSlot, string> = { MORNING: "오전", AFTERNOON: "오후", EVENING: "저녁" };

/** mock store에는 시간대 문구만 있어서, 저장한 칸은 여기에 둔다 */
let mockSlots: CoffeeSettings["availableSlots"] = [
  ...(["MON", "TUE", "WED", "THU", "FRI"] as Weekday[]).map((day) => ({ day, times: ["EVENING" as TimeSlot] })),
  { day: "SAT", times: ["MORNING", "AFTERNOON"] },
  { day: "SUN", times: ["AFTERNOON"] },
];

function summarize(slots: CoffeeSettings["availableSlots"]): string {
  return slots.map((s) => `${DAY_LABEL[s.day]} ${s.times.map((t) => TIME_LABEL[t]).join("·")}`).join(", ");
}

function fromMock(s: MockSettings): CoffeeSettings {
  const defaultMode = s.mode === "CLOSED" || s.mode === "SCHEDULED" ? "OFF" : "ON";
  const periodKind = defaultMode === "ON" ? "REST" : "OPEN";
  const periods = s.periods.filter((p) => p.kind === periodKind && p.to >= todayKst()).map((p) => ({ id: p.id, startDate: p.from, endDate: p.to }));
  const today = todayKst();
  const active = periods.find((p) => p.startDate <= today && today <= p.endDate);
  const next = [...periods].sort((a, b) => a.startDate.localeCompare(b.startDate)).find((p) => p.startDate > today);
  const status: CoffeeSettings["status"] =
    defaultMode === "ON"
      ? active
        ? { availability: "RESTING", nextAvailableDate: addDays(active.endDate, 1) }
        : { availability: "AVAILABLE", nextAvailableDate: null }
      : active
        ? { availability: "AVAILABLE", nextAvailableDate: null }
        : { availability: "NOT_ACCEPTING", nextAvailableDate: next?.startDate ?? null };
  return {
    defaultMode,
    acceptGroup: s.allowGroup,
    topics: s.keywords.map((k) => LABEL_TO_TOPIC[k]).filter((t): t is CoffeeTopic => !!t),
    modes: s.modes,
    availableSlots: mockSlots,
    preferredArea: s.regions || null,
    periodKind,
    periods,
    status,
  };
}

export function useCoffeeSettingsService(): CoffeeSettingsService {
  const store = useStore();
  return useMemo<CoffeeSettingsService>(() => {
    if (API_ENABLED) return apiService;
    const current = fromMock(store.settings);
    const write = (patch: Partial<MockSettings>) => {
      const next = { ...store.settings, ...patch };
      const view = fromMock(next);
      const mode = view.status.availability === "AVAILABLE" ? "OPEN" : view.status.availability === "RESTING" ? "RESTING" : view.status.nextAvailableDate ? "SCHEDULED" : "CLOSED";
      store.setSettings({ ...patch, mode });
      return { ...view, status: view.status };
    };
    return {
      get: () => delay(current),
      save: (body) => {
        const changing = body.defaultMode !== current.defaultMode;
        if (changing && current.periods.length && !body.confirmClearPeriods) {
          return Promise.reject(new ApiError(409, "PERIODS_WILL_BE_CLEARED", "등록한 기간이 모두 지워져요. 바꿀까요?"));
        }
        if (body.availableSlots) mockSlots = body.availableSlots;
        return delay(
          write({
            mode: body.defaultMode === "ON" ? "OPEN" : "CLOSED",
            allowGroup: body.acceptGroup,
            keywords: (body.topics ?? []).map((t) => COFFEE_TOPIC_LABEL[t]),
            modes: body.modes,
            regions: body.preferredArea ?? "",
            times: summarize(mockSlots),
            periods: changing ? [] : store.settings.periods,
          }),
        );
      },
      addPeriod: ({ startDate, endDate }) => {
        if (current.periods.length >= api.PERIOD_LIMIT) return Promise.reject(new ApiError(409, "PERIOD_LIMIT", "기간은 5개까지 등록할 수 있어요."));
        const id = Math.random().toString(36).slice(2);
        write({ periods: [...store.settings.periods, { id, from: startDate, to: endDate, kind: current.periodKind }] });
        return delay({ id, startDate, endDate });
      },
      removePeriod: (id) => {
        write({ periods: store.settings.periods.filter((p) => p.id !== String(id)) });
        return delay(undefined);
      },
    };
  }, [store]);
}
