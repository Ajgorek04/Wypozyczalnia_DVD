package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Klasa repozytorium odpowiedzialna za bezpośrednie operacje na tabeli Film w bazie danych.
 * <p>
 * Służy do pobierania informacji o dostępnych zasobach (filmach) i przygotowywania
 * ich w formie sformatowanej listy tekstowej dla warstwy prezentacji (Klienta).
 * </p>
 *
 * @author Twój Zespół
 * @version 1.0
 */
public class FilmRepository {

    /** Logger do rejestrowania zdarzeń i błędów SQL. */
    private static final Logger logger = LogManager.getLogger(FilmRepository.class);

    /**
     * Domyślny konstruktor klasy.
     */
    public FilmRepository() {
    }

    /**
     * Pobiera listę wszystkich filmów z bazy danych i formatuje ją do czytelnej postaci tekstowej.
     * <p>
     * Metoda wykonuje zapytanie SQL SELECT, a następnie iteruje przez wyniki.
     * Wartość logiczna (boolean) określająca dostępność filmu jest konwertowana
     * na polski tekst ("Tak" lub "Nie").
     * </p>
     *
     * @return Lista ciągów znaków (String), gdzie każdy element to opis jednego filmu
     * w formacie: {@code "ID. Tytuł (Rok) - Dostępny: Tak/Nie"}.
     * W przypadku błędu połączenia z bazą, lista zawiera komunikat o błędzie.
     */
    public List<String> getAllFilmsFormatted() {
        List<String> result = new ArrayList<>();
        // Upewnij się, że nazwy kolumn pasują do Twojej bazy (np. gatunek)
        String sql = "SELECT id, tytul, rok, gatunek, dostepny FROM Film";

        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String tytul = rs.getString("tytul");
                int rok = rs.getInt("rok");
                boolean dostepny = rs.getBoolean("dostepny");

                // --- ZMIANA TUTAJ: Tłumaczenie boolean na polski ---
                String dostepnoscTekst = dostepny ? "Tak" : "Nie";

                // Format: "1. Matrix (1999) - Dostępny: Tak"
                String line = String.format("%d. %s (%d) - Dostępny: %s", id, tytul, rok, dostepnoscTekst);
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