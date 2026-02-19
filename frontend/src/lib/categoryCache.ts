import { CategoryInfo } from "@/types";

let cache: CategoryInfo[] = [];

export function setCategoryCache(cats: CategoryInfo[]) {
  if (cats.length > 0) cache = cats;
}

export function getCategoryColor(categoryName: string): string | null {
  const cat = cache.find((c) => c.name === categoryName);
  return cat?.color ?? null;
}
