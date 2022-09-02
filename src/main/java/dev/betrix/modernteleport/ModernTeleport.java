package dev.betrix.modernteleport;

import dev.betrix.modernteleport.commands.ModernTeleportCommand;
import dev.betrix.modernteleport.commands.TeleportAcceptCommand;
import dev.betrix.modernteleport.commands.TeleportRejectCommand;
import dev.betrix.modernteleport.commands.TeleportRequestCommand;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
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

    public void playSound(Player player, Key key) {
        Sound sound = Sound.sound(key, Sound.Source.BLOCK, 1f, 1f);

        player.playSound(sound);
    }

    public String getPrefix() {
        return getConfig().getString("prefix");
    }

    public void initCommands() {
        TeleportHandler teleportHandler = new TeleportHandler(this);


        Objects.requireNonNull(getCommand("tprequest")).setExecutor(new TeleportRequestCommand(this, teleportHandler));
        Objects.requireNonNull(getCommand("tpaccept")).setExecutor(new TeleportAcceptCommand(this, teleportHandler));
        Objects.requireNonNull(getCommand("tpreject")).setExecutor(new TeleportRejectCommand(this, teleportHandler));

        ModernTeleportCommand modernTeleportCommand = new ModernTeleportCommand(this);
        Objects.requireNonNull(getCommand("modernteleport")).setExecutor(modernTeleportCommand);
        Objects.requireNonNull(getCommand("modernteleport")).setTabCompleter(modernTeleportCommand);

    }

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        initCommands();

        getLogger().info("Modern Teleport initialized");
    }
}
