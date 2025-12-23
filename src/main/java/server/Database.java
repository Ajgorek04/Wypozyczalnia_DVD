package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Klasa narzędziowa odpowiedzialna za zarządzanie połączeniem z bazą danych.
 * <p>
 * Przechowuje parametry konfiguracyjne (URL, użytkownik, hasło) i udostępnia
 * statyczną metodę fabrykującą połączenia JDBC. Klasa umożliwia również
 * dynamiczną zmianę konfiguracji, co jest wykorzystywane w środowisku testowym.
 * </p>
 *
 * @author Igor Błędzińkis, Łukasz Gierczak
 * @version 1.0
 */
public class Database {

    /** Logger log4j do rejestrowania zdarzeń związanych z bazą danych. */
    private static final Logger logger = LogManager.getLogger(Database.class);

    /** Adres URL połączenia JDBC (domyślnie lokalna baza MySQL). */
    private static String URL = "jdbc:mysql://localhost:3306/WypozyczalniaPlytDVD";

    /** Nazwa użytkownika bazy danych. */
    private static String USER = "root";

    /** Hasło użytkownika bazy danych. */
    private static String PASSWORD = "";

    /**
     * Prywatny konstruktor zapobiegający instancjalizacji klasy narzędziowej.
     */
    private Database() {
        throw new IllegalStateException("Klasa narzędziowa - nie należy tworzyć instancji.");
    }

    /**
     * Nawiązuje nowe połączenie z bazą danych.
     * <p>
     * Wykorzystuje sterownik {@link DriverManager} do utworzenia sesji z bazą.
     * W przypadku powodzenia loguje komunikat na poziomie DEBUG.
     * W przypadku błędu (np. brak bazy, złe hasło) loguje wyjątek na poziomie ERROR i zwraca null.
     * </p>
     *
     * @return Obiekt {@link Connection} reprezentujący aktywne połączenie lub {@code null}, jeśli wystąpił błąd.
     */
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

    /**
     * Ustawia parametry połączenia z bazą danych.
     * <p>
     * Metoda ta jest kluczowa dla testów jednostkowych, gdzie podmieniamy
     * produkcyjną bazę MySQL na testową bazę w pamięci (H2).
     * </p>
     *
     * @param url      Nowy adres URL JDBC (np. jdbc:h2:mem:testdb).
     * @param user     Nowa nazwa użytkownika.
     * @param password Nowe hasło.
     */
    public static void setConnectionDetails(String url, String user, String password) {
        URL = url;
        USER = user;
        PASSWORD = password;
    }
}