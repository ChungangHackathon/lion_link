import type { Alumni } from "@/lib/types";
import { LION_PART_LABEL } from "@/lib/types";

/** 나와의 공통점 chips (P-05): 기수 · 파트 · 관심 분야 · 기술 */
export function commonalities(me: Alumni, other: Alumni): string[] {
  const out: string[] = [];
  if (other.generation === me.generation) out.push(`${me.generation}기`);
  if (other.part === me.part) out.push(`${LION_PART_LABEL[me.part]} 파트`);
  other.interests.filter((i) => me.interests.includes(i)).forEach((i) => out.push(`관심 분야 · ${i}`));
  other.skills.filter((s) => me.skills.includes(s)).forEach((s) => out.push(s));
  return out;
}

/** "13일 남음" / "23시간 남음" with urgency flag */
export function timeLeft(iso?: string): { text: string; urgent: boolean } {
  if (!iso) return { text: "", urgent: false };
  const ms = new Date(iso).getTime() - Date.now();
  if (ms <= 0) return { text: "만료됨", urgent: true };
  const h = Math.floor(ms / 3_600_000);
  if (h < 24) return { text: `${Math.max(1, h)}시간 남음`, urgent: true };
  return { text: `${Math.ceil(h / 24)}일 남음`, urgent: false };
}
