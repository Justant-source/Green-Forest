"use client";

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { getPost, getCategories, updatePost, IMAGE_BASE_URL } from "@/lib/api";
import { CategoryInfo } from "@/types";
import { useAuth } from "@/context/AuthContext";
import CategoryRequestModal from "@/components/CategoryRequestModal";
import { compressImage } from "@/lib/imageCompression";

export default function EditPostPage() {
  const router = useRouter();
  const params = useParams();
  const postId = Number(params.id);
  const { isLoggedIn, nickname } = useAuth();

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

  useEffect(() => {
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
        if (post.authorNickname !== nickname) {
          alert("본인이 작성한 글만 수정할 수 있습니다.");
          router.replace(`/posts/${postId}`);
          return;
        }
        setTitle(post.title);
        setContent(post.content);
        setCategory(post.category);
        if (post.imageUrls && post.imageUrls.length > 0) {
          setExistingImageUrls(post.imageUrls);
          setImagePreviews(post.imageUrls.map((url) => `${IMAGE_BASE_URL}${url}`));
        } else if (post.imageUrl) {
          setExistingImageUrls([post.imageUrl]);
          setImagePreviews([`${IMAGE_BASE_URL}${post.imageUrl}`]);
        }
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [postId, isLoggedIn, nickname, router]);

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

    const existingPreviews = existingImageUrls.map((url) => `${IMAGE_BASE_URL}${url}`);
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

    setSubmitting(true);
    try {
      const formData = new FormData();
      formData.append("title", title.trim());
      formData.append("content", content.trim());
      formData.append("category", category);
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
        <div className="w-10 h-10 border-4 border-gray-300 border-t-orange-600 rounded-full animate-spin" />
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
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500"
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
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-orange-500"
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

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            내용
          </label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="내용을 입력하세요"
            rows={8}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-orange-500"
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
            className="w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-orange-50 file:text-orange-700 hover:file:bg-orange-100"
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
                    <span className="absolute bottom-1 left-1 px-1.5 py-0.5 bg-orange-600 text-white text-[10px] rounded">
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
            className="px-6 py-2 bg-orange-600 text-white rounded-lg text-sm font-medium hover:bg-orange-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
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
