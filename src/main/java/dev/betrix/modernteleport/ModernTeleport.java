package dev.betrix.modernteleport;

import dev.betrix.modernteleport.commands.ModernTeleportCommand;
import dev.betrix.modernteleport.commands.TeleportAcceptCommand;
import dev.betrix.modernteleport.commands.TeleportRejectCommand;
import dev.betrix.modernteleport.commands.TeleportRequestCommand;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class ModernTeleport extends JavaPlugin {

    public void playSound(Player player, Key key) {
        Sound sound = Sound.sound(key, Sound.Source.BLOCK, 1f, 1f);

        player.playSound(sound);
    }

    public void messagePlayer(Player player, String messageKey, @Nullable TagResolver... tagResolvers) {
        String message = getConfig().getString(messageKey);

        ArrayList<TagResolver> resolvers = new ArrayList<>();
        resolvers.add(Placeholder.parsed("prefix", getConfig().getString("prefix")));
        resolvers.addAll(Arrays.asList(tagResolvers));


        player.sendMessage(MiniMessage.miniMessage()
                .deserialize(message, resolvers.toArray(new TagResolver[0])));
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
