package org.rin.dto;

import org.rin.model.User;

public class UserHandleDTO {
    private User user;
    private String action; // e.g., "create", "update", "delete"
    public UserHandleDTO(User user, String action) {
        this.user = user;
        this.action = action;
    }
    public User getUser() { return user; }
    public String getAction() { return action; }

    public void setUser(User user) { this.user = user; }
    public void setAction(String action) { this.action = action; }
}
