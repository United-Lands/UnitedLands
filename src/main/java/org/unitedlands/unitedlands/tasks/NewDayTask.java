package org.unitedlands.unitedlands.tasks;

import org.bukkit.Bukkit;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.schedulers.NewDayScheduler;
import org.unitedlands.unitedlands.utils.CostUtils;
import org.unitedlands.utils.United;

public class NewDayTask implements Runnable {

    @Override
    public void run() {

        United.messenger().send(Bukkit.getServer(), "new-day.start");

        var settlements = UnitedLandsDataManager.instance().getSettlements();

        // Map<Country, Double> countryTaxTotals = new HashMap<>();

        for (Settlement settlement : settlements) {

            if (settlement.isBankrupt()) {

                var gracePeriod = UnitedLandsConfig.get().economy().costs().settlement().bankruptcyGracePeriod() * 1000;
                var elapsedTime = System.currentTimeMillis() - settlement.getBankruptcyTimestamp();

                if (elapsedTime > gracePeriod) {
                       // TODO: actual removal
                }

            } else {
                var balance = UnitedLandsEconomyManager.instance().getBalance(settlement.getUuid()).doubleValue();
                var upkeep = CostUtils.getSettlementUpkeep(settlement);

                UnitedLandsEconomyManager.instance().withdraw(settlement.getUuid(), upkeep, "Daily upkeep");
                United.logger().info("Settlement " + settlement.getName() + " paid "
                        + UnitedLandsEconomyManager.instance().format(upkeep) + " upkeep.", "UnitedLands");

                United.messenger().send(settlement.getOnlinePlayers(),
                        "new-day.settlement-upkeep-notice",
                        UnitedLandsEconomyManager.instance().format(upkeep));

                // If the new balance is below 0, mark the settlement as bankrupt.
                if (balance - upkeep < 0) {
                    settlement.setBankrupt(true);
                    settlement.setBankruptcyTimestamp(System.currentTimeMillis());
                    settlement.save();

                    United.messenger().broadcast("new-day.settlement-bankrupt-broadcast",
                            settlement.getCleanName());

                    United.messenger().send(settlement.getOnlinePlayers(),
                            "new-day.settlement-bankrupt-notice",
                            UnitedLandsEconomyManager.instance().format(
                                    UnitedLandsConfig.get().economy().costs().settlement().bankruptcyStopThreshold()),
                            United.formatter().formatDuration(
                                UnitedLandsConfig.get().economy().costs().settlement().bankruptcyGracePeriod() * 1000
                            ));
                }
            }

        }

        for (

        Country country : UnitedLandsDataManager.instance().getCountries()) {

            if (country.isBankrupt()) {
                var gracePeriod = UnitedLandsConfig.get().economy().costs().country().bankruptcyGracePeriod() * 1000;
                var elapsedTime = System.currentTimeMillis() - country.getBankruptcyTimestamp();

                if (elapsedTime > gracePeriod) {
                       // TODO: actual removal
                }
                
            } else {

                var balance = UnitedLandsEconomyManager.instance().getBalance(country.getUuid()).doubleValue();
                var upkeep = CostUtils.getCountryUpkeep(country);

                UnitedLandsEconomyManager.instance().withdraw(country.getUuid(), upkeep, "Daily upkeep");
                United.logger().info("Country " + country.getName() + " paid "
                        + UnitedLandsEconomyManager.instance().format(upkeep) + " upkeep.", "UnitedLands");

                United.messenger().send(country.getOnlinePlayers(),
                        "new-day.country-upkeep-notice",
                        UnitedLandsEconomyManager.instance().format(upkeep));

                // If the new balance is below 0, mark the settlement as bankrupt.
                if (balance - upkeep < 0) {
                    country.setBankrupt(true);
                    country.setBankruptcyTimestamp(System.currentTimeMillis());
                    country.save();

                    United.messenger().broadcast("new-day.country-bankrupt-broadcast",
                            country.getCleanName());

                    United.messenger().send(country.getOnlinePlayers(),
                            "new-day.country-bankrupt-notice",
                            UnitedLandsEconomyManager.instance()
                                    .format(UnitedLandsConfig.get().economy().costs().country().bankruptcyStopThreshold()),
                            United.formatter().formatDuration(
                                UnitedLandsConfig.get().economy().costs().country().bankruptcyGracePeriod() * 1000
                            ));
                }
            }
        }

        NewDayScheduler.instance().finishNewDay();
    }

}
