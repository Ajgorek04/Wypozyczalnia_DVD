package server;

import org.junit.jupiter.api.*;
import java.nio.file.Paths;

/*
 Krótko: ta klasa ustawia właściwości JDBC, aby testy używały H2 in-memory
 oraz wskazuje skrypt inicjalizujący w src/test/resources/init.sql.
*/
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestDatabaseSetup {

    @BeforeAll
    public void init() {
        System.setProperty("jdbc.url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MySQL");
        System.setProperty("jdbc.user", "sa");
        System.setProperty("jdbc.pass", "");
        System.setProperty("jdbc.init", Paths.get("src", "test", "resources", "init.sql").toAbsolutePath().toString());
    }
}
