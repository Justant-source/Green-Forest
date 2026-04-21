"use client";

import { Suspense } from "react";
import GridFeed from "@/components/GridFeed";
import GachaRecentWinsTicker from "@/components/GachaRecentWinsTicker";

function HomeContent() {
  return (
    <div className="space-y-4">
      <GachaRecentWinsTicker />
      <GridFeed initialCategory={null} />
    </div>
  );
}

export default function HomePage() {
  return (
    <Suspense fallback={<div className="flex justify-center py-20"><div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" /></div>}>
      <HomeContent />
    </Suspense>
  );
}
