package dev.betrix.modernteleport.commands;

import dev.betrix.modernteleport.ModernTeleport;
import dev.betrix.modernteleport.TeleportHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeleportRejectCommand implements CommandExecutor {

    private final ModernTeleport modernTeleport;
    private final TeleportHandler teleportHandler;
    private final String prefix;
    private final Configuration config;

    public TeleportRejectCommand(ModernTeleport modernTeleport, TeleportHandler teleportHandler) {
        this.modernTeleport = modernTeleport;
        this.teleportHandler = teleportHandler;
        this.prefix = modernTeleport.getPrefix();
        this.config = modernTeleport.getConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player player) {
            if (!teleportHandler.canUseCommand(player)) {
                modernTeleport.messagePlayer(player, "message.no_permission");
                return true;
            }

            if (teleportHandler.hasPendingRequest(player)) {
                teleportHandler.removePendingRequest(player);
            } else {
                modernTeleport.messagePlayer(player, "messages.no_pending_request");
            }
        }

        return true;
    }
}
