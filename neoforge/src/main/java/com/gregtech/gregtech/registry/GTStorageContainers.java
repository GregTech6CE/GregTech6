package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.block.inventory.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockBehaviour;import net.minecraft.world.level.material.MapColor;import net.neoforged.neoforge.registries.DeferredHolder;
public final class GTStorageContainers {private GTStorageContainers(){}public static final java.util.List<DeferredHolder<Block,DrawerQuadBlock>> DRAWERS=new java.util.ArrayList<>();public static DeferredHolder<Block,BottleCrateBlock> BOTTLE_CRATE;public static DeferredHolder<Block,LockerBlock> LOCKER;public static DeferredHolder<Block,UsbSwitchBlock> USB_SWITCH,HDD_SWITCH;public static DeferredHolder<Block,EnderGarbageBlock> ENDER_GARBAGE;public static DeferredHolder<Block,EnderGarbageDumpBlock> ENDER_GARBAGE_DUMP;

    public static final java.util.List<net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.block.Block,com.gregtech.gregtech.block.inventory.BottleCrateBlock>> BOTTLE_CRATES = new java.util.ArrayList<>();
 public static void initialize(){for(var spec:GTStorageMetals.ALL){String id=spec.suffix().equals("stainless_steel")?"drawer_quad":"drawer_quad_"+spec.suffix();var b=GTBlocks.BLOCKS.register(id,()->new DrawerQuadBlock(spec.material(),BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness(),spec.resistance()).requiresCorrectToolForDrops()));DRAWERS.add(b);GTBlocks.BLOCK_ITEMS.register(id,()->new net.minecraft.world.item.BlockItem(b.get(),new net.minecraft.world.item.Item.Properties().stacksTo(16)));}
 BOTTLE_CRATE=GTBlocks.BLOCKS.register("bottle_crate",()->new BottleCrateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).noOcclusion().strength(.5f,2f).sound(SoundType.WOOD)));GTBlocks.BLOCK_ITEMS.register("bottle_crate",()->new net.minecraft.world.item.BlockItem(BOTTLE_CRATE.get(),new net.minecraft.world.item.Item.Properties().stacksTo(16)));

        BOTTLE_CRATES.add(BOTTLE_CRATE);
        for (var spec : com.gregtech.gregtech.content.storage.BottleCrateVariants.WOODS) {
            var crate = GTBlocks.BLOCKS.register(spec.id(), () -> new com.gregtech.gregtech.block.inventory.BottleCrateBlock(
                    null, spec.texture(), BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).noOcclusion()
                            .strength(.5f, 2f).sound(net.minecraft.world.level.block.SoundType.WOOD)));
            GTBlocks.BLOCK_ITEMS.register(spec.id(), () -> new net.minecraft.world.item.BlockItem(crate.get(), new net.minecraft.world.item.Item.Properties().stacksTo(16)));
            BOTTLE_CRATES.add(crate);
        }
        for (var spec : GTStorageMetals.ALL) {
            String id = "bottle_crate_" + spec.suffix();
            var crate = GTBlocks.BLOCKS.register(id, () -> new com.gregtech.gregtech.block.inventory.BottleCrateBlock(
                    spec.material(), null, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).noOcclusion()
                            .strength(.5f, spec.resistance()).sound(net.minecraft.world.level.block.SoundType.METAL).requiresCorrectToolForDrops()));
            GTBlocks.BLOCK_ITEMS.register(id, () -> new net.minecraft.world.item.BlockItem(crate.get(), new net.minecraft.world.item.Item.Properties().stacksTo(16)));
            BOTTLE_CRATES.add(crate);
        }
 LOCKER=GTBlocks.BLOCKS.register("locker",()->new LockerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3,3).requiresCorrectToolForDrops()));GTBlocks.BLOCK_ITEMS.register("locker",()->new net.minecraft.world.item.BlockItem(LOCKER.get(),new net.minecraft.world.item.Item.Properties()));
 ENDER_GARBAGE=GTBlocks.BLOCKS.register("ender_garbage_bin",()->new EnderGarbageBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3,3).requiresCorrectToolForDrops().noOcclusion()));GTBlocks.BLOCK_ITEMS.register("ender_garbage_bin",()->new net.minecraft.world.item.BlockItem(ENDER_GARBAGE.get(),new net.minecraft.world.item.Item.Properties()));
 ENDER_GARBAGE_DUMP=GTBlocks.BLOCKS.register("ender_garbage_dump",()->new EnderGarbageDumpBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(8,1200).requiresCorrectToolForDrops()));GTBlocks.BLOCK_ITEMS.register("ender_garbage_dump",()->new net.minecraft.world.item.BlockItem(ENDER_GARBAGE_DUMP.get(),new net.minecraft.world.item.Item.Properties()));
 USB_SWITCH=GTBlocks.BLOCKS.register("usb_switch",()->new UsbSwitchBlock(UsbSwitchBlock.Kind.USB,BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6,6).requiresCorrectToolForDrops()));GTBlocks.BLOCK_ITEMS.register("usb_switch",()->new net.minecraft.world.item.BlockItem(USB_SWITCH.get(),new net.minecraft.world.item.Item.Properties()));
 HDD_SWITCH=GTBlocks.BLOCKS.register("hdd_switch",()->new UsbSwitchBlock(UsbSwitchBlock.Kind.HDD,BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6,6).requiresCorrectToolForDrops()));GTBlocks.BLOCK_ITEMS.register("hdd_switch",()->new net.minecraft.world.item.BlockItem(HDD_SWITCH.get(),new net.minecraft.world.item.Item.Properties()));
 }
}
