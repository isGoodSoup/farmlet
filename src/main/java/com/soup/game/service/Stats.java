package com.soup.game.service;

import com.soup.game.cmd.Registry;
import com.soup.game.ent.Player;
import com.soup.game.intf.Item;
import com.soup.game.intf.Service;
import com.soup.game.world.Environment;

import java.util.Map;

/**
 * <h1>Stats</h1>
 * A singleton service class that tracks and displays various game statistics.
 * <p>
 * This class is responsible for monitoring gameplay metrics such as the number of days passed,
 * total water usage, commands executed, crops collected, and the player's overall progress.
 * It can display these statistics via the console.
 * </p>
 *
 * <p>
 * All statistics are stored in public fields and updated dynamically throughout the game.
 * The {@link #showStats(Player)} method formats and prints the player's performance and
 * achievements.
 * </p>
 *
 * <h2>Singleton Access</h2>
 * <pre>{@code
 * Stats stat = Stats.stat; // access the single instance
 * }</pre>
 *
 * <h2>Tracked Statistics</h2>
 * <ul>
 *     <li>{@link #days} – total in-game days passed</li>
 *     <li>{@link #totalWater} – total water used by the player</li>
 *     <li>{@link #totalCmd} – total commands executed</li>
 *     <li>{@link #totalCrops} – total crops in the player’s inventory</li>
 *     <li>{@link #isGameOver} – flag indicating if the game has ended</li>
 * </ul>
 */
@SuppressWarnings("all")
@Service
public class Stats {
    /** Singleton instance of Stats */
    public static final Stats stat = new Stats();
    public int days = 0;
    public int totalWater = 0;
    public int totalCmd = 0;
    public int totalCrops = 0;
    public boolean isGameOver = false;

    /**
     * Displays the current game statistics to the console.
     * <p>
     * The statistics include:
     * <ul>
     *     <li>The overall ending type based on the number of days passed
     *     or if the game is over:
     *         <ul>
     *             <li>Worst Ending – if the game is over</li>
     *             <li>Best Ending – if days &gt; 60</li>
     *             <li>Good Ending – if days are between 15 and 60</li>
     *             <li>Bad Ending – if days &lt; 15</li>
     *         </ul>
     *     </li>
     *     <li>Total commands executed by the player</li>
     *     <li>Total crops in the player's inventory</li>
     *     <li>Number of days passed</li>
     *     <li>Total water used</li>
     *     <li>Player's current level</li>
     *     <li>Player's coin balance</li>
     * </ul>
     * </p>
     */
    public void showStats(Player player) {
        Console.cli.println();
        StringBuilder sb = new StringBuilder(Localization.lang.t("game.stats"));
        if(isGameOver) {
            sb.append(", Worst Ending");
        } else if(days > 60) {
            sb.append(", Best Ending");
        } else if(days >= 15 && days <= 60) {
            sb.append(", Good Ending");
        } else if(days < 15) {
            sb.append(", Bad Ending");
        } else {}

        Console.cli.println(sb.toString(), Console.PURPLE);
        for(Map.Entry<Item, Integer> entries : player.inventory().getAll().entrySet()) {
            totalCrops += entries.getValue();
        }

        Console.cli.println(Localization.lang.t("game.stats.cmd_ran", totalCmd), Console.PURPLE);
        Console.cli.println(Localization.lang.t("game.stats.crops", totalCrops), Console.PURPLE);
        Console.cli.println(Localization.lang.t("game.stats.days", days), Console.PURPLE);
        Console.cli.println(Localization.lang.t("game.stats.waterUsed", totalWater), Console.PURPLE);
        Console.cli.println(Localization.lang.t("game.stats.level", player.level()), Console.PURPLE);
        Console.cli.println(Localization.lang.t("game.stats.coin", player.purse()), Console.PURPLE);
    }

    /**
     * Displays the current in-game time and day.
     * <p>
     * Hours and minutes are formatted as HH:MM, and the current day number is included.
     * </p>
     *
     * @param env the {@link Environment} containing the current hour and day
     * @param day a string representing the day label (e.g., "Day")
     */
    public void showTime(Environment env, String day) {
        int hour = (int) env.hours();
        int minute = (int) ((env.hours() - hour) * 60);
        Console.cli.println(day + " " + days + " - " + String.format("%02d:%02d", hour, minute),
                Console.CYAN);
    }

    /**
     * Displays a list of available commands to the player.
     * <p>
     * Uses the {@link Registry} to fetch all registered commands.
     * </p>
     * @param registry the {@link Registry} containing the command mappings
     */
    public void showHelp(Registry registry) {
        for(String cmd : registry.getCommandNames()) {
            Console.cli.println(" - " + cmd, Console.CYAN);
        }
    }
}
