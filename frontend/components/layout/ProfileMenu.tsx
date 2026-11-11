"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useSession } from "@/lib/session";
import { LION_PART_LABEL } from "@/lib/types";
import { Icon } from "@/components/ui/Icon";
import { Glyph } from "@/components/ui/Icon";

/** Dropdown/ProfileMenu — 회원 (C-01) / 관리자 (C-01b) / 승인 대기 */
export function ProfileMenu({ onNavigate }: { onNavigate?: () => void }) {
  const { session, user, logout } = useSession();
  const router = useRouter();
  const isAdmin = session.role === "ADMIN";
  const isPending = session.status === "PENDING";

  const sub = isAdmin ? `${user?.generation}기 · 운영진 · 관리자` : isPending ? "승인 대기 중" : `${user?.generation}기 · ${user ? LION_PART_LABEL[user.part] : ""}`;

  const Item = ({ href, icon, children, muted }: { href: string; icon: React.ReactNode; children: React.ReactNode; muted?: boolean }) => (
    <Link href={href} onClick={onNavigate} className={`flex items-center gap-2 rounded-[6px] px-3 py-[5px] text-[15px] leading-6 hover:bg-bg-100 ${muted ? "text-ink-500" : "text-ink-900"}`}>
      {icon}
      {children}
    </Link>
  );

  return (
    <div className="flex flex-col p-2">
      <div className="flex flex-col gap-[3px] px-3 py-2">
        <p className="t-body-bold text-ink-900">{user?.name}</p>
        <p className="t-caption2 text-ink-500">{sub}</p>
      </div>
      <hr className="my-1 border-line-200" />
      <Item href="/me" icon={<Icon name="user" />}>
        내 프로필
      </Item>
      <Item href="/me/coffee-settings" icon={<Icon name="coffee" />}>
        커피챗 설정
      </Item>
      <hr className="my-1 border-line-200" />
      {isAdmin && (
        <>
          <Item href="/admin" icon={<Icon name="lock" />}>
            관리자 페이지
          </Item>
          <hr className="my-1 border-line-200" />
        </>
      )}
      <button
        type="button"
        onClick={() => {
          onNavigate?.();
          logout();
          router.push("/login");
        }}
        className="flex items-center gap-2 rounded-[6px] px-3 py-[5px] text-left text-[15px] leading-6 text-ink-500 hover:bg-bg-100"
      >
        <Glyph name="logout" size={18} />
        로그아웃
      </button>
    </div>
  );
}
