// java
package server;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class FilmRepositoryExtraTest {

    private static FilmRepository repo;

    @BeforeAll
    static void init() {
        repo = new FilmRepository();
    }

    @Test
    void containsKnownTitleAndFormat() {
        List<String> films = repo.getAllFilms();
        assertNotNull(films);
        assertFalse(films.isEmpty());
        // sprawdź, że przynajmniej jeden wpis ma format "id. tytul ... Dostępny: true/false"
        boolean okFormat = films.stream().anyMatch(s -> s.matches("^\\d+\\..*Dostępny:\\s*(true|false).*"));
        assertTrue(okFormat, "Brak wpisów w oczekiwanym formacie");
        // sprawdź, że przykładowy tytuł z init.sql występuje
        boolean hasMatrix = films.stream().anyMatch(s -> s.contains("Matrix"));
        assertTrue(hasMatrix, "Brak przykładowego tytułu 'Matrix' w wynikach");
    }
}
