package org.unitedlands.unitedlands;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.classes.ConfigFile;
import org.unitedlands.services.UnitedLanguageService;
import org.unitedlands.unitedlands.classes.Settings;

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
import org.unitedlands.unitedlands.managers.IntegrationManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.schedulers.NewDayScheduler;
import org.unitedlands.unitedlands.utils.UnitedLanguageServiceImplementation;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.unitedlands.managers.PlayerCacheManager;
import org.unitedlands.libs.ormlite.logger.LoggerFactory;
import org.unitedlands.libs.ormlite.logger.NullLogBackend;

public class UnitedLands extends JavaPlugin {

    private static UnitedLands instance;
    private static Settings settings;

    private ConfigFile permissionConfig;

    UnitedLandsDataManager globalDataManager;
    UnitedLandsEconomyManager economyManager;
    DisplayManager displayManager;
    ConfirmationManager confirmationManager;
    PermissionManager permissionManager;
    PlayerCacheManager playerCacheManager;
    ChatChannelManager chatChannelManager;
    IntegrationManager integrationManager;

    private Pl3xMapRenderer mapRenderer;

    private UnitedLandsWebServices webServices;

    private NewDayScheduler newDayScheduler;


    private UnitedLanguageService languageProvider;

    @Override
    public void onEnable() {

        LoggerFactory.setLogBackendFactory(new NullLogBackend.NullLogBackendFactory());

        instance = this;

        saveDefaultConfig();

        permissionConfig = new ConfigFile(this, "permissions.yml");

        Settings.loadSettings(getConfig());

        loadManagers();
        registerListeners();

        webServices = new UnitedLandsWebServices(this);

        languageProvider = new UnitedLanguageServiceImplementation();
        Bukkit.getServicesManager().register(UnitedLanguageService.class, languageProvider, this,
                ServicePriority.Highest);

        getLogger().info("UnitedLands initialized.");
    }

    @Override
    public void onDisable() {
        webServices.stopWebServices();
        newDayScheduler.stopScheduler();
        Pl3xMapRenderer.instance().shutdown();
    }

    private void loadManagers() {

        mapRenderer = new Pl3xMapRenderer();
        permissionManager = new PermissionManager(this);
        globalDataManager = new UnitedLandsDataManager(this, mapRenderer);
        displayManager = new DisplayManager(this);
        confirmationManager = new ConfirmationManager(this);
        playerCacheManager = new PlayerCacheManager(this);
        economyManager = new UnitedLandsEconomyManager(this);
        chatChannelManager = new ChatChannelManager(this);
        integrationManager = new IntegrationManager();

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

    public static Settings getSettings() {
        return settings;
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

    public UnitedLanguageService getLanguageProvider() {
        return languageProvider;
    }

}
