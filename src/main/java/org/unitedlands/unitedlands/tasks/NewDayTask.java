package org.unitedlands.unitedlands.tasks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.BankRecord;
import org.unitedlands.unitedlands.classes.BankRecordTransactionType;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.PlaytimeRecord;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.ULCINCRecord;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.classes.events.NewDayEvent;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.schedulers.NewDayScheduler;
import org.unitedlands.unitedlands.utils.ActivityScoreCalculator;
import org.unitedlands.unitedlands.utils.CostUtils;
import org.unitedlands.utils.United;

public class NewDayTask implements Runnable {

    private long dayStart;
    private volatile List<ULCINCRecord> previousUlcincRecords;

    @Override
    public void run() {

        if (!NewDayScheduler.instance().tryStartNewDay()) {
            United.logger().warning("A new day is already in progress, skipping this run.", "UnitedLands");
            return;
        }

        dayStart = System.currentTimeMillis();

        try {
            United.messenger().send(Bukkit.getServer(), "new-day.start");

            // Start the new day by handling citizen data asynchronously
            handleCitizens();
        } catch (Exception ex) {
            logError("starting the new day", ex);
            handleSettlementsAndCountries();
        }
    }

    private void handleCitizens() {

        var citizenIds = UnitedLandsDataManager.instance().getCitizens().stream().map(Citizen::getUuid).toList();

        // Update on a different thread due to potentially large number of playtime and bank records
        Bukkit.getScheduler().runTaskAsynchronously(UnitedLands.instance(), () -> {

            Map<UUID, Double> calculatedScores = new HashMap<>(citizenIds.size());
            Map<UUID, Double> calculatedIncome = new HashMap<>(citizenIds.size());

            try {
                previousUlcincRecords = UnitedLandsDataManager.instance()
                        .getRecentUlcincRecords(UnitedLandsConfig.get().general().newDay().ulcinc().smoothingDays());
            } catch (Exception ex) {
                logError("loading previous ULCINC records", ex);
            }

            try {
                var playtimeRecords = UnitedLandsDataManager.instance().getRecentPlaytimeRecords(60);

                // We need both NPC and player trade income
                var incomeWindowStart = getIncomeWindowStart();
                var incomeRecords = new ArrayList<>(UnitedLandsDataManager.instance().getBankRecords(BankRecordTransactionType.TRADE_NPC, incomeWindowStart, dayStart));
                incomeRecords.addAll(UnitedLandsDataManager.instance().getBankRecords(BankRecordTransactionType.TRADE_PLAYER, incomeWindowStart, dayStart));

                for (UUID citizenId : citizenIds) {

                    // Update activity score
                    List<PlaytimeRecord> playerRecords = playtimeRecords.stream().filter(r -> citizenId.equals(r.getPlayerUuid())).toList();
                    double score = ActivityScoreCalculator.calculateActivityScore(playerRecords);
                    calculatedScores.put(citizenId, score);

                    // Update last day's income
                    double income = incomeRecords.stream().filter(r -> citizenId.equals(r.getSubject())).collect(Collectors.summingDouble(BankRecord::getAmount));
                    calculatedIncome.put(citizenId, income);
                }
            } catch (Exception ex) {
                logError("calculating citizen activity scores and income", ex);
            }

            // Continue on main thread
            Bukkit.getScheduler().runTask(UnitedLands.instance(), () -> {

                try {
                    for (UUID citizenId : calculatedScores.keySet()) {
                        Citizen citizen = UnitedLandsDataManager.instance().getCitizen(citizenId);
                        if (citizen != null) {
                            citizen.setActivityScore(calculatedScores.get(citizenId));
                            citizen.setLastDayIncome(calculatedIncome.getOrDefault(citizenId, 0d));
                        }
                    }

                    United.logger().info("Updated activity scores and income, saving " + calculatedScores.size() + " citizens records");
                    UnitedLandsDataManager.instance().updateCitizenDbData(UnitedLandsDataManager.instance().getCitizens());
                } catch (Exception ex) {
                    logError("updating citizen records", ex);
                }

                // Proceed to settlements and countries
                handleSettlementsAndCountries();
            });
        });
    }

    private void handleSettlementsAndCountries() {
        try {
            handleSettlements();
        } catch (Exception ex) {
            logError("processing settlements", ex);
        }

        try {
            // Proceed to countries
            handleCountries();
        } catch (Exception ex) {
            logError("processing countries", ex);
        } finally {
            NewDayScheduler.instance().finishNewDay();
            new NewDayEvent(dayStart).callEvent();
        }
    }

    private void handleSettlements() {

        var settlements = new ArrayList<>(UnitedLandsDataManager.instance().getSettlements());

        for (Settlement settlement : settlements) {
            try {
                handleSettlement(settlement);
            } catch (Exception ex) {
                logError("processing settlement " + settlement.getName(), ex);
            }
        }

    }

    private void handleSettlement(Settlement settlement) {

        if (settlement.isBankrupt()) {

            var gracePeriod = UnitedLandsConfig.get().economy().costs().settlement().bankruptcyGracePeriod() * 1000;
            var elapsedTime = System.currentTimeMillis() - getBankruptcyTimestamp(settlement);

            if (elapsedTime > gracePeriod) {
                // TODO: actual removal
            }

        } else {
            var upkeep = CostUtils.getSettlementUpkeep(settlement);

            if (!UnitedLandsEconomyManager.instance().withdraw(settlement.getUuid(), upkeep,
                    BankRecordTransactionType.UPKEEP, "Daily upkeep")) {
                United.logger().error("Settlement " + settlement.getName() + " could not pay "
                        + UnitedLandsEconomyManager.instance().format(upkeep) + " upkeep.", "UnitedLands");
                return;
            }
            United.logger().info("Settlement " + settlement.getName() + " paid "
                    + UnitedLandsEconomyManager.instance().format(upkeep) + " upkeep.");

            United.messenger().send(settlement.getOnlinePlayers(),
                    "new-day.settlement-upkeep-notice",
                    UnitedLandsEconomyManager.instance().format(upkeep));

            var balance = UnitedLandsEconomyManager.instance().getBalance(settlement.getUuid()).doubleValue();

            // If the new balance is below 0, mark the settlement as bankrupt.
            if (balance < 0) {
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
                                UnitedLandsConfig.get().economy().costs().settlement().bankruptcyGracePeriod()
                                        * 1000));
            }
        }
    }

    private void handleCountries() {

        // ULCINC totals


        // Get all citizens that are in a country
        List<Citizen> allCountryCitizens = UnitedLandsDataManager.instance().getCitizens().stream().filter(c -> c.hasCountry()).toList();

        // Total trade income of all country citizens
        var totalIncome = allCountryCitizens.stream().collect(Collectors.summingDouble(Citizen::getLastDayIncome));
        // Total activity score of all country citizens
        var totalActivity = allCountryCitizens.stream().collect(Collectors.summingDouble(Citizen::getActivityScore));

        // Total number of country settlement on the map
        var totalSettlements = UnitedLandsDataManager.instance().getSettlements().stream().filter(s -> s.hasCountry()).collect(Collectors.counting());
        // Total number of regions on the map
        var totalRegions = UnitedLandsDataManager.instance().getRegions().size();

        // Get the total balances of countries and settlements. Store them for later use.
        Map<UUID, Double> countryBalances = new HashMap<>();
        for (var country : UnitedLandsDataManager.instance().getCountries()) {
            var balance = 0d;
            balance += country.getBalance();
            for (var settlement : country.getSettlements()) {
                balance += settlement.getBalance();
            }
            countryBalances.put(country.getUuid(), Math.max(0d, balance));
        }

        // Total wealth of all countries and their settlements
        var totalWealth = countryBalances.values().stream().collect(Collectors.summingDouble(Double::valueOf));

        var smoothingDays = UnitedLandsConfig.get().general().newDay().ulcinc().smoothingDays();
        var wealthWeight = UnitedLandsConfig.get().general().newDay().ulcinc().economyWeights().wealth();
        var incomeWeight = UnitedLandsConfig.get().general().newDay().ulcinc().economyWeights().income();
        var regionWeight = UnitedLandsConfig.get().general().newDay().ulcinc().geographyWeights().region();
        var settlementWeight = UnitedLandsConfig.get().general().newDay().ulcinc().geographyWeights().settlement();

        var ulcincRecords = previousUlcincRecords != null ? previousUlcincRecords
                : UnitedLandsDataManager.instance().getRecentUlcincRecords(smoothingDays);
        Map<UUID, List<ULCINCRecord>> ulcincRecordsByCountry = ulcincRecords.stream()
                .filter(r -> r.getCountryId() != null)
                .collect(Collectors.groupingBy(ULCINCRecord::getCountryId));

        List<ULCINCRecord> newUlcincRecords = new ArrayList<>();

        for (Country country : new ArrayList<>(UnitedLandsDataManager.instance().getCountries())) {

            try {

                // ULCINC calculations

                var countryCitizens = country.getCitizens();

                var countryIncome = countryCitizens.stream().collect(Collectors.summingDouble(Citizen::getLastDayIncome));
                var countryActivity = countryCitizens.stream().collect(Collectors.summingDouble(Citizen::getActivityScore));
                var countryWealth = countryBalances.get(country.getUuid());
                var countrySettlements = country.getSettlements().size();
                var countryRegions = country.getRegions().size();

                // Gather today's indices
                // Check for null divisions and use Math.max() to clamp to score >= 0

                var activityIndex = 0d;
                if (totalActivity > 0d) {
                    activityIndex = Math.max(0d, countryActivity / totalActivity);
                }

                var economyIndex = 0d;
                var economyWeightedShares = 0d;
                var economyWeightSum = 0d;
                if (totalIncome > 0d) {
                    economyWeightedShares += incomeWeight * Math.max(0d, countryIncome / totalIncome);
                    economyWeightSum += incomeWeight;
                }
                if (totalWealth > 0d && Double.isFinite(totalWealth)) {
                    economyWeightedShares += wealthWeight * (countryWealth / totalWealth);
                    economyWeightSum += wealthWeight;
                }
                if (economyWeightSum > 0d) {
                    economyIndex = Math.max(0d, economyWeightedShares / economyWeightSum);
                }

                var miltaryIndex = 0d;

                var geographyIndex = 0d;
                if (totalRegions > 0d && totalSettlements > 0d) {
                    geographyIndex = Math.max(0,
                        ((regionWeight * (double) countryRegions) + (settlementWeight * (double) countrySettlements)) /
                        ((settlementWeight * (double) totalSettlements) + (regionWeight * (double) totalRegions))
                    );
                }

                var newTotalIndex = ((activityIndex + economyIndex + miltaryIndex + geographyIndex) / 4d) * 100d;

                // Get previous records for this country
                var oldUlcincRecords = ulcincRecordsByCountry.getOrDefault(country.getUuid(), List.of()).stream()
                        .sorted(Comparator.comparingLong(ULCINCRecord::getTimestamp).reversed())
                        .limit(smoothingDays)
                        .toList();
                // Sum the totals for smoothing
                var oldTotalIndexSum = oldUlcincRecords.stream().collect(Collectors.summingDouble(ULCINCRecord::getTotalIndex));

                // The new score
                var smoothedTotalIndex = (oldTotalIndexSum + newTotalIndex) / ((double)oldUlcincRecords.size() + 1d);

                var countryRecord = new ULCINCRecord(country, activityIndex, economyIndex, miltaryIndex, geographyIndex, newTotalIndex, smoothedTotalIndex);
                countryRecord.setTimestamp(dayStart);
                newUlcincRecords.add(countryRecord);

                country.setUlcinc(smoothedTotalIndex);
                country.save();

                // Upkeep and bankruptcy

                handleCountryUpkeep(country);

            } catch (Exception ex) {
                logError("processing country " + country.getName(), ex);
            }

        }

        // Save the new ULCINC records in one batch

        UnitedLandsDataManager.instance().createUlcincRecordDbData(newUlcincRecords);
        United.logger().info("Saving " + newUlcincRecords.size() + " ULCINC records...");

        United.logger().info("------------ New Day Statistics ---------------");
        United.logger().info("Total country players: " + allCountryCitizens.size());
        United.logger().info("Total activity of country players: " + totalActivity + " (average: " + String.format("%,.2f", allCountryCitizens.isEmpty() ? 0d : totalActivity / allCountryCitizens.size()) + " per player)");
        United.logger().info("Total income of country players: " + String.format("%,.2f", totalIncome));
        United.logger().info("Total country and settlement wealth: " + String.format("%,.2f", totalWealth));
        United.logger().info("----------------------------------------------");
    }

    private void handleCountryUpkeep(Country country) {

        if (country.isBankrupt()) {
            var gracePeriod = UnitedLandsConfig.get().economy().costs().country().bankruptcyGracePeriod() * 1000;
            var elapsedTime = System.currentTimeMillis() - getBankruptcyTimestamp(country);

            if (elapsedTime > gracePeriod) {
                // TODO: actual removal
            }

        } else {

            var upkeep = CostUtils.getCountryUpkeep(country);

            if (!UnitedLandsEconomyManager.instance().withdraw(country.getUuid(), upkeep,
                    BankRecordTransactionType.UPKEEP, "Daily upkeep")) {
                United.logger().error("Country " + country.getName() + " could not pay "
                        + UnitedLandsEconomyManager.instance().format(upkeep) + " upkeep.", "UnitedLands");
                return;
            }
            United.logger().info("Country " + country.getName() + " paid "
                    + UnitedLandsEconomyManager.instance().format(upkeep) + " upkeep.", "UnitedLands");

            United.messenger().send(country.getOnlinePlayers(),
                    "new-day.country-upkeep-notice",
                    UnitedLandsEconomyManager.instance().format(upkeep));

            var balance = UnitedLandsEconomyManager.instance().getBalance(country.getUuid()).doubleValue();

            // If the new balance is below 0, mark the country as bankrupt.
            if (balance < 0) {
                country.setBankrupt(true);
                country.setBankruptcyTimestamp(System.currentTimeMillis());
                country.save();

                United.messenger().broadcast("new-day.country-bankrupt-broadcast",
                        country.getCleanName());

                United.messenger().send(country.getOnlinePlayers(),
                        "new-day.country-bankrupt-notice",
                        UnitedLandsEconomyManager.instance()
                                .format(UnitedLandsConfig.get().economy().costs().country()
                                        .bankruptcyStopThreshold()),
                        United.formatter().formatDuration(
                                UnitedLandsConfig.get().economy().costs().country().bankruptcyGracePeriod()
                                        * 1000));
            }
        }
    }

    private long getIncomeWindowStart() {
        var lastNewDay = UnitedLandsDataManager.instance().getLatestUlcincTimestamp();
        if (lastNewDay != null && lastNewDay < dayStart)
            return lastNewDay;

        var newDaySettings = UnitedLandsConfig.get().general().newDay();
        long fallbackWindow = newDaySettings.useInterval()
                ? newDaySettings.intervalSeconds() * 1000L
                : 24L * 60 * 60 * 1000;
        return dayStart - fallbackWindow;
    }

    private long getBankruptcyTimestamp(GeopolObject object) {
        if (object.getBankruptcyTimestamp() == null) {
            object.setBankruptcyTimestamp(System.currentTimeMillis());
            object.save();
        }
        return object.getBankruptcyTimestamp();
    }

    private void logError(String step, Exception ex) {
        UnitedLands.instance().getLogger().log(Level.SEVERE, "New day: error while " + step + ": " + ex.getMessage(), ex);
    }


}
