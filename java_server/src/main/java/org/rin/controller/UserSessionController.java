package org.rin.controller;

import io.javalin.Javalin;
import org.rin.config.JavalinConfig;
import org.rin.ws.WSConfig;

public class UserSessionController {
    private final WSConfig wsConfig = WSConfig.getInstance();

    private final Javalin app = JavalinConfig.getInstance();

    public UserSessionController() {
        // Định nghĩa các endpoint liên quan đến user ở đây
        app.get("/ss-users", ctx -> {
            // Trả về danh sách user đang kết nối
            ctx.json(wsConfig.getActiveUsers());
        });
    }
}
