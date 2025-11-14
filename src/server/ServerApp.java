package server;

import java.io.*;
import java.net.*;

public class ServerApp {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(5000);
            System.out.println("Serwer uruchomiony. Oczekiwanie na klienta...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nowy klient połączony!");

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                PrintWriter out = new PrintWriter(
                        clientSocket.getOutputStream(), true);

                String msg = in.readLine();
                System.out.println("Otrzymano: " + msg);

                out.println("Serwer: aktywny -> " + msg);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
