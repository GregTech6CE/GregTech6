package gregtech.bootstrap.content;

import gregtech.GregTech;
import gregtech.bootstrap.InitializationStep;
import gregtech.content.block.GTWorldFluidBlock;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.item.FluidItem;
import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.GTFluidItems;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTGasFluid;
import gregtech.content.registry.RegisteredBlock;
import gregtech.engine.fluid.FluidPhysicalProperties;
import gregtech.engine.fluid.FluidRenderLayers;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers GT6 fluids and fluid container items.
 * Forge registration adapter; definitions are prepared by content.fluid.FluidDefinitions.
 */
public record FluidBootstrap() implements InitializationStep {
    /**
     * Fluids that also exist as world blocks. GT6's fluid springs ({@code WorldgenFluidSpring},
     * Loader_Worldgen:782-797) place oil and natural gas into the world, so those need a
     * {@link LiquidBlock}.
     */
    private static final Set<String> WORLD_FLUID_PATHS = FluidRenderLayers.worldFluidPaths();

    /** Both the block and the fluid reference each other, so both sides stay lazy. */
    private static ForgeFlowingFluid.Properties properties(
            RegistryObject<FluidType> type,
            String field,
            AtomicReference<RegisteredBlock<? extends LiquidBlock>> blockRef) {
        ForgeFlowingFluid.Properties properties = new ForgeFlowingFluid.Properties(
                type,
                () -> GTFluids.still(field).get(),
                () -> GTFluids.flowing(field).get());
        if (blockRef.get() != null) properties.block(() -> blockRef.get().get());
        return properties;
    }

    @Override
    public void run() {
        gregtech.content.fluid.FluidDefinitions.prepare();
        var registrar = ContentRegistrar.shared();

        java.util.Set<String> seenPaths = new java.util.HashSet<>();
        java.util.Set<String> seenItemPaths = new java.util.HashSet<>();
        int count = 0;
        for (var entry : RegisteredFluids.all().entrySet()) {
            String field = entry.getKey();
            RegisteredFluids.FluidEntry flEntry = entry.getValue();

            if (flEntry.usesVanillaFluid()) {
                String displayPath = RegisteredFluids.sanitizePath(flEntry.registryName());
                if (seenItemPaths.add("fluid_item_" + displayPath))
                    GTFluidItems.bindFluidItem(
                            field,
                            registrar.item(
                                    GregTech.NAMESPACE,
                                    "fluid_item_" + displayPath,
                                    () -> new FluidItem(flEntry, new Item.Properties())));
                continue;
            }

            String path = GTFluids.sanitizePath(flEntry.registryName());
            String itemPath = RegisteredFluids.sanitizePath(flEntry.registryName());
            String namespace = GTFluids.namespace(flEntry);

            if (!seenPaths.add(path)) {
                GregTech.LOGGER.warn("Skipping duplicate fluid path '{}' from field '{}'", path, field);
                continue;
            }
            if (!seenItemPaths.add("fluid_item_" + itemPath)) {
                GregTech.LOGGER.warn(
                        "Skipping duplicate fluid item path 'fluid_item_{}' from field '{}'", itemPath, field);
                continue;
            }

            // FluidType
            RegistryObject<FluidType> typeRO =
                    GTFluids.fluidTypes(namespace).register(path, () -> GTFluids.createFluidType(flEntry));

            // Negative density is the original's densityDir = -1: these fluids rise instead of
            // falling. Air itself is density 0, so nothing neutrally buoyant is included.
            boolean lighterThanAir = FluidPhysicalProperties.density(flEntry) < 0;

            // World block for the spring fluids (created first: the fluid references it).
            final AtomicReference<RegistryObject<Fluid>> stillRef = new AtomicReference<>();
            final AtomicReference<RegisteredBlock<? extends LiquidBlock>> blockRef = new AtomicReference<>();
            if (WORLD_FLUID_PATHS.contains(path)) {
                // The spring blocks use vanilla water's block properties: replaceable, no
                // collision, no loot and no random ticks. GTWorldFluidBlock is a LiquidBlock that
                // survives Forge's supplier constructor (the only constructor a mod can use here).
                blockRef.set(registrar.blockWithoutItem(
                        GregTech.NAMESPACE,
                        path,
                        () -> new GTWorldFluidBlock(
                                () -> (FlowingFluid) stillRef.get().get(),
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WATER)
                                        .replaceable()
                                        .noCollission()
                                        .strength(100.0F)
                                        .pushReaction(PushReaction.DESTROY)
                                        .noLootTable()
                                        .liquid()
                                        .sound(SoundType.EMPTY))));
            }

            // Still fluid
            RegistryObject<Fluid> stillRO = GTFluids.fluids(namespace)
                    .register(
                            path,
                            () -> lighterThanAir
                                    ? new GTGasFluid.Source(properties(typeRO, field, blockRef))
                                    : new ForgeFlowingFluid.Source(properties(typeRO, field, blockRef)));
            stillRef.set(stillRO);

            // Flowing fluid
            RegistryObject<Fluid> flowingRO = GTFluids.fluids(namespace)
                    .register(
                            path + "_flowing",
                            () -> lighterThanAir
                                    ? new GTGasFluid.Flowing(properties(typeRO, field, blockRef))
                                    : new ForgeFlowingFluid.Flowing(properties(typeRO, field, blockRef)));

            GTFluids.bindFluid(
                    field,
                    stillRO,
                    flowingRO,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, path),
                    flEntry);

            // Fluid item
            gregtech.content.registry.RegisteredItem<FluidItem> itemRO = registrar.item(
                    namespace, "fluid_item_" + itemPath, () -> new FluidItem(flEntry, new Item.Properties()));
            GTFluidItems.bindFluidItem(field, itemRO);

            count++;
        }

        GregTech.LOGGER.info("Registered {} GT6 fluids", count);
        RegisteredFluids.closeDefinitions();
    }
}
