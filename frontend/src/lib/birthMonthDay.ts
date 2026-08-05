export type BirthMonthDay = {
  month: number | "";
  day: number | "";
};

export function daysInMonth(month: number): number {
  if (month === 2) return 29;
  if ([4, 6, 9, 11].includes(month)) return 30;
  return 31;
}

export function formatBirthMonthDay(month: number | null | undefined, day: number | null | undefined): string {
  if (month == null || day == null) return "";
  return `${String(month).padStart(2, "0")}/${String(day).padStart(2, "0")}`;
}

export function isValidBirthMonthDay(month: number | "", day: number | ""): boolean {
  if (month === "" || day === "") return false;
  if (month < 1 || month > 12) return false;
  return day >= 1 && day <= daysInMonth(month);
}
