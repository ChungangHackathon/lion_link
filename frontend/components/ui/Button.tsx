"use client";

import Link from "next/link";
import { cn } from "@/lib/cn";
import { Icon } from "./Icon";

/** Figma Button: variants primary/secondary/danger/text/chip/chip-selected, sizes L48/M40/S32. */
export type ButtonVariant = "primary" | "secondary" | "danger" | "text" | "chip" | "chip-selected";
export type ButtonSize = "L" | "M" | "S";

interface BaseProps {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  full?: boolean;
  className?: string;
  children?: React.ReactNode;
  leading?: React.ReactNode;
}

type ButtonProps = BaseProps &
  Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, "className" | "children"> & { href?: undefined };
type LinkProps = BaseProps & { href: string; disabled?: boolean; onClick?: React.MouseEventHandler };

const SIZE: Record<ButtonSize, string> = {
  L: "h-12 px-6 rounded-[8px] text-[15px] leading-5",
  M: "h-10 px-4 rounded-[8px] text-[13px] leading-[18px]",
  S: "h-8 px-3 rounded-[6px] text-[12px] leading-4",
};
const CHIP_SIZE: Record<ButtonSize, string> = {
  L: "h-12 px-[14px] rounded-full text-[15px]",
  M: "h-10 px-[14px] rounded-full text-[15px]",
  S: "h-8 px-[14px] rounded-full text-[13px]",
};

const VARIANT: Record<ButtonVariant, string> = {
  primary: "bg-primary text-white hover:bg-primary-hover disabled:bg-line-200 disabled:text-ink-400",
  secondary:
    "bg-white border border-line-300 text-ink-900 hover:bg-bg-100 hover:border-ink-400 disabled:bg-line-200 disabled:text-ink-400 disabled:border-transparent",
  danger: "bg-white border border-orange text-orange hover:bg-orange-back disabled:opacity-50",
  text: "text-ink-500 hover:text-ink-900 hover:bg-bg-100 disabled:text-ink-400",
  chip: "bg-white border border-line-300 text-ink-700 hover:border-ink-400 hover:bg-bg-100 disabled:text-ink-400",
  "chip-selected": "bg-primary text-white border border-primary",
};

export function Button(props: ButtonProps | LinkProps) {
  const { variant = "primary", size = "M", loading, full, className, children, leading, ...rest } = props;
  const isChip = variant === "chip" || variant === "chip-selected";
  const cls = cn(
    "inline-flex items-center justify-center gap-1.5 font-semibold whitespace-nowrap transition-colors select-none cursor-pointer disabled:cursor-not-allowed",
    isChip ? CHIP_SIZE[size] : SIZE[size],
    loading ? "bg-ink-500 text-white" : VARIANT[variant],
    full && "w-full",
    className,
  );
  const content = (
    <>
      {loading ? <Icon name="spinner" className="animate-spin" /> : leading}
      {children}
    </>
  );
  if ("href" in props && props.href !== undefined) {
    const { href, disabled, onClick } = props as LinkProps;
    if (disabled) return <span className={cn(cls, "opacity-60 cursor-not-allowed")}>{content}</span>;
    return (
      <Link href={href} className={cls} onClick={onClick}>
        {content}
      </Link>
    );
  }
  const { type = "button", disabled, ...btn } = rest as React.ButtonHTMLAttributes<HTMLButtonElement>;
  return (
    <button type={type} className={cls} disabled={disabled || loading} {...btn}>
      {content}
    </button>
  );
}

/** Round chip toggle used for 파트·상태·관심 분야 선택 */
export function Chip({
  selected,
  size = "S",
  children,
  ...rest
}: { selected?: boolean; size?: ButtonSize } & Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, "className">) {
  return (
    <Button variant={selected ? "chip-selected" : "chip"} size={size} aria-pressed={selected} {...rest}>
      {children}
    </Button>
  );
}
