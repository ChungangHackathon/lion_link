"use client";

/**
 * 채팅 실시간 연결 (docs/api.md 10장 "실시간 (WebSocket STOMP)").
 * - 연결: ws(s)://{서버}/ws, CONNECT 헤더 Authorization: Bearer {accessToken}
 * - 방 채널 /topic/chat-rooms/{roomId}: MESSAGE · READ
 * - 개인 채널 /user/queue/chat: ROOM_UPDATED · REQUEST_RECEIVED
 * 보내기는 REST(CHAT-11)로 한다. 연결이 안 되면 화면들은 예전처럼 폴링으로 버틴다.
 */

import { Client, type StompSubscription } from "@stomp/stompjs";
import { createContext, useContext, useEffect, useRef, useState } from "react";
import { API_ENABLED, refreshAccessToken, tokenStore } from "./client";
import type { ChatMessage } from "./chat";
import type { Id } from "./posts";

export type RoomEvent = { type: "MESSAGE"; roomId: Id; message: ChatMessage } | { type: "READ"; roomId: Id; userId: Id; lastReadMessageId: Id };
export type PersonalEvent = { type: "ROOM_UPDATED"; roomId: Id; muted: boolean } | { type: "REQUEST_RECEIVED"; roomId: Id };

interface Realtime {
  /** 지금 연결돼 있는지. false면 화면이 폴링으로 새 소식을 확인한다 */
  connected: boolean;
  /** 방 채널 구독. 연결이 끊겼다 붙으면 자동으로 다시 구독한다 */
  subscribeRoom: (roomId: Id, handler: (e: RoomEvent) => void) => () => void;
  /** 개인 채널 이벤트 */
  onPersonal: (handler: (e: PersonalEvent) => void) => () => void;
  /** 끊겼다가 다시 연결됐을 때 (놓친 메시지를 after로 받아오는 데 쓴다) */
  onReconnect: (handler: () => void) => () => void;
  /**
   * 내가 읽거나 방을 나가서 안 읽은 수가 바뀌었다고 알린다.
   * 읽음(CHAT-12)은 서버가 방 채널로만 보내서 개인 채널 소식이 오지 않으므로 화면이 직접 알린다.
   */
  countsChanged: () => void;
  /** countsChanged를 받는다 (상단 메뉴 '채팅' 숫자) */
  onCountsChanged: (handler: () => void) => () => void;
}

const noop = () => () => undefined;
const OFF: Realtime = { connected: false, subscribeRoom: noop, onPersonal: noop, onReconnect: noop, countsChanged: () => undefined, onCountsChanged: noop };
const RealtimeContext = createContext<Realtime>(OFF);
export const useChatRealtime = () => useContext(RealtimeContext);

/** NEXT_PUBLIC_API_URL(http://…) → ws://…/ws */
const WS_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "").replace(/^http/i, "ws") + "/ws";
const USER_QUEUE = "/user/queue/chat";
const roomTopic = (roomId: Id) => `/topic/chat-rooms/${roomId}`;

function parse<T>(body: string): T | null {
  try {
    return JSON.parse(body) as T;
  } catch {
    return null;
  }
}

/** 승인 회원 화면 전체를 감싼다. enabled가 false거나 프로토타입 모드면 연결하지 않는다 */
export function ChatRealtimeProvider({ enabled, children }: { enabled: boolean; children: React.ReactNode }) {
  const [connected, setConnected] = useState(false);
  const client = useRef<Client | null>(null);
  const rooms = useRef(new Map<string, { handlers: Set<(e: RoomEvent) => void>; sub: StompSubscription | null }>());
  const personal = useRef(new Set<(e: PersonalEvent) => void>());
  const reconnects = useRef(new Set<() => void>());
  const counts = useRef(new Set<() => void>());
  const [api] = useState<Omit<Realtime, "connected">>(() => ({
    subscribeRoom: (roomId, handler) => {
      const key = String(roomId);
      let entry = rooms.current.get(key);
      if (!entry) {
        entry = { handlers: new Set(), sub: null };
        rooms.current.set(key, entry);
      }
      entry.handlers.add(handler);
      const c = client.current;
      if (c?.connected && !entry.sub) entry.sub = c.subscribe(roomTopic(key), (f) => dispatchRoom(key, f.body));
      return () => {
        const e = rooms.current.get(key);
        if (!e) return;
        e.handlers.delete(handler);
        if (e.handlers.size === 0) {
          try {
            e.sub?.unsubscribe();
          } catch {
            // 이미 끊긴 연결
          }
          rooms.current.delete(key);
        }
      };
    },
    onPersonal: (handler) => {
      personal.current.add(handler);
      return () => personal.current.delete(handler);
    },
    onReconnect: (handler) => {
      reconnects.current.add(handler);
      return () => reconnects.current.delete(handler);
    },
    countsChanged: () => counts.current.forEach((h) => h()),
    onCountsChanged: (handler) => {
      counts.current.add(handler);
      return () => counts.current.delete(handler);
    },
  }));

  function dispatchRoom(key: string, body: string) {
    const e = parse<RoomEvent>(body);
    if (e) rooms.current.get(key)?.handlers.forEach((h) => h(e));
  }

  useEffect(() => {
    if (!API_ENABLED || !enabled) return;
    let everConnected = false;
    const c = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 3_000,
      heartbeatIncoming: 0,
      heartbeatOutgoing: 0,
      // 연결할 때마다 최신 accessToken을 싣는다 (만료됐으면 refreshToken 쿠키로 다시 받는다)
      beforeConnect: async (self) => {
        const token = tokenStore.get() ?? (await refreshAccessToken().catch(() => null));
        self.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
      },
      onConnect: () => {
        setConnected(true);
        c.subscribe(USER_QUEUE, (f) => {
          const e = parse<PersonalEvent>(f.body);
          if (e) personal.current.forEach((h) => h(e));
        });
        rooms.current.forEach((entry, key) => {
          entry.sub = c.subscribe(roomTopic(key), (f) => dispatchRoom(key, f.body));
        });
        if (everConnected) reconnects.current.forEach((h) => h());
        everConnected = true;
      },
      onWebSocketClose: () => {
        setConnected(false);
        rooms.current.forEach((entry) => (entry.sub = null));
      },
      // 토큰이 만료돼 거절되면 다음 재연결 때 새 토큰을 받도록 비운다
      onStompError: () => {
        setConnected(false);
        void refreshAccessToken().catch(() => null);
      },
    });
    client.current = c;
    c.activate();
    return () => {
      client.current = null;
      setConnected(false);
      void c.deactivate();
    };
  }, [enabled]);

  return <RealtimeContext.Provider value={{ connected, ...api }}>{children}</RealtimeContext.Provider>;
}

/** 개인 채널 이벤트를 받는 훅 (handler는 최신 것을 쓴다) */
export function usePersonalEvents(handler: (e: PersonalEvent) => void) {
  const { onPersonal } = useChatRealtime();
  const ref = useRef(handler);
  useEffect(() => {
    ref.current = handler;
  });
  useEffect(() => onPersonal((e) => ref.current(e)), [onPersonal]);
}
