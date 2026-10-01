package com.gregtech.gregtech.loaders.b;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.prefix.BlockPrefixRegistry;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.block.stone.GTStoneSlabBlock;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.item.FluidItem;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.registry.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Registers creative tabs and populates them — combined from Loader_Creative and CreativeTabHandler. */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Loader_Creative implements IGTLoader {
    private final IEventBus bus;

    public Loader_Creative(IEventBus bus) {
        this.bus = bus;
    }

    @Override
    public void run() {
        GTCreativeTabIcons.register();
        GTCreativeTabIcons.ICONS.register(bus);
        GTCreativeTabs.registerTabs();
        GTCreativeTabs.TABS.register(bus);
    }

    // ==================== Creative tab population ====================

    @SubscribeEvent
    public static void populateTabs(BuildCreativeModeTabContentsEvent event) {
        long started=System.nanoTime();
        try { populateContents(event); }
        finally {
            if (event.getTabKey().location().getNamespace().equals(GregTech.NAMESPACE)) {
                long elapsed=System.nanoTime()-started;
                com.gregtech.gregtech.client.CreativeBuildTimings.record(elapsed);
            }
        }
    }

    private static void populateContents(BuildCreativeModeTabContentsEvent event) {
        if (GTCreativeTabs.TOOLS_TAB != null && event.getTabKey().equals(GTCreativeTabs.TOOLS_TAB.getKey())) {
            for (var suit : com.gregtech.gregtech.registry.GTRadiationProtection.SUIT.values()) event.accept(suit.get());
            for (var entry : com.gregtech.gregtech.registry.GTToolBlocks.manual()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
            var crank = com.gregtech.gregtech.registry.GTToolBlocks.CRANK;
            if (crank != null && crank.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(crank.get()));
            for (var ro : new net.minecraftforge.registries.RegistryObject[]{
                    com.gregtech.gregtech.registry.GTToolBlocks.DUST_FUNNEL,
                    com.gregtech.gregtech.registry.GTToolBlocks.TAP,
                    com.gregtech.gregtech.registry.GTToolBlocks.NOZZLE,
                    com.gregtech.gregtech.registry.GTToolBlocks.ROPE,
                    com.gregtech.gregtech.registry.GTToolBlocks.DYNAMITE}) {
                if (ro != null && ro.isPresent()) {
                    event.accept(new net.minecraft.world.item.ItemStack(
                            (net.minecraft.world.level.block.Block) ro.get()));
                }
            }
            // Wave 43 tool blocks batch 3
            for (var entry : com.gregtech.gregtech.registry.GTToolBlocks.simpleBlocks()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get().asItem(), 1));
            }

            // Wave 45: automation/sorting/workbench/panels/transport
            for (var entry : com.gregtech.gregtech.registry.GTMiscBlocks.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(((net.minecraft.world.level.block.Block) entry.get()).asItem(), 1));
            }

            // Wave 46: tree/wood blocks
            for (var entry : com.gregtech.gregtech.registry.GTWoods.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(((net.minecraft.world.level.block.Block) entry.get()).asItem(), 1));
            }

            // Wave 47: batteries and electric tools
            for (var entry : com.gregtech.gregtech.registry.GTElectricItems.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get(), 1));
            }

            // Wave 48: decorative blocks + placeables
            for (var entry : com.gregtech.gregtech.registry.GTDecorBlocks.all()) {
                if (!entry.isPresent()) continue;
                var block = entry.get();
                if (block instanceof com.gregtech.gregtech.block.misc.ConcreteBlock) {
                    for (var color : net.minecraft.world.item.DyeColor.values())
                        event.accept(com.gregtech.gregtech.block.misc.ConcreteBlock.coloredItem(block, color));
                } else if (block instanceof com.gregtech.gregtech.block.misc.ColoredGlassBlock) {
                    for (var color : net.minecraft.world.item.DyeColor.values())
                        event.accept(com.gregtech.gregtech.block.misc.ColoredGlassBlock.coloredItem(block, color));
                } else {
                    event.accept(new net.minecraft.world.item.ItemStack(block.asItem(), 1));
                }
            }

            // Wave 50: lasers + quantum + logistics
            for (var entry : com.gregtech.gregtech.registry.GTLasers.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(((net.minecraft.world.level.block.Block) entry.get()).asItem(), 1));
            }

            event.accept(com.gregtech.gregtech.content.energy.ZpmEnergy.charged());
            populateTools(event);
            return;
        }
        for (MaterialPrefix prefix : PrefixRegistry.all()) {
            ResourceKey<CreativeModeTab> key = GTCreativeTabs.keyFor(prefix);
            if (key != null && event.getTabKey().equals(key)) {
                populatePrefix(event, prefix);
                return;
            }
        }
        for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
            ResourceKey<CreativeModeTab> key = GTCreativeTabs.keyFor(prefix);
            if (key != null && event.getTabKey().equals(key)) {
                populateBlockPrefix(event, prefix);
                return;
            }
        }
        ResourceKey<CreativeModeTab> stonesKey = GTCreativeTabs.stonesTabKey();
        if (stonesKey != null && event.getTabKey().equals(stonesKey)) {
            populateStones(event);
            return;
        }
        ResourceKey<CreativeModeTab> cruciblesKey = GTCreativeTabs.cruciblesTabKey();
        if (cruciblesKey != null && event.getTabKey().equals(cruciblesKey)) {
            populateCrucibles(event);
            return;
        }
        ResourceKey<CreativeModeTab> burningBoxesKey = GTCreativeTabs.burningBoxesTabKey();
        if (burningBoxesKey != null && event.getTabKey().equals(burningBoxesKey)) {
            populateBurningBoxes(event);
            return;
        }
        ResourceKey<CreativeModeTab> basicMachinesKey = GTCreativeTabs.basicMachinesTabKey();
        if (basicMachinesKey != null && event.getTabKey().equals(basicMachinesKey)) {
            populateBasicMachines(event);
            return;
        }
        ResourceKey<CreativeModeTab> fluidsKey = GTCreativeTabs.fluidsTabKey();
        if (fluidsKey != null && event.getTabKey().equals(fluidsKey)) {
            populateFluids(event);
        }
        ResourceKey<CreativeModeTab> pipesKey = GTCreativeTabs.pipesTabKey();
        if (pipesKey != null && event.getTabKey().equals(pipesKey)) {
            populatePipes(event);
        }
        ResourceKey<CreativeModeTab> itemPipesKey = GTCreativeTabs.itemPipesTabKey();
        if (itemPipesKey != null && event.getTabKey().equals(itemPipesKey)) {
            populateItemPipes(event);
        }
        ResourceKey<CreativeModeTab> fluidContainersKey = GTCreativeTabs.fluidContainersTabKey();
        if (fluidContainersKey != null && event.getTabKey().equals(fluidContainersKey)) {
            populateFluidContainers(event);
        }
        ResourceKey<CreativeModeTab> hoppersKey = GTCreativeTabs.hoppersTabKey();
        if (hoppersKey != null && event.getTabKey().equals(hoppersKey)) {
            populateHoppers(event);
            return;
        }
        ResourceKey<CreativeModeTab> enginesKey = GTCreativeTabs.enginesTabKey();
        if (enginesKey != null && event.getTabKey().equals(enginesKey)) {
            populateEngines(event);
            return;
        }
        ResourceKey<CreativeModeTab> wiresKey = GTCreativeTabs.wiresTabKey();
        if (wiresKey != null && event.getTabKey().equals(wiresKey)) {
            populateWires(event);
            return;
        }
        ResourceKey<CreativeModeTab> technologyKey = GTCreativeTabs.technologyTabKey();
        if (technologyKey != null && event.getTabKey().equals(technologyKey)) {
            populateTechnology(event);
            return;
        }
        ResourceKey<CreativeModeTab> iconSetsKey = GTCreativeTabs.iconSetsTabKey();
        if (iconSetsKey != null && event.getTabKey().equals(iconSetsKey)) {
            populateIconSets(event);
        }
        if (GTCreativeTabs.AXLES_TAB != null
                && event.getTabKey().equals(GTCreativeTabs.AXLES_TAB.getKey())) {
            for (var entry : com.gregtech.gregtech.registry.GTAxles.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
            for (var entry : com.gregtech.gregtech.registry.GTGearboxes.allGearboxes()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
            for (var entry : com.gregtech.gregtech.registry.GTGearboxes.allTransformers()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
            for (var entry : com.gregtech.gregtech.registry.GTPumps.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
            return;
        }
        if (GTCreativeTabs.STORAGE_TAB != null
                && event.getTabKey().equals(GTCreativeTabs.STORAGE_TAB.getKey())) {
            // GT6 metaitem-id order: Chests (0+), Safes (2000+), Drawers (4000+),
            // Mass Storage (6000+), Lockers (7300+), then the Ender Garbage pair
            for (var chest : com.gregtech.gregtech.registry.GTStorage.METAL_CHESTS) {
                if (chest.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(chest.get()));
            }
            for (var chest : com.gregtech.gregtech.registry.GTStorage.REINFORCED_WOOD_CHESTS) {
                if (chest.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(chest.get()));
            }
            var safe = com.gregtech.gregtech.registry.GTStorage.SAFE;
            java.util.stream.Stream.concat(com.gregtech.gregtech.registry.GTStorage.SAFES.stream(), com.gregtech.gregtech.registry.GTStorage.KEY_SAFES.stream())
                    .forEach(entry -> event.accept(new net.minecraft.world.item.ItemStack(entry.get())));
            com.gregtech.gregtech.registry.GTStorage.DRAWERS.forEach(entry -> event.accept(new net.minecraft.world.item.ItemStack(entry.get())));
            for (var storage : com.gregtech.gregtech.registry.GTStorage.MASS_STORAGES) {
                if (storage.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(storage.get()));
            }
            for (var storage : com.gregtech.gregtech.registry.GTStorage.LOGISTICS_MASS_STORAGES) {
                if (storage.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(storage.get()));
            }
            for (var ro : new net.minecraftforge.registries.RegistryObject[]{
                    com.gregtech.gregtech.registry.GTStorage.LOCKER,
                    com.gregtech.gregtech.registry.GTStorage.ENDER_GARBAGE,
                    com.gregtech.gregtech.registry.GTStorage.ENDER_GARBAGE_DUMP}) {
                if (ro != null && ro.isPresent()) {
                    event.accept(new net.minecraft.world.item.ItemStack(
                            (net.minecraft.world.level.block.Block) ro.get()));
                }
            }
        }
        if (GTCreativeTabs.STORAGE_TAB != null
                && event.getTabKey().equals(GTCreativeTabs.STORAGE_TAB.getKey())) {
            for (var entry : com.gregtech.gregtech.registry.GTSensors.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
        }
        if (GTCreativeTabs.ENERGY_NODES_TAB != null
                && event.getTabKey().equals(GTCreativeTabs.ENERGY_NODES_TAB.getKey())) {
            for (var entry : com.gregtech.gregtech.registry.GTEnergyNodes.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
            for(var entry:com.gregtech.gregtech.registry.GTChemicalBatteries.all()) {
                var empty=new ItemStack(entry.get());event.accept(empty);
                var charged=empty.copy();var battery=(com.gregtech.gregtech.item.ChemicalBatteryItem)charged.getItem();
                battery.setCharge(charged,battery.spec().capacity());event.accept(charged);
            }
            var reactor = com.gregtech.gregtech.registry.GTEnergyNodes.REACTOR_CORE_BLOCK;
            if (reactor != null && reactor.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(reactor.get()));
            var reactorCasing = com.gregtech.gregtech.registry.GTEnergyNodes.REACTOR_CASING;
            if (reactorCasing != null && reactorCasing.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(reactorCasing.get()));
            var reactorCore2x2 = com.gregtech.gregtech.registry.GTEnergyNodes.REACTOR_CORE_2X2;
            if (reactorCore2x2 != null && reactorCore2x2.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(reactorCore2x2.get()));
            // Fuel rods
            for (var entry : com.gregtech.gregtech.registry.GTFuelRods.ALL) {
                if (entry.isPresent() && entry.get().definition()!=null) event.accept(new ItemStack(entry.get(), 1));
            }
            // Magnets (N4)
            for (var entry : com.gregtech.gregtech.registry.GTMagnets.all()) {
                if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
            }
        }
        if (GTCreativeTabs.MULTIBLOCKS_TAB != null
                && event.getTabKey().equals(GTCreativeTabs.MULTIBLOCKS_TAB.getKey())) {
            var main = com.gregtech.gregtech.registry.GTMultiblocks.DISTILLATION_TOWER_MAIN;
            if (main != null && main.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(main.get()));
            var boiler = com.gregtech.gregtech.registry.GTMultiblocks.LARGE_BOILER_MAIN;
            if (boiler != null && boiler.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(boiler.get()));
            var turbine = com.gregtech.gregtech.registry.GTMultiblocks.LARGE_TURBINE_MAIN;
            if (turbine != null && turbine.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(turbine.get()));
            for (var ro : new net.minecraftforge.registries.RegistryObject[]{
                    com.gregtech.gregtech.registry.GTMultiblocks.COKE_OVEN_MAIN,
                    com.gregtech.gregtech.registry.GTMultiblocks.TANK_3X3,
                    com.gregtech.gregtech.registry.GTMultiblocks.TANK_5X5,
                    com.gregtech.gregtech.registry.GTMultiblocks.LARGE_CRUCIBLE_MAIN,
                    com.gregtech.gregtech.registry.GTMultiblocks.CRYO_DISTILLATION_MAIN,
                    com.gregtech.gregtech.registry.GTMultiblocks.LARGE_GAS_TURBINE_MAIN,
                    com.gregtech.gregtech.registry.GTMultiblocks.LARGE_DYNAMO_MAIN,
                    com.gregtech.gregtech.registry.GTMultiblocks.HEAT_EXCHANGER_MAIN,
                    com.gregtech.gregtech.registry.GTMultiblocks.BEDROCK_DRILL_MAIN,
                    com.gregtech.gregtech.registry.GTMultiblocks.LIGHTNING_ROD_MAIN,
                    com.gregtech.gregtech.registry.GTMultiblocks.IMPLOSION_COMPRESSOR_MAIN,
                    com.gregtech.gregtech.registry.GTMultiblocks.FUSION_REACTOR_MAIN}) {
                if (ro != null && ro.isPresent()) {
                    event.accept(new net.minecraft.world.item.ItemStack(
                            (net.minecraft.world.level.block.Block) ro.get()));
                }
            }
            for (var entry : com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.blocks().subList(1,4)) event.accept(entry.get());
            for(boolean steam:new boolean[]{true,false})for(var entry:com.gregtech.gregtech.content.multiblock.AxialGeneratorDefinitions.blocks(steam).subList(1,4))event.accept(entry.get());
            for (var entry : com.gregtech.gregtech.registry.GTMultiblocks.parts()) {
                if (entry != null && entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
        }
        if (GTCreativeTabs.MULTI_ITEMS_TAB != null
                && event.getTabKey().equals(GTCreativeTabs.MULTI_ITEMS_TAB.getKey())) {
            for (var entry : com.gregtech.gregtech.registry.GTMultiItems.all()) {
                if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void sanitizeStackCounts(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> invalid = new ArrayList<>();
        for (var entry : event.getEntries()) {
            if (entry.getKey().getCount() != 1) {
                invalid.add(entry.getKey());
            }
        }
        if (invalid.isEmpty()) return;

        GregTech.LOGGER.warn("Creative tab {} contained {} stacks with count != 1; normalizing",
                event.getTabKey().location(), invalid.size());

        for (ItemStack stack : invalid) {
            CreativeModeTab.TabVisibility visibility = event.getEntries().get(stack);
            if (visibility == null) continue;
            event.getEntries().remove(stack);
            event.getEntries().put(copySingle(stack), visibility);
        }
    }

    private static void populateTools(BuildCreativeModeTabContentsEvent event) {
        GTMaterial iron = com.gregtech.gregtech.content.material.generated.ElementMaterials.Iron;
        GTMaterial wood = com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood;
        GTMaterial diamond = com.gregtech.gregtech.content.material.generated.CompoundMaterials.Diamond;
        for (GTToolType type : GTToolType.values()) {
            if (type == GTToolType.GEM_PICK) {
                event.accept(GTToolItem.create(type, diamond, wood));
            } else {
                event.accept(GTToolItem.create(type, iron, type.requiresHeadAssembly() ? wood : iron));
            }
        }
    }

    private static void populateBlockPrefix(BuildCreativeModeTabContentsEvent event, BlockMaterialPrefix prefix) {
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            if (material.has(MaterialProperty.HIDDEN)) continue;
            if (material.resolve() != material) continue;
            if (!prefix.isValidFor(material)) continue;
            ItemStack stack = GTBlocks.getCreativeStack(prefix, material);
            if (!stack.isEmpty()) event.accept(stack);
        }
    }

    private static void populateStones(BuildCreativeModeTabContentsEvent event) {
        for (var entry : GTBlocks.allEntries()) {
            if (!entry.isPresent()) continue;
            if (entry.get() instanceof GTStoneBlock || entry.get() instanceof GTStoneSlabBlock) {
                event.accept(new ItemStack(entry.get().asItem(), 1));
            }
        }
    }

    private static void populateTechnology(BuildCreativeModeTabContentsEvent event) {
        for (var entry : GTTechnological.all()) {
            if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
        }
        for (var entry : new net.minecraftforge.registries.RegistryObject[]{GTStorage.USB_SWITCH, GTStorage.HDD_SWITCH}) {
            if (entry != null && entry.isPresent())
                event.accept(new ItemStack(((net.minecraft.world.level.block.Block) entry.get()).asItem(), 1));
        }
    }

    private static void populateIconSets(BuildCreativeModeTabContentsEvent event) {
        for (var entry : GTIconSetBlocks.all()) {
            if (entry.isPresent()) event.accept(new ItemStack(entry.get().asItem(), 1));
        }
    }

    private static void populateCrucibles(BuildCreativeModeTabContentsEvent event) {
        for (var registry : MachineRegistry.smelteryBlocks()) {
            for (var entry : registry) {
                if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
            }
        }
    }

    private static void populateBurningBoxes(BuildCreativeModeTabContentsEvent event) {
        for (var entry : MachineRegistry.solidBurningBoxes()) {
            if (!entry.isPresent()) continue;
            event.accept(new ItemStack(entry.get(), 1));
        }
        for (var entry : MachineRegistry.burningBoxes()) {
            if (!entry.isPresent()) continue;
            event.accept(new ItemStack(entry.get(), 1));
        }
    }

    private static void populateBasicMachines(BuildCreativeModeTabContentsEvent event) {
        for (var entry : MachineRegistry.basicMachines()) {
            if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
        }
    }

    private static void populateFluids(BuildCreativeModeTabContentsEvent event) {
        GTFluidItems.ITEMS.getEntries().stream()
                .filter(e -> e.isPresent() && e.get() instanceof FluidItem fluid && !fluid.fluidEntry().isHidden())
                .sorted(java.util.Comparator.comparing(e -> e.getId().getPath()))
                .forEach(e -> event.accept(new ItemStack(e.get(), 1)));
    }

    private static void populatePipes(BuildCreativeModeTabContentsEvent event) {
        for (var entry : GTFluidPipes.all()) {
            if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
        }
    }

    private static void populateItemPipes(BuildCreativeModeTabContentsEvent event) {
        for (var entry : GTItemPipes.all()) {
            if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
        }
    }

    private static void populateWires(BuildCreativeModeTabContentsEvent event) {
        for (var entry : GTSignalWires.all()) {
            if (entry.isPresent()) event.accept(new ItemStack(entry.get()));
        }
        for (var entry : GTWires.all()) {
            if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
        }
    }

    private static void populateHoppers(BuildCreativeModeTabContentsEvent event) {
        var hoppers = MachineRegistry.hoppers();
        var queueHoppers = MachineRegistry.queueHoppers();
        int count = Math.max(hoppers.size(), queueHoppers.size());
        for (int i = 0; i < count; i++) {
            if (i < hoppers.size()) {
                var entry = hoppers.get(i);
                if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
            }
            if (i < queueHoppers.size()) {
                var entry = queueHoppers.get(i);
                if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
            }
        }
    }

    private static void populateEngines(BuildCreativeModeTabContentsEvent event) {
        for (var entry : MachineRegistry.allEngines()) {
            if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
        }
        // Steam boiler tanks bridge the burning-box -> engine chain
        for (var entry : com.gregtech.gregtech.registry.GTBoilers.all()) {
            if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
        }
    }

    private static void populateFluidContainers(BuildCreativeModeTabContentsEvent event) {
        for (var entry : GTTanks.all()) {
            if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
        }
        // Wave 43 F11: fluid container items (jug, cup, etc.)
        for (var entry : com.gregtech.gregtech.registry.GTItems.ITEMS.getEntries()) {
            String path = entry.getId().getPath();
            if (path.startsWith("fluid_") && !path.equals("fluid_filter") && !path.equals("fluid_item")
                    && !path.equals("fluid_funnel") && !path.equals("fluid_spring")) {
                if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
            }
        }
    }

    private static void populatePrefix(BuildCreativeModeTabContentsEvent event, MaterialPrefix prefix) {
        for (var binding : GTItems.creativeEntries(prefix)) {
            GTMaterial material=binding.material();
            if (material.has(MaterialProperty.HIDDEN) || material.resolve()!=material || !prefix.isValidFor(material)) continue;
            if(binding.item().isPresent()) event.accept(new ItemStack(binding.item().get(),1));
        }
    }

    private static ItemStack copySingle(ItemStack stack) {
        return stack.isEmpty() ? stack : stack.copyWithCount(1);
    }
}
