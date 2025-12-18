package server;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class TestDatabaseSetup {

    // Konfiguracja H2 w trybie emulacji MySQL
    private static final String H2_URL = "jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE";
    private static final String H2_USER = "sa";
    private static final String H2_PASS = "";

    public static void initDatabase() throws Exception {
        // 1. Zamiast refleksji, używamy normalnej metody settera
        Database.setConnectionDetails(H2_URL, H2_USER, H2_PASS);

        // 2. Wczytanie init.sql i wykonanie go w bazie H2
        try (Connection conn = DriverManager.getConnection(H2_URL, H2_USER, H2_PASS);
             Statement stmt = conn.createStatement()) {

            // Czyścimy bazę przed każdym testem
            stmt.execute("DROP ALL OBJECTS");

            InputStream is = TestDatabaseSetup.class.getResourceAsStream("/init.sql");
            if (is == null) throw new RuntimeException("Nie znaleziono pliku init.sql w resources!");

            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Dzielimy zapytania po średnikach
            String[] queries = sql.split(";");
            for (String query : queries) {
                if (!query.trim().isEmpty()) {
                    stmt.execute(query.trim());
                }
            }
        }
    }
}