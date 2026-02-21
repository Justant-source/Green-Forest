import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { getToken } from "@/lib/auth";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || "http://localhost:8080/ws";

let stompClient: Client | null = null;

export function getStompClient(): Client {
  if (stompClient && stompClient.active) {
    return stompClient;
  }

  const token = getToken();

  stompClient = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    connectHeaders: {
      Authorization: token ? `Bearer ${token}` : "",
    },
    reconnectDelay: 5000,
  });

  return stompClient;
}

export function disconnectStomp() {
  if (stompClient && stompClient.active) {
    stompClient.deactivate();
    stompClient = null;
  }
}
