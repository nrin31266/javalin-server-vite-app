package org.rin.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import io.javalin.websocket.WsContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Lớp quản lý WebSocket connections với STOMP protocol
 */
public class WSConfig {

    public static final String WS_PATH = "/ws";
    private static final Logger log = LoggerFactory.getLogger(WSConfig.class);

    // Singleton instance
    private static WSConfig instance;

    // Data structures
    private final Map<String, WsContext> wsClients = new ConcurrentHashMap<>();
    private final Map<String, UserSession> users = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> topicSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, String> subscriptionIds = new ConcurrentHashMap<>();


    // Scheduler cho heartbeat/ping
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private WSConfig() {
        scheduler.scheduleAtFixedRate(() -> {
            for (WsContext ctx : wsClients.values()) {
                try {
                    if (ctx.session.isOpen()) {
                        ctx.sendPing(ByteBuffer.allocate(0));
                    }
                } catch (Exception ignored) {}
            }
        }, 0, 30, TimeUnit.SECONDS);

        // Cleanup every 5 minutes
        scheduler.scheduleAtFixedRate(this::cleanupInactiveConnections, 5, 5, TimeUnit.MINUTES);
    }

    public static synchronized WSConfig getInstance() {
        if (instance == null) {
            instance = new WSConfig();
        }
        return instance;
    }

    /**
     * Xử lý khi client ngắt kết nối
     */
    public void onDisconnect(WsContext ctx) {
        String sessionId = ctx.sessionId();
        UserSession user = users.get(sessionId);
        if (user != null) {
            log.info("❌ Disconnected: {}", user.getUsername());
            unsubscribeFromAllTopics(sessionId);
        }
        wsClients.remove(sessionId);
        users.remove(sessionId);
    }

    /**
     * Xử lý message từ client - CHỈ XỬ LÝ STOMP
     */
    public void onMessage(WsContext ctx, String message) {
//        log.info("Received message: {}", message);

        // CHỈ XỬ LÝ STOMP FRAMES
        if (isStompFrame(message)) {
            handleStompFrame(ctx, message);
        } else {
            // Nếu không phải STOMP frame, gửi lỗi
            sendStompError(ctx, "Invalid STOMP frame");
        }
    }

    private boolean isStompFrame(String message) {
        return message.startsWith("CONNECT") ||
                message.startsWith("STOMP") ||
                message.startsWith("SUBSCRIBE") ||
                message.startsWith("UNSUBSCRIBE") ||
                message.startsWith("SEND") ||
                message.startsWith("DISCONNECT");
    }

    private void handleStompFrame(WsContext ctx, String frame) {
        if (frame.startsWith("CONNECT") || frame.startsWith("STOMP")) {
            handleStompConnect(ctx, frame);
        } else if (frame.startsWith("SUBSCRIBE")) {
            handleStompSubscribe(ctx, frame);
        } else if (frame.startsWith("UNSUBSCRIBE")) {
            handleStompUnsubscribe(ctx, frame);
        } else if (frame.startsWith("SEND")) {
            handleStompSend(ctx, frame);
        } else if (frame.startsWith("DISCONNECT")) {
            handleStompDisconnect(ctx, frame);
        }
    }

    private void handleStompConnect(WsContext ctx, String frame) {
        String sessionId = ctx.sessionId();
        InetSocketAddress remote = (InetSocketAddress) ctx.session.getRemoteAddress();
        String ip = remote.getAddress().getHostAddress();
        String username = ctx.queryParam("username");

        if (username == null || username.trim().isEmpty()) {
            sendStompError(ctx, "Username is required");
            ctx.session.close();
            return;
        }

        // Tạo user session
        UserSession user = new UserSession(sessionId, ip, remote.getPort(), username, true);
        users.put(sessionId, user);
        wsClients.put(sessionId, ctx);

        // Gửi STOMP CONNECTED frame
        String connectedFrame = "CONNECTED\n" +
                "version:1.2\n" +
                "heart-beat:0,0\n" +
                "user-name:" + username + "\n" +
                "\n\u0000";

        ctx.send(connectedFrame);
        log.info("✅ STOMP Connected: {} ({})", username, ip);
    }

//    private void handleStompSubscribe(WsContext ctx, String frame) {
//        String destination = extractHeaderValue(frame, "destination");
//        String subscriptionId = extractHeaderValue(frame, "id");
//
//        if (destination != null && subscriptionId != null) {
//            String sessionId = ctx.sessionId();
//            subscribeToTopic(sessionId, destination);
//
//            // Lưu subscription ID
//            String key = sessionId + ":" + subscriptionId;
//            subscriptionIds.put(key, destination);
//        }
//    }

    private void handleStompSubscribe(WsContext ctx, String frame) {
        String destination = extractHeaderValue(frame, "destination");
        String subscriptionId = extractHeaderValue(frame, "id");

        if (destination != null && subscriptionId != null) {
            String sessionId = ctx.sessionId();
            subscribeToTopic(sessionId, destination);

            // Lưu subscription ID toi
            String key = sessionId + ":" + subscriptionId;
            subscriptionIds.put(key, destination);
        }
    }

    private void handleStompUnsubscribe(WsContext ctx, String frame) {
        String subscriptionId = extractHeaderValue(frame, "id");

        if (subscriptionId != null) {
            String sessionId = ctx.sessionId();
            String key = sessionId + ":" + subscriptionId;
            String destination = subscriptionIds.get(key);

            if (destination != null) {
                unsubscribeFromTopic(sessionId, destination);
                subscriptionIds.remove(key);
                log.info("📌 STOMP Unsubscribe: {} (id: {})", destination, subscriptionId);
            }
        }
    }

    private void handleStompSend(WsContext ctx, String frame) {
        String destination = extractHeaderValue(frame, "destination");
        String content = extractBodyContent(frame);
        String contentType = extractHeaderValue(frame, "content-type");

        if (destination != null && content != null) {
            try {
                UserSession user = users.get(ctx.sessionId());
                String username = user != null ? user.getUsername() : "unknown";

                // Tạo payload object để gửi
                Map<String, Object> payload = new HashMap<>();
                payload.put("from", username);
                payload.put("timestamp", System.currentTimeMillis());

                // Xử lý content type
                if ("application/json".equals(contentType)) {
                    try {
                        // Parse JSON content nếu là JSON
                        JsonElement jsonContent = JsonParser.parseString(content);
                        payload.put("content", jsonContent);
                    } catch (Exception e) {
                        // Fallback: dùng raw content nếu parse lỗi
                        payload.put("content", content);
                    }
                } else {
                    // Text content
                    payload.put("content", content);
                }

                // GỬI BẰNG buildAndSendToTopic - không dùng createStompMessage nữa
                buildAndSendToTopic(destination, payload);

                log.info("📤 User {} sent message to topic: {}", username, destination);

            } catch (Exception e) {
                log.error("❌ Failed to process SEND frame", e);
                sendStompError(ctx, "Failed to process message: " + e.getMessage());
            }
        }
    }

    private void handleStompDisconnect(WsContext ctx, String frame) {
        String sessionId = ctx.sessionId();
        UserSession user = users.get(sessionId);

        if (user != null) {
            log.info("🔌 STOMP Disconnect: {}", user.getUsername());

            // Gửi receipt nếu có
            String receiptId = extractHeaderValue(frame, "receipt");
            if (receiptId != null) {
                String receiptFrame = "RECEIPT\nreceipt-id:" + receiptId + "\n\n\u0000";
                ctx.send(receiptFrame);
            }

            // Cleanup
            unsubscribeFromAllTopics(sessionId);
            wsClients.remove(sessionId);
            users.remove(sessionId);
            ctx.session.close();
        }
    }

    private String extractHeaderValue(String frame, String headerName) {
        String[] lines = frame.split("\n");
        for (String line : lines) {
            if (line.startsWith(headerName + ":")) {
                return line.substring(headerName.length() + 1).trim();
            }
        }
        return null;
    }

    private String extractBodyContent(String frame) {
        String[] lines = frame.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].equals("") && i + 1 < lines.length) {
                return lines[i + 1];
            }
        }
        return null;
    }


    private void sendStompError(WsContext ctx, String message) {
        String errorFrame = "ERROR\nmessage:" + message + "\n\n\u0000";
        ctx.send(errorFrame);
    }

    private void subscribeToTopic(String sessionId, String topic) {
        UserSession user = users.get(sessionId);
        if (user != null) {
            String processedTopic = topic.replace("{username}", user.getUsername());
            user.subscribeToTopic(processedTopic);

            topicSubscriptions.computeIfAbsent(processedTopic, k -> ConcurrentHashMap.newKeySet())
                    .add(sessionId);

            log.info("📌 {} subscribed to: {}", user.getUsername(), processedTopic);
        }
    }

    private void unsubscribeFromTopic(String sessionId, String topic) {
        UserSession user = users.get(sessionId);
        if (user != null) {
            String processedTopic = topic.replace("{username}", user.getUsername());
            user.unsubscribeFromTopic(processedTopic);

            Set<String> subscribers = topicSubscriptions.get(processedTopic);
            if (subscribers != null) {
                subscribers.remove(sessionId);
                if (subscribers.isEmpty()) {
                    topicSubscriptions.remove(processedTopic);
                }
            }

            log.info("📌 {} unsubscribed from: {}", user.getUsername(), processedTopic);
        }
    }

    private void unsubscribeFromAllTopics(String sessionId) {
        UserSession user = users.get(sessionId);
        if (user != null) {
            Set<String> topics = user.getSubscribedTopics();
            for (String topic : topics) {
                Set<String> subscribers = topicSubscriptions.get(topic);
                if (subscribers != null) {
                    subscribers.remove(sessionId);
                    if (subscribers.isEmpty()) {
                        topicSubscriptions.remove(topic);
                    }
                }
            }
            user.unsubscribeFromAllTopics();
            subscriptionIds.keySet().removeIf(key -> key.startsWith(sessionId + ":"));
        }
    }

    public void buildAndSendToTopic(String topic, Object jsonData) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(jsonData);

        // Lấy danh sách subscribers để thêm subscription headers
        Set<String> subscribers = topicSubscriptions.get(topic);

        if (subscribers != null && !subscribers.isEmpty()) {
            for (String sessionId : subscribers) {
                WsContext ctx = wsClients.get(sessionId);
                if (ctx != null && ctx.session.isOpen()) {
                    try {
                        // Tìm subscription ID cho session này
                        String subscriptionId = findSubscriptionId(sessionId, topic);

                        // Tạo STOMP message với subscription header - GIỐNG SPRING BOOT
                        String stompMessage = "MESSAGE\n" +
                                "destination:" + topic + "\n" +
                                "content-type:application/json\n" +
                                "message-id:" + UUID.randomUUID() + "\n" +
                                "subscription:" + subscriptionId + "\n" +
                                "content-length:" + jsonString.getBytes(StandardCharsets.UTF_8).length + "\n" +
                                "\n" +
                                jsonString + "\n" +
                                "\0";
//                        log.info(stompMessage);

                        ctx.send(stompMessage);
                        log.info("✅ Sent to {} with subscription: {}", topic, subscriptionId);
                    } catch (Exception e) {
                        log.error("❌ Failed to send to session {}: {}", sessionId, e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Tìm subscription ID cho session và topic - optimized
     */
    private String findSubscriptionId(String sessionId, String topic) {
        return subscriptionIds.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(sessionId + ":") &&
                        entry.getValue().equals(topic))
                .findFirst()
                .map(entry -> entry.getKey().split(":")[1])
                .orElse("sub-" + Math.abs(topic.hashCode() % 1000)); // Fallback
    }

    private void sendToTopic(String topic, String stompMessage) {
        Set<String> subscribers = topicSubscriptions.get(topic);
        if (subscribers != null && !subscribers.isEmpty()) {
            int sentCount = 0;
            for (String sessionId : subscribers) {
                WsContext ctx = wsClients.get(sessionId);
                if (ctx != null && ctx.session.isOpen()) {
                    try {
                        ctx.send(stompMessage);
                        sentCount++;
                    } catch (Exception ignored) {}
                }
            }
            log.info("📤 Sent to topic '{}': {} recipients", topic, sentCount);
        }
    }

    // ========== PUBLIC METHODS FOR TOPIC MANAGEMENT ========== //


    /**
     * Gửi message đến specific user
     */
    /**
     * Gửi message đến specific user - dùng buildAndSendToTopic
     */
    public void sendToUser(String username, Object data) throws JsonProcessingException {
        String userTopic = "/user/" + username;
        buildAndSendToTopic(userTopic, data);
    }

    public Map<String, Set<String>> getTopicSubscriptions() {
        return new ConcurrentHashMap<>(topicSubscriptions);
    }

    public List<UserSession> getActiveUsers() {
        return new ArrayList<>(users.values());
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    /**
     * Cleanup inactive connections định kỳ
     */
    public void cleanupInactiveConnections() {
        Iterator<Map.Entry<String, WsContext>> iterator = wsClients.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, WsContext> entry = iterator.next();
            if (!entry.getValue().session.isOpen()) {
                log.info("🧹 Cleaning up inactive connection: {}", entry.getKey());
                iterator.remove();
                users.remove(entry.getKey());
                unsubscribeFromAllTopics(entry.getKey());
            }
        }
    }
}