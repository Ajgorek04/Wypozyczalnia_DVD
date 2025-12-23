package client;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Główna klasa aplikacji klienckiej (Frontend) oparta na bibliotece Swing.
 * <p>
 * Klasa ta odpowiada za:
 * </p>
 * <ul>
 * <li>Wyświetlanie interfejsu graficznego (Logowanie, Panel Użytkownika, Panel Admina).</li>
 * <li>Obsługę zdarzeń (kliknięcia przycisków).</li>
 * <li>Komunikację z serwerem poprzez gniazda sieciowe (Socket).</li>
 * </ul>
 *
 * @author Igor Błędziński, Łukasz Gierczak
 * @version 1.0
 */
public class MainClient {

    /** Przechowuje ID zalogowanego użytkownika (-1 oznacza brak logowania). */
    private static int loggedUserId = -1;
    /** Port serwera. */
    private static final int PORT = 5000;
    /** Adres IP serwera. */
    private static final String HOST = "127.0.0.1";

    /** Główna czcionka aplikacji. */
    private static final Font MAIN_FONT = new Font("Segoe UI", Font.BOLD, 14);
    /** Domyślny kolor tekstu na przyciskach. */
    private static final Color TEXT_COLOR = Color.WHITE;

    /**
     * Prywatny konstruktor zapobiegający instancjalizacji klasy narzędziowej.
     */
    private MainClient() {
        throw new IllegalStateException("Klasa narzędziowa");
    }

    /**
     * Punkt wejścia aplikacji klienckiej.
     * Ustawia systemowy wygląd okien (LookAndFeel) i uruchamia główne okno w wątku Swing (EDT).
     *
     * @param args Argumenty wiersza poleceń (nieużywane).
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            JFrame mainFrame = buildMainFrame();
            mainFrame.setVisible(true);
        });
    }

    // --- GUI: OKNO GŁÓWNE (LOGOWANIE) ---

    /**
     * Buduje główne okno aplikacji (Ekran startowy / Logowanie).
     * Zawiera przyciski do logowania, rejestracji i przeglądania filmów jako gość.
     *
     * @return Skonfigurowany obiekt JFrame ekranu startowego.
     */
    private static JFrame buildMainFrame() {
        JFrame frame = new JFrame("Wypożyczalnia DVD - Witaj");
        frame.setSize(550, 550);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        DarkGradientPanel bgPanel = new DarkGradientPanel();
        bgPanel.setLayout(new GridBagLayout());
        frame.setContentPane(bgPanel);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(40, 60, 40, 60));

        JLabel title = new JLabel("Wypożyczalnia DVD");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(new Color(255, 204, 0)); // Złoty
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel.add(title);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 40)));

        // Przyciski
        JButton loginBtn = createModernButton("Zaloguj się", new Color(41, 128, 185)); // Niebieski
        JButton registerBtn = createModernButton("Zarejestruj się", new Color(41, 128, 185));
        JButton moviesBtn = createModernButton("Przeglądaj Filmy (Gość)", new Color(142, 68, 173)); // Fioletowy
        JButton exitBtn = createModernButton("Wyjście", new Color(90, 90, 90)); // Szary

        for (JButton btn : new JButton[]{loginBtn, registerBtn, moviesBtn, exitBtn}) {
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(btn);
            contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        }

        bgPanel.add(contentPanel);

        // --- AKCJE ---

        exitBtn.addActionListener(e -> System.exit(0));

        loginBtn.addActionListener(e -> {
            JPanel p = new JPanel(new GridLayout(2,2, 5, 5));
            JTextField userField = new JTextField();
            JPasswordField passField = new JPasswordField();
            p.add(new JLabel("Użytkownik:")); p.add(userField);
            p.add(new JLabel("Hasło:")); p.add(passField);

            int result = JOptionPane.showConfirmDialog(frame, p, "Logowanie", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String user = userField.getText();
                String pass = new String(passField.getPassword());
                String reply = sendCommand("LOGIN;" + user + ";" + pass);

                if (reply != null && reply.startsWith("LOGIN_OK;")) {
                    String[] parts = reply.split(";", 2);
                    loggedUserId = Integer.parseInt(parts[1]);
                    frame.setVisible(false);

                    if ("admin".equals(user)) {
                        JFrame adminFrame = buildAdminFrame(frame);
                        adminFrame.setVisible(true);
                    } else {
                        JFrame dashboard = buildDashboardFrame(frame);
                        dashboard.setVisible(true);
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Błąd logowania", "Błąd", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        registerBtn.addActionListener(e -> {
            JPanel p = new JPanel(new GridLayout(2,2, 5, 5));
            JTextField userField = new JTextField();
            JPasswordField passField = new JPasswordField();
            p.add(new JLabel("Użytkownik:")); p.add(userField);
            p.add(new JLabel("Hasło:")); p.add(passField);

            int result = JOptionPane.showConfirmDialog(frame, p, "Rejestracja", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String user = userField.getText();
                String pass = new String(passField.getPassword());
                String reply = sendCommand("REGISTER;" + user + ";" + pass);
                if ("REGISTER_OK".equals(reply)) {
                    JOptionPane.showMessageDialog(frame, "Zarejestrowano pomyślnie!");
                } else {
                    JOptionPane.showMessageDialog(frame, "Rejestracja nie powiodła się", "Błąd", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        moviesBtn.addActionListener(e -> showMoviesWindow(frame));

        return frame;
    }

    // --- GUI: PANEL UŻYTKOWNIKA ---

    /**
     * Buduje panel użytkownika (Dashboard).
     * Wyświetlany po poprawnym zalogowaniu standardowego klienta.
     * Umożliwia dostęp do funkcji takich jak: Wypożycz, Zwróć, Moje Opłaty.
     *
     * @param mainFrame Referencja do głównego okna (aby móc do niego wrócić przy wylogowaniu).
     * @return Skonfigurowany obiekt JFrame panelu użytkownika.
     */
    private static JFrame buildDashboardFrame(JFrame mainFrame) {
        JFrame frame = new JFrame("Panel użytkownika");
        frame.setSize(600, 600);
        frame.setLocationRelativeTo(mainFrame);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        DarkGradientPanel bgPanel = new DarkGradientPanel();
        bgPanel.setLayout(new GridBagLayout());
        frame.setContentPane(bgPanel);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(30, 50, 30, 50));

        JLabel title = new JLabel("Panel Klienta");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel.add(title);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        JButton moviesBtn = createModernButton("Lista Filmów", new Color(41, 128, 185));
        JButton rentBtn = createModernButton("Wypożycz film", new Color(41, 128, 185));
        JButton myRentsBtn = createModernButton("Moje wypożyczenia", new Color(39, 174, 96)); // Zielony
        JButton myTransBtn = createModernButton("Moje transakcje / opłaty", new Color(211, 84, 0)); // Pomarańczowy
        JButton payAndReturnBtn = createModernButton("Zapłać i zwróć film", new Color(192, 57, 43)); // Czerwony
        JButton logoutBtn = createModernButton("Wyloguj", new Color(90, 90, 90));

        for (JComponent c : new JComponent[]{moviesBtn, rentBtn, myRentsBtn, myTransBtn, payAndReturnBtn, new JSeparator(), logoutBtn}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            if (c instanceof JButton) {
                contentPanel.add(c);
                contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            } else {
                c.setMaximumSize(new Dimension(250, 10));
                c.setBackground(new Color(255,255,255,50));
                contentPanel.add(c);
                contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            }
        }

        bgPanel.add(contentPanel);

        // --- LOGIKA ---

        moviesBtn.addActionListener(e -> showMoviesWindow(frame));

        rentBtn.addActionListener(e -> {
            List<String> lines = sendCommandLines("GET_FILMS");
            if (lines == null) { JOptionPane.showMessageDialog(frame, "Błąd połączenia"); return; }

            List<String> available = new ArrayList<>();
            Pattern idPattern = Pattern.compile("^(\\d+)\\.");
            for (String l : lines) {
                if (l.contains("Dostępny: Tak") || l.contains("Dostepny: Tak")) {
                    Matcher m = idPattern.matcher(l);
                    if (m.find()) available.add(m.group(1) + " - " + l.substring(l.indexOf(".") + 1).trim());
                }
            }

            if (available.isEmpty()) { JOptionPane.showMessageDialog(frame, "Brak dostępnych filmów."); return; }

            String selected = (String) JOptionPane.showInputDialog(frame, "Wybierz film:", "Wypożycz", JOptionPane.PLAIN_MESSAGE, null, available.toArray(), available.get(0));
            if (selected == null) return;

            try {
                int filmId = Integer.parseInt(selected.split("\\s*-\\s*", 2)[0].trim());
                String reply = sendCommand("RENT;" + filmId + ";" + loggedUserId);
                JOptionPane.showMessageDialog(frame, reply);
            } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Błąd przetwarzania"); }
        });

        myRentsBtn.addActionListener(e -> showListWindow(frame, "Moje wypożyczenia", "MY_RENTS;" + loggedUserId));
        myTransBtn.addActionListener(e -> showListWindow(frame, "Moje opłaty", "MY_TRANS;" + loggedUserId));

        payAndReturnBtn.addActionListener(e -> {
            List<String> lines = sendCommandLines("MY_TRANS;" + loggedUserId);
            if (lines == null) { JOptionPane.showMessageDialog(frame, "Błąd połączenia"); return; }

            List<String> unpaid = new ArrayList<>();
            Pattern p = Pattern.compile("Opłata\\s+(\\d+)\\s+\\|.*kwota:\\s*([0-9]+\\.?[0-9]*).*?\\|\\s*powód:\\s*(.*?)\\s+\\|\\s*Opłacona:\\s*(NIE|TAK)");

            for (String l : lines) {
                Matcher m = p.matcher(l);
                if (m.find() && "NIE".equalsIgnoreCase(m.group(4))) {
                    unpaid.add("Opłata ID: " + m.group(1) + " | Kwota: " + m.group(2) + " zł | Za: " + m.group(3));
                }
            }

            if (unpaid.isEmpty()) { JOptionPane.showMessageDialog(frame, "Brak opłat do uregulowania."); return; }

            String selected = (String) JOptionPane.showInputDialog(frame, "Wybierz rachunek (spowoduje zwrot filmu):", "Zapłać i zwróć", JOptionPane.QUESTION_MESSAGE, null, unpaid.toArray(), unpaid.get(0));
            if (selected == null) return;

            Matcher mId = Pattern.compile("ID:\\s*(\\d+)").matcher(selected);
            if (mId.find()) {
                String reply = sendCommand("PAY;" + mId.group(1) + ";0.0;" + loggedUserId);
                JOptionPane.showMessageDialog(frame, reply);
            }
        });

        logoutBtn.addActionListener(e -> {
            loggedUserId = -1;
            frame.dispose();
            mainFrame.setVisible(true);
        });

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) { mainFrame.setVisible(true); }
        });

        return frame;
    }

    // --- GUI: PANEL ADMINISTRATORA (POPRAWIONY UKŁAD) ---

    /**
     * Buduje panel administratora.
     * Uruchamiany tylko, gdy login to "admin". Umożliwia zarządzanie użytkownikami.
     *
     * @param mainFrame Referencja do głównego okna.
     * @return Skonfigurowany obiekt JFrame panelu admina.
     */
    private static JFrame buildAdminFrame(JFrame mainFrame) {
        JFrame frame = new JFrame("Panel Administratora");
        // FIX: Zwiększona wysokość, żeby przyciski się nie ucinały
        frame.setSize(700, 600);
        frame.setLocationRelativeTo(mainFrame);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        DarkGradientPanel bgPanel = new DarkGradientPanel();
        bgPanel.setLayout(new BorderLayout());
        frame.setContentPane(bgPanel);

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(20,0,20,0));
        JLabel header = new JLabel("Zarządzanie Użytkownikami");
        header.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.setForeground(Color.WHITE);
        headerPanel.add(header);
        bgPanel.add(headerPanel, BorderLayout.NORTH);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(listModel);
        userList.setFont(new Font("Consolas", Font.PLAIN, 14));

        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.setOpaque(false);
        listWrapper.setBorder(new EmptyBorder(0, 40, 0, 40));
        listWrapper.add(new JScrollPane(userList), BorderLayout.CENTER);
        bgPanel.add(listWrapper, BorderLayout.CENTER);

        // FIX: Grid Layout 2x2 dla przycisków, żeby się nie ucinały
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(new EmptyBorder(20, 40, 30, 40));

        JButton refreshBtn = createModernButton("Odśwież", new Color(41, 128, 185));
        JButton deleteBtn = createModernButton("Usuń użytkownika", new Color(192, 57, 43));
        JButton passBtn = createModernButton("Zmień hasło", new Color(243, 156, 18));
        JButton logoutBtn = createModernButton("Wyloguj", new Color(90, 90, 90));

        btnPanel.add(refreshBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(passBtn);
        btnPanel.add(logoutBtn);
        bgPanel.add(btnPanel, BorderLayout.SOUTH);

        Runnable refreshAction = () -> {
            listModel.clear();
            List<String> lines = sendCommandLines("ADMIN_GET_USERS");
            if (lines != null) for (String l : lines) listModel.addElement(l);
        };

        refreshBtn.addActionListener(e -> refreshAction.run());
        refreshAction.run();

        deleteBtn.addActionListener(e -> {
            String selected = userList.getSelectedValue();
            if (selected == null) { JOptionPane.showMessageDialog(frame, "Wybierz użytkownika"); return; }
            String idStr = selected.split("\\|")[0].replace("ID:", "").trim();
            if (JOptionPane.showConfirmDialog(frame, "Usunąć użytkownika ID " + idStr + "?", "Potwierdź", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(frame, sendCommand("ADMIN_DEL_USER;" + idStr));
                refreshAction.run();
            }
        });

        passBtn.addActionListener(e -> {
            String selected = userList.getSelectedValue();
            if (selected == null) { JOptionPane.showMessageDialog(frame, "Wybierz użytkownika"); return; }
            String idStr = selected.split("\\|")[0].replace("ID:", "").trim();
            String newPass = JOptionPane.showInputDialog(frame, "Nowe hasło:");
            if (newPass != null && !newPass.isBlank()) {
                JOptionPane.showMessageDialog(frame, sendCommand("ADMIN_PASS;" + idStr + ";" + newPass));
            }
        });

        logoutBtn.addActionListener(e -> {
            loggedUserId = -1;
            frame.dispose();
            mainFrame.setVisible(true);
        });

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) { mainFrame.setVisible(true); }
        });

        return frame;
    }

    // --- CUSTOM SWING COMPONENTS ---

    /**
     * Niestandardowy panel z ciemnym gradientem w tle.
     * Służy jako kontener (ContentPane) dla wszystkich okien aplikacji.
     */
    static class DarkGradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint gp = new GradientPaint(0, 0, new Color(43, 50, 58), 0, getHeight(), new Color(20, 20, 20));
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * Nowoczesny przycisk z efektem szkła/gradientu i zaokrąglonymi rogami.
     * Reaguje na najechanie myszką (hover).
     */
    static class ModernButton extends JButton {
        private Color baseColor;
        private boolean isHovered = false;

        public ModernButton(String text, Color color) {
            super(text);
            this.baseColor = color;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(MAIN_FONT);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Preferowany rozmiar (szerokość przycisków)
            Dimension size = new Dimension(260, 45);
            setPreferredSize(size);
            setMaximumSize(size);
            setMinimumSize(size);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            Color c = isHovered ? baseColor.brighter() : baseColor;

            GradientPaint gp = new GradientPaint(0, 0, c.brighter(), 0, h, c.darker());
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w, h, 15, 15);

            g2.setPaint(new GradientPaint(0, 0, new Color(255,255,255, 80), 0, h/2, new Color(255,255,255, 10)));
            g2.fillRoundRect(0, 0, w, h/2, 15, 15);

            g2.setColor(new Color(0,0,0, 50));
            g2.drawRoundRect(0, 0, w-1, h-1, 15, 15);

            g2.dispose();
            super.paintComponent(g); // Rysowanie tekstu
        }
    }

    /**
     * Fabryka przycisków. Tworzy instancję ModernButton.
     *
     * @param text  Tekst na przycisku.
     * @param color Kolor bazowy przycisku.
     * @return Nowy obiekt JButton.
     */
    private static JButton createModernButton(String text, Color color) {
        return new ModernButton(text, color);
    }

    private static void showMoviesWindow(JFrame parent) {
        showListWindow(parent, "Lista Filmów", "GET_FILMS");
    }

    private static void showListWindow(JFrame parent, String title, String command) {
        JFrame listFrame = new JFrame(title);
        listFrame.setSize(500, 400);
        listFrame.setLocationRelativeTo(parent);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        textArea.setMargin(new Insets(10,10,10,10));

        listFrame.add(new JScrollPane(textArea));
        listFrame.setVisible(true);

        List<String> lines = sendCommandLines(command);
        if (lines == null) textArea.setText("Błąd połączenia z serwerem");
        else for (String l : lines) textArea.append(l + "\n");
    }

    // --- KOMUNIKACJA ---

    /**
     * Wysyła komendę do serwera i zwraca pojedynczą linię odpowiedzi.
     * Używane np. przy logowaniu lub rejestracji.
     *
     * @param cmd Treść komendy (np. "LOGIN;user;pass").
     * @return Pierwsza linia odpowiedzi serwera lub null w przypadku błędu.
     */
    private static String sendCommand(String cmd) {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.println(cmd);
            return in.readLine();
        } catch (IOException e) { return null; }
    }

    /**
     * Wysyła komendę do serwera i odbiera wielowierszową odpowiedź (listę).
     * Używane np. do pobierania listy filmów.
     * Czyta dane dopóki serwer nie wyśle "END".
     *
     * @param cmd Treść komendy (np. "GET_FILMS").
     * @return Lista linii odpowiedzi lub null w przypadku błędu.
     */
    private static List<String> sendCommandLines(String cmd) {
        List<String> result = new ArrayList<>();
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.println(cmd);
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                result.add(line);
            }
            return result;
        } catch (IOException e) { return null; }
    }
}