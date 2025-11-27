// java
package server;

import java.io.*;
import java.net.*;
import java.util.List;

public class ServerApp {
    public static void main(String[] args) {

        FilmRepository repo = new FilmRepository();
        UserRepository userRepo = new UserRepository();
        TransactionRepository txRepo = new TransactionRepository();

        System.out.println("---- Lista filmów z bazy ----");
        repo.getAllFilms().forEach(System.out::println);
        System.out.println("------------------------------");

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Serwer uruchomiony. Oczekiwanie na klienta...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nowy klient połączony!");
                new Thread(() -> handleClient(clientSocket, repo, userRepo, txRepo)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket, FilmRepository repo, UserRepository userRepo, TransactionRepository txRepo) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String msg = in.readLine();
            System.out.println("Otrzymano: " + msg);
            if (msg == null) return;

            if ("GET_FILMS".equalsIgnoreCase(msg)) {
                List<String> films = repo.getAllFilms();
                for (String f : films) out.println(f);
                out.println("END");

            } else if (msg.startsWith("REGISTER;")) {
                String[] parts = msg.split(";", 3);
                if (parts.length == 3 && userRepo.registerUser(parts[1], parts[2])) {
                    out.println("REGISTER_OK");
                } else {
                    out.println("REGISTER_FAIL");
                }

            } else if (msg.startsWith("LOGIN;")) {
                String[] parts = msg.split(";", 3);
                if (parts.length == 3) {
                    int userId = userRepo.getUserIdByCredentials(parts[1], parts[2]);
                    if (userId >= 0) out.println("LOGIN_OK;" + userId);
                    else out.println("LOGIN_FAIL");
                } else {
                    out.println("LOGIN_FAIL");
                }

            } else if (msg.startsWith("RENT;")) {
                // RENT;filmId;userId
                String[] parts = msg.split(";", 3);
                if (parts.length == 3) {
                    int filmId = Integer.parseInt(parts[1]);
                    int userId = Integer.parseInt(parts[2]);
                    out.println(txRepo.rentFilm(userId, filmId) ? "RENT_OK" : "RENT_FAIL");
                } else out.println("RENT_FAIL");

            } else if (msg.startsWith("RETURN;")) {
                // RETURN;filmId;userId
                String[] parts = msg.split(";", 3);
                if (parts.length == 3) {
                    int filmId = Integer.parseInt(parts[1]);
                    int userId = Integer.parseInt(parts[2]);
                    out.println(txRepo.returnFilm(userId, filmId) ? "RETURN_OK" : "RETURN_FAIL");
                } else out.println("RETURN_FAIL");

            } else if (msg.startsWith("MY_RENTS;")) {
                // MY_RENTS;userId
                String[] parts = msg.split(";", 2);
                if (parts.length == 2) {
                    int userId = Integer.parseInt(parts[1]);
                    List<String> rents = txRepo.getUserRentals(userId);
                    for (String r : rents) out.println(r);
                    out.println("END");
                } else out.println("END");

            } else if (msg.startsWith("MY_TRANS;")) {
                // MY_TRANS;userId
                String[] parts = msg.split(";", 2);
                if (parts.length == 2) {
                    int userId = Integer.parseInt(parts[1]);
                    List<String> trans = txRepo.getUserTransactions(userId);
                    for (String t : trans) out.println(t);
                    out.println("END");
                } else out.println("END");

            } else if (msg.startsWith("PAY;")) {
                // PAY;oplataId;amount;userId
                String[] parts = msg.split(";", 4);
                if (parts.length == 4) {
                    int oplataId = Integer.parseInt(parts[1]);
                    double amount = Double.parseDouble(parts[2]);
                    int userId = Integer.parseInt(parts[3]);
                    out.println(txRepo.payCharge(userId, oplataId, amount) ? "PAY_OK" : "PAY_FAIL");
                } else out.println("PAY_FAIL");

            } else {
                out.println("Nieznane polecenie");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
