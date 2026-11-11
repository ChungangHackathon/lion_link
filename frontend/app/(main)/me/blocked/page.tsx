"use client";

import { useStore } from "@/lib/store";
import { LION_PART_LABEL } from "@/lib/types";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Card, EmptyState } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

/** ◇ X-02 차단 목록 */
export default function BlockedPage() {
  const toast = useToast();
  const { alumni, blocked, unblock } = useStore();
  const list = blocked.map((id) => alumni.find((a) => a.id === id)!).filter(Boolean);
  return (
    <div className="flex flex-col gap-4">
      <h1 className="t-title text-ink-900">차단 목록</h1>
      <p className="text-[13px] text-ink-500">차단한 동문과는 서로 프로필·게시글이 보이지 않고 커피챗·메시지를 주고받을 수 없어요.</p>
      {list.length === 0 ? (
        <EmptyState title="차단한 동문이 없어요" />
      ) : (
        <Card className="divide-y divide-line-200">
          {list.map((a) => (
            <div key={a.id} className="flex items-center justify-between px-5 py-4">
              <div className="flex items-center gap-3">
                <Avatar name={a.name} part={a.part} size={40} />
                <div>
                  <p className="t-label1 text-ink-900">{a.name}</p>
                  <p className="text-[12px] text-ink-500">
                    {a.generation}기 · {LION_PART_LABEL[a.part]}
                  </p>
                </div>
              </div>
              <Button size="S" variant="secondary" onClick={() => { unblock(a.id); toast(`${a.name}님 차단을 해제했어요`); }}>
                차단 해제
              </Button>
            </div>
          ))}
        </Card>
      )}
    </div>
  );
}
