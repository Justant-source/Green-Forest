"use client";

import { useParams } from "next/navigation";
import ChatRoom from "@/components/ChatRoom";

export default function ConversationPage() {
  const params = useParams();
  const id = Number(params.id);

  if (!id || isNaN(id)) {
    return (
      <div className="text-center py-20 text-gray-500">
        잘못된 대화 ID입니다.
      </div>
    );
  }

  return <ChatRoom conversationId={id} />;
}
