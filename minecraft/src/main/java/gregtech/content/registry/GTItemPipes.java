package gregtech.content.registry;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.machine.GTMachineBlockItem;
import gregtech.content.block.machine.ItemPipeBlock;
import gregtech.engine.machine.ItemPipeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.Item;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.IEventBus;
*///?}

/** Native item pipe registrations populated by ItemPipeDefinitions from the shared catalog. */
public final class GTItemPipes {
    private static final List<RegisteredBlock<ItemPipeBlock>> ALL = new ArrayList<>();
    public static RegisteredBlock<ItemPipeBlock> PIPE_MEDIUM_BRASS;

    private GTItemPipes() {}

    public static RegisteredBlock<ItemPipeBlock> register(
            ContentRegistrar registrar,
            String id,
            GTMaterial material,
            ItemPipeSpec.ItemPipeSize size,
            long stepSize,
            int inventorySize,
            boolean blocking) {
        var spec = ItemPipeSpec.of(id, material, size, stepSize, inventorySize, blocking);
        var block = registrar.block(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                id,
                () -> new ItemPipeBlock(spec, ItemPipeBlock.defaultProperties(spec)),
                pipe -> new GTMachineBlockItem(pipe, new Item.Properties().stacksTo(stackSize(size))));
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

    public static List<RegisteredBlock<ItemPipeBlock>> all() {
        return Collections.unmodifiableList(ALL);
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
