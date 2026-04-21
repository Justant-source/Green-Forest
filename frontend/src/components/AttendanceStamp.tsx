"use client";
import React, { useState } from "react";

interface Props {
  onStamp: () => void;
  disabled?: boolean;
  stamped?: boolean;
}

export default function AttendanceStamp({
  onStamp,
  disabled = false,
  stamped = false,
}: Props) {
  const [animating, setAnimating] = useState(false);

  const handleClick = () => {
    if (disabled || stamped) return;
    setAnimating(true);
    onStamp();
    setTimeout(() => setAnimating(false), 400);
  };

  return (
    <>
      <style jsx>{`
        @keyframes stamp {
          0% {
            transform: scale(0.8);
          }
          50% {
            transform: scale(1.2);
          }
          100% {
            transform: scale(1);
          }
        }
      `}</style>
      <button
        onClick={handleClick}
        disabled={disabled || stamped}
        className={`relative flex flex-col items-center justify-center w-32 h-32 rounded-full
          transition-all duration-200
          ${
            stamped
              ? "bg-green-100 cursor-default"
              : "bg-green-500 hover:bg-green-600 active:scale-95 cursor-pointer"
          }
          ${disabled && !stamped ? "opacity-50 cursor-not-allowed" : ""}
          shadow-lg
        `}
        style={animating ? { animation: "stamp 0.4s ease-out" } : {}}
      >
        <span className="text-4xl">{stamped ? "✅" : "🌿"}</span>
        <span
          className={`text-sm font-bold mt-1 ${
            stamped ? "text-green-700" : "text-white"
          }`}
        >
          {stamped ? "출석완료" : "출석하기"}
        </span>
      </button>
    </>
  );
}
