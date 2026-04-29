"use client";

import { useState, useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { createPost, getCategories, getQuests, searchUsers } from "@/lib/api";
import { CategoryInfo, Quest } from "@/types";
import { useAuth } from "@/context/AuthContext";
import { compressImage } from "@/lib/imageCompression";
import SurveyCreateForm from "@/components/SurveyCreateForm";

function NewPostPageInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { isLoggedIn, authLoaded, nickname, isAdmin } = useAuth();
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
  const [categorySelected, setCategorySelected] = useState(false);

  useEffect(() => {
    if (!authLoaded) return;
    if (!isLoggedIn) {
      router.replace("/login");
    }
  }, [authLoaded, isLoggedIn, router]);

  useEffect(() => {
    const saved = sessionStorage.getItem("selectedCategory");
    const paramCategory = searchParams.get("category");

    getCategories()
      .then((cats) => {
        if (cats.length > 0) {
          setCategories(cats);
          let initialCategory = "";
          if (paramCategory && cats.some((c) => c.label === paramCategory)) {
            initialCategory = cats.find((c) => c.label === paramCategory)?.name || "";
          } else if (saved && cats.some((c) => c.name === saved)) {
            initialCategory = saved;
          }
          if (initialCategory) {
            setCategory(initialCategory);
            setCategorySelected(true);
          }
        }
      })
      .catch(console.error);

    getQuests()
      .then(setQuests)
      .catch(console.error);
  }, [searchParams]);

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
      alert(error instanceof Error ? error.message : "게시글 작성에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  if (!isLoggedIn) return null;

  const activeQuests = quests.filter((q) => q.active);

  const getRewardHint = (catName: string): string => {
    if (catName === "긍정문구") return "+10 물방울 / 일 1회";
    if (catName === "동료칭찬") return "+30 물방울, 본인 + 태그 동료 / 주 1회";
    if (catName === "퀘스트") return "가변 보상";
    return "";
  };

  const getDescription = (catName: string): string => {
    if (catName === "긍정문구") return "팀원에게 전하는 긍정의 한마디";
    if (catName === "동료칭찬") return "동료의 좋은 점을 공개적으로 칭찬";
    if (catName === "퀘스트") return "퀘스트 완료 인증";
    return "";
  };

  const categoryColor = (color: string | undefined): string => {
    if (!color || color === "white") return "bg-gray-100 border-gray-300";
    const colorMap: Record<string, string> = {
      green: "bg-green-50 border-green-300",
      blue: "bg-blue-50 border-blue-300",
      yellow: "bg-yellow-50 border-yellow-300",
      red: "bg-red-50 border-red-300",
      purple: "bg-purple-50 border-purple-300",
      pink: "bg-pink-50 border-pink-300",
      indigo: "bg-indigo-50 border-indigo-300",
      orange: "bg-orange-50 border-orange-300",
    };
    return colorMap[color] || "bg-gray-50 border-gray-300";
  };

  const categoryBgColor = (color: string | undefined): string => {
    if (!color || color === "white") return "bg-gray-200";
    const colorMap: Record<string, string> = {
      green: "bg-green-500",
      blue: "bg-blue-500",
      yellow: "bg-yellow-500",
      red: "bg-red-500",
      purple: "bg-purple-500",
      pink: "bg-pink-500",
      indigo: "bg-indigo-500",
      orange: "bg-orange-500",
    };
    return colorMap[color] || "bg-gray-500";
  };

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">새 글</h1>

      {!categorySelected ? (
        <div className="space-y-6">
          <div>
            <h2 className="text-lg font-semibold text-gray-800 mb-4">어떤 글을 작성하시겠어요?</h2>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              {categories
                .filter((cat) => cat.name !== "이벤트")
                .filter((cat) => !cat.adminOnly || isAdmin)
                .map((cat) => {
                const disabled = cat.name === "퀘스트";
                return (
                  <button
                    key={cat.name}
                    disabled={disabled}
                    onClick={() => {
                      setCategory(cat.name);
                      setCategorySelected(true);
                    }}
                    className={`p-6 rounded-lg border-2 transition-all text-left ${
                      disabled
                        ? "bg-gray-100 border-gray-200 opacity-50 cursor-not-allowed"
                        : `${categoryColor(cat.color)} hover:shadow-md active:scale-95`
                    }`}
                  >
                    <div className="flex items-center gap-3 mb-2">
                      <div className={`w-3 h-3 rounded-full ${disabled ? "bg-gray-300" : categoryBgColor(cat.color)}`} />
                      <h3 className="text-base font-bold text-gray-900">{cat.label}</h3>
                      {disabled && <span className="text-[10px] text-gray-400 font-medium">준비중</span>}
                    </div>
                    <p className="text-xs text-gray-600 mb-3">{getDescription(cat.name)}</p>
                    <p className="text-xs font-medium text-gray-700">{getRewardHint(cat.name)}</p>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      ) : category === "survey" ? (
        <SurveyCreateForm onCancel={() => { setCategorySelected(false); setCategory(""); }} />
      ) : (
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="flex items-center justify-between mb-4">
            <span className="text-sm font-medium text-gray-700">
              선택됨: <span className="text-forest-600 font-semibold">
                {categories.find((c) => c.name === category)?.label}
              </span>
            </span>
            <button
              type="button"
              onClick={() => {
                setCategorySelected(false);
                setCategory("");
              }}
              className="text-xs text-forest-600 hover:text-forest-700 underline"
            >
              카테고리 변경
            </button>
          </div>
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
      )}
    </div>
  );
}

export default function NewPostPage() {
  return (
    <Suspense fallback={<div className="max-w-2xl mx-auto p-6">로딩 중...</div>}>
      <NewPostPageInner />
    </Suspense>
  );
}
