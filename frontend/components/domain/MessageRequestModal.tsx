"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import type { Alumni } from "@/lib/types";
import { useStore } from "@/lib/store";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Textarea } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";

/** ◇ T-04 메시지 요청 보내기 / 첫 메시지 → T-05 보냄 · 수락 대기 */
export function MessageRequestModal({ open, onClose, target }: { open: boolean; onClose: () => void; target: Alumni }) {
  const router = useRouter();
  const toast = useToast();
  const { rooms, createMessageRequest } = useStore();
  const [text, setText] = useState("");
  const existing = rooms.find((r) => r.type === "DIRECT" && r.memberIds.includes(target.id) && r.status !== "DECLINED");

  const send = () => {
    if (existing) {
      onClose();
      router.push(`/chat/${existing.id}`);
      return;
    }
    const id = createMessageRequest(target.id, text.trim());
    onClose();
    toast("메시지 요청을 보냈어요. 상대가 수락하면 대화가 열려요.");
    router.push(`/chat/${id}`);
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={existing ? "이미 대화 중이에요" : `${target.name}님에게 메시지 요청`}
      description={existing ? "채팅방으로 이동할게요." : "첫 메시지는 요청으로 전달돼요. 상대가 수락해야 대화가 이어져요."}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button onClick={send} disabled={!existing && (text.trim().length < 5 || text.length > 300)}>
            {existing ? "채팅방으로" : "요청 보내기"}
          </Button>
        </>
      }
    >
      {!existing && (
        <div className="flex flex-col gap-3">
          <div className="flex items-center gap-2 rounded-[8px] bg-bg-50 p-3">
            <Avatar name={target.name} part={target.part} size={40} />
            <div className="flex flex-col">
              <span className="t-label1 text-ink-900">{target.name}</span>
              <span className="text-[12px] text-ink-500">{target.headline}</span>
            </div>
          </div>
          <Textarea rows={4} placeholder="안녕하세요! 어떤 이야기를 나누고 싶은지 짧게 적어주세요." value={text} onChange={(e) => setText(e.target.value)} counter={{ value: text.length, max: 300 }} help="5~300자" />
        </div>
      )}
    </Modal>
  );
}
