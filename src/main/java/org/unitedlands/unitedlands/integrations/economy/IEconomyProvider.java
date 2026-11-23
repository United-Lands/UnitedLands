package org.unitedlands.unitedlands.integrations.economy;

import java.math.BigDecimal;
import java.util.UUID;

public interface IEconomyProvider {

    String format(BigDecimal amount);

    boolean hasEconomyAccount(UUID uuid);

    void createEconomyAccount(UUID uuid, String name);

    void deleteEconomyAccount(UUID uuid);

    BigDecimal getBalance(UUID uuid);

    boolean has(UUID uuid, BigDecimal amount);

    boolean deposit(UUID uuid, BigDecimal amount);

    boolean withdraw(UUID uuid, BigDecimal amount);

}