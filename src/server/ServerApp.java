package server;

import java.io.*;
import java.net.*;
import java.util.List;

public class ServerApp {
    public static void main(String[] args) {

        // Test pobrania filmów przy starcie
        FilmRepository repo = new FilmRepository();
        System.out.println("---- Lista filmów z bazy ----");
        repo.getAllFilms().forEach(System.out::println);
        System.out.println("------------------------------");

        try {
            ServerSocket serverSocket = new ServerSocket(5000);
            System.out.println("Serwer uruchomiony. Oczekiwanie na klienta...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nowy klient połączony!");

                new Thread(() -> handleClient(clientSocket, repo)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket, FilmRepository repo) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String msg = in.readLine();
            System.out.println("Otrzymano: " + msg);

            if ("GET_FILMS".equalsIgnoreCase(msg)) {
                List<String> films = repo.getAllFilms();
                for (String f : films) {
                    out.println(f);
                }
                out.println("END"); // znacznik końca listy
            } else {
                out.println("Nieznane polecenie");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
