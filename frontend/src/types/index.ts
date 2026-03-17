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
  role: string;
  plantType: string | null;
  plantName: string | null;
  jobClass: string | null;
  partyId: number | null;
  partyName: string | null;
  totalDrops: number;
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
