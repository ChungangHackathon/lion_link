"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { Checkbox } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { useToast } from "@/components/ui/Toast";
import { useSession } from "@/lib/session";
import { VillageHero } from "@/components/brand/VillageHero";
import { API_ENABLED, errorMessage, isApiError } from "@/lib/api/client";
import { getGoogleInfo, landingPath, reapply, signup, updateSignupInfo } from "@/lib/api/auth";

/**
 * ★ A-01 가입 신청 / 이름·기수 입력 → (A-04 구글 계정 선택) → O-01 온보딩 → A-13 승인 대기.
 * 초대 링크 가입은 없어졌고, 누구나 신청한 뒤 운영진이 동문인지 확인해요.
 *
 * 같은 화면을 세 가지로 쓴다 (API 모드).
 *  - 로그인 전: 구글 로그인 뒤 돌아온 가입 신청 (AUTH-09 → AUTH-02)
 *  - 승인 대기: [가입 정보 고치기] (AUTH-06)
 *  - 반려: [다시 신청하기] (AUTH-07)
 */
export default function SignupPage() {
  const router = useRouter();
  const toast = useToast();
  const { login, ready: sessionReady, me, setMe, refreshMe } = useSession();
  const [name, setName] = useState("");
  const [gen, setGen] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [terms, setTerms] = useState(false);
  const [loading, setLoading] = useState(false);
  const [touched, setTouched] = useState(false);
  const prefilled = useRef(false);

  const mode: "signup" | "edit" | "reapply" = me?.status === "PENDING" ? "edit" : me?.status === "REJECTED" ? "reapply" : "signup";

  const genNum = Number(gen.replace(/[^0-9]/g, ""));
  const nameOk = name.trim().length >= 2 && name.trim().length <= 20;
  const genOk = genNum >= 1 && genNum <= 14;
  const ready = nameOk && genOk && (mode !== "signup" || agreed);

  // 칸 미리 채우기: 로그인했으면 내 정보, 아니면 구글 계정 이름(AUTH-09)
  useEffect(() => {
    if (!API_ENABLED || !sessionReady || prefilled.current) return;
    prefilled.current = true;

    if (me) {
      if (me.status === "APPROVED") {
        router.replace(landingPath(me));
        return;
      }
      setName(me.name);
      setGen(String(me.generation));
      return;
    }

    getGoogleInfo()
      .then((info) => {
        if (info.name) setName((v) => v || info.name || "");
      })
      .catch((error) => {
        if (isApiError(error, "UNAUTHENTICATED")) {
          toast("가입 신청 시간이 지났어요. 구글 로그인부터 다시 해주세요.", { kind: "error" });
          router.replace("/login");
        } else {
          toast(errorMessage(error), { kind: "error" });
        }
      });
  }, [sessionReady, me, router, toast]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setTouched(true);
    if (!ready) return;
    setLoading(true);

    if (!API_ENABLED) {
      setTimeout(() => {
        login({ status: "PENDING", userId: "u-me" });
        router.push("/onboarding");
      }, 500);
      return;
    }

    const info = { name: name.trim(), generation: genNum };
    try {
      if (mode === "signup") {
        const res = await signup({ ...info, privacyAgreed: agreed });
        setMe(res.user);
        router.push(landingPath(res.user));
      } else {
        const next = mode === "edit" ? await updateSignupInfo(info) : await reapply(info);
        setMe(next);
        toast(mode === "edit" ? "가입 정보를 고쳤어요" : "다시 신청했어요. 운영진이 확인할게요", { kind: "success" });
        router.push(landingPath(next));
      }
    } catch (error) {
      toast(errorMessage(error), { kind: "error" });
      if (isApiError(error, "UNAUTHENTICATED") && mode === "signup") {
        router.replace("/login");
      } else if (isApiError(error) && error.status === 409) {
        // 이미 가입됨·이미 승인됨 등 상태가 바뀜 → 내 정보를 다시 불러와 맞는 화면으로
        const latest = await refreshMe();
        if (latest) router.replace(landingPath(latest));
      }
      setLoading(false);
    }
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
          <p className="t-label2 text-bg-100">{touched && !nameOk ? "이름은 2~20글자로 입력해주세요." : touched && !genOk ? "기수는 1~14기 사이로 입력해주세요." : "이름·기수는 승인 전까지 고칠 수 있어요."}</p>
        </div>
        {mode === "signup" && (
          <div className={`flex items-center justify-between rounded-[8px] border bg-white p-4 ${touched && !agreed ? "border-orange" : "border-line-200"}`}>
            <Checkbox label="[필수] 개인정보 수집·이용 동의" checked={agreed} onChange={(e) => setAgreed(e.target.checked)} />
            <button type="button" className="t-caption1 text-ink-500 hover:underline" onClick={() => setTerms(true)}>
              전문 보기
            </button>
          </div>
        )}
        <Button type="submit" size="L" full loading={loading}>
          {mode === "edit" ? "가입 정보 저장하기" : mode === "reapply" ? "다시 신청하기" : "가입 신청하기"}
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
