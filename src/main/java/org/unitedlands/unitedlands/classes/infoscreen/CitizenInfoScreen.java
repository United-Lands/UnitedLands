package org.unitedlands.unitedlands.classes.infoscreen;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.unitedlands.unitedlands.classes.Citizen;

import org.unitedlands.unitedlands.classes.metadata.BooleanMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.DoubleMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.FloatMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.IntegerMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.LongMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.StringMetaDataField;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

public class CitizenInfoScreen extends InfoScreen {

    public CitizenInfoScreen(Citizen citizen) {

        var header = buildHeader(citizen.getName());
        addComponent("header", header);

        var joinDate = new SimpleDateFormat("dd-MM-yyyy HH:mm").format(citizen.getJoined());
        var logonDate = new SimpleDateFormat("dd-MM-yyyy HH:mm").format(citizen.getLastLogon());

        addComponent("registered",
                "info-screens.citizen.registered", joinDate, logonDate);

        addComponent("settlement-country",
                "info-screens.citizen.country",
                citizen.getSettlement() != null ? citizen.getSettlement().getCleanName() : "-",
                citizen.getCountry() != null ? citizen.getCountry().getCleanName() : "-");

        addComponent("settlement-ranks",
                "info-screens.citizen.settlement-ranks",
                citizen.getSettlementRanks());

        addComponent("country-ranks",
                "info-screens.citizen.country-ranks",
                String.join(", ", citizen.getCountryRanks()));

        addComponent("balance",
                "info-screens.citizen.balance",
                UnitedLandsEconomyManager.instance().format(UnitedLandsEconomyManager.instance().getBalance(citizen.getUuid())));

        var metadata = citizen.getMetadata();

        if (metadata != null && !metadata.isEmpty()) {

            var metaDataWrapper = United.messenger().get("info-screens.citizen.metadata");

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
                var metadataComponent = United.miniMessage().get().deserialize(finalMetaDataString);
                addComponent("metadata", metadataComponent);
            }

        }
    }

}
