package com.soup.game.swing;

import com.soup.game.core.Game;
import com.soup.game.ent.Animal;
import com.soup.game.service.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

/**
 * <h1>SwingFrame</h1>
 *
 * The main application window for the game, responsible for initializing
 * and displaying the Swing-based user interface.
 *
 * <p>
 * This frame sets up:
 * <ul>
 *     <li>The main {@link SwingPanel} used for rendering the terminal UI</li>
 *     <li>Window properties such as size, title, and close behavior</li>
 *     <li>Application localization settings</li>
 * </ul>
 * </p>
 *
 * <h2>Initialization Flow</h2>
 * <ol>
 *     <li>Creates and attaches the {@link SwingPanel}</li>
 *     <li>Configures frame properties (size, position, visibility)</li>
 *     <li>Initializes localization (default: English)</li>
 *     <li>Starts the game on the Swing Event Dispatch Thread</li>
 * </ol>
 *
 * <h2>Threading</h2>
 * <p>
 * Game initialization is dispatched via {@link SwingUtilities#invokeLater(Runnable)}
 * to ensure proper synchronization with the Swing UI lifecycle.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * public static void main(String[] args) {
 *     new SwingFrame();
 * }
 * }</pre>
 *
 * @author isGoodSoup
 * @since 2.0
 */
public class SwingFrame extends JFrame {
    private static final String TITLE = "farmlet v2.0";
    private static final Logger log = LoggerFactory.getLogger(SwingFrame.class);
    private final SwingPanel panel;

    /**
     * Constructs at runtime the JPanel and creates a window, creating a
     * new {@link Game} using {@link SwingUtilities#invokeLater(Runnable)}
     */
    public SwingFrame() throws HeadlessException {
        super(TITLE);
        this.panel = new SwingPanel(this);
        log.info("SwingFrame started");
        Animal.setPanel(panel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        add(panel);
        setLocationRelativeTo(null);
        setVisible(true);
        Localization.lang.setLocale(Locale.forLanguageTag("en"));
        SwingUtilities.invokeLater(() -> {
            Game game = new Game(panel);
        });
    }
}
