"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { errorMessage } from "@/lib/api/client";
import { useChatService } from "@/lib/api/useChatService";
import { useToast } from "@/components/ui/Toast";
import type { LionPart } from "@/lib/types";
import type { AlumniSummary } from "@/lib/api/alumni";
import type { Id } from "@/lib/api/posts";
import { CoffeeRequestModal } from "./CoffeeRequestModal";
import { MessageRequestModal, type MessageTarget } from "./MessageRequestModal";

/**
 * 메시지 버튼: 그 동문과 이미 쓰고 있는 1:1 방이 있으면 첫 메시지 없이 바로 그 방으로, 없으면 메시지 요청 창.
 * 동문 카드와 동문 상세가 같이 쓴다.
 */
export function useOpenMessage(openRequest: (target: MessageTarget) => void) {
  const router = useRouter();
  const toast = useToast();
  const chatService = useChatService();
  return async (target: MessageTarget) => {
    try {
      const roomId = await chatService.findDirect(target.id);
      if (roomId != null) return router.push(`/chat/${roomId}`);
      openRequest(target);
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    }
  };
}

/** Shared "커피챗 신청 / 메시지 요청" modal state for any screen that renders AlumniCard. */
export function useAlumniActions() {
  const [request, setRequest] = useState<{ id: Id; name: string } | null>(null);
  const [message, setMessage] = useState<MessageTarget | null>(null);
  const modals = (
    <>
      {request && <CoffeeRequestModal open onClose={() => setRequest(null)} target={request} />}
      {message && <MessageRequestModal open onClose={() => setMessage(null)} target={message} />}
    </>
  );
  const onRequest = (p: AlumniSummary) => setRequest({ id: p.id, name: p.name });
  const openMessage = useOpenMessage(setMessage);
  const onMessage = (p: AlumniSummary) => void openMessage({ id: p.id, name: p.name, generation: p.generation, part: (p.lionParts[0] as LionPart | undefined) ?? "ETC", headline: p.headline });
  return { onRequest, onMessage, modals };
}
