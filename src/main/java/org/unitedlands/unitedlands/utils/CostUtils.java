package org.unitedlands.unitedlands.utils;

import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.classes.events.country.CountryUpkeepCalculatedEvent;
import org.unitedlands.unitedlands.classes.events.region.RegionClaimCostCalculatedEvent;
import org.unitedlands.unitedlands.classes.events.region.RegionUpkeepCalculatedEvent;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementClaimCostCalculatedEvent;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementUpkeepCostCalculatedEvent;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class CostUtils {

    public static double getSettlementClaimCosts(Settlement settlement) {
        var baseCosts = UnitedLandsConfig.get().economy().costs().settlement().plotClaimBaseCosts();
        var progression = UnitedLandsConfig.get().economy().costs().settlement().plotClaimCostFormula();

        Expression expression = new ExpressionBuilder(progression)
                .variables("base", "claims")
                .build()
                .setVariable("base", baseCosts)
                .setVariable("claims", settlement.getChunks().size());

        var claimCosts = expression.evaluate();

        SettlementClaimCostCalculatedEvent event = new SettlementClaimCostCalculatedEvent(claimCosts);
        event.callEvent();

        return event.getFinalCosts();
    }

    public static double getSettlementUpkeep(Settlement settlement) {

        var baseUpkeepPerPlot = UnitedLandsConfig.get().economy().costs().settlement().plotUpkeepBaseCosts();
        var upkeepPerPlotFormula = UnitedLandsConfig.get().economy().costs().settlement().upkeepPerPlotFormula();

        Expression expression = new ExpressionBuilder(upkeepPerPlotFormula)
                .variables("base", "claims", "residents")
                .build()
                .setVariable("base", baseUpkeepPerPlot)
                .setVariable("claims", settlement.getChunks().size())
                .setVariable("residents", settlement.getCitizens().size())
                .setVariable("claims", settlement.getChunks().size());

        var upkeepCosts = expression.evaluate();

        upkeepCosts *= settlement.getChunks().size();

        SettlementUpkeepCostCalculatedEvent event = new SettlementUpkeepCostCalculatedEvent(upkeepCosts);
        event.callEvent();

        return event.getFinalCosts();
    }

    public static double getRegionClaimCosts(Country country, Region region) {
        var baseCosts = UnitedLandsConfig.get().economy().costs().country().regionClaimBaseCosts();
        var modifier = UnitedLandsConfig.get().economy().costs().country().regionClaimCostFormula();;

        var capital = country.getCapital();
        var distance = capital.getHomeChunkCoordinates().distance(region.getHomeChunkCoordinates()) * 16;

        Expression expression = new ExpressionBuilder(modifier)
                .variables("base", "regions", "distance")
                .build()
                .setVariable("base", baseCosts)
                .setVariable("regions", country.getRegions().size())
                .setVariable("distance", distance);

        var claimCosts = expression.evaluate();

        RegionClaimCostCalculatedEvent event = new RegionClaimCostCalculatedEvent(claimCosts);
        event.callEvent();

        return event.getFinalCosts();
    }

    public static double getRegionUpkeep(Region region, Country country) {

        var regionBaseUpkeep = UnitedLandsConfig.get().economy().costs().country().regionUkeepBaseCosts();
        var regionUpkeepFormula = UnitedLandsConfig.get().economy().costs().country().regionUpkeepFormula();

        var distance = region.getHomeChunkCoordinates().distance(country.getCapital().getHomeChunkCoordinates()) * 16;

        Expression expression = new ExpressionBuilder(regionUpkeepFormula)
                .variables("base", "distance")
                .build()
                .setVariable("base", regionBaseUpkeep)
                .setVariable("distance", distance);

        var upkeepCosts = expression.evaluate();

        RegionUpkeepCalculatedEvent event = new RegionUpkeepCalculatedEvent(upkeepCosts);
        event.callEvent();

        return event.getFinalCosts();
    }

    public static double getCountryUpkeep(Country country) {

        var upkeepCosts = 0d;

        for (var region : country.getRegions()) {
            upkeepCosts += getRegionUpkeep(region, country);
        }

        CountryUpkeepCalculatedEvent event = new CountryUpkeepCalculatedEvent(upkeepCosts);
        event.callEvent();

        return event.getFinalCosts();
    }

}
