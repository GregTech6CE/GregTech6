package com.gregtech.gregtech.content.multiblock;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.registry.GTMultiblocks;
import com.gregtech.gregtech.registry.RegisteredBlock;
import net.minecraft.world.level.block.Block;
import java.util.List;
import java.util.Objects;
/** Original numeric IDs identify the single object used by structures and recipes. */
public final class LargeMachineParts {
    private LargeMachineParts() {}
    public static RegisteredBlock<Block> find(int id) { return GTMultiblocks.findPart(id); }
    public static Block block(int id) { return Objects.requireNonNull(find(id), "Unknown original part " + id).get(); }
    public static List<RegisteredBlock<Block>> blocks() { return GTMultiblocks.parts(); }
    public static Integer tint(Block block) {
        for (var part : SharedLargeMachineParts.DEFINITIONS) {
            var holder = find(part.originalId());
            if (holder != null && holder.isPresent() && holder.get() == block)
                return GTMaterialRegistry.get(part.material()).getColor();
        }
        for (var part : GTMultiblocks.addonParts()) {
            var holder = part.block();
            if (holder.isPresent() && holder.get() == block)
                return GTMaterialRegistry.get(part.material()).getColor();
        }
        return null;
    }
}
