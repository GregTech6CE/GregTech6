package gregtech.bootstrap.registration;
//? if >=1.21.1 {
/*import gregtech.api.material.*;
import gregtech.internal.material.*;import gregtech.internal.prefix.*;import gregtech.data.MaterialPrefix;import gregtech.registry.*;import net.minecraft.world.item.*;import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class CreativeTabBootstrap {private CreativeTabBootstrap(){}
public static void register(net.neoforged.bus.api.IEventBus bus){GTCreativeTabIcons.register();GTCreativeTabIcons.ICONS.register(bus);GTCreativeTabs.registerTabs();GTCreativeTabs.TABS.register(bus);}
@SubscribeEvent public static void populateTabs(BuildCreativeModeTabContentsEvent event){if(!event.getTabKey().location().getNamespace().equals("gt6ce"))return;
 for(var prefix:PrefixRegistry.all()){var key=GTCreativeTabs.keyFor(prefix);if(key!=null&&key.equals(event.getTabKey())){populatePrefix(event,prefix);return;}}
 for(var prefix:BlockPrefixRegistry.all()){var key=GTCreativeTabs.keyFor(prefix);if(key!=null&&key.equals(event.getTabKey())){populateBlockPrefix(event,prefix);return;}}
 for(var stack:OriginCreativeContents.contents(event.getTabKey().location().getPath()))event.accept(stack.copyWithCount(1));
}
    private static void populatePrefix(BuildCreativeModeTabContentsEvent event, MaterialPrefix prefix) {
        for (var binding : GTItems.creativeEntries(prefix)) {
            GTMaterial material=binding.material();
            if (material.has(MaterialProperty.HIDDEN) || material.resolve()!=material
                    || (!prefix.isValidFor(material) && !gregtech.internal.addon.AddonMaterials.explicitPlateOrFoil(prefix, material))) continue;
            if(binding.item().isBound()) event.accept(new ItemStack(binding.item().get(),1));
*///?} else {

import gregtech.content.material.Materials;
import gregtech.GregTech;
import gregtech.internal.machine.MachineRegistry;
import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.material.MaterialProperty;
import gregtech.internal.prefix.BlockMaterialPrefix;
import gregtech.internal.prefix.BlockPrefixRegistry;
import gregtech.internal.tool.GTToolType;
import gregtech.block.stone.GTStoneBlock;
import gregtech.block.stone.GTStoneSlabBlock;
import gregtech.data.MaterialPrefix;
import gregtech.internal.prefix.PrefixRegistry;
import gregtech.data.generated.GT6Materials;
import gregtech.item.GTToolItem;
import gregtech.bootstrap.InitializationStep;
import gregtech.registry.*;
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
public class CreativeTabBootstrap implements InitializationStep {
    private final IEventBus bus;

    public CreativeTabBootstrap(IEventBus bus) {
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
                gregtech.client.CreativeBuildTimings.record(elapsed);
            }
//?}
        }
    }
//? if >=1.21.1 {
//?} else {

    private static void populateContents(BuildCreativeModeTabContentsEvent event) {
        String family=event.getTabKey().location().getPath();
        if(gregtech.content.creative.CreativeTabCatalog.FAMILIES.contains(family)) {
            for(var stack:OriginCreativeContents.contents(family))event.accept(stack.copyWithCount(1));
            return;
        }

        if (GTCreativeTabs.TOOLS_TAB != null && event.getTabKey().equals(GTCreativeTabs.TOOLS_TAB.getKey())) {
            for (var suit : gregtech.registry.GTRadiationProtection.ALL.values()) event.accept(suit.get());
            for (var entry : gregtech.registry.GTToolBlocks.manual()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
            var crank = gregtech.registry.GTToolBlocks.CRANK;
            if (crank != null && crank.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(crank.get()));
            for (var ro : new net.minecraftforge.registries.RegistryObject[]{
                    gregtech.registry.GTToolBlocks.DUST_FUNNEL,
                    gregtech.registry.GTToolBlocks.TAP,
                    gregtech.registry.GTToolBlocks.NOZZLE,
                    gregtech.registry.GTToolBlocks.ROPE,
                    gregtech.registry.GTToolBlocks.DYNAMITE}) {
                if (ro != null && ro.isPresent()) {
                    event.accept(new net.minecraft.world.item.ItemStack(
                            (net.minecraft.world.level.block.Block) ro.get()));
                }
            }
            // Wave 43 tool blocks batch 3
            for (var entry : gregtech.registry.GTToolBlocks.simpleBlocks()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get().asItem(), 1));
            }

            // Wave 45: automation/sorting/workbench/panels/transport
            for (var entry : gregtech.registry.GTMiscBlocks.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(((net.minecraft.world.level.block.Block) entry.get()).asItem(), 1));
            }

            // Wave 46: tree/wood blocks
            for (var entry : gregtech.registry.GTWoods.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(((net.minecraft.world.level.block.Block) entry.get()).asItem(), 1));
            }

            // Wave 47: batteries and electric tools
            for (var entry : gregtech.registry.GTElectricItems.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get(), 1));
            }

            // Wave 48: decorative blocks + placeables
            for (var entry : gregtech.registry.GTDecorBlocks.all()) {
                if (!entry.isPresent()) continue;
                var block = entry.get();
                if (block instanceof gregtech.block.misc.ConcreteBlock
                        || block instanceof gregtech.block.misc.ColoredConstructionBlock) {
                    for (var color : net.minecraft.world.item.DyeColor.values())
                        event.accept(gregtech.block.misc.ConcreteBlock.coloredItem(block, color));
                } else if (block instanceof gregtech.block.misc.ColoredGlassBlock) {
                    for (var color : net.minecraft.world.item.DyeColor.values())
                        event.accept(gregtech.block.misc.ColoredGlassBlock.coloredItem(block, color));
                } else {
                    event.accept(new net.minecraft.world.item.ItemStack(block.asItem(), 1));
                }
            }

            // Wave 50: lasers + quantum + logistics
            for (var entry : gregtech.registry.GTLasers.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(((net.minecraft.world.level.block.Block) entry.get()).asItem(), 1));
            }

            event.accept(gregtech.content.energy.ZpmEnergy.charged());
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
            for (var entry : gregtech.registry.GTAxles.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
            for (var entry : gregtech.registry.GTGearboxes.allGearboxes()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
            for (var entry : gregtech.registry.GTGearboxes.allTransformers()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
            for (var entry : gregtech.registry.GTPumps.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
            return;
        }
        if (GTCreativeTabs.STORAGE_TAB != null
                && event.getTabKey().equals(GTCreativeTabs.STORAGE_TAB.getKey())) {
            // GT6 metaitem-id order: Chests (0+), Safes (2000+), Drawers (4000+),
            // Mass Storage (6000+), Lockers (7300+), then the Ender Garbage pair
            for (var chest : gregtech.registry.GTStorage.METAL_CHESTS) {
                if (chest.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(chest.get()));
            }
            for (var chest : gregtech.registry.GTStorage.REINFORCED_WOOD_CHESTS) {
                if (chest.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(chest.get()));
            }
            var safe = gregtech.registry.GTStorage.SAFE;
            java.util.stream.Stream.concat(gregtech.registry.GTStorage.SAFES.stream(), gregtech.registry.GTStorage.KEY_SAFES.stream())
                    .forEach(entry -> event.accept(new net.minecraft.world.item.ItemStack(entry.get())));
            gregtech.registry.GTStorage.DRAWERS.forEach(entry -> event.accept(new net.minecraft.world.item.ItemStack(entry.get())));
            gregtech.registry.GTStorage.LOCKERS.forEach(entry -> event.accept(new net.minecraft.world.item.ItemStack(entry.get())));
            for (var storage : gregtech.registry.GTStorage.MASS_STORAGES) {
                if (storage.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(storage.get()));
            }
            for (var storage : gregtech.registry.GTStorage.LOGISTICS_MASS_STORAGES) {
                if (storage.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(storage.get()));
            }
            for (var ro : new net.minecraftforge.registries.RegistryObject[]{
                    gregtech.registry.GTStorage.LOCKER,
                    gregtech.registry.GTStorage.ENDER_GARBAGE,
                    gregtech.registry.GTStorage.ENDER_GARBAGE_DUMP}) {
                if (ro != null && ro.isPresent()) {
                    event.accept(new net.minecraft.world.item.ItemStack(
                            (net.minecraft.world.level.block.Block) ro.get()));
                }
            }
        }
        if (GTCreativeTabs.STORAGE_TAB != null
                && event.getTabKey().equals(GTCreativeTabs.STORAGE_TAB.getKey())) {
            for (var entry : gregtech.registry.GTSensors.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
            for (var entry : gregtech.internal.sensor.AddonSensors.blocks()) {
                event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
        }
        if (GTCreativeTabs.ENERGY_NODES_TAB != null
                && event.getTabKey().equals(GTCreativeTabs.ENERGY_NODES_TAB.getKey())) {
            for (var entry : gregtech.registry.GTEnergyNodes.all()) {
                if (entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
            for(var entry:gregtech.registry.GTChemicalBatteries.allRegistered()) {
                var empty=new ItemStack(entry.get());event.accept(empty);
                var charged=empty.copy();var battery=(gregtech.item.ChemicalBatteryItem)charged.getItem();
                battery.setCharge(charged,battery.spec().capacity());event.accept(charged);
            }
            var reactor = gregtech.registry.GTEnergyNodes.REACTOR_CORE_BLOCK;
            if (reactor != null && reactor.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(reactor.get()));
            var reactorCasing = gregtech.registry.GTEnergyNodes.REACTOR_CASING;
            if (reactorCasing != null && reactorCasing.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(reactorCasing.get()));
            var reactorCore2x2 = gregtech.registry.GTEnergyNodes.REACTOR_CORE_2X2;
            if (reactorCore2x2 != null && reactorCore2x2.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(reactorCore2x2.get()));
            // Fuel rods
            for (var entry : gregtech.registry.GTFuelRods.ALL) {
                if (entry.isPresent() && entry.get().definition()!=null) event.accept(new ItemStack(entry.get(), 1));
            }
        }
        if (GTCreativeTabs.MULTIBLOCKS_TAB != null
                && event.getTabKey().equals(GTCreativeTabs.MULTIBLOCKS_TAB.getKey())) {
            var main = gregtech.registry.GTMultiblocks.DISTILLATION_TOWER_MAIN;
            if (main != null && main.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(main.get()));
            var boiler = gregtech.registry.GTMultiblocks.findPart(17201);
            if (boiler != null && boiler.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(boiler.get()));
            var turbine = gregtech.registry.GTMultiblocks.findPart(17211);
            if (turbine != null && turbine.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(turbine.get()));
            for (var ro : new gregtech.registry.RegisteredBlock[]{
                    gregtech.registry.GTMultiblocks.COKE_OVEN_MAIN,
                    gregtech.registry.GTMultiblocks.findPart(17002),
                    gregtech.registry.GTMultiblocks.findPart(17042),
                    gregtech.registry.GTMultiblocks.findPart(17302),
                    gregtech.registry.GTMultiblocks.CRYO_DISTILLATION_MAIN,
                    gregtech.registry.GTMultiblocks.findPart(17231),
                    gregtech.registry.GTMultiblocks.findPart(17221),
                    gregtech.registry.GTMultiblocks.HEAT_EXCHANGER_MAIN,
                    gregtech.registry.GTMultiblocks.findPart(17999),
                    gregtech.registry.GTMultiblocks.findPart(17998),
                    gregtech.registry.GTMultiblocks.IMPLOSION_COMPRESSOR_MAIN,
                    gregtech.registry.GTMultiblocks.FUSION_REACTOR_MAIN}) {
                if (ro != null && ro.isPresent()) {
                    event.accept(new net.minecraft.world.item.ItemStack(
                            (net.minecraft.world.level.block.Block) ro.get()));
                }
            }
            for (var entry : gregtech.content.multiblock.GasTurbineDefinitions.blocks().subList(1,4)) event.accept(entry.get());
            for(boolean steam:new boolean[]{true,false})for(var entry:gregtech.content.multiblock.AxialGeneratorDefinitions.blocks(steam).subList(1,4))event.accept(entry.get());
            for (var entry : gregtech.registry.GTMultiblocks.parts()) {
                if (entry != null && entry.isPresent()) event.accept(new net.minecraft.world.item.ItemStack(entry.get()));
            }
        }
        if (GTCreativeTabs.MULTI_ITEMS_TAB != null
                && event.getTabKey().equals(GTCreativeTabs.MULTI_ITEMS_TAB.getKey())) {
            for (var entry : gregtech.registry.GTMultiItems.all()) {
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
        GTMaterial iron = gregtech.content.material.generated.ElementMaterials.Iron;
        GTMaterial wood = gregtech.content.material.generated.WoodMaterials.Wood;
        GTMaterial diamond = gregtech.content.material.generated.CompoundMaterials.Diamond;
        for (GTToolType type : GTToolType.values()) {
            if (type == GTToolType.GEM_PICK) {
                event.accept(GTToolItem.create(type, diamond, wood));
            } else {
                event.accept(GTToolItem.create(type, iron, type.requiresHeadAssembly() ? wood : iron));
            }
        }
    }

//?}
    private static void populateBlockPrefix(BuildCreativeModeTabContentsEvent event, BlockMaterialPrefix prefix) {
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            if (material.has(MaterialProperty.HIDDEN)) continue;
            if (material.resolve() != material) continue;
            if (!prefix.isValidFor(material)) continue;
            ItemStack stack = GTBlocks.getCreativeStack(prefix, material);
            if (!stack.isEmpty()) event.accept(stack);
        }
    }
//? if >=1.21.1 {
//?} else {

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
        GTFluidItems.allEntries().stream()
                .filter(e -> e.isPresent() && !e.get().fluidEntry().isHidden())
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
        for (var entry : gregtech.registry.GTBoilers.all()) {
            if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
        }
    }

    private static void populateFluidContainers(BuildCreativeModeTabContentsEvent event) {
        for (var entry : GTTanks.all()) {
            if (entry.isPresent()) event.accept(new ItemStack(entry.get(), 1));
        }
        // Wave 43 F11: fluid container items (jug, cup, etc.)
        for (var entry : gregtech.registry.GTItems.ITEMS.getEntries()) {
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
            if (material.has(MaterialProperty.HIDDEN) || material.resolve()!=material
                    || (!prefix.isValidFor(material) && !gregtech.internal.addon.AddonMaterials.explicitPlateOrFoil(prefix, material))) continue;
            if(binding.item().isPresent()) event.accept(new ItemStack(binding.item().get(),1));
        }
    }

    private static ItemStack copySingle(ItemStack stack) {
        return stack.isEmpty() ? stack : stack.copyWithCount(1);
    }
//?}
}
