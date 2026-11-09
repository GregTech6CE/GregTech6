package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.tool.DynamiteBlock;
import gregtech.content.block.tool.FluidAttachmentBlock;
import gregtech.content.block.tool.PortableContainerBlock;
import gregtech.content.item.PortableFluidContainerItem;
import gregtech.content.material.Materials;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.IEventBus;
*///?}

/** Mining charges, portable containers and fluid attachments; manual stations live in {@link GTManualStations}. */
public final class GTToolBlocks {
    public static final List<RegisteredBlock<PortableContainerBlock>> PORTABLE = new ArrayList<>();
    public static RegisteredBlock<DynamiteBlock> DYNAMITE, BOOMSTICK, DYNAMITE_STRONG;

    private GTToolBlocks() {}

    private static RegisteredBlock<DynamiteBlock> dynamite(
            ContentRegistrar registrar, String id, float resistance, int fortune, GTMaterial material) {
        return registrar.block(
                REGISTRY_NAMESPACE,
                id,
                () -> new DynamiteBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.COLOR_RED)
                                .strength(.2f, 0)
                                .sound(SoundType.GRASS)
                                .noOcclusion()
                                .instabreak(),
                        resistance,
                        fortune,
                        material.getColor()),
                block -> new BlockItem(block, new Item.Properties().stacksTo(64)));
    }

    public static List<RegisteredBlock<DynamiteBlock>> explosives() {
        return List.of(DYNAMITE, BOOMSTICK, DYNAMITE_STRONG);
    }

    private static BlockBehaviour.Properties attachmentProperties(String id) {
        var p = BlockBehaviour.Properties.of().noOcclusion();
        if (java.util.Set.of("tap", "nozzle", "cap_nozzle").contains(id))
            return p.mapColor(MapColor.METAL).strength(1.5f, 6).sound(SoundType.METAL);
        if (id.equals("fluid_funnel"))
            return p.mapColor(MapColor.METAL).strength(2, 6).sound(SoundType.METAL).requiresCorrectToolForDrops();
        return p.strength(.5f, 6);
    }

    public static void registerAll(ContentRegistrar registrar) {
        DYNAMITE = dynamite(registrar, "dynamite", 10, 5, Materials.Red);
        BOOMSTICK = dynamite(registrar, "boomstick", 10, 3, Materials.Orange);
        DYNAMITE_STRONG = dynamite(registrar, "strong_dynamite", 40, 5, Materials.Purple);
        for (var spec : gregtech.engine.fluid.PortableFluidContainerSpec.values()) {
            var block = registrar.blockWithoutItem(
                    REGISTRY_NAMESPACE,
                    "fluid_" + spec.id(),
                    () -> new PortableContainerBlock(
                            spec, BlockBehaviour.Properties.of().strength(1f).noOcclusion()));
            PORTABLE.add(block);
            registrar.item(
                    REGISTRY_NAMESPACE,
                    "fluid_" + spec.id(),
                    () -> new PortableFluidContainerItem(
                            block.get(),
                            spec,
                            new Item.Properties().stacksTo(spec.shapeId().equals("cell") ? 64 : 16)));
        }
        var attachments = new ArrayList<>(gregtech.content.tool.FluidAttachmentSpec.all());
        for (String id : List.of("tap", "nozzle", "fluid_funnel", "cap_nozzle"))
            attachments.add(new gregtech.content.tool.FluidAttachmentSpec(
                    id,
                    id,
                    java.util.Set.of("cap_nozzle", "nozzle").contains(id) ? Materials.Steel : Materials.Ceramic,
                    false));
        for (var spec : attachments) {
            registrar.block(
                    REGISTRY_NAMESPACE,
                    spec.id(),
                    () -> new FluidAttachmentBlock(spec, attachmentProperties(spec.id())),
                    block -> new BlockItem(block, new Item.Properties().stacksTo(64)));
        }
    }

//? if >=1.21.1 {
    /*public static void submit(IEventBus bus) {
        GTFluidComponents.TYPES.register(bus);
    }
*///?}
}
