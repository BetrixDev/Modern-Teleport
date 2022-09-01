package dev.betrix.modernteleport;

import dev.betrix.modernteleport.commands.TeleportAcceptCommand;
import dev.betrix.modernteleport.commands.TeleportRejectCommand;
import dev.betrix.modernteleport.commands.TeleportRequestCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ModernTeleport extends JavaPlugin {
    
    public boolean canUseCommand(Player player) {
        boolean usePermission = getConfig().getBoolean("use_permissions");

        if (!usePermission) {
            return true;
        }

        return player.hasPermission("modernteleport.teleport");
    }

    public String getPrefix() {
        return getConfig().getString("prefix");
    }

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        TeleportHandler teleportHandler = new TeleportHandler(this);

        Objects.requireNonNull(getCommand("tprequest")).setExecutor(new TeleportRequestCommand(this, teleportHandler));
        Objects.requireNonNull(getCommand("tpaccept")).setExecutor(new TeleportAcceptCommand(this, teleportHandler));
        Objects.requireNonNull(getCommand("tpreject")).setExecutor(new TeleportRejectCommand(this, teleportHandler));

        getLogger().info("Modern Teleport initialized");
    }
}
