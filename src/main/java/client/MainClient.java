package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainClient {

    private static int loggedUserId = -1;
    // Ustaw port zgodny z serwerem
    private static final int PORT = 5000;
    private static final String HOST = "127.0.0.1";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame mainFrame = buildMainFrame();
            mainFrame.setVisible(true);
        });
    }

    private static JFrame buildMainFrame() {
        JFrame frame = new JFrame("Wypożyczalnia DVD - Klient");
        frame.setSize(520, 360);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Wypożyczalnia Płyt DVD");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        panel.add(title);

        JButton loginBtn = new JButton("Zaloguj się");
        JButton registerBtn = new JButton("Zarejestruj się");
        JButton moviesBtn = new JButton("Lista Filmów");
        JButton exitBtn = new JButton("Wyjście");

        for (JComponent c : new JComponent[]{loginBtn, registerBtn, moviesBtn, exitBtn}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(c);
            panel.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        frame.add(panel);

        exitBtn.addActionListener(e -> System.exit(0));

        loginBtn.addActionListener(e -> {
            JPanel p = new JPanel(new GridLayout(2,2));
            JTextField userField = new JTextField();
            JPasswordField passField = new JPasswordField();
            p.add(new JLabel("Użytkownik:"));
            p.add(userField);
            p.add(new JLabel("Hasło:"));
            p.add(passField);

            int result = JOptionPane.showConfirmDialog(frame, p, "Logowanie", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String user = userField.getText();
                String pass = new String(passField.getPassword());
                String reply = sendCommand("LOGIN;" + user + ";" + pass);
                if (reply != null && reply.startsWith("LOGIN_OK;")) {
                    String[] parts = reply.split(";", 2);
                    loggedUserId = Integer.parseInt(parts[1]);
                    frame.setVisible(false);
                    JFrame dashboard = buildDashboardFrame(frame);
                    dashboard.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(frame, "Błąd logowania");
                }
            }
        });

        registerBtn.addActionListener(e -> {
            JPanel p = new JPanel(new GridLayout(2,2));
            JTextField userField = new JTextField();
            JPasswordField passField = new JPasswordField();
            p.add(new JLabel("Użytkownik:"));
            p.add(userField);
            p.add(new JLabel("Hasło:"));
            p.add(passField);

            int result = JOptionPane.showConfirmDialog(frame, p, "Rejestracja", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String user = userField.getText();
                String pass = new String(passField.getPassword());
                String reply = sendCommand("REGISTER;" + user + ";" + pass);
                if ("REGISTER_OK".equals(reply)) {
                    JOptionPane.showMessageDialog(frame, "Zarejestrowano");
                } else {
                    JOptionPane.showMessageDialog(frame, "Rejestracja nie powiodła się");
                }
            }
        });

        moviesBtn.addActionListener(e -> {
            JFrame moviesFrame = new JFrame("Lista Filmów");
            moviesFrame.setSize(500, 300);
            moviesFrame.setLocationRelativeTo(frame);
            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            moviesFrame.add(new JScrollPane(textArea));
            moviesFrame.setVisible(true);

            List<String> lines = sendCommandLines("GET_FILMS");
            if (lines == null) {
                textArea.setText("Błąd połączenia z serwerem");
            } else {
                for (String l : lines) textArea.append(l + "\n");
            }
        });

        return frame;
    }

    private static JFrame buildDashboardFrame(JFrame mainFrame) {
        JFrame frame = new JFrame("Panel użytkownika - ID: " + loggedUserId);
        frame.setSize(520, 420); // Trochę większe okno, bo więcej przycisków
        frame.setLocationRelativeTo(mainFrame);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Panel użytkownika");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(20,0,20,0));
        panel.add(title);

        JButton moviesBtn = new JButton("Lista Filmów");
        JButton rentBtn = new JButton("Wypożycz film");
        JButton myRentsBtn = new JButton("Moje wypożyczenia");

        // PRZYWRÓCONE: Przycisk do podglądu historii
        JButton myTransBtn = new JButton("Moje transakcje / opłaty");

        // NOWE: Przycisk akcji (płatność + zwrot)
        JButton payAndReturnBtn = new JButton("Zapłać i zwróć film");

        JButton logoutBtn = new JButton("Wyloguj");

        // Dodajemy wszystkie przyciski do panelu
        for (JComponent c : new JComponent[]{moviesBtn, rentBtn, myRentsBtn, myTransBtn, payAndReturnBtn, logoutBtn}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(c);
            panel.add(Box.createRigidArea(new Dimension(0,8)));
        }

        frame.add(panel);

        moviesBtn.addActionListener(e -> {
            JFrame moviesFrame = new JFrame("Lista Filmów");
            moviesFrame.setSize(500, 300);
            moviesFrame.setLocationRelativeTo(frame);
            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            moviesFrame.add(new JScrollPane(textArea));
            moviesFrame.setVisible(true);

            List<String> lines = sendCommandLines("GET_FILMS");
            if (lines == null) textArea.setText("Błąd połączenia z serwerem");
            else for (String l : lines) textArea.append(l + "\n");
        });

        rentBtn.addActionListener(e -> {
            if (loggedUserId < 0) { JOptionPane.showMessageDialog(frame, "Zaloguj się najpierw"); return; }

            List<String> lines = sendCommandLines("GET_FILMS");
            if (lines == null) { JOptionPane.showMessageDialog(frame, "Błąd połączenia z serwerem"); return; }

            List<String> available = new ArrayList<>();
            Pattern idPattern = Pattern.compile("^(\\d+)\\.");
            for (String l : lines) {
                // Szukamy filmów, które są dostępne
                if (l.contains("Dostępny: true") || l.contains("Dostepny: true")) {
                    Matcher m = idPattern.matcher(l);
                    if (m.find()) {
                        String id = m.group(1);
                        String display = id + " - " + l.substring(l.indexOf(".") + 1).trim();
                        available.add(display);
                    }
                }
            }

            if (available.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Brak dostępnych filmów do wypożyczenia.");
                return;
            }

            JComboBox<String> combo = new JComboBox<>(available.toArray(new String[0]));
            int res = JOptionPane.showConfirmDialog(frame, combo, "Wybierz film do wypożyczenia", JOptionPane.OK_CANCEL_OPTION);
            if (res != JOptionPane.OK_OPTION) return;

            String selected = (String) combo.getSelectedItem();
            if (selected == null) return;
            int filmId;
            try {
                filmId = Integer.parseInt(selected.split("\\s*-\\s*", 2)[0].trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Błąd parsowania id filmu");
                return;
            }

            String reply = sendCommand("RENT;" + filmId + ";" + loggedUserId);
            JOptionPane.showMessageDialog(frame, "Serwer: " + reply);
        });

        myRentsBtn.addActionListener(e -> {
            if (loggedUserId < 0) { JOptionPane.showMessageDialog(frame, "Zaloguj się najpierw"); return; }
            JFrame f = new JFrame("Moje wypożyczenia");
            f.setSize(500, 300);
            f.setLocationRelativeTo(frame);
            JTextArea ta = new JTextArea(); ta.setEditable(false);
            f.add(new JScrollPane(ta));
            f.setVisible(true);

            List<String> lines = sendCommandLines("MY_RENTS;" + loggedUserId);
            if (lines == null) {
                ta.setText("Błąd połączenia z serwerem");
            } else if (lines.isEmpty()) {
                ta.setText("Brak wypożyczeń.");
            } else {
                for (String l : lines) ta.append(l + "\n");
            }
        });

        // PRZYWRÓCONA OBSŁUGA PRZYCISKU "MOJE TRANSAKCJE"
        myTransBtn.addActionListener(e -> {
            if (loggedUserId < 0) { JOptionPane.showMessageDialog(frame, "Zaloguj się najpierw"); return; }
            JFrame f = new JFrame("Moje transakcje / opłaty");
            f.setSize(500, 300);
            f.setLocationRelativeTo(frame);
            JTextArea ta = new JTextArea(); ta.setEditable(false);
            f.add(new JScrollPane(ta));
            f.setVisible(true);

            List<String> lines = sendCommandLines("MY_TRANS;" + loggedUserId);
            if (lines == null) {
                ta.setText("Błąd połączenia z serwerem");
            } else if (lines.isEmpty()) {
                ta.setText("Brak transakcji / opłat.");
            } else {
                for (String l : lines) ta.append(l + "\n");
            }
        });

        // GŁÓWNY PRZYCISK DO ZWROTU I PŁATNOŚCI
        payAndReturnBtn.addActionListener(e -> {
            if (loggedUserId < 0) { JOptionPane.showMessageDialog(frame, "Zaloguj się najpierw"); return; }

            List<String> lines = sendCommandLines("MY_TRANS;" + loggedUserId);
            if (lines == null) { JOptionPane.showMessageDialog(frame, "Błąd połączenia z serwerem"); return; }

            List<String> unpaid = new ArrayList<>();
            // Regex obsługujący kropki i "zł"
            Pattern p = Pattern.compile("Opłata\\s+(\\d+)\\s+\\|.*kwota:\\s*([0-9]+\\.?[0-9]*).*?\\|\\s*powód:\\s*(.*?)\\s+\\|\\s*Opłacona:\\s*(NIE|TAK)");

            for (String l : lines) {
                Matcher m = p.matcher(l);
                if (m.find()) {
                    String status = m.group(4);
                    // Pokazujemy tylko te NIEOPŁACONE
                    if ("NIE".equalsIgnoreCase(status)) {
                        String item = "Opłata ID: " + m.group(1) + " | Kwota: " + m.group(2) + " zł | Za: " + m.group(3);
                        unpaid.add(item);
                    }
                }
            }

            if (unpaid.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Brak opłat do uregulowania (Wszystkie filmy zwrócone i opłacone).");
                return;
            }

            JComboBox<String> combo = new JComboBox<>(unpaid.toArray(new String[0]));
            int res = JOptionPane.showConfirmDialog(frame, combo, "Wybierz rachunek do opłacenia (Spowoduje zwrot filmu)", JOptionPane.OK_CANCEL_OPTION);
            if (res != JOptionPane.OK_OPTION) return;

            String selected = (String) combo.getSelectedItem();
            if (selected == null) return;

            Pattern idExtract = Pattern.compile("ID:\\s*(\\d+)");
            Matcher mId = idExtract.matcher(selected);
            int oplataId = -1;
            if (mId.find()) {
                oplataId = Integer.parseInt(mId.group(1));
            } else {
                JOptionPane.showMessageDialog(frame, "Błąd parsowania ID opłaty");
                return;
            }

            // Kwota nie jest ważna w żądaniu, serwer sam ją wyliczy na podstawie czasu
            double kwota = 0.0;

            String reply = sendCommand("PAY;" + oplataId + ";" + kwota + ";" + loggedUserId);
            JOptionPane.showMessageDialog(frame, "Serwer: " + reply);
        });

        logoutBtn.addActionListener(e -> {
            loggedUserId = -1;
            frame.dispose();
            mainFrame.setVisible(true);
        });

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                mainFrame.setVisible(true);
            }
        });

        return frame;
    }

    private static String sendCommand(String cmd) {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println(cmd);
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<String> sendCommandLines(String cmd) {
        List<String> result = new ArrayList<>();
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println(cmd);
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                result.add(line);
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}