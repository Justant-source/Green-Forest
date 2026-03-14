"use client";

import { useSearchParams } from "next/navigation";
import { Suspense } from "react";
import GridFeed from "@/components/GridFeed";

function HomeContent() {
  const searchParams = useSearchParams();
  const tab = searchParams.get("tab");

  return <GridFeed initialCategory={tab} />;
}

export default function HomePage() {
  return (
    <Suspense fallback={<div className="flex justify-center py-20"><div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" /></div>}>
      <HomeContent />
    </Suspense>
  );
}
