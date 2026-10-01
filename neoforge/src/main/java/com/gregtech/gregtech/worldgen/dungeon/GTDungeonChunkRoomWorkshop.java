package com.gregtech.gregtech.worldgen.dungeon;

import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryRegistries;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.content.book.GTBooks;
import com.gregtech.gregtech.block.machine.MoldBlock;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.block.tool.ManualToolBlock;
import com.gregtech.gregtech.blockentity.machine.MoldBlockEntity;
import com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity;
import com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity;
import com.gregtech.gregtech.blockentity.tool.ManualToolBlockEntity;
import com.gregtech.gregtech.blockentity.tool.MaterialAnvilBlockEntity;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTFluids;



import com.gregtech.gregtech.registry.GTToolBlocks;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Workshop layout from GT6 DungeonChunkRoomWorkshop. Bottle inventories and the stainless water
 * station now retain their original contents and attachments. Many other original part inventories,
 * painted states and equipment variants remain incomplete; see REPAIR_TRACKER_2026-09-20.md.
 * The bottle helpers preserve their own original random call sequence, but this class still omits
 * other GT6 inventory rolls, so whole-room seed parity is not claimed.
 */
public class GTDungeonChunkRoomWorkshop extends GTDungeonChunkRoomEmpty {

    /** GT6's room tag ({@code WorldgenDungeonGT:74}); a cell that already carries it is refused. */
    public static final String TAG_WORKSHOP = "gt.dungeon.workshop";

    /** GT6's {@code ChestGenHooks} categories ({@code :53, :55, :60, :98, :124, :132}) as loot tables. */
    private static final String LOOT_MINESHAFT = "chests/abandoned_mineshaft";
    private static final String LOOT_STRONGHOLD = "chests/stronghold_crossing";
    private static final String LOOT_DUNGEON = "chests/simple_dungeon";
    private static final String LOOT_JUNGLE = "chests/jungle_temple";
    private static final String LOOT_BLACKSMITH = "chests/village_weaponsmith";

    /** GT6's anvil multi-tile materials ({@code Loader_MultiTileEntities:2199-2202}, ids 32034-32037). */
    private static final String[] ANVIL_MATERIALS = {"BlackSteel", "BlueSteel", "RedSteel", "VanadiumSteel"};

    /** GT6's mould meta ids for the three tiers the crucible roll picks (:1070-1072). */
    private static final int GT6_MOLD_BRONZE = 1070;

    /**
     * GT6's fluid field names of the containers of this room ({@code FL.Propane}, {@code FL.Oxygen},
     * {@code FL.Helium} at {@code :51}, {@code :200-202}, {@code FL.Water} at {@code :167}); the port's
     * {@code GTFluids.stack} resolves them to the registered fluids.
     */
    private static final String PROPANE = "Propane", OXYGEN = "Oxygen", HELIUM = "Helium", WATER = "Water";

    /**
     * GT6's {@code tDrinks} ({@code DungeonChunkRoomWorkshop:176}): the seven drinks a dungeon barrel
     * can hold, with the purple drink standing in for three of the seven entries - exactly GT6's
     * {@code FL.array(FL.Purple_Drink, FL.Purple_Drink, FL.Purple_Drink, FL.Vodka, FL.Mead,
     * FL.Whiskey_GlenMcKenner, FL.Wine_Grape_Purple)} order, which {@code UT.Code.select(NF, tDrinks)}
     * ({@code :180}) draws from GT6's global RNG, not the dungeon room's RNG.
     */
    private static final String[] DRINKS = {"Purple_Drink", "Purple_Drink", "Purple_Drink",
            "Vodka", "Mead", "Whiskey_GlenMcKenner", "Wine_Grape_Purple"};

    /**
     * GT6's {@code ALL_SIDES_HORIZONTAL} ({@code CS:682}, {@code {2, 3, 4, 5}} = north, south, west,
     * east - the order the bottle crate's facing roll of {@code :149} and {@code :183} indexes).
     */
    private static final Direction[] HORIZONTAL =
            {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    @Override
    public boolean generate(GTDungeonData data) {
        if (data.hasTag(TAG_WORKSHOP) || !super.generate(data)) return false;
        data.tags.add(TAG_WORKSHOP);

        // GT6: a steel barometer gas cylinder with 8000 mB of propane (:51).
        gasCylinder(data, 5, 1, 1, false, PROPANE);

        // GT6: a chest with mineshaft loot (:53) and one with stronghold loot (:55), both painted.
        data.chest(3, 1, 1, LOOT_MINESHAFT, GTMetalChests.chest(com.gregtech.gregtech.content.material.Materials.StainlessSteel,false), Direction.SOUTH);
        data.chest(2, 1, 1, LOOT_STRONGHOLD, GTMetalChests.chest(com.gregtech.gregtech.content.material.Materials.StainlessSteel,false), Direction.SOUTH);

        // GT6: the vanilla crafting table of the workbench row (:57) and its ceramic mortar on top
        // (:58, multi-tile 32735, the port's mortar_block).
        data.set(1, 1, 1, Blocks.CRAFTING_TABLE.defaultBlockState());
        BlockState mortar = mortar();
        if (mortar != null) data.set(1, 2, 1, mortar);

        // GT6: a chest with dungeon loot (:60).
        data.chest(1, 1, 2, LOOT_DUNGEON, GTMetalChests.chest(com.gregtech.gregtech.content.material.Materials.StainlessSteel,false), Direction.EAST);

        // GT6 4011: stainless steel drawer with four pages of material-specific workshop parts.
        WorkshopDrawerSupplies.place(data,1,1,3);

        // GT6: the mechanical safe with jungle loot (:98) and the coin pile on top (:99).
        data.safe(1, 2, 3, LOOT_JUNGLE, 0, false);
        data.coins(1, 3, 3);

        // GT6 5011 with its original steel parts and Invar lighter inventory.
        WorkshopCraftingSupplies.place(data,1,1,4);

        // GT6: the ceramic measuring pot (:113, multi-tile 32738), written without tank NBT, so it
        // stands empty like the original.
        measuringPot(data, 1, 2, 4);

        // GT6 :116-117: bulk samples of both dungeon rocks, in cobblestone form (BlockStones meta 1).
        // The first store holds 10,000..100,000 blocks, the second 1,000..10,000.
        stoneStorage(data, 4, 1, 1, data.primary, 10_000 + data.next(90_001));
        stoneStorage(data, 4, 2, 1, data.secondary, 1_000 + data.next(9_001));

        // GT6 puts one of the five dungeon keys into the shelf (:118-123), choosing the key with
        // next(generatedKeys.length * 2): half of the rolls are "no key at all". GT6's :122 puts the
        // stack into slot 10 + next(18) of that shelf's inventory, which the port's 28 slot shelf has.
        ItemStack key = ItemStack.EMPTY;
        int keySlot = 0;
        GTDungeonData.WorkshopKeyDecision keyDecision = data.workshopKeyDecision();
        int keyIndex = keyDecision.index();
        if (keyIndex < data.generatedKeys.length) {
            data.generatedKeys[keyIndex] = true;
            keySlot = keyDecision.slot();
            key = data.keyStacks[keyIndex];
        }

        // GT6 :118-124: eight manuals and two duct tapes occupy slots 0..9; the key, if rolled,
        // sits in 10..27 and blacksmith loot fills the remaining front slots lazily.
        if (data.shelf(4, 3, 1, LOOT_BLACKSMITH, key, keySlot)) workshopShelfSupplies(data);

        // GT6 rolled the crucible tier here (:130) and derived the burning box, the crucible and both
        // moulds of the smithy row from it; the port registers those blocks, so the roll comes back.
        int tCrucibleType = data.next(3);

        // GT6's smithy row (:132-141). SIDE_X_NEG of GT6's multi-tiles is the port's WEST.
        data.chest(14, 1, 1, LOOT_BLACKSMITH, GTMetalChests.chest(com.gregtech.gregtech.content.material.Materials.StainlessSteel,false), Direction.WEST);
        data.smooth(14, 1, 2);
        data.set(14, 1, 3, burningBox(tCrucibleType));
        data.smooth(14, 1, 4);
        ingotsOrPlates(data, 14, 1, 5);
        ingotsOrPlates(data, 10, 1, 1);
        // GT6: the vanilla anvil (:138), whose meta is the legacy horizontal facing 3 (east,
        // EnumFacing.byHorizontalIndex) combined with next(3) as the damage state. 1.20.1 has no
        // damage property on the anvil, it has three separate blocks instead.
        int anvilDamage = data.next(3);
        data.set(11, 1, 1, (anvilDamage == 0 ? Blocks.ANVIL : anvilDamage == 1 ? Blocks.CHIPPED_ANVIL
                : Blocks.DAMAGED_ANVIL).defaultBlockState()
                .setValue(AnvilBlock.FACING, Direction.EAST));
        // GT6 :139: a steel grindstone facing south with 1..4 abrasive uses already installed.
        BlockState grindstone = grindstone();
        int grindstoneUses = 1 + data.next(4);
        if (grindstone != null && data.set(12, 1, 1, grindstone)
                && data.level.getBlockEntity(new BlockPos(data.x + 12, data.y + 1, data.z + 1))
                instanceof ManualToolBlockEntity tool) {
            tool.setDungeonGrindstoneUses(grindstoneUses);
        }
        // GT6 :140: the material anvil starts with a Vanadium Steel / Spruce hard hammer on its top.
        if (data.set(11, 1, 4, anvil(data.next(4)))
                && data.level.getBlockEntity(new BlockPos(data.x + 11, data.y + 1, data.z + 4))
                instanceof MaterialAnvilBlockEntity materialAnvil) {
            materialAnvil.setDungeonHammer(GTToolHelper.write(GTToolItems.empty(GTToolType.HARD_HAMMER),
                    GT6Materials.Compounds.VanadiumSteel, GT6Materials.Woods.Spruce));
        }
        ingotsOrPlates(data, 11, 1, 5);

        // GT6's smeltery stack (:143-145): the crucible sits on top of its burning box with one mould
        // on either side, all three of the tier rolled at :130. Both moulds have a random valid cavity.
        BlockState mold = mold(tCrucibleType);
        if (mold != null) shapedMold(data, 14, 2, 2, mold);
        data.set(14, 2, 3, crucible(tCrucibleType));
        if (mold != null) shapedMold(data, 14, 2, 4, mold);

        // GT6: the wooden bottle crate at the end of the shelf row, with mercury, glue, lubricant,
        // ink, purple drink, holy water and indigo bottles inside (:149-159).
        DungeonBottleSupplies.chemicals(data, 11, 1, 14);

        // GT6: the ceramic mixing bowl table (:161, multi-tile 32705); a smooth block (:162).
        BlockState mixingBowl = tool("mixing_bowl");
        if (mixingBowl != null) data.set(13, 1, 14, mixingBowl);
        data.smooth(14, 1, 14);

        // GT6: the vanilla cauldron (:163), whose meta is the water level 0..3.
        int cauldronLevel = data.next(4);
        data.set(14, 1, 13, cauldronLevel == 0 ? Blocks.CAULDRON.defaultBlockState()
                : Blocks.WATER_CAULDRON.defaultBlockState()
                        .setValue(LayeredCauldronBlock.LEVEL, cauldronLevel));

        // GT6: the stainless bathing pot table (:164, multi-tile 32707) and, above it, the stainless
        // steel drum with 64000 mB of water, two stainless taps and its ceiling funnel (:166-170).
        BlockState bathingPot = tool("bathing_pot");
        if (bathingPot != null) data.set(14, 1, 11, bathingPot);
        WorkshopFluidStation.place(data, 14, 2, 14);

        // GT6's drink corner (:174-205): a 2x2 area holds up to three stacked drinks each. GT6 rolls
        // the barrel size first (:174, 32000 mB ironwood barrel 32734 or 16000 mB wooden barrel 32714)
        // and then rolls one of seven drinks per barrel, or a bottle crate of filled bottles; the port
        // owns both barrels with GT6's capacities, so the size roll and the drink roll are kept.
        int tAmount = data.next1in3() ? 32000 : 16000;
        boolean ironwood = tAmount > 16000;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                if (data.next2in3()) {
                    for (int k = 0; k < 3; k++) {
                        // GT6 :180 uses UT.Code.select(NF, tDrinks), whose shared RNG must not
                        // advance the room's own stream. The port's coordinate-seeded selection
                        // preserves the seven weighted entries without cross-chunk RNG state.
                        String drink = data.select(DRINKS, 1 + i, 1 + k, 12 + j);
                        if (data.next1in3()) {
                            DungeonBottleSupplies.drinks(data, 1 + i, 1 + k, 12 + j, drink);
                            break;
                        }
                        drinkBarrel(data, 1 + i, 1 + k, 12 + j, ironwood, drink, tAmount);
                        if (data.next1in3()) break;
                    }
                } else if (data.next2in3()) {
                    // GT6 picks a propane, oxygen or helium gas cylinder here (:199-203): the propane
                    // one is the steel cylinder 32055, the other two the stainless one 32056.
                    switch (data.next(3)) {
                        case 0 -> gasCylinder(data, 1 + i, 1, 12 + j, false, PROPANE);
                        case 1 -> gasCylinder(data, 1 + i, 1, 12 + j, true, OXYGEN);
                        default -> gasCylinder(data, 1 + i, 1, 12 + j, true, HELIUM);
                    }
                }
            }
        }
        return true;
    }

    // ---- the port's own smithy row blocks ----

    /**
     * GT6's burning box {@code 1102+tCrucibleType} (:134, {@code Loader_MultiTileEntities.java:524-528}:
     * 1102 bronze, 1103 invar, 1104 steel) with GT6's facing {@code SIDE_X_NEG} (west).
     */
    private static BlockState burningBox(int tCrucibleType) {
        var block = switch (tCrucibleType) {
            case 1 -> DungeonBindings.supplier("burning_box_solid_invar");
            case 2 -> DungeonBindings.supplier("burning_box_solid_steel");
            default -> DungeonBindings.supplier("burning_box_solid_bronze");
        };
        return block.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST);
    }

    /**
     * GT6's smelting crucible {@code 1020+tCrucibleType} (:144,
     * {@code Loader_MultiTileEntities.java:265-267}: 1020 bronze, 1021 invar, 1022 steel). GT6 writes
     * {@code NBT_FACING} onto it; the port's crucible is a plain block without a facing property, so
     * the direction is not expressible and the block is placed with its default state.
     */
    private static BlockState crucible(int tCrucibleType) {
        var block = switch (tCrucibleType) {
            case 1 -> DungeonBindings.supplier("smelting_crucible_invar");
            case 2 -> DungeonBindings.supplier("smelting_crucible_steel");
            default -> DungeonBindings.supplier("smelting_crucible_bronze");
        };
        return block.get().defaultBlockState();
    }

    /**
     * GT6's mould {@code 1070+tCrucibleType} (:143, :145). The port registers one mould per crucible
     * tier as a companion of the crucible ({@code MachineRegistry:147-153}), carrying the GT6 meta id
     * in its {@code CrucibleSpec}, so the tier is resolved through that id instead of a name.
     */
    private static BlockState mold(int tCrucibleType) {
        int gt6Id = GT6_MOLD_BRONZE + tCrucibleType;
        for (var holder : SmelteryRegistries.molds()) {
            if (!holder.isBound()) continue;
            MoldBlock block = holder.get();
            if (block.spec().gt6MetaId() == gt6Id) return block.defaultBlockState();
        }
        return null;
    }

    /** GT6's steel grindstone {@code 32703} (:139) as the port's {@code grindstone_block}. */
    private static BlockState grindstone() {
        for (var holder : GTManualStations.MANUAL) {
            if (!holder.isBound()) continue;
            ManualToolBlock block = holder.get();
            if (block.kind() == ManualToolBlockEntity.Kind.GRINDSTONE) {
                return block.defaultBlockState().setValue(ManualToolBlock.FACING, Direction.SOUTH);
            }
        }
        return null;
    }

    /** GT6's ceramic mortar {@code 32735} (:58) as the port's {@code mortar_block}. */
    private static BlockState mortar() {
        for (var holder : GTManualStations.MANUAL) {
            if (!holder.isBound()) continue;
            ManualToolBlock block = holder.get();
            if (block.kind() == ManualToolBlockEntity.Kind.MORTAR) return block.defaultBlockState();
        }
        return null;
    }

    /** GT6's second anvil multi-tile {@code 32034+rnd(4)} (:140) as the port's material anvil. */
    private static BlockState anvil(int index) {
        String material = ANVIL_MATERIALS[index];
        for (var holder : GTManualStations.ANVILS) {
            if (!holder.isBound()) continue;
            if (holder.get().material().getName().equals(material)) return holder.get().defaultBlockState();
        }
        return Blocks.ANVIL.defaultBlockState();
    }

    /** GT6's mass storage ({@code 6011}, :116, :117) as the port's own container block. */
    private static BlockState storage(Direction facing) {
        return DungeonBindings.block("mass_storage").defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static void stoneStorage(GTDungeonData data, int x, int y, int z, StoneType rock, int count) {
        Block cobble = GTBlocks.getStone(rock, StoneVariant.COBBLE);
        if (cobble == null) throw new IllegalStateException("Missing GT6 dungeon cobblestone: " + rock);
        if (data.set(x, y, z, storage(Direction.SOUTH))
                && data.level.getBlockEntity(new BlockPos(data.x + x, data.y + y, data.z + z))
                instanceof MassStorageBlockEntity storage) {
            storage.insert(new ItemStack(cobble, count));
        }
    }

    private static void shapedMold(GTDungeonData data, int x, int y, int z, BlockState state) {
        int shape = MoldBlockEntity.randomDungeonShape(data.random);
        if (data.set(x, y, z, state)
                && data.level.getBlockEntity(new BlockPos(data.x + x, data.y + y, data.z + z))
                instanceof MoldBlockEntity mold) {
            mold.setDungeonShape(shape);
        }
    }

    /** All eight GT6 workshop manuals and both utility tapes in their original fixed slots. */
    private static void workshopShelfSupplies(GTDungeonData data) {
        if (!(data.level.getBlockEntity(new BlockPos(data.x + 4, data.y + 3, data.z + 1))
                instanceof BookShelfBlockEntity shelf)) return;
        String[] manuals = {"Manual_Elements", "Manual_Alloys", "Manual_Smeltery",
                "Manual_Random", "Manual_Extenders", "Manual_Steam", "Manual_Tools", "Manual_Printer"};
        for (int i = 0; i < manuals.length; i++) {
            ItemStack book = GTBooks.bookStack(manuals[i]);
            if (book.isEmpty()) throw new IllegalStateException("Missing GT6 workshop manual: " + manuals[i]);
            shelf.inventory().setStackInSlot(i, book);
        }
        var tape = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", "duct_tape"));
        if (tape == null) throw new IllegalStateException("Missing GT6 workshop duct tape");
        shelf.inventory().setStackInSlot(8, new ItemStack(tape));
        shelf.inventory().setStackInSlot(9, new ItemStack(tape));
    }

    /** GT6's compartment drawer ({@code 4011}, :97) as the port's own container block. */
    private static BlockState drawer(Direction facing) {
        return DungeonBindings.block("drawer_quad").defaultBlockState()
                .setValue(com.gregtech.gregtech.block.inventory.DrawerQuadBlock.FACING, facing);
    }

    /**
     * GT6's remaining fluid containers (see the class javadoc): each of them is one of the port's own
     * tanks or vessels at GT6's position, filled with GT6's fluid and amount
     * ({@link GTDungeonData#tank}).
     *
     * <p>GT6's Steel Barometer Gas Cylinder {@code 32055} ({@code :51}, {@code :200}) and its Stainless
     * Barometer Gas Cylinder {@code 32056} ({@code :201}, {@code :202}), both
     * {@code MultiTileEntityBarometerGasCylinder} with {@code NBT_TANK_CAPACITY 8000} - the port's
     * {@code fluid_barometer_gas_cylinder} and {@code fluid_barometer_gas_cylinder_stainless_steel}
     * ({@code PortableFluidContainerSpec.BAROMETER_GAS_CYLINDER}/{@code GAS_CYLINDER_STAINLESS}: steel
     * without acid proof, stainless steel with it, exactly GT6's
     * {@code Loader_MultiTileEntities:2100-2101}).</p>
     */
    private static void gasCylinder(GTDungeonData data, int x, int y, int z, boolean stainless, String fluid) {
        data.tank(x, y, z, container(stainless
                ? "fluid_barometer_gas_cylinder_stainless_steel"
                : "fluid_barometer_gas_cylinder"), GTFluids.stack(fluid, 8000));
    }

    /**
     * GT6's Ceramic Measuring Pot {@code 32738} ({@code :113},
     * {@code Loader_MultiTileEntities:2095}, {@code MultiTileEntityMeasuringPot}, 1000 mB) as the port's
     * {@code fluid_measuring_pot} ({@code PortableFluidContainerSpec.MEASURING_POT}). GT6 writes no tank
     * NBT for it, so the pot is placed empty.
     */
    private static void measuringPot(GTDungeonData data, int x, int y, int z) {
        data.tank(x, y, z, container("fluid_measuring_pot"), null);
    }

    /**
     * GT6's Stainless Steel Drum {@code 32716} ({@code :167},
     * {@code Loader_MultiTileEntities:2152}, {@code MultiTileEntityBarrelMetal}, 64000 mB, gas and acid
     * proof) as the port's {@code drum_stainless_steel} - the same material, capacity and proof flags -
     * filled with GT6's 64000 mB of water.
     */
    private static void drum(GTDungeonData data, int x, int y, int z) {
        data.tank(x, y, z, container("drum_stainless_steel"), GTFluids.stack(WATER, 64000));
    }

    /**
     * One of GT6's drink barrels ({@code :196}): the Ironwood Barrel {@code 32734} at 32000 mB
     * ({@code Loader_MultiTileEntities:2146}) as the port's {@code wood_barrel_ironwood} or the Wooden
     * Barrel {@code 32714} at 16000 mB (treated wood, {@code :2139}) as the port's
     * {@code wood_barrel_treated}; both port barrels carry GT6's material and capacity. GT6 picks the
     * drink with {@code UT.Code.select(NF, tDrinks)} ({@code :180-181}), which is the roll the caller
     * keeps.
     */
    private static void drinkBarrel(GTDungeonData data, int x, int y, int z,
                                    boolean ironwood, String drink, int amount) {
        data.tank(x, y, z, container(ironwood ? "wood_barrel_ironwood" : "wood_barrel_treated"),
                GTFluids.stack(drink, amount));
    }

    /**
     * One of the port's container blocks by registry name, or the port's plain wood barrel should it
     * ever be missing - the port registers every id used here unconditionally
     * ({@code TankDefinitions.register}, {@code GTToolBlocks.registerAll}).
     */
    private static Block container(String id) {
        Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gregtech", id));
        return block == null ? DungeonBindings.block("wood_barrel") : block;
    }

    /** One of the port's simple tool blocks by registry name, or {@code null} when it is missing. */
    private static BlockState tool(String id) {
        Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gregtech", id));
        return block == null ? null : block.defaultBlockState();
    }

    /**
     * GT6's {@code DungeonData.ingots_or_plates} ({@code DungeonData:259-264}): metallic ingots or
     * plates of one of GT6's thirteen {@code sMetals} ({@code :44}) lie on the floor. Both candidates
     * draw their own material and stack size before the final kind roll.
     */
    private static void ingotsOrPlates(GTDungeonData data, int x, int y, int z) {
        data.ingotsOrPlates(x, y, z, 0, WORKSHOP_METALS);
    }

    private static final String[] WORKSHOP_METALS = {
            "DamascusSteel", "DamascusSteel", "DamascusSteel", "BlackSteel", "RedSteel", "BlueSteel",
            "VanadiumSteel", "Steel", "Iron", "Brass", "Bronze", "BismuthBronze", "BlackBronze"};
}
