package gregtech.content.worldgen;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Resolves explicit vanilla outputs in the stratum table using the same config as vanilla world generation. */
public final class StoneLayerOrePlacement {
    private StoneLayerOrePlacement() {}

    static Set<String> disabledOutputs() {
        var layers = StoneLayerField.layers().stream().flatMap(layer -> layer.ores().stream());
        var contacts = GTStoneLayersGen.CONTACTS.stream().flatMap(contact -> contact.ores().stream());
        var disabled = new LinkedHashSet<String>();
        Stream.concat(layers, contacts)
                .map(GTStoneLayersGen.OreDef::blockId)
                .filter(id -> !id.isEmpty())
                .distinct()
                .filter(id -> enabledState(ResourceLocation.parse(id)) == null)
                .forEach(disabled::add);
        return disabled;
    }

    @Nullable
    static BlockState enabledState(ResourceLocation id) {
        if (VanillaOreGeneration.disabled(id)) return null;
        var block = BuiltInRegistries.BLOCK.get(id);
        return block == Blocks.AIR ? null : block.defaultBlockState();
    }
}
