package org.unitedlands.unitedlands;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.classes.ConfigFile;
import org.unitedlands.services.UnitedLanguageService;
import org.unitedlands.unitedlands.classes.Settings;

import org.unitedlands.unitedlands.integrations.Pl3xMap.Pl3xMapRenderer;
import org.unitedlands.unitedlands.integrations.Towny.TownyProvider;
import org.unitedlands.unitedlands.integrations.papi.PlaceholderAPIIntegration;
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
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.schedulers.NewDayScheduler;
import org.unitedlands.unitedlands.utils.UnitedLanguageServiceImplementation;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.unitedlands.managers.PlayerCacheManager;
import org.unitedlands.utils.United;

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

    private Pl3xMapRenderer mapRenderer;
    private TownyProvider townyProvider;

    private UnitedLandsWebServices webServices;

    private NewDayScheduler newDayScheduler;

    private boolean useFloodgate;
    private boolean usePAPI;

    private UnitedLanguageService languageProvider;

    @Override
    public void onEnable() {

        LoggerFactory.setLogBackendFactory(new NullLogBackend.NullLogBackendFactory());

        instance = this;

        saveDefaultConfig();

        // messageConfig = new ConfigFile(this, "messages/en.yml");
        permissionConfig = new ConfigFile(this, "permissions.yml");

        // messageProvider = new MessageProvider(messageConfig.get());

        Settings.loadSettings(getConfig());

        loadManagers();
        loadIntegrations();
        registerListeners();

        webServices = new UnitedLandsWebServices(this);

        languageProvider = new UnitedLanguageServiceImplementation();
        Bukkit.getServicesManager().register(UnitedLanguageService.class, languageProvider, this, ServicePriority.Highest);

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

        newDayScheduler = new NewDayScheduler();
    }

    private void loadIntegrations() {
        var towny = getServer().getPluginManager().getPlugin("Towny");
        if (towny != null && towny.isEnabled()) {
            townyProvider = new TownyProvider(this);
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

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public TownyProvider getTownyProvider() {
        return townyProvider;
    }

    public UnitedLandsWebServices getWebServices() {
        return webServices;
    }

    public boolean useFloodgate() {
        return useFloodgate;
    }

    public boolean usePAPI() {
        return usePAPI;
    }

    public NewDayScheduler getNewDayScheduler() {
        return newDayScheduler;
    }

    public UnitedLanguageService getLanguageProvider() {
        return languageProvider;
    }

}
