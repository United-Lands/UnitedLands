package org.unitedlands.unitedlands.classes.configs;

import java.util.List;

import org.unitedlands.annotations.UnitedConfig;
import org.unitedlands.annotations.UnitedSection;
import org.unitedlands.annotations.UnitedSetting;
import org.unitedlands.registrars.config.UnitedConfigHandler;
import org.unitedlands.registrars.config.UnitedConfigs;
import org.unitedlands.registrars.config.UnitedDynamicSection;

@UnitedConfig(file = "config.yml") // file property möglich, wenn config z.B. "settings.yml" heißen soll
public interface GeneralConfig extends UnitedConfigHandler {
    static GeneralConfig get() { return UnitedConfigs.get(GeneralConfig.class); } // Pflicht

    // Einfacher "developer-mode: true/false" in yaml
    @UnitedSetting(key = "developer-mode", def = "false") 
    boolean developerMode();

    @UnitedSection(key = "mysql")
    MysqlSettings mysql();

    record MysqlSettings(
            @UnitedSetting(key = "host",     def = "localhost")     String host,
            @UnitedSetting(key = "port",     def = "3306")          int    port,
            @UnitedSetting(key = "username", def = "unitedlands")   String username,
            @UnitedSetting(key = "password", def = "unitedlands")   String password,
            @UnitedSetting(key = "database", def = "unitedlands")   String database
    ) {}

    @UnitedSection(key = "general")
    GeneralSettings general();

    record GeneralSettings(
        @UnitedSetting(key = "worlds")                                  List<String>   worlds,
        @UnitedSection(key = "new-day")                                 NewDaySettings newDay,
        @UnitedSetting(key = "playtime-record-threshold", def = "60")   int            playtimeRecordThreshold
    ) {}

    @UnitedSection (key = "economy")
    EconomySettings economy();

    record EconomySettings(
        @UnitedSection (key = "taxes") UnitedDynamicSection<TaxSettings> taxes
    ) { }

    record NewDaySettings(
            @UnitedSetting(key = "use-interval",     def = "false")  boolean useInterval,
            @UnitedSetting(key = "interval-seconds", def = "3600")   int     intervalSeconds,
            @UnitedSetting(key = "new-day-time",     def = "12:00")  String  newDayTime
    ) {}

    record TaxSettings(
        @UnitedSetting(key = "tax-mode",   def = "WEALTH") String taxMode, 
        @UnitedSetting(key = "min-percent",   def = "0.0") double minPercent, 
        @UnitedSetting(key = "max-percent",   def = "25.0") double maxPercent, 
        @UnitedSetting(key = "min-amount",   def = "0.0") double minAmount, 
        @UnitedSetting(key = "max-amount",   def = "0.0") double maxAmount, 
        @UnitedSetting(key = "tax-cap",   def = "10000.0") double taxCap
    ) { }

    record GeopolAttributeDefaults(
        @UnitedSetting(key = "current-value",   def = "0") double currentValue, 
        @UnitedSetting(key = "min-value",       def = "0") double minValue, 
        @UnitedSetting(key = "max-value",       def = "0") double maxValue, 
        @UnitedSetting(key = "daily-change",    def = "0") double dailyChange
    ) { }



}