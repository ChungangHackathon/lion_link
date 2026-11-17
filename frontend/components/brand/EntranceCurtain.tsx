"use client";

import { useEffect, useRef, useState, useSyncExternalStore } from "react";
import { createFabric, restBackground } from "./curtainFabric";

/**
 * 입장 커튼: 서비스에 들어와 첫 화면(로그인)이 뜰 때 커튼이 양쪽으로 걷히며 화면이 나온다.
 *
 * - 서버가 보낸 화면에는 CSS로 그린 닫힌 커튼이 먼저 보이고, 스크립트가 준비되면 캔버스가 이어받아 걷는다.
 * - 서비스를 쓰다가 로그인 화면으로 온 경우(로그아웃·로그인 만료)와 구글 로그인 실패로 돌아온 경우에는 보여 주지 않는다.
 * - 걷히는 중에 화면을 누르거나 키를 누르면 빨리 걷힌다. 움직임 줄이기를 켠 기기에서는 걷는 대신 옅어지며 사라진다.
 * - 스크립트가 끝내 돌지 않아도 화면을 가리고 있지 않도록 CSS가 8초 뒤에 커튼을 지운다 (globals.css).
 */

/** 이 페이지를 연 뒤로 커튼이 걷힌 적이 있는지 */
let opened = false;

/** 닫힌 모습을 보여 주는 최소 시간(ms) */
const HOLD_MS = 380;
/** 뒤 화면의 그림이 다 뜨기를 기다리는 최대 시간(ms) */
const WAIT_MS = 1100;
/** 누르면 이만큼 빨리 걷는다 */
const HURRY = 3.5;
/** 캔버스 픽셀 수의 상한 */
const MAX_PIXELS = 4_000_000;

const REST = { left: restBackground(-1), right: restBackground(1) };

const subscribeNothing = () => () => {};

function cameWithError() {
  return new URLSearchParams(window.location.search).has("error");
}

/** 화면 이동으로 들어왔을 때(서버가 그려 보낸 화면이 아닐 때) 커튼을 보여 줄지 */
function shouldPlay() {
  if (opened || cameWithError()) return false;
  // 이미 무언가 누른 적이 있으면 서비스를 쓰다가 온 것이다
  return !navigator.userActivation?.hasBeenActive;
}

/** 뒤 화면의 그림(로고·마을)이 다 뜨면 끝난다. 오래 걸리면 limit 뒤에 그냥 끝낸다 */
function imagesSettled(limit: number): Promise<void> {
  const pending = Array.from(document.images).filter((img) => !img.complete);
  if (pending.length === 0) return Promise.resolve();
  return new Promise((resolve) => {
    let left = pending.length;
    const one = () => {
      if (--left <= 0) resolve();
    };
    for (const img of pending) {
      img.addEventListener("load", one, { once: true });
      img.addEventListener("error", one, { once: true });
    }
    setTimeout(resolve, limit);
  });
}

export function EntranceCurtain() {
  const rootRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  // 서버가 그려 보낸 화면을 이어받는 중인지. 이때는 서버와 같은 모습(닫힌 커튼)으로 시작해야 한다
  const hydrating = useSyncExternalStore(
    subscribeNothing,
    () => false,
    () => true,
  );
  const [skip] = useState(() => !hydrating && !shouldPlay());
  // 로그인 화면이 주소에서 ?error를 곧 지우므로 처음 그릴 때 봐 둔다
  const [withError] = useState(() => typeof window !== "undefined" && cameWithError());
  const [done, setDone] = useState(false);

  useEffect(() => {
    const root = rootRef.current;
    const canvas = canvasRef.current;
    if (skip || !root || !canvas) return;
    // 스크립트가 늦어 CSS가 이미 커튼을 지웠으면 그대로 둔다
    if (getComputedStyle(root).visibility === "hidden") {
      opened = true;
      return;
    }

    let alive = true;
    let raf = 0;
    let timer: ReturnType<typeof setTimeout> | undefined;
    /** 걷지 않고 옅어지며 사라진다 */
    const leave = (state: "skip" | "fade", ms: number) => {
      opened = true;
      root.dataset.state = state;
      timer = setTimeout(() => setDone(true), ms);
    };
    const stop = () => {
      alive = false;
      cancelAnimationFrame(raf);
      clearTimeout(timer);
    };

    // 구글 로그인 실패로 돌아왔으면 안내가 바로 보이게 비켜 준다
    if (withError) {
      leave("skip", 200);
      return stop;
    }

    // 움직임 줄이기를 켰거나 WebGL을 쓸 수 없으면 걷지 않고 옅어지며 사라진다
    const fabric = window.matchMedia("(prefers-reduced-motion: reduce)").matches ? null : createFabric(canvas);
    if (!fabric) {
      timer = setTimeout(() => leave("fade", 450), HOLD_MS);
      return stop;
    }

    const fit = () => {
      const w = root.clientWidth;
      const h = root.clientHeight;
      // 아주 큰 화면에서도 가볍게: 캔버스가 MAX_PIXELS를 넘지 않게 해상도를 낮춘다 (천은 부드러운 그림이라 티가 나지 않는다)
      const dpr = Math.max(0.75, Math.min(window.devicePixelRatio || 1, 2, Math.sqrt(MAX_PIXELS / Math.max(1, w * h))));
      canvas.width = Math.round(w * dpr);
      canvas.height = Math.round(h * dpr);
      fabric.fit(w, h);
    };
    fit();
    fabric.draw(0);
    root.dataset.live = "";

    const mounted = performance.now();
    let last = mounted;
    let startAt = Infinity;
    let speed = 1;
    let opening = false;

    imagesSettled(WAIT_MS).then(() => {
      startAt = Math.min(startAt, Math.max(performance.now(), mounted + HOLD_MS));
    });

    const hurry = () => {
      speed = HURRY;
      startAt = Math.min(startAt, performance.now());
    };
    const resize = new ResizeObserver(() => {
      fit();
      fabric.draw((performance.now() - mounted) / 1000);
    });
    resize.observe(root);
    window.addEventListener("pointerdown", hurry);
    window.addEventListener("keydown", hurry);

    const frame = (now: number) => {
      if (!alive) return;
      // 탭이 가려져 있다가 돌아와도 건너뛰지 않게 한 번에 흐르는 시간을 묶어 둔다
      const dt = Math.min(0.05, Math.max(0, (now - last) / 1000));
      last = now;
      if (now >= startAt) {
        if (!opening) {
          opening = true;
          opened = true;
          root.dataset.state = "opening";
        }
        fabric.open(dt * speed);
      }
      fabric.draw((now - mounted) / 1000);
      if (opening && fabric.cleared()) {
        setDone(true);
        return;
      }
      raf = requestAnimationFrame(frame);
    };
    raf = requestAnimationFrame(frame);

    return () => {
      stop();
      resize.disconnect();
      window.removeEventListener("pointerdown", hurry);
      window.removeEventListener("keydown", hurry);
      delete root.dataset.live;
      delete root.dataset.state;
    };
  }, [skip, withError]);

  if (skip || done) return null;

  return (
    <div ref={rootRef} className="entrance-curtain" aria-hidden>
      <div className="entrance-curtain-panel right-0" style={{ backgroundImage: REST.right }} />
      <div className="entrance-curtain-panel left-0" style={{ backgroundImage: REST.left }} />
      <canvas ref={canvasRef} className="entrance-curtain-canvas" />
    </div>
  );
}
