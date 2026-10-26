package gregtech.content.registry;

import gregtech.content.material.Materials;
import gregtech.engine.machine.PumpSpec;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.energy.PumpBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.block.Block;
*///?} else {
import net.minecraftforge.registries.RegistryObject;
//?}

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 rotational pumps — RU-powered fluid drainage, 4 tiers. */
public final class GTPumps {
//? if >=1.21.1 {
    /*private static final List<DeferredHolder<Block,PumpBlock>> ALL = new ArrayList<>();
*///?} else {
    private static final List<RegistryObject<PumpBlock>> ALL = new ArrayList<>();
//?}

    private GTPumps() {}

//? if >=1.21.1 {
    /*public static List<DeferredHolder<Block,PumpBlock>> all() { return Collections.unmodifiableList(ALL); }
*///?} else {
    public static List<RegistryObject<PumpBlock>> all() { return Collections.unmodifiableList(ALL); }
//?}

    public static void registerAll() {
        for(var spec:gregtech.content.energy.PumpCatalog.all()){
            String id=spec.id();
//? if >=1.21.1 {
            /*DeferredHolder<Block,PumpBlock> block = GTBlocks.BLOCKS.register(id,
*///?} else {
            RegistryObject<PumpBlock> block = GTBlocks.BLOCKS.register(id,
//?}
                    () -> new PumpBlock(spec, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(4.0f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));
            ALL.add(block);
            GTBlocks.BLOCK_ITEMS.register(id,
                    () -> new BlockItem(block.get(), new Item.Properties()));
        }
    }

    public static void bootstrap() {
        if (ALL.isEmpty()) throw new IllegalStateException("GTPumps failed to initialize");
    }
}
