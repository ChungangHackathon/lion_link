const DAY = ["일", "월", "화", "수", "목", "금", "토"];

/**
 * 서버 시각(+09:00)·날짜("2026-11-20")를 한국 시간 기준 달력 값으로 읽는다.
 * 브라우저·서버 렌더링 시간대가 달라도(예: 해외·UTC) 날짜와 시각이 밀리지 않게 9시간 더한 뒤 UTC 값으로 꺼낸다.
 */
function kst(iso: string) {
  const at = /^\d{4}-\d{2}-\d{2}$/.test(iso) ? `${iso}T00:00:00+09:00` : iso;
  const t = new Date(new Date(at).getTime() + 9 * 3_600_000);
  return { y: t.getUTCFullYear(), mo: t.getUTCMonth() + 1, d: t.getUTCDate(), day: t.getUTCDay(), h: t.getUTCHours(), mi: t.getUTCMinutes() };
}

/** "방금" / "3시간" / "2일" 형식의 상대 시간 (알림·채팅 목록용) */
export function timeAgo(iso: string) {
  const diff = Date.now() - new Date(iso).getTime();
  const m = Math.floor(diff / 60_000);
  if (m < 1) return "방금";
  if (m < 60) return `${m}분`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}시간`;
  const d = Math.floor(h / 24);
  if (d < 7) return `${d}일`;
  return formatDate(iso);
}

/** "11/20 (목) 19:00" */
export function formatDateTime(iso: string) {
  const t = kst(iso);
  return `${t.mo}/${t.d} (${DAY[t.day]}) ${pad(t.h)}:${pad(t.mi)}`;
}

/** "11/20" */
export function formatDate(iso: string) {
  const t = kst(iso);
  return `${t.mo}/${t.d}`;
}

/** "2026.11.20" */
export function formatDateFull(iso: string) {
  const t = kst(iso);
  return `${t.y}.${pad(t.mo)}.${pad(t.d)}`;
}

/** "오후 7:00" */
export function formatTime(iso: string) {
  const { h, mi } = kst(iso);
  return `${h < 12 ? "오전" : "오후"} ${h % 12 === 0 ? 12 : h % 12}:${pad(mi)}`;
}

/** 남은 일수 ("D-3") */
export function daysLeft(iso: string) {
  const d = Math.ceil((new Date(iso).getTime() - Date.now()) / 86_400_000);
  return d <= 0 ? "오늘 마감" : `D-${d}`;
}

const pad = (n: number) => String(n).padStart(2, "0");
