import { CompatClient, Stomp } from "@stomp/stompjs";

let client: CompatClient | null = null;

export function getWebSocketClient(username: string): CompatClient {
  if (!client) {
    client = Stomp.client(`ws://localhost:8080/ws?username=${username}`);
  }
  return client;
}
