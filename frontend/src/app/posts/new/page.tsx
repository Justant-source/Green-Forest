"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { createPost, getCategories, getQuests, searchUsers } from "@/lib/api";
import { CategoryInfo, Quest } from "@/types";
import { useAuth } from "@/context/AuthContext";
import { compressImage } from "@/lib/imageCompression";

export default function NewPostPage() {
  const router = useRouter();
  const { isLoggedIn, authLoaded, nickname } = useAuth();
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("");
  const [content, setContent] = useState("");
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [categories, setCategories] = useState<CategoryInfo[]>([]);
  const [taggedList, setTaggedList] = useState<{ name: string; nickname: string }[]>([]);
  const [tagInput, setTagInput] = useState("");
  const [tagSuggestions, setTagSuggestions] = useState<{ id: number; name: string; nickname: string }[]>([]);
  const [tagError, setTagError] = useState("");
  const [anonymous, setAnonymous] = useState(false);
  const [questId, setQuestId] = useState<number | null>(null);
  const [quests, setQuests] = useState<Quest[]>([]);

  useEffect(() => {
    if (!authLoaded) return;
    if (!isLoggedIn) {
      router.replace("/login");
    }
  }, [authLoaded, isLoggedIn, router]);

  useEffect(() => {
    const saved = sessionStorage.getItem("selectedCategory");

    getCategories()
      .then((cats) => {
        if (cats.length > 0) {
          setCategories(cats);
          const valid = saved && cats.some((c) => c.name === saved);
          setCategory(valid ? saved! : cats[0].name);
        }
      })
      .catch(console.error);

    getQuests()
      .then(setQuests)
      .catch(console.error);
  }, []);

  useEffect(() => {
    if (tagInput.trim().length === 0) {
      setTagSuggestions([]);
      return;
    }
    const timer = setTimeout(() => {
      searchUsers(tagInput.trim()).then((results) => {
        setTagSuggestions(results.filter((u) => !taggedList.some(t => t.nickname === u.nickname) && u.nickname !== nickname));
      });
    }, 300);
    return () => clearTimeout(timer);
  }, [tagInput, taggedList]);

  const addTag = (user: { name: string; nickname: string }) => {
    if (user.nickname === nickname) {
      setTagError("자기 자신은 태그할 수 없습니다.");
      return;
    }
    if (!taggedList.some(t => t.nickname === user.nickname)) {
      setTaggedList([...taggedList, user]);
    }
    setTagInput("");
    setTagSuggestions([]);
    setTagError("");
  };

  const removeTag = (tagNickname: string) => {
    setTaggedList(taggedList.filter((t) => t.nickname !== tagNickname));
  };

  const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    if (files.length + imageFiles.length > 5) {
      alert("이미지는 최대 5장까지 업로드할 수 있습니다.");
      e.target.value = "";
      return;
    }

    const compressed = await Promise.all(files.map((f) => compressImage(f)));
    const newFiles = [...imageFiles, ...compressed];
    setImageFiles(newFiles);

    compressed.forEach((file) => {
      const reader = new FileReader();
      reader.onload = (event) => {
        setImagePreviews((prev) => [...prev, event.target?.result as string]);
      };
      reader.readAsDataURL(file);
    });
    e.target.value = "";
  };

  const removeImage = (index: number) => {
    setImageFiles((prev) => prev.filter((_, i) => i !== index));
    setImagePreviews((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;

    setSubmitting(true);
    try {
      const formData = new FormData();
      formData.append("title", title.trim());
      formData.append("content", content.trim());
      formData.append("category", category);
      formData.append("anonymous", String(anonymous));
      if (taggedList.length > 0) {
        formData.append("taggedNicknames", taggedList.map(t => t.nickname).join(","));
      }
      if (category === "퀘스트" && questId) {
        formData.append("questId", String(questId));
      }
      imageFiles.forEach((file) => {
        formData.append("images", file);
      });

      const newPost = await createPost(formData);
      sessionStorage.removeItem("gridFeedCache");
      router.push(`/posts/${newPost.id}`);
    } catch (error) {
      console.error("Failed to create post:", error);
      alert("게시글 작성에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  if (!isLoggedIn) return null;

  const activeQuests = quests.filter((q) => q.active);

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">새 글</h1>

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            제목
          </label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="제목을 입력하세요"
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-forest-500"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            게시판
          </label>
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-forest-500"
          >
            {categories.map((cat) => (
              <option key={cat.name} value={cat.name}>
                {cat.label}
              </option>
            ))}
          </select>
        </div>

        {category === "퀘스트" && activeQuests.length > 0 && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              퀘스트 선택
            </label>
            <select
              value={questId ?? ""}
              onChange={(e) => setQuestId(e.target.value ? Number(e.target.value) : null)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-forest-500"
            >
              <option value="">퀘스트를 선택하세요</option>
              {activeQuests.map((q) => (
                <option key={q.id} value={q.id}>
                  {q.title} (+{q.rewardDrops} 물방울)
                </option>
              ))}
            </select>
          </div>
        )}

        {category === "동료칭찬" && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              태그할 동료
            </label>
            {taggedList.length > 0 && (
              <div className="flex flex-wrap gap-2 mb-2">
                {taggedList.map((tag) => (
                  <span
                    key={tag.nickname}
                    className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-sm font-medium bg-blue-50 text-blue-600"
                  >
                    @{tag.name}({tag.nickname})
                    <button
                      type="button"
                      onClick={() => removeTag(tag.nickname)}
                      className="ml-1 text-blue-400 hover:text-blue-700 text-xs"
                    >
                      X
                    </button>
                  </span>
                ))}
              </div>
            )}
            <div className="relative">
              <input
                type="text"
                value={tagInput}
                onChange={(e) => {
                  setTagInput(e.target.value);
                  setTagError("");
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    if (tagInput.trim() && tagSuggestions.length > 0) {
                      addTag({ name: tagSuggestions[0].name, nickname: tagSuggestions[0].nickname });
                    } else if (tagInput.trim()) {
                      setTagError("올바른 이름을 입력해주세요.");
                    }
                  }
                }}
                placeholder="이름을 검색하세요"
                className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-forest-500 ${
                  tagError ? "border-red-400" : "border-gray-300"
                }`}
              />
              {tagSuggestions.length > 0 && (
                <ul className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-40 overflow-y-auto">
                  {tagSuggestions.map((user) => (
                    <li key={user.id}>
                      <button
                        type="button"
                        onClick={() => addTag({ name: user.name, nickname: user.nickname })}
                        className="w-full text-left px-4 py-2 hover:bg-forest-50 text-sm"
                      >
                        {user.name}({user.nickname})
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
            {tagError && (
              <p className="text-xs text-red-500 mt-1">{tagError}</p>
            )}
            <p className="text-xs text-gray-400 mt-1">태그된 동료에게 물방울 보너스가 지급됩니다.</p>
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            내용
          </label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="내용을 입력하세요"
            rows={8}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-forest-500"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            이미지 (선택, 최대 5장)
          </label>
          <input
            type="file"
            accept=".png,.jpg,.jpeg"
            multiple
            onChange={handleImageChange}
            className="w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-forest-50 file:text-forest-600 hover:file:bg-forest-100"
          />
          {imagePreviews.length > 0 && (
            <div className="mt-3 flex gap-3 overflow-x-auto pb-2">
              {imagePreviews.map((preview, index) => (
                <div key={index} className="relative flex-shrink-0 w-32 h-32 rounded-xl overflow-hidden">
                  <img
                    src={preview}
                    alt={`미리보기 ${index + 1}`}
                    className="w-full h-full object-cover"
                  />
                  <button
                    type="button"
                    onClick={() => removeImage(index)}
                    className="absolute top-1 right-1 w-6 h-6 bg-black/60 text-white rounded-full flex items-center justify-center text-xs hover:bg-black/80"
                  >
                    X
                  </button>
                  {index === 0 && (
                    <span className="absolute bottom-1 left-1 px-1.5 py-0.5 bg-forest-600 text-white text-[10px] rounded">
                      썸네일
                    </span>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="flex items-center gap-4">
          <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
            <input
              type="checkbox"
              checked={anonymous}
              onChange={(e) => setAnonymous(e.target.checked)}
              className="w-4 h-4 accent-forest-500"
            />
            익명으로 작성
          </label>
        </div>

        <div className="flex justify-end gap-3 pt-4">
          <button
            type="button"
            onClick={() => router.back()}
            className="px-6 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
          >
            취소
          </button>
          <button
            type="submit"
            disabled={submitting || !title.trim() || !content.trim()}
            className="px-6 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {submitting ? "등록 중..." : "등록하기"}
          </button>
        </div>
      </form>
    </div>
  );
}
