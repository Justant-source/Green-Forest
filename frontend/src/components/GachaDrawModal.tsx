"use client";
import React, { useState, useEffect, useRef } from "react";
import { GachaDrawResult, GachaPrizeInfo } from "@/types";

interface Props {
  prize: GachaPrizeInfo | null;
  onConfirm: () => Promise<GachaDrawResult>;
  onClose: () => void;
}

const SYMBOLS = ["💎", "🌟", "🔥", "⚡", "👑", "🏆", "💰", "🍀", "🎯", "✨"];

// 파티클 위치를 렌더 사이에 안정적으로 유지하기 위해 상수로 정의
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

export default function GachaDrawModal({ prize, onConfirm, onClose }: Props) {
  const [phase, setPhase] = useState<Phase>("confirm");
  const [result, setResult] = useState<GachaDrawResult | null>(null);
  const [reelSymbols, setReelSymbols] = useState<[number, number, number]>([0, 1, 2]);
  const [stopped, setStopped] = useState<[boolean, boolean, boolean]>([false, false, false]);
  const [error, setError] = useState("");
  const timersRef = useRef<ReturnType<typeof setTimeout>[]>([]);
  const spinRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const stoppedRef = useRef<[boolean, boolean, boolean]>([false, false, false]);

  const cleanup = () => {
    timersRef.current.forEach(clearTimeout);
    timersRef.current = [];
    if (spinRef.current) { clearInterval(spinRef.current); spinRef.current = null; }
  };

  useEffect(() => () => cleanup(), []);

  const handleDraw = async () => {
    setPhase("spinning");
    const newStopped: [boolean, boolean, boolean] = [false, false, false];
    setStopped(newStopped);
    stoppedRef.current = newStopped;

    // 세 릴 동시 스핀
    spinRef.current = setInterval(() => {
      setReelSymbols(prev => {
        const next: [number, number, number] = [...prev] as [number, number, number];
        if (!stoppedRef.current[0]) next[0] = Math.floor(Math.random() * SYMBOLS.length);
        if (!stoppedRef.current[1]) next[1] = Math.floor(Math.random() * SYMBOLS.length);
        if (!stoppedRef.current[2]) next[2] = Math.floor(Math.random() * SYMBOLS.length);
        return next;
      });
    }, 80);

    let apiResult: GachaDrawResult;
    try {
      apiResult = await onConfirm();
    } catch (e: any) {
      cleanup();
      let msg = "오류가 발생했습니다";
      try {
        if (e?.status === 400) msg = (await e.json())?.message ?? msg;
      } catch {}
      setError(msg);
      setPhase("confirm");
      return;
    }

    // 릴을 순차적으로 정지
    const stop = (idx: 0 | 1 | 2) => {
      stoppedRef.current = [...stoppedRef.current] as [boolean, boolean, boolean];
      stoppedRef.current[idx] = true;
      setStopped([...stoppedRef.current] as [boolean, boolean, boolean]);
    };

    const t1 = setTimeout(() => stop(0), 1800);
    const t2 = setTimeout(() => stop(1), 2500);
    const t3 = setTimeout(() => {
      cleanup();
      stop(2);
      const t4 = setTimeout(() => {
        setResult(apiResult);
        setPhase("result");
      }, 500);
      timersRef.current.push(t4);
    }, 3200);

    timersRef.current = [t1, t2, t3];
  };

  if (!prize) return null;

  return (
    <>
      <style>{`
        @keyframes gf-spinBg {
          0%   { background-position: 0% 50%; }
          50%  { background-position: 100% 50%; }
          100% { background-position: 0% 50%; }
        }
        @keyframes gf-flashBorder {
          0%   { box-shadow: 0 0 40px #ff0, 0 0 80px #f0f, 0 0 120px #ff0; border-color: #ff0; }
          20%  { box-shadow: 0 0 40px #0ff, 0 0 80px #00f, 0 0 120px #0ff; border-color: #0ff; }
          40%  { box-shadow: 0 0 40px #f00, 0 0 80px #ff4500, 0 0 120px #f00; border-color: #f55; }
          60%  { box-shadow: 0 0 40px #0f0, 0 0 80px #0ff, 0 0 120px #0f0; border-color: #0f0; }
          80%  { box-shadow: 0 0 40px #f0f, 0 0 80px #ff0, 0 0 120px #f0f; border-color: #f0f; }
          100% { box-shadow: 0 0 40px #ff0, 0 0 80px #f0f, 0 0 120px #ff0; border-color: #ff0; }
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
        @keyframes gf-shake {
          0%,100% { transform: translateX(0) rotate(0deg); }
          10%     { transform: translateX(-12px) rotate(-3deg); }
          20%     { transform: translateX(12px) rotate(3deg); }
          30%     { transform: translateX(-10px) rotate(-2deg); }
          40%     { transform: translateX(10px) rotate(2deg); }
          50%     { transform: translateX(-8px) rotate(-1deg); }
          60%     { transform: translateX(8px) rotate(1deg); }
          70%     { transform: translateX(-5px); }
          80%     { transform: translateX(5px); }
          90%     { transform: translateX(-2px); }
        }
        @keyframes gf-bounceLoop {
          0%,100% { transform: scale(1); }
          50%     { transform: scale(1.12); }
        }
        @keyframes gf-rotate {
          from { transform: rotate(0deg); }
          to   { transform: rotate(360deg); }
        }
        @keyframes gf-reelBounce {
          0%,100% { transform: scaleY(1); }
          50%     { transform: scaleY(1.08); }
        }
        @keyframes gf-prizeCard {
          0%   { transform: translateY(30px) scale(0.85); opacity: 0; }
          70%  { transform: translateY(-5px) scale(1.03); opacity: 1; }
          100% { transform: translateY(0) scale(1); opacity: 1; }
        }
        @keyframes gf-missText {
          0%   { transform: scale(0.5); opacity: 0; }
          60%  { transform: scale(1.15); opacity: 1; }
          100% { transform: scale(1); opacity: 1; }
        }
        @keyframes gf-bgPulse {
          0%,100% { background-color: #0a0a1a; }
          50%     { background-color: #150a2e; }
        }
        .gf-spin-bg {
          background: linear-gradient(270deg, #1a0533, #0a1a2e, #0d1a00, #1a0a00, #0a0a1a, #1a0533);
          background-size: 600% 600%;
          animation: gf-spinBg 1.5s ease infinite;
        }
        .gf-flash-border { animation: gf-flashBorder 0.35s linear infinite; }
        .gf-gold-text    { animation: gf-goldPulse 1.2s ease-in-out infinite; }
        .gf-win-bg       { animation: gf-winBg 0.9s ease forwards; }
        .gf-trophy       { animation: gf-trophy 0.8s cubic-bezier(0.175,0.885,0.32,1.275) forwards; }
        .gf-win-text     { animation: gf-winText 0.7s cubic-bezier(0.175,0.885,0.32,1.275) forwards; }
        .gf-shake        { animation: gf-shake 0.6s ease; }
        .gf-bounce-loop  { animation: gf-bounceLoop 0.7s ease-in-out infinite; }
        .gf-prize-card   { animation: gf-prizeCard 0.5s cubic-bezier(0.175,0.885,0.32,1.275) forwards; }
        .gf-miss-text    { animation: gf-missText 0.5s cubic-bezier(0.175,0.885,0.32,1.275) forwards; }
        .gf-miss-bg      { animation: gf-bgPulse 1s ease-in-out 2; }
      `}</style>

      <div className="fixed inset-0 z-50 overflow-hidden">

        {/* ───────────── CONFIRM ───────────── */}
        {phase === "confirm" && (
          <div className="absolute inset-0 flex items-center justify-center bg-black/75 backdrop-blur-sm px-4">
            <div
              className="relative w-full max-w-sm rounded-3xl p-7 text-white"
              style={{
                background: "linear-gradient(145deg, #1a1a2e, #16213e, #0f3460)",
                border: "2px solid #ffd700",
                boxShadow: "0 0 40px rgba(255,215,0,0.3), inset 0 0 20px rgba(255,215,0,0.05)",
              }}
            >
              {/* 코너 장식 */}
              {["top-2 left-2", "top-2 right-2", "bottom-2 left-2", "bottom-2 right-2"].map((pos, i) => (
                <div key={i} className={`absolute ${pos} text-yellow-400 text-lg opacity-60`}>✦</div>
              ))}

              <h2
                className="text-center text-2xl font-black mb-1 tracking-widest"
                style={{ color: "#ffd700", textShadow: "0 0 15px rgba(255,215,0,0.6)" }}
              >
                🎰 뽑기 확인
              </h2>
              <div className="text-center text-xs text-yellow-500/60 mb-5">— GACHA MACHINE —</div>

              <div
                className="rounded-2xl p-4 mb-4 text-center"
                style={{ background: "rgba(255,215,0,0.08)", border: "1px solid rgba(255,215,0,0.2)" }}
              >
                <p className="font-bold text-white text-lg">{prize.name}</p>
                <p className="text-yellow-400 text-sm mt-1">💧 30 물방울 차감</p>
                <p className="text-yellow-500/80 text-xs mt-1">
                  현재 당첨 확률: <strong>{(prize.currentProbability * 100).toFixed(2)}%</strong>
                </p>
              </div>

              {error && (
                <p className="text-red-400 text-sm text-center mb-3 bg-red-900/30 rounded-xl py-2">{error}</p>
              )}

              <div className="flex gap-3">
                <button
                  onClick={onClose}
                  className="flex-1 py-3 rounded-xl font-semibold text-gray-400 transition"
                  style={{ background: "rgba(255,255,255,0.07)", border: "1px solid rgba(255,255,255,0.15)" }}
                >
                  취소
                </button>
                <button
                  onClick={handleDraw}
                  className="flex-1 py-3 rounded-xl font-black text-black text-lg tracking-wide transition hover:scale-105"
                  style={{
                    background: "linear-gradient(135deg, #ffd700, #ff8c00)",
                    boxShadow: "0 4px 20px rgba(255,140,0,0.5)",
                  }}
                >
                  🎰 뽑기!
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ───────────── SPINNING ───────────── */}
        {phase === "spinning" && (
          <div className="gf-spin-bg absolute inset-0 flex flex-col items-center justify-center">

            {/* 전체화면 플래시 보더 */}
            <div
              className="gf-flash-border pointer-events-none absolute"
              style={{ inset: 8, borderRadius: 24, border: "4px solid #ffd700" }}
            />

            {/* 배경 파티클 */}
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
              >
                {s.emoji}
              </div>
            ))}

            {/* 타이틀 */}
            <div
              className="text-4xl font-black mb-2 tracking-widest"
              style={{
                color: "#ffd700",
                textShadow: "0 0 20px #ffd700, 0 0 40px #ff8c00",
                animation: "gf-bounceLoop 0.5s ease-in-out infinite",
              }}
            >
              🎰 PULLING... 🎰
            </div>
            <div className="text-yellow-500/60 text-sm mb-8 tracking-widest">운명의 릴이 돌아갑니다</div>

            {/* 슬롯 머신 3릴 */}
            <div className="flex gap-5 mb-10">
              {([0, 1, 2] as const).map((col) => (
                <div
                  key={col}
                  className="relative flex flex-col items-center"
                >
                  {/* 릴 창 */}
                  <div
                    className="w-28 h-28 rounded-2xl flex items-center justify-center overflow-hidden"
                    style={{
                      background: "rgba(0,0,0,0.7)",
                      border: stopped[col]
                        ? "3px solid #ffd700"
                        : "3px solid #a855f7",
                      boxShadow: stopped[col]
                        ? "0 0 25px #ffd700, inset 0 0 15px rgba(255,215,0,0.2)"
                        : "0 0 20px #a855f7, inset 0 0 10px rgba(168,85,247,0.2)",
                      transition: "border-color 0.3s, box-shadow 0.3s",
                    }}
                  >
                    <span
                      className="text-6xl"
                      style={{
                        animation: stopped[col]
                          ? undefined
                          : "gf-reelBounce 0.16s ease-in-out infinite",
                        filter: stopped[col]
                          ? "drop-shadow(0 0 12px gold)"
                          : undefined,
                        transition: "filter 0.3s",
                      }}
                    >
                      {SYMBOLS[reelSymbols[col]]}
                    </span>
                  </div>
                  {/* 정지 표시 */}
                  <div
                    className="mt-2 text-xs font-bold tracking-wider"
                    style={{ color: stopped[col] ? "#ffd700" : "#6b21a8", transition: "color 0.3s" }}
                  >
                    {stopped[col] ? "■ STOP" : "▶ SPIN"}
                  </div>
                </div>
              ))}
            </div>

            {/* 진행 도트 */}
            <div className="flex gap-3">
              {([0, 1, 2] as const).map((i) => (
                <div
                  key={i}
                  className="rounded-full transition-all duration-300"
                  style={{
                    width: stopped[i] ? 20 : 14,
                    height: stopped[i] ? 20 : 14,
                    background: stopped[i] ? "#ffd700" : "#4b5563",
                    boxShadow: stopped[i] ? "0 0 10px #ffd700" : undefined,
                  }}
                />
              ))}
            </div>
          </div>
        )}

        {/* ───────────── RESULT ───────────── */}
        {phase === "result" && result && (
          result.isWinner ? (

            /* ===== WIN ===== */
            <div className="gf-win-bg absolute inset-0 flex flex-col items-center justify-center overflow-hidden">

              {/* 코인 폭포 */}
              {COINS.map((c, i) => (
                <div
                  key={i}
                  className="absolute pointer-events-none"
                  style={{
                    left: c.left,
                    top: "-60px",
                    fontSize: "1.8rem",
                    animation: `gf-coin ${c.dur} linear infinite`,
                    animationDelay: c.delay,
                  }}
                >
                  {c.emoji}
                </div>
              ))}

              {/* 방사형 스파크 */}
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                {WIN_SPARKS.map((sp, i) => {
                  const rad = (sp.angle * Math.PI) / 180;
                  const tx = Math.round(Math.cos(rad) * sp.dist);
                  const ty = Math.round(Math.sin(rad) * sp.dist);
                  return (
                    <div
                      key={i}
                      className="absolute text-xl"
                      style={{
                        animation: `gf-spark 0.8s ease-out infinite`,
                        animationDelay: sp.delay,
                        ["--gf-tx" as string]: `${tx}px`,
                        ["--gf-ty" as string]: `${ty}px`,
                      }}
                    >
                      {sp.emoji}
                    </div>
                  );
                })}
              </div>

              {/* 트로피 */}
              <div
                className="gf-trophy mb-2 relative z-10"
                style={{ fontSize: "6rem", lineHeight: 1 }}
              >
                🏆
              </div>

              {/* 당첨 텍스트 */}
              <div
                className="gf-win-text gf-gold-text font-black relative z-10 mb-3"
                style={{
                  fontSize: "clamp(3rem, 12vw, 5rem)",
                  color: "#ffd700",
                  WebkitTextStroke: "2px #b8860b",
                  letterSpacing: "0.15em",
                }}
              >
                당 첨 !!!
              </div>

              {/* 상품명 카드 */}
              <div
                className="gf-prize-card relative z-10 rounded-3xl px-8 py-4 mb-4 text-center"
                style={{
                  background: "rgba(0,0,0,0.6)",
                  border: "2px solid #ffd700",
                  boxShadow: "0 0 30px rgba(255,215,0,0.4)",
                  backdropFilter: "blur(8px)",
                }}
              >
                <div className="text-white font-black text-2xl">{result.prizeName}</div>
                <div className="text-yellow-400 text-sm mt-1">🎁 관리자에게 수령 문의하세요</div>
              </div>

              {/* 확률 정보 */}
              <div
                className="relative z-10 text-xs mb-6 px-4 py-2 rounded-full"
                style={{ background: "rgba(0,0,0,0.4)", color: "rgba(255,215,0,0.7)" }}
              >
                확률 {(result.probability * 100).toFixed(2)}%에서 기적의 당첨!
              </div>

              <button
                onClick={onClose}
                className="gf-bounce-loop relative z-10 font-black text-black text-xl px-12 py-4 rounded-2xl tracking-wide"
                style={{
                  background: "linear-gradient(135deg, #ffd700, #ff8c00)",
                  boxShadow: "0 0 40px rgba(255,215,0,0.8)",
                }}
              >
                🎊 확인
              </button>
            </div>

          ) : (

            /* ===== LOSS ===== */
            <div
              className="gf-miss-bg absolute inset-0 flex flex-col items-center justify-center px-6"
              style={{ background: "#0a0a1a" }}
            >
              <div className="gf-shake text-8xl mb-5">😢</div>

              <div
                className="gf-miss-text font-black mb-2"
                style={{
                  fontSize: "clamp(2.5rem, 10vw, 4rem)",
                  color: "#6b7280",
                  textShadow: "0 0 20px rgba(107,114,128,0.4)",
                }}
              >
                아쉽게 꽝!
              </div>

              <div className="text-gray-500 text-sm mb-5">
                확률 {(result.probability * 100).toFixed(2)}%… 아쉽게 빗나갔어요
              </div>

              {result.breakdown && (
                <div
                  className="w-full max-w-xs rounded-2xl px-5 py-4 mb-5 text-sm space-y-2"
                  style={{
                    background: "rgba(234,88,12,0.12)",
                    border: "1px solid rgba(234,88,12,0.35)",
                  }}
                >
                  <div className="text-orange-400 font-bold text-center text-base">
                    🔥 미당첨 스택 {result.breakdown.pityStacks}회 누적
                  </div>
                  {result.breakdown.pityBonus > 0 && (
                    <div className="text-orange-300 text-xs text-center">
                      +{(result.breakdown.pityBonus * 100).toFixed(2)}%p 보너스 적립 중
                    </div>
                  )}
                  {result.breakdown.factors.length > 0 && (
                    <div className="border-t border-orange-800/40 pt-2 text-orange-300/80 text-xs text-center">
                      활동 보너스 +{(result.breakdown.activityBonus * 100).toFixed(2)}%p 적용 중
                    </div>
                  )}
                </div>
              )}

              <div className="text-gray-600 text-xs mb-7">
                오늘 남은 뽑기: <strong className="text-gray-500">{result.remainingDrawsToday}회</strong>
              </div>

              <button
                onClick={onClose}
                className="px-10 py-3 rounded-2xl font-bold text-gray-300 transition hover:scale-105"
                style={{
                  background: "rgba(255,255,255,0.08)",
                  border: "1px solid rgba(255,255,255,0.15)",
                }}
              >
                닫기
              </button>
            </div>
          )
        )}
      </div>
    </>
  );
}
