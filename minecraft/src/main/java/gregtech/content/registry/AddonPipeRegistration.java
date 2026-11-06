package gregtech.content.registry;

import gregtech.engine.machine.ItemPipeSpec;
import gregtech.engine.machine.PipeSpec;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.block.machine.ItemPipeBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Addon pipe blocks. They use the addon's namespace and the built-in pipe block-entity types. */
public final class AddonPipeRegistration {
    private AddonPipeRegistration() {}
    private static final Set<String> NAMESPACES = new LinkedHashSet<>();
    private static final List<Supplier<FluidPipeBlock>> FLUID = new ArrayList<>();
    private static final List<Supplier<ItemPipeBlock>> ITEM_PIPES = new ArrayList<>();

    public static void queueFluid(ContentRegistrar registrar, String namespace, PipeSpec spec) {
        NAMESPACES.add(namespace);
        var holder = registrar.block(
                namespace, spec.id(), () -> new FluidPipeBlock(spec, FluidPipeBlock.defaultProperties(spec)),
                block -> fluidItem(block, spec));
        FLUID.add(holder::get);
    }

    public static void queueItem(ContentRegistrar registrar, String namespace, String id, GTMaterial material,
                                 ItemPipeSpec.ItemPipeSize size, long stepSize, int invSize, boolean blocking) {
        var spec = ItemPipeSpec.of(id, material, size, stepSize, invSize, blocking);
        NAMESPACES.add(namespace);
        var holder = registrar.block(
                namespace, id, () -> new ItemPipeBlock(spec, ItemPipeBlock.defaultProperties(spec)),
                block -> itemItem(block, spec));
        ITEM_PIPES.add(holder::get);
    }

    public static List<FluidPipeBlock> fluidBlocks() {
        var blocks = new ArrayList<FluidPipeBlock>();
        for (var block : FLUID) blocks.add(block.get());
        return blocks;
    }

    public static List<ItemPipeBlock> itemBlocks() {
        var blocks = new ArrayList<ItemPipeBlock>();
        for (var block : ITEM_PIPES) blocks.add(block.get());
        return blocks;
    }

    public static Block[] fluidPipeBlocks(java.util.stream.Stream<FluidPipeBlock> builtIn) {
        var blocks = new ArrayList<Block>();
        builtIn.forEach(blocks::add);
        blocks.addAll(fluidBlocks());
        return blocks.toArray(Block[]::new);
    }

    public static Block[] itemPipeBlocks(java.util.stream.Stream<ItemPipeBlock> builtIn) {
        var blocks = new ArrayList<Block>();
        builtIn.forEach(blocks::add);
        blocks.addAll(itemBlocks());
        return blocks.toArray(Block[]::new);
    }

    public static Set<String> namespaces() { return Set.copyOf(NAMESPACES); }

//? if >=1.21.1 {
    /*private static Item fluidItem(FluidPipeBlock block, PipeSpec spec) {
        return new gregtech.content.transport.fluid.FluidTransportBlockItem(block,
                new Item.Properties().stacksTo(fluidStack(spec.size())));
    }
    private static Item itemItem(ItemPipeBlock block, ItemPipeSpec spec) {
        return new gregtech.content.block.machine.ItemPipeBlockItem(block,
                new Item.Properties().stacksTo(GTItemPipes.stackSize(spec.size())));
    }
*///?} else {
    private static Item fluidItem(FluidPipeBlock block, PipeSpec spec) {
        var material = spec.material();
        return new gregtech.content.block.machine.GTMachineBlockItem(block,
                new Item.Properties().stacksTo(fluidStack(spec.size())),
                new gregtech.engine.machine.MachineSpec(spec.id(), material.getLocalName(), material.getColor(),
                        0, 0, gregtech.engine.machine.MachineTextures.BURNING_SOLID, 4.0F, 3.0F));
    }
    private static Item itemItem(ItemPipeBlock block, ItemPipeSpec spec) {
        var material = spec.material();
        return new gregtech.content.block.machine.GTMachineBlockItem(block,
                new Item.Properties().stacksTo(GTItemPipes.stackSize(spec.size())),
                new gregtech.engine.machine.MachineSpec(spec.id(), material.getLocalName(), material.getColor(),
                        0, 0, gregtech.engine.machine.MachineTextures.BURNING_SOLID, 1.5F, spec.blastResistance()));
    }
//?}

    private static int fluidStack(PipeSpec.PipeSize size) {
        return switch (size) {
            case TINY, SMALL -> 64;
            case MEDIUM -> 32;
            default -> 16;
        };
    }
}
