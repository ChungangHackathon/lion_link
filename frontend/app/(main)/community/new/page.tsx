"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { cn } from "@/lib/cn";
import { Suspense, useState } from "react";
import { LION_PART_LABEL, POST_CATEGORIES, POST_CATEGORY_HINT, POST_CATEGORY_LABEL, type PostCategory } from "@/lib/types";
import { API_ENABLED, errorMessage, isApiError } from "@/lib/api/client";
import type { AlumniSummary } from "@/lib/api/alumni";
import { fromKstDateTime, toKstDateTime, type Id, type PostDetail, type PostUpdateRequest, type PostWriteRequest } from "@/lib/api/posts";
import { isHttpUrl, normalizeLink } from "@/lib/api/profile";
import { useApi } from "@/lib/api/useApi";
import { usePostService } from "@/lib/api/usePostService";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Icon, type IconName } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import { ConfirmModal } from "@/components/ui/Modal";
import { Card, EmptyState, ProtoButton, Skeleton } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";
import { AlumniPicker } from "@/components/domain/AlumniPicker";
import { usePostAttachments } from "@/components/domain/PostAttachments";

export default function NewPostPage() {
  return (
    <Suspense fallback={<Skeleton className="h-[600px] w-full" />}>
      <PostEditorLoader />
    </Suspense>
  );
}

/** ?edit=<id>면 글을 불러온 뒤 그 값으로 양식을 채운다 */
function PostEditorLoader() {
  const params = useSearchParams();
  const editId = params.get("edit");
  const service = usePostService();
  // 프로토타입 모드에서는 store가 바뀔 때마다 service가 바뀌므로, 처음 불러온 값으로 양식을 고정한다
  const [loadedFor] = useState(service);
  const { data, error } = useApi(() => loadedFor.get(editId!), [loadedFor, editId], !!editId);

  if (!editId) return <PostEditor initialCategory={(params.get("cat") as PostCategory) || "CONTEST"} />;
  if (error) return <EmptyState title={isApiError(error) && error.status === 404 ? "404 · 삭제되었거나 없는 글이에요" : "글을 불러오지 못했어요"} description={isApiError(error) && error.status !== 404 ? errorMessage(error) : undefined} action={{ label: "커뮤니티로", href: "/community" }} />;
  if (!data) return <Skeleton className="h-[600px] w-full" />;
  if (!data.isMine) return <EmptyState title="내가 쓴 글만 고칠 수 있어요" action={{ label: "글로 돌아가기", href: `/community/${data.id}` }} />;
  return <PostEditor key={String(data.id)} editing={data} initialCategory={data.category} />;
}

/** 지금부터 1시간 뒤 ~ 14일 이내 (POST-03 meetup.meetAt) */
function meetupTimeError(date: string, time: string): string | undefined {
  if (!date || !time) return undefined;
  const at = new Date(toKstDateTime(date, time)).getTime();
  const now = Date.now();
  if (at < now + 3_600_000) return "지금부터 1시간 뒤부터 정할 수 있어요";
  if (at > now + 14 * 86_400_000) return "14일 이내로 정해주세요";
  return undefined;
}

const todayKst = () => new Date(Date.now() + 9 * 3_600_000).toISOString().slice(0, 10);

/**
 * ★ B-05 글쓰기 / 공통 양식 · ◇ B-06 번개 · ◇ B-07 커피챗 모집.
 * 등록: 번개 → CF-14 → L-11 초대 → L-06, 커피챗 → K-01. 취소(변경 있음) → C-08.
 * ?edit=<id> → B-09 글 수정 (모집글 잠금).
 *
 * 함께하는 동문·신청할 사자 고르기와 번개 초대는 동문 목록(mock)을 써서 프로토타입 모드에서만 된다.
 * API 모드에서는 동문 찾기·번개 초대를 연결한 뒤 열린다.
 */
function PostEditor({ editing, initialCategory }: { editing?: PostDetail; initialCategory: PostCategory }) {
  const router = useRouter();
  const toast = useToast();
  const service = usePostService();
  const counts = useApi(() => service.categoryCounts(), [service]).data;
  const meetAt = editing?.meetup ? fromKstDateTime(editing.meetup.meetAt) : undefined;

  const [cat, setCat] = useState<PostCategory>(initialCategory);
  const [title, setTitle] = useState(editing?.title ?? "");
  const [body, setBody] = useState(editing?.content ?? "");
  const [links, setLinks] = useState<string[]>(editing?.links.length ? editing.links : [""]);
  const [anonymous, setAnonymous] = useState(editing?.isAnonymous ?? false);
  // 번개
  const [meetDate, setMeetDate] = useState(meetAt?.date ?? "");
  const [meetTime, setMeetTime] = useState(meetAt?.time ?? "19:00");
  const [place, setPlace] = useState(editing?.meetup?.place ?? "");
  const [cap, setCap] = useState(editing?.meetup?.capacity ?? editing?.recruit?.capacity ?? (initialCategory === "RECRUIT" ? 4 : 6));
  const [companions, setCompanions] = useState<AlumniSummary[]>([]);
  // 커피챗 모집
  const [picked, setPicked] = useState<AlumniSummary | null>(null);
  const targetId: Id | null = picked?.id ?? editing?.recruit?.senior.id ?? null;
  const [deadline, setDeadline] = useState(editing?.recruit ? editing.recruit.deadline.slice(0, 10) : "");
  const [picker, setPicker] = useState<"companion" | "target" | null>(null);
  const [leave, setLeave] = useState(false);
  const [notify, setNotify] = useState<Id | null>(null); // CF-14 → post id
  const [invite, setInvite] = useState<Id | null>(null); // L-11 → post id
  const [saving, setSaving] = useState(false);
  const attachments = usePostAttachments(editing?.files);

  const target = picked
    ? { name: picked.name, generation: picked.generation, part: picked.lionParts[0] ?? ("ETC" as const), acceptGroup: picked.coffeeChat.acceptGroup }
    : editing?.recruit
      ? { name: editing.recruit.senior.name ?? "알 수 없음", generation: editing.recruit.senior.generation ?? 0, part: editing.recruit.senior.lionParts?.[0] ?? ("ETC" as const), acceptGroup: editing.recruit.senior.acceptGroup }
      : undefined;
  const members = editing?.meetup?.participantCount ?? editing?.recruit?.memberCount ?? 1;
  const locked = !!editing && (!!editing.meetup || !!editing.recruit) && members > 1;
  const lockedAll = !!editing?.recruit && editing.recruit.status !== "OPEN";
  const minCap = Math.max(2, editing?.meetup?.participantCount ?? editing?.recruit?.memberCount ?? 2);
  const dirty = title.length > 0 || body.length > 0 || attachments.count > 0;

  const normalizedLinks = links.map(normalizeLink).filter((l): l is string => !!l);
  const badLink = normalizedLinks.some((l) => !isHttpUrl(l));
  const timeError = cat === "MEETUP" ? meetupTimeError(meetDate, meetTime) : undefined;
  const validBase = title.trim().length >= 2 && title.trim().length <= 50 && body.trim().length >= 1 && body.length <= 5000 && !badLink;
  const validExtra =
    cat === "MEETUP"
      ? !!meetDate && !!meetTime && !timeError && place.trim().length > 0 && place.trim().length <= 50 && cap >= minCap && cap <= 30
      : cat === "RECRUIT"
        ? !!targetId && !!deadline && deadline >= todayKst() && cap >= minCap && cap <= 5
        : true;
  const valid = validBase && validExtra && !attachments.uploading;

  const submit = async () => {
    if (!valid || saving) return;
    setSaving(true);
    const base = { category: cat, title: title.trim(), content: body.trim(), links: normalizedLinks, files: attachments.files };
    const meetup = cat === "MEETUP" ? { meetAt: toKstDateTime(meetDate, meetTime), place: place.trim(), capacity: cap } : undefined;
    try {
      if (editing) {
        const patch: PostUpdateRequest = { ...base };
        if (meetup) patch.meetup = meetup;
        if (cat === "RECRUIT" && !lockedAll) {
          // 참여자가 있으면 신청받을 사자는 바꿀 수 없으니 보내지 않는다
          patch.recruit = locked ? { capacity: cap, deadline } : { seniorId: targetId!, capacity: cap, deadline };
        }
        await service.update(editing.id, patch);
        toast("글을 수정했어요");
        router.push(`/community/${editing.id}`);
        return;
      }

      const req: PostWriteRequest = { ...base, isAnonymous: cat === "FREE" ? anonymous : undefined };
      if (meetup) req.meetup = { ...meetup, companionIds: companions.length ? companions.map((a) => a.id) : undefined };
      if (cat === "RECRUIT") req.recruit = { seniorId: targetId!, capacity: cap, deadline };
      const res = await service.create(req);
      if (cat === "MEETUP") {
        setNotify(res.id);
        return;
      }
      toast("글을 등록했어요");
      router.push(`/community/${res.id}`);
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
      setSaving(false);
    }
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
              <span className="text-[12px] text-ink-400">{counts ? (c === "ALL" ? counts.total : counts.counts[c]) : ""}</span>
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
            <Button size="S" disabled={!valid} loading={saving} onClick={submit}>
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
                disabled={((locked || lockedAll) && cat !== c) || (!!editing && editing.isAnonymous && c !== "FREE")}
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
            <ToolButton icon="image" label="사진" onClick={attachments.pickImage} />
            <ToolButton icon="clip" label="파일" onClick={attachments.pickFile} />
            <ToolButton icon="link" label="링크" onClick={() => links.length < 3 && setLinks((x) => (x[x.length - 1] === "" ? x : [...x, ""]))} />
          </div>
          {cat === "FREE" && !editing && (
            <label className="flex cursor-pointer items-center gap-2 t-label2 font-medium text-ink-700">
              <input type="checkbox" checked={anonymous} onChange={(e) => setAnonymous(e.target.checked)} className="size-[18px] accent-[#004c97]" />
              익명으로 쓰기
            </label>
          )}
        </div>

        <div className="flex flex-col gap-1">
          <input aria-label="제목" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="제목을 입력하세요" maxLength={60} className="w-full border-b border-line-200 bg-transparent pb-4 text-[30px] font-semibold leading-10 tracking-[-0.02em] text-ink-900 outline-none placeholder:text-ink-400" />
          <p className={cn("self-end text-[12px]", title.trim().length > 50 ? "text-orange" : "text-ink-400")}>{title.trim().length > 50 ? "50자 이내로 적어주세요 · " : ""}{title.length}/50</p>
        </div>
        <div className="flex flex-col gap-1">
          <textarea aria-label="본문" value={body} onChange={(e) => setBody(e.target.value)} placeholder={POST_CATEGORY_HINT[cat]} rows={8} className="min-h-[200px] w-full resize-y bg-transparent text-[15px] leading-6 text-ink-900 outline-none placeholder:text-ink-400" />
          <p className="self-end text-[12px] text-ink-400">{body.length}/5000</p>
        </div>
        {anonymous && cat === "FREE" && <p className="rounded-[6px] bg-bg-100 px-3 py-2 t-caption1 text-ink-500">익명으로 올라가요. 내 이름 대신 ‘익명 사자’로 보여요.</p>}

      {/* B-06 번개 */}
      {cat === "MEETUP" && (
        <div className="flex flex-col gap-4 rounded-[12px] border border-line-200 bg-bg-50 p-5">
          <p className="t-body-bold text-ink-900">모집 정보</p>
          {lockedAll && <p className="rounded-[6px] bg-bg-100 px-3 py-2 text-[13px] text-ink-500">신청한 뒤에는 모집 정보를 바꿀 수 없어요</p>}
          <div className="grid grid-cols-2 gap-3">
            <Input label="모임 날짜" required type="date" value={meetDate} onChange={(e) => setMeetDate(e.target.value)} />
            <Input label="시각" required type="time" step={1800} value={meetTime} onChange={(e) => setMeetTime(e.target.value)} />
          </div>
          {timeError && <p className="-mt-2 t-caption2 text-orange">{timeError}</p>}
          <Input label="장소" required placeholder="예) 흑석역 2번 출구" value={place} onChange={(e) => setPlace(e.target.value)} />
          <Counter label="정원" hint="2~30명, 작성자 포함" value={cap} min={minCap} max={30} onChange={setCap} error={cap < minCap ? "지금 참여 인원보다 적게 정할 수 없어요" : undefined} />
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
              {companions.map((a) => (
                <span key={a.id} className="inline-flex items-center gap-1.5 rounded-full border border-line-300 py-1 pl-1 pr-2.5 text-[13px] font-semibold">
                  <Avatar name={a.name} part={a.lionParts[0]} size={32} className="!size-6 !text-[11px]" /> {a.name}
                  <button type="button" aria-label={`${a.name} 빼기`} onClick={() => setCompanions((c) => c.filter((x) => x.id !== a.id))} className="text-ink-400">
                    ✕
                  </button>
                </span>
              ))}
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
                  {target.name} · {target.generation}기 · {LION_PART_LABEL[target.part]} · {target.acceptGroup ? "여러 명 신청 가능" : <span className="text-orange">1:1만 받아요</span>}
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
          <Counter label="모집 인원" hint="2~5명, 작성자 포함" value={cap} min={minCap} max={5} onChange={setCap} disabled={lockedAll} />
          <Input label="모집 마감일" required type="date" min={todayKst()} value={deadline} onChange={(e) => setDeadline(e.target.value)} disabled={lockedAll} error={deadline && deadline < todayKst() ? "오늘 이후로 정해주세요" : undefined} />
        </div>
      )}

      {/* 이미지·첨부 미리보기 (FILE-01) */}
      {attachments.inputs}
      {attachments.view}
      {attachments.uploading && <p className="-mt-3 t-caption2 text-ink-500">파일을 올리는 중이에요. 다 올라가면 등록할 수 있어요.</p>}
      {links.map((l, i) => (
        <div key={i} className={cn("flex items-center gap-3 rounded-[8px] border px-4 py-3", l.trim() && !isHttpUrl(normalizeLink(l) ?? "") ? "border-orange" : "border-line-200")}>
          <Icon name="link" size={18} className="text-ink-700" />
          <input aria-label={`링크 ${i + 1}`} value={l} placeholder="https:// 링크를 붙여넣으세요 (선택)" onChange={(e) => setLinks((x) => x.map((y, j) => (j === i ? e.target.value : y)))} className="min-w-0 flex-1 bg-transparent text-[14px] text-ink-900 outline-none placeholder:text-ink-400" />
          {links.length > 1 && (
            <button type="button" aria-label="링크 지우기" onClick={() => setLinks((x) => x.filter((_, j) => j !== i))} className="rounded p-1 text-ink-700 hover:bg-bg-100">
              <Icon name="close" size={16} />
            </button>
          )}
        </div>
      ))}

      {badLink && <p className="-mt-3 t-caption2 text-orange">올바른 링크 주소를 입력해주세요</p>}
      {!editing && !API_ENABLED && (
        <div className="flex gap-2">
          <ProtoButton onClick={() => { setCat("MEETUP"); setCap(6); }}>카테고리 번개 선택</ProtoButton>
          <ProtoButton onClick={() => { setCat("RECRUIT"); setCap(4); }}>카테고리 커피챗 선택</ProtoButton>
        </div>
      )}
      </Card>

      <AlumniPicker open={picker === "companion"} onClose={() => setPicker(null)} onConfirm={(_, people) => setCompanions(people)} initial={companions} max={cap - 1} title="함께하는 동문 추가" description="등록하면 바로 참여자가 돼요" />
      {/* 커피챗 모집의 사자는 오늘 받는 중이면서 여러 명 신청을 받는 사자만 (POST-03 recruit.seniorId) */}
      <AlumniPicker open={picker === "target"} onClose={() => setPicker(null)} onConfirm={(_, people) => setPicked(people[0] ?? null)} single title="신청할 사자 선택" description="여러 명 신청을 받는 사자만 보여요" query={{ available: true }} filter={(a) => a.coffeeChat.acceptGroup} confirmLabel="선택" />

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
      <AlumniPicker
        open={!!invite}
        onClose={() => {
          const id = invite;
          setInvite(null);
          router.push(`/community/${id}`);
        }}
        onConfirm={(ids) => {
          if (!invite) return;
          service.inviteToMeetup(invite, ids).then(
            (res) => toast(res.sentCount > 0 ? `${res.sentCount}명에게 초대 알림을 보냈어요` : "이미 참여했거나 초대한 동문이에요"),
            (e) => toast(errorMessage(e), { kind: "error" }),
          );
        }}
        max={20}
        title="번개 초대 알림 보내기"
        description="최대 20명까지 보낼 수 있어요. 자동으로 참여되지는 않아요."
        confirmLabel="알림 보내기"
        exclude={companions.map((a) => a.id)}
      />
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
