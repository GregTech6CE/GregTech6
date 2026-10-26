package gregtech.bootstrap.content;

import gregtech.GregTech;
import gregtech.engine.fluid.FluidPhysicalProperties;
import gregtech.engine.fluid.FluidRenderLayers;
import gregtech.content.block.GTWorldFluidBlock;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.item.FluidItem;
import gregtech.bootstrap.InitializationStep;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTGasFluid;
import gregtech.content.registry.GTWorldWaterFluid;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTFluidItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers GT6 fluids and fluid container items.
 * Forge registration adapter; definitions are prepared by content.fluid.FluidDefinitions.
 */
public record FluidBootstrap(IEventBus bus) implements InitializationStep {
    /**
     * Fluids that also exist as world blocks. GT6's fluid springs ({@code WorldgenFluidSpring},
     * Loader_Worldgen:782-797) place oil, natural gas and geothermal water into the world, and
     * GT6's ocean/river/swamp passes ({@code WorldgenOcean} / {@code WorldgenRiver} /
     * {@code WorldgenSwamp}, Loader_Worldgen:576-578) replace the water of oceans, rivers and
     * swamps with GT6's own water fluids — all of those need a {@link LiquidBlock}.
     */
    private static final Set<String> WORLD_FLUID_PATHS = FluidRenderLayers.worldFluidPaths();

    /** Both the block and the fluid reference each other, so both sides stay lazy. */
    private static ForgeFlowingFluid.Properties properties(RegistryObject<FluidType> type, String field,
                                                          AtomicReference<RegistryObject<LiquidBlock>> blockRef,
                                                          boolean worldWater) {
        ForgeFlowingFluid.Properties properties = new ForgeFlowingFluid.Properties(type,
                () -> GTFluids.still(field).get(), () -> GTFluids.flowing(field).get());
        if (blockRef.get() != null) {
            properties.block(() -> blockRef.get().get());
            // Vanilla water: WaterFluid.getExplosionResistance() is 100.0F, matching its block's
            // strength(100.0F). ForgeFlowingFluid.Properties defaults it to 1.0F. Only the three
            // world waters are changed, so the oil/gas/geothermal springs keep their old value.
            if (worldWater) properties.explosionResistance(100.0F);
        }
        return properties;
    }

    @Override
    public void run() {
        gregtech.content.fluid.FluidDefinitions.prepare();

        java.util.Set<String> seenPaths = new java.util.HashSet<>();
        java.util.Set<String> seenItemPaths = new java.util.HashSet<>();
        int count = 0;
        for (var entry : RegisteredFluids.all().entrySet()) {
            String field = entry.getKey();
            RegisteredFluids.FluidEntry flEntry = entry.getValue();

            if (flEntry.usesVanillaFluid()) {
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
                GregTech.LOGGER.warn("Skipping duplicate fluid item path 'fluid_item_{}' from field '{}'", itemPath, field);
                continue;
            }

            // FluidType
            RegistryObject<FluidType> typeRO = GTFluids.fluidTypes(namespace).register(path,
                    () -> GTFluids.createFluidType(flEntry));

            // GT6's sea/river/swamp water copy minecraft:water completely, so they get vanilla
            // water's block properties, explosion resistance and WaterFluid flow behaviour.
            boolean worldWater = gregtech.engine.fluid.GTWaterParity.isWorldWater(path);
            // Negative density is the original's densityDir = -1: these fluids rise instead of
            // falling. Air itself is density 0, so nothing neutrally buoyant is included.
            boolean lighterThanAir = !worldWater && FluidPhysicalProperties.density(flEntry) < 0;

            // World block for the spring fluids (created first: the fluid references it).
            final AtomicReference<RegistryObject<Fluid>> stillRef = new AtomicReference<>();
            final AtomicReference<RegistryObject<LiquidBlock>> blockRef = new AtomicReference<>();
            if (WORLD_FLUID_PATHS.contains(path)) {
                // Exactly Blocks.WATER in 1.20.1 - including noLootTable() and the absence of
                // randomTicks() - so the world water blocks drop nothing and never random tick.
                // GTWorldFluidBlock is a LiquidBlock that survives Forge's supplier constructor
                // (it is the only constructor a mod can use here), see its javadoc.
                blockRef.set(GTBlocks.BLOCKS.register(path, () -> new GTWorldFluidBlock(
                        () -> (FlowingFluid) stillRef.get().get(),
                        BlockBehaviour.Properties.of().mapColor(MapColor.WATER).replaceable()
                                .noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY)
                                .noLootTable().liquid().sound(SoundType.EMPTY))));
            }

            // Still fluid
            RegistryObject<Fluid> stillRO = GTFluids.fluids(namespace).register(path, () -> worldWater
                    ? new GTWorldWaterFluid.Source(properties(typeRO, field, blockRef, true))
                    : lighterThanAir ? new GTGasFluid.Source(properties(typeRO, field, blockRef, false))
                    : new ForgeFlowingFluid.Source(properties(typeRO, field, blockRef, false)));
            stillRef.set(stillRO);

            // Flowing fluid
            RegistryObject<Fluid> flowingRO = GTFluids.fluids(namespace).register(path + "_flowing", () -> worldWater
                    ? new GTWorldWaterFluid.Flowing(properties(typeRO, field, blockRef, true))
                    : lighterThanAir ? new GTGasFluid.Flowing(properties(typeRO, field, blockRef, false))
                    : new ForgeFlowingFluid.Flowing(properties(typeRO, field, blockRef, false)));

            GTFluids.bindFluid(field, stillRO, flowingRO,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, path), flEntry);

            // Fluid item
            RegistryObject<FluidItem> itemRO = GTFluidItems.items(namespace).register("fluid_item_" + itemPath,
                    () -> new FluidItem(flEntry, new Item.Properties()));
            GTFluidItems.bindFluidItem(field, itemRO);

            count++;
        }

        GregTech.LOGGER.info("Registered {} GT6 fluids", count);
        RegisteredFluids.closeDefinitions();

        GTFluids.FLUID_TYPES.register(bus);
        GTFluids.FLUIDS.register(bus);
        GTFluids.registerAddonFluids(bus);
        GTFluidItems.ITEMS.register(bus);
        GTFluidItems.registerAddonItems(bus);
    }

}
