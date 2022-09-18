package dev.betrix.modernteleport;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TeleportHandler {

    private final ModernTeleport modernTeleport;
    private final Configuration config;
    private final HashMap<String, Long> coolDowns = new HashMap<>();
    private final ArrayList<TeleportRequest> requests = new ArrayList<>();
    private final ArrayList<TeleportInProgress> teleportsInProgress = new ArrayList<>();

    TeleportHandler(ModernTeleport modernTeleport) {
        this.modernTeleport = modernTeleport;
        this.config = modernTeleport.getConfig();
    }

    private TeleportRequest getRequest(Player p) {
        var filtered = requests.stream().filter(r -> r.target == p || r.sender == p).toList();

        if (filtered.size() == 0) {
            return null;
        } else {
            return filtered.get(0);
        }
    }

    private void removeRequest(Player p) {
        TeleportRequest request = getRequest(p);
        requests.remove(request);
    }

    private TeleportInProgress getTeleportInProgress(Player p) {
        var filtered = teleportsInProgress.stream().filter(r -> r.target == p || r.sender == p).toList();

        if (filtered.size() == 0) {
            return null;
        } else {
            return filtered.get(0);
        }
    }

    private void removeTeleportInProgress(Player p) {
        TeleportInProgress tp = getTeleportInProgress(p);
        requests.remove(tp);
    }

    public void handleRequest(Player sender, Player target) {
        TeleportResult result = canTeleport(sender, target);

        if (result.result()) {
            sendRequest(sender, target);
            modernTeleport.playSound(sender, Key.key("entity.experience_orb.pickup"));
        } else {
            modernTeleport.playSound(sender, Key.key("entity.villager.no"));
        }

        ArrayList<TagResolver> resolvers = new ArrayList<>();
        resolvers.add(Placeholder.unparsed("sender_name", sender.getName()));
        resolvers.add(Placeholder.unparsed("target_name", target.getName()));
        if (result.tagResolvers() != null) {
            resolvers.addAll(Arrays.asList(result.tagResolvers()));
        }

        modernTeleport.messagePlayer(sender, result.messageKey(), resolvers);
    }

    public void handleAccept(Player player) {
        if (hasPendingRequest(player)) {
            TeleportRequest request = getRequest(player);

            if (request.target() == player) {
                doTeleport(player);
                return;
            }
        }

        modernTeleport.messagePlayer(player, "messages.no_pending_request");
    }

    public void handleReject(Player player) {
        TeleportInProgress tpInProgress = getTeleportInProgress(player);
        if (hasPendingRequest(player)) {
            // Find and remove request object
            TeleportRequest request = getRequest(player);
            requests.remove(request);

            // Send players messages dictating the event
            modernTeleport.messagePlayer(request.sender(), "messages.target_denied_message",
                    Placeholder.unparsed("target_name", request.target.getName()));

            modernTeleport.messagePlayer(request.target, "messages.denied_message_confirmation",
                    Placeholder.unparsed("sender_name", request.sender().getName()));
        } else if (tpInProgress != null) {
            cancelTeleport(player);
        } else {
            modernTeleport.messagePlayer(player, "messages.no_pending_request");
        }
    }

    public boolean hasPendingRequest(Player target) {
        TeleportRequest request = getRequest(target);

        if (request == null) {
            return false;
        } else {
            return request.target == target;
        }
    }

    public void cancelTeleport(Player player) {
        TeleportInProgress tp = getTeleportInProgress(player);
        teleportsInProgress.remove(tp);

        modernTeleport.messagePlayer(tp.sender(), "messages.teleportation_cancelled");
        modernTeleport.messagePlayer(tp.target(), "messages.teleportation_cancelled");
    }

    public boolean canUseCommand(Player player) {
        boolean usePermission = config.getBoolean("use_permissions");

        if (!usePermission) {
            return true;
        }

        return player.hasPermission("modernteleport.teleport");
    }

    public long getCoolDownTimeLeft(Player player) {
        String uid = player.getUniqueId().toString();
        long coolDown = this.config.getLong("cool_down");

        if (coolDowns.containsKey(uid)) {
            long time = coolDowns.get(uid);

            return (coolDown * 1000) - (System.currentTimeMillis() - time);
        } else {
            return 0;
        }
    }

    public void sendRequest(Player sender, Player target) {
        // Send the target a request message
        modernTeleport.messagePlayer(target, "messages.request_message",
                Placeholder.unparsed("sender_name", sender.getName()));

        // Store the request
        requests.add(new TeleportRequest(sender, target, System.currentTimeMillis()));

        int timeout = config.getInt("request_timout");

        if (timeout == 0) {
            // A timeout of 0 disables it, so we return early
            return;
        }

        // Create a task to run after the timeout has been reached to delete the request if ignored
        new BukkitRunnable() {
            final String targetUid = target.getUniqueId().toString();

            @Override
            public void run() {
                if (getRequest(target) == null) {
                    cancel();
                    return;
                }

                removeRequest(target);

                modernTeleport.messagePlayer(sender, "messages.target_not_respond",
                        Placeholder.unparsed("target_name", target.getName()));
                modernTeleport.playSound(sender, Key.key("entity.villager.no"));

                // Cancel this tasks
                cancel();
            }
        }.runTaskLaterAsynchronously(modernTeleport, 20L * timeout);
    }

    public TeleportResult canTeleport(Player sender, Player target) {
        if (!canUseCommand(sender)) {
            return new TeleportResult(false, "messages.no_permission");
        }

        if (target == null) {
            return new TeleportResult(false, "messages.invalid_usage");
        }

        if (!canUseCommand(target)) {
            return new TeleportResult(false, "messages.target_no_permission");
        }

        if (sender.getUniqueId().toString().equals(target.getUniqueId().toString())) {
            return new TeleportResult(false, "messages.request_yourself");
        }

        boolean bypassCrossDimension = sender.hasPermission("modernteleport.crossdimension") &&
                target.hasPermission("modernteleport.crossdimension");
        boolean sameWorlds = !sender.getWorld().getUID().equals(target.getWorld().getUID());

        if (!config.getBoolean("cross_world_teleporting")) {
            if (sameWorlds && !bypassCrossDimension) {
                return new TeleportResult(false, "messages.different_worlds");
            }
        } else if (!bypassCrossDimension) {
            List<String> blacklistedWorlds = config.getStringList("blacklisted_worlds");

            if (blacklistedWorlds.contains(sender.getWorld().getName())) {
                return new TeleportResult(false, "messages.blacklisted_world");
            } else if (blacklistedWorlds.contains(target.getWorld().getName())) {
                return new TeleportResult(false, "messages.target_blacklisted_world");
            }
        }

        // Don't distance check when players are in different worlds
        if (!sameWorlds) {
            boolean bypassDistance = sender.hasPermission("modernteleport.bypassdistance") &&
                    target.hasPermission("modernteleport.bypassdistance");

            double distance = sender.getLocation().distance(target.getLocation());
            double maxDistance = config.getDouble("max_distance");

            if (maxDistance > 0 && !bypassDistance) {
                if (distance > maxDistance) {
                    return new TeleportResult(false, "messages.too_far");
                }
            }
        }

        if (hasPendingRequest(target)) {
            return new TeleportResult(false, "messages.has_pending_request");
        }

        long coolDown = getCoolDownTimeLeft(sender);
        long targetCoolDown = getCoolDownTimeLeft(target);

        if (coolDown > 0) {
            return new TeleportResult(false, "messages.user_cool_down",
                    Placeholder.unparsed("cool_down", String.valueOf(coolDown / 1000)));
        } else if (targetCoolDown > 0) {
            return new TeleportResult(false, "messages.target_cool_down",
                    Placeholder.unparsed("cool_down", String.valueOf(coolDown / 1000)));
        }

        return new TeleportResult(true, "messages.request_sent");
    }

    public void doTeleport(Player target) {
        TeleportRequest request = getRequest(target);
        Player sender = request.sender();

        removeTeleportInProgress(sender);
        removeRequest(target);

        long teleportTime = modernTeleport.getConfig().getLong("teleport_time");

        if (teleportTime > 0) {
            Component bossBarTitle = MiniMessage.miniMessage().deserialize(
                    Objects.requireNonNull(modernTeleport.getConfig().getString("messages.boss_bar_title")),
                    Placeholder.unparsed("sender_name", sender.getName()),
                    Placeholder.unparsed("target_name", target.getName()));

            BossBar bossBar = BossBar.bossBar(bossBarTitle, 0, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

            sender.showBossBar(bossBar);
            target.showBossBar(bossBar);

            teleportsInProgress.add(new TeleportInProgress(sender, target));

            modernTeleport.messagePlayer(sender, "messages.sender_tp_confirmation",
                    Placeholder.unparsed("target_name", target.getName()),
                    Placeholder.unparsed("seconds", String.valueOf(teleportTime)));
            modernTeleport.messagePlayer(target, "messages.target_tp_confirmation",
                    Placeholder.unparsed("sender_name", target.getName()),
                    Placeholder.unparsed("seconds", String.valueOf(teleportTime)));

            new BukkitRunnable() {
                private long seconds = 0;

                @Override
                public void run() {
                    boolean isInProgress = getTeleportInProgress(sender) != null;
                    if (seconds == teleportTime || !isInProgress) {
                        sender.hideBossBar(bossBar);
                        target.hideBossBar(bossBar);

                        // Only teleport players if is hasn't been cancelled
                        if (isInProgress) {
                            teleportPlayers(sender, target);
                        }
                        cancel();
                        return;
                    } else {
                        seconds++;
                    }

                    bossBar.progress(bossBar.progress() + 1.0f / teleportTime);
                }
            }.runTaskTimer(modernTeleport, 20L, 20L);
        } else {
            teleportPlayers(sender, target);
        }
    }

    private void teleportPlayers(Player sender, Player target) {
        // Don't execute function if the teleport has been cancelled
        if (getTeleportInProgress(sender) == null) {
            return;
        }

        // Use bukkit's built-in method to teleport
        sender.teleport(target.getLocation());

        coolDowns.put(sender.getUniqueId().toString(), System.currentTimeMillis());
        coolDowns.put(target.getUniqueId().toString(), System.currentTimeMillis());
    }

    private record TeleportRequest(Player sender, Player target, long time) {
    }

    private record TeleportInProgress(Player sender, Player target) {
    }
}
