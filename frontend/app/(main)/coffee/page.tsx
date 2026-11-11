"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useMemo, useState } from "react";
import { cn } from "@/lib/cn";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { COFFEE_STATUS_LABEL, type CoffeeChat, type CoffeeChatStatus } from "@/lib/types";
import { timeLeft } from "@/lib/common";
import { formatDate, formatDateTime } from "@/lib/format";
import { Avatar, AvatarGroup } from "@/components/ui/Avatar";
import { StatusBadge } from "@/components/ui/Badge";
import { Chip } from "@/components/ui/Button";
import { Card, EmptyState, Skeleton, Tabs } from "@/components/ui/Misc";
import { CoffeeChatDetail, statusTone } from "@/components/domain/CoffeeChatDetail";

type Tab = "received" | "sent";
type Filter = "ALL" | "PENDING" | "ACTIVE" | "ENDED";
const FILTERS: { value: Filter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "PENDING", label: "대기 중" },
  { value: "ACTIVE", label: "만남 준비 중" },
  { value: "ENDED", label: "종료" },
];
const ACTIVE: CoffeeChatStatus[] = ["ACCEPTED", "SCHEDULED"];
const ENDED: CoffeeChatStatus[] = ["COMPLETED", "REJECTED", "CANCELED", "EXPIRED"];

export default function CoffeePage() {
  return (
    <Suspense fallback={<Skeleton className="h-[600px] w-full" />}>
      <CoffeeList />
    </Suspense>
  );
}

/** ★ M-01 내 커피챗 / 받은 신청 · ◇ M-02 보낸 신청 — 목록 440 + 오른쪽 상세 */
function CoffeeList() {
  const params = useSearchParams();
  const router = useRouter();
  const { coffeeChats, alumni } = useStore();
  const [tab, setTab] = useState<Tab>(params.get("tab") === "sent" ? "sent" : "received");
  const [filter, setFilter] = useState<Filter>("ALL");

  const all = useMemo(() => coffeeChats.filter((c) => (tab === "received" ? c.receiverId === ME_ID : c.requesterId === ME_ID)), [coffeeChats, tab]);
  const list = all.filter((c) => filter === "ALL" || (filter === "PENDING" && c.status === "PENDING") || (filter === "ACTIVE" && ACTIVE.includes(c.status)) || (filter === "ENDED" && ENDED.includes(c.status)));
  const [selectedId, setSelectedId] = useState<string | null>(params.get("id"));
  const selected = list.find((c) => c.id === selectedId) ?? list[0];

  useEffect(() => {
    if (params.get("tab") === "sent") setTab("sent");
  }, [params]);

  const pendingReceived = coffeeChats.filter((c) => c.receiverId === ME_ID && c.status === "PENDING").length;
  const preparing = coffeeChats.filter((c) => (c.receiverId === ME_ID || c.requesterId === ME_ID) && ACTIVE.includes(c.status)).length;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-end justify-between">
        <h1 className="t-title text-ink-900">내 커피챗</h1>
        <p className="text-[13px] text-ink-500">
          받은 대기 {pendingReceived} · 만남 준비 중 {preparing}
        </p>
      </div>
      <Tabs
        value={tab}
        onChange={(t) => {
          setTab(t);
          setFilter("ALL");
          setSelectedId(null);
          router.replace(`/coffee${t === "sent" ? "?tab=sent" : ""}`);
        }}
        items={[
          { value: "received", label: "받은 신청" },
          { value: "sent", label: "보낸 신청" },
        ]}
      />

      <div className="grid grid-cols-[440px_1fr] gap-6 pt-2">
        <Card className="h-fit overflow-hidden">
          <div className="flex gap-2 border-b border-line-200 p-4">
            {FILTERS.map((f) => (
              <Chip key={f.value} size="M" selected={filter === f.value} onClick={() => setFilter(f.value)}>
                {f.label}
              </Chip>
            ))}
          </div>
          {list.length === 0 ? (
            <EmptyState compact title={tab === "received" ? "받은 신청이 없어요" : "보낸 신청이 없어요"} description={tab === "received" ? "커피챗 받기를 켜두면 후배들이 신청할 수 있어요" : "동문 찾기에서 선배에게 커피챗을 신청해보세요"} action={tab === "received" ? { label: "커피챗 설정", href: "/me/coffee-settings" } : { label: "동문 찾기", href: "/alumni" }} />
          ) : (
            <ul>
              {list.map((c) => (
                <ListRow key={c.id} chat={c} tab={tab} active={selected?.id === c.id} onClick={() => setSelectedId(c.id)} />
              ))}
            </ul>
          )}
        </Card>

        <Card className="h-fit p-6">
          {selected ? <CoffeeChatDetail key={selected.id} chat={selected} compact initialAction={params.get("id") === selected.id ? (params.get("action") as "accept" | "reject" | null) : null} /> : <EmptyState title="신청을 선택해주세요" />}
        </Card>
      </div>
      <span className="hidden">{alumni.length}</span>
    </div>
  );
}

function ListRow({ chat, tab, active, onClick }: { chat: CoffeeChat; tab: Tab; active: boolean; onClick: () => void }) {
  const { alumni } = useStore();
  const other = alumni.find((a) => a.id === (tab === "received" ? chat.requesterId : chat.receiverId));
  const members = chat.memberIds.map((id) => alumni.find((a) => a.id === id)!).filter(Boolean);
  const group = members.length > 0;
  const left = timeLeft(chat.expiresAt);
  if (!other) return null;
  return (
    <li className="border-b border-line-200 last:border-b-0">
      <button type="button" onClick={onClick} className={cn("flex w-full items-start gap-4 px-4 py-4 text-left hover:bg-bg-50", active && "bg-light-card")}>
        {group && tab === "received" ? <AvatarGroup people={[other, ...members]} size={32} /> : <Avatar name={other.name} part={other.part} size={40} />}
        <div className="flex min-w-0 flex-1 flex-col gap-1.5">
          <div className="flex items-baseline justify-between gap-2">
            <p className="truncate">
              <span className="t-body-bold text-ink-900">
                {other.name}
                {group && tab === "received" ? ` 외 ${members.length}명` : ""}
              </span>
              <span className="ml-1.5 text-[13px] text-ink-500">{other.generation}기</span>
            </p>
            <span className="shrink-0 text-[12px] text-ink-400">{formatDate(chat.createdAt)}</span>
          </div>
          <p className="truncate text-[13px] text-ink-700">
            {chat.topic} · {chat.message}
          </p>
          <div className="flex items-center gap-2.5 pt-1">
            <StatusBadge tone={statusTone(chat.status)}>{COFFEE_STATUS_LABEL[chat.status]}</StatusBadge>
            {chat.status === "PENDING" && <span className={cn("text-[12px] font-semibold", left.urgent ? "text-orange" : "text-ink-700")}>{left.text}</span>}
            {chat.status === "SCHEDULED" && chat.scheduledAt && <span className="text-[12px] text-ink-700">{formatDateTime(chat.scheduledAt)}</span>}
          </div>
        </div>
      </button>
    </li>
  );
}
