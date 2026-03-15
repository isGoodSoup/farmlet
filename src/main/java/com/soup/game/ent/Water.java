package com.soup.game.ent;

import com.soup.game.intf.Data;
import com.soup.game.intf.Entity;
import com.soup.game.intf.Item;
import com.soup.game.service.Localization;

@Data
public record Water(String name, int value) implements Item {

    @Override
    public String getName() {
        return Localization.lang.t(name);
    }
}
