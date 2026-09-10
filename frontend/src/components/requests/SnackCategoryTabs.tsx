"use client";

import { SnackSsgCategory, SnackSsgCategoryInfo } from "@/types";

export type SnackCatalogTab = SnackSsgCategory | "ALL";

export default function SnackCategoryTabs({
  value,
  onChange,
  categories,
  showAllTab = false,
}: {
  value: SnackCatalogTab;
  onChange: (code: SnackCatalogTab) => void;
  categories: SnackSsgCategoryInfo[];
  showAllTab?: boolean;
}) {
  if (!showAllTab && categories.length === 0) {
    return null;
  }
  return (
    <div className="flex gap-1.5 overflow-x-auto pb-1 -mx-1 px-1">
      {showAllTab && (
        <button
          type="button"
          onClick={() => onChange("ALL")}
          className={`shrink-0 px-3 py-1.5 rounded-full text-xs font-medium ${
            value === "ALL"
              ? "bg-forest-500 text-white"
              : "bg-gray-100 text-gray-600"
          }`}
        >
          전체
        </button>
      )}
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
