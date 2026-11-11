import { cn } from "@/lib/cn";

/** Icons exported from the Figma file (public/icons). Sizes match the design slots. */
export const ICONS = {
  user: { src: "/icons/user.svg", w: 18, h: 18 },
  "user-96": { src: "/icons/user-96.svg", w: 53, h: 53 },
  bell: { src: "/icons/bell.svg", w: 24, h: 24 },
  chat: { src: "/icons/chat.svg", w: 24, h: 24 },
  "chevron-down": { src: "/icons/chevron-down.svg", w: 16, h: 16 },
  spinner: { src: "/icons/spinner.svg", w: 16, h: 16 },
  lock: { src: "/icons/lock.svg", w: 16, h: 16 },
  star: { src: "/icons/star.svg", w: 20.72, h: 20.72 },
  "star-filled": { src: "/icons/star-filled.svg", w: 22, h: 22 },
  close: { src: "/icons/close.svg", w: 22, h: 22 },
  back: { src: "/icons/back.svg", w: 24, h: 24 },
  coffee: { src: "/icons/coffee.svg", w: 18, h: 18 },
  clock: { src: "/icons/clock.svg", w: 34, h: 34 },
} as const;

export type IconName = keyof typeof ICONS;

export function Icon({
  name,
  size,
  className,
  alt = "",
}: {
  name: IconName;
  size?: number;
  className?: string;
  alt?: string;
}) {
  const i = ICONS[name];
  const w = size ?? i.w;
  const h = size ? size * (i.h / i.w) : i.h;
  // eslint-disable-next-line @next/next/no-img-element
  return <img src={i.src} alt={alt} width={w} height={h} className={cn("block shrink-0", className)} draggable={false} />;
}

/* Small inline glyphs that are not separate assets in Figma (search, send, more, check, plus…). */
export function Glyph({ name, className, size = 16 }: { name: GlyphName; className?: string; size?: number }) {
  const common = { width: size, height: size, viewBox: "0 0 24 24", fill: "none", stroke: "currentColor", strokeWidth: 1.8, strokeLinecap: "round" as const, strokeLinejoin: "round" as const, className: cn("shrink-0", className) };
  switch (name) {
    case "search":
      return (
        <svg {...common}>
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.5-3.5" />
        </svg>
      );
    case "send":
      return (
        <svg {...common}>
          <path d="M22 2 11 13" />
          <path d="M22 2 15 22l-4-9-9-4 20-7Z" />
        </svg>
      );
    case "more":
      return (
        <svg {...common} fill="currentColor" stroke="none">
          <circle cx="5" cy="12" r="2" />
          <circle cx="12" cy="12" r="2" />
          <circle cx="19" cy="12" r="2" />
        </svg>
      );
    case "check":
      return (
        <svg {...common}>
          <path d="m5 12 5 5L20 7" />
        </svg>
      );
    case "plus":
      return (
        <svg {...common}>
          <path d="M12 5v14M5 12h14" />
        </svg>
      );
    case "edit":
      return (
        <svg {...common}>
          <path d="M12 20h9" />
          <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" />
        </svg>
      );
    case "trash":
      return (
        <svg {...common}>
          <path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6" />
        </svg>
      );
    case "chevron-right":
      return (
        <svg {...common}>
          <path d="m9 6 6 6-6 6" />
        </svg>
      );
    case "chevron-left":
      return (
        <svg {...common}>
          <path d="m15 6-6 6 6 6" />
        </svg>
      );
    case "chevron-up":
      return (
        <svg {...common}>
          <path d="m6 15 6-6 6 6" />
        </svg>
      );
    case "chevron-down":
      return (
        <svg {...common}>
          <path d="m6 9 6 6 6-6" />
        </svg>
      );
    case "calendar":
      return (
        <svg {...common}>
          <rect x="3" y="5" width="18" height="16" rx="2" />
          <path d="M16 3v4M8 3v4M3 11h18" />
        </svg>
      );
    case "mail":
      return (
        <svg {...common}>
          <rect x="3" y="5" width="18" height="14" rx="2" />
          <path d="m3 7 9 6 9-6" />
        </svg>
      );
    case "link":
      return (
        <svg {...common}>
          <path d="M10 13a5 5 0 0 0 7 0l3-3a5 5 0 0 0-7-7l-1 1" />
          <path d="M14 11a5 5 0 0 0-7 0l-3 3a5 5 0 0 0 7 7l1-1" />
        </svg>
      );
    case "eye":
      return (
        <svg {...common}>
          <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
          <circle cx="12" cy="12" r="3" />
        </svg>
      );
    case "heart":
      return (
        <svg {...common}>
          <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z" />
        </svg>
      );
    case "comment":
      return (
        <svg {...common}>
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2Z" />
        </svg>
      );
    case "users":
      return (
        <svg {...common}>
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M23 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8" />
        </svg>
      );
    case "logout":
      return (
        <svg {...common}>
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />
        </svg>
      );
    case "settings":
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="3" />
          <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z" />
        </svg>
      );
    case "info":
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="9" />
          <path d="M12 16v-4M12 8h.01" />
        </svg>
      );
    case "alert":
      return (
        <svg {...common}>
          <path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" />
          <path d="M12 9v4M12 17h.01" />
        </svg>
      );
    case "image":
      return (
        <svg {...common}>
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <path d="m21 15-5-5L5 21" />
        </svg>
      );
    case "copy":
      return (
        <svg {...common}>
          <rect x="9" y="9" width="13" height="13" rx="2" />
          <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
        </svg>
      );
    case "block":
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="9" />
          <path d="m5.6 5.6 12.8 12.8" />
        </svg>
      );
    case "pin":
      return (
        <svg {...common}>
          <path d="M12 22s7-6 7-12a7 7 0 1 0-14 0c0 6 7 12 7 12Z" />
          <circle cx="12" cy="10" r="2.5" />
        </svg>
      );
    case "video":
      return (
        <svg {...common}>
          <rect x="3" y="6" width="13" height="12" rx="2" />
          <path d="m16 10 5-3v10l-5-3" />
        </svg>
      );
    case "flash":
      return (
        <svg {...common}>
          <path d="M13 2 3 14h8l-1 8 10-12h-8l1-8Z" />
        </svg>
      );
  }
}

export type GlyphName =
  | "search"
  | "send"
  | "more"
  | "check"
  | "plus"
  | "edit"
  | "trash"
  | "chevron-right"
  | "chevron-left"
  | "chevron-up"
  | "chevron-down"
  | "calendar"
  | "mail"
  | "link"
  | "eye"
  | "heart"
  | "comment"
  | "users"
  | "logout"
  | "settings"
  | "info"
  | "alert"
  | "image"
  | "copy"
  | "block"
  | "pin"
  | "video"
  | "flash";
