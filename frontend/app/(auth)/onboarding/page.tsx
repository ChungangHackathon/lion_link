"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { AuthShell } from "@/components/layout/AuthShell";
import { Button, Chip } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { INTEREST_OPTIONS, LION_PART_LABEL, PROFILE_STATUS_LABEL, type LionPart, type ProfileStatus } from "@/lib/types";
import { useSession } from "@/lib/session";
import { useToast } from "@/components/ui/Toast";

const PARTS = Object.keys(LION_PART_LABEL) as LionPart[];
const STATUSES = Object.keys(PROFILE_STATUS_LABEL) as ProfileStatus[];

/** ★ O-01 기본 정보 입력 / 빈 상태 → ◇ O-02 재직 선택 + 입력 완료 → A-13 */
export default function OnboardingPage() {
  const router = useRouter();
  const { user } = useSession();
  const toast = useToast();
  const [parts, setParts] = useState<LionPart[]>([]);
  const [status, setStatus] = useState<ProfileStatus | null>(null);
  const [interests, setInterests] = useState<string[]>([]);
  const [link, setLink] = useState("");
  const [intro, setIntro] = useState("");
  const [company, setCompany] = useState("");
  const [role, setRole] = useState("");
  const [staff, setStaff] = useState(false);
  const [loading, setLoading] = useState(false);

  const togglePart = (p: LionPart) => {
    setParts((s) => (s.includes(p) ? s.filter((x) => x !== p) : s.length >= 2 ? (toast("멋사 파트는 최대 2개까지 선택할 수 있어요", { kind: "error" }), s) : [...s, p]));
  };
  const toggleInterest = (v: string) => {
    setInterests((s) => (s.includes(v) ? s.filter((x) => x !== v) : s.length >= 3 ? (toast("관심 분야는 최대 3개까지 선택할 수 있어요", { kind: "error" }), s) : [...s, v]));
  };

  const needsCompany = status === "EMPLOYED" || status === "FOUNDER";
  const valid = parts.length > 0 && !!status && interests.length > 0 && (!needsCompany || (company.trim() && role.trim())) && intro.length <= 100;

  const submit = () => {
    setLoading(true);
    setTimeout(() => router.push("/pending"), 500);
  };

  return (
    <AuthShell width={640}>
      <div className="rounded-[8px] bg-bg-100 p-4">
        <p className="t-body-bold text-ink-900">30초면 끝나요. 자세한 프로필은 나중에 채워도 돼요.</p>
      </div>
      <div className="flex gap-3">
        <Input label="이름" value={user?.name ?? "박중앙"} readOnly />
        <Input label="기수" value={`${user?.generation ?? 13}기`} readOnly />
      </div>

      <Field label="멋사 활동 파트" hint="1~2개">
        {PARTS.map((p) => (
          <Chip key={p} selected={parts.includes(p)} onClick={() => togglePart(p)}>
            {LION_PART_LABEL[p]}
          </Chip>
        ))}
      </Field>

      <Field label="현재 상태" hint="1개">
        {STATUSES.map((s) => (
          <Chip key={s} selected={status === s} onClick={() => setStatus(s)}>
            {PROFILE_STATUS_LABEL[s]}
          </Chip>
        ))}
      </Field>

      {needsCompany && (
        <div className="flex flex-col gap-3 rounded-[8px] border border-line-200 bg-bg-50 p-4">
          <div className="flex gap-3">
            <Input label={status === "FOUNDER" ? "회사·팀 이름" : "회사명"} placeholder="예) 핀테크 스타트업" value={company} onChange={(e) => setCompany(e.target.value)} required />
            <Input label="직무" placeholder="예) 백엔드 개발자" value={role} onChange={(e) => setRole(e.target.value)} required />
          </div>
          <p className="t-caption1 text-ink-500">회사명 공개 범위는 나중에 프로필 편집에서 바꿀 수 있어요.</p>
        </div>
      )}

      <Field label="관심 분야" hint="1~3개">
        {INTEREST_OPTIONS.map((v) => (
          <Chip key={v} selected={interests.includes(v)} onClick={() => toggleInterest(v)}>
            {v}
          </Chip>
        ))}
      </Field>

      <Input label="프로필 링크 (선택)" placeholder="링크드인 · 깃허브 · 포트폴리오 등" value={link} onChange={(e) => setLink(e.target.value)} />
      <Input label="한 줄 소개 (선택)" placeholder="예) 핀테크 스타트업 3년 차 백엔드 개발자, 사이드 프로젝트 같이 할 사람 찾아요" value={intro} onChange={(e) => setIntro(e.target.value)} counter={{ value: intro.length, max: 100 }} error={intro.length > 100 ? "100자 이내로 적어주세요" : undefined} />

      {/* 운영진 활동 (v2.9 §3) */}
      <label className="flex items-center justify-between rounded-[8px] border border-line-200 px-4 py-3">
        <span className="flex flex-col">
          <span className="t-label1 text-ink-900">운영진으로 활동했어요</span>
          <span className="t-caption1 text-ink-500">운영진 이력은 프로필에 배지로 표시돼요. 승인 시 운영진이 확인해요.</span>
        </span>
        <input type="checkbox" checked={staff} onChange={(e) => setStaff(e.target.checked)} className="size-[18px] accent-primary" />
      </label>

      <Button size="L" full disabled={!valid} loading={loading} onClick={submit}>
        시작하기
      </Button>
    </AuthShell>
  );
}

function Field({ label, hint, children }: { label: string; hint: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-2.5">
      <div className="flex items-center gap-1.5 t-label2">
        <span className="text-ink-700">{label} *</span>
        <span className="font-normal text-ink-400">{hint}</span>
      </div>
      <div className="flex flex-wrap gap-2">{children}</div>
    </div>
  );
}
