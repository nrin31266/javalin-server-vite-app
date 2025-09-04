package org.rin.DAO;

import org.rin.config.DatabaseConfig;
import org.rin.model.User;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final DataSource ds = DatabaseConfig.getDataSource();

    // Thêm user và trả về User vừa tạo
    public User addUser(String name, String phone) {
        String sql = "INSERT INTO `test-db`.`user` (`name`, `phone`) VALUES (?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, phone);

            int affected = ps.executeUpdate();
            if (affected == 0) return null;

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new User(id, name, phone);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Sửa user theo id và trả về User vừa cập nhật
    public User updateUser(int id, String name, String phone) {
        String sql = "UPDATE `test-db`.`user` SET `name` = ?, `phone` = ? WHERE `id` = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setInt(3, id);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                // Trả về User vừa cập nhật
                return getUserById(id);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Xóa user theo id
    public boolean deleteUser(int id) {
        String sql = "DELETE FROM `test-db`.`user` WHERE `id` = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Lấy danh sách user
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT `id`, `name`, `phone` FROM `test-db`.`user` ORDER BY `id` DESC";
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("phone")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Lấy User theo id
    public User getUserById(int id) {
        String sql = "SELECT `id`, `name`, `phone` FROM `test-db`.`user` WHERE `id` = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("phone")
                    );
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
