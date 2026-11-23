package org.unitedlands.unitedlands.classes;

public enum BankRecordTransactionType {
    SYSTEM,             // Internal or unspecified records 
    ADMIN,              // Admin command actions
    CLAIM,              // Records related to plot or region claiming
    DIPLOMACY,          // Records caused by diplomacy commands
    WAR,                // Records related to war commands
    TRANSFER,           // Direct money transfers via /pay
    CREATION,           // Records related to settlement or country creation,
    DEPOSIT,            // Deposits to settlement / country bank accounts
    TAX,                // Automatic tax records
    TRADE_NPC,          // Records related to NPC trading
    TRADE_PLAYER,       // Records related to chestshop or trade command trading
    UPKEEP,             // Records related to upkeep,
    WITHDRAWAL,         // Withdrawals from settlement / country bank accounts
    PURCHASE            // Purchasing plots
}
