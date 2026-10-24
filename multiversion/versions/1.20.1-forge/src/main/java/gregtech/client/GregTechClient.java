package gregtech.client;

import gregtech.GregTech;
import gregtech.block.MaterialBlockItem;
import gregtech.block.MaterialBlockLike;
import gregtech.block.machine.GTMachineBlockItem;
import gregtech.block.machine.BasicMachineBlock;
import gregtech.block.machine.EngineBlock;
import gregtech.block.machine.CrucibleCrossingBlock;
import gregtech.block.machine.CrucibleFaucetBlock;
import gregtech.block.machine.CrucibleHullBlockItem;
import gregtech.block.machine.MoldBasinBlock;
import gregtech.block.machine.MoldBlock;
import gregtech.block.machine.SmeltingCrucibleBlock;
import gregtech.block.machine.SmeltingCrucibleBlockItem;
import gregtech.block.machine.BurningBoxBlock;
import gregtech.block.machine.SolidBurningBoxBlock;
import gregtech.block.machine.TankBlock;
import gregtech.block.energy.ElectricWireBlock;
import gregtech.block.machine.FluidPipeBlock;
import gregtech.block.machine.HopperBlock;
import gregtech.block.machine.HopperBlockItem;
import gregtech.block.machine.ItemPipeBlock;
import gregtech.block.machine.QueueHopperBlock;
import gregtech.block.machine.QueueHopperBlockItem;
import gregtech.api.machine.EngineType;
import gregtech.api.machine.MachineRegistry;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTMaterialRegistry;
import gregtech.client.gui.BasicMachineScreen;
import gregtech.client.gui.HopperScreen;
import gregtech.item.FluidItem;
import gregtech.blockentity.machine.KineticSteamEngineBlockEntity;
import gregtech.blockentity.machine.MoldBasinBlockEntity;
import gregtech.blockentity.machine.MoldBlockEntity;
import gregtech.blockentity.machine.SmeltingCrucibleBlockEntity;
import gregtech.registry.GTBlockEntities;
import gregtech.item.MaterialItem;
import gregtech.registry.GTBlocks;
import gregtech.registry.GTFluidItems;
import gregtech.api.fluid.FluidRenderLayers;
import gregtech.api.fluid.WaterMixinStatus;
import net.minecraftforge.registries.RegistryObject;
import gregtech.registry.GTItems;
import gregtech.registry.GTItemPipes;
import gregtech.registry.GTFluidPipes;
import gregtech.registry.GTMenuTypes;
import gregtech.registry.GTTanks;
import gregtech.registry.GTEnergyNodes;
import gregtech.registry.GTWires;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GregTechClient {
    private GregTechClient() {}
    private static boolean isReactor(Block block) {
        return block instanceof gregtech.block.energy.ReactorCasingBlock
                || block instanceof gregtech.block.energy.ReactorCoreBlock
                || block instanceof gregtech.block.energy.ReactorCore2x2Block;
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack,tint)->((gregtech.block.plant.BushBlock)((net.minecraft.world.item.BlockItem)stack.getItem()).getBlock()).inventoryColour(tint),
                java.util.Arrays.stream(gregtech.registry.GTBushes.allBlocks()).map(Block::asItem).toArray(net.minecraft.world.item.Item[]::new));
        gregtech.registry.GTChemicalBatteries.allRegistered().forEach(entry->
                event.register((stack,index)->index==0?entry.get().spec().tint(((gregtech.item.ChemicalBatteryItem)stack.getItem()).stored(stack)):0xFFFFFF,entry.get().asItem()));
        java.util.stream.Stream.concat(gregtech.registry.GTMiscBlocks.ADVANCED_CRAFTING_TABLES.stream(),gregtech.registry.GTMiscBlocks.CHARGING_CRAFTING_TABLES.stream()).forEach(entry -> event.register((stack,index) -> index==0?entry.get().material().getColor():0xFFFFFF,entry.get().asItem()));
        gregtech.registry.GTStorage.BOTTLE_CRATES.forEach(entry -> event.register((stack,index) -> index == 0 ? entry.get().tintRgb() : 0xFFFFFF, entry.get().asItem()));
        gregtech.registry.GTStorage.DRAWERS.forEach(entry -> event.register((stack,index) -> index == 0 ? entry.get().material().getColor() : 0xFFFFFF, entry.get().asItem()));
        gregtech.registry.GTStorage.LOCKERS.forEach(entry -> event.register((stack,index) -> index == 0 ? entry.get().material().getColor() : 0xFFFFFF, entry.get().asItem()));
        java.util.stream.Stream.concat(gregtech.registry.GTStorage.SAFES.stream(), gregtech.registry.GTStorage.KEY_SAFES.stream())
                .forEach(entry -> event.register((stack, tintIndex) -> tintIndex == 0 ? entry.get().material().getColor() : 0xFFFFFF, entry.get().asItem()));
        for(var entry:java.util.List.of(gregtech.registry.GTMiscBlocks.FILTER_ITEMS,gregtech.registry.GTMiscBlocks.FILTER_FLUIDS,gregtech.registry.GTMiscBlocks.FILTER_ITEMS_FLUIDS,gregtech.registry.GTMiscBlocks.FILTER_OREDICT))event.register((stack,index)->index==0?gregtech.content.material.Materials.SteelGalvanized.getColor():0xFFFFFF,entry.get().asItem());
        gregtech.registry.GTMiscBlocks.SOURCE_EXTENDERS.values().forEach(entry->
                event.register((stack,index)->index==0?entry.get().spec().material().getColor():0xFFFFFF,entry.get().asItem()));
        GTItems.ITEMS.getEntries().forEach(entry -> {
            if (entry.get() instanceof gregtech.item.PortableFluidContainerItem vessel) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? vessel.spec().material().getColor() : 0xFFFFFF, vessel);
            } else if (entry.get() instanceof gregtech.item.FuelRodItem rod) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? rod.tintRgb() : 0xFFFFFF, rod);
            } else if (entry.get() instanceof gregtech.item.ElectricToolItem electric) {
                event.register(electric::tint, electric);
            } else if (entry.get() instanceof gregtech.api.material.MaterialFormItem materialItem && !(entry.get() instanceof gregtech.item.EnergyCrystalItem)) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? materialItem.getTintColor() : 0xFFFFFF, entry.get());
            }
        });
        for (var entry : gregtech.registry.GTItems.addonEntries()) {
            if (entry.get() instanceof gregtech.api.material.MaterialFormItem materialItem)
                event.register((stack, tintIndex) -> tintIndex == 0 ? materialItem.getTintColor() : 0xFFFFFF, entry.get());
        }
        gregtech.registry.GTMiscBlocks.all().forEach(entry -> {
            if (!(entry.get() instanceof gregtech.block.misc.PanelBlock)
                    && !(entry.get() instanceof gregtech.block.misc.AutoToolBlock)
                    && !(entry.get() instanceof gregtech.block.misc.LongDistEndpointBlock)
                    && !(entry.get() instanceof gregtech.block.misc.LongDistanceTransformerBlock))
                event.register((stack, index) -> index == 0 ? gregtech.content.tool.MiscBlockAppearance.tint(entry.get()) : 0xFFFFFF, entry.get().asItem());
        });
        var multiblockTintBlocks=gregtech.registry.GTMultiblocks.texturedBlocks().stream()
                .filter(ro -> ro!=null && ro.isPresent()).map(ro -> ro.get()).collect(java.util.stream.Collectors.toSet());
        GTBlocks.BLOCK_ITEMS.getEntries().forEach(entry -> {
            if (entry.get() instanceof net.minecraft.world.item.BlockItem signalItem && signalItem.getBlock() instanceof gregtech.block.energy.SignalWireBlock wire) {
                event.register((stack,index)->gregtech.content.energy.SignalWireColors.tint(wire.material(),index),signalItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem zpmItem && zpmItem.getBlock() instanceof gregtech.block.energy.ZpmModuleBlock) {
                event.register((stack,index)->gregtech.content.energy.ZpmEnergy.stored(stack)>0?0xFFDD00:0x806E00,zpmItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem zpmItem && zpmItem.getBlock() instanceof gregtech.block.energy.ZpmDischargerBlock) {
                event.register((stack,index)->index==0?gregtech.api.material.GTMaterialRegistry.get("Osmiridium").getColor():0xFFDD00,zpmItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem laserItem && laserItem.getBlock() instanceof gregtech.block.energy.LaserConverterBlock laser) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? laser.spec().tint() : 0xFFFFFF, laserItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem coreItem
                    && coreItem.getBlock() instanceof gregtech.block.machine.LogisticsCoreControllerBlock) {
                event.register((stack, tintIndex) -> tintIndex == 0
                        ? gregtech.content.material.Materials.SteelGalvanized.getColor()
                        : 0xFFFFFF, coreItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem spikeItem
                    && spikeItem.getBlock() instanceof gregtech.block.misc.SpikeBlock spike) {
                event.register((stack, tintIndex) -> {
                    if (tintIndex != 0) return 0xFFFFFF;
                    var tag = stack.getTag();
                    boolean secondary = tag != null && tag.contains("BlockStateTag")
                            && "true".equals(tag.getCompound("BlockStateTag").getString("secondary"));
                    return spike.materialTint(secondary);
                }, spikeItem);
            } else if (entry.get() instanceof gregtech.block.misc.ConcreteBlockItem concreteItem) {
                event.register((stack, tintIndex) -> tintIndex == 0
                        ? gregtech.block.misc.ConcreteBlock.tint(
                                gregtech.block.misc.ConcreteBlock.itemColor(stack))
                        : 0xFFFFFF, concreteItem);
            } else if (entry.get() instanceof gregtech.block.misc.ColoredGlassBlockItem glassItem) {
                event.register((stack, tintIndex) -> tintIndex == 0
                        ? gregtech.block.misc.ConcreteBlock.tint(
                                gregtech.block.misc.ColoredGlassBlock.itemColor(stack))
                        : 0xFFFFFF, glassItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem buttonItem
                    && buttonItem.getBlock() instanceof gregtech.block.tool.AdvancedButtonBlock) {
                event.register((stack, tintIndex) -> tintIndex == 0
                        ? gregtech.content.material.Materials.TinAlloy.getColor()
                        : 0xFFFFFF, buttonItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem reactorItem && isReactor(reactorItem.getBlock())) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? gregtech.content.material.Materials.Lead.getColor() : 0xFFFFFF, reactorItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem attachmentItem
                    && attachmentItem.getBlock() instanceof gregtech.block.tool.FluidAttachmentBlock attachment) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? attachment.tintRgb() : 0xFFFFFF, attachmentItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem shapedItem
                    && shapedItem.getBlock() instanceof gregtech.block.tool.ShapedToolBlock shaped) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? shaped.tintRgb() : 0xFFFFFF, shapedItem);
            } else if (entry.get() instanceof MaterialBlockItem blockItem && blockItem.material() != null) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? blockItem.getTintColor() : 0xFFFFFF, blockItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem panelItem
                    && panelItem.getBlock() instanceof gregtech.block.misc.PanelBlock panel) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? panel.tintRgb() : 0xFFFFFF, panelItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem barsItem
                    && barsItem.getBlock() instanceof gregtech.block.misc.BarsBlock bars) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? bars.tintRgb() : 0xFFFFFF, barsItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem shelfItem
                    && shelfItem.getBlock() instanceof gregtech.block.BookShelfBlock shelf) {
                event.register((stack, tintIndex) -> tintIndex == 0 && shelf.variant().metal()
                        ? shelf.variant().material().getColor() : 0xFFFFFF, shelfItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem ropeItem
                    && ropeItem.getBlock() instanceof gregtech.block.tool.RopeBlock rope) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? rope.tintRgb() : 0xFFFFFF, ropeItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem explosiveItem
                    && explosiveItem.getBlock()
                    instanceof gregtech.block.tool.DynamiteBlock explosive) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? explosive.tintRgb() : 0xFFFFFF, explosiveItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem pileItem
                    && (pileItem.getBlock() instanceof gregtech.block.misc.PileBlock
                        || pileItem.getBlock() instanceof gregtech.block.misc.CoinPileBlock)) {
                // The four piles are tinted from their block entity in the world; their item form is
                // GT6's plain UNCOLOURED pile (CS.UNCOLOURED, 0xffffffff), because an item stack
                // carries no contents the model could read.
                event.register((stack, tintIndex) -> 0xFFFFFF, pileItem);
            } else if (entry.get() instanceof GTMachineBlockItem machineItem) {
                if (machineItem.getBlock() instanceof EngineBlock engine) {
                    int coreTint = switch (engine.engineType()) {
                        case ELECTRIC, FLUX -> 0x00FF00;
                        case STEAM -> 0x0000FF;
                        case ROTATION -> machineItem.spec().tintRgb();
                        case DIESEL -> 0xFF6600;
                    };
                    event.register((stack, tintIndex) -> switch (tintIndex) {
                        case 0 -> machineItem.spec().tintRgb();
                        case 1 -> coreTint;
                        default -> 0xFFFFFF;
                    }, machineItem);
                } else {
                    event.register((stack, tintIndex) -> tintIndex == 0 ? machineItem.spec().tintRgb() : 0xFFFFFF, machineItem);
                }
            } else if (entry.get() instanceof SmeltingCrucibleBlockItem crucibleItem) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? crucibleItem.spec().tintRgb() : 0xFFFFFF, crucibleItem);
            } else if (entry.get() instanceof CrucibleHullBlockItem hullItem) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? hullItem.spec().tintRgb() : 0xFFFFFF, hullItem);
            } else if (entry.get() instanceof HopperBlockItem hopperItem) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? hopperItem.spec().tintRgb() : 0xFFFFFF, hopperItem);
            } else if (entry.get() instanceof QueueHopperBlockItem queueItem) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? queueItem.spec().tintRgb() : 0xFFFFFF, queueItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem partItem
                    && multiblockTintBlocks.contains(partItem.getBlock())) {
                event.register((stack, tintIndex) ->
                        tintIndex == 0 ? gregtech.registry.GTMultiblocks.tintOf(partItem.getBlock()) : 0xFFFFFF, partItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem toolItem
                    && toolItem.getBlock() instanceof gregtech.block.tool.ManualToolBlock toolBlock) {
                event.register((stack, tintIndex) -> toolBlock.tint(tintIndex), toolItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem crankItem
                    && crankItem.getBlock() instanceof gregtech.block.tool.CrankBlock) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? gregtech.block.tool.CrankBlock.tintRgb() : 0xFFFFFF, crankItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem storageItem
                    && storageItem.getBlock() instanceof gregtech.block.inventory.MassStorageBlock storageBlock
                    && storageBlock.material() != null) {
                int tint = storageBlock.material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, storageItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem chestItem
                    && chestItem.getBlock() instanceof gregtech.block.inventory.MetalChestBlock chestBlock
                    // GT6's loot chests use their own (already coloured) texture, not the material tint
                    && !(chestBlock instanceof gregtech.block.inventory.LootChestBlock)) {
                int tint = chestBlock.material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, chestItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem nodeItem
                    && nodeItem.getBlock() instanceof gregtech.block.energy.EnergyNodeBlock nodeBlock) {
                int tint = nodeBlock.spec().material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, nodeItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem blockItem
                    && blockItem.getBlock() instanceof ElectricWireBlock wireBlock) {
                int wireTint = wireBlock.spec().material().getColor();
                int rubberTint = 0x141414;
                boolean insulated = wireBlock.spec().insulated();
                event.register((stack, tintIndex) -> tintIndex == 0 ? wireTint : (insulated && tintIndex == 1) ? rubberTint : 0xFFFFFF, blockItem);
            }
        });
        gregtech.registry.GTBoilers.all().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get().asItem());
            }
        });
        gregtech.registry.AddonPipeRegistration.fluidBlocks().forEach(pipe -> {
            int tint = pipe.spec().material().getColor();
            event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, pipe.asItem());
        });
        gregtech.registry.AddonPipeRegistration.itemBlocks().forEach(pipe -> {
            int tint = pipe.spec().material().getColor();
            event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, pipe.asItem());
        });
        // Axle item tints
        gregtech.registry.GTAxles.all().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get().asItem());
            }
        });
        // Gearbox item tints
        gregtech.registry.GTGearboxes.allGearboxes().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get().asItem());
            }
        });
        // Pump item tints
        gregtech.registry.GTPumps.all().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get().asItem());
            }
        });
        // Rotation transformer item tints
        gregtech.registry.GTGearboxes.allTransformers().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get().asItem());
            }
        });
        GTFluidItems.ITEMS.getEntries().stream()
                .filter(RegistryObject::isPresent)
                .map(RegistryObject::get)
                .filter(item -> item instanceof FluidItem)
                .map(item -> (FluidItem) item)
                .forEach(fluidItem -> {
                    event.register((stack, t) -> {
                        if(t!=0) return 0xFFFFFF;
                        if(gregtech.api.fluid.FluidDisplayBinding.hasPayload(stack)) {
                            var actual=gregtech.api.fluid.FluidDisplayBinding.resolve(stack);
                            if(!actual.isEmpty()) return net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(actual.getFluid()).getTintColor(actual);
                        }
                        return FluidAppearance.appearance(fluidItem.fluidEntry()).tint();
                    }, fluidItem);
                });
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        for(var entry:java.util.List.of(gregtech.registry.GTMiscBlocks.FILTER_ITEMS,gregtech.registry.GTMiscBlocks.FILTER_FLUIDS,gregtech.registry.GTMiscBlocks.FILTER_ITEMS_FLUIDS,gregtech.registry.GTMiscBlocks.FILTER_OREDICT))event.register((state,level,pos,index)->index==0?gregtech.content.material.Materials.SteelGalvanized.getColor():0xFFFFFF,entry.get());
        gregtech.registry.GTMiscBlocks.SOURCE_EXTENDERS.values().forEach(entry->
                event.register((state,level,pos,index)->index==0?entry.get().spec().material().getColor():0xFFFFFF,entry.get()));
        for (var rod : gregtech.registry.GTFuelRods.ALL) {
            event.register((state, level, pos, tint) -> tint == 0 ? rod.get().tintRgb() : 0xFFFFFF, rod.get().getBlock());
        }
        GTBlocks.BLOCKS.getEntries().forEach(entry -> {
            Block block = entry.get();
            if (block instanceof gregtech.block.plant.BushBlock) {
                event.register((state,level,pos,tint)->((gregtech.block.plant.BushBlock)state.getBlock()).tintColour(tint,state.getValue(gregtech.block.plant.BushBlock.STAGE)),
                        block);
            } else if (block instanceof gregtech.block.tool.PortableContainerBlock vessel) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? vessel.spec().material().getColor() : 0xFFFFFF, block);
            } else if (block instanceof gregtech.block.energy.ZpmModuleBlock) {
                event.register((state,world,pos,index)->state.getValue(gregtech.block.energy.ZpmModuleBlock.CHARGE)>0?0xFFDD00:0x806E00,block);
            } else if (block instanceof gregtech.block.energy.ZpmDischargerBlock) {
                event.register((state,world,pos,index)->index==0?gregtech.api.material.GTMaterialRegistry.get("Osmiridium").getColor():state.getValue(gregtech.block.energy.ZpmDischargerBlock.ACTIVE)?0xFFDD00:0x804000,block);
            } else if (block instanceof gregtech.block.energy.LaserConverterBlock laser) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? laser.spec().tint() : 0xFFFFFF, block);
            } else if (block instanceof gregtech.block.machine.LogisticsCoreControllerBlock) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? gregtech.content.material.Materials.SteelGalvanized.getColor()
                        : 0xFFFFFF, block);
            } else if (block instanceof gregtech.block.misc.SpikeBlock spike) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? spike.materialTint(state.getValue(gregtech.block.misc.SpikeBlock.SECONDARY))
                        : 0xFFFFFF, block);
            } else if (block instanceof gregtech.block.misc.ConcreteBlock) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? gregtech.block.misc.ConcreteBlock.tint(
                                gregtech.block.misc.FixedConstructionColor.colorOf(state))
                        : 0xFFFFFF, block);
            } else if (block instanceof gregtech.block.misc.ColoredGlassBlock) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? gregtech.block.misc.ConcreteBlock.tint(
                                gregtech.block.misc.FixedConstructionColor.colorOf(state))
                        : 0xFFFFFF, block);
            } else if (block instanceof gregtech.block.tool.AdvancedButtonBlock) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? gregtech.content.material.Materials.TinAlloy.getColor()
                        : 0xFFFFFF, block);
            } else if (isReactor(block)) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? gregtech.content.material.Materials.Lead.getColor() : 0xFFFFFF, block);
            } else if (block instanceof gregtech.block.tool.FluidAttachmentBlock attachment) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? attachment.tintRgb() : 0xFFFFFF, block);
            } else if (block instanceof gregtech.block.tool.ShapedToolBlock shaped) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? shaped.tintRgb() : 0xFFFFFF, block);
            } else if (block instanceof MaterialBlockLike materialBlock) {
                int color = materialBlock.material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? color : 0xFFFFFF, block);
            } else if (block instanceof gregtech.block.misc.PanelBlock panel) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? panel.tintRgb() : 0xFFFFFF, panel);
            } else if (block instanceof gregtech.block.misc.PileBlock) {
                // GT6 tints the pile's own greyscale textures with the stored material's colour
                // (`BlockTextureDefault.get(sTextureSides, mMaterial.fRGBaSolid, ...)`,
                // MultiTileEntityIngot.java:46-47, MultiTileEntityPlate.java:46-47); the port reads the
                // colour out of the pile's block entity instead.
                event.register((state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) return 0xFFFFFF;
                    if (level.getBlockEntity(pos) instanceof gregtech.blockentity.misc.PileBlockEntity pile) {
                        var material = gregtech.blockentity.misc.PileBlockEntity.materialOf(pile.stored());
                        if (material != null) return material.getColor();
                    }
                    return 0xFFFFFF;
                }, block);
            } else if (block instanceof gregtech.block.misc.CoinPileBlock) {
                // The same for GT6's coins, which its renderer colours with `mMaterial.fRGBaSolid`
                // (MultiTileEntityCoin.java:430-435); an unbound pile already carries the port's default
                // copper coin, so the colour always comes from the pile's own coin item.
                event.register((state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) return 0xFFFFFF;
                    if (level.getBlockEntity(pos) instanceof gregtech.blockentity.misc.CoinPileBlockEntity pile
                            && gregtech.block.misc.CoinPileBlock.isCoin(pile.coinItem())) {
                        return gregtech.item.MaterialItem.getMaterial(pile.coinItem()).getColor();
                    }
                    return 0xFFFFFF;
                }, block);
            } else if (block instanceof gregtech.block.tool.RopeBlock rope) {
                // GT6 tints the shared greyscale rope texture with the rope material's colour
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? rope.tintRgb() : 0xFFFFFF, rope);
            } else if (block instanceof gregtech.block.misc.BarsBlock bars) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? bars.tintRgb() : 0xFFFFFF, bars);
            } else if (block instanceof gregtech.block.BookShelfBlock shelf) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 && shelf.variant().metal()
                        ? shelf.variant().material().getColor() : 0xFFFFFF, shelf);
            } else if (block instanceof gregtech.block.tool.DynamiteBlock explosive) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? explosive.tintRgb() : 0xFFFFFF, explosive);
            }
        });
        gregtech.registry.GTMiscBlocks.all().forEach(entry -> {
            if (!(entry.get() instanceof gregtech.block.misc.PanelBlock)
                    && !(entry.get() instanceof gregtech.block.misc.AutoToolBlock)
                    && !(entry.get() instanceof gregtech.block.misc.LongDistEndpointBlock)
                    && !(entry.get() instanceof gregtech.block.misc.LongDistanceTransformerBlock))
                event.register((state, level, pos, index) -> index == 0 ? gregtech.content.tool.MiscBlockAppearance.tint(entry.get()) : 0xFFFFFF, entry.get());
        });
        // Surface rocks: tint from the block entity's material (vein indicators).
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || level == null || pos == null) return 0xFFFFFF;
            if (level.getBlockEntity(pos) instanceof gregtech.blockentity.RockBlockEntity rock) {
                var material = gregtech.api.material.GTMaterialRegistry.get(rock.getMaterial());
                if (material != null && material.resolve().isValid()) {
                    return material.resolve().getColor();
                }
            }
            return 0xFFFFFF;
        }, GTBlocks.ROCK.get());
        MachineRegistry.solidBurningBoxes().forEach(entry -> {
            if (!entry.isPresent()) {
                return;
            }
            SolidBurningBoxBlock block = entry.get();
            int tint = block.spec().tintRgb();
            event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, block);
        });
        MachineRegistry.burningBoxes().forEach(entry -> {
            if (!entry.isPresent()) return;
            BurningBoxBlock block = entry.get();
            int tint = block.spec().tintRgb();
            event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, block);
        });
        MachineRegistry.basicMachines().forEach(entry -> {
            if (entry.isPresent()) {
                BasicMachineBlock block = entry.get();
                int tint = block.basicSpec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, block);
            }
        });
        GTFluidPipes.all().forEach(entry -> {
            if (entry.isPresent()) {
                FluidPipeBlock pipe = entry.get();
                int tint = pipe.spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, pipe);
            }
        });
        GTItemPipes.all().forEach(entry -> {
            if (entry.isPresent()) {
                ItemPipeBlock pipe = entry.get();
                int tint = pipe.spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, pipe);
            }
        });
        gregtech.registry.AddonPipeRegistration.fluidBlocks().forEach(pipe -> {
            int tint = pipe.spec().material().getColor();
            event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, pipe);
        });
        gregtech.registry.AddonPipeRegistration.itemBlocks().forEach(pipe -> {
            int tint = pipe.spec().material().getColor();
            event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, pipe);
        });
        GTTanks.all().forEach(entry -> {
            if (entry.isPresent()) {
                TankBlock tank = entry.get();
                int tint = tank.spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, tank);
            }
        });
        gregtech.registry.GTBoilers.all().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        gregtech.registry.GTMultiblocks.texturedBlocks().forEach(entry -> {
            if (entry != null && entry.isPresent()) {
                event.register((state, level, pos, tintIndex) ->
                        tintIndex == 0 ? gregtech.registry.GTMultiblocks.tintOf(entry.get()) : 0xFFFFFF, entry.get());
            }
        });
        gregtech.registry.GTToolBlocks.manual().forEach(entry -> {
            if (entry.isPresent()) {
                event.register((state, level, pos, tintIndex) -> entry.get().tint(tintIndex), entry.get());
            }
        });
        if (gregtech.registry.GTToolBlocks.CRANK != null
                && gregtech.registry.GTToolBlocks.CRANK.isPresent()) {
            event.register((state, level, pos, tintIndex) ->
                    tintIndex == 0 ? gregtech.block.tool.CrankBlock.tintRgb() : 0xFFFFFF, gregtech.registry.GTToolBlocks.CRANK.get());
        }
        GTEnergyNodes.all().forEach(entry -> {
            if (entry.isPresent()) {
                var node = entry.get();
                int tint = node.spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, node);
            }
        });
        gregtech.registry.GTStorage.MASS_STORAGES.forEach(entry -> {
            if (entry.isPresent() && entry.get().material() != null) {
                int tint = entry.get().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        gregtech.registry.GTStorage.LOGISTICS_MASS_STORAGES.forEach(entry -> {
            if (entry.isPresent()) {
                int tint = gregtech.content.material.Materials.Black.getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        gregtech.registry.GTStorage.METAL_CHESTS.forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        java.util.stream.Stream.concat(gregtech.registry.GTMiscBlocks.ADVANCED_CRAFTING_TABLES.stream(),gregtech.registry.GTMiscBlocks.CHARGING_CRAFTING_TABLES.stream()).forEach(entry -> event.register((state,level,pos,index) -> index==0?entry.get().material().getColor():0xFFFFFF,entry.get()));
        gregtech.registry.GTStorage.BOTTLE_CRATES.forEach(entry -> event.register((state,level,pos,index) -> index == 0 ? entry.get().tintRgb() : 0xFFFFFF, entry.get()));
        gregtech.registry.GTStorage.DRAWERS.forEach(entry -> event.register((state,level,pos,index) -> index == 0 ? entry.get().material().getColor() : 0xFFFFFF, entry.get()));
        gregtech.registry.GTStorage.LOCKERS.forEach(entry -> event.register((state,level,pos,index) -> index == 0 ? entry.get().material().getColor() : 0xFFFFFF, entry.get()));
        java.util.stream.Stream.concat(gregtech.registry.GTStorage.SAFES.stream(), gregtech.registry.GTStorage.KEY_SAFES.stream())
                .forEach(entry -> event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? entry.get().material().getColor() : 0xFFFFFF, entry.get()));
        gregtech.registry.GTSignalWires.all().forEach(entry -> {
            var wire = entry.get();
            event.register((state, level, pos, tintIndex) -> gregtech.content.energy.SignalWireColors.tint(wire.material(),tintIndex), wire);
        });
        GTWires.all().forEach(entry -> {
            if (entry.isPresent()) {
                ElectricWireBlock wire = entry.get();
                int wireTint = wire.spec().material().getColor();
                int rubberTint = 0x141414;
                boolean insulated = wire.spec().insulated();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? wireTint : (insulated && tintIndex == 1) ? rubberTint : 0xFFFFFF, wire);
            }
        });
        // Axles: tint via material color
        gregtech.registry.GTAxles.all().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        // Gearboxes: tint via material color
        gregtech.registry.GTGearboxes.allGearboxes().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        // Pumps: tint via material color
        gregtech.registry.GTPumps.all().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        // Rotation transformers: tint via material color
        gregtech.registry.GTGearboxes.allTransformers().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        MachineRegistry.smeltingCrucibles().forEach(entry -> {
            if (entry.isPresent()) {
                registerCrucibleTint(event, entry.get());
            }
        });
        MachineRegistry.moldBasins().forEach(entry -> {
            if (entry.isPresent()) {
                registerCrucibleTint(event, entry.get());
            }
        });
        MachineRegistry.molds().forEach(entry -> {
            if (entry.isPresent()) {
                registerCrucibleTint(event, entry.get());
            }
        });
        MachineRegistry.crucibleCrossings().forEach(entry -> {
            if (entry.isPresent()) {
                registerCrucibleTint(event, entry.get());
            }
        });
        MachineRegistry.crucibleFaucets().forEach(entry -> {
            if (entry.isPresent()) {
                registerCrucibleTint(event, entry.get());
            }
        });
        MachineRegistry.hoppers().forEach(entry -> {
            if (entry.isPresent()) {
                HopperBlock block = entry.get();
                int tint = block.spec().tintRgb();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, block);
            }
        });
        MachineRegistry.queueHoppers().forEach(entry -> {
            if (entry.isPresent()) {
                QueueHopperBlock block = entry.get();
                int tint = block.spec().tintRgb();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, block);
            }
        });
        MachineRegistry.allEngines().forEach(entry -> {
            if (entry.isPresent()) {
                EngineBlock block = entry.get();
                int tint = block.tintRgb();
                if (block.engineType() == EngineType.STEAM) {
                    int coreTint = 0x0000FF;
                    event.register((state, level, pos, tintIndex) -> switch (tintIndex) {
                        case 0 -> tint;
                        case 1 -> level!=null&&pos!=null&&level.getBlockEntity(pos) instanceof gregtech.blockentity.machine.KineticSteamEngineBlockEntity steam
                                ? steamPressureColor(steam.pressureState()) : coreTint;
                        default -> 0xFFFFFF;
                    }, block);
                } else {
                    int coreTint = switch (block.engineType()) {
                        case ELECTRIC, FLUX -> 0x00FF00;
                        case ROTATION -> tint;
                        case DIESEL -> 0xFF6600;
                        default -> 0xFFFFFF;
                    };
                    event.register((state, level, pos, tintIndex) -> switch (tintIndex) {
                        case 0 -> tint;
                        case 1 -> level != null && pos != null
                                && level.getBlockEntity(pos) instanceof gregtech.blockentity.machine.PistonEngineBlockEntity piston
                                ? piston.coreColor() : coreTint;
                        default -> 0xFFFFFF;
                    }, block);
                }
            }
        });
    }

    /**
     * Heat color gradient for steam engines: blue (cold/0%) → green (warm/50%) → red (hot/100%).
     * The engine core tintindex uses this to show fill/temperature visually.
     */
    private static int engineHeatColor(long kuEnergy, long capacity) {
        if (capacity <= 0) return 0x0000FF;
        int pct = (int) Math.min(100, kuEnergy * 100 / capacity);
        int r, g, b;
        if (pct <= 50) {
            double f = pct / 50.0;
            r = 0;
            g = (int) (255.0 * f);
            b = (int) (255.0 * (1.0 - f));
        } else {
            double f = (pct - 50) / 50.0;
            r = (int) (255.0 * f);
            g = (int) (255.0 * (1.0 - f));
            b = 0;
        }
        return (r << 16) | (g << 8) | b;
    }

    private static void registerCrucibleTint(RegisterColorHandlersEvent.Block event, net.minecraft.world.level.block.Block block) {
        if (block instanceof SmeltingCrucibleBlock crucible) {
            int tint = crucible.spec().tintRgb();
            event.register((state, level, pos, tintIndex) -> {
                if (tintIndex != 0) {
                    return 0xFFFFFF;
                }
                if (level != null && pos != null) {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof SmeltingCrucibleBlockEntity be && be.isMeltDownWarning()) {
                        return CrucibleBlockBakedModel.meltdownRgb(tint);
                    }
                }
                return tint;
            }, block);
            return;
        }
        if (block instanceof MoldBasinBlock basin) {
            int tint = basin.spec().tintRgb();
            event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, block);
        } else if (block instanceof MoldBlock mold) {
            int tint = mold.spec().tintRgb();
            event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, block);
        } else if (block instanceof CrucibleCrossingBlock crossing) {
            int tint = crossing.spec().tintRgb();
            event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, block);
        } else if (block instanceof CrucibleFaucetBlock faucet) {
            int tint = faucet.spec().tintRgb();
            event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, block);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GTBlockEntities.BOTTLE_CRATE.get(), BottleCrateRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.BOOKSHELF.get(), BookShelfRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.LOGISTICS_CORE.get(), LogisticsCoverRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.LOGISTICS_WIRE.get(), LogisticsCoverRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.MULTIBLOCK_PORT.get(), LogisticsCoverRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.COIN_PILE.get(), CoinPileRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.COIN_MOLD.get(), CoinMoldRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.SANDWICH_BLOCK.get(), SandwichRenderer::new);
        LayoutPartClients.collect();
        event.registerBlockEntityRenderer(GTBlockEntities.ADDON_FUNCTIONAL_PART.get(), FunctionalPartRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.ADDON_ENERGY_PART.get(), FunctionalPartRenderer::new);
        BehaviorMachineClients.collect();
        var behaviorTypes = new java.util.LinkedHashSet<net.minecraft.world.level.block.entity.BlockEntityType<?>>();
        for (var holder : gregtech.registry.BehaviorMachineRegistration.all()) {
            var type = holder.get().blockEntityType();
            if (type != null) behaviorTypes.add(type);
        }
        for (var type : behaviorTypes)
            event.registerBlockEntityRenderer((net.minecraft.world.level.block.entity.BlockEntityType<
                    net.minecraft.world.level.block.entity.BlockEntity>) (Object) type, BehaviorMachineRenderer::new);
        MaterialFluidVisuals.bootstrap();
        MaterialFluidVisuals.linkAllMoltenMaterials();
        event.registerBlockEntityRenderer(GTBlockEntities.SMELTING_CRUCIBLE.get(), SmeltingCrucibleRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.LARGE_CRUCIBLE.get(), LargeCrucibleRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.PORTABLE_CONTAINER.get(), CapsuleCellRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.MOLD.get(), SmelteryHullRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.MOLD_BASIN.get(), SmelteryHullRenderer::new);
        // (pipe/wire arms render via PipeWireBakedModel — no BERs needed)

        event.registerBlockEntityRenderer(GTBlockEntities.METAL_CHEST.get(), MetalChestRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.TANK.get(),TankCoverRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.ENERGY_NODE.get(), EnergyNodeRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.CHEMICAL_BATTERY.get(), ChemicalBatteryRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.MASS_STORAGE.get(), MassStorageRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.LOGISTICS_MASS_STORAGE.get(), MassStorageRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.MATERIAL_ANVIL.get(), MaterialAnvilRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.PROCESSING_TOOL.get(), ProcessingToolRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.REACTOR_CORE.get(), ReactorRodRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.REACTOR_CORE_2X2.get(), ReactorRodRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.SENSOR.get(), SensorRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.EXTENDER.get(), ExtenderCoverRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.USB_SWITCH.get(), DataSwitchCoverRenderer::new);
        // Covers on fluid pipe faces. The pipe itself still renders through PipeWireBakedModel
        // (see the note above); this BER only adds the cover plates, and it draws nothing for a pipe
        // that carries no cover.
        event.registerBlockEntityRenderer(GTBlockEntities.FLUID_PIPE.get(), PipeCoverRenderer::new);
        // The item pipe's half (§112), for the same reason and with the same "draws nothing for a
        // coverless pipe" rule. One BE type covers every item pipe, exactly as GTBlockEntities:145-151
        // registers GTItemPipes' blocks into a single type.
        event.registerBlockEntityRenderer(GTBlockEntities.ITEM_PIPE.get(), ItemPipeCoverRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.STEAM_BOILER.get(), BoilerBarometerRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.ORIGINAL_LARGE_BOILER.get(), OriginalLargeBoilerBarometerRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.LARGE_TURBINE.get(), LargeTurbineRotorRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.LARGE_GAS_TURBINE.get(), LargeTurbineRotorRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.FUSION_REACTOR.get(), MachineCoverRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.COKE_OVEN.get(), MachineCoverRenderer::new);

        // Cover plates on basic machine faces
        for (var blockRO : gregtech.api.machine.MachineRegistry.basicMachines()) {
            if (!blockRO.isPresent()) continue;
            BlockEntityType<?> beType = blockRO.get().beType();
            if (beType == null) continue;
            @SuppressWarnings("unchecked")
            var typed = (BlockEntityType<gregtech.blockentity.machine.BasicMachineBlockEntity>) (Object) beType;
            event.registerBlockEntityRenderer(typed, MachineCoverRenderer::new);
        }

        // Engine BERs — heat-colored core overlay
        var steamEngines = gregtech.api.machine.MachineRegistry.steamEngines();
        var strongSteamEngines = gregtech.api.machine.MachineRegistry.strongSteamEngines();
        for (var blockRO : steamEngines) {
            event.registerBlockEntityRenderer(
                    (BlockEntityType<KineticSteamEngineBlockEntity>) (Object) blockRO.get().getBeType(),
                    SteamEngineRenderer::new);
        }
        for (var blockRO : strongSteamEngines) {
            event.registerBlockEntityRenderer(
                    (BlockEntityType<KineticSteamEngineBlockEntity>) (Object) blockRO.get().getBeType(),
                    SteamEngineRenderer::new);
        }
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> gregtech.registry.GTSpikes.all().forEach(entry ->
                net.minecraft.client.renderer.item.ItemProperties.register(entry.get().asItem(),
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", "spike_omni"),
                        (stack, level, entity, seed) -> gregtech.block.misc.SpikeBlock.itemMode(stack)
                                == gregtech.block.misc.SpikeBlock.Mode.WALL ? 0 : 1)));
        event.enqueueWork(()->gregtech.registry.GTChemicalBatteries.allRegistered().forEach(entry->
                net.minecraft.client.renderer.item.ItemProperties.register(entry.get().asItem(),
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","battery_charge"),
                        (stack,level,entity,seed)->{
                            var item=(gregtech.item.ChemicalBatteryItem)stack.getItem();
                            return item.spec().display(item.stored(stack))/(float)item.spec().scale();
                        })));
        // In the IDE, omitting --mixin.config starts the game without an error but restores a
        // visible border between vanilla water and GT6 world water. Verify the transformed method
        // itself, independently of fluid tags (which are not loaded yet during client setup).
        Fluids.WATER.isSame(Fluids.FLOWING_WATER);
        if (!WaterMixinStatus.wasInvoked()) {
            throw new IllegalStateException("GregTech water Mixin is inactive: gregtech.mixins.json was not loaded. "
                    + "Regenerate IDEA runs with gradlew genIntellijRuns and restart runClient.");
        }
        ItemBlockRenderTypes.setRenderLayer(gregtech.registry.GTDecorBlocks.GLASS_CLEAR.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(gregtech.registry.GTDecorBlocks.GLASS_GLOW.get(), RenderType.translucent());
        // GT6 world fluids must be built into the translucent chunk layer.
        // ChunkRenderDispatcher picks a fluid's layer with ItemBlockRenderTypes.getRenderLayer
        // (ChunkRenderDispatcher.java:620) and vanilla's ItemBlockRenderTypes.FLUID_RENDER_TYPES only
        // contains Fluids.WATER / Fluids.FLOWING_WATER -> RenderType.translucent()
        // (ItemBlockRenderTypes.java:315-319, default RenderType.solid() at line 400), so a separate
        // fluid stayed on the opaque solid layer - "the liquid still is not transparent like vanilla
        // water". The flowing variants are fluids of their own and are what most blocks of a GT6
        // water body actually carry, so they are registered too: worldWaterLayers() is the shared
        // six water IDs plus twelve spring IDs share this registry policy. setRenderLayer may only be called
        // while the client mod loader is still loading (checkClientLoading,
        // ItemBlockRenderTypes.java:437-448), i.e. here, directly from the event, not from
        // enqueueWork.
        for (var registration : FluidRenderLayers.worldFluidLayers().entrySet()) {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(GregTech.id(registration.getKey()));
            if (fluid == null) {
                throw new IllegalStateException("GT world fluid is missing its registered fluid: "
                        + registration.getKey());
            }
            if (gregtech.api.fluid.GTWaterParity.isWorldWater(registration.getKey())
                    && (!Fluids.WATER.isSame(fluid) || !fluid.isSame(Fluids.WATER))) {
                throw new IllegalStateException("GT world water does not merge with vanilla water: "
                        + registration.getKey() + ". Check gregtech.mixins.json and water registration.");
            }
            ItemBlockRenderTypes.setRenderLayer(fluid, FluidRenderLayers.isTranslucent(registration.getValue())
                    ? RenderType.translucent() : RenderType.solid());
            if (ItemBlockRenderTypes.getRenderLayer(fluid.defaultFluidState()) != RenderType.translucent()) {
                throw new IllegalStateException("GT world fluid was not registered for translucent rendering: "
                        + registration.getKey());
            }
        }

        event.enqueueWork(() -> {
            for (var type : GTMenuTypes.allHopperTypes()) {
                MenuScreens.register(type.get(), HopperScreen::new);
            }
            MenuScreens.register(GTMenuTypes.ADVANCED_CRAFTING.get(),gregtech.client.gui.AdvancedCraftingScreen::new);
            MenuScreens.register(GTMenuTypes.GARBAGE_DUMP.get(), HopperScreen::new);
            MenuScreens.register(GTMenuTypes.BASIC_MACHINE.get(), BasicMachineScreen::new);
            MenuScreens.register(GTMenuTypes.BEHAVIOR_MACHINE.get(),
                    (MenuScreens.ScreenConstructor<gregtech.client.gui.BehaviorMachineMenu,
                            gregtech.client.gui.BehaviorMachineHostScreen>) (menu, inventory, title) -> {
                        var machine = menu.machine();
                        net.minecraft.client.gui.screens.Screen custom = BehaviorMachineClients.screen(
                                menu, machine.addonId(), machine.machineType(), inventory, title);
                        return custom != null
                                ? new gregtech.client.gui.BehaviorMachineCustomScreen(
                                        menu, inventory, title, custom)
                                : new gregtech.client.gui.BehaviorMachineScreen(menu, inventory, title);
                    });
            MenuScreens.register(GTMenuTypes.PART.get(), gregtech.client.gui.PartScreen::new);
            MenuScreens.register(GTMenuTypes.FILTER.get(),gregtech.client.gui.FilterScreen::new);
            MenuScreens.register(GTMenuTypes.USB_SWITCH.get(), gregtech.client.gui.DataSwitchScreen::new);
            MenuScreens.register(GTMenuTypes.HDD_SWITCH.get(), gregtech.client.gui.DataSwitchScreen::new);
            // GT6's bumbliary and advanced bumbliary machines.
            MenuScreens.register(GTMenuTypes.BUMBLIARY.get(), gregtech.client.gui.BumbliaryScreen::new);
            MenuScreens.register(GTMenuTypes.ADVANCED_BUMBLIARY.get(), gregtech.client.gui.BumbliaryScreen::new);
        });
    }
    private static int steamPressureColor(int state){
        int n=Math.max(0,Math.min(31,state));
        return n<16?((n*17)<<8)|(255-n*17):((n-16)*17<<16)|((255-(n-16)*17)<<8);
    }
}
