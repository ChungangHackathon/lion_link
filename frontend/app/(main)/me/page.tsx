"use client";

import { useRef, useState } from "react";
import { cn } from "@/lib/cn";
import { INTEREST_OPTIONS, INTEREST_CODE, LION_PART_LABEL, PROFILE_STATUS_LABEL, type InterestCode, type ProfileStatus } from "@/lib/types";
import { errorMessage } from "@/lib/api/client";
import { receiveBadge, type LinkType } from "@/lib/api/alumni";
import { educationRows, sectionsToDetail } from "@/lib/api/alumniView";
import { completionOf, isEmail, isPhone, linkLabel, LINK_TYPES, PHOTO_RULE, photoError, SCOPE_LABEL, type BasicInfo, type MyProfile, type Scope } from "@/lib/api/myProfile";
import { isHttpUrl, normalizeLink, type ApiLionPart } from "@/lib/api/profile";
import { useApi } from "@/lib/api/useApi";
import { useCoffeeSettingsService } from "@/lib/api/useCoffeeSettingsService";
import { useMyProfileService } from "@/lib/api/useMyProfileService";
import { Avatar } from "@/components/ui/Avatar";
import { AvailabilityBadge } from "@/components/ui/Badge";
import { Button, Chip } from "@/components/ui/Button";
import { Icon, type IconName } from "@/components/ui/Icon";
import { Input, Select, Textarea } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Card, EmptyState, Skeleton } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";
import { isImageUrl } from "@/components/domain/PostRow";
import { SectionListModal } from "@/components/domain/ProfileSectionEditor";
import type { SectionKey } from "@/lib/api/profileSections";

type Section = "basic" | "about" | "contact" | "career" | "education" | "lion" | "project" | "skills" | "links" | "certs";
/** 목록형 섹션 (PROFILE-11) → 한 팝업에서 편집하는 API 섹션들 */
const LIST_SECTIONS: Partial<Record<Section, { title: string; keys: SectionKey[] }>> = {
  career: { title: "경력 편집", keys: ["careers"] },
  education: { title: "학력 편집", keys: ["educations"] },
  lion: { title: "멋사 활동 편집", keys: ["lionActivities", "lionProjects"] },
  project: { title: "프로젝트 편집", keys: ["projects"] },
  certs: { title: "자격증·수상·대외활동·언어 편집", keys: ["certifications", "awards", "activities", "languages"] },
};
const PARTS: ApiLionPart[] = ["PLANNING", "DESIGN", "FRONTEND", "BACKEND"];
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
 * + 프로필 항목 추가 → E-07. (PROFILE-02, 04~10)
 */
export default function MyProfilePage() {
  const service = useMyProfileService();
  const { data, error, reload, setData } = useApi(() => service.get(), [service]);
  if (error && !data) return <EmptyState title="프로필을 불러오지 못했어요" description={errorMessage(error)} action={{ label: "다시 시도", onClick: reload }} />;
  if (!data) return <Skeleton className="h-[600px] w-full" />;
  return <MyProfileView profile={data} update={(patch) => setData((p) => (p ? { ...p, ...patch } : p))} />;
}

function MyProfileView({ profile: p, update }: { profile: MyProfile; update: (patch: Partial<MyProfile>) => void }) {
  const toast = useToast();
  const service = useMyProfileService();
  const coffeeService = useCoffeeSettingsService();
  const coffee = useApi(() => coffeeService.get(), [coffeeService]).data;
  const [edit, setEdit] = useState<Section | null>(null);
  /** 목록형 섹션을 '추가'로 열면 비어 있는 첫 섹션의 폼부터 연다 */
  const [startAdding, setStartAdding] = useState<SectionKey | undefined>();
  const [addOpen, setAddOpen] = useState(false);
  const [saved, setSaved] = useState<string | null>(null);
  const [photoBusy, setPhotoBusy] = useState(false);
  const photoInput = useRef<HTMLInputElement>(null);

  const part = p.lionParts[0] ?? "ETC";
  const sections = sectionsToDetail(p);
  const educations = educationRows(p);
  const completion = completionOf(p);
  const nextStep = !p.bio ? { text: `자기소개를 쓰면 ${Math.min(100, completion + 14)}%가 돼요` } : !p.careers.length ? { text: `경력을 추가하면 ${Math.min(100, completion + 14)}%가 돼요` } : !p.projects.length ? { text: `프로젝트를 추가하면 ${Math.min(100, completion + 7)}%가 돼요` } : null;
  const badge = coffee ? receiveBadge(coffee.status) : null;
  const current = p.careers.find((c) => c.isCurrent);
  const subline = current ? [current.company, current.jobTitle].filter(Boolean).join(" · ") : PROFILE_STATUS_LABEL[p.status];

  const done = (label: string) => {
    setEdit(null);
    setSaved(label);
    toast("저장했어요");
    setTimeout(() => setSaved(null), 2000);
  };

  /** 저장 공통: 실패하면 안내만 하고 창은 열어 둔다 */
  const save = async (label: string, call: () => Promise<Partial<MyProfile>>) => {
    try {
      update(await call());
      done(label);
      return true;
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
      return false;
    }
  };

  const openSection = (s: Section, adding = false) => {
    const list = LIST_SECTIONS[s];
    setStartAdding(adding && list && list.keys.every((k) => p[k].length === 0) ? list.keys[0] : undefined);
    setEdit(s);
  };

  const pickPhoto = async (file: File | undefined) => {
    if (!file) return;
    const problem = photoError(file);
    if (problem) return toast(problem, { kind: "error" });
    setPhotoBusy(true);
    try {
      const { photoUrl } = await service.uploadPhoto(file);
      update({ photoUrl });
      toast("프로필 사진을 바꿨어요");
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setPhotoBusy(false);
    }
  };

  const removePhoto = async () => {
    setPhotoBusy(true);
    try {
      await service.deletePhoto();
      update({ photoUrl: null });
      toast("기본 사진으로 바꿨어요");
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setPhotoBusy(false);
    }
  };

  const openPhotoPicker = () => photoInput.current?.click();
  const filled = Math.round((completion / 100) * 9);
  type Quick = { icon: IconName; label: string; target: "photo" | Section };
  const quick = ([
    ...(p.photoUrl ? [] : [{ icon: "image", label: "프로필 사진 올리기", target: "photo" }]),
    ...(p.bio ? [] : [{ icon: "edit", label: "소개 쓰기", target: "about" }]),
    ...(p.skills.length ? [] : [{ icon: "plus", label: "대표 기술 추가", target: "skills" }]),
    ...(p.links.length ? [] : [{ icon: "link", label: "외부 링크 추가", target: "links" }]),
  ] as Quick[]).slice(0, 3);

  return (
    <div className="flex flex-col gap-6">
      <input ref={photoInput} type="file" accept={PHOTO_RULE.accept} hidden aria-label="프로필 사진 고르기" onChange={(e) => { void pickPhoto(e.target.files?.[0]); e.target.value = ""; }} />

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
              <button key={q.label} type="button" onClick={() => (q.target === "photo" ? openPhotoPicker() : setEdit(q.target))} className="flex h-[34px] items-center gap-2 rounded-full border border-line-200 bg-white px-3 t-label2 font-medium text-ink-900 hover:bg-bg-50">
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
          {isImageUrl(p.photoUrl) ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={p.photoUrl} alt={`${p.name} 프로필 사진`} className="size-[140px] rounded-full object-cover" />
          ) : (
            <Avatar name={p.name} part={part} size={96} className="!size-[140px] !text-[52px]" />
          )}
          {photoBusy && <span className="absolute inset-0 grid place-items-center rounded-full bg-white/70 text-[12px] font-semibold text-ink-700">올리는 중</span>}
          <button type="button" aria-label="프로필 사진 바꾸기" onClick={openPhotoPicker} className="absolute bottom-0 right-0 grid size-9 place-items-center rounded-full border border-line-200 bg-white text-ink-900 shadow-[0_1px_3px_rgba(0,0,0,0.12)] hover:bg-bg-50">
            <Icon name="edit" size={18} />
          </button>
          {p.photoUrl && (
            <button type="button" onClick={() => void removePhoto()} className="absolute -bottom-6 left-0 w-full text-center text-[12px] text-ink-500 underline">
              기본 사진으로
            </button>
          )}
        </div>
        <div className="flex min-w-0 flex-1 flex-col gap-3 pt-1">
          <div className="flex items-start justify-between gap-3">
            <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
              <h1 className="t-title text-ink-900">{p.name}</h1>
              <span className="rounded-[5px] bg-part-be-bg px-2.5 py-1 t-label2 text-ink-500">
                {p.generation}기 · {p.lionParts.map((x) => LION_PART_LABEL[x]).join("·")}
              </span>
              {badge && <AvailabilityBadge mode={badge.mode} note={badge.note} small />}
            </div>
            <div className="flex shrink-0 gap-2">
              <Button size="S" variant="secondary" href={`/alumni/${p.userId}?preview=1`} className="h-[34px]" leading={<Icon name="monitor" size={16} />}>
                다른 동문에게 보이는 모습
              </Button>
              <button type="button" onClick={() => setEdit("basic")} className="flex h-[34px] items-center gap-1.5 rounded-[6px] bg-part-be-bg px-3 t-label2 text-primary-strong hover:bg-part-be-line">
                <Icon name="edit" size={16} /> 프로필 편집
              </button>
            </div>
          </div>
          <p className="text-[18px] leading-7 text-ink-700">{p.headline || <span className="text-ink-400">한 줄 소개를 적어보세요</span>}</p>
          <div className="flex items-center gap-2 t-caption1">
            <span className="text-ink-500">대표 기술</span>
            {p.skills.length ? (
              p.skills.map((s) => (
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
              <Icon name="user" size={16} className="text-ink-900" /> {subline}
            </span>
            {p.region && (
              <span className="flex items-center gap-1.5">
                <Icon name="place" size={16} className="text-ink-900" /> {p.region}
              </span>
            )}
            {p.links[0] && (
              <span className="flex items-center gap-1.5">
                <Icon name="link" size={16} className="text-ink-900" /> {p.links[0].url.replace(/^https?:\/\//, "")}
              </span>
            )}
          </div>
        </div>
      </Card>

      {saved && <p className="rounded-[8px] bg-part-be-bg px-4 py-2 text-[13px] text-primary-strong">{saved} 저장 완료 · 프로필에 바로 반영됐어요</p>}

      {/* 본문 — 추가한 항목만 한 카드 안에 */}
      <Card className="flex flex-col gap-9 rounded-[10px] p-7">
        {p.bio && (
          <MySection title="소개" onEdit={() => setEdit("about")}>
            <p className="whitespace-pre-line text-[14px] leading-6 text-ink-700">{p.bio}</p>
          </MySection>
        )}
        {sections.careers.length > 0 && (
          <MySection title="경력" onEdit={() => openSection("career")} note={p.visibility.company === "PRIVATE" ? "회사명 비공개" : undefined}>
            <ul className="flex flex-col gap-4">
              {sections.careers.map((c, i) => (
                <ItemRow key={i} title={c.company} sub={`${c.role}${c.type ? ` · ${c.type}` : ""}`} meta={c.from ? `${c.from} ~ ${c.to}${c.duration ? ` · ${c.duration}` : ""}` : "시작 시점을 입력해주세요"} />
              ))}
            </ul>
          </MySection>
        )}
        {educations.length > 0 && (
          <MySection title="학력" onEdit={() => openSection("education")}>
            <ul className="flex flex-col gap-4">
              {educations.map((e, i) => (
                <ItemRow key={i} title={e.school} sub={[e.major, e.degree].filter(Boolean).join(" · ")} meta={e.from ? `${e.from} ~ ${e.to}` : undefined} />
              ))}
            </ul>
          </MySection>
        )}
        {sections.lion.length > 0 && (
          <MySection title="멋사 활동" onEdit={() => openSection("lion")}>
            <ul className="flex flex-col gap-2">
              {sections.lion.map((l, i) => (
                <li key={i} className="rounded-[8px] border border-line-200 bg-bg-50 px-[15px] py-3 t-body-bold font-medium text-ink-900">
                  {l.generation}기 {l.role}
                  {l.projects.length > 0 && <span className="ml-2 text-[12px] font-normal text-ink-500">프로젝트 {l.projects.map((x) => x.name).join(", ")}</span>}
                </li>
              ))}
            </ul>
          </MySection>
        )}
        {sections.projects.length > 0 && (
          <MySection title="프로젝트" onEdit={() => openSection("project")}>
            <ul className="flex flex-col gap-4">
              {sections.projects.map((pj, i) => (
                <ItemRow key={i} title={pj.name} sub={pj.type} meta={pj.from ? `${pj.from} ~ ${pj.to}` : undefined} />
              ))}
            </ul>
          </MySection>
        )}
        {sections.certs.length > 0 && (
          <MySection title="자격증·수상·대외활동·언어" onEdit={() => openSection("certs")}>
            <dl className="flex flex-col gap-2 text-[14px]">
              {sections.certs.map((c, i) => (
                <div key={i} className="flex gap-4">
                  <dt className="w-16 text-ink-500">{c.kind}</dt>
                  <dd className="text-ink-900">{c.text}</dd>
                </div>
              ))}
            </dl>
          </MySection>
        )}
        {p.links.length > 0 && (
          <MySection title="외부 링크" onEdit={() => setEdit("links")} note={`${SCOPE_LABEL[p.visibility.links]}`}>
            <ul className="flex flex-col gap-2">
              {p.links.map((l, i) => (
                <li key={i} className="flex items-center gap-2 text-[14px] text-ink-700">
                  <Icon name="link" size={18} className="text-ink-700" /> {linkLabel(l.type)} ·{" "}
                  <a href={l.url} target="_blank" rel="noreferrer" className="font-medium text-ink-900 hover:underline">
                    {l.url.replace(/^https?:\/\//, "")}
                  </a>
                </li>
              ))}
            </ul>
          </MySection>
        )}
        <MySection title="연락처" onEdit={() => setEdit("contact")}>
          <div className="flex flex-col gap-1.5">
            <ContactLine icon="mail" value={p.contact.email} empty="아직 이메일이 없어요" scope={p.visibility.email} />
            {p.contact.phone && <ContactLine icon="chat" value={p.contact.phone} scope={p.visibility.phone} />}
          </div>
        </MySection>
        <button type="button" onClick={() => setAddOpen(true)} className="flex h-[46px] items-center justify-center gap-2 rounded-[8px] border border-dashed border-line-300 text-[14px] font-medium text-ink-500 hover:bg-bg-50">
          <Icon name="plus" size={16} /> 프로필 항목 추가
        </button>
      </Card>

      {/* ★[O] E-07 프로필 항목 추가 */}
      <Modal open={addOpen} onClose={() => setAddOpen(false)} title="프로필 항목 추가" description="추가한 항목만 프로필에 보여요" width={560}>
        <ul className="grid grid-cols-2 gap-2">
          {ADDABLE.map((a) => {
            const added = a.key === "about" ? !!p.bio : a.key === "career" ? p.careers.length > 0 : a.key === "education" ? p.educations.length > 0 : a.key === "lion" ? p.lionActivities.length > 0 : a.key === "project" ? p.projects.length > 0 : a.key === "skills" ? p.skills.length > 0 : a.key === "links" ? p.links.length > 0 : sections.certs.length > 0;
            return (
              <li key={a.key}>
                <button type="button" onClick={() => { setAddOpen(false); openSection(a.key, !added); }} className={cn("flex w-full items-center justify-between rounded-[8px] border px-4 py-3 text-left hover:bg-bg-50", added ? "border-line-200" : "border-line-300")}>
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
      </Modal>

      {/* 목록형 섹션 편집 (PROFILE-11) */}
      {edit && LIST_SECTIONS[edit] && (
        <SectionListModal
          title={LIST_SECTIONS[edit]!.title}
          sections={LIST_SECTIONS[edit]!.keys}
          profile={p}
          service={service}
          startAdding={startAdding}
          onClose={() => setEdit(null)}
          onChanged={update}
        />
      )}

      {/* E-12 기본 정보 편집 (PROFILE-04) */}
      {edit === "basic" && (
        <BasicEditModal
          profile={p}
          onClose={() => setEdit(null)}
          onPhoto={openPhotoPicker}
          onSave={(body) => save("기본 정보", () => service.updateBasic(body))}
        />
      )}

      {/* E-10 소개 편집 (PROFILE-06) */}
      {edit === "about" && (
        <TextModal onClose={() => setEdit(null)} title="소개 편집" label="자기소개" value={p.bio ?? ""} max={1000} rows={8} placeholder="어떤 일을 하고 있는지, 사자들에게 어떤 이야기를 해줄 수 있는지 적어주세요." onSave={(v) => save("소개", () => service.updateBio(v || null))} />
      )}

      {/* E-13 연락처 (PROFILE-09 + 10) */}
      {edit === "contact" && (
        <ContactModal
          profile={p}
          onClose={() => setEdit(null)}
          onSave={(email, phone, emailScope, phoneScope) =>
            save("연락처", async () => {
              const contact = await service.updateContact({ email, phone });
              const { visibility } = await service.updateVisibility({ email: emailScope, phone: phoneScope });
              return { contact, visibility };
            })
          }
        />
      )}

      {/* E-14 대표 기술 (PROFILE-07) */}
      {edit === "skills" && <SkillsModal skills={p.skills} onClose={() => setEdit(null)} onSave={(skills) => save("대표 기술", () => service.updateSkills(skills))} />}

      {/* E-15 외부 링크 (PROFILE-08 + 10) */}
      {edit === "links" && (
        <LinksModal
          links={p.links}
          scope={p.visibility.links}
          onClose={() => setEdit(null)}
          onSave={(links, scope) =>
            save("외부 링크", async () => {
              const res = await service.updateLinks(links);
              const { visibility } = scope !== p.visibility.links ? await service.updateVisibility({ links: scope }) : { visibility: p.visibility };
              return { links: res.links, visibility };
            })
          }
        />
      )}
    </div>
  );
}

function ContactLine({ icon, value, empty, scope }: { icon: IconName; value: string | null; empty?: string; scope: Scope }) {
  return (
    <p className="flex items-center gap-2 text-[14px] font-medium text-ink-900">
      <Icon name={icon} size={18} className="text-ink-700" /> {value ?? empty}
      {value && (
        <span className="ml-1 flex items-center gap-1 text-[12px] font-normal text-ink-500">
          <Icon name="lock" size={12} /> {SCOPE_LABEL[scope]}
        </span>
      )}
    </p>
  );
}

/* ---------- Edit modals ---------- */

function useSaving() {
  const [busy, setBusy] = useState(false);
  const run = async (fn: () => Promise<unknown>) => {
    setBusy(true);
    try {
      await fn();
    } finally {
      setBusy(false);
    }
  };
  return { busy, run };
}

function TextModal({ onClose, title, label, value, max, rows, placeholder, onSave }: { onClose: () => void; title: string; label: string; value: string; max: number; rows: number; placeholder?: string; onSave: (v: string) => Promise<unknown> }) {
  const [v, setV] = useState(value);
  const { busy, run } = useSaving();
  return (
    <Modal
      open
      onClose={onClose}
      title={title}
      width={560}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button disabled={v.trim().length > max} loading={busy} onClick={() => run(() => onSave(v.trim()))}>
            저장
          </Button>
        </>
      }
    >
      <Textarea label={label} rows={rows} value={v} onChange={(e) => setV(e.target.value)} counter={{ value: v.length, max }} placeholder={placeholder} help="비워서 저장하면 소개 섹션이 프로필에서 사라져요" />
    </Modal>
  );
}

/** ◇[O] E-12 섹션 편집 팝업 / 상단 소개 카드 (PROFILE-04). 이름·기수는 여기서 바꿀 수 없다 */
function BasicEditModal({ profile: p, onClose, onPhoto, onSave }: { profile: MyProfile; onClose: () => void; onPhoto: () => void; onSave: (body: BasicInfo) => Promise<unknown> }) {
  const toast = useToast();
  const [status, setStatus] = useState<ProfileStatus>(p.status);
  const [parts, setParts] = useState<ApiLionPart[]>(p.lionParts);
  const [interests, setInterests] = useState<InterestCode[]>(p.interests);
  const [headline, setHeadline] = useState(p.headline ?? "");
  const [major, setMajor] = useState(p.major ?? "");
  const [minor, setMinor] = useState(p.minor ?? "");
  const [region, setRegion] = useState(p.region ?? "");
  const { busy, run } = useSaving();
  const togglePart = (x: ApiLionPart) => setParts((s) => (s.includes(x) ? s.filter((y) => y !== x) : s.length >= 2 ? (toast("멋사 파트는 최대 2개까지 고를 수 있어요", { kind: "error" }), s) : [...s, x]));
  const toggleInterest = (code: InterestCode) => setInterests((s) => (s.includes(code) ? s.filter((x) => x !== code) : s.length >= 3 ? (toast("관심 분야는 최대 3개까지 고를 수 있어요", { kind: "error" }), s) : [...s, code]));
  const valid = parts.length >= 1 && interests.length >= 1 && headline.trim().length <= 100 && major.trim().length <= 30 && minor.trim().length <= 30 && region.trim().length <= 20;
  return (
    <Modal
      open
      onClose={onClose}
      title="기본 정보 편집"
      width={640}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button
            disabled={!valid}
            loading={busy}
            onClick={() =>
              run(() =>
                onSave({
                  headline: headline.trim() || null,
                  major: major.trim() || null,
                  minor: minor.trim() || null,
                  region: region.trim() || null,
                  status,
                  lionParts: parts,
                  interests,
                }),
              )
            }
          >
            저장
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="flex items-center gap-4">
          {isImageUrl(p.photoUrl) ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={p.photoUrl} alt="" className="size-20 rounded-full object-cover" />
          ) : (
            <Avatar name={p.name} part={parts[0]} size={80} />
          )}
          <Button size="S" variant="secondary" onClick={onPhoto}>
            사진 변경
          </Button>
          <span className="text-[12px] text-ink-500">jpg·png·webp, 5MB 이하 · 고르면 바로 저장돼요</span>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Input label="이름" value={p.name} readOnly help="이름·기수는 운영진에게 고쳐달라고 해주세요" />
          <Input label="기수" value={`${p.generation}기`} readOnly />
        </div>
        <Select label="현재 상태" value={status} onChange={(e) => setStatus(e.target.value as ProfileStatus)}>
          {(Object.keys(PROFILE_STATUS_LABEL) as ProfileStatus[]).map((s) => (
            <option key={s} value={s}>
              {PROFILE_STATUS_LABEL[s]}
            </option>
          ))}
        </Select>
        <div className="flex flex-col gap-2">
          <p className="t-body-bold text-ink-700">
            멋사 파트 <span className="text-[12px] font-normal text-ink-400">(1~2개)</span>
          </p>
          <div className="flex flex-wrap gap-2">
            {PARTS.map((x) => (
              <Chip key={x} selected={parts.includes(x)} onClick={() => togglePart(x)}>
                {LION_PART_LABEL[x]}
              </Chip>
            ))}
          </div>
        </div>
        <div className="flex flex-col gap-2">
          <p className="t-body-bold text-ink-700">
            관심 분야 <span className="text-[12px] font-normal text-ink-400">(1~3개)</span>
          </p>
          <div className="flex flex-wrap gap-2">
            {INTEREST_OPTIONS.map((v) => (
              <Chip key={v} selected={interests.includes(INTEREST_CODE[v])} onClick={() => toggleInterest(INTEREST_CODE[v])}>
                {v}
              </Chip>
            ))}
          </div>
        </div>
        <Input label="한 줄 소개" value={headline} onChange={(e) => setHeadline(e.target.value)} counter={{ value: headline.length, max: 100 }} error={headline.trim().length > 100 ? "100자 이내로 적어주세요" : undefined} />
        <div className="grid grid-cols-2 gap-3">
          <Input label="주전공" value={major} onChange={(e) => setMajor(e.target.value)} placeholder="선택" error={major.trim().length > 30 ? "30자 이내" : undefined} />
          <Input label="복수·부전공" value={minor} onChange={(e) => setMinor(e.target.value)} placeholder="선택" error={minor.trim().length > 30 ? "30자 이내" : undefined} />
        </div>
        <Input label="활동 지역" value={region} onChange={(e) => setRegion(e.target.value)} placeholder="예) 서울 동작" counter={{ value: region.length, max: 20 }} error={region.trim().length > 20 ? "20자 이내로 적어주세요" : undefined} />
      </div>
    </Modal>
  );
}

const SCOPES: Scope[] = ["PUBLIC", "CONNECTED", "PRIVATE"];

/** ◇[O] E-13 연락처 편집 (PROFILE-09 + 공개 범위 PROFILE-10) */
function ContactModal({ profile: p, onClose, onSave }: { profile: MyProfile; onClose: () => void; onSave: (email: string, phone: string | null, emailScope: Scope, phoneScope: Scope) => Promise<unknown> }) {
  const [email, setEmail] = useState(p.contact.email ?? "");
  const [ev, setEv] = useState<Scope>(p.visibility.email);
  const [phone, setPhone] = useState(p.contact.phone ?? "");
  const [pv, setPv] = useState<Scope>(p.visibility.phone);
  const { busy, run } = useSaving();
  const emailError = email.trim() && !isEmail(email.trim()) ? "이메일 형식을 확인해주세요" : !email.trim() ? "이메일은 비울 수 없어요" : undefined;
  const phoneError = phone.trim() && !isPhone(phone.trim()) ? "010-0000-0000 형식으로 적어주세요" : undefined;
  return (
    <Modal
      open
      onClose={onClose}
      title="연락처 편집"
      width={520}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button disabled={!!emailError || !!phoneError} loading={busy} onClick={() => run(() => onSave(email.trim(), phone.trim() || null, ev, pv))}>
            저장
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="grid grid-cols-[1fr_160px] gap-3">
          <Input label="이메일" type="email" value={email} onChange={(e) => setEmail(e.target.value)} error={emailError} />
          <Select label="공개 범위" value={ev} onChange={(e) => setEv(e.target.value as Scope)}>
            {SCOPES.map((s) => (
              <option key={s} value={s}>
                {SCOPE_LABEL[s]}
              </option>
            ))}
          </Select>
        </div>
        <div className="grid grid-cols-[1fr_160px] gap-3">
          <Input label="전화번호 (선택)" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="010-0000-0000" error={phoneError} />
          <Select label="공개 범위" value={pv} onChange={(e) => setPv(e.target.value as Scope)}>
            {SCOPES.map((s) => (
              <option key={s} value={s}>
                {SCOPE_LABEL[s]}
              </option>
            ))}
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

/** ◇[O] E-14 대표 기술 편집 (PROFILE-07) — 최대 5개, 태그당 1~20자. 순서대로 보여요 */
function SkillsModal({ skills, onClose, onSave }: { skills: string[]; onClose: () => void; onSave: (skills: string[]) => Promise<unknown> }) {
  const [all, setAll] = useState<string[]>(skills);
  const [input, setInput] = useState("");
  const { busy, run } = useSaving();
  const v = input.trim();
  const inputError = v.length > 20 ? "20자 이내로 적어주세요" : all.length >= 5 && v ? "대표 기술은 5개까지예요" : undefined;
  const add = () => {
    if (!v || v.length > 20 || all.length >= 5) return;
    if (!all.includes(v)) setAll((s) => [...s, v]);
    setInput("");
  };
  return (
    <Modal
      open
      onClose={onClose}
      title="대표 기술 편집"
      width={560}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button loading={busy} onClick={() => run(() => onSave(all))}>
            저장
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap gap-2">
          {all.length === 0 && <span className="text-[13px] text-ink-400">아직 없어요. 아래에 입력하고 Enter를 눌러주세요.</span>}
          {all.map((s) => (
            <span key={s} className="inline-flex items-center gap-1 rounded-[4px] bg-part-be-bg px-2 py-1 text-[13px] font-semibold text-primary">
              {s}
              <button type="button" aria-label={`${s} 삭제`} onClick={() => setAll((a) => a.filter((x) => x !== s))} className="ml-1 text-ink-400">
                ✕
              </button>
            </span>
          ))}
        </div>
        <Input placeholder="기술 입력 후 Enter (최대 5개, 1~20자)" value={input} onChange={(e) => setInput(e.target.value)} onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), add())} counter={{ value: all.length, max: 5 }} error={inputError} />
        <p className="text-[12px] text-ink-500">대표 기술은 프로필 상단과 동문 카드에 입력한 순서대로 보여요. 모두 지우고 저장하면 섹션이 사라져요.</p>
      </div>
    </Modal>
  );
}

/** ◇[O] E-15 외부 링크 편집 (PROFILE-08 + 공개 범위 PROFILE-10) */
function LinksModal({ links, scope, onClose, onSave }: { links: MyProfile["links"]; scope: Scope; onClose: () => void; onSave: (links: MyProfile["links"], scope: Scope) => Promise<unknown> }) {
  const [rows, setRows] = useState<{ type: LinkType; url: string }[]>(links.length ? links : [{ type: "GITHUB", url: "" }]);
  const [vis, setVis] = useState<Scope>(scope);
  const { busy, run } = useSaving();
  const normalized = rows.map((r) => ({ type: r.type, url: normalizeLink(r.url) ?? "" }));
  const bad = normalized.some((r) => r.url && !isHttpUrl(r.url));
  return (
    <Modal
      open
      onClose={onClose}
      title="외부 링크 편집"
      width={560}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button disabled={bad} loading={busy} onClick={() => run(() => onSave(normalized.filter((r) => r.url), vis))}>
            저장
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <Select label="외부 링크 공개" value={vis} onChange={(e) => setVis(e.target.value as Scope)}>
          {SCOPES.map((s) => (
            <option key={s} value={s}>
              {SCOPE_LABEL[s]}
            </option>
          ))}
        </Select>
        {rows.map((r, i) => {
          const url = normalizeLink(r.url);
          return (
            <div key={i} className="grid grid-cols-[140px_1fr_auto] items-start gap-2">
              <Select label={i === 0 ? "종류" : undefined} value={r.type} onChange={(e) => setRows((s) => s.map((x, j) => (j === i ? { ...x, type: e.target.value as LinkType } : x)))}>
                {LINK_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </Select>
              <Input label={i === 0 ? "URL" : undefined} placeholder="github.com/…" value={r.url} onChange={(e) => setRows((s) => s.map((x, j) => (j === i ? { ...x, url: e.target.value } : x)))} error={url && !isHttpUrl(url) ? "주소를 확인해주세요" : undefined} />
              <button type="button" aria-label="링크 삭제" onClick={() => setRows((s) => s.filter((_, j) => j !== i))} className={cn("text-ink-400 hover:text-ink-900", i === 0 ? "mt-9" : "mt-2.5")}>
                ✕
              </button>
            </div>
          );
        })}
        {rows.length < 5 && (
          <Button size="S" variant="secondary" className="w-fit" onClick={() => setRows((s) => [...s, { type: "ETC", url: "" }])}>
            + 링크 추가
          </Button>
        )}
        <p className="text-[12px] text-ink-500">최대 5개까지 추가할 수 있어요. https://는 안 붙여도 돼요.</p>
      </div>
    </Modal>
  );
}

function MySection({ title, onEdit, note, children }: { title: string; onEdit: () => void; note?: string; children: React.ReactNode }) {
  return (
    <section className="flex flex-col gap-3.5">
      <div className="flex items-center justify-between">
        <h2 className="flex items-center gap-2 t-body-bold text-ink-900">
          {title}
          {note && (
            <span className="flex items-center gap-1 text-[12px] font-normal text-ink-500">
              <Icon name="lock" size={12} /> {note}
            </span>
          )}
        </h2>
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
