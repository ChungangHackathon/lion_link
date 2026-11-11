import type { Metadata } from "next";
import { Nanum_Gothic } from "next/font/google";
import "./globals.css";
import { SessionProvider } from "@/lib/session";
import { StoreProvider } from "@/lib/store";
import { ToastProvider } from "@/components/ui/Toast";

const nanum = Nanum_Gothic({
  weight: ["800"],
  subsets: ["latin"],
  variable: "--font-nanum",
  display: "swap",
});

export const metadata: Metadata = {
  title: "멋사동네 · LION:LINK",
  description: "1기부터 지금까지, 멋사 중앙대 선배와 후배를 잇는 동문 네트워크",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko" className={`${nanum.variable} h-full antialiased`}>
      <head>
        <link
          rel="stylesheet"
          href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css"
        />
      </head>
      <body className="min-h-full flex flex-col bg-white text-ink-900">
        <SessionProvider>
          <StoreProvider>
            <ToastProvider>{children}</ToastProvider>
          </StoreProvider>
        </SessionProvider>
      </body>
    </html>
  );
}
