package org.unitedlands.unitedlands.integrations.papi;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class UnitedLandsPAPIExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", UnitedLands.instance().getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getIdentifier() {
        return "unitedlands";
    }

    @Override
    public @NotNull String getVersion() {
        return String.join(", ", UnitedLands.instance().getPluginMeta().getVersion());
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        var citizen = UnitedLandsDataManager.instance().getCitizen(player.getUniqueId());
        if (citizen == null)
            return "MISSING_CITIZEN_DATA";
        if (params.equalsIgnoreCase("player_settlement")) {
            return citizen.hasSettlement() ? applyBrackets(citizen.getSettlement().getCleanName()) : null; //
        }

        if (params.equalsIgnoreCase("player_country")) {
            return citizen.hasCountry() ? applyBrackets(citizen.getCountry().getCleanName()) : null; //
        }
        return null; 
    }

    String applyBrackets(String str) {
        return "<dark_gray>[</dark_gray>" + str + "<dark_gray>]</dark_gray>"; 
    }

}
