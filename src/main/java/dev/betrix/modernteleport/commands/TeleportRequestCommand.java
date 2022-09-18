package dev.betrix.modernteleport.commands;

import dev.betrix.modernteleport.ModernTeleport;
import dev.betrix.modernteleport.TeleportHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeleportRequestCommand implements CommandExecutor {

    private final ModernTeleport modernTeleport;
    private final TeleportHandler teleportHandler;

    public TeleportRequestCommand(ModernTeleport modernTeleport, TeleportHandler teleportHandler) {
        this.modernTeleport = modernTeleport;
        this.teleportHandler = teleportHandler;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (sender instanceof Player player) {
            if (!teleportHandler.canUseCommand(player)) {
                modernTeleport.messagePlayer(player, "message.no_permission");
                return true;
            }

            Player target = modernTeleport.getServer().getPlayer(args[0]);
            teleportHandler.handleRequest(player, target);
        }

        return true;
    }
}
