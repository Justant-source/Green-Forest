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
  email: string;
  nickname: string;
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
