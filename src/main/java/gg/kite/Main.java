package gg.kite;

import com.nexomc.nexo.api.NexoBlocks;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class Main extends JavaPlugin {

    private HikariDataSource dataSource;
    private StorageManager storageManager;
    private Map<String, StorageConfig> storageConfigs;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!initializeDatabase()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        loadStorageConfigs();
        storageManager = new StorageManager(this, dataSource);
        getServer().getPluginManager().registerEvents(new BlockListener(this, storageManager, storageConfigs), this);
        getCommand("zunkernexo").setExecutor(new CommandHandler(this));
        getCommand("zunkernexo").setTabCompleter(new CommandHandler(this));
        getLogger().info("ZunkerNexo enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (dataSource != null) {
            dataSource.close();
        }
        getLogger().info("ZunkerNexo disabled successfully!");
    }

    private boolean initializeDatabase() {
        ConfigurationSection mysqlConfig = getConfig().getConfigurationSection("mysql");
        if (mysqlConfig == null) {
            getLogger().severe("MySQL configuration not found in config.yml!");
            return false;
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s",
                mysqlConfig.getString("host"),
                mysqlConfig.getInt("port"),
                mysqlConfig.getString("database")));
        config.setUsername(mysqlConfig.getString("username"));
        config.setPassword(mysqlConfig.getString("password"));
        config.setMaximumPoolSize(mysqlConfig.getInt("pool-size", 10));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        int retries = 3;
        while (retries > 0) {
            try {
                dataSource = new HikariDataSource(config);
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute(
                            "CREATE TABLE IF NOT EXISTS nexo_storage (" +
                                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                                    "world VARCHAR(255) NOT NULL," +
                                    "x INT NOT NULL," +
                                    "y INT NOT NULL," +
                                    "z INT NOT NULL," +
                                    "block_type VARCHAR(255) NOT NULL," +
                                    "inventory LONGTEXT NOT NULL," +
                                    "UNIQUE KEY uk_location (world, x, y, z))"
                    );
                }
                return true;
            } catch (SQLException e) {
                retries--;
                if (retries == 0) {
                    getLogger().log(Level.SEVERE, "Failed to initialize database after 3 retries!", e);
                    return false;
                }
                getLogger().warning("MySQL connection attempt failed. Retrying in 1 second... (" + retries + " attempts left)");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    getLogger().log(Level.SEVERE, "Interrupted during MySQL retry delay!", ie);
                    return false;
                }
            }
        }
        return false;
    }

    private void loadStorageConfigs() {
        storageConfigs = new HashMap<>();
        ConfigurationSection blocksSection = getConfig().getConfigurationSection("storage-blocks");
        if (blocksSection == null) {
            getLogger().warning("No storage blocks configured in config.yml!");
            return;
        }

        String[] validNexoIds = NexoBlocks.blockIDs();
        for (String key : blocksSection.getKeys(false)) {
            ConfigurationSection section = blocksSection.getConfigurationSection(key);
            if (section == null) continue;

            String nexoId = section.getString("nexo-id");
            String menuTitle = section.getString("menu-title");
            int rows = section.getInt("rows");

            if (nexoId == null || nexoId.isEmpty()) {
                getLogger().warning("Invalid nexo-id for block " + key + ". Skipping.");
                continue;
            }
            if (!contains(validNexoIds, nexoId)) {
                getLogger().warning("Nexo-id " + nexoId + " for block " + key + " is not a valid Nexo block ID. Skipping.");
                continue;
            }
            if (menuTitle == null || menuTitle.isEmpty()) {
                getLogger().warning("Empty menu-title for block " + key + ". Using default: Storage.");
                menuTitle = "Storage";
            }
            if (rows < 1 || rows > 6) {
                getLogger().warning("Invalid rows (" + rows + ") for block " + key + ". Must be 1-6. Using default: 3.");
                rows = 3;
            }

            storageConfigs.put(nexoId, new StorageConfig(nexoId, menuTitle, rows));
        }
    }

    private boolean contains(String[] array, String value) {
        for (String item : array) {
            if (item.equals(value)) return true;
        }
        return false;
    }

    public void reloadPlugin() {
        reloadConfig();
        loadStorageConfigs();
        getLogger().info("ZunkerNexo configuration reloaded!");
    }

    public StorageConfig getStorageConfig(String nexoId) {
        return storageConfigs.get(nexoId);
    }
}