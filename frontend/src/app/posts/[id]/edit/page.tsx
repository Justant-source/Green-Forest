"use client";

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { getPost, getCategories, updatePost, toMediaUrl, searchUsers } from "@/lib/api";
import { CategoryInfo } from "@/types";
import { useAuth } from "@/context/AuthContext";
import CategoryRequestModal from "@/components/CategoryRequestModal";
import { compressImage } from "@/lib/imageCompression";
import { parsePhotoBingoMarker } from "@/lib/events/postMarker";

export default function EditPostPage() {
  const router = useRouter();
  const params = useParams();
  const postId = Number(params.id);
  const { isLoggedIn, nickname, authLoaded } = useAuth();

  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("");
  const [content, setContent] = useState("");
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);
  const [existingImageUrls, setExistingImageUrls] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [showCategoryModal, setShowCategoryModal] = useState(false);
  const [loading, setLoading] = useState(true);
  const [categories, setCategories] = useState<CategoryInfo[]>([]);
  const [taggedList, setTaggedList] = useState<{ name: string; nickname: string }[]>([]);
  const [tagInput, setTagInput] = useState("");
  const [tagSuggestions, setTagSuggestions] = useState<{ id: number; name: string; nickname: string }[]>([]);
  const [tagError, setTagError] = useState("");

  useEffect(() => {
    if (!authLoaded) return;
    if (!isLoggedIn) {
      router.replace("/login");
      return;
    }

    getCategories()
      .then((cats) => {
        if (cats.length > 0) setCategories(cats);
      })
      .catch(console.error);

    getPost(postId)
      .then((post) => {
        if (!post.isAuthor) {
          alert("본인이 작성한 글만 수정할 수 있습니다.");
          router.replace(`/posts/${postId}`);
          return;
        }
        // 빙고 이벤트로 자동 생성된 글은 일반 글 수정 UI로 들어가면 원본 마커가 노출된다.
        // 빙고 참여 화면으로 이동시킨다.
        const bingo = parsePhotoBingoMarker(post.content).bingo;
        if (bingo) {
          router.replace(`/events/${bingo.eventId}`);
          return;
        }
        setTitle(post.title);
        setContent(post.content);
        setCategory(post.category);
        if (post.taggedNicknames && post.taggedNicknames.length > 0) {
          setTaggedList(
            post.taggedNicknames
              .map((entry) => {
                const match = entry.match(/^(.+)\((.+)\)$/);
                return match ? { name: match[1], nickname: match[2] } : null;
              })
              .filter((t): t is { name: string; nickname: string } => t !== null)
          );
        }
        if (post.imageUrls && post.imageUrls.length > 0) {
          setExistingImageUrls(post.imageUrls);
          setImagePreviews(post.imageUrls.map((url) => toMediaUrl(url)));
        } else if (post.imageUrl) {
          setExistingImageUrls([post.imageUrl]);
          setImagePreviews([toMediaUrl(post.imageUrl)]);
        }
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [authLoaded, postId, isLoggedIn, nickname, router]);

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
  }, [tagInput, taggedList, nickname]);

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
    const totalCount = existingImageUrls.length + imageFiles.length + files.length;
    if (totalCount > 5) {
      alert("이미지는 최대 5장까지 업로드할 수 있습니다.");
      e.target.value = "";
      return;
    }

    const compressed = await Promise.all(files.map((f) => compressImage(f)));
    const newFiles = [...imageFiles, ...compressed];
    setImageFiles(newFiles);

    const existingPreviews = existingImageUrls.map((url) => toMediaUrl(url));
    const filePreviews: string[] = [];
    await Promise.all(
      newFiles.map(
        (file) =>
          new Promise<void>((resolve) => {
            const reader = new FileReader();
            reader.onload = (event) => {
              filePreviews.push(event.target?.result as string);
              resolve();
            };
            reader.readAsDataURL(file);
          })
      )
    );
    setImagePreviews([...existingPreviews, ...filePreviews]);
    e.target.value = "";
  };

  const removeImage = (index: number) => {
    const existingCount = existingImageUrls.length;
    if (index < existingCount) {
      setExistingImageUrls((prev) => prev.filter((_, i) => i !== index));
    } else {
      const fileIndex = index - existingCount;
      setImageFiles((prev) => prev.filter((_, i) => i !== fileIndex));
    }
    setImagePreviews((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;
    if (category === "동료칭찬" && taggedList.length === 0) {
      alert("칭찬할 동료를 태그해주세요.");
      return;
    }

    setSubmitting(true);
    try {
      const formData = new FormData();
      formData.append("title", title.trim());
      formData.append("content", content.trim());
      formData.append("category", category);
      if (taggedList.length > 0) {
        formData.append("taggedNicknames", taggedList.map(t => t.nickname).join(","));
      }
      existingImageUrls.forEach((url) => {
        formData.append("existingImageUrls", url);
      });
      imageFiles.forEach((file) => {
        formData.append("images", file);
      });

      await updatePost(postId, formData);
      router.push(`/posts/${postId}`);
    } catch (error) {
      console.error("Failed to update post:", error);
      alert("게시글 수정에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  if (!isLoggedIn) return null;

  if (loading) {
    return (
      <div className="flex justify-center py-20">
        <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">글 수정</h1>

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
          <div className="flex gap-2">
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-forest-500"
            >
              {categories.map((cat) => (
                <option key={cat.name} value={cat.name}>
                  {cat.label}
                </option>
              ))}
            </select>
            <button
              type="button"
              onClick={() => setShowCategoryModal(true)}
              className="px-3 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-100 transition-colors whitespace-nowrap"
            >
              + 요청
            </button>
          </div>
        </div>

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
                    <span className="absolute bottom-1 left-1 px-1.5 py-0.5 bg-forest-500 text-white text-[10px] rounded">
                      썸네일
                    </span>
                  )}
                </div>
              ))}
            </div>
          )}
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
            {submitting ? "수정 중..." : "수정하기"}
          </button>
        </div>
      </form>

      {showCategoryModal && (
        <CategoryRequestModal onClose={() => setShowCategoryModal(false)} />
      )}
    </div>
  );
}
