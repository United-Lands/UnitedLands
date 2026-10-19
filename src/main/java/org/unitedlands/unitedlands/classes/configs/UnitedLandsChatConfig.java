package org.unitedlands.unitedlands.classes.configs;

import java.util.List;

import org.unitedlands.annotations.UnitedConfig;
import org.unitedlands.annotations.UnitedSection;
import org.unitedlands.annotations.UnitedSetting;
import org.unitedlands.registrars.config.UnitedConfigHandler;
import org.unitedlands.registrars.config.UnitedConfigs;
import org.unitedlands.registrars.config.UnitedDynamicSection;

@UnitedConfig(file = "chat.yml")
public interface UnitedLandsChatConfig extends UnitedConfigHandler {
    static UnitedLandsChatConfig get() {
        return UnitedConfigs.get(UnitedLandsChatConfig.class); }

    @UnitedSetting(key = "show-empty-channel-message", def = "true") boolean showEmptyChannelMessage();
    @UnitedSetting(key = "tag-open", def = "[") String tagOpen();
    @UnitedSetting(key = "tag-close", def = "]") String tagClose();
    @UnitedSetting(key = "delimiter", def = ": ") String delimiter();
    @UnitedSetting(key = "player-name-placeholder") String playerNamePlaceholder();
    @UnitedSetting(key = "custom-channel-color", def = "white") String customChannelColor();
    @UnitedSetting(key = "custom-channel-lifetime", def = "259200") Long customChannelLifetime();
    @UnitedSection(key = "default-channels") UnitedDynamicSection<DefaultChatChannelSettings> defaultChannels();    
    @UnitedSection(key = "custom-channels") UnitedDynamicSection<CustomChatChannelSettings> customChannels();    

    public record DefaultChatChannelSettings(
            @UnitedSetting(key = "key") String key,
            @UnitedSetting(key = "color", def = "white") String color,
            @UnitedSetting(key = "tag") String tag,
            @UnitedSetting(key = "prefix") String prefix,
            @UnitedSetting(key = "notify-activity", def = "false") boolean notifyActivity
    ) {}

    public record CustomChatChannelSettings(
            @UnitedSetting(key = "key") String key,
            @UnitedSetting(key = "color", def = "white") String color,
            @UnitedSetting(key = "tag") String tag,
            @UnitedSetting(key = "prefix") String prefix,
            @UnitedSetting(key = "password") String password,
            @UnitedSetting(key = "notify-activity", def = "false") boolean notifyActvity,
            @UnitedSetting(key = "creator") String creator,
            @UnitedSetting(key = "last-activity") long lastActivity,
            @UnitedSetting(key = "blocked-players") List<String> blockedPlayers
    ) {}

}

