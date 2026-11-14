"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Gnb } from "@/components/layout/Gnb";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { ConfirmModal } from "@/components/ui/Modal";
import { useSession } from "@/lib/session";

/** ◇ A-14 반려 안내 / 기본 — [다시 신청하기] → A-01, [로그아웃], [탈퇴하기] → CF-3 */
export default function RejectedPage() {
  const router = useRouter();
  const { logout } = useSession();
  const [leave, setLeave] = useState(false);
  const out = () => {
    logout();
    router.push("/login");
  };
  return (
    <div className="flex min-h-full flex-1 flex-col">
      <Gnb variant="pending" />
      <main className="flex flex-1 justify-center px-4 pt-[150px]">
        <div className="flex w-full max-w-[480px] flex-col items-center gap-5 text-center">
          <span className="grid size-20 place-items-center rounded-full bg-bg-100 text-ink-500">
            <Icon name="alert" size={34} />
          </span>
          <h1 className="text-[22px] font-bold text-ink-900">가입이 승인되지 않았어요</h1>
          <p className="text-[15px] leading-6 text-ink-500">사유: 동문인지 확인할 수 없어요</p>
          <p className="text-[15px] leading-6 text-ink-500">문제가 있다면 운영진에게 문의해주세요</p>
          <div className="flex gap-2 pt-1">
            <Button size="L" href="/signup">
              다시 신청하기
            </Button>
            <Button size="L" variant="secondary" onClick={out}>
              로그아웃
            </Button>
            <Button size="L" variant="danger" onClick={() => setLeave(true)}>
              탈퇴하기
            </Button>
          </div>
        </div>
      </main>
      <ConfirmModal open={leave} onClose={() => setLeave(false)} onConfirm={out} title="정말 탈퇴할까요?" description="가입 신청 정보가 모두 삭제돼요." confirmLabel="탈퇴하기" danger />
    </div>
  );
}
