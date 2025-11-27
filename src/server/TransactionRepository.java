//java
package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionRepository {

    public boolean rentFilm(int userId, int filmId) {
        Connection conn = Database.connect();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            // sprawdź dostępność
            try (PreparedStatement check = conn.prepareStatement("SELECT dostepny FROM Film WHERE id = ? FOR UPDATE")) {
                check.setInt(1, filmId);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next() || !rs.getBoolean("dostepny")) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // ustaw jako niedostępny
            try (PreparedStatement upd = conn.prepareStatement("UPDATE Film SET dostepny = FALSE WHERE id = ?")) {
                upd.setInt(1, filmId);
                upd.executeUpdate();
            }

            // dodaj transakcję (używamy kolumny klient_id zgodnie ze schematem)
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO Transakcja(klient_id, film_id, dataWypozyczenia) VALUES (?, ?, CURDATE())")) {
                ins.setInt(1, userId);
                ins.setInt(2, filmId);
                ins.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try { conn.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
        }
    }

    public boolean returnFilm(int userId, int filmId) {
        Connection conn = Database.connect();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            // znajdź aktywną transakcję
            int transId = -1;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM Transakcja WHERE klient_id = ? AND film_id = ? AND dataZwrotu IS NULL FOR UPDATE")) {
                ps.setInt(1, userId);
                ps.setInt(2, filmId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) transId = rs.getInt("id");
                    else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // ustaw date zwrotu
            try (PreparedStatement upd = conn.prepareStatement(
                    "UPDATE Transakcja SET dataZwrotu = CURDATE() WHERE id = ?")) {
                upd.setInt(1, transId);
                upd.executeUpdate();
            }

            // ustaw film dostępny
            try (PreparedStatement updF = conn.prepareStatement("UPDATE Film SET dostepny = TRUE WHERE id = ?")) {
                updF.setInt(1, filmId);
                updF.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try { conn.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
        }
    }

    public List<String> getUserRentals(int userId) {
        List<String> result = new ArrayList<>();
        Connection conn = Database.connect();
        if (conn == null) return result;

        String sql = "SELECT t.id, f.tytul, t.dataWypozyczenia, t.dataZwrotu " +
                "FROM Transakcja t JOIN Film f ON t.film_id = f.id " +
                "WHERE t.klient_id = ? ORDER BY t.dataWypozyczenia DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String line = rs.getInt("id") + ". " + rs.getString("tytul")
                            + " | Wypożyczono: " + rs.getDate("dataWypozyczenia")
                            + " | Zwrócono: " + rs.getString("dataZwrotu");
                    result.add(line);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { conn.close(); } catch (SQLException ignored) {}
        }
        return result;
    }

    public List<String> getUserTransactions(int userId) {
        List<String> result = new ArrayList<>();
        Connection conn = Database.connect();
        if (conn == null) return result;

        String sql = "SELECT o.id AS oplata_id, o.kwota, o.powod, o.transakcja_id " +
                "FROM Oplata o JOIN Transakcja t ON o.transakcja_id = t.id " +
                "WHERE t.klient_id = ? ORDER BY o.id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String line = "Opłata " + rs.getInt("oplata_id") + " | transakcja: " + rs.getInt("transakcja_id")
                            + " | kwota: " + rs.getDouble("kwota") + " | powód: " + rs.getString("powod");
                    result.add(line);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { conn.close(); } catch (SQLException ignored) {}
        }
        return result;
    }

    public boolean payCharge(int userId, int oplataId, double amount) {
        Connection conn = Database.connect();
        if (conn == null) return false;

        try {
            String check = "SELECT o.id FROM Oplata o JOIN Transakcja t ON o.transakcja_id = t.id WHERE o.id = ? AND t.klient_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setInt(1, oplataId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                }
            }

            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO Rachunek(klient_id, dataWystawienia, lacznaKwota) VALUES (?, CURDATE(), ?)")) {
                ins.setInt(1, userId);
                ins.setDouble(2, amount);
                ins.executeUpdate();
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }
}
