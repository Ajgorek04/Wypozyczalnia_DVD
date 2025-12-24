package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {

    private UserRepository userRepo;

    @BeforeEach
    void setUp() throws Exception {
        TestDatabaseSetup.initDatabase();
        userRepo = new UserRepository();

        userRepo.registerUser("testuser", "testpass");
    }

    @Test
    void shouldRegisterNewUser() {
        boolean result = userRepo.registerUser("nowy", "haslo123");
        assertTrue(result, "Rejestracja powinna się udać");
    }

    @Test
    void shouldNotRegisterExistingUser() {
        boolean result = userRepo.registerUser("testuser", "innehaslo");
        assertFalse(result, "Nie powinno dać się zarejestrować zajętego loginu");
    }

    @Test
    void shouldAuthenticateValidUser() {
        int userId = userRepo.getUserIdByCredentials("testuser", "testpass");
        assertTrue(userId > 0, "Logowanie poprawne powinno zwrócić ID > 0");
    }

    @Test
    void shouldNotAuthenticateInvalidPass() {
        int userId = userRepo.getUserIdByCredentials("testuser", "zlehaslo");
        assertEquals(-1, userId, "Złe hasło powinno zwrócić -1");
    }
}