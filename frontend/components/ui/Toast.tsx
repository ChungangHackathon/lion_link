"use client";

import { createContext, useCallback, useContext, useMemo, useState } from "react";
import { cn } from "@/lib/cn";

type ToastKind = "default" | "success" | "error";
interface ToastItem {
  id: number;
  text: string;
  kind: ToastKind;
  action?: { label: string; onClick: () => void };
}

const ToastContext = createContext<{ toast: (text: string, opts?: { kind?: ToastKind; action?: ToastItem["action"] }) => void } | null>(null);

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);
  const toast = useCallback((text: string, opts?: { kind?: ToastKind; action?: ToastItem["action"] }) => {
    const id = Date.now() + Math.random();
    setItems((s) => [...s, { id, text, kind: opts?.kind ?? "default", action: opts?.action }]);
    setTimeout(() => setItems((s) => s.filter((t) => t.id !== id)), 3500);
  }, []);
  const value = useMemo(() => ({ toast }), [toast]);
  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed bottom-8 left-1/2 z-[60] flex -translate-x-1/2 flex-col items-center gap-2">
        {items.map((t) => (
          <div key={t.id} className={cn("pointer-events-auto flex items-center gap-3 rounded-[8px] px-4 py-3 text-[14px] text-white shadow-lg", t.kind === "error" ? "bg-orange" : "bg-ink-900")}>
            {t.text}
            {t.action && (
              <button type="button" className="font-bold underline underline-offset-2" onClick={t.action.onClick}>
                {t.action.label}
              </button>
            )}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be used inside ToastProvider");
  return ctx.toast;
}
