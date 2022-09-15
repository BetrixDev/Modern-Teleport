package dev.betrix.modernteleport;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Nullable;

public record TeleportResult(boolean result, @Nullable String messageKey, @Nullable TagResolver... tagResolvers) {
}
