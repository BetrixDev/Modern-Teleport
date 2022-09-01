package dev.betrix.modernteleport.commands;

import dev.betrix.modernteleport.ModernTeleport;
import dev.betrix.modernteleport.TeleportHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TeleportRequestCommand implements CommandExecutor {

    private final ModernTeleport modernTeleport;
    private final TeleportHandler teleportHandler;
    private final String prefix;
    private final Configuration config;

    public TeleportRequestCommand(ModernTeleport modernTeleport, TeleportHandler teleportHandler) {
        this.modernTeleport = modernTeleport;
        this.teleportHandler = teleportHandler;

        this.prefix = modernTeleport.getPrefix();
        this.config = modernTeleport.getConfig();
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

            // Check if no args are present and reject command
            if (args[0] == null) {
                return false;
            }

            Player target = modernTeleport.getServer().getPlayer(args[0]);

            if (!modernTeleport.canUseCommand(target)) {
                String noPermission = config.getString("messages.target_no_permission");
                player.sendMessage(MiniMessage.miniMessage()
                        .deserialize(noPermission.replace("%prefix%", prefix)
                                .replace("%target_name%", target.getName())));
                return true;
            }

            if (target == null) {
                // User input an invalid player name
                String notExist = config.getString("messages.player_not_exist");
                player.sendMessage(MiniMessage.miniMessage().deserialize(notExist.replace("%prefix%", prefix)));
                return true;
            }

            if (Objects.equals(target.getUniqueId().toString(), player.getUniqueId().toString())) {
                // User sent a request to themselves
                String requestYourself = config.getString("messages.request_yourself");
                player.sendMessage(MiniMessage.miniMessage()
                        .deserialize(requestYourself.replace("%prefix%", prefix)));
                return true;
            }

            if (teleportHandler.hasPendingRequest(target)) {
                // Target already has a pending request
                String hasPending = config.getString("messages.has_pending_request");

                player.sendMessage(MiniMessage.miniMessage().deserialize(hasPending
                        .replace("%prefix%", prefix)
                        .replace("%target_name%", target.getName()))
                );
                return true;
            }

            long coolDown = teleportHandler.getCoolDownTimeLeft(player);
            long targetCoolDown = teleportHandler.getCoolDownTimeLeft(target);

            if (coolDown > 0) {
                // User is still on cool down
                String coolDownMessage = config.getString("messages.user_cool_down");

                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        coolDownMessage.replace("%prefix%", prefix)
                                .replace("%cool_down%", coolDown / 1000 + "")));

                return true;
            } else if (targetCoolDown > 0) {
                // Target is still on cool down
                String coolDownMessage = config.getString("messages.target_cool_down");

                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        coolDownMessage.replace("%prefix%", prefix)
                                .replace("%cool_down%", coolDown / 1000 + "")
                                .replace("%target_name%", target.getName())));

                return true;
            }

            // After all checks pass, send the request to the target player
            teleportHandler.sendRequest(player, target);
        }

        return true;
    }
}
