export interface WeeklyReport {
  id: number;
  userId: number;
  weekStart: string; // ISO date format (YYYY-MM-DD)
  weekEnd: string;   // ISO date format (YYYY-MM-DD)
  earnedAmount: number;
  partyRank: number | null;
}
