package org.unitedlands.unitedlands.managers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.unitedlands.unitedlands.integrations.Towny.TownyProvider;
import org.unitedlands.unitedlands.integrations.papi.PlaceholderAPIIntegration;
import org.unitedlands.utils.United;

public class IntegrationManager {
    private static IntegrationManager instance;

    public static IntegrationManager instance() {
        return instance;
    }

    private TownyProvider townyProvider;

    private boolean useFloodgate;
    private boolean usePAPI;

    public IntegrationManager() {
        instance = this;
        loadIntegrations();
    }

    public void loadIntegrations() {

        Plugin towny = Bukkit.getPluginManager().getPlugin("Towny");
        if (towny != null && towny.isEnabled()) {
            townyProvider = new TownyProvider();
            United.logger().info("Found Towny, enabling integration...", "UnitedLands");
        }
        Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
        if (floodgate != null && floodgate.isEnabled()) {
            United.logger().info("Enabling floodgate integrations.", "UnitedLands");
            useFloodgate = true;
        }
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi != null && papi.isEnabled()) {
            United.logger().info("Enabling floodgate integrations.", "UnitedLands");
            new PlaceholderAPIIntegration();
            usePAPI = true;
        }

    }

    public TownyProvider getTownyProvider() {
        return townyProvider;
    }

    public void setTownyProvider(TownyProvider townyProvider) {
        this.townyProvider = townyProvider;
    }

    public boolean useFloodgate() {
        return useFloodgate;
    }

    public boolean usePAPI() {
        return usePAPI;
    }

}
