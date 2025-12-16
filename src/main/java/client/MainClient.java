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
        frame.setSize(520, 360);
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
        JButton myTransBtn = new JButton("Moje transakcje / opłaty");
        JButton payBtn = new JButton("Zapłać opłatę");
        JButton returnBtn = new JButton("Zwróć film");
        JButton logoutBtn = new JButton("Wyloguj");

        for (JComponent c : new JComponent[]{moviesBtn, rentBtn, myRentsBtn, myTransBtn, payBtn, returnBtn, logoutBtn}) {
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
                // include only available films (contains "Dostępny: true")
                if (l.contains("Dostępny: true") || l.contains("Dostępny: true")) {
                    Matcher m = idPattern.matcher(l);
                    if (m.find()) {
                        String id = m.group(1);
                        // display "id - title..."
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

        payBtn.addActionListener(e -> {
            if (loggedUserId < 0) { JOptionPane.showMessageDialog(frame, "Zaloguj się najpierw"); return; }

            List<String> lines = sendCommandLines("MY_TRANS;" + loggedUserId);
            if (lines == null) { JOptionPane.showMessageDialog(frame, "Błąd połączenia z serwerem"); return; }

            List<String> unpaid = new ArrayList<>();
            Pattern p = Pattern.compile("Opłata\\s+(\\d+)\\s+\\|.*kwota:\\s*([0-9]+\\.?[0-9]*)\\s+\\|\\s*powód:\\s*(.*?)\\s+\\|\\s*Opłacona:\\s*(NIE|TAK)");
            for (String l : lines) {
                Matcher m = p.matcher(l);
                if (m.find()) {
                    String status = m.group(4);
                    if ("NIE".equalsIgnoreCase(status)) {
                        String item = m.group(1) + " - " + m.group(2) + " zł - " + m.group(3);
                        unpaid.add(item);
                    }
                }
            }

            if (unpaid.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Brak nieopłaconych opłat.");
                return;
            }

            JComboBox<String> combo = new JComboBox<>(unpaid.toArray(new String[0]));
            int res = JOptionPane.showConfirmDialog(frame, combo, "Wybierz opłatę do zapłacenia", JOptionPane.OK_CANCEL_OPTION);
            if (res != JOptionPane.OK_OPTION) return;

            String selected = (String) combo.getSelectedItem();
            if (selected == null) return;

            String[] parts = selected.split("\\s*-\\s*");
            if (parts.length < 2) { JOptionPane.showMessageDialog(frame, "Błąd parsowania opłaty"); return; }
            int oplataId;
            double kwota;
            try {
                oplataId = Integer.parseInt(parts[0].trim());
                String kw = parts[1].trim().replace("zł", "").trim();
                kwota = Double.parseDouble(kw);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Błąd parsowania kwoty/id");
                return;
            }

            String reply = sendCommand("PAY;" + oplataId + ";" + kwota + ";" + loggedUserId);
            JOptionPane.showMessageDialog(frame, "Serwer: " + reply);
        });

        returnBtn.addActionListener(e -> {
            if (loggedUserId < 0) { JOptionPane.showMessageDialog(frame, "Zaloguj się najpierw"); return; }

            List<String> lines = sendCommandLines("MY_RENTS;" + loggedUserId);
            if (lines == null) { JOptionPane.showMessageDialog(frame, "Błąd połączenia z serwerem"); return; }

            List<String> active = new ArrayList<>();
            Pattern idPattern = Pattern.compile("^(\\d+)\\.");
            for (String l : lines) {
                // tylko te niezwrocone (Zwrócono: null)
                if (l.contains("Zwrócono: null")) {
                    Matcher m = idPattern.matcher(l);
                    if (m.find()) {
                        String id = m.group(1);
                        String display = id + " - " + l.substring(l.indexOf(".") + 1).trim();
                        active.add(display);
                    }
                }
            }

            if (active.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Brak wypożyczeń do zwrotu.");
                return;
            }

            JComboBox<String> combo = new JComboBox<>(active.toArray(new String[0]));
            int res = JOptionPane.showConfirmDialog(frame, combo, "Wybierz film do zwrotu", JOptionPane.OK_CANCEL_OPTION);
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

            String reply = sendCommand("RETURN;" + filmId + ";" + loggedUserId);
            if (reply != null && reply.startsWith("RETURN_FAIL;UNPAID")) {
                JOptionPane.showMessageDialog(frame, "Nie można zwrócić - są nieopłacone opłaty");
            } else {
                JOptionPane.showMessageDialog(frame, "Serwer: " + reply);
            }
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
        try (Socket socket = new Socket("127.0.0.1", 5000);
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
        try (Socket socket = new Socket("127.0.0.1", 5000);
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
