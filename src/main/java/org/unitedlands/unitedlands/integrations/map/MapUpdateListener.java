package org.unitedlands.unitedlands.integrations.map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.annotations.UnitedListener;
import org.unitedlands.unitedlands.classes.events.DataLoadedEvent;
import org.unitedlands.unitedlands.classes.events.country.CountryCreatedEvent;
import org.unitedlands.unitedlands.classes.events.country.CountryRemovedEvent;
import org.unitedlands.unitedlands.classes.events.country.CountryUpdatedEvent;
import org.unitedlands.unitedlands.classes.events.region.RegionCreatedEvent;
import org.unitedlands.unitedlands.classes.events.region.RegionRemovedEvent;
import org.unitedlands.unitedlands.classes.events.region.RegionUpdatedEvent;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementCreatedEvent;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementRemovedEvent;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementUpdatedEvent;
import org.unitedlands.unitedlands.classes.events.settlementchunk.SettlementChunkCreatedEvent;
import org.unitedlands.unitedlands.classes.events.settlementchunk.SettlementChunkRemovedEvent;
import org.unitedlands.unitedlands.managers.IntegrationManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;

@UnitedListener
public class MapUpdateListener implements Listener {

    private MapRenderer renderer() {
        return IntegrationManager.instance().getMapRenderer();
    }

    @EventHandler
    public void onDataLoaded(DataLoadedEvent event) {
        var renderer = renderer();
        var dataManager = UnitedLandsDataManager.instance();

        for (var country : dataManager.getCountries()) {
            if (country.getIconPosition() != null)
                renderer.registerIcon(country.getUuid().toString(), country.getUuid().toString() + ".png");
        }

        renderer.setDebugMode(false);
        renderer.addSettlementsToRenderQueue(dataManager.getSettlements());
        renderer.addRegionsToRenderQueue(dataManager.getRegions());
        renderer.addCountriesToRenderQueue(dataManager.getCountries());
    }

    @EventHandler
    public void onSettlementCreated(SettlementCreatedEvent event) {
        renderer().addToRenderQueue(event.getSettlement());
    }

    @EventHandler
    public void onSettlementUpdated(SettlementUpdatedEvent event) {
        if (event.isVisualChange())
            renderer().addToRenderQueue(event.getSettlement());
    }

    @EventHandler
    public void onSettlementRemoved(SettlementRemovedEvent event) {
        renderer().removeSettlement(event.getSettlement());
    }

    @EventHandler
    public void onSettlementChunkCreated(SettlementChunkCreatedEvent event) {
        if (event.getSettlement() != null)
            renderer().addToRenderQueue(event.getSettlement());
    }

    @EventHandler
    public void onSettlementChunkRemoved(SettlementChunkRemovedEvent event) {
        if (event.getSettlement() != null)
            renderer().addToRenderQueue(event.getSettlement());
    }

    @EventHandler
    public void onRegionCreated(RegionCreatedEvent event) {
        renderer().addRegionWithDependentsToRenderQueue(event.getRegion());
    }

    @EventHandler
    public void onRegionUpdated(RegionUpdatedEvent event) {
        if (event.isVisualChange())
            renderer().addRegionWithDependentsToRenderQueue(event.getRegion());
    }

    @EventHandler
    public void onRegionRemoved(RegionRemovedEvent event) {
        renderer().removeRegion(event.getRegion());
    }

    @EventHandler
    public void onCountryCreated(CountryCreatedEvent event) {
        renderer().addCountryWithDependentsToRenderQueue(event.getCountry());
    }

    @EventHandler
    public void onCountryUpdated(CountryUpdatedEvent event) {
        if (event.isVisualChange())
            renderer().addCountryWithDependentsToRenderQueue(event.getCountry());
    }

    @EventHandler
    public void onCountryRemoved(CountryRemovedEvent event) {
        renderer().removeCountry(event.getCountry());
    }

}
