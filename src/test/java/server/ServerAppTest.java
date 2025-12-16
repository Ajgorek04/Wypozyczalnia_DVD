// java
// Plik: `src/test/java/server/ServerAppTest.java`
// Zmiana: ServerSocket(0) i użycie rzeczywistego portu klienta
package server;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

class ServerAppTest {

    @Test
    void handleClient_getFilms_overSocket() throws Exception {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();

            Future<Void> serverTask = ex.submit(() -> {
                try (Socket accepted = ss.accept()) {
                    // wywołaj prywatną metodę handleClient refleksyjnie
                    Method m = ServerApp.class.getDeclaredMethod("handleClient", Socket.class, FilmRepository.class, UserRepository.class, TransactionRepository.class);
                    m.setAccessible(true);
                    m.invoke(null, accepted, new FilmRepository(), new UserRepository(), new TransactionRepository());
                }
                return null;
            });

            // klient
            try (Socket s = new Socket("127.0.0.1", port);
                 PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))
            ) {
                out.println("GET_FILMS");
                String line;
                boolean sawEnd = false;
                boolean sawLine = false;
                while ((line = in.readLine()) != null) {
                    if ("END".equals(line)) { sawEnd = true; break; }
                    sawLine = true;
                }
                assertTrue(sawLine, "Serwer nie odesłał żadnej linii z filmami");
                assertTrue(sawEnd, "Serwer nie wysłał END");
            }

            serverTask.get(5, TimeUnit.SECONDS);
        } finally {
            ex.shutdownNow();
        }
    }
}
