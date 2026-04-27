"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { logout } from "@/lib/auth";
import { getMe, getMyPosts, getMyBookmarks, getMyDropHistory, updateMyProfile, changePassword, getMyPlantGrowth, getMyAttendanceWins, getGachaHistory } from "@/lib/api";
import { User, Post, DropTransaction, PageResponse, PlantGrowth, MyAttendanceWin, GachaDrawRecord } from "@/types";
import GridItem from "@/components/GridItem";
import PlantGrowthBadge from "@/components/PlantGrowthBadge";
import PlantLevelGuide from "@/components/PlantLevelGuide";

type RewardItem = {
  key: string;
  type: "ATTENDANCE" | "GACHA";
  date: string;
  title: string;
  detail?: string;
  deliveryStatus: "PENDING" | "DELIVERED";
  deliveryMemo?: string | null;
  deliveredAt?: string | null;
};

const PLANT_TYPES = [
  { value: "TABLE_PALM", label: "테이블야자", job: "탱커(Guardian)", element: "땅", difficulty: "쉬움" },
  { value: "SPATHIPHYLLUM", label: "스파티필럼", job: "힐러(Healer)", element: "물", difficulty: "쉬움" },
  { value: "HONG_KONG_PALM", label: "홍콩야자", job: "버퍼(Enchanter)", element: "바람", difficulty: "보통" },
  { value: "ORANGE_JASMINE", label: "오렌지자스민", job: "딜러(Striker)", element: "불", difficulty: "어려움" },
];

type Tab = "posts" | "bookmarks" | "drops" | "rewards";

export default function GardenPage() {
  const { isLoggedIn, handleLogout, authLoaded } = useAuth();
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [growth, setGrowth] = useState<PlantGrowth | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<Tab>("posts");
  const [posts, setPosts] = useState<Post[]>([]);
  const [drops, setDrops] = useState<DropTransaction[]>([]);
  const [rewards, setRewards] = useState<RewardItem[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [tabLoading, setTabLoading] = useState(false);

  // 프로필 편집
  const [editing, setEditing] = useState(false);
  const [editPlantName, setEditPlantName] = useState("");
  const [editPlantType, setEditPlantType] = useState("");
  const [saving, setSaving] = useState(false);

  // 레벨 가이드 팝업
  const [showLevelGuide, setShowLevelGuide] = useState(false);

  // 비밀번호 변경
  const [showPwChange, setShowPwChange] = useState(false);
  const [currentPw, setCurrentPw] = useState("");
  const [newPw, setNewPw] = useState("");
  const [newPwConfirm, setNewPwConfirm] = useState("");
  const [pwSaving, setPwSaving] = useState(false);

  useEffect(() => {
    if (!authLoaded) return;
    if (!isLoggedIn) {
      router.replace("/login");
      return;
    }
    Promise.all([
      getMe(),
      getMyPlantGrowth()
    ])
      .then(([u, g]) => {
        setUser(u);
        setGrowth(g);
        setEditPlantName(u.plantName || "");
        setEditPlantType(u.plantType || "");
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [authLoaded, isLoggedIn, router]);

  const fetchTabData = useCallback(async (tab: Tab, pageNum: number, append: boolean) => {
    setTabLoading(true);
    try {
      if (tab === "drops") {
        const data = await getMyDropHistory(pageNum, 20);
        setDrops((prev) => append ? [...prev, ...data.content] : data.content);
        setHasMore(!data.last);
      } else if (tab === "rewards") {
        // 출석 + 가챠 당첨건을 한 번에 조회 (페이지네이션 없이 일괄)
        const [attendanceWins, gachaPage] = await Promise.all([
          getMyAttendanceWins().catch(() => [] as MyAttendanceWin[]),
          getGachaHistory(0, 200).catch(() => ({ content: [] as GachaDrawRecord[], last: true } as PageResponse<GachaDrawRecord>)),
        ]);
        const aItems: RewardItem[] = attendanceWins.map((w) => ({
          key: `attendance-${w.id}`,
          type: "ATTENDANCE",
          date: w.date,
          title: "출석 이벤트 당첨",
          detail: w.message ?? undefined,
          deliveryStatus: w.deliveryStatus === "DELIVERED" ? "DELIVERED" : "PENDING",
          deliveryMemo: w.deliveryMemo,
          deliveredAt: w.deliveredAt,
        }));
        const gItems: RewardItem[] = (gachaPage.content ?? []).filter((d) => d.isWinner).map((d) => ({
          key: `gacha-${d.id}`,
          type: "GACHA",
          date: d.createdAt,
          title: `뽑기 당첨: ${d.prizeName}`,
          detail: `${d.prizeCashValue.toLocaleString()}원 상당`,
          deliveryStatus: d.deliveryStatus === "DELIVERED" ? "DELIVERED" : "PENDING",
        }));
        const merged = [...aItems, ...gItems].sort((a, b) => (a.date < b.date ? 1 : -1));
        setRewards(merged);
        setHasMore(false);
      } else {
        const fetcher = tab === "posts" ? getMyPosts : getMyBookmarks;
        const data: PageResponse<Post> = await fetcher(pageNum, 12);
        setPosts((prev) => append ? [...prev, ...data.content] : data.content);
        setHasMore(!data.last);
      }
    } catch (error) {
      console.error("Failed to fetch:", error);
    } finally {
      setTabLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isLoggedIn) {
      setPage(0);
      setPosts([]);
      setDrops([]);
      setRewards([]);
      fetchTabData(activeTab, 0, false);
    }
  }, [activeTab, isLoggedIn, fetchTabData]);

  const loadMore = () => {
    const nextPage = page + 1;
    setPage(nextPage);
    fetchTabData(activeTab, nextPage, true);
  };

  const handleSaveProfile = async () => {
    if (!editPlantType) return;
    setSaving(true);
    try {
      const updated = await updateMyProfile({
        plantName: editPlantName,
        plantType: editPlantType,
      });
      setUser(updated);
      setEditing(false);
    } catch (error) {
      console.error("Failed to update profile:", error);
      alert("프로필 업데이트에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handleBookmarkChange = (postId: number, bookmarked: boolean) => {
    if (activeTab === "bookmarks" && !bookmarked) {
      setPosts((prev) => prev.filter((p) => p.id !== postId));
    }
  };

  if (!isLoggedIn || loading) {
    return (
      <div className="flex justify-center py-20">
        <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
      </div>
    );
  }

  if (!user) return null;

  const plantInfo = PLANT_TYPES.find((p) => p.value === user.plantType);

  return (
    <div className="max-w-3xl mx-auto">
      {/* 프로필 카드 */}
      <div className="bg-white rounded-2xl border border-gray-200 p-6 mb-6">
        <div className="flex items-start justify-between mb-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{user.nickname}</h1>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setShowPwChange((v) => !v)}
              className="text-sm text-gray-400 hover:text-gray-600 px-3 py-1.5 rounded-lg hover:bg-gray-100 transition-colors"
            >
              비밀번호 변경
            </button>
            <button
              onClick={() => {
                handleLogout();
                router.push("/");
              }}
              className="text-sm text-gray-400 hover:text-gray-600 px-3 py-1.5 rounded-lg hover:bg-gray-100 transition-colors"
            >
              로그아웃
            </button>
          </div>
        </div>

        {/* 비밀번호 변경 */}
        {showPwChange && (
          <div className="bg-gray-50 rounded-xl p-4 mb-4 space-y-3">
            <h3 className="text-sm font-semibold text-gray-700">비밀번호 변경</h3>
            <input
              type="password"
              value={currentPw}
              onChange={(e) => setCurrentPw(e.target.value)}
              placeholder="현재 비밀번호"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
            />
            <input
              type="password"
              value={newPw}
              onChange={(e) => setNewPw(e.target.value)}
              placeholder="새 비밀번호"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
            />
            <input
              type="password"
              value={newPwConfirm}
              onChange={(e) => setNewPwConfirm(e.target.value)}
              placeholder="새 비밀번호 확인"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
            />
            <div className="flex gap-2 pt-1">
              <button
                onClick={async () => {
                  if (!currentPw || !newPw || !newPwConfirm) { alert("모든 항목을 입력해주세요."); return; }
                  if (newPw !== newPwConfirm) { alert("새 비밀번호가 일치하지 않습니다."); return; }
                  if (newPw.length < 4) { alert("새 비밀번호는 4자 이상이어야 합니다."); return; }
                  setPwSaving(true);
                  try {
                    await changePassword(currentPw, newPw);
                    alert("비밀번호가 변경되었습니다. 다시 로그인해주세요.");
                    logout();
                    router.push("/login");
                  } catch {
                    alert("현재 비밀번호가 올바르지 않습니다.");
                  } finally {
                    setPwSaving(false);
                  }
                }}
                disabled={pwSaving}
                className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600 disabled:opacity-50 transition-colors"
              >
                {pwSaving ? "변경 중..." : "변경"}
              </button>
              <button
                onClick={() => { setShowPwChange(false); setCurrentPw(""); setNewPw(""); setNewPwConfirm(""); }}
                className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
              >
                취소
              </button>
            </div>
          </div>
        )}

        {/* 식물 정보 */}
        {user.plantType ? (
          <div className="bg-forest-50 rounded-xl p-4 mb-4">
            <div className="flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <button
                    type="button"
                    onClick={() => setShowLevelGuide(true)}
                    aria-label="레벨 가이드 보기"
                    className="rounded-md hover:bg-forest-100 focus:outline-none focus:ring-2 focus:ring-forest-400 transition-colors"
                  >
                    <PlantGrowthBadge growth={growth} showDetails={true} />
                  </button>
                  <span className="text-lg font-semibold text-forest-600">
                    {user.plantName || plantInfo?.label}
                  </span>
                  <span className="text-xs text-gray-400">({plantInfo?.label})</span>
                </div>
                <div className="text-xs text-gray-500 flex flex-wrap items-center gap-x-1.5 gap-y-0.5">
                  <span><span className="text-gray-400">직업</span> {user.jobClassLabel}</span>
                  <span className="text-gray-300">·</span>
                  <span><span className="text-gray-400">속성</span> {user.elementLabel}</span>
                  <span className="text-gray-300">·</span>
                  <span><span className="text-gray-400">난도</span> {user.difficultyLabel}</span>
                  <span className="text-gray-300">·</span>
                  <span><span className="text-gray-400">EXP</span> ×{user.expMultiplier}</span>
                </div>
              </div>
              {!user.plantLocked && (
                <button
                  onClick={() => setEditing(true)}
                  className="text-xs text-forest-500 hover:text-forest-600 px-3 py-1.5 rounded-lg hover:bg-forest-100 transition-colors"
                >
                  편집
                </button>
              )}
            </div>
          </div>
        ) : (
          <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-4 mb-4">
            <p className="text-sm text-yellow-700 mb-2">아직 식물을 선택하지 않았습니다.</p>
            <button
              onClick={() => setEditing(true)}
              className="text-sm text-forest-500 font-medium hover:underline"
            >
              식물 선택하기
            </button>
          </div>
        )}

        {/* 프로필 편집 모달 */}
        {editing && (
          <div className="bg-gray-50 rounded-xl p-4 mb-4 space-y-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">식물 이름</label>
              <input
                type="text"
                value={editPlantName}
                onChange={(e) => setEditPlantName(e.target.value)}
                placeholder="내 식물의 이름을 지어주세요"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">식물 종류</label>
              <div className="grid grid-cols-2 gap-2">
                {PLANT_TYPES.map((pt) => (
                  <button
                    key={pt.value}
                    type="button"
                    onClick={() => setEditPlantType(pt.value)}
                    className={`text-left p-3 rounded-lg border text-sm transition-colors ${
                      editPlantType === pt.value
                        ? "border-forest-500 bg-forest-50"
                        : "border-gray-200 hover:border-gray-300"
                    }`}
                  >
                    <div className="font-medium text-gray-900">{pt.label}</div>
                    <div className="text-xs text-gray-400">{pt.job} | {pt.element} | {pt.difficulty}</div>
                  </button>
                ))}
              </div>
            </div>
            <div className="flex gap-2 pt-2">
              <button
                onClick={handleSaveProfile}
                disabled={saving || !editPlantType}
                className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600 disabled:opacity-50 transition-colors"
              >
                {saving ? "저장 중..." : "저장"}
              </button>
              <button
                onClick={() => setEditing(false)}
                className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
              >
                취소
              </button>
            </div>
          </div>
        )}

        {/* 물방울 & 파티 요약 */}
        <div className="grid grid-cols-3 gap-4">
          <div className="text-center p-3 bg-accent/5 rounded-xl">
            <div className="text-2xl font-bold text-accent">{user.totalDrops.toLocaleString()}</div>
            <div className="text-xs text-gray-400">물방울</div>
          </div>
          <div className="text-center p-3 bg-forest-50 rounded-xl">
            <div className="text-2xl font-bold text-forest-500 truncate">{user.partyName || "-"}</div>
            <div className="text-xs text-gray-400">파티</div>
          </div>
          <div className="text-center p-3 bg-gray-50 rounded-xl">
            <div className="text-2xl font-bold text-gray-700 truncate">{user.jobClassLabel || "-"}</div>
            <div className="text-xs text-gray-400">직업군</div>
          </div>
        </div>
      </div>

      {/* 탭 */}
      <div className="flex gap-1 mb-6 border-b">
        {([
          { key: "posts" as Tab, label: "내 글" },
          { key: "bookmarks" as Tab, label: "북마크" },
          { key: "drops" as Tab, label: "물방울" },
          { key: "rewards" as Tab, label: "보상" },
        ]).map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-3 sm:px-6 py-3 text-sm font-medium whitespace-nowrap transition-colors border-b-2 -mb-px ${
              activeTab === tab.key
                ? "border-forest-500 text-forest-500"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 탭 컨텐츠 */}
      {activeTab === "rewards" ? (
        // 보상 내역 (출석 + 가챠 통합)
        tabLoading && rewards.length === 0 ? (
          <div className="flex justify-center py-20">
            <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
          </div>
        ) : rewards.length === 0 ? (
          <div className="text-center py-20 text-gray-400">받은 보상이 없습니다.</div>
        ) : (
          <div className="space-y-2">
            {rewards.map((r) => {
              const isDelivered = r.deliveryStatus === "DELIVERED";
              const typeBadge = r.type === "ATTENDANCE"
                ? "bg-blue-100 text-blue-700"
                : "bg-purple-100 text-purple-700";
              const statusBadge = isDelivered
                ? "bg-green-100 text-green-700 border-green-300"
                : "bg-orange-100 text-orange-700 border-orange-300";
              const borderColor = isDelivered ? "border-green-400" : "border-orange-400";
              return (
                <div key={r.key} className={`bg-white rounded-xl border-l-4 ${borderColor} px-4 py-3`}>
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${typeBadge}`}>
                          {r.type === "ATTENDANCE" ? "출석" : "뽑기"}
                        </span>
                        <span className="text-sm font-semibold text-gray-800">{r.title}</span>
                        <span className={`text-xs px-2 py-0.5 rounded-full border font-medium ${statusBadge}`}>
                          {isDelivered ? "수령 완료" : "수령 대기"}
                        </span>
                      </div>
                      {r.detail && <div className="text-xs text-gray-500 mt-1">{r.detail}</div>}
                      <div className="text-xs text-gray-400 mt-0.5">
                        당첨일 {new Date(r.date).toLocaleDateString("ko-KR")}
                      </div>
                      {isDelivered && r.deliveredAt && (
                        <div className="text-xs text-green-700 mt-1">
                          수령일: {new Date(r.deliveredAt).toLocaleString("ko-KR")}
                          {r.deliveryMemo && <span className="ml-2 text-gray-500">메모: {r.deliveryMemo}</span>}
                        </div>
                      )}
                      {!isDelivered && (
                        <div className="text-xs text-orange-700 mt-1">관리자가 보상을 준비 중입니다</div>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )
      ) : activeTab === "drops" ? (
        // 물방울 내역
        tabLoading && drops.length === 0 ? (
          <div className="flex justify-center py-20">
            <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
          </div>
        ) : drops.length === 0 ? (
          <div className="text-center py-20 text-gray-400">물방울 내역이 없습니다.</div>
        ) : (
          <div className="space-y-2">
            {drops.map((tx) => (
              <div key={tx.id} className="flex items-center justify-between px-4 py-3 bg-white rounded-lg border border-gray-100">
                <div>
                  <div className="text-sm font-medium text-gray-700">{tx.reasonLabel}</div>
                  {tx.reasonDetail && (
                    <div className="text-xs text-gray-400">{tx.reasonDetail}</div>
                  )}
                  <div className="text-xs text-gray-300 mt-0.5">{new Date(tx.createdAt).toLocaleDateString("ko-KR")}</div>
                </div>
                <span className={`text-sm font-bold ${tx.amount > 0 ? "text-accent" : "text-red-400"}`}>
                  {tx.amount > 0 ? `+${tx.amount}` : tx.amount}
                </span>
              </div>
            ))}
            {hasMore && (
              <div className="flex justify-center mt-4">
                <button
                  onClick={loadMore}
                  disabled={tabLoading}
                  className="px-6 py-3 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm font-medium text-gray-700 transition-colors disabled:opacity-50"
                >
                  {tabLoading ? "불러오는 중..." : "더보기"}
                </button>
              </div>
            )}
          </div>
        )
      ) : (
        // 글/북마크 그리드
        tabLoading && posts.length === 0 ? (
          <div className="flex justify-center py-20">
            <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
          </div>
        ) : posts.length === 0 ? (
          <div className="text-center py-20 text-gray-400">
            {activeTab === "posts" ? "작성한 글이 없습니다." : "북마크한 글이 없습니다."}
          </div>
        ) : (
          <>
            <div className="grid grid-cols-3 gap-0.5 sm:gap-1">
              {posts.map((post) => (
                <GridItem key={post.id} post={post} onBookmarkChange={handleBookmarkChange} />
              ))}
            </div>
            {hasMore && (
              <div className="flex justify-center mt-8">
                <button
                  onClick={loadMore}
                  disabled={tabLoading}
                  className="px-6 py-3 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm font-medium text-gray-700 transition-colors disabled:opacity-50"
                >
                  {tabLoading ? "불러오는 중..." : "더보기"}
                </button>
              </div>
            )}
          </>
        )
      )}

      {/* 레벨 가이드 팝업 */}
      {showLevelGuide && growth && (
        <div
          className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4"
          onClick={() => setShowLevelGuide(false)}
        >
          <div
            className="bg-white rounded-2xl max-w-md w-full max-h-[85vh] overflow-y-auto p-4"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-base font-bold text-gray-800">레벨 가이드</h2>
              <button
                onClick={() => setShowLevelGuide(false)}
                className="text-gray-400 hover:text-gray-600 text-xl leading-none px-2"
                aria-label="닫기"
              >
                ×
              </button>
            </div>
            <PlantLevelGuide growth={growth} />
          </div>
        </div>
      )}
    </div>
  );
}
