"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import type { LionPart } from "@/lib/types";
import { errorMessage } from "@/lib/api/client";
import type { Id } from "@/lib/api/posts";
import { useChatService } from "@/lib/api/useChatService";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Textarea } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";

export interface MessageTarget {
  id: Id;
  name: string;
  generation?: number | null;
  part?: LionPart;
  headline?: string | null;
}

/**
 * ◇ T-04 메시지 요청 보내기 / 첫 메시지 → T-05 보냄 · 수락 대기 (CHAT-01).
 * 이미 대화 중이거나 요청이 오가는 중이면 서버가 그 방을 돌려주고, 그 방으로 이동한다.
 */
export function MessageRequestModal({ open, onClose, target }: { open: boolean; onClose: () => void; target: MessageTarget }) {
  const router = useRouter();
  const toast = useToast();
  const service = useChatService();
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);
  const len = text.trim().length;

  const send = async () => {
    setBusy(true);
    try {
      const res = await service.sendRequest(target.id, text.trim());
      onClose();
      toast(res.created ? "메시지 요청을 보냈어요. 상대가 수락하면 대화가 열려요." : res.status === "ACTIVE" ? "이미 대화 중이에요. 채팅방으로 이동할게요." : "이미 요청이 오가는 중이에요. 채팅방으로 이동할게요.");
      // 상대가 먼저 보낸 요청이면 요청함에서 수락·거절한다
      router.push(res.status === "REQUESTED" && !res.requestedByMe ? `/chat/requests?id=${res.roomId}` : `/chat/${res.roomId}`);
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`${target.name}님에게 메시지 요청`}
      description="첫 메시지는 요청으로 전달돼요. 상대가 수락해야 대화가 이어져요."
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button onClick={() => void send()} loading={busy} disabled={len < 5 || len > 300}>
            요청 보내기
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <div className="flex items-center gap-2 rounded-[8px] bg-bg-50 p-3">
          <Avatar name={target.name} part={target.part} size={40} />
          <div className="flex flex-col">
            <span className="t-label1 text-ink-900">
              {target.name}
              {target.generation ? <span className="ml-1.5 text-[12px] font-normal text-ink-500">{target.generation}기</span> : null}
            </span>
            {target.headline && <span className="text-[12px] text-ink-500">{target.headline}</span>}
          </div>
        </div>
        <Textarea aria-label="첫 메시지" rows={4} placeholder="안녕하세요! 어떤 이야기를 나누고 싶은지 짧게 적어주세요." value={text} onChange={(e) => setText(e.target.value)} counter={{ value: len, max: 300 }} help="5~300자" />
      </div>
    </Modal>
  );
}
