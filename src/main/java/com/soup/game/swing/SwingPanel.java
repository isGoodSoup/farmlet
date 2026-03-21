package com.soup.game.swing;

import com.soup.game.intf.CommandListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

/**
 * <h1>SwingPanel</h1>
 *
 * A terminal-style UI component built using Swing for rendering game output
 * and handling user input. This panel simulates a console environment with
 * a typewriter effect, colorized text, and sequential message rendering.
 *
 * <p>
 * The panel consists of:
 * <ul>
 *     <li>A {@link JTextPane} ({@code gameLog}) for styled, colored output</li>
 *     <li>A {@link JTextField} ({@code inputField}) for user command input</li>
 *     <li>A {@link JScrollPane} wrapper for scrollable output</li>
 * </ul>
 * </p>
 *
 * <h2>Rendering Model</h2>
 * <p>
 * Text is rendered using a typewriter animation implemented via
 * {@link javax.swing.Timer}. Each character is appended incrementally
 * to the document with a configurable delay.
 * </p>
 *
 * <p>
 * To prevent overlapping animations and ensure deterministic output,
 * a FIFO queue ({@link Queue}) is used. If a render operation is already
 * in progress, new requests are enqueued and executed sequentially.
 * </p>
 *
 * <h2>Threading</h2>
 * <p>
 * All UI updates are executed on the Swing Event Dispatch Thread (EDT)
 * via {@link SwingUtilities#invokeLater(Runnable)} to maintain thread safety.
 * </p>
 *
 * <h2>Styling</h2>
 * <p>
 * The panel uses a custom monospaced font (JetBrains Mono if available)
 * and a dark theme:
 * <ul>
 *     <li>Background: black</li>
 *     <li>Foreground: white</li>
 *     <li>Custom colors supported via {@link java.awt.Color}</li>
 * </ul>
 * </p>
 *
 * <h2>Input Handling</h2>
 * <p>
 * User input is captured via the {@code inputField}. When the user presses Enter,
 * the input is processed and appended to the log with a prompt prefix.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * SwingPanel panel = new SwingPanel(frame);
 * panel.append("Hello world", Color.GREEN);
 * }</pre>
 *
 * @author isGoodSoup
 * @since 2.0
 */
@SuppressWarnings("all")
public class SwingPanel extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(SwingPanel.class);
    private final JFrame frame;
    private final JScrollPane scrollPane;
    private final JTextPane gameLog;
    private final JTextField inputField;
    private final Queue<Runnable> queue = new LinkedList<>();
    private Font font;
    private CommandListener listener;

    private boolean isTyping = false;

    /**
     * Constructs the {@link JTextPane}, {@link JScrollPane}, {@link JTextField} at runtime and
     * creates a custom font from the /fonts/ folder. Sets the padding and amongst other values.
     * @param frame
     */
    public SwingPanel(JFrame frame) {
        log.info("SwingPanel was created");
        this.frame = frame;
        this.gameLog = new JTextPane();
        this.scrollPane = new JScrollPane(gameLog);
        this.inputField = new JTextField();
        setLayout(new BorderLayout());

        try {
            InputStream is = getClass().getResourceAsStream("/fonts/JetBrainsMonoSemiBold.ttf");
            font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(20f);
            gameLog.setFont(font);
        } catch (Exception e) {
            log.error("{}", e.getMessage());
            gameLog.setFont(new Font("Monospaced", Font.PLAIN, 20));
        }

        final int PADDING = 32;
        gameLog.setEditable(false);
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

        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        inputField.setBorder(null);
        gameLog.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);

        inputField.addActionListener(e -> {
            String command = inputField.getText();
            Color color = inputField.getForeground();
            inputField.setText("");
            process(command, color);
            if(listener != null) {
                SwingUtilities.invokeLater(() ->
                        listener.onCommand(command));
            }
        });
    }

    /**
     * Echoes the command input
     * @param command the command
     * @param color the echoed command's color
     */
    private void process(String command, Color color) {
        append("$ " + command, color);
    }

    /**
     * Appends text to the output using the default typewriter delay.
     * <p>
     * This method ensures execution on the EDT and delegates to the
     * queued rendering system.
     * </p>
     *
     * @param text  the text to render
     * @param color the color of the text
     */
    public void append(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            append("\n"+ text, color, 15, null);
        });
    }

    /**
     * Appends text to the output with a typewriter effect.
     * <p>
     * If another rendering operation is currently in progress, this request
     * is queued and executed once the current animation completes.
     * </p>
     *
     * @param text     the text to render
     * @param color    the color of the text
     * @param delay    delay in milliseconds between each character
     * @param callback optional callback executed after rendering completes
     */
    public void append(String text, Color color, int delay, Runnable callback) {
        Runnable task = () -> typing(text, color, delay, callback);
        if(isTyping) {
            queue.add(task);
        } else {
            task.run();
        }
    }

    /**
     * Extraction of Timer logic into a separate method which creates the
     * typewriter effect.
     * <h5>Note</h5>
     * <p>
     * The separation of concerns was in order to fix a major bug I was having
     * on the printing overlapping each other and calling thousands of Timers
     * per tick.
     * </p>
     * @param text the text to print
     * @param color the chosen color
     * @param delay the delay in ms
     * @param callback the runnable callback
     */
    private void typing(String text, Color color, int delay, Runnable callback) {
        isTyping = true;

        StyledDocument doc = gameLog.getStyledDocument();
        final int[] index = {0};

        Style style = gameLog.addStyle("textStyle_" + System.nanoTime(), null);
        if (color != null) {
            StyleConstants.setForeground(style, color);
        } else {
            StyleConstants.setForeground(style, Color.WHITE);
        }

        Timer timer = new Timer(delay, null);
        timer.addActionListener(e -> {
            if (index[0] < text.length()) {
                try {
                    doc.insertString(doc.getLength(),
                            String.valueOf(text.charAt(index[0])), style);
                    gameLog.setCaretPosition(doc.getLength());
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
                index[0]++;
            } else {
                timer.stop();
                isTyping = false;
                if(callback != null) { callback.run(); }
                if(!queue.isEmpty()) {
                    queue.poll().run();
                }
            }
        });
        timer.start();
    }

    /**
     * Requests focus for the input field.
     * <p>
     * This is executed on the EDT to ensure proper focus behavior.
     * </p>
     */
    public void focusInput() {
        SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());
    }

    /**
     * Sets the {@link CommandListener} value through listener
     * @param listener the value
     */
    public void setCommandListener(CommandListener listener) {
        this.listener = listener;
    }
}