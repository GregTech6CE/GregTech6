package gregtech.network;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;

/** Byte encoding shared by the Forge channel and the NeoForge payload. */
final class StructureLayoutSync {
    private StructureLayoutSync() {}

    static void write(FriendlyByteBuf buf, Map<String, String> layouts) {
        buf.writeVarInt(layouts.size());
        for (var entry : layouts.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    static Map<String, String> read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > 4096) throw new IllegalArgumentException("structure layouts");
        var layouts = new LinkedHashMap<String, String>();
        for (int i = 0; i < size; i++) layouts.put(buf.readUtf(), buf.readUtf());
        return Map.copyOf(layouts);
    }
}
