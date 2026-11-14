"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Checkbox } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { useSession } from "@/lib/session";
import { VillageHero } from "@/components/brand/VillageHero";

/**
 * ★ A-01 가입 신청 / 이름·기수 입력 → (A-04 구글 계정 선택) → O-01 온보딩 → A-13 승인 대기.
 * 초대 링크 가입은 없어졌고, 누구나 신청한 뒤 운영진이 동문인지 확인해요.
 */
export default function SignupPage() {
  const router = useRouter();
  const { login } = useSession();
  const [name, setName] = useState("");
  const [gen, setGen] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [terms, setTerms] = useState(false);
  const [loading, setLoading] = useState(false);
  const [touched, setTouched] = useState(false);

  const genNum = Number(gen.replace(/[^0-9]/g, ""));
  const nameOk = name.trim().length >= 2;
  const genOk = genNum >= 1 && genNum <= 14;
  const ready = nameOk && genOk && agreed;

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    setTouched(true);
    if (!ready) return;
    setLoading(true);
    setTimeout(() => {
      login({ status: "PENDING", userId: "u-me" });
      router.push("/onboarding");
    }, 500);
  };

  const field = "h-10 w-full rounded-[6px] border bg-white px-3 text-[15px] text-ink-900 outline-none placeholder:text-ink-400 focus:border-ink-700";

  return (
    <VillageHero generation={genOk ? genNum : 13} tag={nameOk ? `${name.trim()}의 집` : undefined}>
      <form onSubmit={submit} className="flex w-full flex-col gap-6 rounded-[16px] border border-white/70 bg-white/10 p-10 shadow-[0_8px_40px_rgba(0,36,94,0.12)] backdrop-blur-md">
        <h1 className="t-title text-light-card">멋사 중앙대 동문이신가요? 이름과 기수를 알려주세요</h1>
        <div className="flex flex-col gap-2.5">
          <div className="flex gap-3">
            <label className="flex flex-1 flex-col gap-1.5">
              <span className="t-label1 text-bg-100">이름</span>
              <input value={name} onChange={(e) => setName(e.target.value)} placeholder="박중앙" autoComplete="name" aria-invalid={touched && !nameOk} className={`${field} ${touched && !nameOk ? "border-orange" : "border-line-300"}`} />
            </label>
            <label className="flex flex-1 flex-col gap-1.5">
              <span className="t-label1 text-bg-100">기수</span>
              <input value={gen} onChange={(e) => setGen(e.target.value)} placeholder="13기" inputMode="numeric" aria-invalid={touched && !genOk} className={`${field} ${touched && !genOk ? "border-orange" : "border-line-300"}`} />
            </label>
          </div>
          <p className="t-label2 text-bg-100">{touched && !nameOk ? "이름을 2글자 이상 입력해주세요." : touched && !genOk ? "기수는 1~14기 사이로 입력해주세요." : "이름·기수는 승인 전까지 고칠 수 있어요."}</p>
        </div>
        <div className={`flex items-center justify-between rounded-[8px] border bg-white p-4 ${touched && !agreed ? "border-orange" : "border-line-200"}`}>
          <Checkbox label="[필수] 개인정보 수집·이용 동의" checked={agreed} onChange={(e) => setAgreed(e.target.checked)} />
          <button type="button" className="t-caption1 text-ink-500 hover:underline" onClick={() => setTerms(true)}>
            전문 보기
          </button>
        </div>
        <Button type="submit" size="L" full loading={loading}>
          가입 신청하기
        </Button>
        <p className="text-center t-caption1 text-ink-500">운영진이 동문인지 확인한 뒤 승인해요. 보통 1~2일 걸려요.</p>
      </form>

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
