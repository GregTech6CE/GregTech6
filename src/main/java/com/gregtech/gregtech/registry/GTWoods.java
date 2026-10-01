package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.wood.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;
import java.util.function.Supplier;

/** GT6 tree/wood system: logs, planks, leaves, saplings and beams by species. */
public final class GTWoods {
    private static final List<RegistryObject<? extends Block>> ALL = new ArrayList<>();

    // The currently registered species each have five basic wood blocks.
    private static final Map<WoodSpecies, RegistryObject<WoodLogBlock>> LOGS = new LinkedHashMap<>();
    private static final Map<WoodSpecies, RegistryObject<WoodPlanksBlock>> PLANKS = new LinkedHashMap<>();
    private static final Map<WoodSpecies, RegistryObject<WoodLeavesBlock>> LEAVES = new LinkedHashMap<>();
    private static final Map<WoodSpecies, RegistryObject<WoodSaplingBlock>> SAPLINGS = new LinkedHashMap<>();
    private static final Map<WoodSpecies, RegistryObject<WoodBeamBlock>> BEAMS = new LinkedHashMap<>();
    /** GT6 BlocksGT.Planks2.mSlabs[1], meta 0: the Blue Spruce shelf support in dungeon libraries. */
    public static RegistryObject<WoodSlabBlock> SLAB_BLUE_SPRUCE;

    private GTWoods() {}

    public static List<RegistryObject<? extends Block>> all() { return Collections.unmodifiableList(ALL); }

    public static WoodLogBlock log(WoodSpecies s) { return LOGS.get(s).get(); }
    public static WoodPlanksBlock planks(WoodSpecies s) { return PLANKS.get(s).get(); }
    public static WoodBeamBlock beam(WoodSpecies s) { return BEAMS.get(s).get(); }
    public static WoodLeavesBlock leaves(WoodSpecies s) { return LEAVES.get(s).get(); }
    public static WoodSaplingBlock sapling(WoodSpecies s) { return SAPLINGS.get(s).get(); }

    /** Reverse lookup used by worldgen/tests: the species of a registered leaves block, else {@code null}. */
    public static WoodSpecies leavesOf(Block block) {
        for (Map.Entry<WoodSpecies, RegistryObject<WoodLeavesBlock>> e : LEAVES.entrySet()) {
            if (e.getValue().get() == block) return e.getKey();
        }
        return null;
    }

    private static <T extends Block> RegistryObject<T> reg(String id, Supplier<T> blockSupplier) {
        RegistryObject<T> ro = GTBlocks.BLOCKS.register(id, blockSupplier);
        ALL.add(ro);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(ro.get(), new Item.Properties()));
        return ro;
    }

    public static void registerAll() {
        for (WoodSpecies sp : WoodSpecies.values()) {
            BlockBehaviour.Properties base = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD);
            final BlockBehaviour.Properties p = sp.fireproof() ? base.ignitedByLava() : base;

            LOGS.put(sp, reg("log_" + sp.id(), () -> new WoodLogBlock(sp, p)));
            PLANKS.put(sp, reg("planks_" + sp.id(), () -> new WoodPlanksBlock(sp, p)));
            LEAVES.put(sp, reg("leaves_" + sp.id(), () -> new WoodLeavesBlock(sp,
                    BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2f).randomTicks().sound(SoundType.GRASS).noOcclusion())));
            SAPLINGS.put(sp, reg("sapling_" + sp.id(), () -> new WoodSaplingBlock(sp,
                    new com.gregtech.gregtech.worldgen.GTTreeGrower(sp),
                    BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.GRASS))));
            BEAMS.put(sp, reg("beam_" + sp.id(), () -> new WoodBeamBlock(sp, p)));
        }
        SLAB_BLUE_SPRUCE = reg("slab_bluespruce", () -> new WoodSlabBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD).strength(2.0f, 3.0f).sound(SoundType.WOOD)));
    }
}
