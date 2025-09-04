package org.rin;

import io.javalin.Javalin;
import org.rin.config.JavalinConfig;
import org.rin.config.DatabaseConfig;
import org.rin.controller.UserController;
import org.rin.controller.UserSessionController;
import org.rin.ws.WSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        DataSource ds = DatabaseConfig.getDataSource();
        try (Connection conn = ds.getConnection()) {
            System.out.println("✅ DB Connected: " + ds);
        } catch (Exception e) {
            System.err.println("❌ Error connecting to database");
        }

        Javalin app = JavalinConfig.getInstance();


        // REST test
        app.get("/", ctx -> ctx.result("Hello from REST API!"));

        // DB check
        app.get("/db-check", ctx -> {
            try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
                ctx.result("DB is OK!");
            } catch (Exception e) {
                ctx.result("DB ERROR: " + e.getMessage());
            }
        });
        // Controller
        new UserSessionController();
        new UserController();



        // WebSocket
        app.ws(WSConfig.WS_PATH, ws -> {

            ws.onConnect(ctx -> {
//                WSConfig.getInstance().onConnect(ctx);
            });

            ws.onMessage(ctx -> {
                String msg = ctx.message();
                WSConfig.getInstance().onMessage(ctx, msg);
            });
            ws.onClose(ctx -> {
                WSConfig.getInstance().onDisconnect(ctx);
            });
        });

        System.out.println("🚀 WebSocket server started on port 8080");
        System.out.println("📡 WebSocket endpoint: ws://localhost:8080/ws");
    }


}
