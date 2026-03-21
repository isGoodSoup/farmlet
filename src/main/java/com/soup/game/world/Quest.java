package com.soup.game.world;

import com.soup.game.ent.NPC;
import com.soup.game.enums.QuestType;
import com.soup.game.intf.Data;
import com.soup.game.intf.Item;
import com.soup.game.intf.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a quest in the game world.
 * <p>
 * Quests are tasks given by NPCs and have specific objectives,
 * types, rewards, and conditions. They may be repeatable or one-time only.
 * </p>
 */
@World(entity = "quest")
@Data
public record Quest(long id, NPC giver, QuestType type, int reward,
                    int required, Item item, boolean canRedo) {
    private static final Logger log = LoggerFactory.getLogger(Quest.class);

    public Quest(long id, NPC giver, QuestType type, int reward, int required, Item item, boolean canRedo) {
        this.id = id;
        this.giver = giver;
        this.type = type;
        this.reward = reward;
        this.required = required;
        this.item = item;
        this.canRedo = canRedo;
        log.info("New quest= {},{},{},{},{},{},{}", id, giver.name(), type.name(), reward,
                required, item.getName(), canRedo);
    }
}
