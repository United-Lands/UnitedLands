package org.unitedlands.unitedlands;

import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.classes.ConfigFile;
import org.unitedlands.unitedlands.integrations.Pl3xMap.Pl3xMapRenderer;
import org.unitedlands.unitedlands.listeners.BlockListener;
import org.unitedlands.unitedlands.listeners.ChatListener;
import org.unitedlands.unitedlands.listeners.ExplosionListener;
import org.unitedlands.unitedlands.listeners.MobListener;
import org.unitedlands.unitedlands.listeners.PlayerDamageListener;
import org.unitedlands.unitedlands.listeners.PlayerMovementListener;
import org.unitedlands.unitedlands.listeners.PlayerListener;
import org.unitedlands.unitedlands.listeners.RegionListener;
import org.unitedlands.unitedlands.listeners.ServerEventListener;
import org.unitedlands.unitedlands.managers.ChatChannelManager;
import org.unitedlands.unitedlands.managers.ConfirmationManager;
import org.unitedlands.unitedlands.managers.DisplayManager;
import org.unitedlands.unitedlands.managers.FlightManager;
import org.unitedlands.unitedlands.managers.IntegrationManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.schedulers.NewDayScheduler;
import org.unitedlands.unitedlands.utils.FontUtils;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.unitedlands.managers.PlayerCacheManager;
import org.unitedlands.libs.ormlite.logger.LoggerFactory;
import org.unitedlands.libs.ormlite.logger.NullLogBackend;

public class UnitedLands extends JavaPlugin {

    private static UnitedLands instance;

    private ConfigFile permissionConfig;

    // UnitedLandsDataManager globalDataManager;
    // UnitedLandsEconomyManager economyManager;
    // DisplayManager displayManager;
    // ConfirmationManager confirmationManager;
    // PermissionManager permissionManager;
    // PlayerCacheManager playerCacheManager;
    // ChatChannelManager chatChannelManager;
    // IntegrationManager integrationManager;
    // FlightManager flightManager;

    //private Pl3xMapRenderer mapRenderer;

    private UnitedLandsWebServices webServices;

    private NewDayScheduler newDayScheduler;

    @Override
    public void onEnable() {

        LoggerFactory.setLogBackendFactory(new NullLogBackend.NullLogBackendFactory());

        instance = this;

        saveDefaultConfig();

        permissionConfig = new ConfigFile(this, "permissions.yml");

        createManagers();
        registerListeners();

        webServices = new UnitedLandsWebServices(this);

        FontUtils.load(this);

        getLogger().info("UnitedLands initialized.");
    }

    @Override
    public void onDisable() {
        webServices.stopWebServices();
        newDayScheduler.stopScheduler();
        Pl3xMapRenderer.instance().shutdown();
    }

    private void createManagers() {

        new PermissionManager(this);
        new UnitedLandsDataManager(this, new Pl3xMapRenderer());
        new DisplayManager(this);
        new ConfirmationManager(this);
        new PlayerCacheManager(this);
        new UnitedLandsEconomyManager(this);
        new ChatChannelManager(this);
        new IntegrationManager();
        new FlightManager();

        newDayScheduler = new NewDayScheduler();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerMovementListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new ServerEventListener(), this);
        getServer().getPluginManager().registerEvents(new MobListener(), this);
        getServer().getPluginManager().registerEvents(new ExplosionListener(), this);
        getServer().getPluginManager().registerEvents(new RegionListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
    }

    public static UnitedLands instance() {
        return instance;
    }

    public ConfigFile getPermissionConfig() {
        return permissionConfig;
    }

    public UnitedLandsWebServices getWebServices() {
        return webServices;
    }

    public NewDayScheduler getNewDayScheduler() {
        return newDayScheduler;
    }

}
