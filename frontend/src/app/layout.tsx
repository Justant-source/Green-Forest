import type { Metadata } from "next";
import "./globals.css";
import dynamic from "next/dynamic";
import { AuthProvider } from "@/context/AuthContext";
import { CategoryProvider } from "@/context/CategoryContext";
import { notoSansKR } from "@/lib/fonts";

const Header = dynamic(() => import("@/components/Header"), { ssr: false });
const BottomNav = dynamic(() => import("@/components/BottomNav"), { ssr: false });

export const metadata: Metadata = {
  title: "오피스 그린 메이커 - 식물 RPG 커뮤니티",
  description: "함께 성장하는 식물 RPG 커뮤니티",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <body className={`${notoSansKR.className} bg-forest-50 min-h-screen pb-16`} suppressHydrationWarning>
        <AuthProvider>
          <CategoryProvider>
            <Header />
            <main className="max-w-7xl mx-auto px-4 py-6">{children}</main>
            <BottomNav />
          </CategoryProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
