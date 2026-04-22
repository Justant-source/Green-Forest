export interface CategoryInfo {
  id: number;
  name: string;
  label: string;
  color: string;
  hasStatus?: boolean;
}

export interface Post {
  id: number;
  title: string;
  content: string;
  imageUrl: string | null;
  category: string;
  likeCount: number;
  viewCount: number;
  createdAt: string;
  commentCount: number;
  authorNickname: string | null;
  status?: string | null;
  categoryHasStatus?: boolean;
  imageUrls?: string[];
  bookmarked?: boolean;
  liked?: boolean;
  questId?: number | null;
  anonymous?: boolean;
  dropsAwarded?: number;
  taggedNicknames?: string[];
}

export interface Comment {
  id: number;
  content: string;
  authorName: string;
  createdAt: string;
  updatedAt: string | null;
  deleted: boolean;
  parentId: number | null;
  replies: Comment[];
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  last: boolean;
  number: number;
}

export interface User {
  id: number;
  email: string;
  nickname: string;
  name: string;
  role: string;
  plantType: string | null;
  plantTypeLabel: string | null;
  plantName: string | null;
  plantLocked: boolean;
  jobClass: string | null;
  jobClassLabel: string | null;
  jobClassLabelEn: string | null;
  element: string | null;
  elementLabel: string | null;
  difficulty: string | null;
  difficultyLabel: string | null;
  expMultiplier: number;
  partyId: number | null;
  partyName: string | null;
  totalDrops: number;
  createdAt: string;
}

export interface ConversationInfo {
  id: number;
  otherNickname: string;
  lastMessage: string;
  updatedAt: string;
  otherLeft: boolean;
}

export interface ChatMessage {
  id: number;
  conversationId: number;
  senderNickname: string | null;
  content: string;
  systemMessage: boolean;
  createdAt: string;
}

export interface CategoryRequestInfo {
  id: number;
  name: string;
  label: string;
  color: string;
  status: string;
  requesterNickname: string;
  rejectionReason: string | null;
  createdAt: string;
}

export interface Quest {
  id: number;
  title: string;
  description: string;
  rewardDrops: number;
  startDate: string;
  endDate: string;
  targetType: string;
  targetPartyId: number | null;
  maxCompletionsPerUser: number;
  active: boolean;
  voteType: boolean;
  createdByNickname: string;
  completionCount: number;
  myCompletionCount: number;
  createdAt: string;
}

export interface Notification {
  id: number;
  type: string;
  typeLabel: string;
  title: string;
  body: string;
  relatedPostId: number | null;
  relatedQuestId: number | null;
  isRead: boolean;
  createdAt: string;
}

export interface LeaderboardEntry {
  rank: number;
  partyId: number;
  partyName: string;
  totalDrops: number;
  memberCount: number;
}

export interface PartyMember {
  userId: number;
  nickname: string;
  plantType: string | null;
  jobClass: string | null;
  totalDrops: number;
}

export interface DropTransaction {
  id: number;
  amount: number;
  reasonType: string;
  reasonLabel: string;
  reasonDetail: string | null;
  relatedPostId: number | null;
  relatedQuestId: number | null;
  createdAt: string;
}

export interface AdminUser {
  id: number;
  email: string;
  nickname: string;
  name: string;
  role: string;
  plantType: string | null;
  plantName: string | null;
  jobClass: string | null;
  partyId: number | null;
  partyName: string | null;
  totalDrops: number;
}

export interface AdminPost {
  id: number;
  title: string;
  content: string;
  category: string;
  imageUrl: string | null;
  anonymous: boolean;
  createdAt: string;
  authorId: number | null;
  authorNickname: string | null;
  authorName: string | null;
}

export interface AdminPostPage {
  items: AdminPost[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface AdminParty {
  id: number;
  name: string;
  createdAt: string;
  memberCount: number;
}

export interface AdminStats {
  totalUsers: number;
  monthlyPosts: number;
  monthlyDropsIssued: number | null;
  monthlyTransactions: number;
  partyStats: {
    partyId: number;
    partyName: string;
    totalDrops: number;
    memberCount: number;
  }[];
}

// 출석 시스템
export interface AttendanceCheckinInfo {
  checkinId: number;
  checkinAt: string;
  stampStyle: string;
  message: string;
  dropsAwarded: number;
  todayCheckinCount: number;
  monthCheckinCount: number;
  streak: number;
}

export interface TodayBoardEntry {
  userId: number;
  nickname: string;
  plantType: string | null;
  jobClass: string | null;
  stampStyle: string;
  message: string;
  checkinAt: string;
  isWinner: boolean;
}

export interface TodayBoard {
  date: string;
  checkins: TodayBoardEntry[];
  isDrawDone: boolean;
  winner: { userId: number; nickname: string; checkinAt: string } | null;
}

export interface AttendanceDay {
  date: string;
  isWinner: boolean;
  message: string;
}

export interface AttendanceMonth {
  month: string;
  days: AttendanceDay[];
  checkinCount: number;
  winCount: number;
  streak: number;
}

export interface AttendancePhrase {
  id: number;
  phrase: string;
  category: string;
  active?: boolean;
}

// 뽑기 시스템
export interface GachaPrizeInfo {
  id: number;
  name: string;
  description: string | null;
  imageUrl: string | null;
  cashValue: number;
  remainingStock: number;
  tier: 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';
  tierLabel: string;
  currentProbability: number;
  displayOrder: number;
  active?: boolean;
  evMultiplier?: number; // admin only
}

export interface GachaDrawResult {
  drawId: number;
  isWinner: boolean;
  probability: number;
  rngValue?: number; // admin only
  prizeName: string;
  prizeImageUrl: string | null;
  prizeCashValue: number;
  remainingDrawsToday: number;
}

export interface GachaDrawRecord {
  id: number;
  prizeName: string;
  prizeCashValue: number;
  dropsSpent: number;
  winProbability: number;
  isWinner: boolean;
  deliveryStatus: 'NONE' | 'PENDING' | 'DELIVERED';
  createdAt: string;
}

export interface GachaRecentWin {
  drawId: number;
  userNickname: string;
  prizeName: string;
  probability: number;
  createdAt: string;
}

export interface GachaQuota {
  remainingToday: number;
  limit: number;
}

export interface GachaStats {
  remaining: number;
  dailyLimit: number;
  todayDrawCount: number;
  betCost: number;
  expectedReward: number;
  totalActivePrizes: number;
}

export interface PlazaWinner {
  type: 'GACHA' | 'ATTENDANCE';
  userNickname: string;
  prizeName?: string;
  checkinDate?: string;
  createdAt: string;
}

// 식물 성장
export interface PlantGrowth {
  stage: number;
  stageLabel: string;
  score: number;
  likesReceived: number;
  commentsReceived: number;
  praisesReceived: number;
  nextStageScore: number;
  lastGrownAt: string | null;
}

// 관리자용
export interface AdminCreatePrizeRequest {
  name: string;
  description?: string;
  imageUrl?: string;
  cashValue: number;
  totalStock: number;
  tier: 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';
  evMultiplier?: number;
  displayOrder?: number;
}

export interface AdminDeliveryItem {
  id: number;
  userNickname: string;
  prizeName: string;
  prizeCashValue: number;
  deliveryStatus: 'PENDING' | 'DELIVERED';
  deliveryMemo: string | null;
  createdAt: string;
  deliveredAt: string | null;
}

export interface AdminAttendanceDeliveryItem {
  id: number;
  winDate: string;
  userId: number;
  userName: string;
  userNickname: string;
  userEmail: string;
  checkinAt: string;
  winnerDrawnAt: string | null;
  message: string | null;
  deliveryStatus: 'PENDING' | 'DELIVERED';
  deliveryMemo: string | null;
  deliveredAt: string | null;
}

export interface MyAttendanceWin {
  id: number;
  date: string;
  checkinAt: string;
  winnerDrawnAt: string | null;
  message: string | null;
  deliveryStatus: 'NONE' | 'PENDING' | 'DELIVERED';
  deliveryMemo: string | null;
  deliveredAt: string | null;
}

export interface AdminUpdatePrizeRequest {
  name?: string;
  description?: string;
  imageUrl?: string;
  cashValue?: number;
  totalStock?: number;
  remainingStock?: number;
  evMultiplier?: number;
  active?: boolean;
  displayOrder?: number;
  tier?: 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';
}
