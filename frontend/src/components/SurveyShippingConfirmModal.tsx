"use client";

import { useState } from "react";
import { User } from "@/types";
import ShippingAddressForm from "./ShippingAddressForm";

interface Props {
  user: User;
  onConfirm: (recipientName: string) => void;
  onCancel: () => void;
}

export default function SurveyShippingConfirmModal({ user, onConfirm, onCancel }: Props) {
  const [editing, setEditing] = useState(false);
  const [currentUser, setCurrentUser] = useState<User>(user);
  const [recipientName, setRecipientName] = useState(user.name);

  const address = [
    currentUser.zipcode ? `(${currentUser.zipcode})` : "",
    currentUser.addressMain ?? "",
    currentUser.addressDetail ?? "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl w-full max-w-md shadow-2xl overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-100">
          <h2 className="text-base font-bold text-gray-800">배송 정보 확인</h2>
        </div>

        <div className="px-5 py-4 space-y-4">
          <div className="bg-amber-50 border border-amber-200 rounded-lg px-4 py-3 text-sm text-amber-800">
            ⚠ 응답 후에는 변경할 수 없습니다. 신중히 확인해 주세요.
          </div>

          {!editing ? (
            <div className="space-y-2">
              <div className="text-xs font-medium text-gray-500">수령자</div>
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={recipientName}
                  onChange={(e) => setRecipientName(e.target.value)}
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-forest-400"
                  placeholder="수령자 이름"
                />
              </div>

              <div className="text-xs font-medium text-gray-500 mt-2">배송지</div>
              <div className="text-sm text-gray-800 bg-gray-50 rounded-lg px-3 py-2 leading-relaxed">
                {address || <span className="text-gray-400">주소 없음</span>}
              </div>
              <div className="text-xs font-medium text-gray-500">휴대전화</div>
              <div className="text-sm text-gray-800 bg-gray-50 rounded-lg px-3 py-2">
                {currentUser.phone || <span className="text-gray-400">미등록</span>}
              </div>

              <button
                onClick={() => setEditing(true)}
                className="text-xs text-forest-600 underline underline-offset-2 hover:text-forest-800"
              >
                주소 수정하기
              </button>
            </div>
          ) : (
            <ShippingAddressForm
              user={currentUser}
              onSaved={(updated) => {
                setCurrentUser(updated);
                setEditing(false);
              }}
              showRecipientName={true}
              recipientName={recipientName}
              onRecipientNameChange={setRecipientName}
            />
          )}
        </div>

        {!editing && (
          <div className="px-5 py-4 border-t border-gray-100 flex gap-2">
            <button
              onClick={onCancel}
              className="flex-1 py-2 rounded-lg border border-gray-200 text-sm text-gray-600 hover:bg-gray-50 transition-colors"
            >
              취소
            </button>
            <button
              onClick={() => onConfirm(recipientName)}
              disabled={!currentUser.addressMain || !currentUser.phone}
              className="flex-1 py-2 rounded-lg bg-forest-500 text-white text-sm font-medium hover:bg-forest-600 disabled:opacity-50 transition-colors"
            >
              이 정보로 응답 확정
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
