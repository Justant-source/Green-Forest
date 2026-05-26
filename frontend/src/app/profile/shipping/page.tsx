"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { getMe } from "@/lib/api";
import { User } from "@/types";
import ShippingAddressForm from "@/components/ShippingAddressForm";

export default function ShippingPage() {
  const { isLoggedIn, authLoaded } = useAuth();
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (authLoaded && !isLoggedIn) {
      router.replace("/login");
      return;
    }
    if (isLoggedIn) {
      getMe().then(setUser).catch(() => null);
    }
  }, [authLoaded, isLoggedIn, router]);

  if (!authLoaded || !user) {
    return <div className="flex items-center justify-center min-h-screen text-gray-400 text-sm">불러오는 중...</div>;
  }

  return (
    <main className="max-w-lg mx-auto px-4 py-8">
      <button
        onClick={() => router.back()}
        className="text-sm text-gray-500 hover:text-gray-700 mb-4 flex items-center gap-1"
      >
        ← 뒤로
      </button>

      <h1 className="text-xl font-bold text-gray-800 mb-6">배송지 관리</h1>

      {saved && (
        <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">
          배송지가 저장되었습니다.
        </div>
      )}

      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm px-5 py-5">
        <ShippingAddressForm
          user={user}
          onSaved={(updated) => {
            setUser(updated);
            setSaved(true);
            setTimeout(() => setSaved(false), 3000);
          }}
        />
      </div>

      <p className="mt-4 text-xs text-gray-400">
        등록된 배송지는 배송 대상 설문에 응답할 때 자동으로 사용됩니다.
        응답 시점에 수정도 가능합니다.
      </p>
    </main>
  );
}
