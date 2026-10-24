package gregtech.content.multiblock;

import gregtech.internal.addon.AddonMachines;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Block ids and tags named by addon layouts. Ids are resolved once registries exist. */
public final class PlainStructureBlocks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Block> RESOLVED = new HashMap<>();
    private static final Set<String> EMPTY_TAG_WARNINGS = new HashSet<>();

    private PlainStructureBlocks() {}

    /** Fails startup when a declared block id is not registered. Tags wait until a structure check. */
    public static void resolve() {
        if (!AddonMachines.installed()) return;
        AddonMachines.requireKnownBlocks(name -> BuiltInRegistries.BLOCK.containsKey(location(name)));
        RESOLVED.clear();
        for (AddonMachines.PlainBlock block : AddonMachines.plainBlocks()) {
            if (block.name().startsWith("#")) continue;
            RESOLVED.put(block.name(), BuiltInRegistries.BLOCK.get(location(block.name())));
        }
    }

    public static boolean matches(Level level, BlockPos pos, String plain, BlockState state) {
        if (plain.equals("anyopaque")) return state.isSolidRender(level, pos);
        if (plain.startsWith("anyfluid")) {
            var fluid = state.getFluidState();
            if (fluid.isEmpty()) return false;
            int hash = plain.indexOf('#');
            if (hash < 0) return true;
            return fluid.is(TagKey.create(Registries.FLUID, location(plain.substring(hash + 1))));
        }
        if (plain.startsWith("#")) {
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, location(plain.substring(1)));
            var registry = level.registryAccess().registryOrThrow(Registries.BLOCK);
            var holders = registry.getTag(tag);
            if (holders.isEmpty() || holders.get().size() == 0) {
                if (!level.isClientSide() && EMPTY_TAG_WARNINGS.add(plain))
                    LOGGER.warn("Structure tag {} has no blocks, so that cell cannot form", plain);
                return false;
            }
            return state.is(tag);
        }
        Block expected = RESOLVED.get(plain);
        return expected != null && state.getBlock() == expected;
    }

    /** The block shown in a structure preview. An empty tag uses a barrier until its members arrive. */
    public static Block preview(String plain) {
        if (plain.equals("anyopaque")) return Blocks.STONE;
        if (plain.startsWith("anyfluid")) return Blocks.WATER;
        if (!plain.startsWith("#")) {
            Block resolved = RESOLVED.get(plain);
            return resolved == null ? Blocks.BARRIER : resolved;
        }
        var holders = BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, location(plain.substring(1))));
        if (holders.isEmpty() || holders.get().size() == 0) return Blocks.BARRIER;
        Block chosen = null;
        String chosenId = null;
        for (Holder<Block> holder : holders.get()) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(holder.value());
            if (id == null) continue;
            String text = id.toString();
            if (chosenId == null || text.compareTo(chosenId) < 0) {
                chosenId = text;
                chosen = holder.value();
            }
        }
        return chosen == null ? Blocks.BARRIER : chosen;
    }

    private static ResourceLocation location(String value) {
        int colon = value.indexOf(':');
        return ResourceLocation.fromNamespaceAndPath(value.substring(0, colon), value.substring(colon + 1));
    }
}
