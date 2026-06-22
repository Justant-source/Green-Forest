"use client";
import React, { useState, useEffect, useRef } from "react";
import { GachaPrizeInfo, AdminUser, SecretDrawResult } from "@/types";

const SPIN_STARS = Array.from({ length: 18 }, (_, i) => ({
  left: `${Math.round(i * 5.6)}%`,
  top: `${Math.round((i * 43 + 7) % 100)}%`,
  delay: `${(i * 0.11).toFixed(2)}s`,
  dur: `${(0.7 + (i % 5) * 0.15).toFixed(2)}s`,
  tx: `${(i % 2 === 0 ? 1 : -1) * (60 + (i * 31) % 160)}px`,
  ty: `${(i % 3 === 0 ? 1 : -1) * (60 + (i * 23) % 160)}px`,
  emoji: ["✨", "⭐", "💫", "🌟"][i % 4],
}));

const COINS = Array.from({ length: 28 }, (_, i) => ({
  left: `${Math.round(i * 3.6)}%`,
  dur: `${(1.3 + (i % 6) * 0.35).toFixed(2)}s`,
  delay: `${(i * 0.08).toFixed(2)}s`,
  emoji: ["💰", "🪙", "⭐", "💎", "✨", "🌟"][i % 6],
}));

const WIN_SPARKS = Array.from({ length: 24 }, (_, i) => ({
  angle: i * 15,
  dist: 100 + (i % 3) * 80,
  delay: `${(i * 0.04).toFixed(2)}s`,
  emoji: ["⭐", "✨", "💫", "🌟"][i % 4],
}));

type Phase = "confirm" | "spinning" | "result";

interface Props {
  prize: GachaPrizeInfo;
  members: AdminUser[];
  defaultSelectedIds: number[];
  onConfirm: (candidateUserIds: number[], count: number) => Promise<SecretDrawResult[]>;
  onClose: () => void;
}

const LS_KEY = "gf_secret_candidates";

const shuffleArr = (arr: string[]): string[] => {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
};

export default function SecretGachaModal({
  prize,
  members,
  defaultSelectedIds,
  onConfirm,
  onClose,
}: Props) {
  const [phase, setPhase] = useState<Phase>("confirm");
  const [selectedIds, setSelectedIds] = useState<number[]>(defaultSelectedIds);
  const [drawCount, setDrawCount] = useState(1);
  const [search, setSearch] = useState("");
  const [colNames, setColNames] = useState<string[]>([]);
  const [colLocked, setColLocked] = useState<boolean[]>([]);
  const [isSpinningFast, setIsSpinningFast] = useState(false);
  const [results, setResults] = useState<SecretDrawResult[]>([]);
  const [error, setError] = useState("");
  const spinRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const timersRef = useRef<ReturnType<typeof setTimeout>[]>([]);
  const winnerRef = useRef<SecretDrawResult[] | null>(null);
  const colLockedRef = useRef<boolean[]>([]);

  const cleanup = () => {
    timersRef.current.forEach(clearTimeout);
    timersRef.current = [];
    if (spinRef.current) {
      clearInterval(spinRef.current);
      spinRef.current = null;
    }
  };

  useEffect(() => () => cleanup(), []);

  const maxCount = Math.min(prize.remainingStock, Math.max(1, selectedIds.length));
  useEffect(() => {
    setDrawCount((c) => Math.min(c, Math.max(1, maxCount)));
  }, [maxCount]);

  const allNames = members.map((m) => m.name || m.nickname);

  const filtered = members.filter((m) => {
    if (!search) return true;
    const q = search.toLowerCase();
    return (
      (m.name || m.nickname).toLowerCase().includes(q) ||
      m.nickname.toLowerCase().includes(q)
    );
  });

  const toggleId = (id: number) =>
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );

  const handleDraw = async () => {
    if (selectedIds.length === 0) return;
    try { localStorage.setItem(LS_KEY, JSON.stringify(selectedIds)); } catch {}

    const n = drawCount;
    const initLocked = Array(n).fill(false);
    setColNames(Array(n).fill(""));
    setColLocked(initLocked);
    colLockedRef.current = [...initLocked];
    setPhase("spinning");
    setIsSpinningFast(true);

    const pool: string[] = [];
    for (let i = 0; i < 25; i++) pool.push(...shuffleArr(allNames));
    setColNames(Array(n).fill(pool[0]));

    spinRef.current = setInterval(() => {
      setColNames((prev) =>
        prev.map((name, i) =>
          colLockedRef.current[i] ? name : pool[Math.floor(Math.random() * pool.length)]
        )
      );
    }, 60);

    const startTime = Date.now();

    let apiResults: SecretDrawResult[];
    try {
      apiResults = await onConfirm(selectedIds, n);
      winnerRef.current = apiResults;
    } catch (e: any) {
      cleanup();
      let msg = "뽑기에 실패했습니다";
      try { if (e?.status === 400) msg = (await e.json())?.message ?? msg; } catch {}
      setError(msg);
      setPhase("confirm");
      return;
    }

    const waitMore = Math.max(0, 2500 - (Date.now() - startTime));

    const startDecel = () => {
      clearInterval(spinRef.current!);
      spinRef.current = null;
      setIsSpinningFast(false);

      const decelColumn = (colIdx: number, onDone: () => void) => {
        const winnerName = winnerRef.current![colIdx].winnerName;
        const others = shuffleArr(allNames).filter((name) => name !== winnerName).slice(0, 6);
        const seq = [...others, winnerName];
        const delays = [180, 280, 420, 600, 850, 1100];
        let step = 0;
        const doStep = () => {
          setColNames((prev) => { const next = [...prev]; next[colIdx] = seq[step]; return next; });
          if (step === seq.length - 1) {
            colLockedRef.current[colIdx] = true;
            setColLocked((prev) => { const next = [...prev]; next[colIdx] = true; return next; });
            const t = setTimeout(onDone, n > 1 ? 500 : 900);
            timersRef.current.push(t);
          } else {
            const delay = delays[step];
            step++;
            const t = setTimeout(doStep, delay);
            timersRef.current.push(t);
          }
        };
        doStep();
      };

      const decelAll = (i: number) => {
        if (i >= winnerRef.current!.length) {
          const t = setTimeout(() => { setResults(winnerRef.current!); setPhase("result"); }, n > 1 ? 400 : 0);
          timersRef.current.push(t);
          return;
        }
        decelColumn(i, () => decelAll(i + 1));
      };
      decelAll(0);
    };

    if (waitMore > 0) {
      const t = setTimeout(startDecel, waitMore);
      timersRef.current.push(t);
    } else {
      startDecel();
    }
  };

  // 열 수에 따른 크기
  const reelWidth =
    drawCount === 1 ? 340 : drawCount === 2 ? 180 : drawCount === 3 ? 150 :
    drawCount <= 5 ? 115 : 90;
  const reelFontSize =
    drawCount === 1 ? "3.2rem" : drawCount === 2 ? "2.2rem" :
    drawCount === 3 ? "1.7rem" : drawCount <= 5 ? "1.3rem" : "1rem";
  const arrowSize = drawCount <= 2 ? "1.8rem" : "1.3rem";
  const allLockedDone = colLocked.length > 0 && colLocked.every(Boolean);

  return (
    <>
      <style>{`
        @keyframes gf-spinBg {
          0%   { background-position: 0% 50%; }
          50%  { background-position: 100% 50%; }
          100% { background-position: 0% 50%; }
        }
        @keyframes gf-flashBorderSecret {
          0%   { box-shadow: 0 0 40px #d946ef, 0 0 80px #a855f7, 0 0 120px #d946ef; border-color: #d946ef; }
          20%  { box-shadow: 0 0 40px #ff0, 0 0 80px #f0f, 0 0 120px #ff0; border-color: #ff0; }
          40%  { box-shadow: 0 0 40px #0ff, 0 0 80px #00f, 0 0 120px #0ff; border-color: #0ff; }
          60%  { box-shadow: 0 0 40px #0f0, 0 0 80px #0ff, 0 0 120px #0f0; border-color: #0f0; }
          80%  { box-shadow: 0 0 40px #f55, 0 0 80px #ff4500, 0 0 120px #f55; border-color: #f55; }
          100% { box-shadow: 0 0 40px #d946ef, 0 0 80px #a855f7, 0 0 120px #d946ef; border-color: #d946ef; }
        }
        @keyframes gf-coin {
          0%   { transform: translateY(-60px) rotateY(0deg) scale(1); opacity: 1; }
          80%  { opacity: 1; }
          100% { transform: translateY(110vh) rotateY(720deg) scale(0.6); opacity: 0; }
        }
        @keyframes gf-spark {
          0%   { transform: translate(0,0) scale(1); opacity: 1; }
          100% { transform: translate(var(--gf-tx), var(--gf-ty)) scale(0); opacity: 0; }
        }
        @keyframes gf-starFloat {
          0%   { transform: translate(0,0) scale(0.5); opacity: 1; }
          100% { transform: translate(var(--gf-tx), var(--gf-ty)) scale(0); opacity: 0; }
        }
        @keyframes gf-goldPulse {
          0%,100% { text-shadow: 0 0 20px #ffd700, 0 0 40px #ffa500, 0 0 80px #ff8c00, 0 0 120px #ff4500; }
          50%     { text-shadow: 0 0 40px #fff7aa, 0 0 80px #ffd700, 0 0 140px #ffa500, 0 0 200px #ff4500; }
        }
        @keyframes gf-winBg {
          0%   { background: radial-gradient(ellipse at center, #000 0%, #000 100%); }
          25%  { background: radial-gradient(ellipse at center, #ffd700 0%, #ff8c00 30%, #1a0000 80%, #000 100%); }
          60%  { background: radial-gradient(ellipse at center, #fffbe0 0%, #ffd700 20%, #ff8c00 50%, #3d1a00 80%, #000 100%); }
          100% { background: radial-gradient(ellipse at center, #ffd700 0%, #ff8c00 25%, #8b4513 60%, #1a0800 100%); }
        }
        @keyframes gf-trophy {
          0%   { transform: scale(0) rotate(-30deg); opacity: 0; filter: drop-shadow(0 0 0px gold); }
          50%  { transform: scale(1.4) rotate(10deg); opacity: 1; filter: drop-shadow(0 0 40px gold); }
          70%  { transform: scale(0.9) rotate(-5deg); }
          100% { transform: scale(1) rotate(0deg); opacity: 1; filter: drop-shadow(0 0 25px gold); }
        }
        @keyframes gf-winText {
          0%   { transform: scale(0.3) translateY(40px); opacity: 0; }
          60%  { transform: scale(1.25) translateY(-8px); opacity: 1; }
          100% { transform: scale(1) translateY(0); opacity: 1; }
        }
        @keyframes gf-bounceLoop {
          0%,100% { transform: scale(1); }
          50%     { transform: scale(1.12); }
        }
        @keyframes gf-prizeCard {
          0%   { transform: translateY(30px) scale(0.85); opacity: 0; }
          70%  { transform: translateY(-5px) scale(1.03); opacity: 1; }
          100% { transform: translateY(0) scale(1); opacity: 1; }
        }
        @keyframes sf-nameFast {
          0%,100% { transform: scaleY(1); opacity: 1; }
          50%     { transform: scaleY(0.85); opacity: 0.7; }
        }
        @keyframes sf-namePulse {
          0%,100% { opacity: 1; transform: scale(1); }
          50%     { opacity: 0.8; transform: scale(1.04); }
        }
        @keyframes sf-nameLand {
          0%   { transform: scale(1.4) translateY(-12px); opacity: 0; }
          60%  { transform: scale(0.95) translateY(3px); opacity: 1; }
          100% { transform: scale(1) translateY(0); opacity: 1; }
        }
        @keyframes sf-reelGlow {
          0%,100% { text-shadow: 0 0 20px #d946ef, 0 0 40px #a855f7; }
          50%     { text-shadow: 0 0 40px #f0abfc, 0 0 80px #d946ef; }
        }
        @keyframes sf-winnerPulse {
          0%,100% { text-shadow: 0 0 30px #ffd700, 0 0 60px #ff8c00, 0 0 100px #ffa500; transform: scale(1); }
          50%     { text-shadow: 0 0 50px #fff7aa, 0 0 100px #ffd700, 0 0 150px #ff8c00; transform: scale(1.05); }
        }
        .sf-spin-bg {
          background: linear-gradient(270deg, #1a0533, #0a1a2e, #0d1a00, #1a0a00, #0a0a1a, #1a0533);
          background-size: 600% 600%;
          animation: gf-spinBg 1.5s ease infinite;
        }
        .sf-flash-border { animation: gf-flashBorderSecret 0.4s linear infinite; }
        .sf-gold-text    { animation: gf-goldPulse 1.2s ease-in-out infinite; }
        .sf-win-bg       { animation: gf-winBg 0.9s ease forwards; }
        .sf-trophy       { animation: gf-trophy 0.8s cubic-bezier(0.175,0.885,0.32,1.275) forwards; }
        .sf-win-text     { animation: gf-winText 0.7s cubic-bezier(0.175,0.885,0.32,1.275) forwards; }
        .sf-bounce-loop  { animation: gf-bounceLoop 0.7s ease-in-out infinite; }
        .sf-prize-card   { animation: gf-prizeCard 0.5s cubic-bezier(0.175,0.885,0.32,1.275) forwards; }
        .sf-name-fast    { animation: sf-nameFast 0.12s ease-in-out infinite; }
        .sf-name-decel   { animation: sf-namePulse 0.5s ease-in-out; }
        .sf-name-land    { animation: sf-nameLand 0.6s cubic-bezier(0.175,0.885,0.32,1.275) forwards; }
        .sf-name-winner  { animation: sf-winnerPulse 1s ease-in-out infinite; }
        .sf-reel-glow    { animation: sf-reelGlow 0.8s ease-in-out infinite; }
      `}</style>

      <div className="fixed inset-0 z-50 overflow-hidden">

        {/* ══════════════ CONFIRM (후보 선택) ══════════════ */}
        {phase === "confirm" && (
          <div className="absolute inset-0 flex items-center justify-center bg-black/80 backdrop-blur-sm px-4">
            <div
              className="relative w-full max-w-sm rounded-3xl p-5 text-white flex flex-col"
              style={{
                maxHeight: "90vh",
                background: "linear-gradient(145deg, #1a1a2e, #16213e, #0f3460)",
                border: "2px solid #d946ef",
                boxShadow: "0 0 40px rgba(217,70,239,0.35), inset 0 0 20px rgba(217,70,239,0.07)",
              }}
            >
              {["top-2 left-2", "top-2 right-2", "bottom-2 left-2", "bottom-2 right-2"].map((pos, i) => (
                <div key={i} className={`absolute ${pos} text-fuchsia-400 text-lg opacity-60`}>✦</div>
              ))}

              <h2
                className="text-center text-xl font-black mb-0.5 tracking-widest"
                style={{ color: "#d946ef", textShadow: "0 0 15px rgba(217,70,239,0.7)" }}
              >
                몰래 뽑기
              </h2>
              <div className="text-center text-xs text-fuchsia-500/50 mb-1">— SECRET DRAW —</div>
              <div
                className="text-center text-sm mb-3 px-3 py-1.5 rounded-xl"
                style={{ background: "rgba(217,70,239,0.1)", border: "1px solid rgba(217,70,239,0.2)" }}
              >
                <span className="text-yellow-300 font-bold">{prize.name}</span>
                <span className="text-white/50 text-xs ml-2">재고 {prize.remainingStock}개</span>
              </div>

              {error && (
                <p className="text-red-400 text-xs text-center mb-2 bg-red-900/30 rounded-xl py-2 px-3">{error}</p>
              )}

              {/* 검색 + 전체/초기화 */}
              <div className="flex gap-2 mb-2">
                <input
                  type="text"
                  placeholder="이름 검색..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="flex-1 px-3 py-1.5 rounded-lg text-sm text-gray-900 bg-white/90 focus:outline-none focus:ring-2 focus:ring-fuchsia-400"
                />
                <button
                  onClick={() => setSelectedIds(members.map((m) => m.id))}
                  className="text-xs px-2.5 py-1.5 rounded-lg font-semibold"
                  style={{ background: "rgba(217,70,239,0.25)", border: "1px solid rgba(217,70,239,0.4)", color: "#f0abfc" }}
                >
                  전체
                </button>
                <button
                  onClick={() => setSelectedIds([])}
                  className="text-xs px-2.5 py-1.5 rounded-lg font-semibold"
                  style={{ background: "rgba(255,255,255,0.07)", border: "1px solid rgba(255,255,255,0.15)", color: "#9ca3af" }}
                >
                  초기화
                </button>
              </div>

              {/* 회원 목록 */}
              <div
                className="flex-1 overflow-y-auto rounded-xl mb-3"
                style={{
                  minHeight: 0,
                  maxHeight: 220,
                  background: "rgba(0,0,0,0.35)",
                  border: "1px solid rgba(217,70,239,0.2)",
                }}
              >
                {filtered.length === 0 ? (
                  <div className="text-center text-gray-500 py-6 text-sm">검색 결과 없음</div>
                ) : (
                  filtered.map((m) => (
                    <label
                      key={m.id}
                      className="flex items-center gap-3 px-3 py-2.5 cursor-pointer hover:bg-white/5 border-b border-white/5 last:border-0 select-none"
                    >
                      <input
                        type="checkbox"
                        checked={selectedIds.includes(m.id)}
                        onChange={() => toggleId(m.id)}
                        className="w-4 h-4 rounded accent-fuchsia-500 flex-shrink-0"
                      />
                      <div className="min-w-0">
                        <div className="text-sm font-semibold text-white truncate">{m.name || m.nickname}</div>
                        <div className="text-xs text-gray-400 truncate">{m.nickname}</div>
                      </div>
                    </label>
                  ))
                )}
              </div>

              {/* 당첨 인원 선택 */}
              <div
                className="flex items-center justify-between gap-2 mb-3 px-3 py-2 rounded-xl"
                style={{ background: "rgba(217,70,239,0.1)", border: "1px solid rgba(217,70,239,0.2)" }}
              >
                <span className="text-xs text-fuchsia-300 font-semibold shrink-0">당첨 인원</span>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setDrawCount((c) => Math.max(1, c - 1))}
                    disabled={drawCount <= 1}
                    className="w-7 h-7 rounded-full text-base font-black flex items-center justify-center transition"
                    style={{
                      background: drawCount > 1 ? "rgba(217,70,239,0.35)" : "rgba(255,255,255,0.05)",
                      border: "1px solid rgba(217,70,239,0.35)",
                      color: drawCount > 1 ? "#f0abfc" : "#4b5563",
                    }}
                  >-</button>
                  <span className="text-xl font-black text-white w-8 text-center">{drawCount}</span>
                  <button
                    onClick={() => setDrawCount((c) => Math.min(maxCount, c + 1))}
                    disabled={drawCount >= maxCount}
                    className="w-7 h-7 rounded-full text-base font-black flex items-center justify-center transition"
                    style={{
                      background: drawCount < maxCount ? "rgba(217,70,239,0.35)" : "rgba(255,255,255,0.05)",
                      border: "1px solid rgba(217,70,239,0.35)",
                      color: drawCount < maxCount ? "#f0abfc" : "#4b5563",
                    }}
                  >+</button>
                </div>
                <span className="text-xs text-fuchsia-500/60 shrink-0">최대 {maxCount}명</span>
              </div>

              {/* 선택 수 + 버튼 */}
              <div className="flex items-center gap-2">
                <div
                  className="text-xs px-3 py-1.5 rounded-full flex-1 text-center font-semibold"
                  style={{ background: "rgba(217,70,239,0.15)", color: "#f0abfc" }}
                >
                  {selectedIds.length}명 중 {drawCount}명 뽑기
                </div>
                <button
                  onClick={onClose}
                  className="px-4 py-2 rounded-xl text-sm font-semibold text-gray-400 transition"
                  style={{ background: "rgba(255,255,255,0.07)", border: "1px solid rgba(255,255,255,0.15)" }}
                >
                  취소
                </button>
                <button
                  onClick={handleDraw}
                  disabled={selectedIds.length === 0}
                  className={`px-5 py-2 rounded-xl text-sm font-black tracking-wide transition ${
                    selectedIds.length > 0 ? "text-black hover:scale-105" : "text-gray-500 cursor-not-allowed"
                  }`}
                  style={
                    selectedIds.length > 0
                      ? { background: "linear-gradient(135deg, #d946ef, #a855f7)", boxShadow: "0 4px 20px rgba(217,70,239,0.6)" }
                      : { background: "rgba(255,255,255,0.05)" }
                  }
                >
                  뽑기!
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ══════════════ SPINNING (N열 룰렛) ══════════════ */}
        {phase === "spinning" && (
          <div className="sf-spin-bg absolute inset-0 flex flex-col items-center justify-center px-2">

            <div
              className="sf-flash-border pointer-events-none absolute"
              style={{ inset: 8, borderRadius: 24, border: "4px solid #d946ef" }}
            />

            {SPIN_STARS.map((s, i) => (
              <div
                key={i}
                className="absolute pointer-events-none text-2xl"
                style={{
                  left: s.left, top: s.top,
                  animation: `gf-starFloat ${s.dur} ease-out infinite`,
                  animationDelay: s.delay,
                  ["--gf-tx" as string]: s.tx,
                  ["--gf-ty" as string]: s.ty,
                }}
              >{s.emoji}</div>
            ))}

            <div
              className="text-3xl font-black mb-6 tracking-widest"
              style={{ color: "#d946ef", textShadow: "0 0 20px #d946ef, 0 0 40px #a855f7", animation: "gf-bounceLoop 0.6s ease-in-out infinite" }}
            >
              SECRET DRAW
            </div>

            {/* N열 룰렛 박스 */}
            <div className="flex gap-2 mb-6 justify-center flex-wrap" style={{ maxWidth: "96vw" }}>
              {Array.from({ length: drawCount }, (_, i) => {
                const isLocked = colLocked[i] ?? false;
                const name = colNames[i] ?? "";
                const nameFontSize =
                  name.length > 5 ? `calc(${reelFontSize} * 0.72)` :
                  name.length > 3 ? `calc(${reelFontSize} * 0.86)` : reelFontSize;
                return (
                  <div
                    key={i}
                    className="relative flex items-center justify-center flex-shrink-0"
                    style={{
                      width: reelWidth,
                      height: 130,
                      borderRadius: 20,
                      border: isLocked ? "4px solid #ffd700" : "4px solid #d946ef",
                      boxShadow: isLocked
                        ? "0 0 60px #ffd700, 0 0 120px rgba(255,215,0,0.4), inset 0 0 30px rgba(255,215,0,0.15)"
                        : "0 0 50px #d946ef, 0 0 100px rgba(217,70,239,0.3), inset 0 0 25px rgba(217,70,239,0.12)",
                      background: "rgba(0,0,0,0.75)",
                      overflow: "hidden",
                      transition: "border-color 0.5s, box-shadow 0.5s",
                    }}
                  >
                    {/* 번호 뱃지 */}
                    {drawCount > 1 && (
                      <div
                        className="absolute top-1.5 left-2 text-xs font-black pointer-events-none z-10"
                        style={{ color: isLocked ? "rgba(255,215,0,0.7)" : "rgba(217,70,239,0.6)" }}
                      >{i + 1}</div>
                    )}

                    {/* 스피드 라인 */}
                    {isSpinningFast && !isLocked && (
                      <div className="absolute inset-0 pointer-events-none overflow-hidden">
                        {[...Array(6)].map((_, li) => (
                          <div key={li} className="absolute" style={{
                            left: `${8 + li * 15}%`, top: 0, width: 1.5, height: "100%",
                            background: `linear-gradient(to bottom, transparent, rgba(217,70,239,${0.28 - li * 0.03}), transparent)`,
                            animation: `sf-nameFast ${0.18 + li * 0.04}s ease-in-out infinite`,
                            animationDelay: `${li * 0.02}s`,
                          }} />
                        ))}
                      </div>
                    )}

                    {/* 포인터 */}
                    <div className="absolute pointer-events-none" style={{ left: 4, fontSize: arrowSize, color: isLocked ? "#ffd700" : "#d946ef", textShadow: isLocked ? "0 0 12px #ffd700" : "0 0 8px #d946ef", transition: "color 0.5s" }}>▶</div>
                    <div className="absolute pointer-events-none" style={{ right: 4, fontSize: arrowSize, color: isLocked ? "#ffd700" : "#d946ef", textShadow: isLocked ? "0 0 12px #ffd700" : "0 0 8px #d946ef", transition: "color 0.5s" }}>◀</div>

                    {/* 이름 */}
                    <div
                      key={name + String(isLocked)}
                      className={isLocked ? "sf-name-land sf-name-winner" : isSpinningFast ? "sf-name-fast sf-reel-glow" : "sf-name-decel sf-reel-glow"}
                      style={{
                        fontSize: nameFontSize,
                        fontWeight: 900,
                        color: isLocked ? "#ffd700" : "#ffffff",
                        letterSpacing: "0.06em",
                        textAlign: "center",
                        padding: `0 ${drawCount === 1 ? 50 : 22}px`,
                        transition: "color 0.5s",
                        userSelect: "none",
                        overflow: "hidden",
                        maxWidth: "100%",
                      }}
                    >{name}</div>

                    <div className="absolute inset-x-0 top-0 h-8 pointer-events-none" style={{ background: "linear-gradient(to bottom, rgba(0,0,0,0.8), transparent)" }} />
                    <div className="absolute inset-x-0 bottom-0 h-8 pointer-events-none" style={{ background: "linear-gradient(to top, rgba(0,0,0,0.8), transparent)" }} />
                  </div>
                );
              })}
            </div>

            <div
              className="text-sm font-bold tracking-widest px-6 py-2 rounded-full"
              style={{
                color: allLockedDone ? "#ffd700" : "rgba(217,70,239,0.8)",
                background: allLockedDone ? "rgba(255,215,0,0.12)" : "rgba(217,70,239,0.12)",
                border: allLockedDone ? "1px solid rgba(255,215,0,0.3)" : "1px solid rgba(217,70,239,0.3)",
                transition: "all 0.5s",
              }}
            >
              {allLockedDone ? "당첨자 확정!" : isSpinningFast ? "추첨 중..." : "결정 중..."}
            </div>
          </div>
        )}

        {/* ══════════════ RESULT (당첨 결과) ══════════════ */}
        {phase === "result" && results.length > 0 && (
          <div className="sf-win-bg absolute inset-0 flex flex-col items-center justify-center overflow-hidden">

            {COINS.map((c, i) => (
              <div key={i} className="absolute pointer-events-none"
                style={{ left: c.left, top: "-60px", fontSize: "1.8rem", animation: `gf-coin ${c.dur} linear infinite`, animationDelay: c.delay }}>
                {c.emoji}
              </div>
            ))}

            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              {WIN_SPARKS.map((sp, i) => {
                const rad = (sp.angle * Math.PI) / 180;
                const tx = Math.round(Math.cos(rad) * sp.dist);
                const ty = Math.round(Math.sin(rad) * sp.dist);
                return (
                  <div key={i} className="absolute text-xl"
                    style={{ animation: `gf-spark 0.8s ease-out infinite`, animationDelay: sp.delay, ["--gf-tx" as string]: `${tx}px`, ["--gf-ty" as string]: `${ty}px` }}>
                    {sp.emoji}
                  </div>
                );
              })}
            </div>

            <div className="sf-trophy mb-2 relative z-10" style={{ fontSize: results.length === 1 ? "6rem" : "4rem", lineHeight: 1 }}>
              🏆
            </div>

            {results.length === 1 ? (
              <>
                <div className="sf-win-text relative z-10 mb-1 text-center px-4"
                  style={{ fontSize: "clamp(2rem, 9vw, 3.5rem)", fontWeight: 900, color: "#ffd700",
                    textShadow: "0 0 30px #ffd700, 0 0 60px #ff8c00, 0 0 100px #ffa500", letterSpacing: "0.04em" }}>
                  {results[0].winnerName} 님
                </div>
                <div className="sf-win-text sf-gold-text font-black relative z-10 mb-5"
                  style={{ fontSize: "clamp(2.5rem, 11vw, 4.5rem)", color: "#ffd700",
                    WebkitTextStroke: "2px #b8860b", letterSpacing: "0.18em", animationDelay: "0.15s" }}>
                  당 첨 !!!
                </div>
              </>
            ) : (
              <>
                <div className="sf-win-text sf-gold-text font-black relative z-10 mb-3"
                  style={{ fontSize: "clamp(1.8rem, 7vw, 3rem)", color: "#ffd700",
                    WebkitTextStroke: "1.5px #b8860b", letterSpacing: "0.14em" }}>
                  당 첨 !!!
                </div>
                <div className="relative z-10 mb-4 w-full px-6" style={{ maxHeight: "35vh", overflowY: "auto" }}>
                  {results.map((r, i) => (
                    <div key={i} className="sf-win-text flex items-center gap-3 mb-2 px-4 py-2 rounded-2xl"
                      style={{ background: "rgba(0,0,0,0.55)", border: "1px solid rgba(255,215,0,0.4)", animationDelay: `${i * 0.08}s` }}>
                      <span style={{ color: "rgba(255,215,0,0.55)", fontSize: "0.85rem", fontWeight: 700, minWidth: 20 }}>{i + 1}.</span>
                      <span style={{ color: "#ffd700", fontWeight: 900, fontSize: "1.2rem" }}>{r.winnerName} 님</span>
                    </div>
                  ))}
                </div>
              </>
            )}

            <div className="sf-prize-card relative z-10 rounded-3xl px-8 py-4 mb-6 text-center"
              style={{ background: "rgba(0,0,0,0.6)", border: "2px solid #ffd700",
                boxShadow: "0 0 30px rgba(255,215,0,0.5)", backdropFilter: "blur(8px)" }}>
              <div className="text-white font-black text-xl">{results[0].prizeName}</div>
              {results.length > 1 && (
                <div className="text-yellow-400 text-sm mt-0.5">{results.length}명 당첨</div>
              )}
              <div className="text-yellow-400 text-sm mt-1">관리자가 직접 전달합니다</div>
              <div className="text-fuchsia-300 text-xs mt-1.5 px-3 py-1 rounded-full inline-block"
                style={{ background: "rgba(217,70,239,0.15)" }}>
                몰래뽑기 이벤트
              </div>
            </div>

            <button onClick={onClose}
              className="sf-bounce-loop relative z-10 font-black text-black text-xl px-12 py-4 rounded-2xl tracking-wide"
              style={{ background: "linear-gradient(135deg, #ffd700, #ff8c00)", boxShadow: "0 0 40px rgba(255,215,0,0.8)" }}>
              확인
            </button>
          </div>
        )}
      </div>
    </>
  );
}
