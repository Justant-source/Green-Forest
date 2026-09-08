"use client";

import { RequestStatus } from "@/types";
import { toMediaUrl } from "@/lib/api";
import SsgItemImageLink from "@/components/requests/SsgItemImageLink";
import { formatKstDateTime, formatKstMonthDay } from "@/lib/datetime";

export interface MyRequestListItem {
  id: number;
  title: string;
  imageUrl?: string | null;
  ssgUrl?: string | null;
  quantity: number;
  status: RequestStatus;
  rejectReasonLabel?: string | null;
  rejectNote?: string | null;
  createdAt: string;
  purchasedAt?: string | null;
  rejectedAt?: string | null;
}

interface Props {
  items: MyRequestListItem[];
  emptyText: string;
  maxQty?: number;
  showQuantity?: boolean;
  busyId?: number | null;
  onQuantityChange?: (id: number, quantity: number) => void;
  onCancel: (id: number) => void;
}

function statusBadge(item: MyRequestListItem) {
  if (item.status === "PENDING") return { label: "대기", cls: "bg-forest-50 text-forest-600" };
  if (item.status === "PURCHASED") {
    const md = formatKstMonthDay(item.purchasedAt || item.createdAt);
    return { label: md ? `${md} 구매 완료` : "구매 완료", cls: "bg-gray-100 text-gray-600" };
  }
  const md = formatKstMonthDay(item.rejectedAt || item.createdAt);
  return { label: md ? `${md} 거절` : "거절", cls: "bg-red-50 text-red-500" };
}

export default function MyRequestList({
  items,
  emptyText,
  maxQty = 2,
  showQuantity = true,
  busyId,
  onQuantityChange,
  onCancel,
}: Props) {
  if (items.length === 0) {
    return <div className="text-center py-6 text-sm text-gray-400">{emptyText}</div>;
  }

  return (
    <div className="space-y-2">
      {items.map((item) => {
        const badge = statusBadge(item);
        const img = item.imageUrl ? toMediaUrl(item.imageUrl, "sm") : "";
        const pending = item.status === "PENDING";
        return (
          <div
            key={item.id}
            className="flex items-center gap-3 px-3 py-3 bg-white rounded-lg border border-gray-100"
          >
            {item.imageUrl !== undefined && (
              <SsgItemImageLink
                href={item.ssgUrl}
                className="w-12 h-12 rounded-lg bg-gray-100 overflow-hidden shrink-0 block"
              >
                {img ? (
                  <img src={img} alt={item.title} className="w-full h-full object-cover" />
                ) : (
                  <div className="w-full h-full" />
                )}
              </SsgItemImageLink>
            )}
            <div className="flex-1 min-w-0">
              <div className="text-sm font-medium text-gray-800 truncate">{item.title}</div>
              <div className="text-xs text-gray-400 mt-0.5">
                {showQuantity ? `${item.quantity}개 · ` : ""}
                {formatKstDateTime(item.createdAt)}
              </div>
              {item.status === "REJECTED" && (
                <div className="text-xs text-red-500 mt-0.5">
                  {item.rejectReasonLabel}
                  {item.rejectNote ? ` · ${item.rejectNote}` : ""}
                </div>
              )}
            </div>
            <span className={`text-[10px] font-medium px-2 py-0.5 rounded-full shrink-0 ${badge.cls}`}>
              {badge.label}
            </span>
            {pending && onQuantityChange && (
              <div className="flex items-center gap-1 shrink-0">
                <button
                  type="button"
                  disabled={busyId === item.id || item.quantity <= 1}
                  onClick={() => onQuantityChange(item.id, item.quantity - 1)}
                  className="w-7 h-7 rounded bg-gray-100 text-sm disabled:opacity-40"
                >
                  −
                </button>
                <span className="w-4 text-center text-sm">{item.quantity}</span>
                <button
                  type="button"
                  disabled={busyId === item.id || item.quantity >= maxQty}
                  onClick={() => onQuantityChange(item.id, item.quantity + 1)}
                  className="w-7 h-7 rounded bg-gray-100 text-sm disabled:opacity-40"
                >
                  ＋
                </button>
              </div>
            )}
            {pending && (
              <button
                type="button"
                disabled={busyId === item.id}
                onClick={() => onCancel(item.id)}
                className="text-xs text-gray-400 hover:text-red-500 shrink-0"
              >
                취소
              </button>
            )}
          </div>
        );
      })}
    </div>
  );
}
