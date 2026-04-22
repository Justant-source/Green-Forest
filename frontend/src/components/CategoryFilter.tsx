"use client";

import { useCategories } from "@/context/CategoryContext";

interface CategoryFilterProps {
  selected: string | null;
  onSelect: (cat: string | null) => void;
}

const getCategoryDotColor = (color: string | undefined): string => {
  if (!color || color === "white") return "bg-gray-400";
  const colorMap: Record<string, string> = {
    green: "bg-green-500",
    blue: "bg-blue-500",
    yellow: "bg-yellow-500",
    red: "bg-red-500",
    purple: "bg-purple-500",
    pink: "bg-pink-500",
    indigo: "bg-indigo-500",
    orange: "bg-orange-500",
  };
  return colorMap[color] || "bg-gray-400";
};

export default function CategoryFilter({ selected, onSelect }: CategoryFilterProps) {
  const { categories } = useCategories();

  return (
    <div className="sticky top-0 z-20 bg-white/95 backdrop-blur py-2 -mx-4 px-4 sm:-mx-6 sm:px-6 flex gap-2 overflow-x-auto scrollbar-hide" style={{ WebkitOverflowScrolling: "touch" }}>
      <button
        onClick={() => onSelect(null)}
        className={`px-4 py-2 rounded-full text-sm font-medium transition-colors whitespace-nowrap shrink-0 ${
          selected === null
            ? "bg-forest-500 text-white"
            : "bg-white text-gray-700 border border-gray-300 hover:bg-gray-100"
        }`}
      >
        전체
      </button>
      {categories.map((cat) => {
        const isActive = selected === cat.name;
        return (
          <button
            key={cat.name}
            onClick={() => onSelect(cat.name)}
            className={`flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition-colors whitespace-nowrap shrink-0 ${
              isActive
                ? "bg-forest-500 text-white"
                : "bg-white text-gray-700 border border-gray-300 hover:bg-gray-100"
            }`}
          >
            <div className={`w-2 h-2 rounded-full ${getCategoryDotColor(cat.color)}`} />
            {cat.label}
          </button>
        );
      })}
    </div>
  );
}
