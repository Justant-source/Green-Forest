export interface CategoryInfo {
  id: number;
  name: string;
  label: string;
  color: string;
  hasStatus?: boolean;
  adminOnly?: boolean;
}

export type SurveyOptionType = "TEXT_ONLY" | "IMAGE_ONLY" | "TEXT_AND_IMAGE";

export interface SurveyOption {
  id: number;
  type: SurveyOptionType;
  text: string | null;
  imageUrl: string | null;
  voteCount: number;
  voted: boolean;
  addedByUser: boolean;
}

export interface Survey {
  id: number;
  postId: number;
  closesAt: string;
  anonymous: boolean;
  allowOptionAddByUser: boolean;
  allowMultiSelect: boolean;
  notice: boolean;
  closed: boolean;
  requiresShipping: boolean;
  totalVotes: number;
  options: SurveyOption[];
  hasVoted: boolean;
}

export interface SurveyNotice {
  surveyId: number;
  postId: number;
  title: string;
  closesAt: string;
}

export interface SurveyVoter {
  userId: number;
  nickname: string;
  name: string;
  votedAt: string;
}

export interface SurveyVoteDetail {
  optionId: number;
  text: string | null;
  imageUrl: string | null;
  voteCount: number;
  voters: SurveyVoter[];
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
  isAuthor?: boolean;
  dropsAwarded?: number;
  taggedNicknames?: string[];
  photoExhibitionSubmissionId?: number | null;
  photoExhibitionEventId?: number | null;
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
  birthMonth?: number | null;
  birthDay?: number | null;
  zipcode?: string | null;
  addressMain?: string | null;
  addressDetail?: string | null;
  phone?: string | null;
}

export interface AdminSurveyDelivery {
  id: number;
  surveyId: number;
  surveyTitle: string;
  userId: number;
  userNickname: string;
  userName: string;
  optionId: number;
  optionText: string | null;
  optionImageUrl: string | null;
  recipientName: string;
  recipientPhone: string;
  recipientZipcode: string;
  recipientAddressMain: string;
  recipientAddressDetail: string | null;
  deliveryStatus: "PENDING" | "SHIPPED" | "DELIVERED" | "CANCELED";
  deliveryStatusLabel: string;
  trackingNumber: string | null;
  deliveredAt: string | null;
  deliveredBy: number | null;
  deliveryMemo: string | null;
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
  relatedRequestKind: "SNACK" | "SUPPLY" | null;
  isRead: boolean;
  createdAt: string;
}

export type RequestStatus = "PENDING" | "PURCHASED" | "REJECTED";
export type RequestRejectReason = "OUT_OF_STOCK" | "POLICY" | "DUPLICATE" | "OTHER";

export type SnackSsgCategory =
  | "SNACK"
  | "COOKIE"
  | "SAND"
  | "PIE"
  | "BABY"
  | "TRADITIONAL"
  | "ANJU"
  | "CANDY"
  | "JELLY"
  | "CHOCOLATE"
  | "CEREAL"
  | "ICE_CREAM";

export interface SnackSsgCategoryInfo {
  code: SnackSsgCategory;
  label: string;
}

export const SNACK_SSG_CATEGORIES: SnackSsgCategoryInfo[] = [
  { code: "SNACK", label: "스낵/과자" },
  { code: "COOKIE", label: "쿠키/비스킷/크래커" },
  { code: "SAND", label: "샌드/스틱과자" },
  { code: "PIE", label: "파이/케익" },
  { code: "BABY", label: "유아과자/시리얼" },
  { code: "TRADITIONAL", label: "떡/한과/전통과자" },
  { code: "ANJU", label: "소시지/원물/안주스낵" },
  { code: "CANDY", label: "사탕/카라멜/껌" },
  { code: "JELLY", label: "젤리/푸딩" },
  { code: "CHOCOLATE", label: "초콜릿/초코바" },
  { code: "CEREAL", label: "시리얼" },
  { code: "ICE_CREAM", label: "아이스크림/빙수" },
];

export interface SnackCatalogCard {
  id: number;
  name: string;
  category: SnackSsgCategory | null;
  categoryLabel: string | null;
  imageUrl: string | null;
  pendingQty: number;
  myPendingQty: number;
  myRequestId: number | null;
  available: boolean;
  hiddenFromUsers: boolean;
  canRequest: boolean;
}

export interface SnackRequest {
  id: number;
  catalogItemId: number;
  name: string;
  imageUrl: string | null;
  quantity: number;
  status: RequestStatus;
  rejectReason: RequestRejectReason | null;
  rejectReasonLabel: string | null;
  rejectNote: string | null;
  createdAt: string;
  purchasedAt: string | null;
  rejectedAt: string | null;
}

export interface SupplyRequest {
  id: number;
  title: string;
  quantity: number;
  memo: string | null;
  status: RequestStatus;
  rejectReason: RequestRejectReason | null;
  rejectReasonLabel: string | null;
  rejectNote: string | null;
  createdAt: string;
  purchasedAt: string | null;
  rejectedAt: string | null;
}

export interface AdminSupplyRequest extends SupplyRequest {
  userId: number;
  nickname: string;
}

export interface SnackGroupedPendingLine {
  id: number;
  userId: number;
  nickname: string;
  quantity: number;
  createdAt: string;
}

export interface SnackGroupedPending {
  catalogItemId: number;
  name: string;
  imageUrl: string | null;
  ssgUrl: string;
  available: boolean;
  hiddenFromUsers: boolean;
  requesterCount: number;
  totalQty: number;
  lines: SnackGroupedPendingLine[];
}

export interface AdminSnackHistoryItem {
  id: number;
  catalogItemId: number;
  name: string;
  imageUrl: string | null;
  quantity: number;
  status: RequestStatus;
  rejectReasonLabel: string | null;
  rejectNote: string | null;
  createdAt: string;
  purchasedAt: string | null;
  rejectedAt: string | null;
  userId: number | null;
  nickname: string | null;
}

export interface AdminSnackCatalogItem {
  id: number;
  ssgItemId: string;
  category: SnackSsgCategory | null;
  categoryLabel: string | null;
  name: string;
  imageUrl: string | null;
  ssgUrl: string;
  displayOrder: number;
  available: boolean;
  hiddenFromUsers: boolean;
  hiddenLockedByAdmin: boolean;
  lastSeenAt: string | null;
  pendingQty: number;
}

export interface CatalogSyncStatusInfo {
  running: boolean;
  lastSuccessAt: string | null;
  lastFailAt: string | null;
  lastError: string | null;
  cooldownUntil: string | null;
  itemCount: number | null;
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
  birthMonth?: number | null;
  birthDay?: number | null;
}

export interface UpcomingBirthday {
  userId: number;
  name: string;
  nickname: string;
  birthMonth: number;
  birthDay: number;
  daysUntil: number;
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
export interface GachaProbabilityFactor {
  label: string;
  weight: number;
  bonus: number;
}

export interface GachaProbabilityBreakdown {
  base: number;
  pityBonus: number;
  pityStacks: number;
  activityBonus: number;
  factors: GachaProbabilityFactor[];
  total: number;
}

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
  probabilityBreakdown?: GachaProbabilityBreakdown;
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
  breakdown?: GachaProbabilityBreakdown;
}

export interface SecretDrawResult {
  drawId: number;
  userId: number;
  winnerName: string;
  winnerNickname: string;
  prizeId: number;
  prizeName: string;
  prizeImageUrl: string | null;
  prizeCashValue: number;
  remainingStock: number;
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
  todayCaps?: Record<string, { used: number; cap: number }>;
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
  secretEvent: boolean;
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

export interface AdminDrawRecord {
  id: number;
  userNickname: string;
  prizeName: string;
  prizeCashValue: number;
  dropsSpent: number;
  winProbability: number;
  rngValue: number;
  isWinner: boolean;
  deliveryStatus: string;
  createdAt: string;
}

export interface AdminDrawHistoryPage {
  items: AdminDrawRecord[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  totalDrawsInFilter: number;
  totalWinsInFilter: number;
  actualWinRate: number;
}
