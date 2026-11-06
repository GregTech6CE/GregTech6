package gregtech.platform.neoforge.fluid;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.data.RegisteredFluids;
import gregtech.content.fluid.FluidDefinitions;
import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTGasFluid;
import gregtech.content.registry.GTWorldWaterFluid;
import gregtech.content.registry.RegisteredBlock;
import gregtech.content.registry.RegisteredItem;
import gregtech.engine.fluid.FluidPhysicalProperties;
import gregtech.engine.fluid.FluidRenderLayers;
import gregtech.engine.fluid.GTWaterParity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Saltnya full fluid table and IDs with masson's Neo BaseFlowingFluid registration boundary. */
public final class FluidRegistries {
    private static final List<RegisteredItem<? extends Item>> ALL_ITEMS = new ArrayList<>();

    private FluidRegistries() {}

    public static java.util.stream.Stream<RegisteredItem<? extends Item>> allItems() {
        return ALL_ITEMS.stream();
    }

    public static void register(ContentRegistrar registrar) {
        FluidDefinitions.prepare();
        var paths = new HashSet<String>();
        for (var definition : RegisteredFluids.all().entrySet()) {
            String field = definition.getKey();
            var entry = definition.getValue();
            if (entry.usesVanillaFluid()) {
                String displayPath = RegisteredFluids.sanitizePath(entry.registryName());
                if (paths.add(displayPath))
                    ALL_ITEMS.add(registrar.item(
                            REGISTRY_NAMESPACE,
                            "fluid_item_" + displayPath,
                            () -> new FluidDisplayItem(entry, new Item.Properties())));
                continue;
            }
            String path = RegisteredFluids.sanitizePath(entry.registryName());
            if (!paths.add(path)) continue;
            String namespace = GTFluids.namespace(entry);
            var type = GTFluids.fluidTypes(namespace).register(path, () -> GTFluids.createFluidType(entry));
            var stillRef = new AtomicReference<DeferredHolder<Fluid, ? extends Fluid>>();
            var blockRef = new AtomicReference<RegisteredBlock<? extends LiquidBlock>>();
            boolean water = GTWaterParity.isWorldWater(path);
            // Negative density is the original's densityDir = -1: these fluids rise instead of
            // falling. Air itself is density 0, so nothing neutrally buoyant is included.
            boolean lighterThanAir = !water && FluidPhysicalProperties.density(entry) < 0;
            if (FluidRenderLayers.worldFluidPaths().contains(path)) {
                var holder = registrar.blockWithoutItem(
                        REGISTRY_NAMESPACE,
                        path,
                        () -> new gregtech.content.block.GTWorldFluidBlock(
                                (FlowingFluid) stillRef.get().get(),
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WATER)
                                        .replaceable()
                                        .noCollission()
                                        .strength(100)
                                        .pushReaction(PushReaction.DESTROY)
                                        .noLootTable()
                                        .liquid()
                                        .sound(SoundType.EMPTY)));
                blockRef.set(holder);
            }
            var still = GTFluids.fluids(namespace)
                    .register(
                            path,
                            () -> water
                                    ? new GTWorldWaterFluid.Source(properties(field, type, blockRef, true))
                                    : lighterThanAir
                                            ? new GTGasFluid.Source(properties(field, type, blockRef, false))
                                            : new BaseFlowingFluid.Source(properties(field, type, blockRef, false)));
            stillRef.set(still);
            var flowing = GTFluids.fluids(namespace)
                    .register(
                            path + "_flowing",
                            () -> water
                                    ? new GTWorldWaterFluid.Flowing(properties(field, type, blockRef, true))
                                    : lighterThanAir
                                            ? new GTGasFluid.Flowing(properties(field, type, blockRef, false))
                                            : new BaseFlowingFluid.Flowing(properties(field, type, blockRef, false)));
            GTFluids.bindFluid(field, still, flowing, ResourceLocation.fromNamespaceAndPath(namespace, path), entry);
            ALL_ITEMS.add(registrar.item(
                    namespace, "fluid_item_" + path, () -> new FluidDisplayItem(entry, new Item.Properties())));
        }
        RegisteredFluids.closeDefinitions();
    }

    public static void submit(IEventBus bus) {
        GTFluids.FLUID_TYPES.register(bus);
        GTFluids.FLUIDS.register(bus);
        GTFluids.registerAddonFluids(bus);
    }

    private static BaseFlowingFluid.Properties properties(
            String field,
            DeferredHolder<FluidType, FluidType> type,
            AtomicReference<RegisteredBlock<? extends LiquidBlock>> block,
            boolean water) {
        var properties = new BaseFlowingFluid.Properties(
                type,
                () -> GTFluids.still(field).get(),
                () -> GTFluids.flowing(field).get());
        if (block.get() != null) properties.block(() -> block.get().get());
        if (water) properties.explosionResistance(100);
        return properties;
    }
}
