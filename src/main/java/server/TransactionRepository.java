package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionRepository {
    private static final Logger logger = LogManager.getLogger(TransactionRepository.class);

    private static final double OPLATA_STARTOWA = 10.00;
    private static final double STAWKA_ZA_MINUTE = 1.50;

    public String rentFilm(int userId, int filmId) {
        Connection conn = Database.connect();
        if (conn == null) return "Błąd połączenia z bazą";

        try {
            conn.setAutoCommit(false);

            boolean available = false;
            try (PreparedStatement ps = conn.prepareStatement("SELECT dostepny FROM Film WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, filmId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) available = rs.getBoolean("dostepny");
            }

            if (!available) {
                conn.rollback();
                return "Film jest niedostępny";
            }

            int transakcjaId = -1;
            String insertTrans = "INSERT INTO Transakcja (klient_id, film_id, dataWypozyczenia) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertTrans, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setInt(2, filmId);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) transakcjaId = keys.getInt(1);
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE Film SET dostepny = 0 WHERE id = ?")) {
                ps.setInt(1, filmId);
                ps.executeUpdate();
            }

            String feeSql = "INSERT INTO Oplata (transakcja_id, kwota, powod) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(feeSql)) {
                ps.setInt(1, transakcjaId);
                ps.setDouble(2, OPLATA_STARTOWA);
                ps.setString(3, "Wypożyczenie (Start)");
                ps.executeUpdate();
            }

            conn.commit();
            logger.info("Użytkownik " + userId + " wypożyczył film " + filmId);
            return String.format(Locale.US, "Wypożyczono. Opłata bieżąca: %.2f zł. Czas start!", OPLATA_STARTOWA);

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) {}
            logger.error("Błąd SQL przy wypożyczaniu", e);
            return "Błąd bazy danych";
        } finally {
            try { if(conn!=null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException e) {}
        }
    }

    public String returnFilm(int userId, int filmId) {
        Connection conn = Database.connect();
        try {
            String check = "SELECT count(*) FROM Transakcja t JOIN Oplata o ON o.transakcja_id = t.id " +
                    "WHERE t.klient_id = ? AND t.film_id = ? AND t.dataZwrotu IS NULL AND o.rachunek_id IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setInt(1, userId);
                ps.setInt(2, filmId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    // Jest nieopłacona transakcja
                    return "RETURN_FAIL;UNPAID - Aby zwrócić film, wejdź w 'Zapłać opłatę'. System automatycznie policzy czas i zwróci film po zapłacie.";
                }
            }
            return "Film już został zwrócony (lub nie masz takiego wypożyczenia).";
        } catch (SQLException e) {
            return "Błąd bazy";
        }
    }

    public List<String> getUserTransactions(int userId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT o.id, o.kwota, o.powod, o.rachunek_id, t.dataWypozyczenia, t.dataZwrotu " +
                "FROM Oplata o JOIN Transakcja t ON o.transakcja_id = t.id " +
                "WHERE t.klient_id = ?";

        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int oid = rs.getInt("id");
                double kwotaBaza = rs.getDouble("kwota");
                String powod = rs.getString("powod");
                boolean oplacona = rs.getObject("rachunek_id") != null;

                Timestamp dw = rs.getTimestamp("dataWypozyczenia");
                Timestamp dz = rs.getTimestamp("dataZwrotu");

                // LOGIKA: Jeśli film NIE oddany (dz == null) i NIE opłacony -> doliczamy czas "na żywo"
                if (!oplacona && dz == null && dw != null) {
                    LocalDateTime start = dw.toLocalDateTime();
                    LocalDateTime now = LocalDateTime.now();
                    long minuty = Duration.between(start, now).toMinutes();
                    if (minuty < 0) minuty = 0;

                    double doplata = minuty * STAWKA_ZA_MINUTE;
                    double calaSuma = kwotaBaza + doplata; // 10.00 + (min * 1.50)

                    kwotaBaza = calaSuma;
                    powod = String.format(Locale.US, "Suma bieżąca (Start + %d min)", minuty);
                }

                String line = String.format(Locale.US, "Opłata %d | kwota: %.2f zł | powód: %s | Opłacona: %s",
                        oid, kwotaBaza, powod, (oplacona ? "TAK" : "NIE"));
                list.add(line);
            }
        } catch (SQLException e) { logger.error("Błąd opłat", e); }
        return list;
    }

    public String payTransaction(int oplataId) {
        Connection conn = Database.connect();
        if (conn == null) return "Błąd połączenia";

        try {
            conn.setAutoCommit(false);

            int klientId = -1;
            int transakcjaId = -1;
            int filmId = -1;
            double kwotaStartowa = 0;
            Timestamp dataWyp = null;
            Timestamp dataZwrotu = null;

            String sqlCheck = "SELECT t.klient_id, t.id as tid, t.film_id, t.dataWypozyczenia, t.dataZwrotu, o.kwota, o.rachunek_id " +
                    "FROM Oplata o JOIN Transakcja t ON o.transakcja_id = t.id " +
                    "WHERE o.id = ? FOR UPDATE"; // Blokujemy rekord

            try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setInt(1, oplataId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (rs.getObject("rachunek_id") != null) { conn.rollback(); return "Już opłacona!"; }

                    klientId = rs.getInt("klient_id");
                    transakcjaId = rs.getInt("tid");
                    filmId = rs.getInt("film_id");
                    kwotaStartowa = rs.getDouble("kwota");
                    dataWyp = rs.getTimestamp("dataWypozyczenia");
                    dataZwrotu = rs.getTimestamp("dataZwrotu");
                } else {
                    conn.rollback(); return "Błąd: Opłata nie istnieje.";
                }
            }

            double finalnaKwota = kwotaStartowa;
            String opisRachunku = "Opłacenie wypożyczenia";

            if (dataZwrotu == null) {
                LocalDateTime now = LocalDateTime.now();
                long minuty = Duration.between(dataWyp.toLocalDateTime(), now).toMinutes();
                if (minuty < 0) minuty = 0;

                double kosztCzasu = minuty * STAWKA_ZA_MINUTE;
                finalnaKwota += kosztCzasu;
                opisRachunku = String.format(Locale.US, "Startowe + Czas (%d min)", minuty);

                try (PreparedStatement ps = conn.prepareStatement("UPDATE Transakcja SET dataZwrotu = ? WHERE id = ?")) {
                    ps.setTimestamp(1, Timestamp.valueOf(now));
                    ps.setInt(2, transakcjaId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE Film SET dostepny = 1 WHERE id = ?")) {
                    ps.setInt(1, filmId);
                    ps.executeUpdate();
                }
                logger.info("Auto-zwrot filmu ID: " + filmId + " przy płatności.");
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE Oplata SET kwota = ?, powod = ? WHERE id = ?")) {
                ps.setDouble(1, finalnaKwota);
                ps.setString(2, opisRachunku);
                ps.setInt(3, oplataId);
                ps.executeUpdate();
            }

            int nowyRachunekId = -1;
            String insRachunek = "INSERT INTO Rachunek (klient_id, dataWystawienia, lacznaKwota) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insRachunek, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, klientId);
                ps.setDate(2, Date.valueOf(java.time.LocalDate.now()));
                ps.setDouble(3, finalnaKwota);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) nowyRachunekId = keys.getInt(1);
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE Oplata SET rachunek_id = ? WHERE id = ?")) {
                ps.setInt(1, nowyRachunekId);
                ps.setInt(2, oplataId);
                ps.executeUpdate();
            }

            conn.commit();
            return String.format(Locale.US, "Sukces! Płatność przyjęta: %.2f zł. Film został automatycznie zwrócony.", finalnaKwota);

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) {}
            logger.error("Błąd płatności", e);
            return "Błąd bazy danych";
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) {}
        }
    }

    public List<String> getUserRentals(int userId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT f.id, f.tytul, t.dataWypozyczenia, t.dataZwrotu " +
                "FROM Transakcja t JOIN Film f ON t.film_id = f.id " +
                "WHERE t.klient_id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int fid = rs.getInt("id");
                String tytul = rs.getString("tytul");
                String dw = rs.getString("dataWypozyczenia");
                String dz = rs.getString("dataZwrotu");
                String stan = (dz == null) ? "Wypożyczony" : "Zwrócony (" + dz + ")";
                list.add(fid + ". " + tytul + " - " + stan);
            }
        } catch (SQLException e) { logger.error("Błąd historii", e); }
        return list;
    }
}