package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repozytorium zarządzające danymi użytkowników, uwierzytelnianiem i operacjami administracyjnymi.
 *
 * <br>Klasa odpowiada za:
 * <ul>
 * <li>Rejestrację nowych kont (wraz z tworzeniem profilu Klienta).</li>
 * <li>Weryfikację danych logowania (porównywanie skrótów haseł).</li>
 * <li>Operacje administracyjne: przeglądanie listy użytkowników, zmianę haseł oraz
 * bezpieczne usuwanie użytkowników wraz z całą ich historią (transakcje, opłaty).</li>
 * </ul>
 *
 * @author Twój Zespół
 * @version 2.0
 */
public class UserRepository {

    /** Logger log4j do rejestrowania zdarzeń związanych z użytkownikami. */
    private static final Logger logger = LogManager.getLogger(UserRepository.class);

    /**
     * Domyślny konstruktor.
     */
    public UserRepository() {
    }

    /**
     * Rejestruje nowego użytkownika w systemie.
     *
     * <br>Metoda wykonuje następujące kroki:
     * <ol>
     * <li>Sprawdza, czy login nie jest już zajęty.</li>
     * <li>Tworzy rekord w tabeli Uzytkownik (z zahaszowanym hasłem).</li>
     * <li>Tworzy powiązany rekord w tabeli Klient (wymagane przez klucze obce).</li>
     * </ol>
     *
     * @param username Nazwa użytkownika (login).
     * @param password Hasło (zostanie zahaszowane przed zapisem).
     * @return {@code true} jeśli rejestracja się powiodła, {@code false} jeśli login jest zajęty lub wystąpił błąd.
     */
    public boolean registerUser(String username, String password) {
        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            logger.warn("Próba rejestracji z pustymi danymi.");
            return false;
        }
        Connection conn = Database.connect();
        if (conn == null) return false;
        try {
            // Sprawdzenie duplikatów
            try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM Uzytkownik WHERE username = ?")) {
                check.setString(1, username);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) return false;
                }
            }

            // Dodanie użytkownika
            String sql = "INSERT INTO Uzytkownik(username, password_hash) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, hash(password));
                ps.executeUpdate();

                // Dodanie profilu klienta
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

    /**
     * Weryfikuje dane logowania użytkownika.
     *
     * @param username Podany login.
     * @param password Podane hasło (tekst jawny).
     * @return ID użytkownika (liczba > 0) w przypadku sukcesu, lub -1 w przypadku błędu logowania.
     */
    public int getUserIdByCredentials(String username, String password) {
        if (username == null || password == null) return -1;
        Connection conn = Database.connect();
        if (conn == null) return -1;
        try (PreparedStatement ps = conn.prepareStatement("SELECT id, password_hash FROM Uzytkownik WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password_hash");
                    // Porównanie zahaszowanego hasła z bazy z zahaszowanym hasłem podanym przez usera
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

    /**
     * Pobiera listę wszystkich użytkowników (dla panelu administratora).
     * <p>
     * Lista nie zawiera konta głównego administratora ('admin'), aby zapobiec jego przypadkowemu usunięciu.
     * </p>
     *
     * @return Lista sformatowanych ciągów znaków (ID i Login).
     */
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

    /**
     * Usuwa użytkownika i wszystkie powiązane z nim dane z systemu.
     *
     * <br>Operacja jest wykonywana w transakcji, aby zachować spójność bazy danych.
     * Kolejność usuwania (Kaskada):
     * <ol>
     * <li>Opłaty (tabela Oplata)</li>
     * <li>Rachunki (tabela Rachunek)</li>
     * <li>Odblokowanie filmów trzymanych przez użytkownika</li>
     * <li>Transakcje (tabela Transakcja)</li>
     * <li>Dane osobowe (tabela Klient)</li>
     * <li>Konto logowania (tabela Uzytkownik)</li>
     * </ol>
     *
     * @param userId ID użytkownika do usunięcia.
     * @return {@code true} jeśli operacja się powiodła.
     */
    public boolean deleteUser(int userId) {
        Connection conn = Database.connect();
        if (conn == null) return false;
        try {
            conn.setAutoCommit(false);

            // 1. Usuń Opłaty
            String delOplaty = "DELETE o FROM Oplata o JOIN Transakcja t ON o.transakcja_id = t.id WHERE t.klient_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(delOplaty)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            // 2. Usuń Rachunki
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Rachunek WHERE klient_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            // 3. Odblokuj filmy (żeby nie zostały "zjedzone" przez system)
            try (PreparedStatement ps = conn.prepareStatement("UPDATE Film SET dostepny=1 WHERE id IN (SELECT film_id FROM Transakcja WHERE klient_id=? AND dataZwrotu IS NULL)")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            // 4. Usuń Transakcje
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Transakcja WHERE klient_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            // 5. Usuń z Klient
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Klient WHERE id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            // 6. Usuń z Uzytkownik
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

    /**
     * Zmienia hasło wybranego użytkownika (funkcja administracyjna).
     *
     * @param userId ID użytkownika.
     * @param newPass Nowe hasło (zostanie zahaszowane).
     * @return {@code true} jeśli zmiana się powiodła.
     */
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

    /**
     * Metoda pomocnicza do haszowania haseł algorytmem SHA-256.
     *
     * @param input Hasło w tekście jawnym.
     * @return Skrót hasła w formacie szesnastkowym (Hex String).
     */
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