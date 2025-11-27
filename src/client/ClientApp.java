package client;

import java.io.*;
import java.net.*;

public class ClientApp {
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