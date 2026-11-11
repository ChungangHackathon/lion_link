"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { Gnb } from "@/components/layout/Gnb";
import { useSession } from "@/lib/session";

/** Member area: GNB + page. Pending members are sent to /pending, logged-out to /login. */
export default function MainLayout({ children }: { children: React.ReactNode }) {
  const { session } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (!session.loggedIn) router.replace("/login");
    else if (session.status === "PENDING") router.replace("/pending");
    else if (session.status === "REJECTED") router.replace("/rejected");
  }, [session, router]);

  return (
    <div className="flex min-h-full flex-1 flex-col bg-white">
      <Gnb />
      <main className="container-page flex flex-1 flex-col py-8">{children}</main>
    </div>
  );
}
