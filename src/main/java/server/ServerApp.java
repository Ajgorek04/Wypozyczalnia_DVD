package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.List;

public class ServerApp {
    private static final Logger logger = LogManager.getLogger(ServerApp.class);
    private static final int PORT = 5000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("========================================");
            logger.info(" Serwer Wypożyczalni DVD (ADMIN ENABLED)");
            logger.info(" Port: " + PORT);
            logger.info("========================================");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket)).start();
                } catch (IOException e) {
                    logger.error("Błąd akceptacji klienta", e);
                }
            }
        } catch (IOException e) {
            logger.fatal("Nie można uruchomić serwera na porcie " + PORT, e);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final UserRepository userRepo;
        private final FilmRepository filmRepo;
        private final TransactionRepository transRepo;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.userRepo = new UserRepository();
            this.filmRepo = new FilmRepository();
            this.transRepo = new TransactionRepository();
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String request;
                while ((request = in.readLine()) != null) {
                    logger.info("[Komenda] " + request);
                    String[] parts = request.split(";");
                    String command = parts[0];

                    try {
                        switch (command) {
                            case "LOGIN":
                                if (parts.length < 3) { out.println("LOGIN_FAIL"); break; }
                                int userId = userRepo.getUserIdByCredentials(parts[1], parts[2]);
                                out.println(userId >= 0 ? "LOGIN_OK;" + userId : "LOGIN_FAIL");
                                break;

                            case "REGISTER":
                                if (parts.length < 3) { out.println("REGISTER_FAIL"); break; }
                                boolean regOk = userRepo.registerUser(parts[1], parts[2]);
                                out.println(regOk ? "REGISTER_OK" : "REGISTER_FAIL");
                                break;

                            case "GET_FILMS":
                                List<String> films = filmRepo.getAllFilmsFormatted();
                                for (String f : films) out.println(f);
                                out.println("END");
                                break;

                            case "RENT":
                                if (parts.length < 3) { out.println("Błąd danych"); break; }
                                String rentResult = transRepo.rentFilm(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]));
                                out.println(rentResult);
                                break;

                            case "MY_TRANS":
                                if (parts.length < 2) { out.println("END"); break; }
                                List<String> trans = transRepo.getUserTransactions(Integer.parseInt(parts[1]));
                                for (String t : trans) out.println(t);
                                out.println("END");
                                break;

                            case "PAY":
                                if (parts.length < 2) { out.println("Błąd"); break; }
                                String payResult = transRepo.payTransaction(Integer.parseInt(parts[1]));
                                out.println(payResult);
                                break;

                            case "MY_RENTS":
                                if (parts.length < 2) { out.println("END"); break; }
                                List<String> rents = transRepo.getUserRentals(Integer.parseInt(parts[1]));
                                for (String r : rents) out.println(r);
                                out.println("END");
                                break;


                            case "ADMIN_GET_USERS":
                                List<String> users = userRepo.getAllUsers();
                                for (String u : users) out.println(u);
                                out.println("END");
                                break;

                            case "ADMIN_DEL_USER":
                                if (parts.length < 2) { out.println("ERROR"); break; }
                                boolean delOk = userRepo.deleteUser(Integer.parseInt(parts[1]));
                                out.println(delOk ? "Usunięto pomyślnie" : "Błąd usuwania (może użytkownik nie istnieje?)");
                                break;

                            case "ADMIN_PASS":
                                if (parts.length < 3) { out.println("ERROR"); break; }
                                boolean passOk = userRepo.changeUserPassword(Integer.parseInt(parts[1]), parts[2]);
                                out.println(passOk ? "Hasło zmienione" : "Błąd zmiany hasła");
                                break;

                            default:
                                out.println("UNKNOWN_COMMAND");
                                break;
                        }
                    } catch (Exception e) {
                        logger.error("Błąd przetwarzania komendy: " + command, e);
                        out.println("ERROR");
                    }
                }
            } catch (IOException e) {
            } finally {
                try { socket.close(); } catch (IOException e) {}
            }
        }
    }
}