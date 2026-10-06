package org.unitedlands.unitedlands.managers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.util.InternalException;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.BankRecord;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.configs.GeneralConfig;
import org.unitedlands.unitedlands.integrations.economy.IEconomyProvider;
import org.unitedlands.unitedlands.integrations.economy.VaultEconomyProvider;
import org.unitedlands.utils.United;

public class UnitedLandsEconomyManager {

    private static UnitedLandsEconomyManager instance;

    public static UnitedLandsEconomyManager instance() {
        return instance;
    }

    private final UnitedLands plugin;

    private IEconomyProvider economyProvider;

    public UnitedLandsEconomyManager(UnitedLands plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public boolean hasEconomy() {
        return economyProvider != null;
    }

    public void loadEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                economyProvider = new VaultEconomyProvider(plugin);
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

        if (GeneralConfig.get().economy().taxes().get("settlement").taxMode().equals("INCOME"))
        {
            if (!citizen.hasSettlement()) {
                return deposit(citizen.getUuid(), amount, reason);
            } else {
                var percent = citizen.getSettlement().getTax();

                var settlementShare = amount * percent;
                var citizenShare = amount - settlementShare;

                depositAndTax(citizen.getSettlement(), settlementShare, "Income tax");
                return deposit(citizen.getUuid(), citizenShare, reason);
            }
        } else {
            return deposit(citizen.getUuid(), amount, reason);
        }
    }

    public boolean depositAndTax(Settlement settlement, double amount, String reason) {

        if (GeneralConfig.get().economy().taxes().get("country").taxMode().equals("INCOME"))
        {
            if (!settlement.hasCountry()) {
                return deposit(settlement.getUuid(), amount, reason);
            } else {

                // TODO: Country tax implementation
                var percent = 0.1f; // settlement.getCountry().getTax();

                var countryShare = amount * percent;
                var settlementShare = amount - countryShare;

                deposit(settlement.getCountry().getUuid(), countryShare, "Income tax");
                
                return deposit(settlement.getUuid(), settlementShare, reason);
            }
        } else {
            return deposit(settlement.getUuid(), amount, reason);
        }
    }


    // Logging

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
