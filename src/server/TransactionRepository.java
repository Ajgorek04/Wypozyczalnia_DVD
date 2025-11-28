package server;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TransactionRepository {

    public boolean rentFilm(int userId, int filmId) {
        Connection conn = Database.connect();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement check = conn.prepareStatement("SELECT dostepny FROM Film WHERE id = ? FOR UPDATE")) {
                check.setInt(1, filmId);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next() || !rs.getBoolean("dostepny")) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement upd = conn.prepareStatement("UPDATE Film SET dostepny = FALSE WHERE id = ?")) {
                upd.setInt(1, filmId);
                upd.executeUpdate();
            }

            // wstaw transakcję i pobierz id
            int transId;
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO Transakcja(klient_id, film_id, dataWypozyczenia) VALUES (?, ?, NOW())",
                    Statement.RETURN_GENERATED_KEYS)) {
                ins.setInt(1, userId);
                ins.setInt(2, filmId);
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    if (keys.next()) transId = keys.getInt(1);
                    else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // dodaj opłatę startową 10 zł (rachunek_id NULL => nieopłacona)
            try (PreparedStatement insOp = conn.prepareStatement(
                    "INSERT INTO Oplata(transakcja_id, rachunek_id, kwota, powod) VALUES (?, NULL, ?, ?)")) {
                insOp.setInt(1, transId);
                insOp.setDouble(2, 10.0);
                insOp.setString(3, "Opłata startowa");
                insOp.executeUpdate();
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

    public String returnFilmWithCheck(int userId, int filmId) {
        Connection conn = Database.connect();
        if (conn == null) return "RETURN_FAIL";

        try {
            conn.setAutoCommit(false);

            int transId = -1;
            Timestamp tsWyp = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, dataWypozyczenia FROM Transakcja WHERE klient_id = ? AND film_id = ? AND dataZwrotu IS NULL FOR UPDATE")) {
                ps.setInt(1, userId);
                ps.setInt(2, filmId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        transId = rs.getInt("id");
                        tsWyp = rs.getTimestamp("dataWypozyczenia");
                    } else {
                        conn.rollback();
                        return "RETURN_FAIL";
                    }
                }
            }

            long hours = 0;
            if (tsWyp != null) {
                Instant start = tsWyp.toInstant();
                Instant now = Instant.now();
                hours = Duration.between(start, now).toHours();
                if (hours < 0) hours = 0;
            }
            double totalDue = 10.0 + Math.max(0, hours - 1) * 1.0;

            double existingTotal = 0.0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT IFNULL(SUM(kwota),0) FROM Oplata WHERE transakcja_id = ?")) {
                ps.setInt(1, transId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) existingTotal = rs.getDouble(1);
                }
            }

            if (existingTotal + 0.001 < totalDue) {
                double diff = Math.round((totalDue - existingTotal) * 100.0) / 100.0;
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO Oplata(transakcja_id, rachunek_id, kwota, powod) VALUES (?, NULL, ?, ?)")) {
                    ins.setInt(1, transId);
                    ins.setDouble(2, diff);
                    ins.setString(3, "Dopłata za czas wypożyczenia (" + diff + " zł)");
                    ins.executeUpdate();
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Oplata WHERE transakcja_id = ? AND rachunek_id IS NULL")) {
                ps.setInt(1, transId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        conn.rollback();
                        return "RETURN_FAIL;UNPAID";
                    }
                }
            }

            try (PreparedStatement upd = conn.prepareStatement(
                    "UPDATE Transakcja SET dataZwrotu = NOW() WHERE id = ?")) {
                upd.setInt(1, transId);
                upd.executeUpdate();
            }

            try (PreparedStatement updF = conn.prepareStatement("UPDATE Film SET dostepny = TRUE WHERE id = ?")) {
                updF.setInt(1, filmId);
                updF.executeUpdate();
            }

            conn.commit();
            return "RETURN_OK";
        } catch (SQLException e) {
            e.printStackTrace();
            try { conn.rollback(); } catch (SQLException ignored) {}
            return "RETURN_FAIL";
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
                    Timestamp tw = rs.getTimestamp("dataWypozyczenia");
                    Timestamp tz = rs.getTimestamp("dataZwrotu");
                    String line = rs.getInt("id") + ". " + rs.getString("tytul")
                            + " | Wypożyczono: " + (tw != null ? tw.toString() : "null")
                            + " | Zwrócono: " + (tz != null ? tz.toString() : "null");
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

        String sql = "SELECT o.id AS oplata_id, o.kwota, o.powod, o.transakcja_id, o.rachunek_id " +
                "FROM Oplata o JOIN Transakcja t ON o.transakcja_id = t.id " +
                "WHERE t.klient_id = ? ORDER BY o.id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String paid = rs.getObject("rachunek_id") == null ? "NIE" : "TAK";
                    String line = "Opłata " + rs.getInt("oplata_id") + " | transakcja: " + rs.getInt("transakcja_id")
                            + " | kwota: " + rs.getDouble("kwota") + " | powód: " + rs.getString("powod")
                            + " | Opłacona: " + paid;
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

    /**
     * Płacenie opłaty:
     * - sprawdza, czy dana opłata istnieje i należy do użytkownika oraz nie jest jeszcze opłacona
     * - pobiera kwotę z tabeli Oplata i używa jej przy tworzeniu Rachunek (ignoruje przekazany parametr amount)
     */
    public boolean payCharge(int userId, int oplataId, double amount) {
        Connection conn = Database.connect();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            // sprawdź uprawnienia i czy opłata nie jest już opłacona (rachunek_id IS NULL)
            Integer transId = null;
            String check = "SELECT o.transakcja_id FROM Oplata o JOIN Transakcja t ON o.transakcja_id = t.id WHERE o.id = ? AND t.klient_id = ? AND o.rachunek_id IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setInt(1, oplataId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    transId = rs.getInt(1);
                }
            }

            // pobierz kwotę opłaty z tabeli Oplata (użyj tej kwoty do rachunku)
            double oplataKwota;
            try (PreparedStatement ps = conn.prepareStatement("SELECT kwota FROM Oplata WHERE id = ?")) {
                ps.setInt(1, oplataId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        oplataKwota = rs.getDouble(1);
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // wstaw rachunek i pobierz wygenerowane id (używamy oplataKwota)
            int rachunekId;
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO Rachunek(klient_id, dataWystawienia, lacznaKwota) VALUES (?, NOW(), ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ins.setInt(1, userId);
                ins.setDouble(2, oplataKwota);
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    if (keys.next()) rachunekId = keys.getInt(1);
                    else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // powiąż opłatę z rachunkiem
            try (PreparedStatement upd = conn.prepareStatement("UPDATE Oplata SET rachunek_id = ? WHERE id = ?")) {
                upd.setInt(1, rachunekId);
                upd.setInt(2, oplataId);
                upd.executeUpdate();
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
}
