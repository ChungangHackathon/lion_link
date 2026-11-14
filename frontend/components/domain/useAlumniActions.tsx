"use client";

import { useState } from "react";
import type { LionPart } from "@/lib/types";
import type { AlumniSummary } from "@/lib/api/alumni";
import type { Id } from "@/lib/api/posts";
import { CoffeeRequestModal } from "./CoffeeRequestModal";
import { MessageRequestModal, type MessageTarget } from "./MessageRequestModal";

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
  const onMessage = (p: AlumniSummary) => setMessage({ id: p.id, name: p.name, generation: p.generation, part: (p.lionParts[0] as LionPart | undefined) ?? "ETC", headline: p.headline });
  return { onRequest, onMessage, modals };
}
