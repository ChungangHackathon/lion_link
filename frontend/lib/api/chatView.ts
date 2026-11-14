/** 채팅 화면용 작은 변환 함수들 */

import type { LionPart } from "@/lib/types";
import { userName, type ChatUser } from "./chat";

export const partOf = (u: ChatUser | null | undefined): LionPart => (u?.lionParts?.[0] as LionPart | undefined) ?? "ETC";
/** AvatarGroup에 넣을 모양 */
export const avatarPeople = (users: ChatUser[]) => users.map((u) => ({ name: userName(u), part: partOf(u) }));
/** 재전송 중복 방지용 id (CHAT-11 clientMsgId) */
export const newClientId = () => (typeof crypto !== "undefined" && "randomUUID" in crypto ? crypto.randomUUID() : `c-${Date.now()}-${Math.random().toString(36).slice(2)}`);

/** 내 회원 id. API 모드는 GET /me의 id, 프로토타입 모드는 mock '나' */
export const myIdOf = (me: { id: number | string } | null | undefined, mockId: string) => (me ? String(me.id) : mockId);
