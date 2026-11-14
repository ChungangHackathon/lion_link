import Link from "next/link";
import { cn } from "@/lib/cn";

/**
 * GNB 워드마크 "멋사동네" (수정 화면 GNB 967:16637) — OkDanDan Bold 45, 멋사 CAU BLUE #004C97 / 동네 LION ORANGE #FF6000.
 * 폰트 파일은 재배포가 안 돼서 Figma에서 윤곽선으로 내보낸 SVG(public/brand/logo.svg, 128×36)를 쓴다.
 */
export function Logo({ href = "/home", className, height = 36 }: { href?: string; className?: string; height?: number }) {
  return (
    <Link href={href} className={cn("inline-flex shrink-0 select-none", className)} aria-label="멋사동네 홈">
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src="/brand/logo.svg" alt="멋사동네" width={(height * 128) / 36} height={height} draggable={false} />
    </Link>
  );
}
