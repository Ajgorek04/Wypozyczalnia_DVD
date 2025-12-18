package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class UserRepository {
    // 1. Dodanie loggera
    private static final Logger logger = LogManager.getLogger(UserRepository.class);

    public boolean registerUser(String username, String password) {
        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            logger.warn("Próba rejestracji z pustymi danymi.");
            return false;
        }

        Connection conn = Database.connect();
        if (conn == null) return false;

        try {
            // Sprawdzanie czy user istnieje
            try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM Uzytkownik WHERE username = ?")) {
                check.setString(1, username);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        logger.info("Nieudana rejestracja: użytkownik " + username + " już istnieje.");
                        return false;
                    }
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
                        logger.info("Utworzono nowego użytkownika: " + username + " (ID: " + userId + ")");

                        // Tworzenie wpisu w tabeli Klient
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
            // 2. Zastąpienie e.printStackTrace() loggerem
            logger.error("Błąd SQL podczas rejestracji użytkownika " + username, e);
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
                        int id = rs.getInt("id");
                        logger.info("Pomyślne logowanie użytkownika: " + username + " (ID: " + id + ")");
                        return id;
                    }
                }
                logger.warn("Nieudana próba logowania dla użytkownika: " + username);
                return -1;
            }
        } catch (SQLException e) {
            logger.error("Błąd SQL podczas logowania użytkownika " + username, e);
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
            logger.error("Błąd algorytmu haszowania", e);
            throw new RuntimeException(e);
        }
    }
}