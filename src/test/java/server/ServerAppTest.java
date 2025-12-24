package server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class ServerAppTest {

    private ExecutorService executor;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        TestDatabaseSetup.initDatabase();
        executor = Executors.newSingleThreadExecutor();

        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void shouldHandleLoginAndGetFilms() throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);

        executor.submit(() -> {
            try {
                Socket clientSocket = serverSocket.accept();

                Class<?>[] declaredClasses = ServerApp.class.getDeclaredClasses();
                Class<?> handlerClass = null;
                for (Class<?> c : declaredClasses) {
                    if (c.getName().endsWith("ClientHandler")) {
                        handlerClass = c;
                        break;
                    }
                }

                if (handlerClass != null) {
                    Constructor<?> ctor = handlerClass.getDeclaredConstructor(Socket.class);
                    ctor.setAccessible(true);
                    Runnable handler = (Runnable) ctor.newInstance(clientSocket);
                    handler.run();
                }
                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        try (Socket socket = new Socket("localhost", port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("REGISTER;test;test");
            assertEquals("REGISTER_OK", in.readLine());

            out.println("LOGIN;test;test");
            String loginResp = in.readLine();
            assertTrue(loginResp.startsWith("LOGIN_OK"), "Logowanie powinno się udać");

            out.println("GET_FILMS");
            String line = in.readLine();
            assertNotNull(line);
            assertTrue(line.contains("Matrix"), "Powinien zwrócić pierwszy film");

            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
            }
        }
    }
}