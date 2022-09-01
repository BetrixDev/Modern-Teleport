package dev.betrix.modernteleport.commands;

import dev.betrix.modernteleport.ModernTeleport;
import dev.betrix.modernteleport.TeleportHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeleportAcceptCommand implements CommandExecutor {

    private final ModernTeleport modernTeleport;
    private final TeleportHandler teleportHandler;
    private final String prefix;

    public TeleportAcceptCommand(ModernTeleport modernTeleport, TeleportHandler teleportHandler) {
        this.modernTeleport = modernTeleport;
        this.teleportHandler = teleportHandler;

        this.prefix = modernTeleport.getPrefix();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (sender instanceof Player player) {
            if (!modernTeleport.canUseCommand(player)) {
                String noPermission = config.getString("messages.no_permission");
                player.sendMessage(MiniMessage.miniMessage()
                        .deserialize(noPermission.replace("%prefix%", prefix)));
                return true;
            }

            if (teleportHandler.hasPendingRequest(player)) {
                teleportHandler.doTeleport(player);
            } else {
                String noPending = config.getString("messages.no_pending_request");

                player.sendMessage(
                        MiniMessage.miniMessage().deserialize(noPending.replace("%prefix%", modernTeleport.getPrefix()))
                );
            }
        }

        return true;
    }
}
