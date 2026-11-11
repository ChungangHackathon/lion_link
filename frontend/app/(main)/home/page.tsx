"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { cn } from "@/lib/cn";
import { useSession } from "@/lib/session";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { LION_PART_LABEL, POST_CATEGORY_LABEL } from "@/lib/types";
import { daysLeft, formatDateTime, timeAgo } from "@/lib/format";
import { Avatar } from "@/components/ui/Avatar";
import { Tag } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Glyph, Icon } from "@/components/ui/Icon";
import { Switch } from "@/components/ui/Input";
import { Card, ProgressBar } from "@/components/ui/Misc";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { AlumniCard } from "@/components/domain/AlumniCard";
import { useAlumniActions } from "@/components/domain/useAlumniActions";

const NEWS_TABS = ["전체", "공모전·채용", "지금 사자는", "자료"] as const;

/** ★ H-01 홈 / 전체 — 3단 264 / 576 / 312. ◇ H-04 승인 후 첫 방문 (welcome banner). */
export default function HomePage() {
  const { user, session, update } = useSession();
  const { alumni, coffeeChats, rooms, posts, bookmarks, settings, setSettings } = useStore();
  const { onRequest, onMessage, modals } = useAlumniActions();
  const toast = useToast();
  const router = useRouter();
  const [tab, setTab] = useState<(typeof NEWS_TABS)[number]>("전체");
  const [rest, setRest] = useState(false); // H-06 쉬기 팝업
  const [confirmOn, setConfirmOn] = useState(false); // CF-21

  const me = alumni.find((a) => a.id === ME_ID) ?? user!;
  const pendingReceived = coffeeChats.filter((c) => c.receiverId === ME_ID && c.status === "PENDING");
  const unread = rooms.reduce((n, r) => n + r.unread, 0);
  const requests = rooms.filter((r) => r.isRequest && r.status === "REQUESTED" && r.memberIds[0] !== ME_ID);
  const active = coffeeChats.filter((c) => (c.receiverId === ME_ID || c.requesterId === ME_ID) && ["ACCEPTED", "SCHEDULED"].includes(c.status));
  const upcoming = useMemo(() => coffeeChats.filter((c) => c.status === "SCHEDULED" && c.scheduledAt && (c.receiverId === ME_ID || c.requesterId === ME_ID)).sort((a, b) => a.scheduledAt!.localeCompare(b.scheduledAt!))[0], [coffeeChats]);
  const completion = me.completion ?? 45;

  // 선배 추천: 같은 파트 + 커피챗 가능, 나보다 선배
  const recommended = alumni.filter((a) => a.id !== ME_ID && a.receive === "OPEN" && a.generation < me.generation).sort((a, b) => (a.part === me.part ? -1 : 1) - (b.part === me.part ? -1 : 1)).slice(0, 2);
  const recruits = posts.filter((p) => (p.recruit && p.recruit.status === "OPEN") || p.meetup).slice(0, 3);
  const news = posts.filter((p) => tab === "전체" || (tab === "공모전·채용" && ["CONTEST", "JOB"].includes(p.category)) || (tab === "지금 사자는" && ["NOW", "FREE"].includes(p.category)) || (tab === "자료" && p.category === "MATERIAL")).slice(0, 4);

  const toggleReceive = (on: boolean) => {
    if (!on && settings.mode === "OPEN") return setRest(true); // 받는 중 끄기 → H-06
    if (on && settings.mode === "RESTING") return setConfirmOn(true); // 쉬는 중 켜기 → CF-21
    setSettings({ mode: on ? "OPEN" : "CLOSED" });
    toast(on ? "커피챗을 받기 시작했어요" : "커피챗을 받지 않아요");
  };

  return (
    <div className="-my-8 -mx-4 flex-1 bg-bg-50 px-4 py-7">
      <div className="mx-auto grid max-w-[1200px] grid-cols-[264px_576px_312px] justify-center gap-6">
        {/* ───── Left rail ───── */}
        <aside className="flex flex-col gap-5">
          <Card className="relative overflow-hidden rounded-[10px]">
            <div className="h-[60px] bg-part-fe-line" />
            <div className="absolute left-[19px] top-[23px]">
              <Avatar name={me.name} part={me.part} size={80} className="ring-4 ring-white" />
            </div>
            <div className="flex flex-col gap-2.5 px-5 pb-5 pt-[56px]">
              <p className="text-[18px] font-bold text-ink-900">{me.name}</p>
              <p className="text-[13px] text-ink-500">
                {me.generation}기 · {LION_PART_LABEL[me.part]}
              </p>
              <p className="text-[13px] text-ink-700">{me.headline}</p>
              <hr className="border-line-200" />
              <p className="text-[13px] leading-5 text-ink-700">{me.intro}</p>
              <Link href="/me" className="t-label2 text-primary hover:underline">
                내 프로필 →
              </Link>
            </div>
          </Card>

          {/* Card/커피챗 받기 */}
          <Card className="flex flex-col gap-[13px] rounded-[10px] p-5">
            <div className="flex items-center justify-between">
              <span className="flex items-center gap-1.5 text-[15px] text-ink-500">
                <Icon name="coffee" /> 커피챗 받기
              </span>
              <Switch checked={settings.mode === "OPEN"} onChange={toggleReceive} label="커피챗 받기" />
            </div>
            {settings.mode === "OPEN" ? (
              <>
                <p className="text-[14px] font-bold text-ink-900">커피챗 설정</p>
                <dl className="flex flex-col gap-2.5 text-[13px]">
                  <Row k="방식" v={settings.modes.map((m) => (m === "ONLINE" ? "온라인" : "오프라인")).join(" · ")} />
                  <Row k="선호 지역" v={settings.regions} />
                  <div className="flex flex-col gap-2">
                    <dt className="text-ink-500">키워드</dt>
                    <dd className="flex flex-wrap gap-1.5">
                      {settings.keywords.map((k) => (
                        <Tag key={k}>{k}</Tag>
                      ))}
                    </dd>
                  </div>
                </dl>
              </>
            ) : settings.mode === "RESTING" ? (
              <>
                <p className="text-[14px] font-bold text-ink-900">쉬는 중이에요</p>
                <p className="text-[13px] text-ink-500">{settings.restUntil ? `${settings.restUntil}까지 신청을 받지 않아요` : "다시 켜면 바로 신청을 받아요"}</p>
              </>
            ) : settings.mode === "SCHEDULED" ? (
              <>
                <p className="text-[14px] font-bold text-ink-900">{settings.openFrom}부터 받아요</p>
                <p className="text-[13px] text-ink-500">여는 기간이 시작되면 자동으로 켜져요</p>
              </>
            ) : (
              <>
                <p className="text-[14px] font-bold text-ink-900">커피챗을 받지 않아요</p>
                <p className="text-[13px] text-ink-500">켜면 후배들이 신청할 수 있어요</p>
              </>
            )}
            <Link href="/me/coffee-settings" className="t-label2 mt-3 text-ink-900 underline underline-offset-2">
              설정 수정 →
            </Link>
          </Card>

          <Card className="flex flex-col gap-2.5 rounded-[10px] px-5 py-4 text-[13px]">
            <StatLink href="/coffee" label="진행 중인 커피챗" value={active.length} />
            <StatLink href="/chat/requests" label="받은 메시지 요청" value={requests.length} />
            <StatLink href="/me/bookmarks" label="관심 동문" value={bookmarks.length} />
          </Card>
        </aside>

        {/* ───── Center ───── */}
        <section className="flex flex-col gap-6">
          {session.firstVisit && (
            <div className="flex items-start justify-between gap-4 rounded-[10px] border border-[#cce4ff] bg-[#f1f5fc] px-6 py-4">
              <div>
                <p className="t-body-bold text-ink-900">승인됐어요! 멋사동네에 온 걸 환영해요 🎉</p>
                <p className="t-caption1 text-ink-500">먼저 동문 찾기에서 같은 파트 선배를 둘러보세요.</p>
              </div>
              <button type="button" aria-label="닫기" onClick={() => update({ firstVisit: false })} className="text-ink-400 hover:text-ink-900">
                <Icon name="close" size={18} />
              </button>
            </div>
          )}
          <div className="flex flex-col gap-1">
            <h1 className="t-title text-ink-900">{me.name}님, 반가워요!</h1>
            <p className="t-body2 text-ink-500">{pendingReceived.length + unread + requests.length > 0 ? "확인해야할 메시지가 있어요 !" : "오늘도 좋은 하루 보내세요."}</p>
          </div>

          <div className="grid grid-cols-3 gap-5">
            <StatTile href="/coffee" icon="coffee" label="받은 커피챗 대기" value={pendingReceived.length} unit="건" accent />
            <StatTile href="/chat" icon="chat" label="안 읽은 메시지" value={unread} unit="개" />
            <StatTile href="/chat/requests" icon="mail" label="메세지 요청" value={requests.length} unit="건" />
          </div>

          {/* H-02 완성도 배너 */}
          {completion < 100 ? (
            <div className="flex flex-col gap-2 rounded-[10px] border border-[#cce4ff] bg-[#f1f5fc] px-7 py-[18px] shadow-[0_4px_4px_rgba(0,0,0,0.08)]">
              <div className="flex items-center justify-between">
                <span className="flex items-center gap-1.5 t-label1 text-ink-900">
                  <Glyph name="edit" size={20} /> 프로필을 완성해주세요
                </span>
                <Link href="/me" className="flex items-center gap-0.5 t-body-bold text-navy hover:underline">
                  프로필 채우기 <Glyph name="chevron-right" size={24} />
                </Link>
              </div>
              <ProgressBar value={completion} className="w-[317px]" />
              <p className="flex items-center gap-2.5 py-2 text-[14px] text-ink-900">
                <Glyph name="info" size={18} /> 프로필을 채우면 후배들이 더 쉽게 찾아와요
              </p>
            </div>
          ) : (
            <div className="rounded-[10px] border border-line-200 bg-white px-7 py-4 text-[14px] text-ink-700">프로필 완성도 100% · 할 일이 없어요 👍</div>
          )}

          {/* 선배 추천 */}
          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <h2 className="t-title3 text-ink-900">선배</h2>
              <Link href="/alumni" className="text-[13px] text-ink-500 hover:underline">
                전체보기 →
              </Link>
            </div>
            <div className="flex gap-5 py-2">
              {recommended.map((a) => (
                <AlumniCard key={a.id} person={a} onRequest={onRequest} onMessage={onMessage} className="w-[278px]" />
              ))}
            </div>
          </div>

          {/* 커뮤니티 새 소식 */}
          <Card className="flex flex-col gap-5 rounded-[10px] p-5">
            <div className="flex items-center justify-between">
              <h2 className="t-title3 text-ink-900">커뮤니티 새 소식</h2>
              <Link href="/community" className="text-[13px] text-ink-500 hover:underline">
                전체 보기 →
              </Link>
            </div>
            <div className="flex gap-7 border-b border-line-200">
              {NEWS_TABS.map((t) => (
                <button key={t} type="button" onClick={() => setTab(t)} className={cn("-mb-px border-b-2 pb-2 t-body-bold", tab === t ? "border-ink-800 text-orange-over" : "border-transparent text-ink-500")}>
                  {t}
                </button>
              ))}
            </div>
            <ul>
              {news.map((p) => (
                <li key={p.id} className="border-b border-line-200 last:border-b-0">
                  <Link href={`/community/${p.id}`} className="flex items-center justify-between gap-4 px-1 py-[18px] hover:bg-bg-50">
                    <div className="flex min-w-0 flex-col gap-2">
                      <div className="flex items-center gap-1.5">
                        <Tag className="font-semibold">{POST_CATEGORY_LABEL[p.category]}</Tag>
                        {(p.recruit || p.deadline) && <span className="rounded-[4px] border-[0.5px] border-orange bg-orange-back px-2 py-0.5 text-[12px] font-semibold text-ink-700">{daysLeft(p.recruit?.deadline ?? p.deadline!)}</span>}
                      </div>
                      <p className="truncate t-body-bold text-ink-900">{p.title}</p>
                      <p className="t-caption1 text-ink-500">
                        {p.anonymous ? "익명" : alumni.find((a) => a.id === p.authorId)?.name} · {p.anonymous ? "" : `${alumni.find((a) => a.id === p.authorId)?.generation}기 · `}
                        {timeAgo(p.createdAt)} 전{p.comments.length > 0 && ` · 댓글 ${p.comments.length}`}
                      </p>
                    </div>
                    {p.images?.[0] && <span className="grid h-[72px] w-[96px] shrink-0 place-items-center rounded-[8px] border border-dashed border-line-300 bg-bg-100 text-[12px] text-ink-400">Image</span>}
                  </Link>
                </li>
              ))}
            </ul>
          </Card>
        </section>

        {/* ───── Right rail ───── */}
        <aside className="flex flex-col gap-4">
          <Card className="flex flex-col gap-3 rounded-[10px] p-5">
            <h2 className="t-title3 text-ink-900">다가오는 커피챗</h2>
            {upcoming ? (
              <>
                <UpcomingRow chat={upcoming} />
                <div className="flex gap-2">
                  <Button size="S" variant="secondary" className="flex-1" href={upcoming.roomId ? `/chat/${upcoming.roomId}` : "/chat"}>
                    채팅
                  </Button>
                  <Button size="S" className="flex-1" href={`/coffee/${upcoming.id}`}>
                    상세
                  </Button>
                </div>
              </>
            ) : (
              <p className="py-2 text-[13px] text-ink-500">확정된 일정이 없어요. 받은 신청을 확인해보세요.</p>
            )}
          </Card>

          <Card className="flex flex-col gap-2.5 rounded-[10px] p-5">
            <div className="flex items-center justify-between">
              <h2 className="t-title3 text-ink-900">모집 중인 번개·커피챗</h2>
              <Link href="/community?cat=MEETUP" className="text-[12px] text-ink-500 hover:underline">
                더보기
              </Link>
            </div>
            <ul className="text-[13px]">
              {recruits.map((p) => (
                <li key={p.id} className="border-b border-line-200 last:border-b-0">
                  <Link href={`/community/${p.id}`} className="flex flex-col gap-[3px] py-2.5 hover:bg-bg-50">
                    <span className="truncate font-semibold text-ink-900">{p.title}</span>
                    <span className="text-ink-500">
                      {p.meetup ? `번개 · ${formatDateTime(p.meetup.at)} · 참여 ${p.meetup.memberIds.length}/${p.meetup.max}명` : p.recruit ? `커피챗 모집 · 마감 ${daysLeft(p.recruit.deadline)} · ${p.recruit.memberIds.length}/${p.recruit.max}명` : "행사"}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </Card>

          <Link href="/community/p-5" className="block">
            <Card className="flex flex-col gap-2.5 rounded-[10px] p-5 hover:bg-bg-50">
              <h2 className="t-title3 text-ink-900">1년 전 오늘</h2>
              <div className="grid h-[120px] place-items-center rounded-[8px] border border-dashed border-line-300 bg-bg-100 text-[12px] text-ink-400">
                <span className="flex flex-col items-center gap-1">
                  <Glyph name="image" size={20} /> Image
                </span>
              </div>
              <p className="t-body-bold text-ink-900">13기 중커톤 시상식 후기</p>
              <p className="t-label2 text-ink-500">지금 사자는 · 2025년 11월 3일</p>
            </Card>
          </Link>
          <p className="text-[11px] leading-[1.6] text-ink-400">이용약관 · 개인정보처리방침 · 피드백 보내기 · © 2026 LION:LINK</p>
        </aside>
      </div>

      {modals}

      {/* ◇[O] H-06 쉬기 팝업 / 받는 중에 끄기 */}
      <RestModal
        open={rest}
        onClose={() => setRest(false)}
        onConfirm={(mode, until) => {
          setRest(false);
          setSettings(mode === "REST" ? { mode: "RESTING", restUntil: until } : { mode: "CLOSED" });
          toast(mode === "REST" ? `${until}까지 쉬어요` : "커피챗을 받지 않아요");
        }}
      />
      {/* ◇[O] CF-21 쉬는 기간 끝내기 확인 */}
      <ConfirmModal open={confirmOn} onClose={() => setConfirmOn(false)} onConfirm={() => { setConfirmOn(false); setSettings({ mode: "OPEN", restUntil: undefined }); toast("쉬는 기간을 끝내고 다시 받기 시작했어요"); }} title="쉬는 기간을 끝낼까요?" description="지금 켜면 등록한 쉬는 기간이 삭제되고 바로 신청을 받아요." confirmLabel="끝내고 받기" />
      <span className="hidden">{router ? null : null}</span>
    </div>
  );
}

function Row({ k, v }: { k: string; v: string }) {
  return (
    <div className="flex items-start justify-between">
      <dt className="text-ink-500">{k}</dt>
      <dd className="text-right font-medium text-ink-900">{v}</dd>
    </div>
  );
}

function StatLink({ href, label, value }: { href: string; label: string; value: number }) {
  return (
    <Link href={href} className="flex items-center justify-between hover:underline">
      <span className="text-ink-700">{label}</span>
      <span className="text-[14px] font-bold text-ink-900">{value}</span>
    </Link>
  );
}

function StatTile({ href, icon, label, value, unit, accent }: { href: string; icon: "coffee" | "chat" | "mail"; label: string; value: number; unit: string; accent?: boolean }) {
  return (
    <Link href={href} className="flex flex-col gap-1.5 rounded-[9px] bg-white px-6 py-4 shadow-[0_4px_4px_rgba(0,0,0,0.1)] hover:shadow-[0_6px_12px_rgba(0,0,0,0.12)]">
      <span className="flex items-center gap-1.5 text-[13px] font-semibold text-ink-900">
        {icon === "mail" ? <Glyph name="mail" size={17} /> : <Icon name={icon} size={17} />}
        {label}
      </span>
      <span className="flex items-end gap-0.5">
        <span className={cn("text-[30px] font-extrabold leading-[35px] tracking-tight", accent ? "text-primary" : "text-[#1a1200]")}>{value}</span>
        <span className="pb-1 text-[16px] font-bold text-ink-900">{unit}</span>
      </span>
    </Link>
  );
}

function UpcomingRow({ chat }: { chat: { receiverId: string; requesterId: string; scheduledAt?: string; mode: string } }) {
  const { alumni } = useStore();
  const other = alumni.find((a) => a.id === (chat.receiverId === ME_ID ? chat.requesterId : chat.receiverId));
  const d = new Date(chat.scheduledAt!);
  return (
    <div className="flex items-center gap-3">
      <div className="flex size-[54px] shrink-0 flex-col items-center justify-center rounded-[8px] border-[0.8px] border-light-text bg-[#f1f5fc] text-navy">
        <span className="text-[11px]">{d.getMonth() + 1}월</span>
        <span className="text-[22px] font-bold leading-none">{d.getDate()}</span>
      </div>
      <div className="flex min-w-0 flex-col gap-1">
        <p className="flex items-center gap-0.5">
          <span className="t-body-bold text-ink-900">
            {other?.name} {other && other.generation < 13 ? "선배" : ""}
          </span>
          <span className="t-label1">・</span>
          <span className="t-label2 text-ink-600">
            {other?.generation}기 {other && LION_PART_LABEL[other.part]}
          </span>
        </p>
        <p className="t-caption1 text-ink-500">
          {formatDateTime(chat.scheduledAt!)} · {chat.mode === "ONLINE" ? "온라인" : "오프라인"}
        </p>
      </div>
    </div>
  );
}

/** H-06 쉬기 팝업 — 쉬는 기간 등록 또는 아예 끄기 */
function RestModal({ open, onClose, onConfirm }: { open: boolean; onClose: () => void; onConfirm: (mode: "REST" | "OFF", until?: string) => void }) {
  const [mode, setMode] = useState<"REST" | "OFF">("REST");
  const [until, setUntil] = useState(() => {
    const t = new Date();
    t.setDate(t.getDate() + 14);
    return t.toISOString().slice(0, 10);
  });
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="커피챗을 잠시 쉴까요?"
      description="쉬는 동안 동문 목록에 '쉬는 중'으로 보여요. 기간이 끝나면 자동으로 다시 받아요."
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button onClick={() => onConfirm(mode, until)}>{mode === "REST" ? "쉬기" : "받지 않기"}</Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <label className={cn("flex cursor-pointer items-start gap-3 rounded-[8px] border p-3", mode === "REST" ? "border-primary bg-part-be-bg/40" : "border-line-200")}>
          <input type="radio" name="rest" checked={mode === "REST"} onChange={() => setMode("REST")} className="mt-1 accent-primary" />
          <span className="flex flex-1 flex-col gap-2">
            <span className="t-label1 text-ink-900">기간을 정해서 쉬기</span>
            <span className="text-[13px] text-ink-500">쉬는 기간은 최대 5개까지 등록할 수 있어요.</span>
            <input type="date" value={until} onChange={(e) => setUntil(e.target.value)} className="h-9 w-[180px] rounded-[6px] border border-line-300 px-2 text-[14px]" />
          </span>
        </label>
        <label className={cn("flex cursor-pointer items-start gap-3 rounded-[8px] border p-3", mode === "OFF" ? "border-primary bg-part-be-bg/40" : "border-line-200")}>
          <input type="radio" name="rest" checked={mode === "OFF"} onChange={() => setMode("OFF")} className="mt-1 accent-primary" />
          <span className="flex flex-col">
            <span className="t-label1 text-ink-900">기간 없이 받지 않기</span>
            <span className="text-[13px] text-ink-500">다시 켤 때까지 신청을 받지 않아요.</span>
          </span>
        </label>
      </div>
    </Modal>
  );
}
