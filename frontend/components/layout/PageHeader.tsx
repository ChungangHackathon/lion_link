"use client";

import { useRouter } from "next/navigation";
import { cn } from "@/lib/cn";
import { Icon } from "@/components/ui/Icon";

/** PageHeader (90:986) — 제목만 / 뒤로 + 제목 / 제목 + 버튼 */
export function PageHeader({ title, back, action, className, description }: { title: React.ReactNode; back?: string | true; action?: React.ReactNode; className?: string; description?: React.ReactNode }) {
  const router = useRouter();
  return (
    <div className={cn("flex items-center justify-between gap-3", className)}>
      <div className="flex items-center gap-3">
        {back && (
          <button type="button" aria-label="뒤로" onClick={() => (back === true ? router.back() : router.push(back))} className="-ml-1 rounded p-1 hover:bg-bg-100">
            <Icon name="back" />
          </button>
        )}
        <div>
          <h1 className="t-title text-ink-900">{title}</h1>
          {description && <p className="t-body2 text-ink-500">{description}</p>}
        </div>
      </div>
      {action}
    </div>
  );
}
