"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { AuthShell } from "@/components/layout/AuthShell";
import { Button } from "@/components/ui/Button";
import { ConfirmModal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { API_ENABLED, GOOGLE_LOGIN_URL } from "@/lib/api/client";
import { googleErrorMessage, landingPath } from "@/lib/api/auth";
import { useSession } from "@/lib/session";

/** ★ A-10 로그인 / 기본 → A-04b(구글) → H-01. "계정에 문제가 있나요?" → CF-2 */
export default function LoginPage() {
  const router = useRouter();
  const { login, ready, me } = useSession();
  const toast = useToast();
  const [help, setHelp] = useState(false);
  const [loading, setLoading] = useState(false);

  // 구글 로그인 실패로 돌아오면 ?error={code}가 붙어 있다
  useEffect(() => {
    const code = new URLSearchParams(window.location.search).get("error");
    if (!code) return;
    toast(googleErrorMessage(code), { kind: "error" });
    router.replace("/login");
  }, [toast, router]);

  // 이미 로그인돼 있으면 상태에 맞는 화면으로 보낸다
  useEffect(() => {
    if (ready && me) router.replace(landingPath(me));
  }, [ready, me, router]);

  const google = () => {
    setLoading(true);
    if (API_ENABLED) {
      // AUTH-01: 서버가 구글 로그인 화면으로 보내고, 끝나면 /auth/callback 또는 /signup으로 돌아온다
      window.location.href = GOOGLE_LOGIN_URL;
      return;
    }
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
        <p className="t-body-bold text-ink-500">1기부터 지금까지, 멋사 중앙대 사자와 사자를 잇는 동문 네트워크</p>
      </div>
      <Button size="L" variant="secondary" full loading={loading} onClick={google} className="border-ink-500" leading={<GoogleMark />}>
        구글로 계속하기
      </Button>
      <hr className="border-line-200" />
      <p className="t-label2 text-center text-ink-500">멋사 중앙대 동문 네트워크예요. 처음이면 구글 계정으로 가입 신청하고, 운영진 승인 후 이용할 수 있어요.</p>
      <div className="flex justify-center">
        <button type="button" onClick={() => setHelp(true)} className="t-label2 text-ink-700 underline underline-offset-2">
          계정에 문제가 있나요?
        </button>
      </div>

      {/* 프로토타입 모드(NEXT_PUBLIC_API_URL 없음) 전용: 역할 바꿔 로그인 */}
      {!API_ENABLED && (
        <div className="mt-2 flex flex-wrap justify-center gap-2 border-t border-dashed border-line-300 pt-4">
          <DemoLogin label="승인 대기 회원으로" onClick={() => { login({ status: "PENDING" }); router.push("/pending"); }} />
          <DemoLogin label="관리자(정운영)로" onClick={() => { login({ role: "ADMIN" }); router.push("/admin"); }} />
          <DemoLogin label="가입 신청 화면 보기" onClick={() => router.push("/signup")} />
        </div>
      )}

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
