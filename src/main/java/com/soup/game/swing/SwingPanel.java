package com.soup.game.swing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;

@SuppressWarnings("all")
public class SwingPanel extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(SwingPanel.class);
    private final JFrame frame;
    private final JScrollPane scrollPane;
    private final JTextArea gameLog;
    private final JTextField inputField;
    private Font font;

    public SwingPanel(JFrame frame) {
        this.frame = frame;
        this.gameLog = new JTextArea();
        this.scrollPane = new JScrollPane(gameLog);
        this.inputField = new JTextField();
        setLayout(new BorderLayout());

        try {
            InputStream is = getClass().getResourceAsStream("/fonts/JetBrainsMonoSemiBold.ttf");
            font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(24f);
            gameLog.setFont(font);
        } catch (Exception e) {
            log.error("{}", e.getMessage());
            gameLog.setFont(new Font("Monospaced", Font.PLAIN, 24));
        }

        final int PADDING = 15;
        gameLog.setEditable(false);
        gameLog.setLineWrap(true);
        gameLog.setWrapStyleWord(true);
        gameLog.setFont(font);
        gameLog.setBackground(Color.BLACK);
        gameLog.setForeground(Color.WHITE);
        gameLog.setCaretColor(Color.WHITE);
        gameLog.setSelectionColor(Color.DARK_GRAY);
        gameLog.setSelectedTextColor(Color.WHITE);

        inputField.setFont(font);
        inputField.setBackground(Color.BLACK);
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setBorder(null);

        scrollPane.setBorder(null);
        gameLog.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);

        inputField.addActionListener(e -> {
            String command = inputField.getText();
            inputField.setText("");
            process(command);
        });
    }

    private void process(String command) {
        appendToLog("$ " + command);
    }

    public void appendToLog(String text) {
        final int[] index = {0};
        Timer timer = new Timer(30, null);
        timer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (index[0] < text.length()) {
                    gameLog.append(String.valueOf(text.charAt(index[0])));
                    gameLog.setCaretPosition(gameLog.getDocument().getLength());
                    index[0]++;
                } else {
                    ((Timer)e.getSource()).stop();
                }
            }
        });
        timer.start();
    }
}