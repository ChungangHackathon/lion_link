const DAY = ["일", "월", "화", "수", "목", "금", "토"];

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
  const t = new Date(iso);
  return `${t.getMonth() + 1}/${t.getDate()} (${DAY[t.getDay()]}) ${pad(t.getHours())}:${pad(t.getMinutes())}`;
}

/** "11/20" */
export function formatDate(iso: string) {
  const t = new Date(iso);
  return `${t.getMonth() + 1}/${t.getDate()}`;
}

/** "2026.11.20" */
export function formatDateFull(iso: string) {
  const t = new Date(iso);
  return `${t.getFullYear()}.${pad(t.getMonth() + 1)}.${pad(t.getDate())}`;
}

/** "오후 7:00" */
export function formatTime(iso: string) {
  const t = new Date(iso);
  const h = t.getHours();
  return `${h < 12 ? "오전" : "오후"} ${h % 12 === 0 ? 12 : h % 12}:${pad(t.getMinutes())}`;
}

/** 남은 일수 ("D-3") */
export function daysLeft(iso: string) {
  const d = Math.ceil((new Date(iso).getTime() - Date.now()) / 86_400_000);
  return d <= 0 ? "오늘 마감" : `D-${d}`;
}

const pad = (n: number) => String(n).padStart(2, "0");
