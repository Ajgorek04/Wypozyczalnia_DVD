// java
package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/WypozyczalniaPlytDVD";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // wpisz swoje hasło jeśli masz

    public static Connection connect() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("Błąd połączenia z bazą danych: " + e.getMessage());
            return null;
        }
    }
}
