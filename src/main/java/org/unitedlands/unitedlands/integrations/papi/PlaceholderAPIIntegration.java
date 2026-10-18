package org.unitedlands.unitedlands.integrations.papi;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;

public class PlaceholderAPIIntegration {

    public static PlaceholderAPIIntegration instance;

    public static PlaceholderAPIIntegration instance() {
        return instance;
    }

    public PlaceholderAPIIntegration() {
        instance = this;
        
        new UnitedLandsPAPIExpansion().register();        
    }

    public String setPlaceholders(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

}
