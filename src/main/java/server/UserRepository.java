package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private static final Logger logger = LogManager.getLogger(UserRepository.class);


    public boolean registerUser(String username, String password) {
        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            logger.warn("Próba rejestracji z pustymi danymi.");
            return false;
        }
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
                        String insKlient = "INSERT INTO Klient(id, imie, nazwisko, email, telefon) VALUES (?, ?, ?, ?, ?)";
                        try (PreparedStatement pk = conn.prepareStatement(insKlient)) {
                            pk.setInt(1, userId);
                            pk.setString(2, ""); pk.setString(3, ""); pk.setString(4, ""); pk.setString(5, "");
                            pk.executeUpdate();
                        }
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Błąd rejestracji", e);
            return false;
        } finally {
            try { conn.close(); } catch (SQLException ignored) {}
        }
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
            logger.error("Błąd logowania", e);
            return -1;
        } finally {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }


    public List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        String sql = "SELECT id, username FROM Uzytkownik WHERE username != 'admin'"; // Nie pokazujemy admina na liście do usunięcia
        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add("ID: " + rs.getInt("id") + " | Login: " + rs.getString("username"));
            }
        } catch (SQLException e) {
            logger.error("Błąd pobierania użytkowników", e);
        }
        return users;
    }

    public boolean deleteUser(int userId) {
        Connection conn = Database.connect();
        if (conn == null) return false;
        try {
            conn.setAutoCommit(false);

            String delOplaty = "DELETE o FROM Oplata o JOIN Transakcja t ON o.transakcja_id = t.id WHERE t.klient_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(delOplaty)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Rachunek WHERE klient_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE Film SET dostepny=1 WHERE id IN (SELECT film_id FROM Transakcja WHERE klient_id=? AND dataZwrotu IS NULL)")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Transakcja WHERE klient_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Klient WHERE id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Uzytkownik WHERE id = ?")) {
                ps.setInt(1, userId);
                int rows = ps.executeUpdate();
                conn.commit();
                logger.info("Usunięto użytkownika ID: " + userId);
                return rows > 0;
            }

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) {}
            logger.error("Błąd usuwania użytkownika ID " + userId, e);
            return false;
        } finally {
            try { conn.close(); } catch (SQLException e) {}
        }
    }

    public boolean changeUserPassword(int userId, String newPass) {
        String sql = "UPDATE Uzytkownik SET password_hash = ? WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash(newPass));
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            logger.info("Zmieniono hasło dla ID: " + userId);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Błąd zmiany hasła", e);
            return false;
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