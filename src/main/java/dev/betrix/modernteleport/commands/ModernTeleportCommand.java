package dev.betrix.modernteleport.commands;

import dev.betrix.modernteleport.ModernTeleport;
import net.kyori.adventure.key.Key;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ModernTeleportCommand implements CommandExecutor, TabCompleter {

    private final ModernTeleport modernTeleport;
    private final Configuration config;
    private final String prefix;

    public ModernTeleportCommand(ModernTeleport modernTeleport) {
        this.modernTeleport = modernTeleport;
        this.config = modernTeleport.getConfig();
        this.prefix = modernTeleport.getPrefix();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player player) {
            if (args[0].equals("reload")) {
                if (player.hasPermission("modernteleport.reload")) {
                    modernTeleport.reloadConfig();
                    modernTeleport.initCommands();
                    modernTeleport.messagePlayer(player, "messages.config_reloaded");
                    modernTeleport.playSound(player, Key.key("entity.experience_orb.pickup"));
                }
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        list.add("reload");

        return list;
    }
}
