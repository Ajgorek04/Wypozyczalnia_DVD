// java
package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FilmRepository {

    public List<String> getAllFilms() {
        List<String> films = new ArrayList<>();
        String sql = "SELECT id, tytul, gatunek, rok, dostepny FROM Film";

        try (Connection conn = Database.connect()) {
            if (conn == null) {
                System.err.println("Nie można nawiązać połączenia z bazą danych.");
                return films;
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    String film = rs.getInt("id") + ". " + rs.getString("tytul")
                            + " (" + rs.getInt("rok") + ") - "
                            + rs.getString("gatunek")
                            + " | Dostępny: " + rs.getBoolean("dostepny");
                    films.add(film);
                }

            }
        } catch (SQLException e) {
            System.err.println("Błąd podczas pobierania filmów: " + e.getMessage());
        }

        return films;
    }
}
