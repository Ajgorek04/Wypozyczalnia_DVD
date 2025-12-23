package client;

import java.io.*;
import java.net.*;

/**
 * Prosta aplikacja konsolowa służąca do testowania połączenia z serwerem.
 * <p>
 * Klasa ta nawiązuje połączenie z serwerem na localhost na porcie 5000,
 * wysyła jeden komunikat testowy i oczekuje na odpowiedź.
 * Służy głównie do celów diagnostycznych, aby sprawdzić, czy serwer nasłuchuje.
 * </p>
 *
 * @author Twój Zespół
 * @version 1.0
 */
public class ClientApp {

    /**
     * Prywatny konstruktor zapobiegający instancjalizacji klasy narzędziowej.
     */
    private ClientApp() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Główna metoda uruchomieniowa aplikacji testowej.
     * <p>
     * Tworzy gniazdo (Socket), strumienie wejścia/wyjścia i przeprowadza
     * prostą wymianę komunikatów z serwerem.
     * </p>
     *
     * @param args Argumenty wiersza poleceń (nie są wykorzystywane w tej aplikacji).
     */
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 5000);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            out.println("Serwer działa poprawnie!");
            String response = in.readLine();

            System.out.println("Odpowiedź serwera: " + response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}