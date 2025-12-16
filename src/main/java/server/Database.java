// java
package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.io.IOException;

public class Database {

    private static final String DEFAULT_URL = "jdbc:mysql://127.0.0.1:3306/WypozyczalniaPlytDVD";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = ""; // wpisz swoje hasło jeśli masz

    // zabezpieczenie przed wielokrotnym inicjowaniem (dla in-memory DB)
    private static volatile boolean initialized = false;

    public static Connection connect() {
        String url = System.getProperty("jdbc.url", DEFAULT_URL);
        String user = System.getProperty("jdbc.user", DEFAULT_USER);
        String pass = System.getProperty("jdbc.pass", DEFAULT_PASSWORD);
        String initPath = System.getProperty("jdbc.init"); // opcjonalny skrypt SQL do uruchomienia

        try {
            Connection conn = DriverManager.getConnection(url, user, pass);

            if (initPath != null && !initialized) {
                synchronized (Database.class) {
                    if (!initialized) {
                        try {
                            executeSqlScript(conn, initPath);
                            initialized = true;
                            System.out.println("Baza inicjalizowana ze skryptu: " + initPath);
                        } catch (IOException e) {
                            System.err.println("Nie można odczytać skryptu inicjalizującego: " + e.getMessage());
                        }
                    }
                }
            }

            return conn;
        } catch (SQLException e) {
            System.err.println("Błąd połączenia z bazą danych: " + e.getMessage());
            return null;
        }
    }

    private static void executeSqlScript(Connection conn, String scriptPath) throws IOException, SQLException {
        String content = Files.lines(Path.of(scriptPath)).collect(Collectors.joining("\n"));
        // prosty split po średniku; zakładamy, że skrypt używa ';' do zakończeń poleceń
        String[] statements = content.split(";");
        try (Statement stmt = conn.createStatement()) {
            for (String raw : statements) {
                String sql = raw.trim();
                if (sql.isEmpty()) continue;
                stmt.execute(sql);
            }
        }
    }
}
