package dev.betrix.modernteleport;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

record TeleportRequest(Player sender, Player target, long time) {
}

public class TeleportHandler {

    private final ModernTeleport modernTeleport;
    private final HashMap<String, Long> coolDowns = new HashMap<>();
    private final HashMap<String, TeleportRequest> requests = new HashMap<>();

    TeleportHandler(ModernTeleport modernTeleport) {
        this.modernTeleport = modernTeleport;
    }

    public boolean hasPendingRequest(Player target) {
        return requests.containsKey(target.getUniqueId().toString());
    }

    public void removePendingRequest(Player target) {
        TeleportRequest request = requests.get(target.getUniqueId().toString());

        String confirmation = modernTeleport.getConfig().getString("messages.denied_message_confirmation");
        String targetDenied = modernTeleport.getConfig().getString("messages.target_denied_message");
        String prefix = modernTeleport.getConfig().getString("prefix");

        // Send players messages dictating the event
        request.sender().sendMessage(MiniMessage.miniMessage().deserialize(targetDenied
                .replace("%prefix%", prefix)
                .replace("%target_name%", target.getName()
                ))
        );

        target.sendMessage(
                MiniMessage.miniMessage().deserialize(confirmation
                        .replace("%prefix%", prefix)
                        .replace("%sender_name%", request.sender().getName()
                        ))
        );

        // Remove request from data
        requests.remove(target.getUniqueId().toString());
    }

    public void setCoolDown(Player player) {
        if (player.hasPermission("modernteleport.bypasscooldown")) {
            return;
        }

        coolDowns.put(player.getUniqueId().toString(), System.currentTimeMillis());
    }

    public long getCoolDownTimeLeft(Player player) {
        String uid = player.getUniqueId().toString();
        long coolDown = this.modernTeleport.getConfig().getLong("cool_down");

        if (coolDowns.containsKey(uid)) {
            long time = coolDowns.get(uid);

            return (coolDown * 1000) - (System.currentTimeMillis() - time);
        } else {
            return 0;
        }
    }

    public void sendRequest(Player sender, Player target) {
        String rawMessage = modernTeleport.getConfig().getString("messages.request_message");
        String prefix = modernTeleport.getConfig().getString("prefix");
        String injectedMessage = rawMessage.replace("%prefix%", prefix).replace("%sender_name%", sender.getName());

        Component message = MiniMessage.miniMessage().deserialize(injectedMessage);
        target.sendMessage(message);

        // Store the request
        requests.put(target.getUniqueId().toString(), new TeleportRequest(sender, target, System.currentTimeMillis()));

        int timeout = modernTeleport.getConfig().getInt("request_timout");

        if (timeout == 0) {
            // A timeout of 0 disables it, so we return early
            return;
        }

        // Create a task that runs every second to check if the request should be canceled
        new BukkitRunnable() {
            final String targetUid = target.getUniqueId().toString();
            private int count = 0;

            @Override
            public void run() {
                if (!requests.containsKey(targetUid)) {
                    cancel();
                }

                if (count == timeout) {
                    requests.remove(targetUid);

                    String message = modernTeleport.getConfig().getString("messages.target_not_respond");
                    sender.sendMessage(MiniMessage.miniMessage()
                            .deserialize(message.replace("%prefix%", modernTeleport.getPrefix())
                                    .replace("%target_name%", target.getName())));

                    // Cancel this tasks
                    cancel();
                }

                count++;
            }
        }.runTaskTimerAsynchronously(modernTeleport, 0, 20); // Run every second
    }

    public boolean doTeleport(Player target) {
        TeleportRequest request = requests.get(target.getUniqueId().toString());
        // Run command "/tp PLAYER_NAME TARGET_NAME"
        boolean success = modernTeleport.getServer().dispatchCommand(modernTeleport.getServer().getConsoleSender(),
                "tp " + request.sender().getName() + " " + target.getName());

        if (success) {
            requests.remove(target.getUniqueId().toString());
            setCoolDown(target);
            setCoolDown(request.sender());
        }

        return success;
    }
}
