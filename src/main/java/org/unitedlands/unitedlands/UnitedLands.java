package org.unitedlands.unitedlands;

import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.classes.ConfigFile;
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
import org.unitedlands.unitedlands.managers.TitleManager;
import org.unitedlands.libs.ormlite.logger.LoggerFactory;
import org.unitedlands.libs.ormlite.logger.NullLogBackend;

public class UnitedLands extends JavaPlugin {

    private static UnitedLands instance;

    private ConfigFile permissionConfig;

    private UnitedLandsWebServices webServices;

    private NewDayScheduler newDayScheduler;

    @Override
    public void onEnable() {

        LoggerFactory.setLogBackendFactory(new NullLogBackend.NullLogBackendFactory());

        instance = this;

        saveDefaultConfig();

        permissionConfig = new ConfigFile(this, "permissions.yml");

        try {
            createManagers();
        } catch (Exception ex) {
            getLogger().severe("UnitedLands failed to start and will be disabled: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        webServices = new UnitedLandsWebServices();

        FontUtils.load(this);

        getLogger().info("UnitedLands initialized.");
    }

    @Override
    public void onDisable() {
        runShutdownStep("web services", () -> {
            if (webServices != null)
                webServices.stopWebServices();
        });
        runShutdownStep("new day scheduler", () -> {
            if (newDayScheduler != null)
                newDayScheduler.stopScheduler();
        });
        runShutdownStep("chat channels", () -> {
            if (ChatChannelManager.instance() != null)
                ChatChannelManager.instance().shutdown();
        });
        runShutdownStep("map renderer", () -> {
            if (IntegrationManager.instance() != null)
                IntegrationManager.instance().getMapRenderer().shutdown();
        });
        runShutdownStep("database", () -> {
            var dataManager = UnitedLandsDataManager.instance();
            if (dataManager != null && dataManager.getDatabaseManager() != null)
                dataManager.getDatabaseManager().close();
        });
    }

    private void runShutdownStep(String name, Runnable step) {
        try {
            step.run();
        } catch (Exception ex) {
            getLogger().severe("Error while shutting down " + name + ": " + ex.getMessage());
        }
    }

    private void createManagers() {

        new PermissionManager();
        new UnitedLandsDataManager();
        new DisplayManager();
        new ConfirmationManager();
        new PlayerCacheManager();
        new UnitedLandsEconomyManager();
        new ChatChannelManager();
        new IntegrationManager();
        new FlightManager();
        new TitleManager();

        newDayScheduler = new NewDayScheduler();
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
