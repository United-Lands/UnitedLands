package org.unitedlands.unitedlands.integrations.map;

import java.util.Collection;

import org.bukkit.Location;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;

public class NoOpMapRenderer implements MapRenderer {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void loadMessages() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void setDebugMode(boolean debugMode) {
    }

    @Override
    public void registerIcon(String key, String filename) {
    }

    @Override
    public void addToRenderQueue(Settlement settlement) {
    }

    @Override
    public void addToRenderQueue(Region region) {
    }

    @Override
    public void addToRenderQueue(Country country) {
    }

    @Override
    public void addSettlementsToRenderQueue(Collection<Settlement> settlements) {
    }

    @Override
    public void addRegionsToRenderQueue(Collection<Region> regions) {
    }

    @Override
    public void addCountriesToRenderQueue(Collection<Country> countries) {
    }

    @Override
    public void removeSettlement(Settlement settlement) {
    }

    @Override
    public void removeRegion(Region region) {
    }

    @Override
    public void removeCountry(Country country) {
    }

    @Override
    public void removeCountryIcon(Country country) {
    }

    @Override
    public void renderMarker(String worldName, Location location, String markerName, String markerKey,
            String layerId, String layerName) {
    }

    @Override
    public void removeMarker(String worldName, String markerKey, String layerId, String layerName) {
    }

}
