"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/context/AuthContext";

const tabs = [
  { href: "/requests/snacks", label: "간식" },
  { href: "/requests/supplies", label: "사무용품" },
];

export default function RequestsLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { isLoggedIn, authLoaded } = useAuth();

  useEffect(() => {
    if (!authLoaded) return;
    if (!isLoggedIn) {
      router.replace("/login");
    }
  }, [authLoaded, isLoggedIn, router]);

  if (!authLoaded || !isLoggedIn) return null;

  return (
    <div className="max-w-2xl mx-auto pb-8">
      <div className="flex gap-1 mb-6 border-b">
        {tabs.map((t) => (
          <Link
            key={t.href}
            href={t.href}
            className={`px-4 py-2.5 text-sm font-medium transition-colors border-b-2 -mb-px ${
              pathname === t.href
                ? "border-forest-500 text-forest-500"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            {t.label}
          </Link>
        ))}
      </div>
      {children}
    </div>
  );
}
