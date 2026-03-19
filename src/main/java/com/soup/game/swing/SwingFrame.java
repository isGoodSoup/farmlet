package com.soup.game.swing;

import com.soup.game.core.Game;

import javax.swing.*;
import java.awt.*;

public class SwingFrame extends JFrame {
    private static final String TITLE = "farmlet v2.0";
    private final SwingPanel panel;

    public SwingFrame() throws HeadlessException {
        super(TITLE);
        this.panel = new SwingPanel(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        add(panel);
        setLocationRelativeTo(null);
        setVisible(true);
        new Game(panel);
    }
}
