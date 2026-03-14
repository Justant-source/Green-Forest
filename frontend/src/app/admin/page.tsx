"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import {
  AdminUser, AdminParty, AdminStats, Quest,
  CategoryInfo, CategoryRequestInfo,
} from "@/types";
import {
  getAdminCategories, createAdminCategory, deleteAdminCategory,
  getPendingCategoryRequests, approveCategoryRequest, rejectCategoryRequest,
  getAdminUsers, updateAdminUser,
  getAdminParties, createAdminParty, deleteAdminParty,
  getAdminStats, getQuests,
  createAdminQuest, deleteAdminQuest,
  awardDrops, deductDrops,
  createAnnouncement,
} from "@/lib/api";

type AdminTab = "dashboard" | "users" | "parties" | "quests" | "drops" | "categories" | "announce";

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

  useEffect(() => {
    if (!isLoggedIn || !isAdmin) {
      router.replace("/");
      return;
    }
    loadAllData();
  }, [isLoggedIn, isAdmin, router]);

  const loadAllData = async () => {
    try {
      const [s, u, p, q, cats, reqs] = await Promise.all([
        getAdminStats(),
        getAdminUsers(),
        getAdminParties(),
        getQuests(),
        getAdminCategories(),
        getPendingCategoryRequests(),
      ]);
      setStats(s);
      setUsers(u);
      setParties(p);
      setQuests(q);
      setCategories(cats);
      setRequests(reqs);
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
    { key: "parties", label: "파티" },
    { key: "quests", label: "퀘스트" },
    { key: "drops", label: "물방울" },
    { key: "categories", label: "게시판" },
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
        <div className="space-y-3">
          {users.map((u) => (
            <div key={u.id} className="flex items-center gap-3 px-4 py-3 bg-white rounded-lg border border-gray-200">
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-sm">{u.nickname}</span>
                  <span className="text-xs text-gray-400">{u.email}</span>
                  {u.role === "ADMIN" && <span className="text-[10px] bg-forest-100 text-forest-600 px-1.5 py-0.5 rounded">관리자</span>}
                </div>
                <div className="text-xs text-gray-400">
                  {u.plantType ? PLANT_OPTIONS.find((p) => p.value === u.plantType)?.label : "미선택"}
                  {u.partyName && ` | ${u.partyName}`}
                  {` | 물방울: ${u.totalDrops}`}
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
            </div>
          ))}
        </div>
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
            {users.map((u) => <option key={u.id} value={u.id}>{u.nickname} ({u.email})</option>)}
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

      {/* Announce */}
      {tab === "announce" && (
        <div className="max-w-lg space-y-4">
          <input
            type="text"
            value={annTitle}
            onChange={(e) => setAnnTitle(e.target.value)}
            placeholder="공지 제목"
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
          />
          <textarea
            value={annContent}
            onChange={(e) => setAnnContent(e.target.value)}
            placeholder="공지 내용"
            rows={4}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-forest-500"
          />
          <button
            onClick={async () => {
              if (!annTitle.trim() || !annContent.trim()) { alert("제목과 내용을 입력하세요."); return; }
              try {
                await createAnnouncement(annTitle.trim(), annContent.trim());
                alert("공지가 전송되었습니다.");
                setAnnTitle("");
                setAnnContent("");
              } catch { alert("공지 전송 실패"); }
            }}
            className="px-6 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600 transition-colors"
          >
            전체 공지 보내기
          </button>
        </div>
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
