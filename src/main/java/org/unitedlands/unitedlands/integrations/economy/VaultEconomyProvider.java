package org.unitedlands.unitedlands.integrations.economy;

import java.math.BigDecimal;
import java.util.UUID;

import org.apache.logging.log4j.util.InternalException;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.utils.United;

import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse.ResponseType;

public class VaultEconomyProvider implements IEconomyProvider {

    private Economy economy;

    protected final UnitedLands plugin;

    public VaultEconomyProvider(UnitedLands plugin) {
        this.plugin = plugin;
        loadEconomy();
    }

    private void loadEconomy() {
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            throw new InternalException("Error loading Vault Economy class.");
        }
        economy = rsp.getProvider();
    }

    

    @Override
    public boolean hasEconomyAccount(UUID uuid) {
        return economy.hasAccount(uuid);
    }

    @Override
    public String format(BigDecimal amount) {
        return economy.format("UnitedLands", amount);
    }

    @Override
    public void createEconomyAccount(UUID uuid, String name) {
        economy.createAccount(uuid, name, null, false);
    }

    @Override
    public void deleteEconomyAccount(UUID uuid) {
        economy.deleteAccount("UnitedLands", uuid);
    }

    @Override
    public BigDecimal getBalance(UUID uuid) {
        return economy.balance("UnitedLands", uuid);
    }

    @Override
    public boolean has(UUID uuid, BigDecimal amount) {
        return economy.has("UnitedLands", uuid, amount);
    }

    @Override
    public boolean deposit(UUID uuid, BigDecimal amount) {
        var response = economy.deposit("UnitedLands", uuid, amount);
        if (response.type != ResponseType.SUCCESS) {
            United.logger().warning("Error depositing to " + uuid.toString() + ": " + response.errorMessage, "UnitedLands");
        }
        return true;
    }

    @Override
    public boolean withdraw(UUID uuid, BigDecimal amount) {
        var response = economy.withdraw("UnitedLands", uuid, amount);
        if (response.type != ResponseType.SUCCESS) {
            United.logger().warning("Error withdrawing from " + uuid.toString() + ": " + response.errorMessage,
                    "UnitedLands");
        }
        return true;
    }

}
