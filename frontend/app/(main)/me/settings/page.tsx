"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useSession } from "@/lib/session";
import { API_ENABLED, errorMessage } from "@/lib/api/client";
import { withdraw } from "@/lib/api/auth";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Card } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

/** "2026-10-21T…" → "2026년 10월 21일" (한국 시간) */
const joinedDate = (iso: string) => new Date(iso).toLocaleDateString("ko-KR", { year: "numeric", month: "long", day: "numeric", timeZone: "Asia/Seoul" });

/** ◇ X-03 설정 / 기본 · ◇[O] CF-3 탈퇴 확인 (AUTH-08: '탈퇴'를 직접 입력해 본인 의사 확인) → 로그인 */
export default function SettingsPage() {
  const router = useRouter();
  const toast = useToast();
  const { session, me, logout } = useSession();
  const [confirm, setConfirm] = useState(false);
  const [typed, setTyped] = useState("");
  const [busy, setBusy] = useState(false);
  const isAdmin = API_ENABLED ? me?.role === "ADMIN" : session.role === "ADMIN";

  const doWithdraw = async () => {
    setBusy(true);
    try {
      if (API_ENABLED) await withdraw(typed);
      await logout();
      router.push("/login");
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
      setBusy(false);
    }
  };
  return (
    <div className="flex flex-col gap-6">
      <h1 className="t-title text-ink-900">설정</h1>
      <Card className="flex flex-col divide-y divide-line-200 px-6">
        <div className="flex items-center justify-between py-4 text-[14px]">
          <span className="text-ink-500">로그인 계정</span>
          <span className="text-ink-900">{me?.email ? `${me.email} (구글)` : "—"}</span>
        </div>
        <div className="flex items-center justify-between py-4 text-[14px]">
          <span className="text-ink-500">가입일</span>
          <span className="text-ink-900">{me?.createdAt ? joinedDate(me.createdAt) : "—"}</span>
        </div>
      </Card>
      <div>
        {isAdmin ? (
          <p className="text-[13px] text-ink-500">관리자 계정은 탈퇴할 수 없어요. 다른 운영진에게 관리자 권한을 넘긴 뒤 탈퇴해주세요.</p>
        ) : (
          <Button variant="danger" onClick={() => setConfirm(true)}>
            탈퇴하기
          </Button>
        )}
      </div>

      <Modal
        open={confirm}
        onClose={() => {
          setConfirm(false);
          setTyped("");
        }}
        title="탈퇴하면 되돌릴 수 없어요"
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirm(false)}>
              취소
            </Button>
            <Button variant="danger" disabled={typed !== "탈퇴"} loading={busy} onClick={() => void doWithdraw()}>
              탈퇴하기
            </Button>
          </>
        }
      >
        <ul className="list-disc pl-5 text-[14px] leading-7 text-ink-700">
          <li>진행 중인 커피챗은 취소돼요</li>
          <li>내가 만든 모집 중인 번개·모집글은 종료돼요</li>
          <li>글·댓글은 ‘탈퇴한 사자’로 남아요</li>
        </ul>
        <Input className="mt-4" label="확인을 위해 '탈퇴'를 입력해주세요" placeholder="탈퇴" value={typed} onChange={(e) => setTyped(e.target.value)} />
      </Modal>
    </div>
  );
}
