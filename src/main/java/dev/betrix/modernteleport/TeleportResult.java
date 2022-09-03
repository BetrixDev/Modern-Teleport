package dev.betrix.modernteleport;

import org.jetbrains.annotations.Nullable;

public record TeleportResult(boolean result, @Nullable String messageKey) {
}
