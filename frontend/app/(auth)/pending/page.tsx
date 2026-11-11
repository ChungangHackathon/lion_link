"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Gnb } from "@/components/layout/Gnb";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { ProgressBar, ProtoButton } from "@/components/ui/Misc";
import { useSession } from "@/lib/session";

/** ★ A-13 승인 대기 / 기본 · ◇ A-15 승인 완료 순간 → H-04 */
export default function PendingPage() {
  const router = useRouter();
  const { update } = useSession();
  const [approved, setApproved] = useState(false);
  const completion = 43;

  const enter = () => {
    update({ status: "APPROVED", firstVisit: true });
    router.push("/home");
  };

  return (
    <div className="flex min-h-full flex-1 flex-col">
      <Gnb variant="pending" />
      <main className="flex flex-1 justify-center px-4 pt-[188px]">
        <div className="flex w-full max-w-[480px] flex-col items-center gap-5 text-center">
          <span className="grid size-20 place-items-center rounded-full bg-bg-100">
            {approved ? <CheckBig /> : <Icon name="clock" />}
          </span>
          {approved ? (
            <>
              <h1 className="text-[22px] font-bold text-ink-900">승인됐어요! 멋사동네에 온 걸 환영해요 🎉</h1>
              <p className="text-[15px] leading-[1.6] text-ink-500">이제 동문을 찾고 커피챗을 신청할 수 있어요. 프로필을 채워두면 후배들이 더 쉽게 찾아와요.</p>
              <ProgressBar value={completion} className="w-[360px]" />
              <div className="flex w-full items-center justify-between rounded-[8px] bg-bg-100 px-3.5 py-2.5">
                <p className="text-[13px] font-medium text-ink-900">자기소개를 쓰면 57%가 돼요</p>
                <Button size="S" variant="secondary" onClick={enter}>
                  쓰러 가기
                </Button>
              </div>
              <Button size="L" onClick={enter} className="w-[360px]">
                홈으로 가기
              </Button>
            </>
          ) : (
            <>
              <h1 className="text-[22px] font-bold text-ink-900">운영진이 동문 확인 중이에요.</h1>
              <p className="text-[15px] leading-[1.6] text-ink-500">보통 1~2일 걸려요. 그동안 프로필을 채워두면 승인되자마자 후배들이 찾아올 수 있어요.</p>
              <ProgressBar value={completion} className="w-[360px]" />
              <div className="flex w-full items-center justify-between rounded-[8px] bg-bg-100 px-3.5 py-2.5">
                <p className="text-[13px] font-medium text-ink-900">자기소개를 쓰면 57%가 돼요</p>
                <Button size="S" variant="secondary" href="/me">
                  쓰러 가기
                </Button>
              </div>
              <p className="text-[13px] text-ink-400">3일이 지나도 소식이 없으면 운영진에게 문의해주세요</p>
              <ProtoButton onClick={() => setApproved(true)}>승인된 순간 보기</ProtoButton>
              <ProtoButton href="/rejected">반려 안내 보기</ProtoButton>
            </>
          )}
        </div>
      </main>
    </div>
  );
}

function CheckBig() {
  return (
    <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#2155a4" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="m5 12 5 5L20 7" />
    </svg>
  );
}
