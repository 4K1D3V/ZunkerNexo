package gg.kite;

import com.nexomc.nexo.api.events.custom_block.NexoBlockBreakEvent;
import com.nexomc.nexo.api.events.custom_block.NexoBlockInteractEvent;
import com.nexomc.nexo.api.events.custom_block.NexoBlockPlaceEvent;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class BlockListener implements Listener {

    private final Main plugin;
    private final StorageManager storageManager;
    private final Map<String, StorageConfig> storageConfigs;

    public BlockListener(Main plugin, StorageManager storageManager, Map<String, StorageConfig> storageConfigs) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.storageConfigs = storageConfigs;
    }

    @EventHandler
    public void onNexoBlockPlace(@NotNull NexoBlockPlaceEvent event) {
        CustomBlockMechanic mechanic = event.getMechanic();
        String nexoId = mechanic.getItemID();
        StorageConfig config = storageConfigs.get(nexoId);
        if (config == null) return;

        // Initialize empty inventory for the new block
        Location location = event.getBlock().getLocation();
        String menuTitle = ChatColor.translateAlternateColorCodes('&', config.menuTitle());
        Inventory inventory = plugin.getServer().createInventory(null, config.rows() * 9, Component.text(menuTitle));
        storageManager.saveInventory(location, menuTitle, inventory);
        event.getPlayer().sendMessage(Component.text(ChatColor.GREEN + "Storage block placed successfully!"));
    }

    @EventHandler
    public void onNexoBlockInteract(@NotNull NexoBlockInteractEvent event) {
        CustomBlockMechanic mechanic = event.getMechanic();
        String nexoId = mechanic.getItemID();
        StorageConfig config = storageConfigs.get(nexoId);
        if (config == null) return;

        if (!event.getPlayer().hasPermission("zunkernexo.interact")) {
            event.getPlayer().sendMessage(Component.text(ChatColor.RED + "You don't have permission to interact with this storage block!"));
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        Location location = event.getBlock().getLocation();
        String menuTitle = ChatColor.translateAlternateColorCodes('&', config.menuTitle());

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Display storage info
            Inventory inventory = storageManager.loadInventory(location, menuTitle, config.rows());
            int itemCount = 0;
            for (ItemStack item : inventory.getContents()) {
                if (item != null) itemCount++;
            }
            event.getPlayer().sendMessage(Component.text(ChatColor.YELLOW + "Storage Info: " + ChatColor.RESET + menuTitle +
                    ChatColor.YELLOW + " (" + itemCount + "/" + (config.rows() * 9) + " slots used)"));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Open storage menu
            Inventory inventory = storageManager.loadInventory(location, menuTitle, config.rows());
            event.getPlayer().openInventory(inventory);
            event.getPlayer().sendMessage(Component.text(ChatColor.GREEN + "Opened storage: " + menuTitle));
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                storageManager.saveInventory(location, menuTitle, inventory);
            }, 1L);
        }
    }

    @EventHandler
    public void onNexoBlockBreak(@NotNull NexoBlockBreakEvent event) {
        CustomBlockMechanic mechanic = event.getMechanic();
        String nexoId = mechanic.getItemID();
        StorageConfig config = storageConfigs.get(nexoId);
        if (config == null) return;

        Location location = event.getBlock().getLocation();
        ItemStack[] contents = storageManager.getAndDeleteInventory(location);

        // Drop stored items
        World world = location.getWorld();
        if (world != null) {
            for (ItemStack item : contents) {
                if (item != null) {
                    world.dropItemNaturally(location, item);
                }
            }
            event.getPlayer().sendMessage(Component.text(ChatColor.GREEN + "Storage block broken, items dropped."));
        } else {
            event.getPlayer().sendMessage(Component.text(ChatColor.RED + "Error: Unable to drop items (world not loaded)."));
        }

        // Respect Nexo's drop mechanics (already set in event.drop)
    }
}