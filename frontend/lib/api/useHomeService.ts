"use client";

import { useMemo } from "react";
import { useStore } from "@/lib/store";
import { useSession } from "@/lib/session";
import { ME_ID } from "@/lib/mock/users";
import { API_ENABLED } from "./client";
import { dismissWelcome, getHome, type HomeData } from "./home";
import { summaryFromMock } from "./mock/alumni";
import type { ApiLionPart } from "./profile";

/**
 * 홈 화면이 쓰는 API 묶음 (HOME-01 · 02).
 * 프로토타입 모드에서는 store의 mock 데이터를 HOME-01 응답 모양으로 바꿔 준다.
 */
export interface HomeService {
  get: () => Promise<HomeData>;
  dismissWelcome: () => Promise<void>;
}

const apiService: HomeService = {
  get: getHome,
  dismissWelcome: async () => {
    await dismissWelcome();
  },
};

const delay = <T,>(value: T) => new Promise<T>((resolve) => setTimeout(() => resolve(value), 120));
const parts = (p: string): ApiLionPart[] => (p === "ETC" ? [] : [p as ApiLionPart]);

export function useHomeService(): HomeService {
  const store = useStore();
  const { session, update } = useSession();
  const firstVisit = session.firstVisit;
  return useMemo<HomeService>(() => {
    if (API_ENABLED) return apiService;
    const { alumni, coffeeChats, rooms, posts, bookmarks } = store;
    const me = alumni.find((a) => a.id === ME_ID)!;
    const mine = (c: (typeof coffeeChats)[number]) => c.receiverId === ME_ID || c.requesterId === ME_ID || c.memberIds.includes(ME_ID);
    const upcoming = coffeeChats
      .filter((c) => c.status === "SCHEDULED" && c.scheduledAt && mine(c) && c.scheduledAt >= new Date().toISOString())
      .sort((a, b) => a.scheduledAt!.localeCompare(b.scheduledAt!))[0];
    const partner = upcoming && alumni.find((a) => a.id === (upcoming.receiverId === ME_ID ? upcoming.requesterId : upcoming.receiverId));
    const seniors = alumni
      .filter((a) => a.id !== ME_ID && a.generation < me.generation && a.receive === "OPEN")
      .sort((a, b) => Number(b.part === me.part) - Number(a.part === me.part) || b.generation - a.generation)
      .slice(0, 2);
    const old = posts.find((p) => p.id === "p-5");
    const data: HomeData = {
      me: { id: me.id, name: me.name, generation: me.generation, lionParts: (me.parts ?? [me.part]).filter((p): p is ApiLionPart => p !== "ETC"), headline: me.headline || null, bio: me.intro || null, photoUrl: null },
      welcomeBannerVisible: firstVisit,
      counts: {
        pendingCoffeeChats: coffeeChats.filter((c) => c.receiverId === ME_ID && c.status === "PENDING").length,
        activeCoffeeChats: coffeeChats.filter((c) => (c.status === "ACCEPTED" || c.status === "SCHEDULED") && mine(c)).length,
        unreadMessages: rooms.reduce((n, r) => n + r.unread, 0),
        messageRequests: rooms.filter((r) => r.isRequest && r.status === "REQUESTED" && r.memberIds[0] !== ME_ID).length,
        bookmarks: bookmarks.length,
      },
      upcomingCoffeeChat: upcoming
        ? {
            id: upcoming.id,
            scheduledAt: upcoming.scheduledAt!,
            meetingMode: upcoming.mode === "OFFLINE" ? "OFFLINE" : "ONLINE",
            roomId: upcoming.roomId ?? null,
            partner: partner
              ? { id: partner.id, name: partner.name, generation: partner.generation, lionParts: parts(partner.part), photoUrl: null, isDeleted: false }
              : { id: null, name: null, generation: null, lionParts: null, photoUrl: null, isDeleted: true },
          }
        : null,
      recommendedSeniors: seniors.map((a) => summaryFromMock(a, bookmarks)),
      onThisDay: old ? { id: old.id, category: old.category, title: "13기 중커톤 시상식 후기", thumbnailUrl: null, createdAt: "2025-11-14T15:00:00+09:00" } : null,
    };
    return {
      get: () => delay(structuredClone(data)),
      dismissWelcome: () => {
        update({ firstVisit: false });
        return delay(undefined);
      },
    };
  }, [store, firstVisit, update]);
}
