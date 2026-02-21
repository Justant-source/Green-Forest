import { getToken } from "@/lib/auth";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8080/ws";
const NULL_CHAR = "\u0000";

type MessageHandler = (body: string) => void;

export class StompClient {
  private ws: WebSocket | null = null;
  private subscriptions: Map<string, MessageHandler> = new Map();
  private subIdCounter = 0;
  private connected = false;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private destroyed = false;

  connect() {
    if (this.destroyed) return;

    const token = getToken();
    this.ws = new WebSocket(WS_URL);

    this.ws.onopen = () => {
      // STOMP CONNECT 프레임
      const connectFrame = [
        "CONNECT",
        "accept-version:1.2",
        `Authorization:Bearer ${token || ""}`,
        "heart-beat:0,0",
        "",
        NULL_CHAR,
      ].join("\n");
      this.ws!.send(connectFrame);
    };

    this.ws.onmessage = (event) => {
      const data = typeof event.data === "string" ? event.data : "";
      const command = data.split("\n")[0];

      if (command === "CONNECTED") {
        this.connected = true;
        // 연결 후 대기중인 구독 재등록
        this.subscriptions.forEach((handler, dest) => {
          this.sendSubscribe(dest);
        });
        return;
      }

      if (command === "MESSAGE") {
        // 헤더와 바디 분리
        const parts = data.split("\n\n");
        if (parts.length < 2) return;
        const body = parts.slice(1).join("\n\n").replace(NULL_CHAR, "");
        // destination 헤더에서 구독 매칭
        const headerLines = parts[0].split("\n");
        let destination = "";
        for (const line of headerLines) {
          if (line.startsWith("destination:")) {
            destination = line.substring("destination:".length);
            break;
          }
        }
        const handler = this.subscriptions.get(destination);
        if (handler) handler(body);
      }
    };

    this.ws.onclose = () => {
      this.connected = false;
      if (!this.destroyed) {
        this.reconnectTimer = setTimeout(() => this.connect(), 5000);
      }
    };

    this.ws.onerror = () => {
      // onclose가 이어서 호출됨
    };
  }

  subscribe(destination: string, handler: MessageHandler) {
    this.subscriptions.set(destination, handler);
    if (this.connected) {
      this.sendSubscribe(destination);
    }
  }

  private sendSubscribe(destination: string) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    const id = `sub-${this.subIdCounter++}`;
    const frame = [
      "SUBSCRIBE",
      `id:${id}`,
      `destination:${destination}`,
      "",
      NULL_CHAR,
    ].join("\n");
    this.ws.send(frame);
  }

  disconnect() {
    this.destroyed = true;
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    if (this.ws) {
      if (this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(["DISCONNECT", "", NULL_CHAR].join("\n"));
      }
      this.ws.close();
      this.ws = null;
    }
    this.connected = false;
    this.subscriptions.clear();
  }

  isConnected() {
    return this.connected;
  }
}
