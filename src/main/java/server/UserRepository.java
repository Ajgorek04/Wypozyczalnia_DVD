//java
package server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class UserRepository {

    public boolean registerUser(String username, String password) {
        if (username == null || password == null || username.isBlank() || password.isBlank()) return false;

        Connection conn = Database.connect();
        if (conn == null) return false;

        try {
            try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM Uzytkownik WHERE username = ?")) {
                check.setString(1, username);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) return false;
                }
            }

            String sql = "INSERT INTO Uzytkownik(username, password_hash) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, hash(password));
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int userId = keys.getInt(1);
                        // utwórz odpowiadający wpis w tabeli Klient z tym samym id (puste dane kontaktowe)
                        String insKlient = "INSERT INTO Klient(id, imie, nazwisko, email, telefon) VALUES (?, ?, ?, ?, ?)";
                        try (PreparedStatement pk = conn.prepareStatement(insKlient)) {
                            pk.setInt(1, userId);
                            pk.setString(2, "");
                            pk.setString(3, "");
                            pk.setString(4, "");
                            pk.setString(5, "");
                            pk.executeUpdate();
                        }
                    }
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    public boolean authenticateUser(String username, String password) {
        return getUserIdByCredentials(username, password) >= 0;
    }

    public int getUserIdByCredentials(String username, String password) {
        if (username == null || password == null) return -1;
        Connection conn = Database.connect();
        if (conn == null) return -1;

        try (PreparedStatement ps = conn.prepareStatement("SELECT id, password_hash FROM Uzytkownik WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password_hash");
                    if (stored != null && stored.equals(hash(password))) {
                        return rs.getInt("id");
                    }
                }
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        } finally {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
