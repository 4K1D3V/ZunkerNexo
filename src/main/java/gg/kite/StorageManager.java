package gg.kite;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.Component;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;

// To resolve SQL warnings in IntelliJ, configure a MySQL data source:
// View > Tool Windows > Database > + > Data Source > MySQL
// Enter config.yml credentials (host, port, database, username, password)
// Or suppress via Alt+Enter on SQL lines
public class StorageManager {

    private final Main plugin;
    private final HikariDataSource dataSource;

    public StorageManager(Main plugin, HikariDataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
    }

    public Inventory loadInventory(@NotNull Location location, String blockType, int rows) {
        if (location.getWorld() == null) {
            plugin.getLogger().warning("Cannot load inventory at " + location + ": World is null.");
            return plugin.getServer().createInventory(null, rows * 9, Component.text(blockType));
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT inventory FROM nexo_storage WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String base64 = rs.getString("inventory");
                byte[] bytes = Base64.getDecoder().decode(base64);
                ItemStack[] contents = deserializeItems(bytes);
                Inventory inventory = plugin.getServer().createInventory(null, rows * 9, Component.text(blockType));
                // Handle inventory size mismatch
                if (contents.length > rows * 9) {
                    plugin.getLogger().warning("Inventory at " + location + " has " + contents.length + " slots but config allows " + (rows * 9) + ". Truncating.");
                    ItemStack[] newContents = new ItemStack[rows * 9];
                    System.arraycopy(contents, 0, newContents, 0, rows * 9);
                    // Optionally drop excess items
                    World world = location.getWorld();
                    if (world != null) {
                        for (int i = rows * 9; i < contents.length; i++) {
                            if (contents[i] != null) {
                                world.dropItemNaturally(location, contents[i]);
                            }
                        }
                    }
                    inventory.setContents(newContents);
                } else if (contents.length < rows * 9) {
                    ItemStack[] newContents = new ItemStack[rows * 9];
                    System.arraycopy(contents, 0, newContents, 0, contents.length);
                    inventory.setContents(newContents);
                } else {
                    inventory.setContents(contents);
                }
                return inventory;
            }
        } catch (SQLException | IllegalStateException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load inventory at " + location, e);
        }

        return plugin.getServer().createInventory(null, rows * 9, Component.text(blockType));
    }

    public void saveInventory(@NotNull Location location, String blockType, @NotNull Inventory inventory) {
        if (location.getWorld() == null) {
            plugin.getLogger().warning("Cannot save inventory at " + location + ": World is null.");
            return;
        }

        byte[] bytes = serializeItems(inventory.getContents());
        String base64 = Base64.getEncoder().encodeToString(bytes);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO nexo_storage (world, x, y, z, block_type, inventory) " +
                             "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE inventory = ?")) {
            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());
            stmt.setString(5, blockType);
            stmt.setString(6, base64);
            stmt.setString(7, base64);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save inventory at " + location, e);
        }
    }

    public ItemStack[] getAndDeleteInventory(@NotNull Location location) {
        if (location.getWorld() == null) {
            plugin.getLogger().warning("Cannot get/delete inventory at " + location + ": World is null.");
            return new ItemStack[0];
        }

        ItemStack[] contents = null;
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement selectStmt = conn.prepareStatement(
                    "SELECT inventory FROM nexo_storage WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                selectStmt.setString(1, location.getWorld().getName());
                selectStmt.setInt(2, location.getBlockX());
                selectStmt.setInt(3, location.getBlockY());
                selectStmt.setInt(4, location.getBlockZ());
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    String base64 = rs.getString("inventory");
                    byte[] bytes = Base64.getDecoder().decode(base64);
                    contents = deserializeItems(bytes);
                }
            }
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM nexo_storage WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                deleteStmt.setString(1, location.getWorld().getName());
                deleteStmt.setInt(2, location.getBlockX());
                deleteStmt.setInt(3, location.getBlockY());
                deleteStmt.setInt(4, location.getBlockZ());
                deleteStmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException | IllegalStateException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get and delete inventory at " + location, e);
        }
        return contents != null ? contents : new ItemStack[0];
    }

    private byte @NotNull [] serializeItems(ItemStack @NotNull [] items) {
        try (ByteArrayOutputStream baos = new  ByteArrayOutputStream();
             DataOutputStream dos = new  DataOutputStream(baos)) {
            dos.writeInt(items.length);
            for (ItemStack item : items) {
                byte[] itemBytes = item != null ? item.serializeAsBytes() : new byte[0];
                dos.writeInt(itemBytes.length);
                dos.write(itemBytes);
            }
            return baos.toByteArray();
        } catch ( IOException e) {
            throw new IllegalStateException("Failed to serialize items", e);
        }
    }

    private ItemStack @NotNull [] deserializeItems(byte[] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             DataInputStream dis = new  DataInputStream(bais)) {
            int length = dis.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                int itemLength = dis.readInt();
                if (itemLength > 0) {
                    byte[] itemBytes = new byte[itemLength];
                    dis.readFully(itemBytes);
                    items[i] = ItemStack.deserializeBytes(itemBytes);
                }
            }
            return items;
        } catch ( IOException e) {
            throw new IllegalStateException("Failed to deserialize items", e);
        }
    }
}