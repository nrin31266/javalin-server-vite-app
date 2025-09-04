package org.rin.ws;

import java.util.HashSet;
import java.util.Set;

public class UserSession {
    private String sessionId;
    private String ip;
    private int port;
    private String username;
    private boolean isActive;
    private Set<String> subscribedTopics;

    public UserSession(String sessionId, String ip, int port, String username, boolean isActive) {
        this.sessionId = sessionId;
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.isActive = isActive;
        this.subscribedTopics = new HashSet<>();
    }

    public void subscribeToTopic(String topic) {
        subscribedTopics.add(topic);
    }

    public void unsubscribeFromTopic(String topic) {
        subscribedTopics.remove(topic);
    }

    public void unsubscribeFromAllTopics() {
        subscribedTopics.clear();
    }

    public boolean isSubscribedToTopic(String topic) {
        return subscribedTopics.contains(topic);
    }

    public Set<String> getSubscribedTopics() {
        return new HashSet<>(subscribedTopics);
    }

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public String getIp() { return ip; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}