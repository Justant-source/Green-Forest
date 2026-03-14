"use client";

import { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { CategoryInfo } from "@/types";
import { getCategories } from "@/lib/api";

interface CategoryContextType {
  categories: CategoryInfo[];
  getCategoryColor: (categoryName: string) => string | null;
}

const CategoryContext = createContext<CategoryContextType>({
  categories: [],
  getCategoryColor: () => null,
});

export function CategoryProvider({ children }: { children: ReactNode }) {
  const [categories, setCategories] = useState<CategoryInfo[]>([]);

  useEffect(() => {
    getCategories()
      .then((cats) => {
        if (cats.length > 0) setCategories(cats);
      })
      .catch(console.error);
  }, []);

  const getCategoryColor = (categoryName: string): string | null => {
    const cat = categories.find((c) => c.name === categoryName);
    return cat?.color ?? null;
  };

  return (
    <CategoryContext.Provider value={{ categories, getCategoryColor }}>
      {children}
    </CategoryContext.Provider>
  );
}

export function useCategories() {
  return useContext(CategoryContext);
}
