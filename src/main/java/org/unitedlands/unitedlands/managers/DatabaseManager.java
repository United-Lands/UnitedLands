package org.unitedlands.unitedlands.managers;

import java.sql.SQLException;

import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.BankRecord;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.PlaytimeRecord;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.configs.GeneralConfig;
import org.unitedlands.unitedlands.classes.db.BankRecordService;
import org.unitedlands.unitedlands.classes.db.CitizenService;
import org.unitedlands.unitedlands.classes.db.CountryService;
import org.unitedlands.unitedlands.classes.db.LoginChallengeService;
import org.unitedlands.unitedlands.classes.db.PlaytimeRecordService;
import org.unitedlands.unitedlands.classes.db.RegionService;
import org.unitedlands.unitedlands.classes.db.SchemaVersion;
import org.unitedlands.unitedlands.classes.db.SettlementChunkService;
import org.unitedlands.unitedlands.classes.db.SettlementService;
import org.unitedlands.unitedlands.classes.webservices.LoginChallenge;
import org.unitedlands.utils.United;

import org.unitedlands.libs.ormlite.dao.Dao;
import org.unitedlands.libs.ormlite.dao.DaoManager;
import org.unitedlands.libs.ormlite.jdbc.DataSourceConnectionSource;
import org.unitedlands.libs.ormlite.support.ConnectionSource;
import org.unitedlands.libs.ormlite.table.TableUtils;
import org.unitedlands.libs.zaxxer.hikari.HikariConfig;
import org.unitedlands.libs.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {

    private HikariDataSource hikariDataSource;
    private ConnectionSource connectionSource;

    private CountryService countryService;
    private RegionService regionService;
    private SettlementService settlementService;
    private SettlementChunkService settlementChunkService;
    private CitizenService citizenService;
    private LoginChallengeService loginChallengeService;
    private BankRecordService bankRecordService;
    private PlaytimeRecordService playtimeRecordService;

    public void initialize() {

        // instance = this;

        String host = GeneralConfig.get().mysql().host();
        int port = GeneralConfig.get().mysql().port();
        String database = GeneralConfig.get().mysql().database();
        String username = GeneralConfig.get().mysql().username();
        String password = GeneralConfig.get().mysql().password();

        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                host,
                port,
                database,
                GeneralConfig.get().developerMode() ? "false" : "true");

        try {

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);

            // Connection pool settings
            config.setMaximumPoolSize(24);
            config.setMinimumIdle(2);
            config.setIdleTimeout(600000); // 10 minutes
            config.setMaxLifetime(1800000); // 30 minutes
            config.setConnectionTimeout(5000); // 5 seconds
            config.setValidationTimeout(3000); // 3 seconds

            // Validation query
            config.setConnectionTestQuery("SELECT 1");
            config.setLeakDetectionThreshold(15000); // Warn if connection held 15+ seconds
            hikariDataSource = new HikariDataSource(config);
            connectionSource = new DataSourceConnectionSource(hikariDataSource, jdbcUrl);

            United.logger().info("Connected to MySQL database with HikariCP.", "UnitedLands");

            verifySchemaVersion();
            registerServices();

            United.logger().info("DatabaseManager initialized successfully.", "UnitedLands");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void registerServices() throws SQLException {
        this.countryService = new CountryService(getDao(Country.class));
        this.regionService = new RegionService(getDao(Region.class));
        this.settlementService = new SettlementService(getDao(Settlement.class));
        this.settlementChunkService = new SettlementChunkService(getDao(SettlementChunk.class));
        this.citizenService = new CitizenService(getDao(Citizen.class));
        this.loginChallengeService = new LoginChallengeService(getDao(LoginChallenge.class));
        this.bankRecordService = new BankRecordService(getDao(BankRecord.class));
        this.playtimeRecordService = new PlaytimeRecordService(getDao(PlaytimeRecord.class));
    }

    private void verifySchemaVersion() throws SQLException {

        if (GeneralConfig.get().developerMode())
            return;

        Dao<SchemaVersion, Integer> versionDao = getDao(SchemaVersion.class);
        SchemaVersion version = versionDao.queryForId(1);

        if (version == null) {
            version = new SchemaVersion(1);
            versionDao.create(version);
        }

        applyMigrations(versionDao, version);
    }

    private void applyMigrations(Dao<SchemaVersion, Integer> versionDao, SchemaVersion version) throws SQLException {
        // Example for future migrations on production server

        if (GeneralConfig.get().developerMode())
            return;

        if (version.getVersion() < 2) {
            versionDao.executeRaw("ALTER TABLE settlement ADD COLUMN ruined TINYINT(1) NOT NULL;");
            versionDao.executeRaw("ALTER TABLE settlement ADD COLUMN ruin_time BIGINT NULL;");
            versionDao.executeRaw("ALTER TABLE region ADD COLUMN administrator_uuid VARCHAR(36) NULL;");
            versionDao.executeRaw("ALTER TABLE region ADD COLUMN claimed_time BIGINT NULL;");
            versionDao.executeRaw("ALTER TABLE region ADD COLUMN default_name VARCHAR(255) NULL;");
            version.setVersion(2);
            versionDao.update(version);
        }

        if (version.getVersion() < 3) {
            versionDao.executeRaw("ALTER TABLE region ADD COLUMN area DOUBLE NOT NULL;");
            version.setVersion(3);
            versionDao.update(version);
        }

        if (version.getVersion() < 4) {
            versionDao.executeRaw("ALTER TABLE settlement ADD COLUMN attributes_serialized MEDIUMTEXT NULL;");
            versionDao.executeRaw("ALTER TABLE region ADD COLUMN attributes_serialized MEDIUMTEXT NULL;");
            versionDao.executeRaw("ALTER TABLE country ADD COLUMN attributes_serialized MEDIUMTEXT NULL;");
            versionDao.executeRaw("ALTER TABLE settlement ADD COLUMN attribute_modifiers_serialized MEDIUMTEXT NULL;");
            versionDao.executeRaw("ALTER TABLE region ADD COLUMN attribute_modifiers_serialized MEDIUMTEXT NULL;");
            versionDao.executeRaw("ALTER TABLE country ADD COLUMN attribute_modifiers_serialized MEDIUMTEXT NULL;");
            version.setVersion(4);
            versionDao.update(version);
        }

        if (version.getVersion() < 5) {
            TableUtils.createTableIfNotExists(connectionSource, BankRecord.class);
            version.setVersion(5);
            versionDao.update(version);
        }

        if (version.getVersion() < 6) {
            TableUtils.createTableIfNotExists(connectionSource, PlaytimeRecord.class);
            versionDao.executeRaw("ALTER TABLE citizen ADD COLUMN total_playtime BIGINT NOT NULL;");
            version.setVersion(6);
            versionDao.update(version);
        }

        if (version.getVersion() < 7) {
            versionDao.executeRaw("ALTER TABLE settlement ADD COLUMN visitor_spawn_serialized VARCHAR(255) NULL;");
            version.setVersion(7);
            versionDao.update(version);
        }

        if (version.getVersion() < 8) {
            versionDao.executeRaw("ALTER TABLE country DROP COLUMN allies_serialized;");
            version.setVersion(8);
            versionDao.update(version);
        }
    }

    public <T, ID> Dao<T, ID> getDao(Class<T> clazz) throws SQLException {

        // In developer mode, drop the table if it exists
        if (GeneralConfig.get().developerMode())
            TableUtils.dropTable(connectionSource, clazz, true);

        TableUtils.createTableIfNotExists(connectionSource, clazz);
        return DaoManager.createDao(connectionSource, clazz);
    }

    public void close() {
        try {
            if (connectionSource != null) {
                connectionSource.close();
                United.logger().info("Disconnected from MySQL database.", "UnitedLands");
            }
            if (hikariDataSource != null) {
                hikariDataSource.close();
                United.logger().info("HikariCP connection closed.", "UnitedLands");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CountryService getCountryService() {
        return countryService;
    }

    public RegionService getRegionService() {
        return regionService;
    }

    public SettlementService getSettlementService() {
        return settlementService;
    }

    public SettlementChunkService getSettlementChunkService() {
        return settlementChunkService;
    }

    public CitizenService getCitizenService() {
        return citizenService;
    }

    public LoginChallengeService getLoginChallengeService() {
        return loginChallengeService;
    }

    public BankRecordService getBankRecordService() {
        return bankRecordService;
    }

    public PlaytimeRecordService getPlaytimeRecordService() {
        return playtimeRecordService;
    }

    public ConnectionSource getConnectionSource() {
        return connectionSource;
    }

}
