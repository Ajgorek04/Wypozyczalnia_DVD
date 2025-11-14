package client;

import javax.swing.*;
import java.awt.*;

public class MainClient {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Wypożyczalnia DVD - Klient");
        frame.setSize(500, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Wypożyczalnia Płyt DVD");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JButton loginBtn = new JButton("Zaloguj się");
        JButton registerBtn = new JButton("Zarejestruj się");
        JButton moviesBtn = new JButton("Lista Filmów");
        JButton exitBtn = new JButton("Wyjście");

        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        moviesBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(title);
        panel.add(loginBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(registerBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(moviesBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(exitBtn);

        frame.add(panel);
        frame.setVisible(true);
    }
}
