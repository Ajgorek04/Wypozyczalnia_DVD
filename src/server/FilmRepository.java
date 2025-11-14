package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FilmRepository {

    public List<String> getAllFilms() {
        List<String> films = new ArrayList<>();

        String sql = "SELECT id, tytul, gatunek, rok, dostepny FROM Film";

        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String film = rs.getInt("id") + ". " + rs.getString("tytul")
                        + " (" + rs.getInt("rok") + ") - "
                        + rs.getString("gatunek")
                        + " | Dostępny: " + rs.getBoolean("dostepny");
                films.add(film);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return films;
    }
}
