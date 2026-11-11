package gregtech.content.registry;

import gregtech.content.block.energy.AxleBlock;
import gregtech.content.material.generated.WoodMaterials;
import gregtech.engine.energy.AxleSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** GT6 rotational axles — RU conductors in several material/size tiers. */
public final class GTAxles {

    private static final List<RegisteredBlock<AxleBlock>> ALL = new ArrayList<>();

    public static RegisteredBlockEntity<gregtech.content.blockentity.energy.AxleBlockEntity> AXLE;

    private GTAxles() {}

    public static List<RegisteredBlock<AxleBlock>> all() {

        return Collections.unmodifiableList(ALL);
    }

    private static void add(ContentRegistrar registrar, AxleSpec spec) {
        RegisteredBlock<AxleBlock> block = registrar.block(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                spec.id(),
                () -> new AxleBlock(
                        spec,
                        BlockBehaviour.Properties.of()
                                .mapColor(spec.material() == WoodMaterials.WoodTreated ? MapColor.WOOD : MapColor.METAL)
                                .strength(2.0f, 6.0f)
                                .sound(spec.material() == WoodMaterials.WoodTreated ? SoundType.WOOD : SoundType.METAL)
                                .noOcclusion()
                                // Arms depend on neighbour thickness, so no per-state shape cache.
                                .dynamicShape()),
                axle -> new net.minecraft.world.item.BlockItem(
                        axle, new Item.Properties().stacksTo(spec.size() <= 2 ? 64 : spec.size() == 3 ? 32 : 16)));
        ALL.add(block);
    }

    public static void registerAll(ContentRegistrar registrar) {
        gregtech.content.energy.AxleCatalog.specifications().forEach(spec -> add(registrar, spec));
        AXLE = registrar.blockEntity(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                "axle",
                () -> BlockEntityType.Builder.of(
                                gregtech.content.blockentity.energy.AxleBlockEntity::new,
                                all().stream().map(RegisteredBlock::get).toArray(Block[]::new))
                        .build(null));
    }

    public static void bootstrap() {
        if (ALL.isEmpty()) throw new IllegalStateException("GTAxles failed to initialize");
    }
}
