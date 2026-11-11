"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { LION_PART_LABEL, POST_CATEGORIES, POST_CATEGORY_HINT, POST_CATEGORY_LABEL, type Post, type PostCategory } from "@/lib/types";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Glyph } from "@/components/ui/Icon";
import { Input, Select, Textarea } from "@/components/ui/Input";
import { ConfirmModal } from "@/components/ui/Modal";
import { ProtoButton, Skeleton } from "@/components/ui/Misc";
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
    <div className="mx-auto flex w-full max-w-[792px] flex-col gap-6">
      <h1 className="t-title text-ink-900">{editing ? "글 수정" : "글쓰기"}</h1>

      <Select label="카테고리" required value={cat} disabled={locked || lockedAll} onChange={(e) => { const c = e.target.value as PostCategory; setCat(c); setCap(c === "RECRUIT" ? 4 : 6); }} help={locked ? "참여자가 있어 카테고리를 바꿀 수 없어요" : undefined}>
        {POST_CATEGORIES.map((c) => (
          <option key={c} value={c}>
            {POST_CATEGORY_LABEL[c]}
          </option>
        ))}
      </Select>
      <Input label="제목" required placeholder="2~50자로 입력해주세요" value={title} onChange={(e) => setTitle(e.target.value)} counter={{ value: title.length, max: 50 }} error={title.length > 50 ? "50자 이내로 적어주세요" : undefined} />
      <Textarea label="본문" required rows={6} placeholder={POST_CATEGORY_HINT[cat]} value={body} onChange={(e) => setBody(e.target.value)} counter={{ value: body.length, max: 5000 }} />

      {cat === "FREE" && (
        <label className="flex items-center gap-2 text-[14px] text-ink-700">
          <input type="checkbox" checked={anonymous} onChange={(e) => setAnonymous(e.target.checked)} className="size-[18px] accent-primary" /> 익명으로 쓰기 <span className="text-[12px] text-ink-400">(자유 카테고리만 · 댓글도 익명으로 달려요)</span>
        </label>
      )}

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
              <p className="t-label1 text-ink-700">신청할 선배 *</p>
              {target ? (
                <p className="text-[14px] text-ink-900">
                  {target.name} · {target.generation}기 · {LION_PART_LABEL[target.part]} · {target.allowGroup ? "여러 명 신청 가능" : <span className="text-orange">1:1만 받아요</span>}
                </p>
              ) : (
                <p className="text-[13px] text-ink-500">여러 명 신청을 받는 선배만 고를 수 있어요</p>
              )}
              {locked && <p className="text-[12px] text-ink-500">참여자가 있어 신청할 선배를 바꿀 수 없어요</p>}
            </div>
            <Button size="S" variant="secondary" disabled={locked || lockedAll} onClick={() => setPicker("target")}>
              선배 선택
            </Button>
          </div>
          <Counter label="모집 인원" hint="2~5명, 작성자 포함" value={cap} min={Math.max(2, minCap)} max={5} onChange={setCap} disabled={lockedAll} />
          <Input label="모집 마감일" required type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} disabled={lockedAll} />
        </div>
      )}

      {/* 이미지·첨부 (UI only) */}
      <Dropzone title="이미지를 끌어다 놓거나 클릭해서 올려주세요" hint="0~5장 · jpg/png/webp · 장당 10MB" />
      <Dropzone title="첨부파일을 끌어다 놓거나 클릭해서 올려주세요" hint="0~3개 · pdf/hwp/docx/pptx/xlsx/zip · 개당 20MB" />

      <div className="flex flex-col gap-2">
        {links.map((l, i) => (
          <Input key={i} label={i === 0 ? "링크 (선택)" : undefined} placeholder="https://" value={l} onChange={(e) => setLinks((s) => s.map((x, j) => (j === i ? e.target.value : x)))} />
        ))}
        {links.length < 3 && (
          <button type="button" onClick={() => setLinks((s) => [...s, ""])} className="w-fit text-[14px] text-ink-700 underline underline-offset-2">
            + 링크 추가
          </button>
        )}
      </div>

      {!editing && (
        <div className="flex gap-2">
          <ProtoButton onClick={() => { setCat("MEETUP"); setCap(6); }}>카테고리 번개 선택</ProtoButton>
          <ProtoButton onClick={() => { setCat("RECRUIT"); setCap(4); }}>카테고리 커피챗 선택</ProtoButton>
        </div>
      )}

      <div className="flex justify-end gap-2 pt-2">
        <Button size="L" variant="secondary" onClick={() => (dirty ? setLeave(true) : router.back())}>
          취소
        </Button>
        <Button size="L" disabled={!valid} onClick={submit}>
          {editing ? "저장" : "등록"}
        </Button>
      </div>

      <AlumniPicker open={picker === "companion"} onClose={() => setPicker(null)} onConfirm={setCompanions} initial={companions} max={cap - 1} title="함께하는 동문 추가" description="등록하면 바로 참여자가 돼요" />
      <AlumniPicker open={picker === "target"} onClose={() => setPicker(null)} onConfirm={(ids) => setTargetId(ids[0])} single title="신청할 선배 선택" description="여러 명 신청을 받는 선배만 보여요" filter={(a) => a.allowGroup && a.receive === "OPEN"} confirmLabel="선택" />

      <ConfirmModal open={leave} onClose={() => setLeave(false)} onConfirm={() => router.back()} title="작성 중인 내용이 사라져요" description="지금 나가면 적은 내용은 저장되지 않아요." confirmLabel="나가기" cancelLabel="계속 작성" danger />
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

function Dropzone({ title, hint }: { title: string; hint: string }) {
  return (
    <button type="button" className="flex flex-col items-center gap-1 rounded-[12px] border border-dashed border-line-300 bg-bg-50 px-6 py-8 text-center hover:bg-bg-100">
      <Glyph name="image" size={22} className="text-ink-700" />
      <span className="t-label1 text-ink-900">{title}</span>
      <span className="text-[12px] text-ink-500">{hint}</span>
    </button>
  );
}
