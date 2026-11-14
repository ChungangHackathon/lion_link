"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { useStore } from "@/lib/store";
import { profileDetail, type ProfileDetail } from "@/lib/mock/profile";
import { ME_ID } from "@/lib/mock/users";
import { INTEREST_OPTIONS, LION_PART_LABEL, PROFILE_STATUS_LABEL, type LionPart, type ProfileStatus } from "@/lib/types";
import { Avatar } from "@/components/ui/Avatar";
import { AvailabilityBadge, PartTag, Tag } from "@/components/ui/Badge";
import { Button, Chip } from "@/components/ui/Button";
import { Icon, type IconName } from "@/components/ui/Icon";
import { Checkbox, Input, Select, Textarea } from "@/components/ui/Input";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { Card, ProgressBar } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

type Section = "basic" | "about" | "contact" | "career" | "education" | "lion" | "project" | "skills" | "links" | "certs";
const ADDABLE: { key: Section; label: string; desc: string }[] = [
  { key: "about", label: "소개", desc: "나를 소개하는 글" },
  { key: "career", label: "경력", desc: "회사 · 직무 · 기간" },
  { key: "education", label: "학력", desc: "학교 · 전공 · 학위" },
  { key: "lion", label: "멋사 활동", desc: "기수 · 역할 · 프로젝트" },
  { key: "project", label: "프로젝트", desc: "사이드 프로젝트 · 대외 활동" },
  { key: "skills", label: "대표 기술", desc: "대표 기술 최대 5개" },
  { key: "links", label: "외부 링크", desc: "깃허브 · 링크드인 · 포트폴리오" },
  { key: "certs", label: "자격증·수상·언어", desc: "자격증 · 수상 · 대외활동 · 언어" },
];

/**
 * ★ P-02 마이페이지 / 내 프로필 (= 마이페이지 첫 화면). 섹션별 바로 편집(연필) → E-09~E-15 팝업,
 * + 프로필 항목 추가 → E-07, 경력 추가 → E-04, 마지막 항목 삭제 → CF-23.
 */
export default function MyProfilePage() {
  const toast = useToast();
  const { alumni, updateMe, settings } = useStore();
  const me = alumni.find((a) => a.id === ME_ID)!;
  const [detail, setDetail] = useState<ProfileDetail>(() => profileDetail(ME_ID));
  const [edit, setEdit] = useState<Section | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [careerForm, setCareerForm] = useState<{ index: number | null } | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<{ section: "career" | "project" | "certs"; index: number } | null>(null);
  const [saved, setSaved] = useState<string | null>(null);

  const completion = (() => {
    let c = 30;
    if (detail.about) c += 14;
    if (detail.careers.length) c += 14;
    if (detail.educations.length) c += 10;
    if (detail.lion.length) c += 10;
    if (detail.repSkills.length) c += 8;
    if (detail.links.length) c += 7;
    if (detail.projects.length) c += 7;
    return Math.min(100, c);
  })();
  const nextStep = !detail.about ? { text: `자기소개를 쓰면 ${Math.min(100, completion + 14)}%가 돼요`, section: "about" as Section } : !detail.careers.length ? { text: `경력을 추가하면 ${Math.min(100, completion + 14)}%가 돼요`, section: "career" as Section } : !detail.projects.length ? { text: `프로젝트를 추가하면 ${Math.min(100, completion + 7)}%가 돼요`, section: "project" as Section } : null;

  const done = (label: string) => {
    setEdit(null);
    setSaved(label);
    toast("저장했어요");
    setTimeout(() => setSaved(null), 2000);
  };

  const filled = Math.round((completion / 100) * 9);
  type Quick = { icon: IconName; label: string; section: Section };
  const quick = ([
    { icon: "image", label: "프로필 사진 올리기", section: "basic" },
    ...(detail.careers.length ? [] : [{ icon: "edit", label: "경력 추가", section: "career" }]),
    ...(detail.about ? [] : [{ icon: "edit", label: "소개 쓰기", section: "about" }]),
    ...(detail.projects.length ? [] : [{ icon: "plus", label: "프로젝트 추가", section: "project" }]),
  ] as Quick[]).slice(0, 3);

  return (
    <div className="flex flex-col gap-6">
      {/* 완성도 카드 (본인에게만) — 항목 9개 = 9칸 */}
      {completion < 100 && (
        <div className="flex flex-col gap-3 rounded-[10px] bg-[#edf2fb] px-6 py-5 shadow-[0_3px_6px_rgba(0,0,0,0.1)]">
          <p className="flex items-center gap-2.5 t-title3">
            <span className="text-ink-900">프로필 완성도</span>
            <span className="text-primary-strong">{completion}%</span>
          </p>
          <div className="flex gap-1" role="progressbar" aria-valuenow={completion} aria-valuemin={0} aria-valuemax={100} aria-label="프로필 완성도">
            {Array.from({ length: 9 }, (_, i) => (
              <span key={i} className={cn("h-2 flex-1 rounded-[4px]", i < filled ? "bg-primary-strong" : "bg-line-200")} />
            ))}
          </div>
          <p className="t-caption1 text-ink-500">{nextStep ? nextStep.text : `아래 ${quick.length}가지만 채우면 완성돼요`}</p>
          <div className="flex flex-wrap gap-2">
            {quick.map((q) => (
              <button key={q.label} type="button" onClick={() => (q.section === "career" ? setCareerForm({ index: null }) : setEdit(q.section))} className="flex h-[34px] items-center gap-2 rounded-full border border-line-200 bg-white px-3 t-label2 font-medium text-ink-900 hover:bg-bg-50">
                <Icon name={q.icon} size={16} className="text-ink-700" />
                {q.label}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 상단 소개 카드 (E-12) */}
      <Card className="flex gap-8 rounded-[10px] p-8">
        <div className="relative h-[140px] w-[140px] shrink-0">
          <Avatar name={me.name} part={me.part} size={96} className="!size-[140px] !text-[52px]" />
          <button type="button" aria-label="프로필 사진 바꾸기" onClick={() => setEdit("basic")} className="absolute bottom-0 right-0 grid size-9 place-items-center rounded-full border border-line-200 bg-white text-ink-900 shadow-[0_1px_3px_rgba(0,0,0,0.12)] hover:bg-bg-50">
            <Icon name="edit" size={18} />
          </button>
        </div>
        <div className="flex min-w-0 flex-1 flex-col gap-3 pt-1">
          <div className="flex items-start justify-between gap-3">
            <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
              <h1 className="t-title text-ink-900">{me.name}</h1>
              <span className="rounded-[5px] bg-part-be-bg px-2.5 py-1 t-label2 text-ink-500">
                {me.generation}기 · {LION_PART_LABEL[me.part]}
              </span>
              <AvailabilityBadge mode={settings.mode} note={settings.restUntil ? `쉬는 중 · ${settings.restUntil}까지` : undefined} small />
            </div>
            <div className="flex shrink-0 gap-2">
              <Button size="S" variant="secondary" href={`/alumni/${ME_ID}?preview=1`} className="h-[34px]" leading={<Icon name="monitor" size={16} />}>
                다른 동문에게 보이는 모습
              </Button>
              <button type="button" onClick={() => setEdit("basic")} className="flex h-[34px] items-center gap-1.5 rounded-[6px] bg-part-be-bg px-3 t-label2 text-primary-strong hover:bg-part-be-line">
                <Icon name="edit" size={16} /> 프로필 편집
              </button>
            </div>
          </div>
          <p className="text-[18px] leading-7 text-ink-700">{me.intro || <span className="text-ink-400">한 줄 소개를 적어보세요</span>}</p>
          <div className="flex items-center gap-2 t-caption1">
            <span className="text-ink-500">대표 기술</span>
            {detail.repSkills.length ? (
              detail.repSkills.map((s) => (
                <span key={s} className="rounded-[4px] bg-bg-100 px-2 py-1 text-[12px] text-ink-700">
                  {s}
                </span>
              ))
            ) : (
              <span className="text-ink-400">아직 없어요</span>
            )}
            <button type="button" aria-label="대표 기술 편집" onClick={() => setEdit("skills")} className="rounded p-1 text-ink-500 hover:bg-bg-100 hover:text-ink-900">
              <Icon name="edit" size={14} />
            </button>
          </div>
          <div className="flex flex-wrap items-center gap-x-5 gap-y-1 text-[14px] text-ink-500">
            <span className="flex items-center gap-1.5">
              <Icon name="user" size={16} className="text-ink-900" /> {me.headline}
            </span>
            {detail.location && (
              <span className="flex items-center gap-1.5">
                <Icon name="place" size={16} className="text-ink-900" /> {detail.location}
              </span>
            )}
            {detail.links[0] && (
              <span className="flex items-center gap-1.5">
                <Icon name="link" size={16} className="text-ink-900" /> {detail.links[0].url}
              </span>
            )}
          </div>
        </div>
      </Card>

      {saved && <p className="rounded-[8px] bg-part-be-bg px-4 py-2 text-[13px] text-primary-strong">{saved} 저장 완료 · 프로필에 바로 반영됐어요</p>}

      {/* 본문 — 추가한 항목만 한 카드 안에 */}
      <Card className="flex flex-col gap-9 rounded-[10px] p-7">
        {detail.about && (
          <MySection title="소개" onEdit={() => setEdit("about")}>
            <p className="whitespace-pre-line text-[14px] leading-6 text-ink-700">{detail.about}</p>
          </MySection>
        )}
        {detail.careers.length > 0 && (
          <MySection title="경력" onEdit={() => setEdit("career")}>
            <ul className="flex flex-col gap-4">
              {detail.careers.map((c, i) => (
                <ItemRow key={i} title={c.company} sub={`${c.role}${c.type ? ` · ${c.type}` : ""}`} meta={c.from ? `${c.from} ~ ${c.to} · ${c.duration}` : "시작 시점을 입력해주세요"} />
              ))}
            </ul>
          </MySection>
        )}
        {detail.educations.length > 0 && (
          <MySection title="학력" onEdit={() => setEdit("education")}>
            <ul className="flex flex-col gap-4">
              {detail.educations.map((e, i) => (
                <ItemRow key={i} title={e.school} sub={`${e.major} · ${e.degree}`} meta={`${e.from} ~ ${e.to}`} />
              ))}
            </ul>
          </MySection>
        )}
        {detail.lion.length > 0 && (
          <MySection title="멋사 활동" onEdit={() => setEdit("lion")}>
            <ul className="flex flex-col gap-2">
              {detail.lion.map((l, i) => (
                <li key={i} className="rounded-[8px] border border-line-200 bg-bg-50 px-[15px] py-3 t-body-bold font-medium text-ink-900">
                  {l.generation}기 {l.role}
                </li>
              ))}
            </ul>
          </MySection>
        )}
        {detail.projects.length > 0 && (
          <MySection title="프로젝트" onEdit={() => setEdit("project")}>
            <ul className="flex flex-col gap-4">
              {detail.projects.map((pj, i) => (
                <ItemRow key={i} title={pj.name} sub={pj.type} meta={`${pj.from} ~ ${pj.to}`} />
              ))}
            </ul>
          </MySection>
        )}
        {detail.certs.length > 0 && (
          <MySection title="자격증·수상·대외활동·언어" onEdit={() => setEdit("certs")}>
            <dl className="flex flex-col gap-2 text-[14px]">
              {detail.certs.map((c, i) => (
                <div key={i} className="flex gap-4">
                  <dt className="w-16 text-ink-500">{c.kind}</dt>
                  <dd className="text-ink-900">{c.text}</dd>
                </div>
              ))}
            </dl>
          </MySection>
        )}
        {detail.links.length > 0 && (
          <MySection title="외부 링크" onEdit={() => setEdit("links")}>
            <ul className="flex flex-col gap-2">
              {detail.links.map((l) => (
                <li key={l.label} className="flex items-center gap-2 text-[14px] text-ink-700">
                  <Icon name="link" size={18} className="text-ink-700" /> {l.label} · <span className="font-medium text-ink-900">{l.url}</span>
                </li>
              ))}
            </ul>
          </MySection>
        )}
        <MySection title="연락처" onEdit={() => setEdit("contact")}>
          <p className="flex items-center gap-2 text-[14px] font-medium text-ink-900">
            <Icon name="mail" size={18} className="text-ink-700" /> {detail.email?.value ?? "아직 연락처가 없어요"}
            {detail.email && (
              <span className="ml-1 flex items-center gap-1 text-[12px] font-normal text-ink-500">
                <Icon name="lock" size={12} /> {detail.email.visibility === "ALL" ? "모든 동문" : detail.email.visibility === "CONNECTED" ? "연결된 동문만" : "나만"}
              </span>
            )}
          </p>
        </MySection>
        <button type="button" onClick={() => setAddOpen(true)} className="flex h-[46px] items-center justify-center gap-2 rounded-[8px] border border-dashed border-line-300 text-[14px] font-medium text-ink-500 hover:bg-bg-50">
          <Icon name="plus" size={16} /> 프로필 항목 추가
        </button>
      </Card>

      {/* ★[O] E-07 프로필 항목 추가 */}
      <Modal open={addOpen} onClose={() => setAddOpen(false)} title="프로필 항목 추가" description="추가한 항목만 프로필에 보여요" width={560}>
        <ul className="grid grid-cols-2 gap-2">
          {ADDABLE.map((a) => {
            const added = a.key === "about" ? !!detail.about : a.key === "career" ? detail.careers.length > 0 : a.key === "education" ? detail.educations.length > 0 : a.key === "lion" ? detail.lion.length > 0 : a.key === "project" ? detail.projects.length > 0 : a.key === "skills" ? detail.repSkills.length > 0 : a.key === "links" ? detail.links.length > 0 : detail.certs.length > 0;
            return (
              <li key={a.key}>
                <button type="button" onClick={() => { setAddOpen(false); if (a.key === "career") setCareerForm({ index: null }); else setEdit(a.key); }} className={cn("flex w-full items-center justify-between rounded-[8px] border px-4 py-3 text-left hover:bg-bg-50", added ? "border-line-200" : "border-line-300")}>
                  <span className="flex flex-col">
                    <span className="t-label1 text-ink-900">{a.label}</span>
                    <span className="text-[12px] text-ink-500">{a.desc}</span>
                  </span>
                  <span className="text-[12px] text-ink-400">{added ? "편집" : "+ 추가"}</span>
                </button>
              </li>
            );
          })}
        </ul>
        {ADDABLE.every((a) => (a.key === "about" ? !!detail.about : true)) && detail.careers.length > 0 && detail.projects.length > 0 && <p className="mt-3 text-center text-[13px] text-ink-500">모든 항목을 추가했어요 👍</p>}
      </Modal>

      {/* E-12 기본 정보 편집 */}
      <BasicEditModal open={edit === "basic"} onClose={() => setEdit(null)} me={me} detail={detail} onSave={(patch, loc) => { updateMe(patch); setDetail((d) => ({ ...d, location: loc })); done("기본 정보"); }} />

      {/* E-10 소개 편집 (한 칸짜리) */}
      <TextModal open={edit === "about"} onClose={() => setEdit(null)} title="소개 편집" label="자기소개" value={detail.about} max={1000} rows={8} placeholder="어떤 일을 하고 있는지, 사자들에게 어떤 이야기를 해줄 수 있는지 적어주세요." onSave={(v) => { setDetail((d) => ({ ...d, about: v })); done("소개"); }} />

      {/* E-09 경력 편집 (목록형) + E-04 경력 추가 */}
      <ListEditModal
        open={edit === "career"}
        onClose={() => setEdit(null)}
        title="경력 편집"
        visibility={{ label: "회사명 공개", hint: "비공개면 '비공개 회사'로 보여요" }}
        items={detail.careers.map((c) => ({ title: c.company, sub: `${c.role} · ${c.type} · ${c.from} ~ ${c.to}` }))}
        onAdd={() => setCareerForm({ index: null })}
        onEdit={(i) => setCareerForm({ index: i })}
        onDelete={(i) => (detail.careers.length === 1 ? setConfirmDelete({ section: "career", index: i }) : setDetail((d) => ({ ...d, careers: d.careers.filter((_, j) => j !== i) })))}
        addLabel="+ 경력 추가"
      />
      <CareerFormModal
        open={!!careerForm}
        onClose={() => setCareerForm(null)}
        initial={careerForm?.index != null ? detail.careers[careerForm.index] : undefined}
        onSave={(c) => {
          setDetail((d) => ({ ...d, careers: careerForm?.index != null ? d.careers.map((x, j) => (j === careerForm.index ? c : x)) : [c, ...d.careers] }));
          setCareerForm(null);
          done("경력");
        }}
      />

      {/* E-09b 학력 */}
      <ListEditModal open={edit === "education"} onClose={() => setEdit(null)} title="학력 편집" items={detail.educations.map((e) => ({ title: e.school, sub: `${e.major} · ${e.degree} · ${e.from} ~ ${e.to}` }))} onAdd={() => { setDetail((d) => ({ ...d, educations: [...d.educations, { school: "중앙대학교", major: "소프트웨어학부", degree: "학사", from: "2021.03", to: "재학 중" }] })); toast("학력을 추가했어요"); }} onEdit={() => toast("학력 수정 폼은 경력 추가 모달(E-04)과 같은 틀이에요")} onDelete={(i) => setDetail((d) => ({ ...d, educations: d.educations.filter((_, j) => j !== i) }))} addLabel="+ 학력 추가" />

      {/* E-09c 멋사 활동 */}
      <ListEditModal open={edit === "lion"} onClose={() => setEdit(null)} title="멋사 활동 편집" items={detail.lion.map((l) => ({ title: `${l.generation}기 · ${l.role}`, sub: `${LION_PART_LABEL[l.part]} · 프로젝트 ${l.projects.length}개` }))} onAdd={() => { setDetail((d) => ({ ...d, lion: [...d.lion, { generation: me.generation + 1, role: `${LION_PART_LABEL[me.part]} 운영진`, part: me.part, projects: [] }] })); toast("활동을 추가했어요. 운영진 이력은 승인 시 운영진이 확인해요."); }} onEdit={() => toast("활동 수정 폼은 경력 추가 모달(E-04)과 같은 틀이에요")} onDelete={(i) => setDetail((d) => ({ ...d, lion: d.lion.filter((_, j) => j !== i) }))} addLabel="+ 활동 추가" />

      {/* 프로젝트 */}
      <ListEditModal open={edit === "project"} onClose={() => setEdit(null)} title="프로젝트 편집" items={detail.projects.map((p) => ({ title: p.name, sub: `${p.type} · ${p.from} ~ ${p.to}` }))} onAdd={() => { setDetail((d) => ({ ...d, projects: [...d.projects, { name: "새 프로젝트", type: "사이드 프로젝트 · 프론트엔드", from: "2026.01", to: "2026.06", skills: ["React"] }] })); toast("프로젝트를 추가했어요"); }} onEdit={() => toast("프로젝트 수정 폼은 경력 추가 모달(E-04)과 같은 틀이에요")} onDelete={(i) => (detail.projects.length === 1 ? setConfirmDelete({ section: "project", index: i }) : setDetail((d) => ({ ...d, projects: d.projects.filter((_, j) => j !== i) })))} addLabel="+ 프로젝트 추가" />

      {/* E-13 연락처 */}
      <ContactModal open={edit === "contact"} onClose={() => setEdit(null)} detail={detail} onSave={(email, phone) => { setDetail((d) => ({ ...d, email, phone })); done("연락처"); }} />

      {/* E-14 대표 기술 */}
      <SkillsModal open={edit === "skills"} onClose={() => setEdit(null)} rep={detail.repSkills} skills={detail.skills} onSave={(rep, skills) => { setDetail((d) => ({ ...d, repSkills: rep, skills })); updateMe({ skills: [...rep, ...skills] }); done("대표 기술"); }} />

      {/* E-15 외부 링크 */}
      <LinksModal open={edit === "links"} onClose={() => setEdit(null)} links={detail.links} onSave={(links) => { setDetail((d) => ({ ...d, links })); done("외부 링크"); }} />

      {/* 자격증·수상·언어 */}
      <ListEditModal open={edit === "certs"} onClose={() => setEdit(null)} title="자격증·수상·대외활동·언어 편집" items={detail.certs.map((c) => ({ title: c.kind, sub: c.text }))} onAdd={() => { setDetail((d) => ({ ...d, certs: [...d.certs, { kind: "자격증", text: "SQLD · 2026.03" }] })); toast("항목을 추가했어요"); }} onEdit={() => toast("항목 수정 폼은 경력 추가 모달(E-04)과 같은 틀이에요")} onDelete={(i) => (detail.certs.length === 1 ? setConfirmDelete({ section: "certs", index: i }) : setDetail((d) => ({ ...d, certs: d.certs.filter((_, j) => j !== i) })))} addLabel="+ 항목 추가" />

      {/* ◇[O] CF-23 마지막 항목 삭제 확인 */}
      <ConfirmModal
        open={!!confirmDelete}
        onClose={() => setConfirmDelete(null)}
        onConfirm={() => {
          if (!confirmDelete) return;
          const { section, index } = confirmDelete;
          const key = section === "career" ? "careers" : section === "project" ? "projects" : "certs";
          setDetail((d) => ({ ...d, [key]: (d[key] as unknown[]).filter((_, j) => j !== index) }));
          setConfirmDelete(null);
          setEdit(null);
          toast("삭제했어요");
        }}
        title={`이 항목을 지우면 ${confirmDelete?.section === "career" ? "경력" : confirmDelete?.section === "project" ? "프로젝트" : "자격증·수상"} 섹션이 프로필에서 사라져요`}
        description="다시 추가하려면 '프로필 항목 추가'에서 넣을 수 있어요."
        confirmLabel="삭제"
        danger
      />
    </div>
  );
}

/* ---------- Edit modals ---------- */

function TextModal({ open, onClose, title, label, value, max, rows, placeholder, onSave }: { open: boolean; onClose: () => void; title: string; label: string; value: string; max: number; rows: number; placeholder?: string; onSave: (v: string) => void }) {
  const [v, setV] = useState(value);
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      width={560}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button disabled={v.length > max} onClick={() => onSave(v.trim())}>
            저장
          </Button>
        </>
      }
    >
      <Textarea label={label} rows={rows} value={v} onChange={(e) => setV(e.target.value)} counter={{ value: v.length, max }} placeholder={placeholder} />
    </Modal>
  );
}

function ListEditModal({ open, onClose, title, items, onAdd, onEdit, onDelete, addLabel, visibility }: { open: boolean; onClose: () => void; title: string; items: { title: string; sub: string }[]; onAdd: () => void; onEdit: (i: number) => void; onDelete: (i: number) => void; addLabel: string; visibility?: { label: string; hint: string } }) {
  const [vis, setVis] = useState("ALL");
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      width={560}
      footer={
        <Button variant="secondary" onClick={onClose}>
          닫기
        </Button>
      }
    >
      <div className="flex flex-col gap-4">
        {visibility && (
          <Select label={visibility.label} value={vis} onChange={(e) => setVis(e.target.value)} help={visibility.hint}>
            <option value="ALL">전체 공개</option>
            <option value="CONNECTED">연결된 동문만</option>
            <option value="ME">비공개</option>
          </Select>
        )}
        <ul className="flex flex-col gap-2">
          {items.length === 0 && <li className="rounded-[8px] bg-bg-50 px-4 py-5 text-center text-[13px] text-ink-500">아직 항목이 없어요</li>}
          {items.map((it, i) => (
            <li key={i} className="flex items-center justify-between rounded-[8px] border border-line-200 px-4 py-3">
              <span className="flex min-w-0 flex-col">
                <span className="t-label1 text-ink-900">{it.title}</span>
                <span className="truncate text-[12px] text-ink-500">{it.sub}</span>
              </span>
              <span className="flex shrink-0 gap-1.5">
                <Button size="S" variant="secondary" onClick={() => onEdit(i)}>
                  수정
                </Button>
                <Button size="S" variant="text" onClick={() => onDelete(i)}>
                  삭제
                </Button>
              </span>
            </li>
          ))}
        </ul>
        <Button variant="secondary" onClick={onAdd}>
          {addLabel}
        </Button>
      </div>
    </Modal>
  );
}

const YEARS = Array.from({ length: 15 }, (_, i) => 2026 - i);
const MONTHS = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, "0"));

/** ★[O] E-04 경력 추가 모달 / Modal/Form 마스터 */
function CareerFormModal({ open, onClose, initial, onSave }: { open: boolean; onClose: () => void; initial?: ProfileDetail["careers"][number]; onSave: (c: ProfileDetail["careers"][number]) => void }) {
  const [company, setCompany] = useState(initial?.company ?? "");
  const [role, setRole] = useState(initial?.role ?? "");
  const [type, setType] = useState(initial?.type ?? "정규직");
  const [from, setFrom] = useState(initial?.from ?? "2026.01");
  const [to, setTo] = useState(initial?.to ?? "현재");
  const [current, setCurrent] = useState(initial ? initial.to === "현재" : true);
  const [desc, setDesc] = useState("");
  const valid = company.trim().length > 0 && role.trim().length > 0 && !!from;
  const duration = () => {
    const [fy, fm] = from.split(".").map(Number);
    const end = current ? new Date() : new Date(Number(to.split(".")[0]), Number(to.split(".")[1]) - 1);
    const months = (end.getFullYear() - fy) * 12 + (end.getMonth() + 1 - fm);
    return months < 12 ? `${Math.max(1, months)}개월` : `${Math.floor(months / 12)}년 ${months % 12}개월`;
  };
  const YM = ({ label, value, onChange, required, disabled }: { label: string; value: string; onChange: (v: string) => void; required?: boolean; disabled?: boolean }) => {
    const [y, m] = value.split(".");
    return (
      <div className="flex flex-col gap-1.5">
        <p className="t-body-bold text-ink-700">
          {label}
          {required && " *"}
        </p>
        <div className="grid grid-cols-2 gap-2">
          <Select value={y ?? ""} disabled={disabled} onChange={(e) => onChange(`${e.target.value}.${m ?? "01"}`)}>
            {YEARS.map((yy) => (
              <option key={yy}>{yy}</option>
            ))}
          </Select>
          <Select value={m ?? ""} disabled={disabled} onChange={(e) => onChange(`${y ?? "2026"}.${e.target.value}`)}>
            {MONTHS.map((mm) => (
              <option key={mm}>{mm}</option>
            ))}
          </Select>
        </div>
      </div>
    );
  };
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={initial ? "경력 수정" : "경력 추가"}
      width={520}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button disabled={!valid} onClick={() => onSave({ company: company.trim(), role: role.trim(), type, from, to: current ? "현재" : to, duration: duration() })}>
            저장
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <Input label="회사명" required placeholder="예) 핀테크 스타트업" value={company} onChange={(e) => setCompany(e.target.value)} />
        <Input label="직무" required placeholder="예) 백엔드 개발자" value={role} onChange={(e) => setRole(e.target.value)} />
        <Select label="고용 형태" required value={type} onChange={(e) => setType(e.target.value)}>
          {["정규직", "계약직", "인턴", "프리랜서", "창업"].map((t) => (
            <option key={t}>{t}</option>
          ))}
        </Select>
        <YM label="시작 년월" required value={from} onChange={setFrom} />
        <YM label="종료 년월" value={current ? "" : to === "현재" ? "2026.01" : to} onChange={setTo} disabled={current} />
        <Checkbox label="현재 재직 중" checked={current} onChange={(e) => setCurrent(e.target.checked)} />
        <Textarea label="설명 (선택)" rows={3} value={desc} onChange={(e) => setDesc(e.target.value)} counter={{ value: desc.length, max: 300 }} />
      </div>
    </Modal>
  );
}

/** ◇[O] E-12 섹션 편집 팝업 / 상단 소개 카드 */
function BasicEditModal({ open, onClose, me, detail, onSave }: { open: boolean; onClose: () => void; me: { name: string; generation: number; status: ProfileStatus; part: LionPart; interests: string[]; intro: string }; detail: ProfileDetail; onSave: (patch: { status: ProfileStatus; part: LionPart; interests: string[]; intro: string }, location?: string) => void }) {
  const [status, setStatus] = useState<ProfileStatus>(me.status);
  const [part, setPart] = useState<LionPart>(me.part);
  const [interests, setInterests] = useState<string[]>(me.interests);
  const [intro, setIntro] = useState(me.intro);
  const [major, setMajor] = useState("소프트웨어학부");
  const [minor, setMinor] = useState("");
  const [loc, setLoc] = useState(detail.location ?? "");
  const toggle = (v: string) => setInterests((s) => (s.includes(v) ? s.filter((x) => x !== v) : s.length >= 3 ? s : [...s, v]));
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="기본 정보 편집"
      width={640}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button disabled={interests.length === 0 || intro.length > 100} onClick={() => onSave({ status, part, interests, intro }, loc)}>
            저장
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="flex items-center gap-4">
          <Avatar name={me.name} part={part} size={80} />
          <Button size="S" variant="secondary">
            사진 변경
          </Button>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Input label="이름" value={me.name} readOnly />
          <Input label="기수" value={`${me.generation}기`} readOnly />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Select label="현재 상태" value={status} onChange={(e) => setStatus(e.target.value as ProfileStatus)}>
            {(Object.keys(PROFILE_STATUS_LABEL) as ProfileStatus[]).map((s) => (
              <option key={s} value={s}>
                {PROFILE_STATUS_LABEL[s]}
              </option>
            ))}
          </Select>
          <Select label="멋사 파트" value={part} onChange={(e) => setPart(e.target.value as LionPart)}>
            {(Object.keys(LION_PART_LABEL) as LionPart[]).map((p) => (
              <option key={p} value={p}>
                {LION_PART_LABEL[p]}
              </option>
            ))}
          </Select>
        </div>
        <div className="flex flex-col gap-2">
          <p className="t-body-bold text-ink-700">
            관심 분야 <span className="text-[12px] font-normal text-ink-400">(1~3개)</span>
          </p>
          <div className="flex flex-wrap gap-2">
            {INTEREST_OPTIONS.map((v) => (
              <Chip key={v} selected={interests.includes(v)} onClick={() => toggle(v)}>
                {v}
              </Chip>
            ))}
          </div>
        </div>
        <Input label="한 줄 소개" value={intro} onChange={(e) => setIntro(e.target.value)} counter={{ value: intro.length, max: 100 }} />
        <div className="grid grid-cols-2 gap-3">
          <Input label="주전공" value={major} onChange={(e) => setMajor(e.target.value)} />
          <Input label="복수·부전공" value={minor} onChange={(e) => setMinor(e.target.value)} placeholder="선택" />
        </div>
        <Input label="활동 지역" value={loc} onChange={(e) => setLoc(e.target.value)} placeholder="예) 서울 동작" />
      </div>
    </Modal>
  );
}

/** ◇[O] E-13 연락처 편집 */
function ContactModal({ open, onClose, detail, onSave }: { open: boolean; onClose: () => void; detail: ProfileDetail; onSave: (email: ProfileDetail["email"], phone: ProfileDetail["phone"]) => void }) {
  const [email, setEmail] = useState(detail.email?.value ?? "");
  const [ev, setEv] = useState(detail.email?.visibility ?? "CONNECTED");
  const [phone, setPhone] = useState(detail.phone?.value ?? "");
  const [pv, setPv] = useState(detail.phone?.visibility ?? "CONNECTED");
  const VIS = (
    <>
      <option value="ALL">전체 공개</option>
      <option value="CONNECTED">연결된 동문만</option>
      <option value="ME">비공개</option>
    </>
  );
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="연락처 편집"
      width={520}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button onClick={() => onSave(email ? { value: email, visibility: ev } : undefined, phone ? { value: phone, visibility: pv } : undefined)}>저장</Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="grid grid-cols-[1fr_160px] gap-3">
          <Input label="이메일" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          <Select label="공개 범위" value={ev} onChange={(e) => setEv(e.target.value as "ALL")}>
            {VIS}
          </Select>
        </div>
        <div className="grid grid-cols-[1fr_160px] gap-3">
          <Input label="전화번호 (선택)" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="010-0000-0000" />
          <Select label="공개 범위" value={pv} onChange={(e) => setPv(e.target.value as "ALL")}>
            {VIS}
          </Select>
        </div>
        <p className="rounded-[8px] bg-bg-50 px-4 py-3 text-[12px] leading-5 text-ink-500">
          알림 메일은 항상 가입한 구글 이메일로 가요.
          <br />
          공개 범위: 전체 공개 / 연결된 동문만(커피챗·메시지로 연결된 동문) / 비공개
        </p>
      </div>
    </Modal>
  );
}

/** ◇[O] E-14 대표 기술 편집 — ★로 대표 기술 최대 5개 */
function SkillsModal({ open, onClose, rep, skills, onSave }: { open: boolean; onClose: () => void; rep: string[]; skills: string[]; onSave: (rep: string[], skills: string[]) => void }) {
  const [all, setAll] = useState<string[]>([...rep, ...skills]);
  const [stars, setStars] = useState<string[]>(rep);
  const [input, setInput] = useState("");
  const add = () => {
    const v = input.trim();
    if (!v || v.length > 20 || all.includes(v) || all.length >= 20) return;
    setAll((s) => [...s, v]);
    setInput("");
  };
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="대표 기술 편집"
      width={560}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button onClick={() => onSave(all.filter((s) => stars.includes(s)), all.filter((s) => !stars.includes(s)))}>저장</Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap gap-2">
          {all.map((s) => {
            const on = stars.includes(s);
            return (
              <span key={s} className={cn("inline-flex items-center gap-1 rounded-[4px] px-2 py-1 text-[13px]", on ? "bg-part-be-bg font-semibold text-primary" : "bg-bg-100 text-ink-700")}>
                <button type="button" aria-label={on ? "대표 해제" : "대표로"} onClick={() => setStars((st) => (on ? st.filter((x) => x !== s) : st.length >= 5 ? st : [...st, s]))}>
                  <Icon name="star-filled" size={12} className={on ? "" : "opacity-30"} />
                </button>
                {s}
                <button type="button" aria-label="삭제" onClick={() => { setAll((a) => a.filter((x) => x !== s)); setStars((st) => st.filter((x) => x !== s)); }} className="ml-1 text-ink-400">
                  ✕
                </button>
              </span>
            );
          })}
        </div>
        <Input placeholder="태그 입력 후 Enter (0~20개, 1~20자)" value={input} onChange={(e) => setInput(e.target.value)} onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), add())} counter={{ value: all.length, max: 20 }} />
        <p className="text-[12px] text-ink-500">★로 대표 기술 최대 5개 · 대표 기술은 프로필 상단과 동문 카드에 보여요 ({stars.length}/5)</p>
      </div>
    </Modal>
  );
}

/** ◇[O] E-15 외부 링크 편집 */
function LinksModal({ open, onClose, links, onSave }: { open: boolean; onClose: () => void; links: ProfileDetail["links"]; onSave: (l: ProfileDetail["links"]) => void }) {
  const [rows, setRows] = useState(links.length ? links : [{ label: "깃허브", url: "", visibility: "ALL" as const }]);
  const [vis, setVis] = useState<"ALL" | "CONNECTED" | "ME">(links[0]?.visibility ?? "ALL");
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="외부 링크 편집"
      width={560}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button onClick={() => onSave(rows.filter((r) => r.url.trim()).map((r) => ({ ...r, visibility: vis })))}>저장</Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <Select label="외부 링크 공개" value={vis} onChange={(e) => setVis(e.target.value as "ALL")}>
          <option value="ALL">전체 공개</option>
          <option value="CONNECTED">연결된 동문만</option>
          <option value="ME">비공개</option>
        </Select>
        {rows.map((r, i) => (
          <div key={i} className="grid grid-cols-[140px_1fr_auto] items-end gap-2">
            <Select label={i === 0 ? "종류" : undefined} value={r.label} onChange={(e) => setRows((s) => s.map((x, j) => (j === i ? { ...x, label: e.target.value } : x)))}>
              {["깃허브", "링크드인", "포트폴리오", "블로그", "기타"].map((t) => (
                <option key={t}>{t}</option>
              ))}
            </Select>
            <Input label={i === 0 ? "URL" : undefined} placeholder="github.com/…" value={r.url} onChange={(e) => setRows((s) => s.map((x, j) => (j === i ? { ...x, url: e.target.value } : x)))} />
            <button type="button" aria-label="삭제" onClick={() => setRows((s) => s.filter((_, j) => j !== i))} className="mb-2.5 text-ink-400 hover:text-ink-900">
              ✕
            </button>
          </div>
        ))}
        {rows.length < 5 && (
          <Button size="S" variant="secondary" className="w-fit" onClick={() => setRows((s) => [...s, { label: "기타", url: "", visibility: vis }])}>
            + 링크 추가
          </Button>
        )}
        <p className="text-[12px] text-ink-500">최대 5개까지 추가할 수 있어요</p>
      </div>
    </Modal>
  );
}

function MySection({ title, onEdit, children }: { title: string; onEdit: () => void; children: React.ReactNode }) {
  return (
    <section className="flex flex-col gap-3.5">
      <div className="flex items-center justify-between">
        <h2 className="t-body-bold text-ink-900">{title}</h2>
        <button type="button" onClick={onEdit} aria-label={`${title} 편집`} className="flex items-center gap-1 rounded px-1 py-0.5 text-[12px] text-ink-700 hover:bg-bg-100">
          <Icon name="edit" size={14} /> 편집
        </button>
      </div>
      {children}
    </section>
  );
}

function ItemRow({ title, sub, meta }: { title: string; sub?: string; meta?: string }) {
  return (
    <li className="flex items-start gap-3.5">
      <span className="grid size-11 shrink-0 place-items-center rounded-[8px] bg-bg-100 text-ink-400">
        <Icon name="image" size={18} />
      </span>
      <div className="flex flex-col gap-0.5">
        <span className="t-label1 text-ink-900">{title}</span>
        {sub && <span className="t-caption1 text-ink-700">{sub}</span>}
        {meta && <span className="text-[12px] text-ink-400">{meta}</span>}
      </div>
    </li>
  );
}
