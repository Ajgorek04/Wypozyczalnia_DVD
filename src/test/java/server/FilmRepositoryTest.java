package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FilmRepositoryTest {

    private FilmRepository filmRepo;

    @BeforeEach
    void setUp() throws Exception {
        TestDatabaseSetup.initDatabase();
        filmRepo = new FilmRepository();
    }

    @Test
    void shouldGetAllFilmsFormatted() {
        List<String> films = filmRepo.getAllFilmsFormatted();
        assertFalse(films.isEmpty(), "Baza nie powinna być pusta (dane z init.sql)");

        // Sprawdź format (Locale.US kropki, itp.)
        // Oczekujemy np: "1. Matrix (1999) - Dostępny: true"
        String firstFilm = films.get(0);
        assertTrue(firstFilm.contains("Matrix"), "Powinien być Matrix");
        assertTrue(firstFilm.contains("Dostępny:"), "Musi zawierać status dostępności");
    }
}