package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.api.material.*;
import gregtech.content.block.inventory.SafeBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTSafes {
    private GTSafes() {}

    private static final java.util.List<RegisteredBlock<SafeBlock>> ALL = new java.util.ArrayList<>();

    public static java.util.List<RegisteredBlock<SafeBlock>> all() {
        return java.util.List.copyOf(ALL);
    }

    public static SafeBlock safe(gregtech.api.material.GTMaterial material, boolean keyLocked) {
        for (var b : ALL) if (b.get().material() == material && b.get().keyLocked() == keyLocked) return b.get();
        return null;
    }

    public static void initialize(ContentRegistrar registrar) {
        for (var spec : GTStorageMetals.ALL)
            for (boolean locked : new boolean[] {false, true}) {
                String id = !locked && spec.suffix().equals("steel")
                        ? "safe"
                        : (locked ? "key_safe_" : "safe_") + spec.suffix();
                var b = registrar.block(
                        REGISTRY_NAMESPACE,
                        id,
                        () -> new SafeBlock(
                                spec.material(),
                                locked,
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(spec.hardness() * 2, spec.resistance() * 2)
                                        .requiresCorrectToolForDrops()),
                        bare -> new BlockItem(bare, new Item.Properties().stacksTo(16)));
                ALL.add(b);
            }
    }
}
