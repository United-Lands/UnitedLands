package org.unitedlands.unitedlands.integrations.map;

import java.util.Collection;

import org.bukkit.Location;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;

public interface MapRenderer {

    boolean isAvailable();

    void initialize();

    void loadMessages();

    void shutdown();

    void setDebugMode(boolean debugMode);

    void registerIcon(String key, String filename);

    void addToRenderQueue(Settlement settlement);

    void addToRenderQueue(Region region);

    void addToRenderQueue(Country country);

    void addSettlementsToRenderQueue(Collection<Settlement> settlements);

    void addRegionsToRenderQueue(Collection<Region> regions);

    void addCountriesToRenderQueue(Collection<Country> countries);

    default void addRegionWithDependentsToRenderQueue(Region region) {
        for (var settlement : region.getSettlements()) {
            addToRenderQueue(settlement);
        }
        addToRenderQueue(region);
    }

    default void addCountryWithDependentsToRenderQueue(Country country) {
        for (var region : country.getRegions()) {
            for (var settlement : region.getSettlements()) {
                addToRenderQueue(settlement);
            }
            addToRenderQueue(region);
        }
        addToRenderQueue(country);
        if (country.getOverlord() != null) {
            addCountryWithDependentsToRenderQueue(country.getOverlord());
        }
    }

    void removeSettlement(Settlement settlement);

    void removeRegion(Region region);

    void removeCountry(Country country);

    void removeCountryIcon(Country country);

    void renderMarker(String worldName, Location location, String markerName, String markerKey, String layerId,
            String layerName);

    void removeMarker(String worldName, String markerKey, String layerId, String layerName);

}
