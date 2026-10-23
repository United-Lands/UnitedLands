package org.unitedlands.unitedlands.managers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.util.InternalException;
import org.bukkit.Bukkit;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.BankRecord;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.integrations.economy.IEconomyProvider;
import org.unitedlands.unitedlands.integrations.economy.VaultEconomyProvider;
import org.unitedlands.utils.United;

public class UnitedLandsEconomyManager {

    private static UnitedLandsEconomyManager instance;

    public static UnitedLandsEconomyManager instance() {
        return instance;
    }

    private IEconomyProvider economyProvider;

    public UnitedLandsEconomyManager() {
        instance = this;
    }

    public boolean hasEconomy() {
        return economyProvider != null;
    }

    public void loadEconomy() {
        if (UnitedLands.instance().getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                economyProvider = new VaultEconomyProvider(UnitedLands.instance());
                United.logger().info("Found Vault, enabling economy...", "UnitedLands");
            } catch (InternalException ex) {
                economyProvider = null;
                United.logger().error("Error creating Vault economy provider.", "UnitedLands");
            }
        }
    }

    public String format(int amount) {
        if (economyProvider == null)
            return String.valueOf(amount);
        return economyProvider.format(new BigDecimal(amount));
    }

    public String format(double amount) {
        if (economyProvider == null)
            return String.valueOf(amount);
        return economyProvider.format(new BigDecimal(amount));
    }

    public String format(BigDecimal amount) {
        if (economyProvider == null)
            return String.valueOf(amount);
        return economyProvider.format(amount);
    }

    public void createAccount(UUID uuid, String name) {
        if (economyProvider == null)
            return;
        economyProvider.createEconomyAccount(uuid, name);
    }

    public void deleteAccount(UUID uuid) {
        if (economyProvider == null)
            return;
        economyProvider.deleteEconomyAccount(uuid);
    }

    public BigDecimal getBalance(UUID uuid) {
        if (economyProvider == null)
            return new BigDecimal(Double.MAX_VALUE);
        return economyProvider.getBalance(uuid);
    }

    public boolean has(UUID uuid, double amount) {
        return has(uuid, new BigDecimal(amount));
    }

    public boolean has(UUID uuid, BigDecimal amount) {
        if (economyProvider == null)
            return true;
        return economyProvider.has(uuid, amount);
    }

    public boolean deposit(UUID uuid, int amount, String reason) {
        return deposit(uuid, new BigDecimal(amount), reason);
    }

    public boolean deposit(UUID uuid, double amount, String reason) {
        return deposit(uuid, new BigDecimal(amount), reason);
    }

    public boolean deposit(UUID uuid, BigDecimal amount, String reason) {
        if (economyProvider == null)
            return true;
        if (economyProvider.deposit(uuid, amount)) {
            saveBankRecord(uuid, amount, reason);
            return true;
        }
        return false;
    }

    public boolean withdraw(UUID uuid, int amount, String reason) {
        return withdraw(uuid, new BigDecimal(amount), reason);
    }

    public boolean withdraw(UUID uuid, double amount, String reason) {
        return withdraw(uuid, new BigDecimal(amount), reason);
    }

    public boolean withdraw(UUID uuid, BigDecimal amount, String reason) {
        if (economyProvider == null)
            return true;
        if (economyProvider.withdraw(uuid, amount)) {
            saveBankRecord(uuid, amount.multiply(BigDecimal.valueOf(-1)), reason);
            return true;
        }
        return false;
    }

    // Helper methods for tax modes

    public boolean depositAndTax(Citizen citizen, double amount, String reason) {
        if (!citizen.hasSettlement()) {
            return deposit(citizen.getUuid(), amount, reason);
        } else {
            var percent = citizen.getSettlement().getTax();

            if (percent == 0)
                return deposit(citizen.getUuid(), amount, reason);

            var settlementShare = amount * percent;

            // Do two separate transactions so that the bankhistory properly shows taxes
            deposit(citizen.getUuid(), amount, reason);
            withdraw(citizen.getUuid(), settlementShare, "Tax");

            // Run a tick later to show message after other payment messages in this tick
            Bukkit.getScheduler().runTaskLater(UnitedLands.instance(), () -> {
                United.messenger().send(citizen.getPlayer(), "player.money.paid-taxes", UnitedLandsEconomyManager.instance().format(settlementShare));
            }, 1);

            depositAndTax(citizen.getSettlement(), settlementShare, "Tax");
            return true;
        }
    }

    public boolean depositAndTax(Settlement settlement, double amount, String reason) {
        if (!settlement.hasCountry()) {
            return deposit(settlement.getUuid(), amount, reason);
        } else {
            var percent = settlement.getCountry().getTax();

            if (percent == 0)
                return deposit(settlement.getUuid(), amount, reason);

            var countryShare = amount * percent;

            deposit(settlement.getUuid(), amount, reason);
            withdraw(settlement.getUuid(), countryShare, "Tax");

            deposit(settlement.getCountry().getUuid(), countryShare, "Tax");
            return true;
        }
    }

    // Logging

    // Method for writing records from external plugins that would otherwise not
    // show up in UnitedLands bank history
    public void addVirtualBankRecord(UUID objectId, BigDecimal amount, String message) {
        saveBankRecord(objectId, amount, message);
    }

    private void saveBankRecord(UUID objectId, BigDecimal amount, String message) {
        var record = new BankRecord(objectId, amount.doubleValue(), message);
        try {
            UnitedLandsDataManager.instance().createBankRecordDbData(record);
        } catch (Exception ex) {
            United.logger().error("Failed to create bank record: " + ex.getMessage(), "UnitedLands");
        }
    }

    public List<BankRecord> getBankRecords(UUID objectId, int startIndex, int count) {
        try {
            return UnitedLandsDataManager.instance().getBankRecords(objectId, startIndex, count);
        } catch (Exception ex) {
            United.logger().error("Failed to bank records: " + ex.getMessage(), "UnitedLands");
        }
        return new ArrayList<>();
    }

}
