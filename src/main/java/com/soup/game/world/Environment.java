package com.soup.game.world;

import com.soup.game.enums.Gamerule;
import com.soup.game.enums.Seasons;
import com.soup.game.enums.Weather;
import com.soup.game.intf.World;
import com.soup.game.service.Console;
import com.soup.game.service.Localization;
import com.soup.game.service.Stats;

import java.util.Objects;

/**
 * <h1>Environment</h1>
 * Represents the game's environmental state, including season, weather,
 * day tracking, and the time of day.
 * <p>
 * The {@code Environment} class manages seasonal changes, weather cycles,
 * and tracks dry days. It interacts with {@link Stats} to update the day
 * count and with {@link Console} for logging environmental messages.
 * </p>
 *
 * <p>
 * Seasons affect crop growth and harvesting mechanics, while weather can
 * influence hydration, growth speed, and certain gameplay events.
 * </p>
 */
@World
public class Environment {
    private Seasons season;
    private Weather weather;
    private int dryDay;
    private float hours;

    /**
     * Initializes the environment with default values:
     * {@link Weather#SUNNY} and {@link Seasons#WINTER}.
     */
    public Environment() {
        this.weather = Weather.SUNNY;
        this.season = Seasons.WINTER;
        this.dryDay = 0;
        this.hours = 0f;
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
        if (Stats.stat.days % 30 == 0) {
            season = season.next();
            Console.cli.println(Localization.lang.t("game.season.new",
                    season.getKey()), Console.PURPLE);
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
        if (Gamerule.isEnabled(Gamerule.ENABLE_WEATHER_CYCLE)
                || Gamerule.isEnabled(Gamerule.ENABLE_STOP_TIME)) {
            return;
        }

        weather = Weather.getRandomWeather();
        if (Objects.equals(weather, Weather.DRY)) {
            dryDay++;
        } else {
            dryDay = 0;
        }

        Console.cli.println(weather.message(), Console.CYAN);
    }
}