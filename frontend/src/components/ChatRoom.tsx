"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { useRouter } from "next/navigation";
import { ChatMessage } from "@/types";
import { getConversationMessages, sendChatMessage, leaveConversation, getConversations } from "@/lib/api";
import { Client } from "@stomp/stompjs";
import { getToken } from "@/lib/auth";
import { useAuth } from "@/context/AuthContext";

interface ChatRoomProps {
  conversationId: number;
}

function formatTime(dateStr: string): string {
  const date = new Date(dateStr.endsWith("Z") ? dateStr : dateStr + "Z");
  return date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
}

export default function ChatRoom({ conversationId }: ChatRoomProps) {
  const router = useRouter();
  const { isLoggedIn, nickname, authLoaded } = useAuth();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(true);
  const [otherNickname, setOtherNickname] = useState("");
  const [otherLeft, setOtherLeft] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const stompConnected = useRef(false);

  // 메시지 중복 없이 병합
  const mergeMessages = useCallback((prev: ChatMessage[], incoming: ChatMessage[]) => {
    const ids = new Set(prev.map((m) => m.id));
    const newMsgs = incoming.filter((m) => !ids.has(m.id));
    if (newMsgs.length === 0) return prev;
    return [...prev, ...newMsgs];
  }, []);

  useEffect(() => {
    if (!authLoaded) return;
    if (!isLoggedIn) {
      router.push("/login");
      return;
    }

    // 초기 로드
    Promise.all([
      getConversations(),
      getConversationMessages(conversationId),
    ])
      .then(([convList, msgs]) => {
        setMessages(msgs);
        const conv = convList.find((c) => c.id === conversationId);
        if (conv) {
          setOtherNickname(conv.otherNickname);
          setOtherLeft(conv.otherLeft);
        }
      })
      .catch(console.error)
      .finally(() => setLoading(false));

    // STOMP 연결 시도
    let client: Client | null = null;
    try {
      client = new Client({
        brokerURL: process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8080/ws",
        connectHeaders: {
          Authorization: `Bearer ${getToken() || ""}`,
        },
        reconnectDelay: 5000,
        onConnect: () => {
          stompConnected.current = true;
          client!.subscribe(`/topic/messages/${conversationId}`, (frame) => {
            const msg: ChatMessage = JSON.parse(frame.body);
            setMessages((prev) => {
              if (prev.some((m) => m.id === msg.id)) return prev;
              return [...prev, msg];
            });
            if (msg.systemMessage && msg.content.includes("나갔습니다")) {
              setOtherLeft(true);
            }
          });
        },
        onDisconnect: () => { stompConnected.current = false; },
        onStompError: () => { stompConnected.current = false; },
        onWebSocketError: () => { stompConnected.current = false; },
      });
      client.activate();
    } catch {
      // WebSocket 지원 안 되면 폴링으로 동작
    }

    // 폴링 — STOMP 연결 여부와 관계없이 3초마다 새 메시지 확인
    const pollInterval = setInterval(() => {
      getConversationMessages(conversationId)
        .then((msgs) => {
          setMessages((prev) => mergeMessages(prev, msgs));
          // 나간 상태 갱신
          const leaveMsg = msgs.find(
            (m) => m.systemMessage && m.content.includes("나갔습니다") && !m.content.startsWith(nickname || "")
          );
          if (leaveMsg) setOtherLeft(true);
        })
        .catch(() => {});
    }, 3000);

    return () => {
      clearInterval(pollInterval);
      if (client) client.deactivate();
      stompConnected.current = false;
    };
  }, [conversationId, authLoaded, isLoggedIn, nickname, router, mergeMessages]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim()) return;

    const content = input.trim();
    setInput("");
    try {
      const msg = await sendChatMessage(conversationId, content);
      setMessages((prev) => {
        if (prev.some((m) => m.id === msg.id)) return prev;
        return [...prev, msg];
      });
    } catch {
      alert("메시지 전송에 실패했습니다.");
      setInput(content);
    }
  };

  const handleLeave = async () => {
    if (!confirm("대화를 나가시겠습니까?")) return;
    try {
      await leaveConversation(conversationId);
      router.push("/conversations");
    } catch (error) {
      console.error("Failed to leave:", error);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-20">
        <div className="w-10 h-10 border-4 border-gray-300 border-t-orange-600 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-[calc(100vh-64px)] max-w-2xl mx-auto">
      {/* 상단 바 */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 bg-white">
        <div className="flex items-center gap-3">
          <button
            onClick={() => router.push("/conversations")}
            className="text-gray-500 hover:text-gray-700 transition-colors"
          >
            &larr;
          </button>
          <span className="font-medium text-gray-900">
            {otherNickname || "대화"}
          </span>
          {otherLeft && (
            <span className="text-xs text-gray-400">(나감)</span>
          )}
        </div>
        <button
          onClick={handleLeave}
          className="text-sm text-red-500 hover:text-red-600 px-3 py-1.5 rounded-lg hover:bg-red-50 transition-colors"
        >
          나가기
        </button>
      </div>

      {/* 메시지 영역 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3 bg-gray-50">
        {messages.map((msg) => {
          if (msg.systemMessage) {
            return (
              <div key={msg.id} className="text-center">
                <span className="text-xs text-gray-400 bg-gray-200 px-3 py-1 rounded-full">
                  {msg.content}
                </span>
              </div>
            );
          }

          const isMine = msg.senderNickname === nickname;

          return (
            <div
              key={msg.id}
              className={`flex ${isMine ? "justify-end" : "justify-start"}`}
            >
              <div className={`max-w-[70%] ${isMine ? "order-1" : ""}`}>
                {!isMine && (
                  <p className="text-xs text-gray-500 mb-1">{msg.senderNickname}</p>
                )}
                <div
                  className={`px-4 py-2.5 rounded-2xl text-sm break-words ${
                    isMine
                      ? "bg-orange-400 text-white rounded-tr-md"
                      : "bg-white text-gray-800 border border-gray-200 rounded-tl-md"
                  }`}
                >
                  {msg.content}
                </div>
                <p className={`text-xs text-gray-400 mt-1 ${isMine ? "text-right" : ""}`}>
                  {formatTime(msg.createdAt)}
                </p>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* 입력 폼 */}
      <form
        onSubmit={handleSend}
        className="flex items-center gap-2 px-4 py-3 border-t border-gray-200 bg-white"
      >
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder={otherLeft ? "상대방이 나간 대화입니다" : "메시지를 입력하세요..."}
          disabled={otherLeft}
          className="flex-1 px-4 py-2.5 border border-gray-300 rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-orange-500 disabled:bg-gray-100 disabled:text-gray-400"
        />
        <button
          type="submit"
          disabled={!input.trim() || otherLeft}
          className="px-5 py-2.5 bg-orange-400 text-white rounded-full text-sm font-medium hover:bg-orange-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          전송
        </button>
      </form>
    </div>
  );
}
