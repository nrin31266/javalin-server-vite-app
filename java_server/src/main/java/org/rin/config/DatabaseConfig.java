package org.rin.config;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

public class DatabaseConfig {

    // Singleton instance
    // volatile an toàn khi nhiều thread đọc/ghi
    private static volatile BasicDataSource dataSource;

    private DatabaseConfig() {}

    private static void initDataSource() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://localhost:3306/test-db?useSSL=false&serverTimezone=UTC");
        ds.setUsername("root");
        ds.setPassword("root");

        // Connection pool config
        ds.setInitialSize(5);            // số connection tạo sẵn
        ds.setMinIdle(5);                // số connection idle tối thiểu
        ds.setMaxIdle(10);               // số connection idle tối đa
        ds.setMaxTotal(20);              // tổng số connection tối đa
        ds.setMaxOpenPreparedStatements(100);

        dataSource = ds;
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null) {
                    initDataSource();
                }
            }
        }
        return dataSource;
    }
}
