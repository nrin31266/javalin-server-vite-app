import { createContext, useContext, useEffect, useState } from "react";
import { getWebSocketClient } from "./socket";
import type { CompatClient } from "@stomp/stompjs";
import { v4 as uuidv4 } from 'uuid';


const WsContext = createContext<CompatClient | null>(null);
export const useWebSocket = () => useContext(WsContext);

const WebSocketProvider = ({ children }: { children: React.ReactNode }) => {
  const [stompClient, setStompClient] = useState<CompatClient | null>(null);
  // const username = localStorage.getItem("username");

  useEffect(() => {
    const client = getWebSocketClient(uuidv4());

    if (!client.connected) {
      client.connect(
        { },
        
        () => {
          console.log("✅ Connected WebSocket");
          setStompClient(client);
        },
        (err: unknown) => console.log("❌ WS connect error", err),
        
      );
    }

    return () => {
      if (client.connected) {
        client.disconnect(() => console.log("🔌 Disconnected"));
      }
    };
  }, []);

  return (
    <WsContext.Provider value={stompClient}>{children}</WsContext.Provider>
  );
};

export default WebSocketProvider;
