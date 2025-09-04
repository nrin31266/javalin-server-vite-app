package org.rin.config;

import io.javalin.Javalin;

public class JavalinConfig {
    private static Javalin app;

    private JavalinConfig() {}

    public static synchronized Javalin getInstance() {
        if (app == null) {
            app = Javalin.create(config -> {
                config.bundledPlugins .enableCors(cors ->{
                     cors.addRule(it->{
                         it.allowHost("http://localhost:3000",
                                 "http://localhost:5173");
                     });
                });
            }).start("0.0.0.0", 8080);
        }
        return app;
    }
}
