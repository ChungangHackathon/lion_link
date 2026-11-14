"use client";

import { useEffect } from "react";
import { createPortal } from "react-dom";
import { cn } from "@/lib/cn";
import { Button } from "./Button";
import { Icon } from "./Icon";

/**
 * Modal shell (Figma Modal/Form). Closes on ✕, 취소, Esc, or backdrop click
 * — per GD Guide "모달 ✕·취소·바깥 클릭 → 닫힘".
 */
export function Modal({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  width = 480,
  className,
}: {
  open: boolean;
  onClose: () => void;
  title?: React.ReactNode;
  description?: React.ReactNode;
  children?: React.ReactNode;
  footer?: React.ReactNode;
  width?: number;
  className?: string;
}) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    document.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = prev;
    };
  }, [open, onClose]);

  if (!open || typeof document === "undefined") return null;

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink-900/50 p-4" onMouseDown={(e) => e.target === e.currentTarget && onClose()} role="presentation">
      <div role="dialog" aria-modal="true" className={cn("flex max-h-[90vh] w-full flex-col gap-4 overflow-hidden rounded-[12px] bg-white p-7 shadow-[0_12px_40px_rgba(17,24,39,0.18)]", className)} style={{ maxWidth: width }}>
        {(title || description) && (
          <div className="flex items-start justify-between gap-4">
            <div className="flex flex-col gap-1.5">
              {title && <h2 className={cn("text-ink-900", children ? "t-title3" : "t-body-bold")}>{title}</h2>}
              {description && <p className="t-body2 text-ink-500">{description}</p>}
            </div>
            <button type="button" onClick={onClose} aria-label="닫기" className="-m-1 shrink-0 rounded p-1 hover:bg-bg-100">
              <Icon name="close" size={22} className="text-ink-900" />
            </button>
          </div>
        )}
        {children && <div className="min-h-0 flex-1 overflow-y-auto">{children}</div>}
        {footer && <div className="flex justify-end gap-2 pt-3">{footer}</div>}
      </div>
    </div>,
    document.body,
  );
}

/** Modal/Confirm — 기본 / 위험 */
export function ConfirmModal({
  open,
  onClose,
  onConfirm,
  title,
  description,
  confirmLabel = "확인",
  cancelLabel = "취소",
  danger,
  loading,
}: {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: React.ReactNode;
  description?: React.ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
  loading?: boolean;
}) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      description={description}
      footer={
        <>
          <Button variant="secondary" size="S" onClick={onClose}>
            {cancelLabel}
          </Button>
          <Button variant={danger ? "danger" : "primary"} size="S" onClick={onConfirm} loading={loading}>
            {confirmLabel}
          </Button>
        </>
      }
    />
  );
}

/** Dropdown / popover anchored to a trigger. */
export function Popover({ open, onClose, children, align = "right", className }: { open: boolean; onClose: () => void; children: React.ReactNode; align?: "left" | "right"; className?: string }) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, onClose]);
  if (!open) return null;
  return (
    <>
      <div className="fixed inset-0 z-30" onClick={onClose} aria-hidden />
      <div className={cn("absolute top-full z-40 mt-2 rounded-[10px] border border-line-300 bg-white shadow-dropdown", align === "right" ? "right-0" : "left-0", className)}>{children}</div>
    </>
  );
}
