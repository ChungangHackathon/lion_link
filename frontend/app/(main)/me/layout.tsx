import { MY_NAV, SideNav } from "@/components/layout/SideNav";

/** 마이페이지 shell: SideNav 240 + content (P-02 · CS-01 · N-01 · X-01 · X-02 · X-03) */
export default function MeLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-[240px_1fr] gap-10">
      <SideNav title="마이페이지" items={MY_NAV} />
      <div className="min-w-0">{children}</div>
    </div>
  );
}
