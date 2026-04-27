"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import BingoGrid from "@/components/events/photobingo/BingoGrid";
import { getEvent } from "@/lib/events/api";
import type { Event } from "@/lib/events/types";
import { useAuth } from "@/context/AuthContext";

export default function EventDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const { isLoggedIn, authLoaded } = useAuth();
  const eventId = Number(params?.id);
  const [event, setEvent] = useState<Event | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(eventId)) return;
    getEvent(eventId)
      .then(setEvent)
      .catch((e) => setError(e?.message ?? "이벤트를 불러올 수 없습니다."));
  }, [eventId]);

  useEffect(() => {
    if (authLoaded && !isLoggedIn) {
      router.replace("/login?redirect=" + encodeURIComponent(`/events/${eventId}`));
    }
  }, [authLoaded, isLoggedIn, eventId, router]);

  if (error) {
    return <div className="text-center text-red-500 py-10">{error}</div>;
  }
  if (!event) {
    return (
      <div className="flex justify-center py-20">
        <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <div className="flex items-center gap-2 text-sm">
        <Link href="/" className="text-gray-500 hover:text-gray-700">
          ← 뒤로
        </Link>
      </div>
      <div>
        <h1 className="text-xl font-bold text-gray-900">{event.title}</h1>
        {event.description && (
          <p className="text-sm text-gray-600 mt-1">{event.description}</p>
        )}
        <div className="text-xs text-gray-500 mt-1">
          상태: <span className="font-medium">{event.status}</span>
        </div>
      </div>

      {event.type === "PHOTO_BINGO" && event.config && (
        <BingoGrid
          eventId={event.id}
          eventStatus={event.status}
          rewards={event.config.rewards}
        />
      )}
    </div>
  );
}
