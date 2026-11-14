import { cn } from "@/lib/cn";

/**
 * Icon/* set from Figma (수정 화면 › 03 홈 · 알림, 24px grid, stroke 1.8).
 * Files live in public/icons and are painted through a CSS mask so the icon
 * takes the current text colour (`text-ink-700`, `text-white` …).
 */
export const ICON_NAMES = [
  "alert", "back", "bell", "bell-off", "cal", "chat", "check", "chev-d", "chev-r", "clip", "clock", "close", "coffee", "copy",
  "download", "edit", "external", "filter", "image", "link", "lock", "mail", "menu", "monitor", "more", "pin", "place", "plus",
  "refresh", "search", "send", "spinner", "star", "star-f", "trash", "upload", "user", "users", "wifi-off",
] as const;
export type FigmaIconName = (typeof ICON_NAMES)[number];

/** Older names used around the codebase → Figma icon (+ optional rotation). */
const ALIAS: Record<string, { icon: FigmaIconName; rotate?: number }> = {
  "chevron-down": { icon: "chev-d" },
  "chevron-up": { icon: "chev-d", rotate: 180 },
  "chevron-right": { icon: "chev-r" },
  "chevron-left": { icon: "back" },
  "star-filled": { icon: "star-f" },
  calendar: { icon: "cal" },
  comment: { icon: "chat" },
  info: { icon: "alert" },
};

type InlineName = "heart" | "eye" | "logout" | "block" | "settings" | "flash";
export type IconName = FigmaIconName | keyof typeof ALIAS | InlineName;
export type GlyphName = IconName;

const INLINE: Record<InlineName, React.ReactNode> = {
  heart: <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z" />,
  eye: (
    <>
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8Z" />
      <circle cx="12" cy="12" r="3" />
    </>
  ),
  logout: <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />,
  block: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="m5.6 5.6 12.8 12.8" />
    </>
  ),
  settings: (
    <>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3h.1a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8v.1a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z" />
    </>
  ),
  flash: <path d="M13 2 3 14h9l-1 8 10-12h-9l1-8Z" />,
};

export function Icon({ name, size = 20, className, label }: { name: IconName; size?: number; className?: string; label?: string }) {
  if (name in INLINE) {
    return (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" className={cn("shrink-0", className)} role={label ? "img" : undefined} aria-label={label} aria-hidden={label ? undefined : true}>
        {INLINE[name as InlineName]}
      </svg>
    );
  }
  const a = ALIAS[name] ?? { icon: name as FigmaIconName };
  const url = `url(/icons/${a.icon}.svg)`;
  return (
    <span
      role={label ? "img" : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
      className={cn("inline-block shrink-0 bg-current", name === "spinner" && "animate-spin", className)}
      style={{
        width: size,
        height: size,
        maskImage: url,
        WebkitMaskImage: url,
        maskSize: "100% 100%",
        WebkitMaskSize: "100% 100%",
        maskRepeat: "no-repeat",
        WebkitMaskRepeat: "no-repeat",
        transform: a.rotate ? `rotate(${a.rotate}deg)` : undefined,
      }}
    />
  );
}

/** Same component under its old name (small inline glyphs used to be separate). */
export function Glyph({ name, className, size = 16 }: { name: GlyphName; className?: string; size?: number }) {
  return <Icon name={name} size={size} className={className} />;
}
