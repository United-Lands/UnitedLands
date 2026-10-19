package org.unitedlands.unitedlands.classes.configs;

import java.util.List;

import org.unitedlands.annotations.UnitedConfig;
import org.unitedlands.annotations.UnitedSection;
import org.unitedlands.annotations.UnitedSetting;
import org.unitedlands.registrars.config.UnitedConfigHandler;
import org.unitedlands.registrars.config.UnitedConfigs;
import org.unitedlands.registrars.config.UnitedDynamicSection;

@UnitedConfig(file = "config.yml") 
public interface UnitedLandsConfig extends UnitedConfigHandler {
    static UnitedLandsConfig get() { return UnitedConfigs.get(UnitedLandsConfig.class); } // Pflicht

    @UnitedSetting(key = "developer-mode", def = "false") 
    boolean developerMode();

    @UnitedSection(key = "mysql")
    MysqlSettings mysql();

    @UnitedSection(key = "general")
    GeneralSettings general();

    @UnitedSection (key = "economy")
    EconomySettings economy();

    @UnitedSection (key = "import")
    ImportSettings importSetting();

    @UnitedSection (key = "map-rendering")
    UnitedDynamicSection<MapRenderSettings> mapRendering();

    @UnitedSection(key = "geopol-attribute-defaults")
    DefaultGeopolAttributes geopolAttributeDefaults();
        
    @UnitedSection (key = "protection")
    ProtectionSettings protection();

    @UnitedSection (key = "settlement-chunks")
    UnitedDynamicSection<SettlementChunkSettings> settlementChunks();

    record MapRenderSettings(
        @UnitedSetting(key = "fill",    def = "0")  int     fill,
        @UnitedSetting(key = "stroke",  def = "0")  int     stroke,
        @UnitedSetting(key = "width",   def = "1")  int     width,
        @UnitedSetting(key = "dash",    def = "0")  String  dash
    ) {}

    record MysqlSettings(
            @UnitedSetting(key = "host",     def = "localhost")     String host,
            @UnitedSetting(key = "port",     def = "3306")          int    port,
            @UnitedSetting(key = "username", def = "unitedlands")   String username,
            @UnitedSetting(key = "password", def = "unitedlands")   String password,
            @UnitedSetting(key = "database", def = "unitedlands")   String database
    ) {}


    record GeneralSettings(
        @UnitedSetting(key = "worlds")                                  List<String>   worlds,
        @UnitedSection(key = "new-day")                                 NewDaySettings newDay,
        @UnitedSetting(key = "playtime-record-threshold", def = "60")                       long    playtimeRecordThreshold,
        @UnitedSetting(key = "protect-unclaimed-land", def = "false")                       boolean protectUnclaimedLand,
        @UnitedSetting(key = "allow-settlement-claims-outside-home-region", def = "true")   boolean allowSettlementClaimsOutsideHomeRegion,
        @UnitedSection(key = "flight")                                 FlightSettings flight

    ) {}

    record NewDaySettings(
            @UnitedSetting(key = "use-interval",     def = "false")  boolean useInterval,
            @UnitedSetting(key = "interval-seconds", def = "3600")   int     intervalSeconds,
            @UnitedSetting(key = "new-day-time",     def = "12:00")  String  newDayTime
    ) {}

    record FlightSettings(
            @UnitedSetting(key = "enabled", def = "false")          boolean         enabled,
            @UnitedSetting(key = "permissions")                     List<String>    permissions,
            @UnitedSetting(key = "any-settlement-override-permission")  List<String>  anySettlementOverridePermissions,
            @UnitedSetting(key = "wilderness-override-permission")  List<String>    wildernessOverridePermissions,
            @UnitedSetting(key = "dimension-override-permission")  List<String>    dimensionOverridePermissions
    ) { }

    record EconomySettings(
        @UnitedSection (key = "taxes") UnitedDynamicSection<TaxSettings> taxes,
        @UnitedSection (key = "costs") CostSettings                      costs
    ) { }

    record TaxSettings(
        @UnitedSetting(key = "min-percent",   def = "0.0") double minPercent, 
        @UnitedSetting(key = "max-percent",   def = "25.0") double maxPercent
    ) { }

    public record GeopolAttributeDefaults(
        @UnitedSetting(key = "display",         def = "")  String display, 
        @UnitedSetting(key = "current-value",   def = "0") double currentValue, 
        @UnitedSetting(key = "min-value",       def = "0") double minValue, 
        @UnitedSetting(key = "max-value",       def = "0") double maxValue, 
        @UnitedSetting(key = "daily-change",    def = "0") double dailyChange
    ) { }

    record ImportSettings(
        @UnitedSetting(key = "scale",   def = "1")      double  scale, 
        @UnitedSetting(key = "offset-x",   def = "0")   int     offsetX, 
        @UnitedSetting(key = "offset-y",   def = "0")   int     offsetY
    ) {}

    record CostSettings(
        @UnitedSection (key = "settlement") SettlementCostSettings  settlement,        
        @UnitedSection (key = "country")    CountryCostSettings     country
    ) {}

    record SettlementCostSettings(
        @UnitedSetting(key = "create",                      def = "8000")   int     createCosts, 
        @UnitedSetting(key = "bankruptcy-stop-threshold",   def = "10000")  int     bankruptcyStopThreshold, 
        @UnitedSetting(key = "bankruptcy-grace-period",     def = "259195") long    bankruptcyGracePeriod, 
        @UnitedSetting(key = "plot-claim-base",             def = "48")     int     plotClaimBaseCosts, 
        @UnitedSetting(key = "plot-claim-cost-formula",     def = "base")   String  plotClaimCostFormula, 
        @UnitedSetting(key = "plot-upkeep-base",            def = "3")      int     plotUpkeepBaseCosts, 
        @UnitedSetting(key = "upkeep-per-plot-formula", def = "base")       String  upkeepPerPlotFormula 
    ) {}

    record CountryCostSettings(
        @UnitedSetting(key = "create",                      def = "80000")  int     createCosts, 
        @UnitedSetting(key = "bankruptcy-stop-threshold",   def = "10000")  int     bankruptcyStopThreshold, 
        @UnitedSetting(key = "bankruptcy-grace-period",     def = "259195") long    bankruptcyGracePeriod, 
        @UnitedSetting(key = "region-claim-base",           def = "8000")   int     regionClaimBaseCosts, 
        @UnitedSetting(key = "region-claim-cost-formula",   def = "base")   String  regionClaimCostFormula, 
        @UnitedSetting(key = "region-upkeep-base",          def = "250")    int     regionUkeepBaseCosts, 
        @UnitedSetting(key = "region-upkeep-formula",       def = "base")   String  regionUpkeepFormula 
    ) {}

    record DefaultGeopolAttributes(
        @UnitedSection(key = "settlement") UnitedDynamicSection<GeopolAttributeDefaults> settlement,         
        @UnitedSection(key = "country") UnitedDynamicSection<GeopolAttributeDefaults> country 
    ) { }

    record ProtectionSettings(
        @UnitedSetting (key = "containers")     List<String> containers,
        @UnitedSetting (key = "use-blocks")     List<String> useBlocks,
        @UnitedSetting (key = "switch-tags")    List<String> switchTags,
        @UnitedSetting (key = "switch-blocks")  List<String> switchBlocks,
        @UnitedSetting (key = "interact")       List<String> interact,
        @UnitedSetting (key = "monsters")       List<String> monsters,
        @UnitedSetting (key = "animals")        List<String> animals

    ) {}

    public record SettlementChunkSettings(
        @UnitedSetting(key = "map-icon",            def = "+")  String  mapIcon, 
        @UnitedSetting(key = "map-icon-color",      def = "§f") String  mapIconColor, 
        @UnitedSetting(key = "max-per-settlement",  def = "-1") int     maxPerSettlement
    ) {}




}