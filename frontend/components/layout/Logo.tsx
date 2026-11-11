import Link from "next/link";
import { cn } from "@/lib/cn";

/** Wordmark "멋사동네" — NanumGothic ExtraBold 28, 멋사 blue / 동네 orange */
export function Logo({ href = "/home", className, size = 28 }: { href?: string; className?: string; size?: number }) {
  return (
    <Link href={href} className={cn("inline-flex items-baseline font-logo font-extrabold leading-none tracking-tight select-none", className)} style={{ fontSize: size }} aria-label="멋사동네 홈">
      <span className="text-primary">멋사</span>
      <span className="text-orange">동네</span>
    </Link>
  );
}
