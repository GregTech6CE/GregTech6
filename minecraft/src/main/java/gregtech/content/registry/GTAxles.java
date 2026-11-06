package gregtech.content.registry;

import gregtech.content.material.generated.WoodMaterials;
import gregtech.engine.energy.AxleSpec;
import gregtech.content.block.energy.AxleBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
*///?} else {
//?}


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 rotational axles — RU conductors in several material/size tiers. */
public final class GTAxles {

    private static final List<RegisteredBlock<AxleBlock>> ALL = new ArrayList<>();

//? if >=1.21.1 {
/*    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.energy.AxleBlockEntity>> AXLE=ENTITIES.register("axle",()->BlockEntityType.Builder.of(gregtech.content.blockentity.energy.AxleBlockEntity::new,all().stream().map(RegisteredBlock::get).toArray(Block[]::new)).build(null));

    private GTAxles() {}

    public static void register(ContentRegistrar registrar,IEventBus bus){if(!ALL.isEmpty())throw new IllegalStateException("Axles registered twice");registerAll(registrar);bootstrap();ENTITIES.register(bus);}
*///?} else {
    private GTAxles() {}
//?}


    public static List<RegisteredBlock<AxleBlock>> all() {

        return Collections.unmodifiableList(ALL);
    }

    private static void add(ContentRegistrar registrar, AxleSpec spec) {
        RegisteredBlock<AxleBlock> block = registrar.block(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                spec.id(),
                () -> new AxleBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(spec.material() == WoodMaterials.WoodTreated ? MapColor.WOOD : MapColor.METAL)
                        .strength(2.0f, 6.0f)
                        .sound(spec.material() == WoodMaterials.WoodTreated ? SoundType.WOOD : SoundType.METAL)
                        .noOcclusion()
                        // Arms depend on neighbour thickness, so no per-state shape cache.
                        .dynamicShape()),
                axle -> new net.minecraft.world.item.BlockItem(axle, new Item.Properties().stacksTo(spec.size()<=2?64:spec.size()==3?32:16)));
        ALL.add(block);
    }

    public static void registerAll(ContentRegistrar registrar){gregtech.content.energy.AxleCatalog.specifications().forEach(spec -> add(registrar, spec));}

    public static void bootstrap() {
        if (ALL.isEmpty()) throw new IllegalStateException("GTAxles failed to initialize");
    }
}
