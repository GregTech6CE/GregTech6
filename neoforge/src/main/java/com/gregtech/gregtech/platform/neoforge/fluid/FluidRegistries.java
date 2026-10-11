package com.gregtech.gregtech.platform.neoforge.fluid;

import com.gregtech.gregtech.api.fluid.FluidRenderLayers;
import com.gregtech.gregtech.api.fluid.GTWaterParity;
import com.gregtech.gregtech.content.fluid.FluidDefinitions;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTWorldWaterFluid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

/** Saltnya full fluid table and IDs with masson's Neo BaseFlowingFluid registration boundary. */
public final class FluidRegistries {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, "gregtech");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, "gregtech");
    private FluidRegistries() {}
    public static void register(IEventBus bus) {
        FluidDefinitions.prepare();
        var paths = new HashSet<String>();
        for (var definition : RegisteredFluids.all().entrySet()) {
            String field = definition.getKey(); var entry = definition.getValue();
            if (entry.usesVanillaFluid()) continue;
            String path = RegisteredFluids.sanitizePath(entry.registryName());
            if (!paths.add(path)) continue;
            var type = GTFluids.FLUID_TYPES.register(path, () -> GTFluids.createFluidType(entry));
            var stillRef = new AtomicReference<DeferredHolder<Fluid, ? extends Fluid>>();
            var blockRef = new AtomicReference<DeferredHolder<Block, LiquidBlock>>();
            boolean water = GTWaterParity.isWorldWater(path);
            if (FluidRenderLayers.worldFluidPaths().contains(path)) blockRef.set(BLOCKS.register(path, () -> new com.gregtech.gregtech.block.GTWorldFluidBlock(
                    (FlowingFluid) stillRef.get().get(), BlockBehaviour.Properties.of().mapColor(MapColor.WATER).replaceable().noCollission().strength(100).pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY))));
            var still = GTFluids.FLUIDS.register(path, () -> water ? new GTWorldWaterFluid.Source(properties(field, type, blockRef, true)) : new BaseFlowingFluid.Source(properties(field, type, blockRef, false)));
            stillRef.set(still);
            var flowing = GTFluids.FLUIDS.register(path + "_flowing", () -> water ? new GTWorldWaterFluid.Flowing(properties(field, type, blockRef, true)) : new BaseFlowingFluid.Flowing(properties(field, type, blockRef, false)));
            GTFluids.bindFluid(field, still, flowing, ResourceLocation.fromNamespaceAndPath("gregtech", path), entry);
            ITEMS.register("fluid_item_" + path, () -> new FluidDisplayItem(entry, new Item.Properties()));
        }
        RegisteredFluids.closeDefinitions();
        GTFluids.FLUID_TYPES.register(bus); GTFluids.FLUIDS.register(bus); BLOCKS.register(bus); ITEMS.register(bus);
    }
    private static BaseFlowingFluid.Properties properties(String field, DeferredHolder<FluidType, FluidType> type,
            AtomicReference<DeferredHolder<Block, LiquidBlock>> block, boolean water) {
        var properties = new BaseFlowingFluid.Properties(type, () -> GTFluids.still(field).get(), () -> GTFluids.flowing(field).get());
        if (block.get() != null) properties.block(() -> block.get().get());
        if (water) properties.explosionResistance(100);
        return properties;
    }
}
