package org.unitedlands.unitedlands.classes.infoscreen;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.Region;

import org.unitedlands.unitedlands.classes.metadata.BooleanMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.DoubleMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.FloatMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.IntegerMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.LongMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.StringMetaDataField;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.utils.CostUtils;
import org.unitedlands.utils.United;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class CountryInfoScreen extends InfoScreen {

    public CountryInfoScreen(Country country) {

        var header = buildHeader(country.getCleanName());
        addComponent("header", header);

        addComponent("founded", "info-screens.country.founded",
                new SimpleDateFormat("dd-MM-yyyy HH:mm").format(country.getFoundingTimestamp()),
                country.getFounderName() != null ? country.getFounderName() : "-");

        addComponent("leader", "info-screens.country.leader",
                country.getLeader() != null ? country.getLeader().getName() : "-");

        addComponent("capital", "info-screens.country.capital",
                country.getCapital() != null ? country.getCapital().getCleanName() : "-");

        var areaComponent = MiniMessage.miniMessage().deserialize(United.messenger().get("info-screens.country.area",
                String.valueOf(country.getRegionCount()),
                String.format("%,.0f", country.getArea())))
                .hoverEvent(
                        HoverEvent.showText(
                                Component.text(
                                        String.join(", ", country.getRegions().stream().map(Region::getCleanName).toList()))));
        addComponent("area", areaComponent);

        addComponent("balance", "info-screens.country.balance",
                UnitedLandsEconomyManager.instance().format(UnitedLandsEconomyManager.instance().getBalance(country.getUuid())),
                UnitedLandsEconomyManager.instance().format(CostUtils.getCountryUpkeep(country)));

        var ongoingClaims = UnitedLandsDataManager.instance().getRegionClaimsOngoing(country);
        if (ongoingClaims.size() > 0) {
            List<String> claimsList = new ArrayList<>();
            for (var claim : ongoingClaims)
                claimsList.add("<red>" + claim.getCleanName() + "</red> ("
                        + United.formatter().formatDuration(claim.getClaimEndTime() - System.currentTimeMillis()) + ")");

            addComponent("claims", United.messenger().get("info-screens.country.claims", String.join(", ", claimsList)));
        }

        var metadata = country.getMetadata();

        if (metadata != null && !metadata.isEmpty()) {

            var metaDataWrapper = United.messenger().get("info-screens.country.metadata");

            List<String> fields = new ArrayList<>();
            for (var m : metadata.values()) {
                if (!m.showInScreens())
                    continue;
                if (m.getValue() == null)
                    continue;
                var field = "<bold>" + m.getLabel() + "</bold>: ";
                if (m instanceof StringMetaDataField typedData) {
                    field += typedData.getValue();
                } else if (m instanceof IntegerMetaDataField typedData) {
                    field += typedData.getValue();
                } else if (m instanceof LongMetaDataField typedData) {
                    field += typedData.getValue();
                } else if (m instanceof FloatMetaDataField typedData) {
                    field += String.format("%.2f", typedData.getValue());
                } else if (m instanceof DoubleMetaDataField typedData) {
                    field += String.format("%.2f", typedData.getValue());
                } else if (m instanceof BooleanMetaDataField typedData) {
                    field += typedData.getValue() ? "Yes" : "No";
                }
                fields.add(field);
            }

            if (!fields.isEmpty()) {
                var finalMetaDataString = metaDataWrapper.replace("{metadata}",
                        String.join("<dark_gray> | </dark_gray>", fields));
                var metadataComponent = MiniMessage.miniMessage().deserialize(finalMetaDataString);
                addComponent("metadata", metadataComponent);
            }
        }
    }

}
