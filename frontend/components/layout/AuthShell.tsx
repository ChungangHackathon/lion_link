import { cn } from "@/lib/cn";
import { Gnb } from "./Gnb";

/** Centered card layout for 로그인·온보딩 screens (GNB 로고만). */
export function AuthShell({ children, width = 560, gnb = "logo", className }: { children: React.ReactNode; width?: number; gnb?: "logo" | "pending" | "default"; className?: string }) {
  return (
    <div className="flex min-h-full flex-1 flex-col">
      <Gnb variant={gnb} />
      <main className="flex flex-1 justify-center px-4 pb-16 pt-12">
        <div className={cn("flex w-full flex-col gap-6 rounded-[12px] border border-line-200 bg-white p-10", className)} style={{ maxWidth: width }}>
          {children}
        </div>
      </main>
    </div>
  );
}
