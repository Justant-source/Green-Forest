"use client";

import { useState, useEffect } from "react";
import { CategoryInfo } from "@/types";
import { getCategories } from "@/lib/api";
import { setCategoryCache } from "@/lib/categoryCache";

interface CategoryFilterProps {
  selected: string | null;
  onSelect: (cat: string | null) => void;
}

export default function CategoryFilter({ selected, onSelect }: CategoryFilterProps) {
  const [categories, setCategories] = useState<CategoryInfo[]>([]);

  useEffect(() => {
    getCategories()
      .then((cats) => {
        if (cats.length > 0) {
          setCategories(cats);
          setCategoryCache(cats);
        }
      })
      .catch(console.error);
  }, []);

  return (
    <div className="flex gap-2 overflow-x-auto scrollbar-hide" style={{ WebkitOverflowScrolling: "touch" }}>
      <button
        onClick={() => onSelect(null)}
        className={`px-4 py-2 rounded-full text-sm font-medium transition-colors whitespace-nowrap shrink-0 ${
          selected === null
            ? "bg-orange-400 text-white"
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
            className={`px-4 py-2 rounded-full text-sm font-medium transition-colors whitespace-nowrap shrink-0 ${
              isActive
                ? "bg-orange-400 text-white"
                : "bg-white text-gray-700 border border-gray-300 hover:bg-gray-100"
            }`}
          >
            {cat.label}
          </button>
        );
      })}
    </div>
  );
}
