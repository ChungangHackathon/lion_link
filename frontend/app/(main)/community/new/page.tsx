"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { cn } from "@/lib/cn";
import { Suspense, useState } from "react";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { LION_PART_LABEL, POST_CATEGORIES, POST_CATEGORY_HINT, POST_CATEGORY_LABEL, type Post, type PostCategory } from "@/lib/types";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Icon, type IconName } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import { ConfirmModal } from "@/components/ui/Modal";
import { Card, ProtoButton, Skeleton } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";
import { AlumniPicker } from "@/components/domain/AlumniPicker";

export default function NewPostPage() {
  return (
    <Suspense fallback={<Skeleton className="h-[600px] w-full" />}>
      <PostEditor />
    </Suspense>
  );
}

/**
 * ★ B-05 글쓰기 / 공통 양식 · ◇ B-06 번개 · ◇ B-07 커피챗 모집.
 * 등록: 번개 → CF-14 → L-11 초대 → L-06, 커피챗 → K-01. 취소(변경 있음) → C-08.
 * ?edit=<id> → B-09 글 수정 (모집글 잠금).
 */
function PostEditor() {
  const router = useRouter();
  const toast = useToast();
  const params = useSearchParams();
  const { posts, alumni, createPost, updatePost } = useStore();
  const editing = params.get("edit") ? posts.find((p) => p.id === params.get("edit")) : undefined;

  const [cat, setCat] = useState<PostCategory>(editing?.category ?? ((params.get("cat") as PostCategory) || "CONTEST"));
  const [title, setTitle] = useState(editing?.title ?? "");
  const [body, setBody] = useState(editing?.body ?? "");
  const [links, setLinks] = useState<string[]>(editing?.links ?? [""]);
  const [anonymous, setAnonymous] = useState(editing?.anonymous ?? false);
  // 번개
  const [meetDate, setMeetDate] = useState(editing?.meetup ? editing.meetup.at.slice(0, 10) : "");
  const [meetTime, setMeetTime] = useState(editing?.meetup ? editing.meetup.at.slice(11, 16) : "19:00");
  const [place, setPlace] = useState(editing?.meetup?.place ?? "");
  const [cap, setCap] = useState(editing?.meetup?.max ?? editing?.recruit?.max ?? (cat === "RECRUIT" ? 4 : 6));
  const [companions, setCompanions] = useState<string[]>([]);
  // 커피챗 모집
  const [targetId, setTargetId] = useState<string | null>(editing?.recruit?.targetId ?? null);
  const [deadline, setDeadline] = useState(editing?.recruit ? editing.recruit.deadline.slice(0, 10) : "");
  const [picker, setPicker] = useState<"companion" | "target" | null>(null);
  const [leave, setLeave] = useState(false);
  const [notify, setNotify] = useState<string | null>(null); // CF-14 → post id
  const [invite, setInvite] = useState<string | null>(null); // L-11 → post id

  const target = targetId ? alumni.find((a) => a.id === targetId) : undefined;
  const locked = !!editing && ((editing.meetup && editing.meetup.memberIds.length > 1) || (editing.recruit && editing.recruit.memberIds.length > 1));
  const lockedAll = !!editing?.recruit && editing.recruit.status !== "OPEN";
  const minCap = editing?.meetup?.memberIds.length ?? editing?.recruit?.memberIds.length ?? 2;
  const dirty = title.length > 0 || body.length > 0;

  const validBase = title.trim().length >= 2 && title.length <= 50 && body.trim().length >= 1 && body.length <= 5000;
  const validExtra = cat === "MEETUP" ? !!meetDate && !!meetTime && place.trim().length > 0 && cap >= 2 && cap <= 30 : cat === "RECRUIT" ? !!targetId && !!deadline && cap >= 2 && cap <= 5 : true;
  const valid = validBase && validExtra;

  const submit = () => {
    if (!valid) return;
    const base: Omit<Post, "id" | "views" | "likes" | "comments" | "createdAt" | "authorId"> = {
      category: cat,
      title: title.trim(),
      body: body.trim(),
      anonymous: cat === "FREE" ? anonymous : false,
      keywords: title.split(/\s+/).filter((w) => w.length > 1).slice(0, 3),
      links: links.filter((l) => l.trim()),
    };
    if (cat === "MEETUP") base.meetup = { at: new Date(`${meetDate}T${meetTime}:00`).toISOString(), place: place.trim(), max: cap, memberIds: editing?.meetup?.memberIds ?? [ME_ID, ...companions] };
    if (cat === "RECRUIT") base.recruit = { targetId: targetId!, status: editing?.recruit?.status ?? "OPEN", max: cap, memberIds: editing?.recruit?.memberIds ?? [ME_ID], deadline: new Date(`${deadline}T23:59:00`).toISOString() };

    if (editing) {
      updatePost(editing.id, { ...base, edited: true });
      toast("글을 수정했어요");
      router.push(`/community/${editing.id}`);
      return;
    }
    const id = createPost(base);
    if (cat === "MEETUP") return setNotify(id);
    toast("글을 등록했어요");
    router.push(`/community/${id}`);
  };

  return (
    <div className="grid grid-cols-[220px_minmax(0,1fr)] gap-6">
      {/* Left: 목록 + 카테고리 (B-01과 같은 자리) */}
      <aside className="flex flex-col gap-4">
        <Button size="L" full href="/community" className="text-[18px] font-bold shadow-[0_2px_4px_rgba(0,0,0,0.15)]" leading={<Icon name="menu" size={24} />}>
          목록
        </Button>
        <Card className="rounded-[10px] p-2">
          {(["ALL", ...POST_CATEGORIES] as const).map((c) => (
            <Link key={c} href={c === "ALL" ? "/community" : `/community?cat=${c}`} className="flex w-full items-center justify-between rounded-[6px] px-3 py-[9px] text-[14px] font-medium text-ink-700 hover:bg-bg-50">
              {c === "ALL" ? "전체" : POST_CATEGORY_LABEL[c]}
              <span className="text-[12px] text-ink-400">{c === "ALL" ? posts.length : posts.filter((p) => p.category === c).length}</span>
            </Link>
          ))}
        </Card>
      </aside>

      <Card className="flex min-w-0 flex-col gap-6 rounded-[10px] px-[22px] pb-7 pt-5">
        <div className="flex items-center justify-between">
          <h1 className="t-title3 text-ink-900">{editing ? "글 수정" : "글쓰기"}</h1>
          <div className="flex gap-2">
            <Button size="S" variant="secondary" onClick={() => (dirty ? setLeave(true) : router.back())}>
              취소
            </Button>
            <Button size="S" disabled={!valid} onClick={submit}>
              {editing ? "저장" : "등록"}
            </Button>
          </div>
        </div>

        <div className="flex flex-col gap-2.5">
          <p className="t-label2 text-ink-700">카테고리 *</p>
          <div role="radiogroup" aria-label="카테고리" className="flex flex-wrap gap-2">
            {POST_CATEGORIES.filter((c) => c !== "NOW" || cat === "NOW").map((c) => (
              <button
                key={c}
                type="button"
                role="radio"
                aria-checked={cat === c}
                disabled={(locked || lockedAll) && cat !== c}
                onClick={() => { setCat(c); setCap(c === "RECRUIT" ? 4 : 6); }}
                className={cn("h-8 rounded-full border px-3.5 t-label2 transition-colors disabled:opacity-40", cat === c ? "border-light-text bg-blue-100/60 text-navy" : "border-line-300 bg-white font-medium text-ink-700 hover:bg-bg-50")}
              >
                {c === "MATERIAL" ? "족보·자료" : c === "EVENT" ? "세미나 · 행사" : c === "JOB" ? "채용 · 협업" : POST_CATEGORY_LABEL[c]}
              </button>
            ))}
          </div>
          {locked && <p className="t-caption1 text-ink-500">참여자가 있어 카테고리를 바꿀 수 없어요</p>}
        </div>

        <div className="flex items-center justify-between border-y border-line-200 py-3">
          <div className="flex gap-4">
            <ToolButton icon="image" label="사진" onClick={() => toast("사진 올리기는 백엔드 연결 후 동작해요")} />
            <ToolButton icon="clip" label="파일" onClick={() => toast("파일 첨부는 백엔드 연결 후 동작해요")} />
            <ToolButton icon="link" label="링크" onClick={() => links.length < 3 && setLinks((x) => (x[x.length - 1] === "" ? x : [...x, ""]))} />
          </div>
          {cat !== "MEETUP" && cat !== "RECRUIT" && (
            <label className="flex cursor-pointer items-center gap-2 t-label2 font-medium text-ink-700">
              <input type="checkbox" checked={anonymous} onChange={(e) => setAnonymous(e.target.checked)} className="size-[18px] accent-[#004c97]" />
              익명으로 쓰기
            </label>
          )}
        </div>

        <div className="flex flex-col gap-1">
          <input aria-label="제목" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="제목을 입력하세요" maxLength={60} className="w-full border-b border-line-200 bg-transparent pb-4 text-[30px] font-semibold leading-10 tracking-[-0.02em] text-ink-900 outline-none placeholder:text-ink-400" />
          <p className={cn("self-end text-[12px]", title.length > 50 ? "text-orange" : "text-ink-400")}>{title.length > 50 ? "50자 이내로 적어주세요 · " : ""}{title.length}/50</p>
        </div>
        <div className="flex flex-col gap-1">
          <textarea aria-label="본문" value={body} onChange={(e) => setBody(e.target.value)} placeholder={POST_CATEGORY_HINT[cat]} rows={8} className="min-h-[200px] w-full resize-y bg-transparent text-[15px] leading-6 text-ink-900 outline-none placeholder:text-ink-400" />
          <p className="self-end text-[12px] text-ink-400">{body.length}/5000</p>
        </div>
        {anonymous && cat !== "MEETUP" && cat !== "RECRUIT" && <p className="rounded-[6px] bg-bg-100 px-3 py-2 t-caption1 text-ink-500">익명으로 올라가요. 내 이름 대신 ‘익명 사자’로 보여요.</p>}

      {/* B-06 번개 */}
      {cat === "MEETUP" && (
        <div className="flex flex-col gap-4 rounded-[12px] border border-line-200 bg-bg-50 p-5">
          <p className="t-body-bold text-ink-900">모집 정보</p>
          {lockedAll && <p className="rounded-[6px] bg-bg-100 px-3 py-2 text-[13px] text-ink-500">신청한 뒤에는 모집 정보를 바꿀 수 없어요</p>}
          <div className="grid grid-cols-2 gap-3">
            <Input label="모임 날짜" required type="date" value={meetDate} onChange={(e) => setMeetDate(e.target.value)} />
            <Input label="시각" required type="time" step={1800} value={meetTime} onChange={(e) => setMeetTime(e.target.value)} />
          </div>
          <Input label="장소" required placeholder="예) 흑석역 2번 출구" value={place} onChange={(e) => setPlace(e.target.value)} />
          <Counter label="정원" hint="2~30명, 작성자 포함" value={cap} min={Math.max(2, minCap)} max={30} onChange={setCap} error={cap < minCap ? "지금 참여 인원보다 적게 정할 수 없어요" : undefined} />
          {!editing && (
            <div className="flex items-center justify-between">
              <div>
                <p className="t-label1 text-ink-700">함께하는 동문 (선택)</p>
                <p className="text-[12px] text-ink-500">등록하면 바로 참여자가 돼요</p>
              </div>
              <Button size="S" variant="secondary" onClick={() => setPicker("companion")}>
                + 함께하는 동문 추가
              </Button>
            </div>
          )}
          {companions.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {companions.map((id) => {
                const a = alumni.find((x) => x.id === id);
                return (
                  <span key={id} className="inline-flex items-center gap-1.5 rounded-full border border-line-300 py-1 pl-1 pr-2.5 text-[13px] font-semibold">
                    <Avatar name={a?.name} part={a?.part} size={32} className="!size-6 !text-[11px]" /> {a?.name}
                    <button type="button" onClick={() => setCompanions((c) => c.filter((x) => x !== id))} className="text-ink-400">
                      ✕
                    </button>
                  </span>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* B-07 커피챗 모집 */}
      {cat === "RECRUIT" && (
        <div className="flex flex-col gap-4 rounded-[12px] border border-line-200 bg-bg-50 p-5">
          <p className="t-body-bold text-ink-900">모집 정보</p>
          {lockedAll && <p className="rounded-[6px] bg-bg-100 px-3 py-2 text-[13px] text-ink-500">신청한 뒤에는 모집 정보를 바꿀 수 없어요</p>}
          <div className="flex items-center justify-between">
            <div>
              <p className="t-label1 text-ink-700">신청할 사자 *</p>
              {target ? (
                <p className="text-[14px] text-ink-900">
                  {target.name} · {target.generation}기 · {LION_PART_LABEL[target.part]} · {target.allowGroup ? "여러 명 신청 가능" : <span className="text-orange">1:1만 받아요</span>}
                </p>
              ) : (
                <p className="text-[13px] text-ink-500">여러 명 신청을 받는 사자만 고를 수 있어요</p>
              )}
              {locked && <p className="text-[12px] text-ink-500">참여자가 있어 신청할 사자를 바꿀 수 없어요</p>}
            </div>
            <Button size="S" variant="secondary" disabled={locked || lockedAll} onClick={() => setPicker("target")}>
              사자 선택
            </Button>
          </div>
          <Counter label="모집 인원" hint="2~5명, 작성자 포함" value={cap} min={Math.max(2, minCap)} max={5} onChange={setCap} disabled={lockedAll} />
          <Input label="모집 마감일" required type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} disabled={lockedAll} />
        </div>
      )}

      {/* 이미지·첨부 미리보기 */}
      <div className="flex gap-3">
        <button type="button" onClick={() => toast("사진 올리기는 백엔드 연결 후 동작해요")} aria-label="사진 추가" className="grid size-[64px] place-items-center rounded-[8px] border border-line-200 bg-white text-ink-500 hover:bg-bg-50">
          <Icon name="plus" size={22} />
        </button>
      </div>
      {links.map((l, i) => (
        <div key={i} className="flex items-center gap-3 rounded-[8px] border border-line-200 px-4 py-3">
          <Icon name="link" size={18} className="text-ink-700" />
          <input aria-label={`링크 ${i + 1}`} value={l} placeholder="https:// 링크를 붙여넣으세요 (선택)" onChange={(e) => setLinks((x) => x.map((y, j) => (j === i ? e.target.value : y)))} className="min-w-0 flex-1 bg-transparent text-[14px] text-ink-900 outline-none placeholder:text-ink-400" />
          {links.length > 1 && (
            <button type="button" aria-label="링크 지우기" onClick={() => setLinks((x) => x.filter((_, j) => j !== i))} className="rounded p-1 text-ink-700 hover:bg-bg-100">
              <Icon name="close" size={16} />
            </button>
          )}
        </div>
      ))}

      {!editing && (
        <div className="flex gap-2">
          <ProtoButton onClick={() => { setCat("MEETUP"); setCap(6); }}>카테고리 번개 선택</ProtoButton>
          <ProtoButton onClick={() => { setCat("RECRUIT"); setCap(4); }}>카테고리 커피챗 선택</ProtoButton>
        </div>
      )}
      </Card>

      <AlumniPicker open={picker === "companion"} onClose={() => setPicker(null)} onConfirm={setCompanions} initial={companions} max={cap - 1} title="함께하는 동문 추가" description="등록하면 바로 참여자가 돼요" />
      <AlumniPicker open={picker === "target"} onClose={() => setPicker(null)} onConfirm={(ids) => setTargetId(ids[0])} single title="신청할 사자 선택" description="여러 명 신청을 받는 사자만 보여요" filter={(a) => a.allowGroup && a.receive === "OPEN"} confirmLabel="선택" />

      <ConfirmModal open={leave} onClose={() => setLeave(false)} onConfirm={() => router.back()} title="작성 중인 내용이 사라져요. 나갈까요?" confirmLabel="나가기" />
      {/* ◇[O] CF-14 번개 알림 확인 */}
      <ConfirmModal
        open={!!notify}
        onClose={() => { const id = notify; setNotify(null); router.push(`/community/${id}`); }}
        onConfirm={() => { setInvite(notify); setNotify(null); }}
        title="동문들에게 번개를 알릴까요?"
        description="초대 알림을 보내면 고른 동문에게 번개 소식이 전달돼요. 나중에 글에서도 보낼 수 있어요."
        confirmLabel="초대 알림 보내기"
        cancelLabel="나중에"
      />
      {/* ◇[O] L-11 번개 초대 알림 보내기 */}
      <AlumniPicker open={!!invite} onClose={() => { const id = invite; setInvite(null); router.push(`/community/${id}`); }} onConfirm={(ids) => { toast(`${ids.length}명에게 초대 알림을 보냈어요`); }} max={20} title="번개 초대 알림 보내기" description="최대 20명까지 보낼 수 있어요" confirmLabel="알림 보내기" />
    </div>
  );
}

function Counter({ label, hint, value, min, max, onChange, error, disabled }: { label: string; hint: string; value: number; min: number; max: number; onChange: (v: number) => void; error?: string; disabled?: boolean }) {
  return (
    <div className="flex flex-col gap-1.5">
      <p className="t-label1 text-ink-700">
        {label} * <span className="text-[12px] font-normal text-ink-400">{hint}</span>
      </p>
      <div className="flex items-center gap-2">
        <button type="button" disabled={disabled || value <= min} onClick={() => onChange(value - 1)} className="grid size-8 place-items-center rounded-[6px] border border-line-300 text-ink-700 disabled:opacity-30">
          −
        </button>
        <span className="w-12 text-center text-[15px] font-semibold text-ink-900">{value}명</span>
        <button type="button" disabled={disabled || value >= max} onClick={() => onChange(value + 1)} className="grid size-8 place-items-center rounded-[6px] border border-line-300 text-ink-700 disabled:opacity-30">
          +
        </button>
      </div>
      {error && <p className="t-caption2 text-orange">{error}</p>}
    </div>
  );
}


function ToolButton({ icon, label, onClick }: { icon: IconName; label: string; onClick: () => void }) {
  return (
    <button type="button" onClick={onClick} className="flex w-9 flex-col items-center gap-1 rounded p-1 text-ink-700 hover:bg-bg-100">
      <Icon name={icon} size={24} />
      <span className="text-[10px] leading-3">{label}</span>
    </button>
  );
}
