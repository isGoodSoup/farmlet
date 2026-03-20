package com.soup.game.world;

import com.soup.game.enums.Gamerule;
import com.soup.game.enums.Seasons;
import com.soup.game.enums.Weather;
import com.soup.game.intf.World;
import com.soup.game.service.Colors;
import com.soup.game.service.Localization;
import com.soup.game.service.Stats;
import com.soup.game.swing.SwingPanel;

import java.util.Objects;

/**
 * <h1>Environment</h1>
 * Represents the in-game environmental conditions, including seasons, weather,
 * daily time progression, and consecutive dry days.
 * <p>
 * The {@code Environment} class manages:
 * <ul>
 *     <li>Current season and seasonal changes every 30 days</li>
 *     <li>Randomized daily weather events (sunny, rainy, dry, etc.)</li>
 *     <li>Tracking consecutive dry days for crop management</li>
 *     <li>Hour-based time progression within a single day</li>
 * </ul>
 * </p>
 * <p>
 * This class interacts with the {@link SwingPanel} to display season and weather messages
 * to the player, and it respects {@link Gamerule} flags for weather cycling and time stoppage.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * Environment env = new Environment(panel);
 * env.season();          // Advances the season if a new 30-day cycle starts
 * env.weather();         // Randomizes today's weather and prints it
 * env.advanceTime(1.5f); // Advances the day by 1.5 hours
 * float currentHour = env.hours();
 * Seasons currentSeason = env.getSeason();
 * }</pre>
 *
 * <h2>Notes:</h2>
 * <ul>
 *     <li>Seasons automatically rotate every 30 in-game days.</li>
 *     <li>Weather changes only occur if {@link Gamerule#ENABLE_WEATHER_CYCLE} and
 *         {@link Gamerule#ENABLE_STOP_TIME} are disabled.</li>
 *     <li>Dry day counter increments whenever {@link Weather#DRY} occurs consecutively.</li>
 *     <li>All messages are appended to the {@link SwingPanel} for display.</li>
 * </ul>
 */
@World
public class Environment {
    private final SwingPanel panel;
    private Seasons season;
    private Weather weather;
    private int dryDay;
    private float hours;

    /**
     * Initializes the environment with default values:
     * {@link Weather#SUNNY} and {@link Seasons#SPRING}.
     */
    public Environment(SwingPanel panel) {
        this.weather = Weather.SUNNY;
        this.season = Seasons.SPRING;
        this.dryDay = 0;
        this.hours = 0f;
        this.panel = panel;
    }

    /**
     * Returns the current season in the game.
     * @return the {@link Seasons} value representing the current season
     */
    public Seasons getSeason() {
        return season;
    }

    /**
     * Returns the current weather.
     * @return the {@link Weather} value representing today's weather
     */
    public Weather getWeather() {
        return weather;
    }

    /**
     * Returns the current number of consecutive dry days.
     * @return the dry day count
     */
    public int dryDay() {
        return dryDay;
    }

    /**
     * Returns the number of hours passed in the current day.
     * @return current in-game hours
     */
    public float hours() {
        return hours;
    }

    /**
     * Sets the current number of hours in the day.
     * @param i the hour value to set (0–24)
     */
    public void hours(float i) {
        this.hours = i;
    }

    /**
     * Advances the in-game time by a specified number of hours.
     * @param i the number of hours to add
     */
    public void advanceTime(float i) {
        this.hours += i;
    }

    /**
     * Updates the season based on the day counter.
     * <p>
     * Seasons advance every 30 days. A console message is printed whenever
     * a new season begins.
     * </p>
     */
    public void season() {
        if(Stats.stat().days != 0) {
            if(Stats.stat().days % 30 == 0) {
                season = season.next();
                panel.append(Localization.lang.t("game.season.new",
                        season.getKey()), Colors.PURPLE);
            }
        }
    }

    /**
     * Updates the day's weather randomly if weather cycling is enabled.
     * <p>
     * - If the weather is {@link Weather#DRY}, increments {@link #dryDay}.
     * - Otherwise, resets {@link #dryDay} to 0.
     * - Prints a message describing the day's weather.
     * </p>
     * <p>
     * This method respects the following {@link Gamerule}s:
     * <ul>
     *     <li>{@link Gamerule#ENABLE_WEATHER_CYCLE}</li>
     *     <li>{@link Gamerule#ENABLE_STOP_TIME}</li>
     * </ul>
     * If either is enabled, the weather does not change.
     * </p>
     */
    public void weather() {
        if(Gamerule.isEnabled(Gamerule.ENABLE_WEATHER_CYCLE)
                || Gamerule.isEnabled(Gamerule.ENABLE_STOP_TIME)) {
            return;
        }

        weather = Weather.getRandomWeather();
        if (Objects.equals(weather, Weather.DRY)) {
            dryDay++;
        } else {
            dryDay = 0;
        }

        panel.append(weather.message(), Colors.CYAN);
    }
}