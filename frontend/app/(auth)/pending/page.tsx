"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Gnb } from "@/components/layout/Gnb";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { ProgressBar, ProtoButton } from "@/components/ui/Misc";
import { useSession } from "@/lib/session";
import { API_ENABLED } from "@/lib/api/client";
import { landingPath } from "@/lib/api/auth";

/** ★ A-13 승인 대기 / 기본 · ◇ A-15 승인 완료 순간 → H-04 */
export default function PendingPage() {
  const router = useRouter();
  const { update, ready, session, me, refreshMe } = useSession();
  const [demoApproved, setApproved] = useState(false);
  const approved = demoApproved || (API_ENABLED && me?.status === "APPROVED");
  const completion = 43;
  const editable = API_ENABLED ? !!me?.signupInfoEditable : true;

  // 로그인 전이거나 승인 대기가 아니면 맞는 화면으로
  useEffect(() => {
    if (!API_ENABLED || !ready) return;
    if (!session.loggedIn) router.replace("/login");
    else if (me?.status === "REJECTED") router.replace(landingPath(me));
  }, [ready, session.loggedIn, me, router]);

  // 다른 탭에서 돌아오면 승인됐는지 다시 확인한다
  useEffect(() => {
    if (!API_ENABLED) return;
    const check = () => {
      if (document.visibilityState === "visible") void refreshMe();
    };
    document.addEventListener("visibilitychange", check);
    return () => document.removeEventListener("visibilitychange", check);
  }, [refreshMe]);

  const enter = () => {
    update({ status: "APPROVED", firstVisit: true });
    router.push("/home");
  };

  return (
    <div className="flex min-h-full flex-1 flex-col">
      <Gnb variant="pending" />
      {/* ◇ A-15 승인 완료 순간 — 상단 배너 */}
      {approved && (
        <div className="container-page pt-4">
          <div role="status" className="flex items-center justify-between rounded-[8px] bg-[#1f2937] px-5 py-3.5 text-white">
            <p className="flex items-center gap-2.5 text-[14px] font-medium">
              <Icon name="check" size={18} /> 가입이 승인됐어요!
            </p>
            <button type="button" onClick={enter} className="t-label2 underline underline-offset-2">
              시작하기
            </button>
          </div>
        </div>
      )}
      <main className={`flex flex-1 justify-center px-4 ${approved ? "pt-[54px]" : "pt-[188px]"}`}>
        <div className="flex w-full max-w-[480px] flex-col items-center gap-5 text-center">
          <span className="grid size-20 place-items-center rounded-full bg-bg-100 text-ink-700">
            <Icon name="clock" size={34} />
          </span>
          <h1 className="text-[22px] font-bold text-ink-900">운영진이 동문 확인 중이에요.</h1>
          <p className="text-[15px] leading-6 text-ink-500">{approved ? "보통 1~2일 걸려요." : "보통 1~2일 걸려요. 그동안 프로필을 채워두면 승인되자마자 사자들이 찾아올 수 있어요."}</p>
          <ProgressBar value={completion} className="w-[360px]" />
          <div className="flex w-full items-center justify-between rounded-[8px] bg-bg-100 px-3.5 py-2.5">
            <p className="text-[13px] font-medium text-ink-900">자기소개를 쓰면 57%가 돼요</p>
            <Button size="S" variant="secondary" onClick={approved ? enter : undefined} href={approved ? undefined : "/onboarding"}>
              쓰러 가기
            </Button>
          </div>
          {!approved && editable && (
            <Button size="S" variant="secondary" href="/signup">
              가입 정보 고치기
            </Button>
          )}
          <p className="text-[13px] text-ink-400">3일이 지나도 소식이 없으면 운영진에게 문의해주세요</p>
          {!approved && !API_ENABLED && (
            <>
              <ProtoButton onClick={() => setApproved(true)}>승인된 순간 보기</ProtoButton>
              <ProtoButton href="/rejected">반려 안내 보기</ProtoButton>
            </>
          )}
        </div>
      </main>
    </div>
  );
}
