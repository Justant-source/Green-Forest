"use client";

import { SnackSsgCategory, SnackSsgCategoryInfo } from "@/types";

export default function SnackCategoryTabs({
  value,
  onChange,
  categories,
}: {
  value: SnackSsgCategory;
  onChange: (code: SnackSsgCategory) => void;
  categories: SnackSsgCategoryInfo[];
}) {
  if (categories.length === 0) {
    return null;
  }
  return (
    <div className="flex gap-1.5 overflow-x-auto pb-1 -mx-1 px-1">
      {categories.map((c) => (
        <button
          key={c.code}
          type="button"
          onClick={() => onChange(c.code)}
          className={`shrink-0 px-3 py-1.5 rounded-full text-xs font-medium ${
            value === c.code
              ? "bg-forest-500 text-white"
              : "bg-gray-100 text-gray-600"
          }`}
        >
          {c.label}
        </button>
      ))}
    </div>
  );
}
