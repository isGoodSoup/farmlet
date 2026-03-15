package com.soup.game.service;

import com.soup.game.intf.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * Provides basic console input/output utilities for the game.
 * <p>
 * The Console class acts as a centralized service for printing
 * messages to the terminal. It wraps standard output and error
 * streams and offers helper methods for common console operations
 * such as printing text, printing lines, displaying errors, and
 * comparing strings.
 * </p>
 *
 * <p>
 * A singleton instance of this class is available through
 * {@link #cli}, allowing global access to console functionality
 * throughout the application.
 * </p>
 */
@Service
public class Console {
    /**
     * Global singleton instance of the console service.
     * <p>
     * This instance can be used anywhere in the application
     * to perform console operations.
     * </p>
     */
    public static final Console cli = new Console();
    private final Scanner scan = new Scanner(System.in);
    private final Map<String, Consumer<String[]>> commands = new LinkedHashMap<>();

    /**
     * Returns the map of game commands and their associated actions.
     * <p>
     * Each entry in the map consists of a command string (e.g., "plant", "harvest")
     * mapped to a {@link java.util.function.Consumer} that defines the action
     * executed when the command is invoked. The map preserves insertion order.
     * </p>
     * @return a {@link Map} of command names to their corresponding actions
     */
    public Map<String, Consumer<String[]>> cmd() {
        return commands;
    }

    /**
     * Prints an error message to standard error.
     * @param str message to print
     */
    public void error(String str) {
        System.err.println(str);
    }

    /**
     * Prints text to standard output without newline.
     * @param str text to print
     */
    public void print(String str) {
        System.out.print(str);
    }

    /**
     * Prints text to standard output with newline.
     * @param str text to print
     */
    public void println(String str) {
        System.out.println(str);
    }

    /**
     * Prints a blank line to standard output.
     */
    public void println() {
        System.out.println();
    }

    /**
     * Compares two strings ignoring case.
     * @param str1 first string
     * @param str2 second string
     * @return true if strings are equal ignoring case
     */
    public boolean equals(String str1, String str2) {
        return str1.equalsIgnoreCase(str2);
    }

    /**
     * Calculates the sum of a variable number of integer values.
     *
     * <p>This method accepts zero or more integers and returns the
     * total of all provided values. If no numbers are supplied,
     * the method returns {@code 0}.</p>
     *
     * @param n the integers to be summed
     * @return the total sum of all provided integers
     */
    public int sum(int... n) {
        int sum = 0;
        for(int j : n) { sum += j; }
        return sum;
    }

    /**
     * Prints a prompt and reads a line from the console.
     * @param q prompt to display
     * @return user input string
     */
    public String reply(String q) {
        print(q + "$ ");
        return scan.nextLine();
    }

    /**
     * Prints a prompt and reads an integer from the console.
     * @param q prompt to display
     * @return user input integer
     */
    public int replyNum(String q) {
        print(q + "$ ");
        return scan.nextInt();
    }
}
