package com.soup.game.service;

import com.soup.game.ent.NPC;
import com.soup.game.intf.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Factory service responsible for creating and providing access to {@link NPC} instances.
 * <p>
 * This class initializes a fixed pool of NPCs at construction time and serves
 * randomized instances on demand. It follows a simple singleton-style access
 * pattern via the {@link #factory} field.
 * </p>
 * <p>
 * Internally, a static list is used to store NPC instances, and a shared
 * {@link Random} is used to retrieve random entries from the pool.
 * </p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *     <li>The pool size is fixed at 16 NPCs by default.</li>
 *     <li>{@link #query()} returns a randomly selected NPC from the pool.</li>
 * </ul>
 *
 * <h3>Notes</h3>
 * <ul>
 *     <li>This implementation is not thread-safe.</li>
 *     <li>NPC instances are reused (not newly created per query).</li>
 *     <li>The pool size and initialization logic can be adjusted in {@link #build()}.</li>
 * </ul>
 */
@SuppressWarnings("all")
@Service
public class NPCFactory {
    public static final NPCFactory factory = new NPCFactory();
    private static final List<NPC> npcs = new ArrayList<>();
    private static final Random random = new Random();
    private static final Logger log = LoggerFactory.getLogger(NPCFactory.class);

    /**
     * Populates the internal NPC pool.
     * <p>
     * By default, this method creates 16 new {@link NPC} instances and adds
     * them to the pool. This method can be modified to customize NPC creation,
     * such as assigning roles, names, or attributes.
     * </p>
     */
    public void build() {
        npcs.clear();
        log.info("Creating NPCs");
        for(int i = 0; i < 16; i++) {
            npcs.add(new NPC());
        }
        log.debug("NPCs created successfully");
    }

    /**
     * Retrieves a random NPC from the pool.
     *
     * @return a randomly selected {@link NPC} instance from the internal list
     * @throws IllegalArgumentException if the NPC pool is empty
     */
    public NPC query() {
        return npcs.get(random.nextInt(npcs.size()));
    }
}
