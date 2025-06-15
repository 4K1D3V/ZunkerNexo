package gg.kite;

import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CommandHandler implements TabExecutor {

    private final Main plugin;

    public CommandHandler(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("zunkernexo.use")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§6ZunkerNexo §7- Version 1.0-DEV");
            sender.sendMessage("§7Usage: /" + label + " reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("zunkernexo.reload")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage("§aZunkerNexo configuration reloaded!");
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Usage: /" + label + " [reload]");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("zunkernexo.reload")) {
            completions.add("reload");
        }
        return completions;
    }
}