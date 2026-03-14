"use client";

interface SortSelectorProps {
  value: string;
  onChange: (sort: string) => void;
}

export default function SortSelector({ value, onChange }: SortSelectorProps) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="px-3 py-2 rounded-lg border border-gray-300 bg-white text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-forest-500"
    >
      <option value="latest">최신순</option>
      <option value="popular">인기순</option>
      <option value="views">조회순</option>
    </select>
  );
}
