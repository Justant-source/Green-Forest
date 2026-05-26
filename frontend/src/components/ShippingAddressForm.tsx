"use client";

import { useState } from "react";
import { User } from "@/types";
import { updateMyProfile } from "@/lib/api";
import AddressSearchButton from "./AddressSearchButton";

interface Props {
  user: User;
  onSaved: (updated: User) => void;
  recipientName?: string;
  onRecipientNameChange?: (name: string) => void;
  showRecipientName?: boolean;
}

export default function ShippingAddressForm({
  user,
  onSaved,
  recipientName,
  onRecipientNameChange,
  showRecipientName = false,
}: Props) {
  const [zipcode, setZipcode] = useState(user.zipcode ?? "");
  const [addressMain, setAddressMain] = useState(user.addressMain ?? "");
  const [addressDetail, setAddressDetail] = useState(user.addressDetail ?? "");
  const [phone, setPhone] = useState(user.phone ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const handleSave = async () => {
    if (!addressMain.trim()) { setError("주소를 입력해 주세요."); return; }
    if (!phone.trim()) { setError("휴대전화 번호를 입력해 주세요."); return; }
    setError("");
    setSaving(true);
    try {
      const updated = await updateMyProfile({ zipcode, addressMain, addressDetail, phone });
      onSaved(updated);
    } catch {
      setError("저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-3">
      {showRecipientName && onRecipientNameChange && (
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-1">수령자 이름</label>
          <input
            type="text"
            value={recipientName ?? ""}
            onChange={(e) => onRecipientNameChange(e.target.value)}
            placeholder="수령자 이름"
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-forest-400"
          />
        </div>
      )}

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">주소</label>
        <div className="flex gap-2">
          <input
            type="text"
            value={zipcode}
            readOnly
            placeholder="우편번호"
            className="w-28 px-3 py-2 border border-gray-200 rounded-md text-sm bg-gray-50 focus:outline-none"
          />
          <AddressSearchButton
            onSelect={({ zipcode: z, addressMain: a }) => {
              setZipcode(z);
              setAddressMain(a);
            }}
          />
        </div>
        <input
          type="text"
          value={addressMain}
          readOnly
          placeholder="도로명 주소"
          className="mt-1 w-full px-3 py-2 border border-gray-200 rounded-md text-sm bg-gray-50 focus:outline-none"
        />
        <input
          type="text"
          value={addressDetail}
          onChange={(e) => setAddressDetail(e.target.value)}
          placeholder="상세 주소 (동/호수 등)"
          className="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-forest-400"
        />
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">휴대전화</label>
        <input
          type="tel"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          placeholder="010-0000-0000"
          className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-forest-400"
        />
      </div>

      {error && <p className="text-xs text-red-500">{error}</p>}

      <button
        onClick={handleSave}
        disabled={saving}
        className="w-full py-2 rounded-md bg-forest-500 text-white text-sm font-medium hover:bg-forest-600 disabled:opacity-50 transition-colors"
      >
        {saving ? "저장 중..." : "배송지 저장"}
      </button>
    </div>
  );
}
