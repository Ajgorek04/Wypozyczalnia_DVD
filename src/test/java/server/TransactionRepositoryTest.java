package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TransactionRepositoryTest {

    private TransactionRepository transRepo;
    private UserRepository userRepo;
    private int userId;

    @BeforeEach
    void setUp() throws Exception {
        TestDatabaseSetup.initDatabase();
        transRepo = new TransactionRepository();
        userRepo = new UserRepository();

        // Rejestrujemy klienta do testów
        userRepo.registerUser("klient1", "pass");
        userId = userRepo.getUserIdByCredentials("klient1", "pass");
    }

    @Test
    void shouldRentFilmAndCreateFee() {
        // 1. Wypożycz film ID 1 (Matrix)
        String result = transRepo.rentFilm(userId, 1);

        assertTrue(result.contains("Wypożyczono"), "Komunikat sukcesu");
        assertTrue(result.contains("10.00"), "Musi zawierać opłatę startową 10.00");

        // 2. Sprawdź czy jest opłata w systemie
        List<String> trans = transRepo.getUserTransactions(userId);
        assertEquals(1, trans.size());
        assertTrue(trans.get(0).contains("Opłacona: NIE"));
    }

    @Test
    void shouldNotReturnIfNotPaid() {
        transRepo.rentFilm(userId, 1);

        // Próba zwrotu bez płacenia
        String result = transRepo.returnFilm(userId, 1);

        assertTrue(result.contains("RETURN_FAIL;UNPAID"), "Musi blokować zwrot nieopłaconego filmu");
    }

    @Test
    void shouldPayAndAutoReturn() {
        transRepo.rentFilm(userId, 1);

        // Pobierz ID opłaty z listy
        List<String> trans = transRepo.getUserTransactions(userId);
        String line = trans.get(0);
        // Wyciągamy ID (np. "Opłata 1 | ...")
        int oplataId = Integer.parseInt(line.split("\\|")[0].replaceAll("[^0-9]", ""));

        // Płacimy (co powinno wywołać auto-zwrot)
        String payResult = transRepo.payTransaction(oplataId);

        assertTrue(payResult.contains("Sukces"), "Płatność udana");
        assertTrue(payResult.contains("zwrócony"), "Film powinien zostać zwrócony");

        // Sprawdzamy status
        trans = transRepo.getUserTransactions(userId);
        assertTrue(trans.get(0).contains("Opłacona: TAK"));

        // Sprawdzamy czy film faktycznie oddany w bazie (Repozytorium filmów)
        FilmRepository filmRepo = new FilmRepository();
        String filmInfo = filmRepo.getAllFilmsFormatted().get(0); // Matrix
        assertTrue(filmInfo.contains("Dostępny: true"), "Film powinien być znów dostępny");
    }
}