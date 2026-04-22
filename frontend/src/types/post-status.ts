export type PostStatus = "REGISTERED" | "ING" | "COMPLETE";

export const PostStatusLabels: Record<PostStatus, string> = {
  REGISTERED: "등록",
  ING: "진행중",
  COMPLETE: "완료",
};

export const PostStatusColors: Record<PostStatus, string> = {
  REGISTERED: "bg-blue-100 text-blue-700",
  ING: "bg-yellow-100 text-yellow-700",
  COMPLETE: "bg-green-100 text-green-700",
};
