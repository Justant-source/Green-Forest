import { Client } from "@stomp/stompjs";
import { getToken } from "@/lib/auth";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8080/ws";

export function createStompClient(): Client {
  const token = getToken();

  return new Client({
    brokerURL: WS_URL,
    connectHeaders: {
      Authorization: token ? `Bearer ${token}` : "",
    },
    reconnectDelay: 5000,
  });
}
