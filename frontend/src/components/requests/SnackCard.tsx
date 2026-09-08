"use client";

import { SnackCatalogCard } from "@/types";
import { toMediaUrl } from "@/lib/api";
import SsgItemImageLink from "@/components/requests/SsgItemImageLink";

interface Props {
  item: SnackCatalogCard;
  busy?: boolean;
  onRequest: (catalogItemId: number) => void;
  onCancel: (requestId: number) => void;
}

export default function SnackCard({ item, busy, onRequest, onCancel }: Props) {
  const img = item.imageUrl ? toMediaUrl(item.imageUrl, "sm") : "";
  const requested = item.myPendingQty > 0 && item.myRequestId != null;

  return (
    <div className="bg-white border border-gray-100 rounded-xl p-3 flex flex-col">
      <SsgItemImageLink
        href={item.ssgUrl}
        className="aspect-square w-full rounded-lg bg-gray-100 overflow-hidden mb-2 block"
      >
        {img ? (
          <img src={img} alt={item.name} className="w-full h-full object-cover" />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-xs text-gray-300">이미지 없음</div>
        )}
      </SsgItemImageLink>
      <h3 className="text-sm font-medium text-gray-800 line-clamp-2 min-h-[2.5rem]">{item.name}</h3>
      {item.pendingQty > 0 && (
        <p className="text-xs text-forest-500 mt-1">신청 {item.pendingQty}명</p>
      )}
      {item.hiddenFromUsers && (
        <p className="text-xs text-gray-400 mt-0.5">숨김 상품</p>
      )}
      <div className="mt-auto pt-2">
        {requested ? (
          <button
            type="button"
            disabled={busy}
            onClick={() => onCancel(item.myRequestId!)}
            className="w-full text-xs text-gray-400 hover:text-red-500 py-2"
          >
            취소
          </button>
        ) : item.canRequest ? (
          <button
            type="button"
            disabled={busy}
            onClick={() => onRequest(item.id)}
            className="w-full py-2 rounded-lg bg-forest-500 text-white text-sm font-medium disabled:opacity-50"
          >
            요청하기
          </button>
        ) : null}
      </div>
    </div>
  );
}
