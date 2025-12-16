// java
package server;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TransactionRepository {

    public boolean rentFilm(int userId, int filmId) {
        if (userId <= 0 || filmId <= 0) {
            System.err.println("Niepoprawne id user/film: " + userId + " / " + filmId);
            return false;
        }

        Connection conn = Database.connect();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            // zablokuj wiersz filmu i sprawdź dostępność
            try (PreparedStatement check = conn.prepareStatement("SELECT dostepny FROM Film WHERE id = ? FOR UPDATE")) {
                check.setInt(1, filmId);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next() || !rs.getBoolean("dostepny")) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // ustaw na niedostępny
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
                int affected = ins.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    System.err.println("Brak wstawionej transakcji dla user=" + userId + " film=" + filmId);
                    return false;
                }
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    if (keys.next()) transId = keys.getInt(1);
                    else {
                        conn.rollback();
                        System.err.println("Nie uzyskano wygenerowanego id transakcji");
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
            System.out.println("Wypożyczono film: filmId=" + filmId + " userId=" + userId + " transId=" + transId);
            return true;
        } catch (SQLException e) {
            System.err.println("Błąd podczas rentFilm: " + e.getMessage());
            try { conn.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
        }
    }

    public String returnFilmWithCheck(int userId, int filmId) {
        if (userId <= 0 || filmId <= 0) {
            return "RETURN_FAIL";
        }

        Connection conn = Database.connect();
        if (conn == null) return "RETURN_FAIL";

        try {
            conn.setAutoCommit(false);

            int transId = -1;
            Timestamp tsWyp = null;

            // Pobierz transakcję (z blokadą)
            String transQuery = "SELECT id, dataWypozyczenia FROM Transakcja WHERE klient_id = ? AND film_id = ? AND dataZwrotu IS NULL FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(transQuery)) {
                ps.setInt(1, userId);
                ps.setInt(2, filmId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        transId = rs.getInt("id");
                        tsWyp = rs.getTimestamp("dataWypozyczenia");
                        System.out.println("Transakcja znaleziona: ID = " + transId + ", Data wypożyczenia = " + tsWyp);
                    } else {
                        System.out.println("Brak aktywnej transakcji dla użytkownika " + userId + " i filmu " + filmId);
                        conn.rollback();
                        return "RETURN_FAIL";
                    }
                }
            }

            // Oblicz należność (bezpiecznie jeśli tsWyp == null)
            long hours = 0;
            if (tsWyp != null) {
                try {
                    hours = Duration.between(tsWyp.toInstant(), Instant.now()).toHours();
                } catch (Exception ex) {
                    // jeśli typ daty różny - dalej liczymy jako 0
                    hours = 0;
                }
            }
            double totalDue = 10.0 + Math.max(0, hours - 1) * 1.0;
            System.out.println("Obliczona należność: " + totalDue);

            // Sprawdź istniejące opłaty
            double existingTotal = 0.0;
            String paymentQuery = "SELECT IFNULL(SUM(kwota), 0) FROM Oplata WHERE transakcja_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(paymentQuery)) {
                ps.setInt(1, transId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) existingTotal = rs.getDouble(1);
                    System.out.println("Istniejące opłaty: " + existingTotal);
                }
            }

            // Dodaj brakującą opłatę (jeśli potrzeba)
            if (existingTotal + 0.001 < totalDue) {
                double diff = Math.round((totalDue - existingTotal) * 100.0) / 100.0;
                System.out.println("Dodawanie brakującej opłaty: " + diff);
                String insertPayment = "INSERT INTO Oplata(transakcja_id, rachunek_id, kwota, powod) VALUES (?, NULL, ?, ?)";
                try (PreparedStatement ins = conn.prepareStatement(insertPayment)) {
                    ins.setInt(1, transId);
                    ins.setDouble(2, diff);
                    ins.setString(3, "Dopłata za czas wypożyczenia (" + diff + " zł)");
                    ins.executeUpdate();
                }
            }

            // Sprawdź, czy wszystkie opłaty są opłacone
            String unpaidQuery = "SELECT COUNT(*) FROM Oplata WHERE transakcja_id = ? AND rachunek_id IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(unpaidQuery)) {
                ps.setInt(1, transId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        System.out.println("Nieopłacone opłaty dla transakcji: " + transId);
                        conn.rollback();
                        return "RETURN_FAIL;UNPAID";
                    }
                }
            }

            // Zaktualizuj transakcję i film
            String updateTransaction = "UPDATE Transakcja SET dataZwrotu = NOW() WHERE id = ?";
            try (PreparedStatement upd = conn.prepareStatement(updateTransaction)) {
                upd.setInt(1, transId);
                upd.executeUpdate();
            }

            String updateFilm = "UPDATE Film SET dostepny = TRUE WHERE id = ?";
            try (PreparedStatement updF = conn.prepareStatement(updateFilm)) {
                updF.setInt(1, filmId);
                updF.executeUpdate();
            }

            conn.commit();
            System.out.println("Film zwrócony pomyślnie: Film ID = " + filmId + ", Transakcja ID = " + transId);
            return "RETURN_OK";
        } catch (SQLException e) {
            System.err.println("Błąd podczas returnFilmWithCheck: " + e.getMessage());
            try { conn.rollback(); } catch (SQLException ignored) {}
            return "RETURN_FAIL";
        } finally {
            try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
        }
    }

    // Pozostałe metody (getUserRentals, getUserTransactions, payCharge) można pozostawić bez zmian
    // lub analogicznie poprawić logowanie / try-with-resources jeśli potrzeba.
    // (zostawione bez zmian w tej wersji)
    public List<String> getUserRentals(int userId) {
        List<String> result = new ArrayList<>();
        Connection conn = Database.connect();
        if (conn == null) return result;

        // Zwracamy również id filmu jako pierwszą liczbę w linii (tak jak GET_FILMS),
        // klient parsuje pierwszą liczbę jako filmId przy zwrocie.
        String sql = "SELECT t.id, f.id AS film_id, f.tytul, t.dataWypozyczenia, t.dataZwrotu " +
                "FROM Transakcja t JOIN Film f ON t.film_id = f.id " +
                "WHERE t.klient_id = ? ORDER BY t.dataWypozyczenia DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp tw = rs.getTimestamp("dataWypozyczenia");
                    Timestamp tz = rs.getTimestamp("dataZwrotu");
                    String line = rs.getInt("film_id") + ". " + rs.getString("tytul")
                            + " | Wypożyczono: " + (tw != null ? tw.toString() : "null")
                            + " | Zwrócono: " + (tz != null ? tz.toString() : "null");
                    result.add(line);
                }
            }
        } catch (SQLException e) {
            System.err.println("Błąd getUserRentals: " + e.getMessage());
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
            System.err.println("Błąd getUserTransactions: " + e.getMessage());
        } finally {
            try { conn.close(); } catch (SQLException ignored) {}
        }
        return result;
    }

    public boolean payCharge(int userId, int oplataId, double amount) {
        Connection conn = Database.connect();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

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

            try (PreparedStatement upd = conn.prepareStatement("UPDATE Oplata SET rachunek_id = ? WHERE id = ?")) {
                upd.setInt(1, rachunekId);
                upd.setInt(2, oplataId);
                upd.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Błąd payCharge: " + e.getMessage());
            try { conn.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
        }
    }
}
