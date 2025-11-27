//java
package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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

        // Logowanie -> przy sukcesie otwiera dashboard i ukrywa okno główne
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
                    try {
                        loggedUserId = Integer.parseInt(reply.split(";",2)[1]);
                    } catch (Exception ex) { loggedUserId = -1; }
                    JOptionPane.showMessageDialog(frame, "Zalogowano pomyślnie. ID: " + loggedUserId);
                    // otwórz dashboard
                    JFrame dash = buildDashboardFrame(frame);
                    frame.setVisible(false);
                    dash.setVisible(true);
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
                    JOptionPane.showMessageDialog(frame, "Rejestracja zakończona sukcesem");
                } else {
                    JOptionPane.showMessageDialog(frame, "Rejestracja nie powiodła się (użytkownik może już istnieć)");
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
        JButton returnBtn = new JButton("Zwróć film");
        JButton myRentsBtn = new JButton("Moje wypożyczenia");
        JButton myTransBtn = new JButton("Moje transakcje / opłaty");
        JButton payBtn = new JButton("Zapłać opłatę");
        JButton logoutBtn = new JButton("Wyloguj");

        for (JComponent c : new JComponent[]{moviesBtn, rentBtn, returnBtn, myRentsBtn, myTransBtn, payBtn, logoutBtn}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(c);
            panel.add(Box.createRigidArea(new Dimension(0,8)));
        }

        frame.add(panel);

        // działania podobne do poprzednich, ale korzystające ze stanu loggedUserId
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
            String fid = JOptionPane.showInputDialog(frame, "Podaj id filmu do wypożyczenia:");
            if (fid == null || fid.isBlank()) return;
            String reply = sendCommand("RENT;" + fid + ";" + loggedUserId);
            JOptionPane.showMessageDialog(frame, "Serwer: " + reply);
        });

        returnBtn.addActionListener(e -> {
            if (loggedUserId < 0) { JOptionPane.showMessageDialog(frame, "Zaloguj się najpierw"); return; }
            String fid = JOptionPane.showInputDialog(frame, "Podaj id filmu do zwrotu:");
            if (fid == null || fid.isBlank()) return;
            String reply = sendCommand("RETURN;" + fid + ";" + loggedUserId);
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
            if (lines == null) ta.setText("Błąd połączenia z serwerem");
            else for (String l : lines) ta.append(l + "\n");
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
            if (lines == null) ta.setText("Błąd połączenia z serwerem");
            else for (String l : lines) ta.append(l + "\n");
        });

        payBtn.addActionListener(e -> {
            if (loggedUserId < 0) { JOptionPane.showMessageDialog(frame, "Zaloguj się najpierw"); return; }
            String input = JOptionPane.showInputDialog(frame, "Podaj id opłaty oraz kwotę oddzielone przecinkiem (np. 1,25.5):");
            if (input == null || input.isBlank()) return;
            String[] parts = input.split(",", 2);
            if (parts.length < 2) { JOptionPane.showMessageDialog(frame, "Niepoprawne dane"); return; }
            String reply = sendCommand("PAY;" + parts[0].trim() + ";" + parts[1].trim() + ";" + loggedUserId);
            JOptionPane.showMessageDialog(frame, "Serwer: " + reply);
        });

        logoutBtn.addActionListener(e -> {
            loggedUserId = -1;
            frame.dispose();
            mainFrame.setVisible(true);
        });

        // gdy dashboard się zamknie (np. krzyżyk), wracamy do main
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                loggedUserId = -1;
                mainFrame.setVisible(true);
            }
        });

        return frame;
    }

    // zwraca pierwszą linijkę odpowiedzi (dla komend typu LOGIN/REGISTER/RENT/RETURN/PAY)
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

    // zwraca wszystkie linie odpowiedzi aż do linii "END" (lub null przy błędzie)
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
