package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FilmRepository {
    private static final Logger logger = LogManager.getLogger(FilmRepository.class);

    public List<String> getAllFilmsFormatted() {
        List<String> result = new ArrayList<>();
        // Twoja tabela nazywa się Film
        String sql = "SELECT id, tytul, rok, gatunek, dostepny FROM Film";

        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String tytul = rs.getString("tytul");
                int rok = rs.getInt("rok");
                boolean dostepny = rs.getBoolean("dostepny");

                String line = String.format("%d. %s (%d) - Dostępny: %b", id, tytul, rok, dostepny);
                result.add(line);
            }
            logger.info("Pobrano filmy z bazy.");

        } catch (SQLException e) {
            logger.error("Błąd SQL przy pobieraniu filmów", e);
            result.add("Błąd bazy danych.");
        }
        return result;
    }
}