package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final Logger logger = LogManager.getLogger(Database.class);
    // Zmieniłem nazwę bazy na zgodną z Twoim SQL
    private static final String URL = "jdbc:mysql://localhost:3306/WypozyczalniaPlytDVD";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // <-- WPISZ HASŁO JEŚLI MASZ

    public static Connection connect() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            logger.debug("Połączono z bazą: " + URL);
            return conn;
        } catch (SQLException e) {
            logger.error("Błąd połączenia z bazą! Sprawdź czy baza 'WypozyczalniaPlytDVD' istnieje.", e);
            return null;
        }
    }
}