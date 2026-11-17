"use client";

import { useMemo } from "react";
import { useStore } from "@/lib/store";
import type { AppNotification } from "@/lib/types";
import { API_ENABLED, ApiError } from "./client";
import * as noti from "./notifications";
import type { EmailSettings, EmailType, NotificationItem, NotificationList, NotificationTab } from "./notifications";
import type { Id } from "./posts";

/**
 * 알림 드롭다운 · 알림 설정 · 수신 거부 화면이 쓰는 API 묶음 (NOTI-01 ~ 06).
 * 프로토타입 모드에서는 store의 mock 알림을 같은 응답 모양으로 바꿔 준다.
 */
export interface NotificationService {
  list: (tab?: "ALL" | Exclude<NotificationTab, "ACCOUNT">, signal?: AbortSignal) => Promise<NotificationList>;
  read: (id: Id) => Promise<void>;
  readAll: () => Promise<void>;
  getSettings: () => Promise<EmailSettings>;
  updateSettings: (patch: Partial<EmailSettings>) => Promise<EmailSettings>;
  unsubscribe: (token: string, type: EmailType) => Promise<void>;
}

const apiService: NotificationService = {
  list: noti.listNotifications,
  read: noti.readNotification,
  readAll: noti.readAllNotifications,
  getSettings: noti.getEmailSettings,
  updateSettings: noti.updateEmailSettings,
  unsubscribe: noti.unsubscribeEmail,
};

// ---------------------------------------------------------------------------
// 프로토타입 mock
// ---------------------------------------------------------------------------

const delay = <T,>(value: T) => new Promise<T>((resolve) => setTimeout(() => resolve(value), 80));

/** mock 알림은 이동 주소(href)만 갖고 있어서, 주소에서 대상을 읽어 API 모양으로 바꾼다 */
function fromMock(n: AppNotification, actorName: (id: string) => string | undefined): NotificationItem {
  const tab: NotificationTab = n.tab === "COFFEE" ? "COFFEE_CHAT" : (n.tab as NotificationTab);
  const coffee = n.href.match(/^\/coffee\?id=([^&]+)/);
  const room = n.href.match(/^\/chat\/(?!requests)([^/?]+)/);
  const post = n.href.match(/^\/community\/([^/?]+)/);
  const type = n.quick === "coffee-request" ? "COFFEE_CHAT_REQUESTED" : n.href === "/chat/requests" ? "MESSAGE_REQUESTED" : n.text.includes("수락") ? "COFFEE_CHAT_ACCEPTED" : tab === "COMMUNITY" ? "COMMENT_CREATED" : "MESSAGE";
  const [targetType, targetId]: [NotificationItem["targetType"], Id | null] = coffee ? ["COFFEE_CHAT", coffee[1]] : room ? ["ROOM", room[1]] : post ? ["POST", post[1]] : n.href.startsWith("/chat") ? ["ROOM", null] : ["USER", null];
  const name = n.actorId ? actorName(n.actorId) : undefined;
  return {
    id: n.id,
    tab,
    type,
    targetType,
    targetId,
    actor: n.actorId ? { id: n.actorId, name: name ?? null, photoUrl: null, isDeleted: !name } : null,
    message: n.text,
    read: n.read,
    createdAt: n.at,
  };
}

const MOCK_SETTING: Record<keyof EmailSettings, "emailCoffee" | "emailMessage" | "emailApproved"> = { emailCoffeeChat: "emailCoffee", emailMessageRequest: "emailMessage", emailSignupApproved: "emailApproved" };

export function useNotificationService(): NotificationService {
  const store = useStore();
  return useMemo<NotificationService>(() => {
    if (API_ENABLED) return apiService;
    const name = (id: string) => store.alumni.find((a) => a.id === id)?.name;
    const all = [...store.notifications].sort((a, b) => b.at.localeCompare(a.at)).map((n) => fromMock(n, name));
    const unread = (tab?: NotificationTab) => all.filter((n) => !n.read && (!tab || n.tab === tab)).length;
    const settings = (): EmailSettings => ({ emailCoffeeChat: store.notiSettings.emailCoffee, emailMessageRequest: store.notiSettings.emailMessage, emailSignupApproved: store.notiSettings.emailApproved });
    return {
      list: (tab = "ALL") =>
        delay({
          items: all.filter((n) => tab === "ALL" || n.tab === tab).slice(0, 20),
          unreadCounts: { all: unread(), coffeeChat: unread("COFFEE_CHAT"), chat: unread("CHAT"), community: unread("COMMUNITY") },
        }),
      read: (id) => {
        store.markNotificationsRead([String(id)]);
        return delay(undefined);
      },
      readAll: () => {
        store.markNotificationsRead();
        return delay(undefined);
      },
      getSettings: () => delay(settings()),
      updateSettings: (patch) => {
        const next = { ...settings(), ...patch };
        store.setNotiSettings(Object.fromEntries(Object.entries(patch).map(([k, v]) => [MOCK_SETTING[k as keyof EmailSettings], v])));
        return delay(next);
      },
      unsubscribe: (token, type) => {
        if (!token) return Promise.reject(new ApiError(400, "INVALID_UNSUBSCRIBE_TOKEN", "수신 거부 링크가 올바르지 않아요."));
        store.setNotiSettings({ [MOCK_SETTING[noti.EMAIL_SETTING_KEY[type]]]: false });
        return delay(undefined);
      },
    };
  }, [store]);
}
