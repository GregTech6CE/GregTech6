package gregtech.content.registry;
//? if >=1.21.1 {
/*import gregtech.internal.machine.ItemPipeSpec;
import gregtech.internal.material.GTMaterial;
import gregtech.content.block.machine.ItemPipeBlock;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
*///?} else {

import gregtech.internal.machine.ItemPipeSpec;
import gregtech.internal.material.GTMaterial;
import gregtech.content.block.machine.ItemPipeBlock;
import gregtech.content.block.machine.GTMachineBlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Native item pipe registrations populated by ItemPipeDefinitions from the shared catalog. */
//?}
public final class GTItemPipes {
//? if >=1.21.1 {
 /*private GTItemPipes() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("gt6ce");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("gt6ce");
    private static final java.util.List<DeferredBlock<ItemPipeBlock>> ALL = new java.util.ArrayList<>();
    public static DeferredBlock<ItemPipeBlock> PIPE_MEDIUM_BRASS;

    public static DeferredBlock<ItemPipeBlock> register(String id, GTMaterial material,
            ItemPipeSpec.ItemPipeSize size, long stepSize, int inventorySize, boolean blocking) {
        var spec = ItemPipeSpec.of(id, material, size, stepSize, inventorySize, blocking);
        var block = BLOCKS.register(id, () -> new ItemPipeBlock(spec, ItemPipeBlock.defaultProperties(spec)));
        ALL.add(block);
        ITEMS.register(id, () -> new gregtech.content.block.machine.ItemPipeBlockItem(
                block.get(), new Item.Properties().stacksTo(stackSize(size))));
        return block;
    }

    public static int stackSize(ItemPipeSpec.ItemPipeSize size) {
        return switch (size) {
            case MEDIUM, RESTRICTIVE_MEDIUM -> 64;
            case LARGE, RESTRICTIVE_LARGE -> 32;
            case HUGE, RESTRICTIVE_HUGE -> 16;
        };
    }

    public static java.util.List<DeferredBlock<ItemPipeBlock>> all() {
        return java.util.Collections.unmodifiableList(ALL);
    }

    public static void register(IEventBus bus) {
        if (!ALL.isEmpty()) throw new IllegalStateException("Item pipes registered twice");
        gregtech.content.transport.ItemPipeDefinitions.register();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(GTItemPipes::capabilities);
    }

    private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                GTBlockEntities.ITEM_PIPE.get(), (be, side) -> be.capabilityHandler(side));
    }
*///?} else {
    private static final List<RegistryObject<ItemPipeBlock>> ALL = new ArrayList<>();

    private GTItemPipes() {}

    public static RegistryObject<ItemPipeBlock> PIPE_MEDIUM_BRASS;

    public static RegistryObject<ItemPipeBlock> register(
            String id, GTMaterial material, ItemPipeSpec.ItemPipeSize size, long stepSize, int invSize, boolean blocking) {
        ItemPipeSpec spec = ItemPipeSpec.of(id, material, size, stepSize, invSize, blocking);
        RegistryObject<ItemPipeBlock> block = GTBlocks.BLOCKS.register(id,
                () -> new ItemPipeBlock(spec, ItemPipeBlock.defaultProperties(spec)));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(id,
                () -> new GTMachineBlockItem(block.get(),
                        new Item.Properties().stacksTo(stackSize(size)),
                        new gregtech.internal.machine.MachineSpec(
                                id, material.getLocalName(), material.getColor(), 0, 0,
                                gregtech.internal.machine.MachineTextures.BURNING_SOLID,
                                1.5F, spec.blastResistance())));
        return block;
    }

    /**
     * GT6's stack sizes for item pipes ({@code MultiTileEntityPipeItem.addItemPipes:77-82}):
     * medium 64, large 32, huge 16 — the restrictive variants follow their base size.
     */
    public static int stackSize(ItemPipeSpec.ItemPipeSize size) {
        return switch (size) {
            case MEDIUM, RESTRICTIVE_MEDIUM -> 64;
            case LARGE, RESTRICTIVE_LARGE -> 32;
            case HUGE, RESTRICTIVE_HUGE -> 16;
        };
    }

    public static List<RegistryObject<ItemPipeBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void bootstrap() {
        if (ALL.isEmpty()) throw new IllegalStateException("GTItemPipes failed to initialize");
    }
//?}
}
