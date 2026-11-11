"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { AuthShell } from "@/components/layout/AuthShell";
import { Button } from "@/components/ui/Button";
import { ConfirmModal } from "@/components/ui/Modal";
import { useSession } from "@/lib/session";

/** ★ A-10 로그인 / 기본 → A-04b(구글) → H-01. "계정에 문제가 있나요?" → CF-2 */
export default function LoginPage() {
  const router = useRouter();
  const { login } = useSession();
  const [help, setHelp] = useState(false);
  const [loading, setLoading] = useState(false);

  const google = () => {
    setLoading(true);
    // A-04b: 구글 계정 선택 (구글 화면 · 디자인 대상 아님) → 로그인 완료
    setTimeout(() => {
      login({ role: "MEMBER", status: "APPROVED" });
      router.push("/home");
    }, 500);
  };

  return (
    <AuthShell>
      <div className="flex flex-col items-center gap-2 pb-4 pt-2 text-center">
        <h1 className="t-title text-ink-900">LION:LINK</h1>
        <p className="t-body-bold text-ink-500">1기부터 지금까지, 멋사 중앙대 선배와 후배를 잇는 동문 네트워크</p>
      </div>
      <Button size="L" variant="secondary" full loading={loading} onClick={google} className="border-ink-500" leading={<GoogleMark />}>
        구글로 로그인
      </Button>
      <hr className="border-line-200" />
      <p className="t-label2 text-center text-ink-500">초대 링크로만 가입할 수 있어요. 링크가 없다면 운영진에게 문의해주세요.</p>
      <div className="flex justify-center">
        <button type="button" onClick={() => setHelp(true)} className="t-label2 text-ink-700 underline underline-offset-2">
          계정에 문제가 있나요?
        </button>
      </div>

      {/* 데모 전용: 역할 바꿔 로그인 */}
      <div className="mt-2 flex flex-wrap justify-center gap-2 border-t border-dashed border-line-300 pt-4">
        <DemoLogin label="승인 대기 회원으로" onClick={() => { login({ status: "PENDING" }); router.push("/pending"); }} />
        <DemoLogin label="관리자(정운영)로" onClick={() => { login({ role: "ADMIN" }); router.push("/admin"); }} />
        <DemoLogin label="초대 링크 가입 보기" onClick={() => router.push("/invite/abc123")} />
      </div>

      <ConfirmModal
        open={help}
        onClose={() => setHelp(false)}
        onConfirm={() => setHelp(false)}
        title="계정에 문제가 있나요?"
        description="가입한 구글 계정을 잊었거나 로그인이 되지 않으면 운영진(likelion@cau.ac.kr)에게 메일로 문의해주세요. 보통 1~2일 안에 답장드려요."
        confirmLabel="확인"
        cancelLabel="닫기"
      />
    </AuthShell>
  );
}

function DemoLogin({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button type="button" onClick={onClick} className="rounded-[8px] border border-dashed border-ink-400 px-3 py-2 text-[12px] text-ink-500 hover:bg-bg-100">
      ▶ 프로토타입: {label}
    </button>
  );
}

export function GoogleMark({ muted }: { muted?: boolean }) {
  return <span className={`grid size-6 place-items-center rounded-full border text-[13px] font-bold ${muted ? "border-line-300 text-ink-400" : "border-ink-500 text-ink-700"}`}>G</span>;
}
