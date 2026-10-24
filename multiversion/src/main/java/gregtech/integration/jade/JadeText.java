package gregtech.integration.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Resolve names on the viewing client, never using the dedicated server's language. */
final class JadeText {
    private JadeText() {}

    /** Extra translation-key fields are optional so old server data retains its literal fallback. */
    static Component translated(CompoundTag data, String keyField, String fallbackField) {
        String key = data.getString(keyField);
        String fallback = data.getString(fallbackField);
        return key.isEmpty() ? Component.literal(fallback) : Component.translatableWithFallback(key, fallback);
    }

    static Component direction(String direction) {
        return Component.translatableWithFallback("jade.gt6ce.direction." + direction, direction);
    }
}
