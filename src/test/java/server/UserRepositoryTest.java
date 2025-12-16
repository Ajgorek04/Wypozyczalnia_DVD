// java
package server;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryExtraTest {

    private static UserRepository repo;

    @BeforeAll
    static void init() { repo = new UserRepository(); }

    @Test
    void rejectNullAndBlankRegistration() {
        assertFalse(repo.registerUser(null, "p"));
        assertFalse(repo.registerUser("u", null));
        assertFalse(repo.registerUser("   ", "p"));
        assertFalse(repo.registerUser("u2", "   "));
    }

    @Test
    void duplicateRegistrationFails() {
        String name = "dup_" + System.nanoTime();
        assertTrue(repo.registerUser(name, "p"));
        assertFalse(repo.registerUser(name, "p"), "Powinna być odrzucona rejestracja z tym samym username");
    }
}
