"use client";

import { useEffect, useRef, useState } from "react";
import { useSession } from "@/lib/session";
import { cn } from "@/lib/cn";
import { INTEREST_OPTIONS, INTEREST_CODE, LION_PART_LABEL, PROFILE_STATUS_LABEL, type InterestCode, type ProfileStatus } from "@/lib/types";
import { errorMessage } from "@/lib/api/client";
import { receiveBadge, type LinkType } from "@/lib/api/alumni";
import { detailFromMine, staffLabelOf } from "@/lib/api/alumniView";
import { completionOf, isEmail, isPhone, isWorking, LINK_TYPES, PHOTO_RULE, photoError, SCOPE_LABEL, workLine, workOf, type BasicInfoRequest, type MyProfile, type Scope } from "@/lib/api/myProfile";
import { isHttpUrl, normalizeLink, type ApiLionPart } from "@/lib/api/profile";
import { useApi } from "@/lib/api/useApi";
import { useCoffeeSettingsService } from "@/lib/api/useCoffeeSettingsService";
import { useMyProfileService } from "@/lib/api/useMyProfileService";
import { Avatar } from "@/components/ui/Avatar";
import { AvailabilityBadge, PartTag, Tag } from "@/components/ui/Badge";
import { Button, Chip } from "@/components/ui/Button";
import { Icon, type IconName } from "@/components/ui/Icon";
import { Input, Select, Textarea } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Card, EmptyState, Skeleton } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";
import { isImageUrl } from "@/components/domain/PostRow";
import { SectionListModal } from "@/components/domain/ProfileSectionEditor";
import { EducationList, ProfileSections } from "@/components/domain/ProfileSections";
import { LION_PARTS, type SectionKey } from "@/lib/api/profileSections";

type Section = "basic" | "about" | "contact" | "career" | "education" | "lion" | "project" | "skills" | "links" | "certs";
/** 목록형 섹션 (PROFILE-11) → 한 팝업에서 편집하는 API 섹션들 */
const LIST_SECTIONS: Partial<Record<Section, { title: string; keys: SectionKey[] }>> = {
  career: { title: "경력 편집", keys: ["careers"] },
  education: { title: "학력 편집", keys: ["educations"] },
  lion: { title: "멋사 활동 편집", keys: ["lionActivities", "lionProjects"] },
  project: { title: "프로젝트 편집", keys: ["projects"] },
  certs: { title: "자격증·수상·대외활동·언어 편집", keys: ["certifications", "awards", "activities", "languages"] },
};

/**
 * ★ P-02 마이페이지 / 내 프로필 (= 마이페이지 첫 화면). 다른 동문에게 보이는 모습(P-01)을 그대로 보여주고,
 * 섹션마다 편집(연필) → E-09~E-15 팝업. (PROFILE-02, 04~10)
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
  const [saved, setSaved] = useState<string | null>(null);
  const [photoBusy, setPhotoBusy] = useState(false);
  const photoInput = useRef<HTMLInputElement>(null);

  const part = p.lionParts[0] ?? "ETC";
  // 파트를 바꾸면 상단바 아바타·프로필 메뉴도 따라 바꾼다
  const { setMyParts, refreshMe } = useSession();
  const partsKey = p.lionParts.join();
  useEffect(() => {
    if (partsKey) setMyParts(partsKey.split(",") as typeof p.lionParts);
  }, [partsKey, setMyParts]);
  const detail = detailFromMine(p);
  const completion = completionOf(p);
  const nextStep = !p.bio ? { text: `자기소개를 쓰면 ${Math.min(100, completion + 14)}%가 돼요` } : !p.careers.length ? { text: `경력을 추가하면 ${Math.min(100, completion + 14)}%가 돼요` } : !p.projects.length ? { text: `프로젝트를 추가하면 ${Math.min(100, completion + 7)}%가 돼요` } : null;
  const badge = coffee ? receiveBadge(coffee.status) : null;
  const subline = workLine(workOf(p), p.status);
  const staffLabel = p.isStaff ? staffLabelOf(p.lionActivities) ?? "운영진" : null;

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
      void refreshMe(); // 상단바 사진도 바꾼다
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
      void refreshMe();
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

      {/* 상단 소개 카드 — 동문 프로필(P-01)과 같은 모양에 편집 버튼만 더한다 (E-12) */}
      <Card className="rounded-[10px] p-[30px]">
        <div className="flex gap-[19px]">
          <div className="flex w-[150px] flex-col items-center gap-2">
            <div className="relative size-[150px]">
              {isImageUrl(p.photoUrl) ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={p.photoUrl} alt={`${p.name} 프로필 사진`} className="size-[150px] rounded-full object-cover" />
              ) : (
                <Avatar name={p.name} part={part} size={96} className="!size-[150px] !text-[56px]" />
              )}
              {photoBusy && <span className="absolute inset-0 grid place-items-center rounded-full bg-white/70 text-[12px] font-semibold text-ink-700">올리는 중</span>}
              <button type="button" aria-label="프로필 사진 바꾸기" onClick={openPhotoPicker} className="absolute right-0 top-1 grid size-9 place-items-center rounded-full border border-line-200 bg-white text-ink-900 shadow-[0_1px_3px_rgba(0,0,0,0.12)] hover:bg-bg-50">
                <Icon name="edit" size={18} />
              </button>
            </div>
            {badge && <AvailabilityBadge mode={badge.mode} note={badge.note} className="-mt-5 bg-white" />}
            {p.photoUrl && (
              <button type="button" onClick={() => void removePhoto()} className="text-[12px] text-ink-500 underline">
                기본 사진으로
              </button>
            )}
          </div>
          <div className="flex min-w-0 flex-1 flex-col gap-4 pt-6">
            <div className="flex items-start justify-between gap-3">
              <div className="flex flex-wrap items-center gap-2.5">
                <h1 className="text-[28px] font-bold leading-[34px] text-ink-900">{p.name}</h1>
                <PartTag part={part} generation={p.generation} />
                {staffLabel && <span className="rounded-[5px] bg-orange-back px-2 py-[3px] text-[12px] font-semibold text-orange-badge">{staffLabel}</span>}
              </div>
              <button type="button" onClick={() => setEdit("basic")} className="flex h-[34px] shrink-0 items-center gap-1.5 rounded-[6px] bg-part-be-bg px-3 t-label2 text-primary-strong hover:bg-part-be-line">
                <Icon name="edit" size={16} /> 프로필 편집
              </button>
            </div>
            {p.bio && <p className="line-clamp-3 t-body2 text-ink-700">{p.bio}</p>}
          </div>
        </div>

        <div className="mt-6 grid grid-cols-[1fr_300px] gap-6">
          <div className="flex flex-col gap-3 text-[13px]">
            <p className="t-label1 text-ink-900">{p.headline || subline || <span className="text-ink-400">한 줄 소개를 적어보세요</span>}</p>
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-ink-500">대표 기술</span>
              {p.skills.length ? p.skills.map((s) => <Tag key={s}>{s}</Tag>) : <span className="text-ink-400">아직 없어요</span>}
              <button type="button" aria-label="대표 기술 편집" onClick={() => setEdit("skills")} className="rounded p-1 text-ink-500 hover:bg-bg-100 hover:text-ink-900">
                <Icon name="edit" size={14} />
              </button>
            </div>
            {p.region && (
              <p className="flex items-center gap-1 text-ink-700">
                <Icon name="pin" size={14} /> {p.region}
              </p>
            )}
          </div>
          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
              <span className="t-label2 text-ink-500">학력</span>
              <button type="button" aria-label="학력 편집" onClick={() => openSection("education", true)} className="flex items-center gap-1 rounded px-1.5 py-1 text-[12px] text-ink-500 hover:bg-bg-100 hover:text-ink-900">
                <Icon name="edit" size={14} /> {detail.educations.length ? "편집" : "추가"}
              </button>
            </div>
            {detail.educations.length === 0 && !p.minor && <p className="text-[12px] text-ink-400">아직 학력이 없어요</p>}
            <EducationList educations={detail.educations} />
            {/* 복수·부전공은 마이페이지에서만 보여준다 (프로필 기본 정보 minor, 학력 편집에서 저장) */}
            {p.minor && (
              <p className="flex items-center gap-2 text-[13px] text-ink-700">
                <span className="rounded-[4px] bg-bg-100 px-1.5 py-0.5 text-[12px] font-semibold text-ink-500">복수·부전공</span>
                {p.minor}
              </p>
            )}
          </div>
        </div>
      </Card>

      {saved && <p className="rounded-[8px] bg-part-be-bg px-4 py-2 text-[13px] text-primary-strong">{saved} 저장 완료 · 프로필에 바로 반영됐어요</p>}

      {/* 본문 — 동문 프로필과 같은 섹션, 섹션마다 편집 연필 */}
      <ProfileSections detail={detail} onEdit={(s) => (s === "about" || s === "links" || s === "contact" ? setEdit(s) : openSection(s as Section, true))} />

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
          onSave={(body) =>
            save("기본 정보", async () => {
              await service.updateBasic(body);
              // 파트를 바꾸면 가입 기수 멋사 활동 파트도 서버에서 같이 바뀐다. 프로필을 다시 불러온다
              return service.get();
            })
          }
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
      {edit === "skills" && (
        <SkillsModal
          skills={p.skills}
          onClose={() => setEdit(null)}
          onSave={async (skills) => {
            // 창을 닫지 않고 바로 저장한다 (save()는 저장 뒤 창을 닫는다)
            try {
              update(await service.updateSkills(skills));
              return true;
            } catch (e) {
              toast(errorMessage(e), { kind: "error" });
              return false;
            }
          }}
        />
      )}

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
function BasicEditModal({ profile: p, onClose, onPhoto, onSave }: { profile: MyProfile; onClose: () => void; onPhoto: () => void; onSave: (body: BasicInfoRequest) => Promise<unknown> }) {
  const toast = useToast();
  const [status, setStatus] = useState<ProfileStatus>(p.status);
  const [parts, setParts] = useState<ApiLionPart[]>(p.lionParts);
  const [interests, setInterests] = useState<InterestCode[]>(p.interests);
  const [headline, setHeadline] = useState(p.headline ?? "");
  const [region, setRegion] = useState(p.region ?? "");
  // 회사·직무는 경력과 따로 적는다. 재직·창업을 고르면 바로 입력 상자가 열린다
  const [company, setCompany] = useState(p.company ?? "");
  const [jobTitle, setJobTitle] = useState(p.jobTitle ?? "");
  const { busy, run } = useSaving();
  const needsCompany = isWorking(status);
  const companyError = company.trim().length > 30 ? "30자 이내로 적어주세요" : undefined;
  const jobTitleError = jobTitle.trim().length > 30 ? "30자 이내로 적어주세요" : undefined;
  const companyOk = !needsCompany || (!!company.trim() && !!jobTitle.trim() && !companyError && !jobTitleError);
  const toggleInterest = (code: InterestCode) => setInterests((s) => (s.includes(code) ? s.filter((x) => x !== code) : s.length >= 3 ? (toast("관심 분야는 최대 3개까지 고를 수 있어요", { kind: "error" }), s) : [...s, code]));
  const valid = parts.length === 1 && interests.length >= 1 && headline.trim().length <= 100 && region.trim().length <= 20 && companyOk;
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
                  // 전공은 학력 항목에서만 입력한다. 기본 정보 저장 때는 원래 값을 그대로 보낸다
                  major: p.major,
                  minor: p.minor,
                  region: region.trim() || null,
                  status,
                  lionParts: parts,
                  interests,
                  ...(needsCompany ? { company: company.trim(), jobTitle: jobTitle.trim() } : {}),
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
        {/* 가입 때 정보 입력 화면(온보딩)과 같은 모양: 상태 칩 → 재직·창업이면 아래에 회사 입력 상자 */}
        <div className="flex flex-col gap-2">
          <p className="t-body-bold text-ink-700">
            현재 상태 <span className="text-[12px] font-normal text-ink-400">(1개)</span>
          </p>
          <div className="flex flex-wrap gap-2">
            {(Object.keys(PROFILE_STATUS_LABEL) as ProfileStatus[]).map((s) => (
              <Chip key={s} selected={status === s} onClick={() => setStatus(s)}>
                {PROFILE_STATUS_LABEL[s]}
              </Chip>
            ))}
          </div>
        </div>
        {needsCompany && (
          <div className="flex flex-col gap-3 rounded-[8px] border border-line-200 bg-bg-50 p-4">
            <div className="flex gap-3">
              <Input label={status === "FOUNDER" ? "회사·팀 이름" : "회사명"} placeholder="예) 핀테크 스타트업" value={company} onChange={(e) => setCompany(e.target.value)} required error={companyError} />
              <Input label="직무" placeholder="예) 백엔드 개발자" value={jobTitle} onChange={(e) => setJobTitle(e.target.value)} required error={jobTitleError} />
            </div>
            <p className="t-caption1 text-ink-500">프로필 이름 아래에 보여요. 경력은 따로 적어주세요. 회사명 공개 범위는 경력 편집에서 바꿀 수 있어요.</p>
          </div>
        )}
        <div className="flex flex-col gap-2">
          <p className="t-body-bold text-ink-700">
            멋사 파트 <span className="text-[12px] font-normal text-ink-400">(1개 · 가입 기수 멋사 활동 파트도 같이 바뀌어요)</span>
          </p>
          <div className="flex flex-wrap gap-2">
            {LION_PARTS.map((x) => (
              <Chip key={x} selected={parts.includes(x)} onClick={() => setParts([x])}>
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
/**
 * ◇[O] 대표 기술 편집 (PROFILE-07). [추가]·Enter로 넣거나 ✕로 빼면 바로 저장한다. 닫기는 오른쪽 위 X.
 * 저장에 실패하면 목록을 바꾸지 않고 입력값도 남겨 둔다.
 */
function SkillsModal({ skills, onClose, onSave }: { skills: string[]; onClose: () => void; onSave: (skills: string[]) => Promise<boolean> }) {
  const [all, setAll] = useState<string[]>(skills);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const v = input.trim();
  const full = all.length >= 5;
  const inputError = v.length > 20 ? "20자 이내로 적어주세요" : full && v ? "대표 기술은 5개까지예요" : v && all.includes(v) ? "이미 추가한 기술이에요" : undefined;
  const canAdd = !!v && !inputError && !busy;

  const commit = async (next: string[]) => {
    setBusy(true);
    const ok = await onSave(next);
    setBusy(false);
    if (ok) setAll(next);
    return ok;
  };
  const add = async () => {
    if (!canAdd) return;
    if (await commit([...all, v])) setInput("");
  };
  const remove = (s: string) => {
    if (!busy) void commit(all.filter((x) => x !== s));
  };

  return (
    <Modal open onClose={onClose} title="대표 기술 편집" width={560}>
      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap gap-2">
          {all.length === 0 && <span className="text-[13px] text-ink-400">아직 없어요. 아래에 입력하고 [추가]를 눌러주세요.</span>}
          {all.map((s) => (
            <span key={s} className="inline-flex items-center gap-1 rounded-[4px] bg-part-be-bg px-2 py-1 text-[13px] font-semibold text-primary">
              {s}
              <button type="button" aria-label={`${s} 삭제`} disabled={busy} onClick={() => remove(s)} className="ml-1 text-ink-400 hover:text-ink-900 disabled:cursor-not-allowed">
                ✕
              </button>
            </span>
          ))}
        </div>
        <div className="flex items-start gap-2">
          <Input
            className="flex-1"
            placeholder="기술 입력 (최대 5개, 1~20자)"
            value={input}
            disabled={full}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              // 맥 한글 입력은 글자를 조합하는 중에도 Enter가 먼저 온다. 그때 추가하면 조합 중이던 마지막 글자가 입력칸에 다시 남는다
              if (e.key !== "Enter" || e.nativeEvent.isComposing || e.keyCode === 229) return;
              e.preventDefault();
              void add();
            }}
            counter={{ value: all.length, max: 5 }}
            error={inputError}
          />
          <Button disabled={!canAdd} loading={busy} onClick={() => void add()}>
            추가
          </Button>
        </div>
        <p className="text-[12px] text-ink-500">추가하거나 지우면 바로 저장돼요. 프로필 상단과 동문 카드에 입력한 순서대로 보여요.</p>
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
