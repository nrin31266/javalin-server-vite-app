package org.rin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.javalin.Javalin;
import org.rin.config.JavalinConfig;
import org.rin.DAO.UserDAO;
import org.rin.dto.UserHandleDTO;
import org.rin.model.User;
import org.rin.ws.WSConfig;

import java.util.List;

public class UserController {

    private final Javalin app = JavalinConfig.getInstance();
    private final UserDAO userDAO = new UserDAO(); // tạo instance bình thường
    private final WSConfig wsConfig = WSConfig.getInstance();
    ObjectMapper mapper = new ObjectMapper();
    Gson gson = new Gson();
    public UserController() {
        registerRoutes();
    }

    private void registerRoutes() {
        // Lấy tất cả user
        app.get("/users", ctx -> {
            List<User> users = userDAO.getAllUsers();

            ctx.json(users);
        });

        // Lấy user theo id
        app.get("/users/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            User user = userDAO.getUserById(id);
            if (user != null) {
                ctx.json(user);
            } else {
                ctx.status(404).result("User not found");
            }
        });

        // Thêm user
        app.post("/users", ctx -> {
            System.out.println(ctx.body());
            User req = ctx.bodyAsClass(User.class); // JSON body {name, phone}
            User newUser = userDAO.addUser(req.getName(), req.getPhone());
            if (newUser != null) {
                ctx.status(201).json(newUser);
                wsConfig.buildAndSendToTopic(
                        "/topic/manager/users",
                        new UserHandleDTO(newUser,"add")
                );
            } else {
                ctx.status(500).result("Failed to create user");
            }
        });

        // Cập nhật user
        app.put("/users/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            User req = ctx.bodyAsClass(User.class);
            User updatedUser = userDAO.updateUser(id, req.getName(), req.getPhone());
            if (updatedUser != null) {
                ctx.json(updatedUser);
                wsConfig.buildAndSendToTopic(
                        "/topic/manager/users",
                        new UserHandleDTO(updatedUser,"update")
                );
            } else {
                ctx.status(404).result("User not found or update failed");
            }
        });

        // Xóa user
        app.delete("/users/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            boolean deleted = userDAO.deleteUser(id);
            if (deleted) {
                ctx.status(204); // No Content
                wsConfig.buildAndSendToTopic(
                        "/topic/manager/users",
                        new UserHandleDTO(new User(id, "", ""),"delete")
                );
            } else {
                ctx.status(404).result("User not found");
            }
        });
    }
}
