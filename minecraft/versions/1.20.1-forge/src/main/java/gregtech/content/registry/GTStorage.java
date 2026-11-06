package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.api.material.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** GT6 storage blocks (mass storage; drawers/lockers/safes to follow). */
public final class GTStorage {

    public static final java.util.List<RegisteredBlock<gregtech.content.block.inventory.LockerBlock>> LOCKERS =
            new java.util.ArrayList<>();
    public static RegisteredBlock<gregtech.content.block.inventory.MassStorageBlock> MASS_STORAGE;
    /** Per-material variants in GT6 metalset order. */
    public static final java.util.List<RegisteredBlock<gregtech.content.block.inventory.MassStorageBlock>>
            MASS_STORAGES = new java.util.ArrayList<>();
    /** GT6 6200+ metalset: direct logistics-network storages, in metalset registration order. */
    public static final java.util.List<RegisteredBlock<gregtech.content.block.inventory.LogisticsMassStorageBlock>>
            LOGISTICS_MASS_STORAGES = new java.util.ArrayList<>();

    public static final java.util.List<RegisteredBlock<gregtech.content.block.inventory.MetalChestBlock>> METAL_CHESTS =
            new java.util.ArrayList<>();
    public static RegisteredBlock<gregtech.content.block.inventory.DrawerQuadBlock> DRAWER_QUAD;
    public static final java.util.List<RegisteredBlock<gregtech.content.block.inventory.DrawerQuadBlock>> DRAWERS =
            new java.util.ArrayList<>();

    public static gregtech.content.block.inventory.DrawerQuadBlock drawer(gregtech.api.material.GTMaterial material) {
        for (var entry : DRAWERS) if (entry.get().material() == material) return entry.get();
        throw new IllegalArgumentException("No drawer for " + material.getName());
    }
    /** GT6's Wooden Bottlecrate (multi-tile 8762, a nine bottle display crate). */
    public static RegisteredBlock<gregtech.content.block.inventory.BottleCrateBlock> BOTTLE_CRATE;
    /** GT6 multi-tile 19000: sixteen USB-stick positions selected by a cover. */
    public static RegisteredBlock<gregtech.content.block.inventory.UsbSwitchBlock> USB_SWITCH;
    /** GT6 multi-tile 19001: one HDD holding sixteen selectable data positions. */
    public static RegisteredBlock<gregtech.content.block.inventory.UsbSwitchBlock> HDD_SWITCH;

    public static RegisteredBlock<gregtech.content.block.inventory.SafeBlock> SAFE;
    public static final java.util.List<RegisteredBlock<gregtech.content.block.inventory.SafeBlock>> SAFES =
            new java.util.ArrayList<>();
    public static final java.util.List<RegisteredBlock<gregtech.content.block.inventory.SafeBlock>> KEY_SAFES =
            new java.util.ArrayList<>();

    public static gregtech.content.block.inventory.SafeBlock safe(
            gregtech.api.material.GTMaterial material, boolean keyLocked) {
        for (var entry : keyLocked ? KEY_SAFES : SAFES) if (entry.get().material() == material) return entry.get();
        throw new IllegalArgumentException("No safe for " + material.getName());
    }

    public static RegisteredBlock<gregtech.content.block.inventory.LockerBlock> LOCKER;
    public static RegisteredBlock<gregtech.content.block.inventory.EnderGarbageBlock> ENDER_GARBAGE;
    public static RegisteredBlock<gregtech.content.block.inventory.EnderGarbageDumpBlock> ENDER_GARBAGE_DUMP;

    public static final java.util.List<RegisteredBlock<gregtech.content.block.inventory.BottleCrateBlock>>
            BOTTLE_CRATES = new java.util.ArrayList<>();

    private GTStorage() {}

    public static final java.util.List<RegisteredBlock<gregtech.content.block.inventory.MetalChestBlock>>
            REINFORCED_WOOD_CHESTS = new java.util.ArrayList<>();

    public static gregtech.content.block.inventory.MetalChestBlock reinforcedChest(
            gregtech.api.material.GTMaterial material) {
        for (var entry : REINFORCED_WOOD_CHESTS) if (entry.get().material() == material) return entry.get();
        throw new IllegalArgumentException("No reinforced wooden chest for " + material.getName());
    }

    /** Registered chest of the requested material, for world generation and content definitions. */
    public static gregtech.content.block.inventory.MetalChestBlock chest(gregtech.api.material.GTMaterial material) {
        for (var entry : METAL_CHESTS) {
            var chest = entry.get();
            if (chest.material() == material) return chest;
        }
        throw new IllegalArgumentException("No registered GT chest for " + material.getName());
    }

    public static void registerAll(ContentRegistrar registrar) {
        MASS_STORAGE = RegisteredBlock.reference("mass_storage_stainless_steel");
        for (GTStorageMetals.Spec spec : GTStorageMetals.ALL) {
            String id = spec.suffix().equals("stainless_steel") ? "drawer_quad" : "drawer_quad_" + spec.suffix();
            var drawer = registrar.block(
                    REGISTRY_NAMESPACE,
                    id,
                    () -> new gregtech.content.block.inventory.DrawerQuadBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()),
                    block -> new BlockItem(block, new Item.Properties().stacksTo(16)));
            DRAWERS.add(drawer);
            if (spec.suffix().equals("stainless_steel")) DRAWER_QUAD = drawer;
        }
        BOTTLE_CRATE = registrar.block(
                REGISTRY_NAMESPACE,
                "bottle_crate",
                () -> new gregtech.content.block.inventory.BottleCrateBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .noOcclusion()
                        .strength(0.5f, 2.0f)
                        .sound(net.minecraft.world.level.block.SoundType.WOOD)),
                block -> new BlockItem(block, new Item.Properties().stacksTo(16)));

        BOTTLE_CRATES.add(BOTTLE_CRATE);
        for (var spec : gregtech.content.storage.BottleCrateVariants.WOODS) {
            var crate = registrar.block(
                    REGISTRY_NAMESPACE,
                    spec.id(),
                    () -> new gregtech.content.block.inventory.BottleCrateBlock(
                            null,
                            spec.texture(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOD)
                                    .noOcclusion()
                                    .strength(.5f, 2f)
                                    .sound(net.minecraft.world.level.block.SoundType.WOOD)),
                    block -> new BlockItem(block, new Item.Properties().stacksTo(16)));
            BOTTLE_CRATES.add(crate);
        }
        for (var spec : GTStorageMetals.ALL) {
            String id = "bottle_crate_" + spec.suffix();
            var crate = registrar.block(
                    REGISTRY_NAMESPACE,
                    id,
                    () -> new gregtech.content.block.inventory.BottleCrateBlock(
                            spec.material(),
                            null,
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .noOcclusion()
                                    .strength(.5f, spec.resistance())
                                    .sound(net.minecraft.world.level.block.SoundType.METAL)
                                    .requiresCorrectToolForDrops()),
                    block -> new BlockItem(block, new Item.Properties().stacksTo(16)));
            BOTTLE_CRATES.add(crate);
        }
        USB_SWITCH = registrar.block(
                REGISTRY_NAMESPACE,
                "usb_switch",
                () -> new gregtech.content.block.inventory.UsbSwitchBlock(
                        gregtech.content.block.inventory.UsbSwitchBlock.Kind.USB,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(6.0f, 6.0f)
                                .requiresCorrectToolForDrops()),
                block -> new BlockItem(block, new Item.Properties()));
        HDD_SWITCH = registrar.block(
                REGISTRY_NAMESPACE,
                "hdd_switch",
                () -> new gregtech.content.block.inventory.UsbSwitchBlock(
                        gregtech.content.block.inventory.UsbSwitchBlock.Kind.HDD,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(6.0f, 6.0f)
                                .requiresCorrectToolForDrops()),
                block -> new BlockItem(block, new Item.Properties()));
        SAFE = registrar.block(
                REGISTRY_NAMESPACE,
                "safe",
                () -> new gregtech.content.block.inventory.SafeBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(12.0f, 12.0f)
                        .requiresCorrectToolForDrops()),
                block -> new BlockItem(block, new Item.Properties().stacksTo(16)));
        for (var spec : GTStorageMetals.ALL) {
            String id = spec.suffix().equals("stainless_steel") ? "locker" : "locker_" + spec.suffix();
            var locker = registrar.block(
                    REGISTRY_NAMESPACE,
                    id,
                    () -> new gregtech.content.block.inventory.LockerBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()),
                    block -> new BlockItem(block, new Item.Properties().stacksTo(16)));
            LOCKERS.add(locker);
            if (id.equals("locker")) LOCKER = locker;
        }
        ENDER_GARBAGE = registrar.block(
                REGISTRY_NAMESPACE,
                "ender_garbage_bin",
                () -> new gregtech.content.block.inventory.EnderGarbageBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BLACK)
                        .strength(3.0f, 3.0f)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()),
                block -> new BlockItem(block, new Item.Properties()));
        ENDER_GARBAGE_DUMP = registrar.block(
                REGISTRY_NAMESPACE,
                "ender_garbage_dump",
                () -> new gregtech.content.block.inventory.EnderGarbageDumpBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BLACK)
                        .strength(8.0f, 1200.0f)
                        .requiresCorrectToolForDrops()),
                block -> new BlockItem(block, new Item.Properties()));

        // per-material variants (GT6 metalset): mass storages + metal chests
        for (GTStorageMetals.Spec spec : GTStorageMetals.ALL) {
            for (boolean keyLocked : new boolean[] {false, true}) {
                var target = keyLocked ? KEY_SAFES : SAFES;
                if (!keyLocked && spec.suffix().equals("steel")) {
                    target.add(SAFE);
                    continue;
                }
                String id = (keyLocked ? "key_safe_" : "safe_") + spec.suffix();
                var safe = registrar.block(
                        REGISTRY_NAMESPACE,
                        id,
                        () -> new gregtech.content.block.inventory.SafeBlock(
                                spec.material(),
                                keyLocked,
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(spec.hardness() * 2, spec.resistance() * 2)
                                        .requiresCorrectToolForDrops()),
                        block -> new BlockItem(block, new Item.Properties().stacksTo(16)));
                target.add(safe);
            }
            var storage = registrar.block(
                    REGISTRY_NAMESPACE,
                    "mass_storage_" + spec.suffix(),
                    () -> new gregtech.content.block.inventory.MassStorageBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()),
                    block -> new BlockItem(block, new Item.Properties().stacksTo(16)));
            MASS_STORAGES.add(storage);
            var logisticsStorage = registrar.block(
                    REGISTRY_NAMESPACE,
                    "logistics_mass_storage_" + spec.suffix(),
                    () -> new gregtech.content.block.inventory.LogisticsMassStorageBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()),
                    block -> new BlockItem(block, new Item.Properties().stacksTo(16)));
            LOGISTICS_MASS_STORAGES.add(logisticsStorage);
            var chest = registrar.block(
                    REGISTRY_NAMESPACE,
                    "chest_" + spec.suffix(),
                    () -> new gregtech.content.block.inventory.MetalChestBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()),
                    block -> new gregtech.content.block.inventory.MetalChestBlockItem(block, new Item.Properties()));
            METAL_CHESTS.add(chest);
            var wooden = registrar.block(
                    REGISTRY_NAMESPACE,
                    "reinforced_wood_chest_" + spec.suffix(),
                    () -> new gregtech.content.block.inventory.MetalChestBlock(
                            spec.material(),
                            gregtech.content.block.inventory.MetalChestBlock.Shell.REINFORCED_WOOD,
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOD)
                                    .sound(net.minecraft.world.level.block.SoundType.WOOD)
                                    .strength(spec.hardness() / 2, spec.resistance() / 2)
                                    .noOcclusion()),
                    block -> new gregtech.content.block.inventory.MetalChestBlockItem(
                            block, new Item.Properties().stacksTo(16)));
            REINFORCED_WOOD_CHESTS.add(wooden);
        }
    }
}
