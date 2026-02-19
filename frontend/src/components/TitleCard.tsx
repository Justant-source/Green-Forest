import { getCategoryColor } from "@/lib/categoryCache";

interface TitleCardProps {
  title: string;
  postId: number;
  category: string;
  likeCount: number;
  viewCount: number;
}

const colorMap: Record<string, { bg: string; text: string }> = {
  yellow: { bg: "#FFF3B0", text: "#7A6C00" },
  blue: { bg: "#C1E1FF", text: "#1B4965" },
  orange: { bg: "#FFE4B5", text: "#8B6914" },
  purple: { bg: "#E2C9FF", text: "#5B2C8B" },
  green: { bg: "#B5EAD7", text: "#2D6A4F" },
  red: { bg: "#FFD6D6", text: "#8B3A3A" },
  pink: { bg: "#FCCDE2", text: "#8B2252" },
  indigo: { bg: "#B0C4DE", text: "#2F4F4F" },
  teal: { bg: "#B2DFDB", text: "#00695C" },
  gray: { bg: "#E0E0E0", text: "#424242" },
};

const fallback = { bg: "#F3F4F6", text: "#374151" };

export default function TitleCard({ title, category, likeCount, viewCount }: TitleCardProps) {
  const catColor = getCategoryColor(category);
  const colors = (catColor && colorMap[catColor]) || fallback;

  return (
    <div
      className="aspect-[4/5] flex flex-col items-center justify-center p-4 relative"
      style={{ backgroundColor: colors.bg }}
    >
      <p
        className="text-center font-semibold text-sm sm:text-lg line-clamp-3 leading-relaxed"
        style={{ color: colors.text }}
      >
        {title}
      </p>
      <div
        className="absolute bottom-2 right-2 flex gap-2 text-[10px] sm:text-xs opacity-70"
        style={{ color: colors.text }}
      >
        <span>♥ {likeCount}</span>
        <span>👁 {viewCount}</span>
      </div>
    </div>
  );
}
