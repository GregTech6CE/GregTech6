package gregtech.content.registry;

//? if >=1.21.1 {
/*import gregtech.engine.machine.ItemPipeSpec;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.machine.ItemPipeBlock;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
*///?} else {

import gregtech.engine.machine.ItemPipeSpec;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.machine.ItemPipeBlock;
import gregtech.content.block.machine.GTMachineBlockItem;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Native item pipe registrations populated by ItemPipeDefinitions from the shared catalog. */
//?}
public final class GTItemPipes {
    private static final java.util.List<RegisteredBlock<ItemPipeBlock>> ALL = new java.util.ArrayList<>();
    public static RegisteredBlock<ItemPipeBlock> PIPE_MEDIUM_BRASS;

    private GTItemPipes() {}

    public static RegisteredBlock<ItemPipeBlock> register(ContentRegistrar registrar, String id, GTMaterial material,
            ItemPipeSpec.ItemPipeSize size, long stepSize, int inventorySize, boolean blocking) {
        var spec = ItemPipeSpec.of(id, material, size, stepSize, inventorySize, blocking);
        var block = registrar.block(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                id,
                () -> new ItemPipeBlock(spec, ItemPipeBlock.defaultProperties(spec)),
                pipe -> {
//? if >=1.21.1 {
                    /*return new gregtech.content.block.machine.ItemPipeBlockItem(
                            pipe, new Item.Properties().stacksTo(stackSize(size)));
*///?} else {
                    return new GTMachineBlockItem(pipe,
                            new Item.Properties().stacksTo(stackSize(size)),
                            new gregtech.engine.machine.MachineSpec(
                                    id, material.getLocalName(), material.getColor(), 0, 0,
                                    gregtech.engine.machine.MachineTextures.BURNING_SOLID,
                                    1.5F, spec.blastResistance()));
//?}
                });
        ALL.add(block);
        return block;
    }

    public static int stackSize(ItemPipeSpec.ItemPipeSize size) {
        return switch (size) {
            case MEDIUM, RESTRICTIVE_MEDIUM -> 64;
            case LARGE, RESTRICTIVE_LARGE -> 32;
            case HUGE, RESTRICTIVE_HUGE -> 16;
        };
    }

    public static java.util.List<RegisteredBlock<ItemPipeBlock>> all() {
        return java.util.Collections.unmodifiableList(ALL);
    }

//? if >=1.21.1 {
    /*public static void submit(IEventBus bus) {
        bus.addListener(GTItemPipes::capabilities);
    }

    private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                GTBlockEntities.ITEM_PIPE.get(), (be, side) -> be.capabilityHandler(side));
    }
*///?}
}
