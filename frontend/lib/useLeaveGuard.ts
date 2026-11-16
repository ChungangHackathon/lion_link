"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

/**
 * 저장하지 않은 변경이 있을 때 화면을 떠나려 하면 붙잡는다.
 * - 앱 안 링크(메뉴·GNB 등)를 누르면 이동을 멈추고 pendingHref를 채운다 → 화면이 '저장할까요?' 창을 띄운다
 * - 새로고침·탭 닫기는 브라우저 기본 확인창으로 막는다
 * App Router에는 이동을 막는 API가 없어서, 문서의 링크 클릭을 캡처 단계에서 가로챈다. (브라우저 뒤로가기는 막지 못한다)
 */
export function useLeaveGuard(dirty: boolean) {
  const router = useRouter();
  const [pendingHref, setPendingHref] = useState<string | null>(null);
  const dirtyRef = useRef(dirty);
  useEffect(() => {
    dirtyRef.current = dirty;
  });

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (!dirtyRef.current || e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
      const a = (e.target as Element | null)?.closest?.("a[href]") as HTMLAnchorElement | null;
      if (!a || a.target === "_blank" || a.hasAttribute("download")) return;
      const url = new URL(a.href, location.href);
      if (url.origin !== location.origin) return;
      if (url.pathname === location.pathname && url.search === location.search) return;
      e.preventDefault();
      e.stopPropagation();
      setPendingHref(url.pathname + url.search + url.hash);
    };
    const onUnload = (e: BeforeUnloadEvent) => {
      if (!dirtyRef.current) return;
      e.preventDefault();
      e.returnValue = "";
    };
    document.addEventListener("click", onClick, true);
    window.addEventListener("beforeunload", onUnload);
    return () => {
      document.removeEventListener("click", onClick, true);
      window.removeEventListener("beforeunload", onUnload);
    };
  }, []);

  return {
    pendingHref,
    /** 붙잡은 곳으로 이동한다 (저장했거나 버리기로 했을 때) */
    leave: () => {
      const href = pendingHref;
      dirtyRef.current = false;
      setPendingHref(null);
      if (href) router.push(href);
    },
    /** 이 화면에 남는다 */
    stay: () => setPendingHref(null),
  };
}
