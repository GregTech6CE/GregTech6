package com.gregtech.gregtech.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

/** GT6 storage blocks (mass storage; drawers/lockers/safes to follow). */
public final class GTStorage {

    public static RegistryObject<com.gregtech.gregtech.block.inventory.MassStorageBlock> MASS_STORAGE;
    /** Per-material variants in GT6 metalset order. */
    public static final java.util.List<RegistryObject<com.gregtech.gregtech.block.inventory.MassStorageBlock>> MASS_STORAGES =
            new java.util.ArrayList<>();
    /** GT6 6200+ metalset: direct logistics-network storages, in metalset registration order. */
    public static final java.util.List<RegistryObject<com.gregtech.gregtech.block.inventory.LogisticsMassStorageBlock>> LOGISTICS_MASS_STORAGES =
            new java.util.ArrayList<>();
    public static final java.util.List<RegistryObject<com.gregtech.gregtech.block.inventory.MetalChestBlock>> METAL_CHESTS =
            new java.util.ArrayList<>();
    public static RegistryObject<com.gregtech.gregtech.block.inventory.DrawerQuadBlock> DRAWER_QUAD;
    public static final java.util.List<RegistryObject<com.gregtech.gregtech.block.inventory.DrawerQuadBlock>> DRAWERS = new java.util.ArrayList<>();
    public static com.gregtech.gregtech.block.inventory.DrawerQuadBlock drawer(com.gregtech.gregtech.api.material.GTMaterial material) {
        for (var entry : DRAWERS) if (entry.get().material() == material) return entry.get();
        throw new IllegalArgumentException("No drawer for " + material.getName());
    }
    /** GT6's Wooden Bottlecrate (multi-tile 8762, a nine bottle display crate). */
    public static RegistryObject<com.gregtech.gregtech.block.inventory.BottleCrateBlock> BOTTLE_CRATE;
    /** GT6 multi-tile 19000: sixteen USB-stick positions selected by a cover. */
    public static RegistryObject<com.gregtech.gregtech.block.inventory.UsbSwitchBlock> USB_SWITCH;
    /** GT6 multi-tile 19001: one HDD holding sixteen selectable data positions. */
    public static RegistryObject<com.gregtech.gregtech.block.inventory.UsbSwitchBlock> HDD_SWITCH;
    public static RegistryObject<com.gregtech.gregtech.block.inventory.SafeBlock> SAFE;
    public static final java.util.List<RegistryObject<com.gregtech.gregtech.block.inventory.SafeBlock>> SAFES = new java.util.ArrayList<>();
    public static final java.util.List<RegistryObject<com.gregtech.gregtech.block.inventory.SafeBlock>> KEY_SAFES = new java.util.ArrayList<>();
    public static com.gregtech.gregtech.block.inventory.SafeBlock safe(com.gregtech.gregtech.api.material.GTMaterial material, boolean keyLocked) {
        for (var entry : keyLocked ? KEY_SAFES : SAFES) if (entry.get().material() == material) return entry.get();
        throw new IllegalArgumentException("No safe for " + material.getName());
    }
    public static RegistryObject<com.gregtech.gregtech.block.inventory.LockerBlock> LOCKER;
    public static RegistryObject<com.gregtech.gregtech.block.inventory.EnderGarbageBlock> ENDER_GARBAGE;
    public static RegistryObject<com.gregtech.gregtech.block.inventory.EnderGarbageDumpBlock> ENDER_GARBAGE_DUMP;

    private GTStorage() {}
    public static final java.util.List<RegistryObject<com.gregtech.gregtech.block.inventory.MetalChestBlock>> REINFORCED_WOOD_CHESTS =
            new java.util.ArrayList<>();

    public static com.gregtech.gregtech.block.inventory.MetalChestBlock reinforcedChest(com.gregtech.gregtech.api.material.GTMaterial material) {
        for (var entry : REINFORCED_WOOD_CHESTS) if (entry.get().material() == material) return entry.get();
        throw new IllegalArgumentException("No reinforced wooden chest for " + material.getName());
    }

    /** Registered chest of the requested material, for world generation and content definitions. */
    public static com.gregtech.gregtech.block.inventory.MetalChestBlock chest(com.gregtech.gregtech.api.material.GTMaterial material) {
        for (var entry : METAL_CHESTS) {
            var chest = entry.get();
            if (chest.material() == material) return chest;
        }
        throw new IllegalArgumentException("No registered GT chest for " + material.getName());
    }


    public static void registerAll() {
        MASS_STORAGE = GTBlocks.BLOCKS.register("mass_storage", () ->
                new com.gregtech.gregtech.block.inventory.MassStorageBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(4.0f, 4.0f)
                                .requiresCorrectToolForDrops()));
        GTBlocks.BLOCK_ITEMS.register("mass_storage",
                () -> new BlockItem(MASS_STORAGE.get(), new Item.Properties().stacksTo(16)));   // GT6 Storage: 16
        for (GTStorageMetals.Spec spec : GTStorageMetals.ALL) {
            String id = spec.suffix().equals("stainless_steel") ? "drawer_quad" : "drawer_quad_" + spec.suffix();
            var drawer = GTBlocks.BLOCKS.register(id, () -> new com.gregtech.gregtech.block.inventory.DrawerQuadBlock(spec.material(),
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness(),spec.resistance()).requiresCorrectToolForDrops()));
            GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(drawer.get(),new Item.Properties().stacksTo(16)));
            DRAWERS.add(drawer);
            if (spec.suffix().equals("stainless_steel")) DRAWER_QUAD = drawer;
        }
        BOTTLE_CRATE = GTBlocks.BLOCKS.register("bottle_crate", () ->
                new com.gregtech.gregtech.block.inventory.BottleCrateBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.WOOD).noOcclusion()
                                .strength(0.5f, 2.0f)
                                .sound(net.minecraft.world.level.block.SoundType.WOOD)));
        GTBlocks.BLOCK_ITEMS.register("bottle_crate",
                () -> new BlockItem(BOTTLE_CRATE.get(), new Item.Properties()));   // GT6 Bottlecrate: 64
        USB_SWITCH = GTBlocks.BLOCKS.register("usb_switch", () ->
                new com.gregtech.gregtech.block.inventory.UsbSwitchBlock(
                        com.gregtech.gregtech.block.inventory.UsbSwitchBlock.Kind.USB,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(6.0f, 6.0f)
                                .requiresCorrectToolForDrops()));
        GTBlocks.BLOCK_ITEMS.register("usb_switch",
                () -> new BlockItem(USB_SWITCH.get(), new Item.Properties()));
        HDD_SWITCH = GTBlocks.BLOCKS.register("hdd_switch", () ->
                new com.gregtech.gregtech.block.inventory.UsbSwitchBlock(
                        com.gregtech.gregtech.block.inventory.UsbSwitchBlock.Kind.HDD,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(6.0f, 6.0f)
                                .requiresCorrectToolForDrops()));
        GTBlocks.BLOCK_ITEMS.register("hdd_switch",
                () -> new BlockItem(HDD_SWITCH.get(), new Item.Properties()));
        SAFE = GTBlocks.BLOCKS.register("safe", () ->
                new com.gregtech.gregtech.block.inventory.SafeBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(12.0f, 12.0f)
                                .requiresCorrectToolForDrops()));
        GTBlocks.BLOCK_ITEMS.register("safe",
                () -> new BlockItem(SAFE.get(), new Item.Properties().stacksTo(16)));
        LOCKER = GTBlocks.BLOCKS.register("locker", () ->
                new com.gregtech.gregtech.block.inventory.LockerBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(3.0f, 3.0f)
                                .requiresCorrectToolForDrops()));
        GTBlocks.BLOCK_ITEMS.register("locker",
                () -> new BlockItem(LOCKER.get(), new Item.Properties()));
        ENDER_GARBAGE = GTBlocks.BLOCKS.register("ender_garbage_bin", () ->
                new com.gregtech.gregtech.block.inventory.EnderGarbageBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.COLOR_BLACK)
                                .strength(3.0f, 3.0f)
                                .requiresCorrectToolForDrops()
                                .noOcclusion()));
        GTBlocks.BLOCK_ITEMS.register("ender_garbage_bin",
                () -> new BlockItem(ENDER_GARBAGE.get(), new Item.Properties()));
        ENDER_GARBAGE_DUMP = GTBlocks.BLOCKS.register("ender_garbage_dump", () ->
                new com.gregtech.gregtech.block.inventory.EnderGarbageDumpBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.COLOR_BLACK)
                                .strength(8.0f, 1200.0f)
                                .requiresCorrectToolForDrops()));
        GTBlocks.BLOCK_ITEMS.register("ender_garbage_dump",
                () -> new BlockItem(ENDER_GARBAGE_DUMP.get(), new Item.Properties()));

        // per-material variants (GT6 metalset): mass storages + metal chests
        for (GTStorageMetals.Spec spec : GTStorageMetals.ALL) {
            for (boolean keyLocked : new boolean[]{false, true}) {
                var target = keyLocked ? KEY_SAFES : SAFES;
                if (!keyLocked && spec.suffix().equals("steel")) { target.add(SAFE); continue; }
                String id = (keyLocked ? "key_safe_" : "safe_") + spec.suffix();
                var safe = GTBlocks.BLOCKS.register(id, () -> new com.gregtech.gregtech.block.inventory.SafeBlock(spec.material(), keyLocked,
                        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness() * 2, spec.resistance() * 2)
                                .requiresCorrectToolForDrops()));
                GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(safe.get(), new Item.Properties().stacksTo(16)));
                target.add(safe);
            }
            var storage = GTBlocks.BLOCKS.register("mass_storage_" + spec.suffix(), () ->
                    new com.gregtech.gregtech.block.inventory.MassStorageBlock(spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()));
            GTBlocks.BLOCK_ITEMS.register("mass_storage_" + spec.suffix(),
                    () -> new BlockItem(storage.get(), new Item.Properties().stacksTo(16)));   // GT6 Storage: 16
            MASS_STORAGES.add(storage);
            var logisticsStorage = GTBlocks.BLOCKS.register("logistics_mass_storage_" + spec.suffix(), () ->
                    new com.gregtech.gregtech.block.inventory.LogisticsMassStorageBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()));
            GTBlocks.BLOCK_ITEMS.register("logistics_mass_storage_" + spec.suffix(),
                    () -> new BlockItem(logisticsStorage.get(), new Item.Properties().stacksTo(16)));
            LOGISTICS_MASS_STORAGES.add(logisticsStorage);
            var chest = GTBlocks.BLOCKS.register("chest_" + spec.suffix(), () ->
                    new com.gregtech.gregtech.block.inventory.MetalChestBlock(spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()));
            GTBlocks.BLOCK_ITEMS.register("chest_" + spec.suffix(),
                    () -> new com.gregtech.gregtech.block.inventory.MetalChestBlockItem(chest.get(), new Item.Properties()));
            METAL_CHESTS.add(chest);
            var wooden = GTBlocks.BLOCKS.register("reinforced_wood_chest_" + spec.suffix(), () ->
                    new com.gregtech.gregtech.block.inventory.MetalChestBlock(spec.material(),
                            com.gregtech.gregtech.block.inventory.MetalChestBlock.Shell.REINFORCED_WOOD,
                            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                                    .sound(net.minecraft.world.level.block.SoundType.WOOD)
                                    .strength(spec.hardness() / 2, spec.resistance() / 2).noOcclusion()));
            GTBlocks.BLOCK_ITEMS.register("reinforced_wood_chest_" + spec.suffix(), () ->
                    new com.gregtech.gregtech.block.inventory.MetalChestBlockItem(wooden.get(), new Item.Properties().stacksTo(16)));
            REINFORCED_WOOD_CHESTS.add(wooden);
        }
    }
}
