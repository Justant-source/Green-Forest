"use client";

import { createContext, useCallback, useContext, useEffect, useRef, useState, ReactNode } from "react";
import { getEventMode } from "@/lib/events/api";
import type { EventModeResponse, Event } from "@/lib/events/types";

interface EventModeContextType {
  mode: "ACTIVE" | "NONE";
  event: Event | null;
  /** 서버 기준 현재 시각 — 1초 간격 틱이 적용된 표시용 시계. */
  now: Date | null;
  refresh: () => void;
}

const EventModeContext = createContext<EventModeContextType>({
  mode: "NONE",
  event: null,
  now: null,
  refresh: () => {},
});

const POLL_INTERVAL_MS = 60_000;

export function EventModeProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<"ACTIVE" | "NONE">("NONE");
  const [event, setEvent] = useState<Event | null>(null);
  const [now, setNow] = useState<Date | null>(null);
  const offsetMsRef = useRef<number>(0); // server - client

  const applyMode = useCallback((res: EventModeResponse) => {
    setMode(res.mode);
    setEvent(res.event);
    const serverDate = new Date(res.serverNow);
    offsetMsRef.current = serverDate.getTime() - Date.now();
    setNow(serverDate);
  }, []);

  const fetchMode = useCallback(async () => {
    try {
      const res = await getEventMode();
      applyMode(res);
    } catch {
      /* 네트워크 장애 시 기존 상태 유지 */
    }
  }, [applyMode]);

  useEffect(() => {
    fetchMode();
    const poll = setInterval(fetchMode, POLL_INTERVAL_MS);
    const onFocus = () => fetchMode();
    window.addEventListener("focus", onFocus);
    document.addEventListener("visibilitychange", onFocus);
    return () => {
      clearInterval(poll);
      window.removeEventListener("focus", onFocus);
      document.removeEventListener("visibilitychange", onFocus);
    };
  }, [fetchMode]);

  // 1초 간격 카운트다운: 클라이언트 시계 + offset 으로 서버 시각 근사치를 재계산
  useEffect(() => {
    const tick = setInterval(() => {
      setNow(new Date(Date.now() + offsetMsRef.current));
    }, 1000);
    return () => clearInterval(tick);
  }, []);

  return (
    <EventModeContext.Provider value={{ mode, event, now, refresh: fetchMode }}>
      {children}
    </EventModeContext.Provider>
  );
}

export function useEventMode() {
  return useContext(EventModeContext);
}
