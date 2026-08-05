"use client";

import { daysInMonth } from "@/lib/birthMonthDay";

type Props = {
  month: number | "";
  day: number | "";
  onChange: (value: { month: number | ""; day: number | "" }) => void;
  required?: boolean;
  idPrefix?: string;
};

export default function BirthMonthDaySelect({
  month,
  day,
  onChange,
  required = false,
  idPrefix = "birth",
}: Props) {
  const maxDay = month === "" ? 31 : daysInMonth(month);

  const handleMonth = (raw: string) => {
    const nextMonth = raw === "" ? "" : Number(raw);
    let nextDay = day;
    if (nextMonth !== "" && day !== "" && day > daysInMonth(nextMonth)) {
      nextDay = "";
    }
    onChange({ month: nextMonth, day: nextDay });
  };

  const handleDay = (raw: string) => {
    onChange({ month, day: raw === "" ? "" : Number(raw) });
  };

  return (
    <div className="flex gap-2">
      <div className="flex-1">
        <label htmlFor={`${idPrefix}-month`} className="sr-only">월</label>
        <select
          id={`${idPrefix}-month`}
          value={month === "" ? "" : String(month)}
          onChange={(e) => handleMonth(e.target.value)}
          required={required}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-forest-500 bg-white"
        >
          <option value="">월</option>
          {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
            <option key={m} value={m}>{m}월</option>
          ))}
        </select>
      </div>
      <div className="flex-1">
        <label htmlFor={`${idPrefix}-day`} className="sr-only">일</label>
        <select
          id={`${idPrefix}-day`}
          value={day === "" ? "" : String(day)}
          onChange={(e) => handleDay(e.target.value)}
          required={required}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-forest-500 bg-white"
        >
          <option value="">일</option>
          {Array.from({ length: maxDay }, (_, i) => i + 1).map((d) => (
            <option key={d} value={d}>{d}일</option>
          ))}
        </select>
      </div>
    </div>
  );
}
