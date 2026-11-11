"use client";

import { useState } from "react";
import type { Alumni } from "@/lib/types";
import { CoffeeRequestModal } from "./CoffeeRequestModal";
import { MessageRequestModal } from "./MessageRequestModal";

/** Shared "커피챗 신청 / 메시지" modal state for any screen that renders AlumniCard. */
export function useAlumniActions() {
  const [request, setRequest] = useState<Alumni | null>(null);
  const [message, setMessage] = useState<Alumni | null>(null);
  const modals = (
    <>
      {request && <CoffeeRequestModal open onClose={() => setRequest(null)} target={request} />}
      {message && <MessageRequestModal open onClose={() => setMessage(null)} target={message} />}
    </>
  );
  return { onRequest: setRequest, onMessage: setMessage, modals };
}
