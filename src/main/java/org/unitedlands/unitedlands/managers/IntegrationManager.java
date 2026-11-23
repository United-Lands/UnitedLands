package org.unitedlands.unitedlands.managers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.unitedlands.unitedlands.integrations.Pl3xMap.Pl3xMapRenderer;
import org.unitedlands.unitedlands.integrations.Towny.TownyProvider;
import org.unitedlands.unitedlands.integrations.map.MapRenderer;
import org.unitedlands.unitedlands.integrations.map.NoOpMapRenderer;
import org.unitedlands.unitedlands.integrations.papi.PlaceholderAPIIntegration;
import org.unitedlands.utils.United;

public class IntegrationManager {
    private static IntegrationManager instance;

    public static IntegrationManager instance() {
        return instance;
    }

    private TownyProvider townyProvider;
    private MapRenderer mapRenderer = new NoOpMapRenderer();

    private boolean useFloodgate;
    private boolean usePAPI;

    public IntegrationManager() {
        instance = this;
        loadIntegrations();
    }

    public void loadIntegrations() {

        Plugin pl3xMap = Bukkit.getPluginManager().getPlugin("Pl3xMap");
        if (pl3xMap != null && pl3xMap.isEnabled()) {
            mapRenderer = new Pl3xMapRenderer();
            United.logger().info("Found Pl3xMap, enabling map rendering...", "UnitedLands");
        } else {
            United.logger().warning("Pl3xMap not found, map rendering is disabled.", "UnitedLands");
        }

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
            United.logger().info("Enabling PlaceholderAPI integrations.", "UnitedLands");
            new PlaceholderAPIIntegration();
            usePAPI = true;
        }

    }

    public MapRenderer getMapRenderer() {
        return mapRenderer;
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
