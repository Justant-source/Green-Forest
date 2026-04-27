"use client";

import { useCallback, useEffect, useState } from "react";
import { listEvents } from "@/lib/events/api";
import type { Event } from "@/lib/events/types";
import EventCreateForm from "./EventCreateForm";
import EventList from "./EventList";
import BingoScoringPanel from "./BingoScoringPanel";

type Mode = { kind: "list"; creating: boolean } | { kind: "scoring"; eventId: number };

export default function EventAdminTab() {
  const [events, setEvents] = useState<Event[] | null>(null);
  const [mode, setMode] = useState<Mode>({ kind: "list", creating: false });
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try {
      const list = await listEvents("DRAFT,SCHEDULED,ACTIVE,ENDED,SCORED");
      setEvents(list);
    } catch (e: any) {
      setError(e?.message ?? "로딩 실패");
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  if (mode.kind === "scoring") {
    return (
      <BingoScoringPanel
        eventId={mode.eventId}
        onBack={() => {
          setMode({ kind: "list", creating: false });
          refresh();
        }}
      />
    );
  }

  return (
    <div className="space-y-3">
      {!mode.creating && (
        <div className="flex justify-end">
          <button
            onClick={() => setMode({ kind: "list", creating: true })}
            className="px-3 py-1.5 rounded-full bg-forest-500 hover:bg-forest-600 text-white text-sm font-medium"
          >
            + 새 이벤트
          </button>
        </div>
      )}

      {error && <div className="text-sm text-red-500">{error}</div>}

      {mode.creating && (
        <EventCreateForm
          onCancel={() => setMode({ kind: "list", creating: false })}
          onCreated={() => {
            setMode({ kind: "list", creating: false });
            refresh();
          }}
        />
      )}

      {events === null ? (
        <div className="flex justify-center py-6">
          <div className="w-8 h-8 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
        </div>
      ) : (
        <EventList
          events={events}
          onRefresh={refresh}
          onSelect={(eventId) => setMode({ kind: "scoring", eventId })}
        />
      )}
    </div>
  );
}
