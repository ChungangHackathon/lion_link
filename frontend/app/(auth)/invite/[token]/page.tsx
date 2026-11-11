"use client";

import { useRouter } from "next/navigation";
import { Suspense, use, useState } from "react";
import { Checkbox, Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Misc";
import { useSession } from "@/lib/session";
import { invites } from "@/lib/mock/notifications";
import { GoogleMark } from "../../login/page";
import { VillageHero } from "@/components/brand/VillageHero";

/**
 * ★ A-01 초대 가입 / 동의 전 → A-02 동의 후 → A-04 구글 → O-01.
 * A-05/06/07: 유효하지 않음 · 만료 · 이미 사용된 링크.
 */
function InviteInner({ params }: { params: Promise<{ token: string }> }) {
  const { token } = use(params);
  const router = useRouter();
  const { login } = useSession();
  const [agreed, setAgreed] = useState(false);
  const [terms, setTerms] = useState(false);
  const [loading, setLoading] = useState(false);

  const invite = invites.find((i) => i.token === token) ?? invites[0];
  const problem = token === "expired" ? "EXPIRED" : token === "used" ? "USED" : token === "invalid" ? "INVALID" : null;

  const go = () => {
    setLoading(true);
    setTimeout(() => {
      login({ status: "PENDING", userId: "u-me" });
      router.push("/onboarding");
    }, 500);
  };

  return (
    <VillageHero generation={invite.generation}>
      <div className="flex w-full flex-col gap-6 rounded-[12px] p-10">
        {problem ? (
          <InviteProblem kind={problem} />
        ) : (
          <>
            <h1 className="t-title text-light-card">
              {invite.name}님, {invite.generation}기 동문으로 초대받았어요!
            </h1>
            <div className="flex flex-col gap-2.5">
              <div className="flex gap-3">
                <Input label={<span className="text-bg-100">이름</span>} value={invite.name} readOnly />
                <Input label={<span className="text-bg-100">기수</span>} value={`${invite.generation}기`} readOnly />
              </div>
              <p className="t-label2 text-bg-100">이름·기수는 초대 정보로 고정돼요.</p>
            </div>
            <div className="flex items-center justify-between rounded-[8px] border border-line-200 bg-white p-4">
              <Checkbox label="[필수] 개인정보 수집·이용 동의" checked={agreed} onChange={(e) => setAgreed(e.target.checked)} />
              <button type="button" className="t-caption1 text-ink-500 hover:underline" onClick={() => setTerms(true)}>
                전문 보기
              </button>
            </div>
            <Button size="L" full variant="secondary" disabled={!agreed} loading={loading} onClick={go} leading={<GoogleMark muted={!agreed} />} className={agreed ? "border-ink-500" : "bg-bg-100 border-line-200"}>
              구글로 계속하기
            </Button>
          </>
        )}
      </div>

      {/* [O] A-03 개인정보 동의 전문 / 모달 */}
      <Modal open={terms} onClose={() => setTerms(false)} title="개인정보 수집·이용 동의" width={640} footer={<Button onClick={() => { setAgreed(true); setTerms(false); }}>동의하고 닫기</Button>}>
        <div className="max-h-[340px] overflow-y-auto rounded-[8px] border border-line-200 bg-bg-50 p-4 text-[14px] leading-6 text-ink-700">
          <p className="font-bold">1. 수집 항목</p>
          <p>이름, 기수, 구글 계정 이메일, 프로필에 직접 입력한 정보(활동 파트, 현재 상태, 관심 분야, 경력·학력 등)</p>
          <p className="mt-3 font-bold">2. 수집 목적</p>
          <p>동문 확인(가입 승인), 동문 간 커피챗·채팅 연결, 커뮤니티 운영, 알림 메일 발송</p>
          <p className="mt-3 font-bold">3. 보유 기간</p>
          <p>탈퇴 시까지. 탈퇴 후 즉시 파기하며, 법령에 따라 보관이 필요한 정보는 해당 기간 동안 보관해요.</p>
          <p className="mt-3 font-bold">4. 동의 거부 권리</p>
          <p>동의하지 않을 수 있으나, 이 경우 멋사동네에 가입할 수 없어요.</p>
          <p className="mt-3 font-bold">5. 공개 범위</p>
          <p>이름·기수·파트·소개는 승인된 동문에게 공개돼요. 연락처·회사명·외부 링크는 프로필 편집에서 공개 범위를 정할 수 있어요.</p>
        </div>
      </Modal>
    </VillageHero>
  );
}

function InviteProblem({ kind }: { kind: "EXPIRED" | "USED" | "INVALID" }) {
  const copy = {
    INVALID: { title: "유효하지 않은 초대 링크예요", desc: "주소가 잘못 복사됐을 수 있어요. 운영진에게 받은 링크를 다시 확인해주세요." },
    EXPIRED: { title: "만료된 초대 링크예요", desc: "초대 링크는 7일 동안만 쓸 수 있어요. 운영진에게 다시 보내달라고 요청해주세요." },
    USED: { title: "이미 사용된 초대 링크예요", desc: "이 링크로 이미 가입이 진행됐어요. 본인이 가입했다면 로그인해주세요." },
  }[kind];
  return (
    <div className="flex flex-col items-center gap-4 rounded-[12px] bg-white p-8 text-center">
      <h1 className="text-[22px] font-bold text-ink-900">{copy.title}</h1>
      <p className="t-body2 text-ink-500">{copy.desc}</p>
      <Button href="/login" variant="secondary">
        로그인으로
      </Button>
    </div>
  );
}

/** Cache Components: params must be read inside Suspense. */
export default function InvitePage({ params }: { params: Promise<{ token: string }> }) {
  return (
    <Suspense fallback={<Skeleton className="h-[400px] w-full" />}>
      <InviteInner params={params} />
    </Suspense>
  );
}
