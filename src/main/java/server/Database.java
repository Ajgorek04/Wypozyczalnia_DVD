package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final Logger logger = LogManager.getLogger(Database.class);

    private static String URL = "jdbc:mysql://localhost:3306/WypozyczalniaPlytDVD";
    private static String USER = "root";
    private static String PASSWORD = "";

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

    public static void setConnectionDetails(String url, String user, String password) {
        URL = url;
        USER = user;
        PASSWORD = password;
    }
}