package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.inventory.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTStorageContainers {

    private GTStorageContainers() {}

    public static final java.util.List<RegisteredBlock<LockerBlock>> LOCKERS = new java.util.ArrayList<>();
    public static final java.util.List<RegisteredBlock<DrawerQuadBlock>> DRAWERS = new java.util.ArrayList<>();
    public static RegisteredBlock<BottleCrateBlock> BOTTLE_CRATE;
    public static RegisteredBlock<LockerBlock> LOCKER;
    public static RegisteredBlock<UsbSwitchBlock> USB_SWITCH, HDD_SWITCH;
    public static RegisteredBlock<EnderGarbageBlock> ENDER_GARBAGE;
    public static RegisteredBlock<EnderGarbageDumpBlock> ENDER_GARBAGE_DUMP;

    public static final java.util.List<RegisteredBlock<BottleCrateBlock>> BOTTLE_CRATES =
            new java.util.ArrayList<>();

    public static void initialize(ContentRegistrar registrar) {
        for (var spec : GTStorageMetals.ALL) {
            String id = spec.suffix().equals("stainless_steel") ? "drawer_quad" : "drawer_quad_" + spec.suffix();
            var registeredDrawer = registrar.block(
                    REGISTRY_NAMESPACE,
                    id,
                    () -> new DrawerQuadBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()),
                    bare -> new BlockItem(bare, new Item.Properties().stacksTo(16)));
            DRAWERS.add(registeredDrawer);
        }
        BOTTLE_CRATE = registrar.block(
                REGISTRY_NAMESPACE,
                "bottle_crate",
                () -> new BottleCrateBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .noOcclusion()
                        .strength(.5f, 2f)
                        .sound(SoundType.WOOD)),
                bare -> new BlockItem(bare, new Item.Properties().stacksTo(16)));

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
                    bare -> new BlockItem(bare, new Item.Properties().stacksTo(16)));
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
                    bare -> new BlockItem(bare, new Item.Properties().stacksTo(16)));
            BOTTLE_CRATES.add(crate);
        }
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
                    bare -> new BlockItem(bare, new Item.Properties().stacksTo(16)));
            LOCKERS.add(locker);
            if (id.equals("locker")) LOCKER = locker;
        }
        ENDER_GARBAGE = registrar.block(
                REGISTRY_NAMESPACE,
                "ender_garbage_bin",
                () -> new EnderGarbageBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BLACK)
                        .strength(3, 3)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()),
                bare -> new BlockItem(bare, new Item.Properties()));
        ENDER_GARBAGE_DUMP = registrar.block(
                REGISTRY_NAMESPACE,
                "ender_garbage_dump",
                () -> new EnderGarbageDumpBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BLACK)
                        .strength(8, 1200)
                        .requiresCorrectToolForDrops()),
                bare -> new BlockItem(bare, new Item.Properties()));
        USB_SWITCH = registrar.block(
                REGISTRY_NAMESPACE,
                "usb_switch",
                () -> new UsbSwitchBlock(
                        UsbSwitchBlock.Kind.USB,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(6, 6)
                                .requiresCorrectToolForDrops()),
                bare -> new BlockItem(bare, new Item.Properties()));
        HDD_SWITCH = registrar.block(
                REGISTRY_NAMESPACE,
                "hdd_switch",
                () -> new UsbSwitchBlock(
                        UsbSwitchBlock.Kind.HDD,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(6, 6)
                                .requiresCorrectToolForDrops()),
                bare -> new BlockItem(bare, new Item.Properties()));
    }
}
