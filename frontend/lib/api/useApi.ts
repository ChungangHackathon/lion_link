"use client";

import { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";

export interface ApiState<T> {
  data: T | undefined;
  error: unknown;
  loading: boolean;
  /** 같은 조건으로 다시 불러온다 */
  reload: () => void;
  /** 서버 응답을 기다리지 않고 화면 값을 바로 바꾼다 (좋아요 등) */
  setData: (update: (prev: T | undefined) => T | undefined) => void;
}

/**
 * 화면에서 API를 불러올 때 쓰는 작은 훅. deps가 바뀌면 다시 부르고, 이전 요청은 취소한다.
 * enabled가 false면 부르지 않는다 (예: 앞선 데이터가 아직 없을 때). deps 길이는 항상 같아야 한다.
 *
 * @example
 *   const { data, loading, error } = useApi((signal) => service.list({ q }, signal), [service, q]);
 */
export function useApi<T>(load: (signal: AbortSignal) => Promise<T>, deps: unknown[], enabled = true): ApiState<T> {
  const [data, setDataState] = useState<T | undefined>(undefined);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(enabled);
  const [tick, setTick] = useState(0);
  const loader = useRef(load);

  useLayoutEffect(() => {
    loader.current = load;
  });

  useEffect(() => {
    if (!enabled) return;
    const controller = new AbortController();
    setLoading(true);
    loader.current(controller.signal).then(
      (value) => {
        if (controller.signal.aborted) return;
        setDataState(value);
        setError(null);
        setLoading(false);
      },
      (err) => {
        if (controller.signal.aborted) return;
        setError(err);
        setLoading(false);
      },
    );
    return () => controller.abort();
    // deps는 호출하는 쪽이 정한다
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, tick, ...deps]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  const setData = useCallback((update: (prev: T | undefined) => T | undefined) => setDataState(update), []);

  return { data, error, loading, reload, setData };
}
