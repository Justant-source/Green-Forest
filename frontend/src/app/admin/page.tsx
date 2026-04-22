"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import {
  AdminUser, AdminParty, AdminStats, Quest,
  CategoryInfo, CategoryRequestInfo,
  AttendancePhrase, GachaPrizeInfo, AdminDeliveryItem, AdminAttendanceDeliveryItem, AdminPost,
} from "@/types";
import {
  getAdminCategories, createAdminCategory, deleteAdminCategory,
  getPendingCategoryRequests, approveCategoryRequest, rejectCategoryRequest,
  getAdminUsers, updateAdminUser, resetAdminUserPassword, deleteAdminUser,
  getAdminParties, createAdminParty, deleteAdminParty,
  getAdminStats, getQuests,
  createAdminQuest, deleteAdminQuest,
  awardDrops, deductDrops,
  adminListAnnouncements, adminCreateAnnouncement, adminActivateAnnouncement,
  adminDeactivateAllAnnouncements, adminDeleteAnnouncement,
  adminListPhrases, adminCreatePhrase, adminUpdatePhrase, adminDeletePhrase,
  adminListAllPrizes, adminCreatePrize, adminUpdatePrize, adminDeactivatePrize,
  adminListDeliveries, adminMarkDelivered,
  adminListAttendanceDeliveries, adminMarkAttendanceDelivered,
  adminListPosts, adminUpdatePost, adminDeletePost,
} from "@/lib/api";

type AdminTab = "dashboard" | "users" | "parties" | "quests" | "drops" | "categories" | "announce" | "attendance" | "gacha" | "posts";

const PLANT_OPTIONS = [
  { value: "TABLE_PALM", label: "테이블야자" },
  { value: "SPATHIPHYLLUM", label: "스파티필럼" },
  { value: "HONG_KONG_PALM", label: "홍콩야자" },
  { value: "ORANGE_JASMINE", label: "오렌지자스민" },
];

export default function AdminPage() {
  const router = useRouter();
  const { isLoggedIn, isAdmin } = useAuth();
  const [tab, setTab] = useState<AdminTab>("dashboard");
  const [loading, setLoading] = useState(true);

  // Dashboard
  const [stats, setStats] = useState<AdminStats | null>(null);

  // Users
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [parties, setParties] = useState<AdminParty[]>([]);

  // Quests
  const [quests, setQuests] = useState<Quest[]>([]);

  // Categories
  const [categories, setCategories] = useState<CategoryInfo[]>([]);
  const [requests, setRequests] = useState<CategoryRequestInfo[]>([]);

  // Drops form
  const [dropUserId, setDropUserId] = useState<number | null>(null);
  const [dropAmount, setDropAmount] = useState("");
  const [dropReason, setDropReason] = useState("");
  const [dropMode, setDropMode] = useState<"award" | "deduct">("award");

  // Quest create form
  const [questForm, setQuestForm] = useState({
    title: "", description: "", rewardDrops: "50",
    startDate: "", endDate: "", isVoteType: false,
  });

  // Category create
  const [newCatName, setNewCatName] = useState("");
  const [newCatLabel, setNewCatLabel] = useState("");
  const [newCatColor, setNewCatColor] = useState("green");

  // Announce
  const [annTitle, setAnnTitle] = useState("");
  const [annContent, setAnnContent] = useState("");
  const [announcements, setAnnouncements] = useState<{ id: number; title: string; content: string; active: boolean; createdAt: string }[]>([]);

  // Attendance
  const [phrases, setPhrases] = useState<AttendancePhrase[]>([]);
  const [attendanceFrom, setAttendanceFrom] = useState("");
  const [attendanceTo, setAttendanceTo] = useState("");
  const [newPhrase, setNewPhrase] = useState("");
  const [newCategory, setNewCategory] = useState("");
  const [attendanceDeliveries, setAttendanceDeliveries] = useState<AdminAttendanceDeliveryItem[]>([]);
  const [attendanceDeliveryTab, setAttendanceDeliveryTab] = useState<"PENDING" | "DELIVERED">("PENDING");
  const [attendanceDeliveryMemo, setAttendanceDeliveryMemo] = useState<Record<number, string>>({});

  // Gacha
  const [prizes, setPrizes] = useState<GachaPrizeInfo[]>([]);
  const [newPrizeName, setNewPrizeName] = useState("");
  const [newPrizeCash, setNewPrizeCash] = useState("");
  const [newPrizeStock, setNewPrizeStock] = useState("");
  const [newPrizeTier, setNewPrizeTier] = useState<"COMMON" | "RARE" | "EPIC" | "LEGENDARY">("COMMON");
  const [newPrizeImageFile, setNewPrizeImageFile] = useState<File | null>(null);
  const [newPrizeImagePreview, setNewPrizeImagePreview] = useState<string | null>(null);
  const [editingPrizeId, setEditingPrizeId] = useState<number | null>(null);
  const [editPrizeForm, setEditPrizeForm] = useState<{
    name: string; description: string; imageUrl: string;
    cashValue: string; totalStock: string; remainingStock: string;
    tier: "COMMON" | "RARE" | "EPIC" | "LEGENDARY";
    evMultiplier: string; active: boolean;
  } | null>(null);
  const [editPrizeImageFile, setEditPrizeImageFile] = useState<File | null>(null);
  const [editPrizeImagePreview, setEditPrizeImagePreview] = useState<string | null>(null);
  const [deliveries, setDeliveries] = useState<AdminDeliveryItem[]>([]);
  const [deliveryTab, setDeliveryTab] = useState<"PENDING" | "DELIVERED">("PENDING");
  const [deliveryMemo, setDeliveryMemo] = useState<Record<number, string>>({});

  useEffect(() => {
    if (!isLoggedIn || !isAdmin) {
      router.replace("/");
      return;
    }
    loadAllData();
  }, [isLoggedIn, isAdmin, router]);

  useEffect(() => {
    if (tab !== "gacha") return;
    (async () => {
      try {
        const data = await adminListDeliveries(deliveryTab);
        setDeliveries(data);
      } catch {
        setDeliveries([]);
      }
    })();
  }, [tab, deliveryTab]);

  useEffect(() => {
    if (tab !== "attendance") return;
    (async () => {
      try {
        const data = await adminListAttendanceDeliveries(attendanceDeliveryTab);
        setAttendanceDeliveries(data);
      } catch {
        setAttendanceDeliveries([]);
      }
    })();
  }, [tab, attendanceDeliveryTab]);

  const loadAllData = async () => {
    try {
      const [s, u, p, q, cats, reqs, phr, prz, anns] = await Promise.all([
        getAdminStats(),
        getAdminUsers(),
        getAdminParties(),
        getQuests(),
        getAdminCategories(),
        getPendingCategoryRequests(),
        adminListPhrases(),
        adminListAllPrizes(),
        adminListAnnouncements(),
      ]);
      setStats(s);
      setUsers(u);
      setParties(p);
      setQuests(q);
      setCategories(cats);
      setRequests(reqs);
      setPhrases(phr);
      setPrizes([...prz].sort((a, b) => a.displayOrder - b.displayOrder));
      setAnnouncements(anns);
    } catch (error) {
      console.error("Failed to load admin data:", error);
    } finally {
      setLoading(false);
    }
  };

  if (!isLoggedIn || !isAdmin) return null;

  if (loading) {
    return (
      <div className="flex justify-center py-20">
        <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
      </div>
    );
  }

  const tabs: { key: AdminTab; label: string }[] = [
    { key: "dashboard", label: "대시보드" },
    { key: "users", label: "유저" },
    { key: "posts", label: "게시글" },
    { key: "parties", label: "파티" },
    { key: "quests", label: "퀘스트" },
    { key: "drops", label: "물방울" },
    { key: "categories", label: "게시판" },
    { key: "attendance", label: "출석" },
    { key: "gacha", label: "뽑기" },
    { key: "announce", label: "공지" },
  ];

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold mb-4">관리자</h1>

      <div className="flex gap-1 mb-6 overflow-x-auto scrollbar-hide border-b">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-2.5 text-sm font-medium transition-colors border-b-2 -mb-px whitespace-nowrap ${
              tab === t.key
                ? "border-forest-500 text-forest-500"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Dashboard */}
      {tab === "dashboard" && stats && (
        <div className="space-y-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <StatCard label="전체 유저" value={stats.totalUsers} />
            <StatCard label="이번달 글" value={stats.monthlyPosts} />
            <StatCard label="이번달 물방울" value={stats.monthlyDropsIssued ?? 0} />
            <StatCard label="이번달 거래" value={stats.monthlyTransactions} />
          </div>
          {stats.partyStats.length > 0 && (
            <div>
              <h3 className="font-semibold mb-3">파티별 물방울</h3>
              <div className="space-y-2">
                {stats.partyStats.map((ps) => (
                  <div key={ps.partyId} className="flex items-center gap-4 px-4 py-3 bg-white rounded-lg border">
                    <span className="font-medium text-gray-900 w-20">{ps.partyName}</span>
                    <div className="flex-1 bg-gray-100 rounded-full h-4 overflow-hidden">
                      <div
                        className="bg-forest-400 h-full rounded-full"
                        style={{ width: `${Math.min((ps.totalDrops / Math.max(...stats.partyStats.map((s) => s.totalDrops), 1)) * 100, 100)}%` }}
                      />
                    </div>
                    <span className="text-sm font-medium text-gray-700 w-24 text-right">{ps.totalDrops.toLocaleString()}</span>
                    <span className="text-xs text-gray-400 w-12">{ps.memberCount}명</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Users */}
      {tab === "users" && (
        <UsersPanel
          users={users}
          setUsers={setUsers}
          parties={parties}
        />
      )}

      {/* Parties */}
      {tab === "parties" && (
        <div className="space-y-4">
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const input = (e.target as HTMLFormElement).elements.namedItem("partyName") as HTMLInputElement;
              if (!input.value.trim()) return;
              try {
                await createAdminParty(input.value.trim());
                const data = await getAdminParties();
                setParties(data);
                input.value = "";
              } catch { alert("파티 생성 실패"); }
            }}
            className="flex gap-2"
          >
            <input name="partyName" placeholder="파티 이름" className="px-3 py-2 border border-gray-300 rounded-lg text-sm flex-1 focus:outline-none focus:ring-2 focus:ring-forest-500" />
            <button type="submit" className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600 transition-colors">추가</button>
          </form>
          <div className="space-y-2">
            {parties.map((p) => (
              <div key={p.id} className="flex items-center justify-between px-4 py-3 bg-white rounded-lg border">
                <div>
                  <span className="font-medium text-sm">{p.name}</span>
                  <span className="text-xs text-gray-400 ml-2">{p.memberCount}명</span>
                </div>
                <button
                  onClick={async () => {
                    if (!confirm(`"${p.name}" 파티를 삭제하시겠습니까?`)) return;
                    try {
                      await deleteAdminParty(p.id);
                      setParties((prev) => prev.filter((pp) => pp.id !== p.id));
                    } catch { alert("삭제 실패"); }
                  }}
                  className="text-sm text-red-500 hover:text-red-700 font-medium"
                >
                  삭제
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Quests */}
      {tab === "quests" && (
        <div className="space-y-6">
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              try {
                await createAdminQuest({
                  title: questForm.title,
                  description: questForm.description,
                  rewardDrops: Number(questForm.rewardDrops),
                  startDate: questForm.startDate,
                  endDate: questForm.endDate,
                  isVoteType: questForm.isVoteType,
                });
                const data = await getQuests();
                setQuests(data);
                setQuestForm({ title: "", description: "", rewardDrops: "50", startDate: "", endDate: "", isVoteType: false });
              } catch { alert("퀘스트 생성 실패"); }
            }}
            className="bg-white p-4 rounded-xl border space-y-3"
          >
            <h3 className="font-semibold text-sm">퀘스트 생성</h3>
            <div className="grid grid-cols-2 gap-3">
              <input value={questForm.title} onChange={(e) => setQuestForm((f) => ({ ...f, title: e.target.value }))} placeholder="퀘스트 제목" className="px-3 py-2 border border-gray-300 rounded-lg text-sm col-span-2 focus:outline-none focus:ring-2 focus:ring-forest-500" required />
              <textarea value={questForm.description} onChange={(e) => setQuestForm((f) => ({ ...f, description: e.target.value }))} placeholder="설명" className="px-3 py-2 border border-gray-300 rounded-lg text-sm col-span-2 resize-none focus:outline-none focus:ring-2 focus:ring-forest-500" rows={2} required />
              <input type="number" value={questForm.rewardDrops} onChange={(e) => setQuestForm((f) => ({ ...f, rewardDrops: e.target.value }))} placeholder="보상 물방울" className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500" required />
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input type="checkbox" checked={questForm.isVoteType} onChange={(e) => setQuestForm((f) => ({ ...f, isVoteType: e.target.checked }))} className="w-4 h-4 accent-forest-500" />
                투표 퀘스트
              </label>
              <input type="date" value={questForm.startDate} onChange={(e) => setQuestForm((f) => ({ ...f, startDate: e.target.value }))} className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500" required />
              <input type="date" value={questForm.endDate} onChange={(e) => setQuestForm((f) => ({ ...f, endDate: e.target.value }))} className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500" required />
            </div>
            <button type="submit" className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600 transition-colors">생성</button>
          </form>

          <div className="space-y-2">
            {quests.map((q) => (
              <div key={q.id} className="flex items-center justify-between px-4 py-3 bg-white rounded-lg border">
                <div>
                  <div className="font-medium text-sm">{q.title}</div>
                  <div className="text-xs text-gray-400">{q.startDate} ~ {q.endDate} | +{q.rewardDrops} 물방울</div>
                </div>
                <button
                  onClick={async () => {
                    if (!confirm("퀘스트를 삭제하시겠습니까?")) return;
                    try {
                      await deleteAdminQuest(q.id);
                      setQuests((prev) => prev.filter((qq) => qq.id !== q.id));
                    } catch { alert("삭제 실패"); }
                  }}
                  className="text-sm text-red-500 hover:text-red-700 font-medium"
                >
                  삭제
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Drops */}
      {tab === "drops" && (
        <div className="max-w-lg space-y-4">
          <div className="flex gap-2 mb-4">
            <button
              onClick={() => setDropMode("award")}
              className={`px-4 py-2 rounded-full text-sm font-medium ${dropMode === "award" ? "bg-forest-500 text-white" : "bg-white border text-gray-700"}`}
            >
              지급
            </button>
            <button
              onClick={() => setDropMode("deduct")}
              className={`px-4 py-2 rounded-full text-sm font-medium ${dropMode === "deduct" ? "bg-red-500 text-white" : "bg-white border text-gray-700"}`}
            >
              차감
            </button>
          </div>
          <select
            value={dropUserId ?? ""}
            onChange={(e) => setDropUserId(e.target.value ? Number(e.target.value) : null)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-forest-500"
          >
            <option value="">유저 선택</option>
            {users.map((u) => <option key={u.id} value={u.id}>{u.nickname} | {u.name} | {u.email}</option>)}
          </select>
          <input
            type="number"
            value={dropAmount}
            onChange={(e) => setDropAmount(e.target.value)}
            placeholder="물방울 수량"
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
          />
          <input
            type="text"
            value={dropReason}
            onChange={(e) => setDropReason(e.target.value)}
            placeholder="사유"
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
          />
          <button
            onClick={async () => {
              if (!dropUserId || !dropAmount || !dropReason) { alert("모든 항목을 입력하세요."); return; }
              try {
                if (dropMode === "award") {
                  await awardDrops(dropUserId, Number(dropAmount), dropReason);
                } else {
                  await deductDrops(dropUserId, Number(dropAmount), dropReason);
                }
                alert(`물방울 ${dropMode === "award" ? "지급" : "차감"} 완료`);
                setDropAmount("");
                setDropReason("");
                setDropUserId(null);
                const updatedUsers = await getAdminUsers();
                setUsers(updatedUsers);
              } catch { alert("실패"); }
            }}
            className={`px-6 py-2 rounded-lg text-sm font-medium text-white transition-colors ${
              dropMode === "award" ? "bg-forest-500 hover:bg-forest-600" : "bg-red-500 hover:bg-red-600"
            }`}
          >
            {dropMode === "award" ? "지급" : "차감"}
          </button>
        </div>
      )}

      {/* Categories */}
      {tab === "categories" && (
        <div className="space-y-6">
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              if (!newCatName.trim() || !newCatLabel.trim()) return;
              try {
                const created = await createAdminCategory({ name: newCatName.trim(), label: newCatLabel.trim(), color: newCatColor });
                setCategories((prev) => [...prev, created]);
                setNewCatName("");
                setNewCatLabel("");
              } catch { alert("생성 실패"); }
            }}
            className="flex flex-wrap gap-2"
          >
            <input value={newCatName} onChange={(e) => setNewCatName(e.target.value)} placeholder="코드" className="px-3 py-2 border border-gray-300 rounded-lg text-sm w-28 focus:outline-none focus:ring-2 focus:ring-forest-500" required />
            <input value={newCatLabel} onChange={(e) => setNewCatLabel(e.target.value)} placeholder="표시 이름" className="px-3 py-2 border border-gray-300 rounded-lg text-sm w-28 focus:outline-none focus:ring-2 focus:ring-forest-500" required />
            <button type="submit" className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600 transition-colors">추가</button>
          </form>
          <div className="space-y-2">
            {categories.map((cat) => (
              <div key={cat.id} className="flex items-center justify-between px-4 py-3 border border-gray-200 rounded-lg bg-white">
                <div className="flex items-center gap-3">
                  <span className="font-medium text-sm">{cat.label}</span>
                  <span className="text-xs text-gray-400">{cat.name}</span>
                </div>
                <button
                  onClick={async () => {
                    if (!confirm(`"${cat.label}" 게시판를 삭제하시겠습니까?`)) return;
                    try {
                      await deleteAdminCategory(cat.id);
                      setCategories((prev) => prev.filter((c) => c.id !== cat.id));
                    } catch { alert("삭제 실패"); }
                  }}
                  className="text-sm text-red-500 hover:text-red-700 font-medium"
                >
                  삭제
                </button>
              </div>
            ))}
          </div>

          {requests.length > 0 && (
            <div>
              <h3 className="font-semibold mb-3">대기중인 요청</h3>
              <div className="space-y-2">
                {requests.map((req) => (
                  <div key={req.id} className="flex items-center justify-between px-4 py-3 border border-gray-200 rounded-lg bg-white">
                    <div>
                      <span className="font-medium text-sm">{req.name}</span>
                      <span className="text-xs text-gray-400 ml-2">by {req.requesterNickname}</span>
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={async () => {
                          try {
                            const created = await approveCategoryRequest(req.id, { label: req.name, color: "green", hasStatus: false });
                            setRequests((prev) => prev.filter((r) => r.id !== req.id));
                            setCategories((prev) => [...prev, created]);
                          } catch { alert("승인 실패"); }
                        }}
                        className="text-xs text-forest-500 font-medium"
                      >
                        승인
                      </button>
                      <button
                        onClick={async () => {
                          try {
                            await rejectCategoryRequest(req.id, "");
                            setRequests((prev) => prev.filter((r) => r.id !== req.id));
                          } catch { alert("거절 실패"); }
                        }}
                        className="text-xs text-red-500 font-medium"
                      >
                        거절
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Attendance */}
      {tab === "attendance" && (
        <div className="space-y-6">
          <div className="max-w-lg space-y-3">
            <h3 className="font-semibold text-sm">기간 통계</h3>
            <div className="flex gap-2">
              <input
                type="date"
                value={attendanceFrom}
                onChange={(e) => setAttendanceFrom(e.target.value)}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
              />
              <input
                type="date"
                value={attendanceTo}
                onChange={(e) => setAttendanceTo(e.target.value)}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
              />
              <button className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600 transition-colors">
                조회
              </button>
            </div>
          </div>

          {/* 출석 당첨 수령 관리 */}
          <div>
            <h3 className="font-semibold text-sm mb-3">출석 당첨 수령 관리</h3>
            <div className="flex gap-2 mb-3">
              {(["PENDING", "DELIVERED"] as const).map((t) => (
                <button
                  key={t}
                  onClick={() => setAttendanceDeliveryTab(t)}
                  className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
                    attendanceDeliveryTab === t ? "bg-forest-500 text-white" : "bg-white border border-gray-300 text-gray-600 hover:bg-gray-50"
                  }`}
                >
                  {t === "PENDING" ? "미수령" : "수령완료"}
                </button>
              ))}
            </div>
            {attendanceDeliveries.length === 0 ? (
              <div className="text-center text-gray-400 text-sm py-6 bg-white rounded-xl border">
                {attendanceDeliveryTab === "PENDING" ? "미수령 당첨자가 없습니다" : "수령 완료 내역이 없습니다"}
              </div>
            ) : (
              <div className="space-y-2 max-w-2xl">
                {attendanceDeliveries.map((d) => (
                  <div key={d.id} className={`bg-white rounded-xl border-l-4 p-4 ${d.deliveryStatus === "PENDING" ? "border-orange-400" : "border-green-400"}`}>
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="font-semibold text-sm text-gray-800">{d.userName}</span>
                          <span className="text-gray-400 text-xs">({d.userNickname} / {d.userEmail})</span>
                          <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${d.deliveryStatus === "PENDING" ? "bg-orange-100 text-orange-700" : "bg-green-100 text-green-700"}`}>
                            {d.deliveryStatus === "PENDING" ? "미수령" : "수령완료"}
                          </span>
                        </div>
                        <div className="text-xs text-gray-500 mt-1">
                          당첨일 {d.winDate} · 출석시각 {new Date(d.checkinAt).toLocaleString("ko-KR")}
                          {d.winnerDrawnAt && <> · 추첨 {new Date(d.winnerDrawnAt).toLocaleString("ko-KR")}</>}
                        </div>
                        {d.message && <div className="text-xs text-gray-600 mt-1">한마디: {d.message}</div>}
                        {d.deliveryStatus === "DELIVERED" && (
                          <div className="text-xs text-green-700 mt-1">
                            전달완료 {d.deliveredAt && new Date(d.deliveredAt).toLocaleString("ko-KR")}
                            {d.deliveryMemo && <span className="ml-2 text-gray-500">메모: {d.deliveryMemo}</span>}
                          </div>
                        )}
                      </div>
                      {d.deliveryStatus === "PENDING" && (
                        <div className="flex items-center gap-2 shrink-0">
                          <input
                            type="text"
                            placeholder="메모 (선택)"
                            value={attendanceDeliveryMemo[d.id] ?? ""}
                            onChange={(e) => setAttendanceDeliveryMemo(prev => ({ ...prev, [d.id]: e.target.value }))}
                            className="px-2 py-1 border border-gray-300 rounded-lg text-xs w-28 focus:outline-none focus:ring-1 focus:ring-forest-500"
                          />
                          <button
                            onClick={async () => {
                              try {
                                await adminMarkAttendanceDelivered(d.id, attendanceDeliveryMemo[d.id]);
                                const updated = await adminListAttendanceDeliveries(attendanceDeliveryTab);
                                setAttendanceDeliveries(updated);
                              } catch { alert("처리 실패"); }
                            }}
                            className="px-3 py-1.5 bg-green-500 text-white rounded-lg text-xs font-medium hover:bg-green-600 whitespace-nowrap"
                          >
                            수령 완료
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div>
            <h3 className="font-semibold text-sm mb-3">말뭉치 관리</h3>
            <div className="flex gap-2 mb-3">
              <input
                type="text"
                value={newPhrase}
                onChange={(e) => setNewPhrase(e.target.value)}
                placeholder="한마디"
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
              />
              <input
                type="text"
                value={newCategory}
                onChange={(e) => setNewCategory(e.target.value)}
                placeholder="카테고리"
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
              />
              <button
                onClick={async () => {
                  if (!newPhrase.trim() || !newCategory.trim()) return;
                  try {
                    await adminCreatePhrase({
                      phrase: newPhrase.trim(),
                      category: newCategory.trim(),
                    });
                    const updated = await adminListPhrases();
                    setPhrases(updated);
                    setNewPhrase("");
                    setNewCategory("");
                  } catch {
                    alert("생성 실패");
                  }
                }}
                className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600 transition-colors"
              >
                추가
              </button>
            </div>
            <div className="space-y-2">
              {phrases.map((p) => (
                <div
                  key={p.id}
                  className="flex items-center justify-between px-4 py-2 bg-white rounded-lg border"
                >
                  <div className="flex-1">
                    <span className="text-sm font-medium">{p.phrase}</span>
                    <span className="text-xs text-gray-400 ml-2">{p.category}</span>
                  </div>
                  <button
                    onClick={async () => {
                      if (!confirm("삭제하시겠습니까?")) return;
                      try {
                        await adminDeletePhrase(p.id);
                        const updated = await adminListPhrases();
                        setPhrases(updated);
                      } catch {
                        alert("삭제 실패");
                      }
                    }}
                    className="text-xs text-red-500 hover:text-red-700 font-medium"
                  >
                    삭제
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Gacha */}
      {tab === "gacha" && (() => {
        const TIER_BORDER: Record<string, string> = {
          COMMON: "border-gray-300",
          RARE: "border-blue-300",
          EPIC: "border-purple-400",
          LEGENDARY: "border-yellow-400",
        };
        const TIER_BG: Record<string, string> = {
          COMMON: "bg-gray-50",
          RARE: "bg-blue-50",
          EPIC: "bg-purple-50",
          LEGENDARY: "bg-yellow-50",
        };
        const TIER_BADGE: Record<string, string> = {
          COMMON: "bg-gray-200 text-gray-700",
          RARE: "bg-blue-200 text-blue-800",
          EPIC: "bg-purple-200 text-purple-800",
          LEGENDARY: "bg-yellow-300 text-yellow-900",
        };
        const inputCls = "w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500";

        const openEdit = (p: GachaPrizeInfo) => {
          setEditingPrizeId(p.id);
          setEditPrizeForm({
            name: p.name,
            description: p.description ?? "",
            imageUrl: p.imageUrl ?? "",
            cashValue: String(p.cashValue),
            totalStock: String(p.remainingStock),
            remainingStock: String(p.remainingStock),
            tier: p.tier,
            evMultiplier: String(p.evMultiplier ?? 1),
            active: true,
          });
          setEditPrizeImageFile(null);
          setEditPrizeImagePreview(p.imageUrl ?? null);
        };

        const handleMoveOrder = async (idx: number, dir: -1 | 1) => {
          const swapIdx = idx + dir;
          if (swapIdx < 0 || swapIdx >= prizes.length) return;

          // 배열에서 두 항목 위치 교환
          const newOrder = [...prizes];
          [newOrder[idx], newOrder[swapIdx]] = [newOrder[swapIdx], newOrder[idx]];

          // 낙관적 UI 업데이트 (즉시 반영)
          setPrizes(newOrder);

          // 전체 인덱스 기준으로 displayOrder 재기록
          try {
            await Promise.all(
              newOrder.map((p, i) => adminUpdatePrize(p.id, { displayOrder: i }))
            );
          } catch {
            alert("순서 변경 실패");
            setPrizes([...(await adminListAllPrizes())].sort((a, b) => a.displayOrder - b.displayOrder));
          }
        };

        const loadDeliveries = async (tab: "PENDING" | "DELIVERED") => {
          try {
            const data = await adminListDeliveries(tab);
            setDeliveries(data);
          } catch { setDeliveries([]); }
        };

        return (
          <div className="space-y-8">
            {/* 당첨 수령 관리 */}
            <div>
              <h3 className="font-semibold text-sm mb-3">당첨 수령 관리</h3>
              <div className="flex gap-2 mb-3">
                {(["PENDING", "DELIVERED"] as const).map((t) => (
                  <button
                    key={t}
                    onClick={async () => { setDeliveryTab(t); await loadDeliveries(t); }}
                    className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
                      deliveryTab === t ? "bg-forest-500 text-white" : "bg-white border border-gray-300 text-gray-600 hover:bg-gray-50"
                    }`}
                  >
                    {t === "PENDING" ? "미수령" : "수령완료"}
                  </button>
                ))}
              </div>
              {deliveries.length === 0 ? (
                <div className="text-center text-gray-400 text-sm py-6 bg-white rounded-xl border">
                  {deliveryTab === "PENDING" ? "미수령 당첨자가 없습니다" : "수령 완료 내역이 없습니다"}
                </div>
              ) : (
                <div className="space-y-2 max-w-2xl">
                  {deliveries.map((d) => (
                    <div key={d.id} className={`bg-white rounded-xl border-l-4 p-4 ${d.deliveryStatus === "PENDING" ? "border-orange-400" : "border-green-400"}`}>
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-semibold text-sm text-gray-800">{d.userNickname}</span>
                            <span className="text-gray-400 text-xs">→</span>
                            <span className="text-sm text-gray-700">{d.prizeName}</span>
                            <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${d.deliveryStatus === "PENDING" ? "bg-orange-100 text-orange-700" : "bg-green-100 text-green-700"}`}>
                              {d.deliveryStatus === "PENDING" ? "미수령" : "수령완료"}
                            </span>
                          </div>
                          <div className="text-xs text-gray-400 mt-0.5">
                            {new Date(d.createdAt).toLocaleString("ko-KR")} · {d.prizeCashValue.toLocaleString()}원
                            {d.deliveryMemo && <span className="ml-2 text-gray-500">메모: {d.deliveryMemo}</span>}
                          </div>
                        </div>
                        {d.deliveryStatus === "PENDING" && (
                          <div className="flex items-center gap-2 shrink-0">
                            <input
                              type="text"
                              placeholder="메모 (선택)"
                              value={deliveryMemo[d.id] ?? ""}
                              onChange={(e) => setDeliveryMemo(prev => ({ ...prev, [d.id]: e.target.value }))}
                              className="px-2 py-1 border border-gray-300 rounded-lg text-xs w-28 focus:outline-none focus:ring-1 focus:ring-forest-500"
                            />
                            <button
                              onClick={async () => {
                                try {
                                  await adminMarkDelivered(d.id, deliveryMemo[d.id]);
                                  await loadDeliveries(deliveryTab);
                                } catch { alert("처리 실패"); }
                              }}
                              className="px-3 py-1.5 bg-green-500 text-white rounded-lg text-xs font-medium hover:bg-green-600 whitespace-nowrap"
                            >
                              수령 완료
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* 상품 추가 폼 */}
            <form
              onSubmit={async (e) => {
                e.preventDefault();
                if (!newPrizeName.trim() || !newPrizeCash || !newPrizeStock) return;
                try {
                  await adminCreatePrize({
                    name: newPrizeName.trim(),
                    cashValue: Number(newPrizeCash),
                    totalStock: Number(newPrizeStock),
                    tier: newPrizeTier,
                  }, newPrizeImageFile ?? undefined);
                  setPrizes([...(await adminListAllPrizes())].sort((a, b) => a.displayOrder - b.displayOrder));
                  setNewPrizeName(""); setNewPrizeCash(""); setNewPrizeStock("");
                  setNewPrizeImageFile(null); setNewPrizeImagePreview(null);
                } catch { alert("상품 생성 실패"); }
              }}
              className="bg-white p-4 rounded-xl border space-y-3 max-w-lg"
            >
              <h3 className="font-semibold text-sm">상품 추가</h3>
              <input type="text" value={newPrizeName} onChange={(e) => setNewPrizeName(e.target.value)} placeholder="상품명" className={inputCls} required />
              <input type="number" value={newPrizeCash} onChange={(e) => setNewPrizeCash(e.target.value)} placeholder="현금가치 (원)" className={inputCls} required />
              <input type="number" value={newPrizeStock} onChange={(e) => setNewPrizeStock(e.target.value)} placeholder="재고" className={inputCls} required />
              <select value={newPrizeTier} onChange={(e) => setNewPrizeTier(e.target.value as "COMMON" | "RARE" | "EPIC" | "LEGENDARY")} className={inputCls}>
                <option value="COMMON">일반</option>
                <option value="RARE">레어</option>
                <option value="EPIC">에픽</option>
                <option value="LEGENDARY">레전더리</option>
              </select>
              <div>
                <label className="text-xs text-gray-500 mb-1 block">이미지 (선택)</label>
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => {
                    const file = e.target.files?.[0] ?? null;
                    setNewPrizeImageFile(file);
                    if (file) {
                      const reader = new FileReader();
                      reader.onload = (ev) => setNewPrizeImagePreview(ev.target?.result as string);
                      reader.readAsDataURL(file);
                    } else {
                      setNewPrizeImagePreview(null);
                    }
                  }}
                  className="w-full text-xs text-gray-500 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-medium file:bg-forest-50 file:text-forest-600 hover:file:bg-forest-100"
                />
                {newPrizeImagePreview && (
                  <img src={newPrizeImagePreview} alt="미리보기" className="mt-2 w-16 h-16 object-cover rounded-lg border border-gray-200" />
                )}
              </div>
              <button type="submit" className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600 transition-colors">추가</button>
            </form>

            {/* 상품 목록 */}
            <div>
              <h3 className="font-semibold text-sm mb-3">상품 목록</h3>
              <div className="space-y-2 max-w-2xl">
                {prizes.map((p, idx) => (
                  <div key={p.id} className={`border-2 rounded-xl ${TIER_BORDER[p.tier]} ${p.active === false ? "bg-gray-100 opacity-60" : TIER_BG[p.tier]}`}>
                    {/* 헤더 행 */}
                    <div className="flex items-center gap-2 px-4 py-3">
                      {/* 순서 버튼 */}
                      <div className="flex flex-col gap-0.5">
                        <button onClick={() => handleMoveOrder(idx, -1)} disabled={idx === 0} className="w-6 h-5 flex items-center justify-center text-gray-400 hover:text-gray-700 disabled:opacity-20 text-xs leading-none">▲</button>
                        <button onClick={() => handleMoveOrder(idx, 1)} disabled={idx === prizes.length - 1} className="w-6 h-5 flex items-center justify-center text-gray-400 hover:text-gray-700 disabled:opacity-20 text-xs leading-none">▼</button>
                      </div>

                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <span className={`text-xs px-2 py-0.5 rounded-full font-semibold ${TIER_BADGE[p.tier]}`}>{p.tierLabel}</span>
                          <span className="font-medium text-sm">{p.name}</span>
                          {!p.remainingStock && <span className="text-xs text-red-400 font-medium">품절</span>}
                        </div>
                        <div className="text-xs text-gray-500 mt-0.5">
                          {p.cashValue.toLocaleString()}원 | 재고 {p.remainingStock}개 | 확률 {(p.currentProbability * 100).toFixed(2)}%
                        </div>
                      </div>

                      <div className="flex gap-2 shrink-0">
                        <button
                          onClick={() => editingPrizeId === p.id ? setEditingPrizeId(null) : openEdit(p)}
                          className="text-xs px-3 py-1.5 bg-forest-500 text-white rounded-lg hover:bg-forest-600 font-medium"
                        >
                          {editingPrizeId === p.id ? "닫기" : "수정"}
                        </button>
                        {p.active === false ? (
                          <button
                            onClick={async () => {
                              try {
                                await adminUpdatePrize(p.id, { active: true });
                                setPrizes([...(await adminListAllPrizes())].sort((a, b) => a.displayOrder - b.displayOrder));
                              } catch { alert("활성화 실패"); }
                            }}
                            className="text-xs px-3 py-1.5 border border-green-400 text-green-600 rounded-lg hover:bg-green-50 font-medium"
                          >
                            활성화
                          </button>
                        ) : (
                          <button
                            onClick={async () => {
                              if (!confirm(`"${p.name}"을 비활성화하시겠습니까?`)) return;
                              try {
                                await adminDeactivatePrize(p.id);
                                setPrizes([...(await adminListAllPrizes())].sort((a, b) => a.displayOrder - b.displayOrder));
                              } catch { alert("비활성화 실패"); }
                            }}
                            className="text-xs px-3 py-1.5 border border-red-300 text-red-500 rounded-lg hover:bg-red-50 font-medium"
                          >
                            비활성화
                          </button>
                        )}
                      </div>
                    </div>

                    {/* 수정 폼 (펼침) */}
                    {editingPrizeId === p.id && editPrizeForm && (
                      <div className="border-t border-gray-200 px-4 py-4 space-y-3 bg-white rounded-b-xl">
                        <div className="grid grid-cols-2 gap-3">
                          <div>
                            <label className="text-xs text-gray-500 mb-1 block">상품명</label>
                            <input type="text" value={editPrizeForm.name} onChange={(e) => setEditPrizeForm({ ...editPrizeForm, name: e.target.value })} className={inputCls} />
                          </div>
                          <div>
                            <label className="text-xs text-gray-500 mb-1 block">등급</label>
                            <select value={editPrizeForm.tier} onChange={(e) => setEditPrizeForm({ ...editPrizeForm, tier: e.target.value as "COMMON" | "RARE" | "EPIC" | "LEGENDARY" })} className={inputCls}>
                              <option value="COMMON">일반</option>
                              <option value="RARE">레어</option>
                              <option value="EPIC">에픽</option>
                              <option value="LEGENDARY">레전더리</option>
                            </select>
                          </div>
                        </div>
                        <div>
                          <label className="text-xs text-gray-500 mb-1 block">설명</label>
                          <input type="text" value={editPrizeForm.description} onChange={(e) => setEditPrizeForm({ ...editPrizeForm, description: e.target.value })} placeholder="(선택)" className={inputCls} />
                        </div>
                        <div>
                          <label className="text-xs text-gray-500 mb-1 block">이미지</label>
                          <input
                            type="file"
                            accept="image/*"
                            onChange={(e) => {
                              const file = e.target.files?.[0] ?? null;
                              setEditPrizeImageFile(file);
                              if (file) {
                                const reader = new FileReader();
                                reader.onload = (ev) => setEditPrizeImagePreview(ev.target?.result as string);
                                reader.readAsDataURL(file);
                              }
                            }}
                            className="w-full text-xs text-gray-500 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-medium file:bg-forest-50 file:text-forest-600 hover:file:bg-forest-100"
                          />
                          {editPrizeImagePreview && (
                            <img src={editPrizeImagePreview} alt="미리보기" className="mt-2 w-16 h-16 object-cover rounded-lg border border-gray-200" />
                          )}
                        </div>
                        <div className="grid grid-cols-3 gap-3">
                          <div>
                            <label className="text-xs text-gray-500 mb-1 block">현금가치 (원)</label>
                            <input type="number" value={editPrizeForm.cashValue} onChange={(e) => setEditPrizeForm({ ...editPrizeForm, cashValue: e.target.value })} className={inputCls} />
                          </div>
                          <div>
                            <label className="text-xs text-gray-500 mb-1 block">남은 재고</label>
                            <input type="number" value={editPrizeForm.remainingStock} onChange={(e) => setEditPrizeForm({ ...editPrizeForm, remainingStock: e.target.value })} className={inputCls} />
                          </div>
                          <div>
                            <label className="text-xs text-gray-500 mb-1 block">EV 배율</label>
                            <input type="number" step="0.01" value={editPrizeForm.evMultiplier} onChange={(e) => setEditPrizeForm({ ...editPrizeForm, evMultiplier: e.target.value })} className={inputCls} />
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <input type="checkbox" id={`active-${p.id}`} checked={editPrizeForm.active} onChange={(e) => setEditPrizeForm({ ...editPrizeForm, active: e.target.checked })} className="w-4 h-4" />
                          <label htmlFor={`active-${p.id}`} className="text-sm text-gray-700">활성화</label>
                        </div>
                        <div className="flex gap-2 pt-1">
                          <button
                            onClick={async () => {
                              try {
                                await adminUpdatePrize(p.id, {
                                  name: editPrizeForm.name,
                                  description: editPrizeForm.description || undefined,
                                  cashValue: Number(editPrizeForm.cashValue),
                                  remainingStock: Number(editPrizeForm.remainingStock),
                                  tier: editPrizeForm.tier,
                                  evMultiplier: Number(editPrizeForm.evMultiplier),
                                  active: editPrizeForm.active,
                                }, editPrizeImageFile ?? undefined);
                                setPrizes([...(await adminListAllPrizes())].sort((a, b) => a.displayOrder - b.displayOrder));
                                setEditingPrizeId(null);
                                setEditPrizeImageFile(null);
                                setEditPrizeImagePreview(null);
                              } catch { alert("수정 실패"); }
                            }}
                            className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600"
                          >
                            저장
                          </button>
                          <button onClick={() => setEditingPrizeId(null)} className="px-4 py-2 border border-gray-300 text-gray-600 rounded-lg text-sm font-medium hover:bg-gray-50">취소</button>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        );
      })()}

      {/* Announce */}
      {tab === "announce" && (
        <div className="space-y-6 max-w-2xl">
          {/* 새 공지 작성 */}
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              if (!annTitle.trim() || !annContent.trim()) return;
              try {
                await adminCreateAnnouncement(annTitle.trim(), annContent.trim());
                const updated = await adminListAnnouncements();
                setAnnouncements(updated);
                setAnnTitle("");
                setAnnContent("");
              } catch { alert("공지 생성 실패"); }
            }}
            className="bg-white p-4 rounded-xl border space-y-3"
          >
            <h3 className="font-semibold text-sm">새 공지 작성</h3>
            <input
              type="text"
              value={annTitle}
              onChange={(e) => setAnnTitle(e.target.value)}
              placeholder="제목"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
              required
            />
            <textarea
              value={annContent}
              onChange={(e) => setAnnContent(e.target.value)}
              placeholder="내용"
              rows={4}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500 resize-none"
              required
            />
            <button
              type="submit"
              className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600 transition-colors"
            >
              저장 (미게시)
            </button>
          </form>

          {/* 공지 목록 */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-semibold text-sm">공지 목록</h3>
              {announcements.some((a) => a.active) && (
                <button
                  onClick={async () => {
                    if (!confirm("현재 공지를 내리겠습니까?")) return;
                    try {
                      await adminDeactivateAllAnnouncements();
                      setAnnouncements(await adminListAnnouncements());
                    } catch { alert("실패"); }
                  }}
                  className="text-xs px-3 py-1.5 border border-red-300 text-red-500 rounded-lg hover:bg-red-50"
                >
                  공지 내리기
                </button>
              )}
            </div>
            <div className="space-y-2">
              {announcements.map((a) => (
                <div
                  key={a.id}
                  className={`rounded-xl border-l-4 p-4 ${a.active ? "border-blue-500 bg-blue-50" : "border-gray-200 bg-white"}`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        {a.active && (
                          <span className="text-xs px-2 py-0.5 rounded-full bg-blue-500 text-white font-medium">현재 공지</span>
                        )}
                        <span className="font-semibold text-sm text-gray-800">{a.title}</span>
                      </div>
                      <p className="text-xs text-gray-500 mt-1 line-clamp-2">{a.content}</p>
                      <p className="text-xs text-gray-400 mt-1">{new Date(a.createdAt).toLocaleDateString("ko-KR")}</p>
                    </div>
                    <div className="flex gap-2 shrink-0">
                      {!a.active && (
                        <button
                          onClick={async () => {
                            try {
                              await adminActivateAnnouncement(a.id);
                              setAnnouncements(await adminListAnnouncements());
                            } catch { alert("게시 실패"); }
                          }}
                          className="text-xs px-3 py-1.5 bg-blue-500 text-white rounded-lg hover:bg-blue-600 font-medium"
                        >
                          게시
                        </button>
                      )}
                      <button
                        onClick={async () => {
                          if (!confirm("삭제하시겠습니까?")) return;
                          try {
                            await adminDeleteAnnouncement(a.id);
                            setAnnouncements(await adminListAnnouncements());
                          } catch { alert("삭제 실패"); }
                        }}
                        className="text-xs px-3 py-1.5 border border-red-300 text-red-500 rounded-lg hover:bg-red-50 font-medium"
                      >
                        삭제
                      </button>
                    </div>
                  </div>
                </div>
              ))}
              {announcements.length === 0 && (
                <div className="text-center text-gray-400 text-sm py-6 bg-white rounded-xl border">공지가 없습니다</div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Posts */}
      {tab === "posts" && (
        <PostsPanel categories={categories} />
      )}
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="bg-white rounded-xl border p-4 text-center">
      <div className="text-2xl font-bold text-gray-900">{value.toLocaleString()}</div>
      <div className="text-xs text-gray-400 mt-1">{label}</div>
    </div>
  );
}

function UsersPanel({
  users, setUsers, parties,
}: {
  users: AdminUser[];
  setUsers: React.Dispatch<React.SetStateAction<AdminUser[]>>;
  parties: AdminParty[];
}) {
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<{ nickname: string; name: string; email: string; role: "USER" | "ADMIN"; } | null>(null);
  const [keyword, setKeyword] = useState("");

  const filtered = users.filter((u) => {
    if (!keyword.trim()) return true;
    const k = keyword.toLowerCase();
    return (
      (u.nickname ?? "").toLowerCase().includes(k) ||
      (u.name ?? "").toLowerCase().includes(k) ||
      (u.email ?? "").toLowerCase().includes(k)
    );
  });

  const openEdit = (u: AdminUser) => {
    setEditingId(u.id);
    setForm({ nickname: u.nickname, name: u.name ?? "", email: u.email, role: u.role === "ADMIN" ? "ADMIN" : "USER" });
  };

  const save = async (u: AdminUser) => {
    if (!form) return;
    try {
      await updateAdminUser(u.id, {
        nickname: form.nickname,
        name: form.name,
        email: form.email,
        role: form.role,
      });
      setUsers((prev) => prev.map((x) => x.id === u.id ? { ...x, nickname: form.nickname, name: form.name, email: form.email, role: form.role } : x));
      setEditingId(null);
      setForm(null);
    } catch (e: any) {
      alert("수정 실패: " + (e?.message ?? ""));
    }
  };

  return (
    <div className="space-y-3">
      <input
        type="text"
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        placeholder="닉네임·실명·이메일로 검색"
        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
      />
      {filtered.map((u) => (
        <div key={u.id} className="bg-white rounded-lg border border-gray-200">
          <div className="flex items-center gap-3 px-4 py-3">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-medium text-sm">{u.nickname}</span>
                <span className="text-xs text-gray-500">{u.name}</span>
                <span className="text-xs text-gray-400">{u.email}</span>
                {u.role === "ADMIN" && <span className="text-[10px] bg-forest-100 text-forest-600 px-1.5 py-0.5 rounded">관리자</span>}
              </div>
              <div className="text-xs text-gray-400">
                {u.plantType ? PLANT_OPTIONS.find((p) => p.value === u.plantType)?.label : "미선택"}
                {u.partyName && ` | ${u.partyName}`}
                {` | 물방울: ${u.totalDrops.toLocaleString()}`}
              </div>
            </div>
            <select
              value={u.partyId ?? ""}
              onChange={async (e) => {
                const val = e.target.value ? Number(e.target.value) : null;
                try {
                  await updateAdminUser(u.id, { partyId: val });
                  setUsers((prev) => prev.map((uu) => uu.id === u.id ? { ...uu, partyId: val, partyName: parties.find((p) => p.id === val)?.name ?? null } : uu));
                } catch { alert("파티 변경 실패"); }
              }}
              className="px-2 py-1 border border-gray-300 rounded text-xs bg-white"
            >
              <option value="">파티없음</option>
              {parties.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
            <button
              onClick={() => editingId === u.id ? (setEditingId(null), setForm(null)) : openEdit(u)}
              className="px-2 py-1 text-xs text-forest-600 hover:text-forest-800 font-medium border border-forest-300 rounded hover:bg-forest-50 transition-colors"
            >
              {editingId === u.id ? "닫기" : "수정"}
            </button>
            <button
              onClick={async () => {
                if (!confirm(`"${u.nickname}" 유저의 비밀번호를 리셋하시겠습니까?`)) return;
                try {
                  const result = await resetAdminUserPassword(u.id);
                  alert(`임시 비밀번호: ${result.tempPassword}\n\n이 비밀번호를 해당 유저에게 전달해주세요.`);
                } catch { alert("비밀번호 리셋 실패"); }
              }}
              className="px-2 py-1 text-xs text-orange-600 hover:text-orange-800 font-medium border border-orange-300 rounded hover:bg-orange-50 transition-colors"
            >
              PW리셋
            </button>
            <button
              onClick={async () => {
                if (!confirm(`정말로 "${u.nickname}" 유저를 삭제하시겠습니까?\n\n이 유저의 모든 게시글·댓글·물방울 내역 등이 함께 삭제됩니다.`)) return;
                try {
                  await deleteAdminUser(u.id);
                  setUsers((prev) => prev.filter((x) => x.id !== u.id));
                  sessionStorage.removeItem("gridFeedCache");
                } catch { alert("삭제 실패"); }
              }}
              className="px-2 py-1 text-xs text-red-600 hover:text-red-800 font-medium border border-red-300 rounded hover:bg-red-50 transition-colors"
            >
              삭제
            </button>
          </div>
          {editingId === u.id && form && (
            <div className="border-t border-gray-200 px-4 py-4 space-y-3 bg-gray-50 rounded-b-lg">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-gray-500 mb-1 block">닉네임</label>
                  <input type="text" value={form.nickname} onChange={(e) => setForm({ ...form, nickname: e.target.value })} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm" />
                </div>
                <div>
                  <label className="text-xs text-gray-500 mb-1 block">실명</label>
                  <input type="text" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm" />
                </div>
                <div className="col-span-2">
                  <label className="text-xs text-gray-500 mb-1 block">메일주소</label>
                  <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm" />
                </div>
                <div>
                  <label className="text-xs text-gray-500 mb-1 block">권한</label>
                  <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as "USER" | "ADMIN" })} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white">
                    <option value="USER">일반</option>
                    <option value="ADMIN">관리자</option>
                  </select>
                </div>
              </div>
              <div className="text-[11px] text-gray-400">※ 물방울은 이 페이지에서 수정할 수 없습니다. 지급·차감은 "물방울" 탭을 이용하세요.</div>
              <div className="flex gap-2">
                <button onClick={() => save(u)} className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600">저장</button>
                <button onClick={() => { setEditingId(null); setForm(null); }} className="px-4 py-2 border border-gray-300 text-gray-600 rounded-lg text-sm font-medium hover:bg-gray-50">취소</button>
              </div>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function PostsPanel({ categories }: { categories: CategoryInfo[] }) {
  const [posts, setPosts] = useState<AdminPost[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [categoryFilter, setCategoryFilter] = useState("");
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<{ title: string; content: string; category: string; anonymous: boolean } | null>(null);

  const load = async (p = page) => {
    setLoading(true);
    try {
      const data = await adminListPosts({ page: p, size: 20, category: categoryFilter || undefined, keyword: keyword || undefined });
      setPosts(data.items);
      setTotalPages(data.totalPages);
      setPage(data.page);
    } catch {
      setPosts([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(0); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [categoryFilter]);

  const openEdit = (p: AdminPost) => {
    setEditingId(p.id);
    setEditForm({ title: p.title, content: p.content, category: p.category, anonymous: p.anonymous });
  };

  const save = async (p: AdminPost) => {
    if (!editForm) return;
    try {
      await adminUpdatePost(p.id, editForm);
      setPosts((prev) => prev.map((x) => x.id === p.id ? { ...x, ...editForm } : x));
      sessionStorage.removeItem("gridFeedCache");
      setEditingId(null);
      setEditForm(null);
    } catch { alert("수정 실패"); }
  };

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-2">
        <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)} className="px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white">
          <option value="">전체 게시판</option>
          {categories.map((c) => <option key={c.id} value={c.name}>{c.label}</option>)}
        </select>
        <input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => { if (e.key === "Enter") load(0); }}
          placeholder="제목·내용 검색"
          className="flex-1 min-w-[150px] px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
        />
        <button onClick={() => load(0)} className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600">검색</button>
      </div>

      {loading ? (
        <div className="text-center text-gray-400 py-8">불러오는 중...</div>
      ) : posts.length === 0 ? (
        <div className="text-center text-gray-400 text-sm py-8 bg-white rounded-xl border">게시글이 없습니다</div>
      ) : (
        <div className="space-y-2">
          {posts.map((p) => (
            <div key={p.id} className="bg-white rounded-lg border">
              <div className="flex items-start gap-3 px-4 py-3">
                {p.imageUrl && (
                  <img src={p.imageUrl.startsWith("http") ? p.imageUrl : (process.env.NEXT_PUBLIC_IMAGE_BASE_URL ?? "") + p.imageUrl} alt="" className="w-12 h-12 rounded-lg object-cover flex-shrink-0" />
                )}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-[10px] px-1.5 py-0.5 rounded bg-gray-100 text-gray-600">{p.category}</span>
                    <span className="font-medium text-sm truncate">{p.title}</span>
                    {p.anonymous && <span className="text-[10px] text-gray-400">[익명]</span>}
                  </div>
                  <p className="text-xs text-gray-500 line-clamp-2 mt-0.5">{p.content}</p>
                  <div className="text-[11px] text-gray-400 mt-1">
                    {p.authorNickname ?? "(삭제된 유저)"} {p.authorName && `(${p.authorName})`} · {new Date(p.createdAt).toLocaleString("ko-KR")}
                  </div>
                </div>
                <div className="flex flex-col gap-1 shrink-0">
                  <button
                    onClick={() => editingId === p.id ? (setEditingId(null), setEditForm(null)) : openEdit(p)}
                    className="px-2 py-1 text-xs text-forest-600 border border-forest-300 rounded hover:bg-forest-50"
                  >
                    {editingId === p.id ? "닫기" : "수정"}
                  </button>
                  <button
                    onClick={async () => {
                      if (!confirm(`"${p.title}" 게시글을 삭제하시겠습니까?`)) return;
                      try {
                        await adminDeletePost(p.id);
                        setPosts((prev) => prev.filter((x) => x.id !== p.id));
                        sessionStorage.removeItem("gridFeedCache");
                      } catch { alert("삭제 실패"); }
                    }}
                    className="px-2 py-1 text-xs text-red-600 border border-red-300 rounded hover:bg-red-50"
                  >
                    삭제
                  </button>
                </div>
              </div>
              {editingId === p.id && editForm && (
                <div className="border-t border-gray-200 px-4 py-4 space-y-3 bg-gray-50 rounded-b-lg">
                  <div>
                    <label className="text-xs text-gray-500 mb-1 block">제목</label>
                    <input type="text" value={editForm.title} onChange={(e) => setEditForm({ ...editForm, title: e.target.value })} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm" />
                  </div>
                  <div>
                    <label className="text-xs text-gray-500 mb-1 block">내용</label>
                    <textarea value={editForm.content} onChange={(e) => setEditForm({ ...editForm, content: e.target.value })} rows={5} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm resize-none" />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-xs text-gray-500 mb-1 block">게시판</label>
                      <select value={editForm.category} onChange={(e) => setEditForm({ ...editForm, category: e.target.value })} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white">
                        {categories.map((c) => <option key={c.id} value={c.name}>{c.label}</option>)}
                      </select>
                    </div>
                    <label className="flex items-end gap-2 text-sm text-gray-700 cursor-pointer">
                      <input type="checkbox" checked={editForm.anonymous} onChange={(e) => setEditForm({ ...editForm, anonymous: e.target.checked })} className="w-4 h-4 accent-forest-500 mb-2" />
                      <span className="mb-2">익명</span>
                    </label>
                  </div>
                  <div className="flex gap-2">
                    <button onClick={() => save(p)} className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600">저장</button>
                    <button onClick={() => { setEditingId(null); setEditForm(null); }} className="px-4 py-2 border border-gray-300 text-gray-600 rounded-lg text-sm font-medium hover:bg-gray-50">취소</button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex justify-center gap-2 pt-2">
          <button disabled={page === 0} onClick={() => load(page - 1)} className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm disabled:opacity-30">이전</button>
          <span className="px-3 py-1.5 text-sm text-gray-600">{page + 1} / {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => load(page + 1)} className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm disabled:opacity-30">다음</button>
        </div>
      )}
    </div>
  );
}
