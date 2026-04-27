"use client";

import { useEffect, useMemo, useState } from "react";
import BingoCell from "./BingoCell";
import BingoProgressBar from "./BingoProgressBar";
import CellUploadSheet from "./CellUploadSheet";
import BingoActivityTicker from "./BingoActivityTicker";
import { deleteCellImage, getMySubmission, uploadCellImage } from "@/lib/events/api";
import { compressBingoImage } from "@/lib/events/imageCompression";
import { estimateReward } from "@/lib/events/bingo-rules";
import { useAuth } from "@/context/AuthContext";
import type {
  PhotoBingoCellDto,
  PhotoBingoRewards,
  PhotoBingoSubmissionDto,
  EventStatus,
} from "@/lib/events/types";

interface Props {
  eventId: number;
  eventStatus: EventStatus;
  rewards: PhotoBingoRewards;
}

// StrictMode dev 2중 mount 시 useRef 가 새로 생성돼 컴포넌트 내부 가드가 무력화되는
// 문제를 피하기 위해 모듈 스코프에서 in-flight 요청을 병합한다. 같은 eventId 로 동시
// 호출되면 같은 Promise 를 공유해 서버에는 단 1회만 요청이 날아간다.
const inflightSubmission = new Map<number, Promise<PhotoBingoSubmissionDto>>();

function fetchSubmissionDedup(eventId: number): Promise<PhotoBingoSubmissionDto> {
  const existing = inflightSubmission.get(eventId);
  if (existing) return existing;
  const p = getMySubmission(eventId).finally(() => {
    inflightSubmission.delete(eventId);
  });
  inflightSubmission.set(eventId, p);
  return p;
}

export default function BingoGrid({ eventId, eventStatus, rewards }: Props) {
  const { user } = useAuth();
  const [submission, setSubmission] = useState<PhotoBingoSubmissionDto | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [sheetCell, setSheetCell] = useState<PhotoBingoCellDto | null>(null);
  const [uploadingIdx, setUploadingIdx] = useState<number | null>(null);
  const [localPreview, setLocalPreview] = useState<Record<number, string>>({});

  useEffect(() => {
    let cancelled = false;
    fetchSubmissionDedup(eventId)
      .then((s) => {
        if (!cancelled) setSubmission(s);
      })
      .catch((e: any) => {
        if (!cancelled) setError(e?.message ?? "로딩 실패");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [eventId]);

  const uploadedSet = useMemo(() => {
    const s = new Set<number>();
    submission?.cells.forEach((c) => {
      if (c.imageUrl) s.add(c.cellIndex);
    });
    return s;
  }, [submission]);

  const approvedSet = useMemo(() => {
    const s = new Set<number>();
    submission?.cells.forEach((c) => {
      if (c.scoreStatus === "APPROVED") s.add(c.cellIndex);
    });
    return s;
  }, [submission]);

  const scored = eventStatus === "ENDED" || eventStatus === "SCORED";
  const estimate = useMemo(
    () => estimateReward(scored ? approvedSet : uploadedSet, rewards),
    [uploadedSet, approvedSet, rewards, scored]
  );

  const handlePick = async (cell: PhotoBingoCellDto, file: File) => {
    setUploadingIdx(cell.cellIndex);
    setError(null);
    const preview = URL.createObjectURL(file);
    setLocalPreview((p) => ({ ...p, [cell.cellIndex]: preview }));
    try {
      const compressed = await compressBingoImage(file);
      const updated = await uploadCellImage(eventId, cell.cellIndex, compressed);
      setSubmission(updated);
    } catch (e: any) {
      setError(e?.message ?? "업로드 실패");
    } finally {
      setUploadingIdx(null);
      setLocalPreview((p) => {
        const copy = { ...p };
        delete copy[cell.cellIndex];
        return copy;
      });
      URL.revokeObjectURL(preview);
    }
  };

  const handleDelete = async (cellIndex: number) => {
    if (!confirm("이 사진을 삭제할까요?")) return;
    try {
      const updated = await deleteCellImage(eventId, cellIndex);
      setSubmission(updated);
    } catch (e: any) {
      setError(e?.message ?? "삭제 실패");
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-10">
        <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
      </div>
    );
  }
  if (!submission) {
    return <div className="text-center text-red-500 py-10">{error ?? "제출물을 불러올 수 없습니다."}</div>;
  }

  return (
    <div className="space-y-4">
      <BingoActivityTicker
        eventId={eventId}
        status={eventStatus}
        myUserId={user?.id ?? null}
      />

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
          {error}
        </div>
      )}

      <div className="grid grid-cols-3 gap-2">
        {submission.cells.map((cell) => (
          <BingoCell
            key={cell.id}
            cell={cell}
            localPreview={localPreview[cell.cellIndex]}
            uploading={uploadingIdx === cell.cellIndex}
            eventStatus={eventStatus}
            onTap={() => setSheetCell(cell)}
            onDelete={() => handleDelete(cell.cellIndex)}
          />
        ))}
      </div>

      <BingoProgressBar
        uploaded={uploadedSet.size}
        lines={estimate.lines}
        reward={estimate.reward}
        tier={estimate.tier}
      />

      {scored && (
        <div className="bg-forest-50 border border-forest-200 rounded-xl p-4 text-sm space-y-1">
          <div className="font-semibold text-forest-800">
            {eventStatus === "SCORED" ? "보상 지급 완료" : "채점을 기다리는 중"}
          </div>
          <div className="text-gray-700">
            달성한 줄: <b>{submission.achievedLines}줄</b>
            {submission.finalRewardDrops > 0 && (
              <>
                {" · 지급 물방울: "}
                <b className="text-forest-700">💧{submission.finalRewardDrops}</b>
              </>
            )}
          </div>
        </div>
      )}

      <CellUploadSheet
        theme={sheetCell?.theme ?? ""}
        open={sheetCell !== null}
        onClose={() => setSheetCell(null)}
        onPick={(file) => {
          if (sheetCell) handlePick(sheetCell, file);
        }}
      />
    </div>
  );
}
