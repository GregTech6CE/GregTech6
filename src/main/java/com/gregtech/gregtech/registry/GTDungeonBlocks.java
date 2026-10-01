package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.misc.DungeonPortalBlock;
import com.gregtech.gregtech.content.plant.BedrockFlowers;
import com.gregtech.gregtech.GregTech;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The two dungeon portal blocks the ported portal rooms build
 * ({@code gregapi/worldgen/dungeon/DungeonChunkRoomPortalNether.java} and
 * {@code DungeonChunkRoomPortalEnd.java}).
 *
 * <p>GT6 has no blocks for these: its rooms leave the Nether room's obsidian frame to be lit by the
 * player and place the vanilla end portal in the End room, while its own miniature portals
 * (multi-tiles 32766 and 32000) are built by the player. The port's blocks are the substitution
 * described in {@link DungeonPortalBlock}; their hardness is GT6's (obsidian for the Nether portal,
 * end stone for the End one) and their stack size is GT6's multi-tile stack size of 16
 * ({@code Loader_MultiTileEntities:2002-2003}).</p>
 */
public final class GTDungeonBlocks {

    /** GT6's {@code MultiTileEntityMiniPortalNether} (32766) as a dungeon room's portal. */
    public static RegistryObject<DungeonPortalBlock> PORTAL_NETHER;
    /** GT6's {@code MultiTileEntityMiniPortalEnd} (32000) as a dungeon room's portal. */
    public static RegistryObject<DungeonPortalBlock> PORTAL_END;

    private static final List<RegistryObject<? extends Block>> ALL = new ArrayList<>();
    private static final Map<String, RegistryObject<FlowerPotBlock>> POTTED_FLOWERS = new LinkedHashMap<>();

    private GTDungeonBlocks() {}

    public static List<RegistryObject<? extends Block>> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** One GT6 indicator flower in a vanilla-shaped pot; the pot itself is not a separate inventory item. */
    public static Block pottedFlower(String flowerId) {
        RegistryObject<FlowerPotBlock> pot = POTTED_FLOWERS.get(flowerId);
        if (pot == null || !pot.isPresent())
            throw new IllegalStateException("Missing dungeon potted flower " + flowerId);
        return pot.get();
    }

    /** Forge keeps the pot-content map separate from block registration. */
    public static void registerPotPlants() {
        FlowerPotBlock empty = (FlowerPotBlock) Blocks.FLOWER_POT;
        for (String id : POTTED_FLOWERS.keySet()) {
            Block flower = ForgeRegistries.BLOCKS.getValue(GregTech.id(id));
            if (flower == null) throw new IllegalStateException("Missing GT6 indicator flower " + id);
            empty.addPlant(GregTech.id(id), POTTED_FLOWERS.get(id));
        }
    }

    /** GT6's portal properties: obsidian/end stone hardness, no collision, light while it is open. */
    private static BlockBehaviour.Properties properties(float hardness, float resistance) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(hardness, resistance)
                .sound(SoundType.GLASS)
                .noCollission()
                .noOcclusion()
                .lightLevel(state -> state.getValue(DungeonPortalBlock.ACTIVE) ? 11 : 0);
    }

    private static RegistryObject<DungeonPortalBlock> reg(String id, Supplier<DungeonPortalBlock> block) {
        RegistryObject<DungeonPortalBlock> registered = GTBlocks.BLOCKS.register(id, block);
        ALL.add(registered);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(registered.get(), new Item.Properties().stacksTo(16)));
        return registered;
    }

    public static void registerAll() {
        if (!ALL.isEmpty()) return;
        PORTAL_NETHER = reg("dungeon_portal_nether",
                () -> new DungeonPortalBlock(DungeonPortalBlock.Target.NETHER, properties(50.0F, 1200.0F)));
        PORTAL_END = reg("dungeon_portal_end",
                () -> new DungeonPortalBlock(DungeonPortalBlock.Target.END, properties(3.0F, 9.0F)));
        for (BedrockFlowers.Flower flower : BedrockFlowers.ALL) {
            String id = flower.id();
            RegistryObject<FlowerPotBlock> potted = GTBlocks.BLOCKS.register("potted_" + id,
                    () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT,
                            () -> ForgeRegistries.BLOCKS.getValue(GregTech.id(id)),
                            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY)));
            POTTED_FLOWERS.put(id, potted);
            ALL.add(potted);
        }
    }
}
