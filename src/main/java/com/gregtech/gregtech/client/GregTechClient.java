package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.MaterialBlockItem;
import com.gregtech.gregtech.block.MaterialBlockLike;
import com.gregtech.gregtech.block.machine.GTMachineBlockItem;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.block.machine.EngineBlock;
import com.gregtech.gregtech.block.machine.CrucibleCrossingBlock;
import com.gregtech.gregtech.block.machine.CrucibleFaucetBlock;
import com.gregtech.gregtech.block.machine.CrucibleHullBlockItem;
import com.gregtech.gregtech.block.machine.MoldBasinBlock;
import com.gregtech.gregtech.block.machine.MoldBlock;
import com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock;
import com.gregtech.gregtech.block.machine.SmeltingCrucibleBlockItem;
import com.gregtech.gregtech.block.machine.BurningBoxBlock;
import com.gregtech.gregtech.block.machine.SolidBurningBoxBlock;
import com.gregtech.gregtech.block.machine.TankBlock;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.block.machine.HopperBlock;
import com.gregtech.gregtech.block.machine.HopperBlockItem;
import com.gregtech.gregtech.block.machine.ItemPipeBlock;
import com.gregtech.gregtech.block.machine.QueueHopperBlock;
import com.gregtech.gregtech.block.machine.QueueHopperBlockItem;
import com.gregtech.gregtech.api.machine.EngineType;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.client.gui.BasicMachineScreen;
import com.gregtech.gregtech.client.gui.HopperScreen;
import com.gregtech.gregtech.item.FluidItem;
import com.gregtech.gregtech.blockentity.machine.KineticSteamEngineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MoldBasinBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MoldBlockEntity;
import com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTFluidItems;
import com.gregtech.gregtech.api.fluid.FluidRenderLayers;
import com.gregtech.gregtech.api.fluid.WaterMixinStatus;
import net.minecraftforge.registries.RegistryObject;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTItemPipes;
import com.gregtech.gregtech.registry.GTFluidPipes;
import com.gregtech.gregtech.registry.GTMenuTypes;
import com.gregtech.gregtech.registry.GTTanks;
import com.gregtech.gregtech.registry.GTEnergyNodes;
import com.gregtech.gregtech.registry.GTWires;
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
        return block instanceof com.gregtech.gregtech.block.energy.ReactorCasingBlock
                || block instanceof com.gregtech.gregtech.block.energy.ReactorCoreBlock
                || block instanceof com.gregtech.gregtech.block.energy.ReactorCore2x2Block;
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack,tint)->((com.gregtech.gregtech.block.plant.BushBlock)((net.minecraft.world.item.BlockItem)stack.getItem()).getBlock()).tintColour(tint,3),
                java.util.Arrays.stream(com.gregtech.gregtech.registry.GTBushes.allBlocks()).map(Block::asItem).toArray(net.minecraft.world.item.Item[]::new));
        com.gregtech.gregtech.registry.GTChemicalBatteries.allRegistered().forEach(entry->
                event.register((stack,index)->index==0?entry.get().spec().chemistry().color:0xFFFFFF,entry.get().asItem()));
        java.util.stream.Stream.concat(com.gregtech.gregtech.registry.GTMiscBlocks.ADVANCED_CRAFTING_TABLES.stream(),com.gregtech.gregtech.registry.GTMiscBlocks.CHARGING_CRAFTING_TABLES.stream()).forEach(entry -> event.register((stack,index) -> index==0?entry.get().material().getColor():0xFFFFFF,entry.get().asItem()));
        com.gregtech.gregtech.registry.GTStorage.BOTTLE_CRATES.forEach(entry -> event.register((stack,index) -> index == 0 ? entry.get().tintRgb() : 0xFFFFFF, entry.get().asItem()));
        com.gregtech.gregtech.registry.GTStorage.DRAWERS.forEach(entry -> event.register((stack,index) -> index == 0 ? entry.get().material().getColor() : 0xFFFFFF, entry.get().asItem()));
        java.util.stream.Stream.concat(com.gregtech.gregtech.registry.GTStorage.SAFES.stream(), com.gregtech.gregtech.registry.GTStorage.KEY_SAFES.stream())
                .forEach(entry -> event.register((stack, tintIndex) -> tintIndex == 0 ? entry.get().material().getColor() : 0xFFFFFF, entry.get().asItem()));
        for(var entry:java.util.List.of(com.gregtech.gregtech.registry.GTMiscBlocks.FILTER_ITEMS,com.gregtech.gregtech.registry.GTMiscBlocks.FILTER_FLUIDS,com.gregtech.gregtech.registry.GTMiscBlocks.FILTER_ITEMS_FLUIDS,com.gregtech.gregtech.registry.GTMiscBlocks.FILTER_OREDICT))event.register((stack,index)->index==0?com.gregtech.gregtech.content.material.Materials.SteelGalvanized.getColor():0xFFFFFF,entry.get().asItem());
        com.gregtech.gregtech.registry.GTMiscBlocks.SOURCE_EXTENDERS.values().forEach(entry->
                event.register((stack,index)->index==0?entry.get().spec().material().getColor():0xFFFFFF,entry.get().asItem()));
        GTItems.ITEMS.getEntries().forEach(entry -> {
            if (entry.get() instanceof com.gregtech.gregtech.item.PortableFluidContainerItem vessel) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? vessel.spec().material().getColor() : 0xFFFFFF, vessel);
            } else if (entry.get() instanceof com.gregtech.gregtech.item.FuelRodItem rod) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? rod.tintRgb() : 0xFFFFFF, rod);
            } else if (entry.get() instanceof com.gregtech.gregtech.item.ElectricToolItem electric) {
                event.register(electric::tint, electric);
            } else if (entry.get() instanceof com.gregtech.gregtech.api.material.MaterialFormItem materialItem) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? materialItem.getTintColor() : 0xFFFFFF, entry.get());
            }
        });
        com.gregtech.gregtech.registry.GTMiscBlocks.all().forEach(entry -> {
            if (!(entry.get() instanceof com.gregtech.gregtech.block.misc.PanelBlock)
                    && !(entry.get() instanceof com.gregtech.gregtech.block.misc.AutoToolBlock)
                    && !(entry.get() instanceof com.gregtech.gregtech.block.misc.LongDistEndpointBlock)
                    && !(entry.get() instanceof com.gregtech.gregtech.block.misc.LongDistanceTransformerBlock))
                event.register((stack, index) -> index == 0 ? com.gregtech.gregtech.content.tool.MiscBlockAppearance.tint(entry.get()) : 0xFFFFFF, entry.get().asItem());
        });
        var multiblockTintBlocks=com.gregtech.gregtech.registry.GTMultiblocks.texturedBlocks().stream()
                .filter(ro -> ro!=null && ro.isPresent()).map(ro -> ro.get()).collect(java.util.stream.Collectors.toSet());
        GTBlocks.BLOCK_ITEMS.getEntries().forEach(entry -> {
            if (entry.get() instanceof net.minecraft.world.item.BlockItem signalItem && signalItem.getBlock() instanceof com.gregtech.gregtech.block.energy.SignalWireBlock wire) {
                event.register((stack,index)->index==0?wire.material().getColor():index==1?0x604040:0xFFFFFF,signalItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem zpmItem && zpmItem.getBlock() instanceof com.gregtech.gregtech.block.energy.ZpmModuleBlock) {
                event.register((stack,index)->com.gregtech.gregtech.content.energy.ZpmEnergy.stored(stack)>0?0xFFDD00:0x806E00,zpmItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem zpmItem && zpmItem.getBlock() instanceof com.gregtech.gregtech.block.energy.ZpmDischargerBlock) {
                event.register((stack,index)->index==0?com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Osmiridium").getColor():0xFFDD00,zpmItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem laserItem && laserItem.getBlock() instanceof com.gregtech.gregtech.block.energy.LaserConverterBlock laser) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? laser.spec().tint() : 0xFFFFFF, laserItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem coreItem
                    && coreItem.getBlock() instanceof com.gregtech.gregtech.block.machine.LogisticsCoreControllerBlock) {
                event.register((stack, tintIndex) -> tintIndex == 0
                        ? com.gregtech.gregtech.content.material.Materials.SteelGalvanized.getColor()
                        : 0xFFFFFF, coreItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem spikeItem
                    && spikeItem.getBlock() instanceof com.gregtech.gregtech.block.misc.SpikeBlock spike) {
                event.register((stack, tintIndex) -> {
                    if (tintIndex != 0) return 0xFFFFFF;
                    var tag = stack.getTag();
                    boolean secondary = tag != null && tag.contains("BlockStateTag")
                            && "true".equals(tag.getCompound("BlockStateTag").getString("secondary"));
                    return spike.materialTint(secondary);
                }, spikeItem);
            } else if (entry.get() instanceof com.gregtech.gregtech.block.misc.ConcreteBlockItem concreteItem) {
                event.register((stack, tintIndex) -> tintIndex == 0
                        ? com.gregtech.gregtech.block.misc.ConcreteBlock.tint(
                                com.gregtech.gregtech.block.misc.ConcreteBlock.itemColor(stack))
                        : 0xFFFFFF, concreteItem);
            } else if (entry.get() instanceof com.gregtech.gregtech.block.misc.ColoredGlassBlockItem glassItem) {
                event.register((stack, tintIndex) -> tintIndex == 0
                        ? com.gregtech.gregtech.block.misc.ConcreteBlock.tint(
                                com.gregtech.gregtech.block.misc.ColoredGlassBlock.itemColor(stack))
                        : 0xFFFFFF, glassItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem buttonItem
                    && buttonItem.getBlock() instanceof com.gregtech.gregtech.block.tool.AdvancedButtonBlock) {
                event.register((stack, tintIndex) -> tintIndex == 0
                        ? com.gregtech.gregtech.content.material.Materials.TinAlloy.getColor()
                        : 0xFFFFFF, buttonItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem reactorItem && isReactor(reactorItem.getBlock())) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? com.gregtech.gregtech.content.material.Materials.Lead.getColor() : 0xFFFFFF, reactorItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem attachmentItem
                    && attachmentItem.getBlock() instanceof com.gregtech.gregtech.block.tool.FluidAttachmentBlock attachment) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? attachment.tintRgb() : 0xFFFFFF, attachmentItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem shapedItem
                    && shapedItem.getBlock() instanceof com.gregtech.gregtech.block.tool.ShapedToolBlock shaped) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? shaped.tintRgb() : 0xFFFFFF, shapedItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem magnetItem
                    && magnetItem.getBlock() instanceof com.gregtech.gregtech.block.energy.MagnetBlock magnet) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? magnet.tintRgb() : 0xFFFFFF, magnetItem);
            } else if (entry.get() instanceof MaterialBlockItem blockItem && blockItem.material() != null) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? blockItem.getTintColor() : 0xFFFFFF, blockItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem panelItem
                    && panelItem.getBlock() instanceof com.gregtech.gregtech.block.misc.PanelBlock panel) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? panel.tintRgb() : 0xFFFFFF, panelItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem barsItem
                    && barsItem.getBlock() instanceof com.gregtech.gregtech.block.misc.BarsBlock bars) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? bars.tintRgb() : 0xFFFFFF, barsItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem shelfItem
                    && shelfItem.getBlock() instanceof com.gregtech.gregtech.block.BookShelfBlock shelf) {
                event.register((stack, tintIndex) -> tintIndex == 0 && shelf.variant().metal()
                        ? shelf.variant().material().getColor() : 0xFFFFFF, shelfItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem ropeItem
                    && ropeItem.getBlock() instanceof com.gregtech.gregtech.block.tool.RopeBlock rope) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? rope.tintRgb() : 0xFFFFFF, ropeItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem explosiveItem
                    && explosiveItem.getBlock()
                    instanceof com.gregtech.gregtech.block.tool.DynamiteBlock explosive) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? explosive.tintRgb() : 0xFFFFFF, explosiveItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem pileItem
                    && (pileItem.getBlock() instanceof com.gregtech.gregtech.block.misc.PileBlock
                        || pileItem.getBlock() instanceof com.gregtech.gregtech.block.misc.CoinPileBlock)) {
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
                        tintIndex == 0 ? com.gregtech.gregtech.registry.GTMultiblocks.tintOf(partItem.getBlock()) : 0xFFFFFF, partItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem toolItem
                    && toolItem.getBlock() instanceof com.gregtech.gregtech.block.tool.ManualToolBlock toolBlock) {
                event.register((stack, tintIndex) -> toolBlock.tint(tintIndex), toolItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem crankItem
                    && crankItem.getBlock() instanceof com.gregtech.gregtech.block.tool.CrankBlock) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? com.gregtech.gregtech.block.tool.CrankBlock.tintRgb() : 0xFFFFFF, crankItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem storageItem
                    && storageItem.getBlock() instanceof com.gregtech.gregtech.block.inventory.MassStorageBlock storageBlock
                    && storageBlock.material() != null) {
                int tint = storageBlock.material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, storageItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem chestItem
                    && chestItem.getBlock() instanceof com.gregtech.gregtech.block.inventory.MetalChestBlock chestBlock
                    // GT6's loot chests use their own (already coloured) texture, not the material tint
                    && !(chestBlock instanceof com.gregtech.gregtech.block.inventory.LootChestBlock)) {
                int tint = chestBlock.material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, chestItem);
            } else if (entry.get() instanceof net.minecraft.world.item.BlockItem nodeItem
                    && nodeItem.getBlock() instanceof com.gregtech.gregtech.block.energy.EnergyNodeBlock nodeBlock) {
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
        com.gregtech.gregtech.registry.GTBoilers.all().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get().asItem());
            }
        });
        // Axle item tints
        com.gregtech.gregtech.registry.GTAxles.all().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get().asItem());
            }
        });
        // Gearbox item tints
        com.gregtech.gregtech.registry.GTGearboxes.allGearboxes().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get().asItem());
            }
        });
        // Pump item tints
        com.gregtech.gregtech.registry.GTPumps.all().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get().asItem());
            }
        });
        // Rotation transformer item tints
        com.gregtech.gregtech.registry.GTGearboxes.allTransformers().forEach(entry -> {
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
                        if(com.gregtech.gregtech.api.fluid.FluidDisplayBinding.hasPayload(stack)) {
                            var actual=com.gregtech.gregtech.api.fluid.FluidDisplayBinding.resolve(stack);
                            if(!actual.isEmpty()) return net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(actual.getFluid()).getTintColor(actual);
                        }
                        return FluidAppearance.appearance(fluidItem.fluidEntry()).tint();
                    }, fluidItem);
                });
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        for(var entry:java.util.List.of(com.gregtech.gregtech.registry.GTMiscBlocks.FILTER_ITEMS,com.gregtech.gregtech.registry.GTMiscBlocks.FILTER_FLUIDS,com.gregtech.gregtech.registry.GTMiscBlocks.FILTER_ITEMS_FLUIDS,com.gregtech.gregtech.registry.GTMiscBlocks.FILTER_OREDICT))event.register((state,level,pos,index)->index==0?com.gregtech.gregtech.content.material.Materials.SteelGalvanized.getColor():0xFFFFFF,entry.get());
        com.gregtech.gregtech.registry.GTMiscBlocks.SOURCE_EXTENDERS.values().forEach(entry->
                event.register((state,level,pos,index)->index==0?entry.get().spec().material().getColor():0xFFFFFF,entry.get()));
        for (var rod : com.gregtech.gregtech.registry.GTFuelRods.ALL) {
            event.register((state, level, pos, tint) -> tint == 0 ? rod.get().tintRgb() : 0xFFFFFF, rod.get().getBlock());
        }
        GTBlocks.BLOCKS.getEntries().forEach(entry -> {
            Block block = entry.get();
            if (block instanceof com.gregtech.gregtech.block.plant.BushBlock) {
                event.register((state,level,pos,tint)->((com.gregtech.gregtech.block.plant.BushBlock)state.getBlock()).tintColour(tint,state.getValue(com.gregtech.gregtech.block.plant.BushBlock.STAGE)),
                        block);
            } else if (block instanceof com.gregtech.gregtech.block.tool.PortableContainerBlock vessel) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? vessel.spec().material().getColor() : 0xFFFFFF, block);
            } else if (block instanceof com.gregtech.gregtech.block.energy.ZpmModuleBlock) {
                event.register((state,world,pos,index)->state.getValue(com.gregtech.gregtech.block.energy.ZpmModuleBlock.CHARGE)>0?0xFFDD00:0x806E00,block);
            } else if (block instanceof com.gregtech.gregtech.block.energy.ZpmDischargerBlock) {
                event.register((state,world,pos,index)->index==0?com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Osmiridium").getColor():state.getValue(com.gregtech.gregtech.block.energy.ZpmDischargerBlock.ACTIVE)?0xFFDD00:0x804000,block);
            } else if (block instanceof com.gregtech.gregtech.block.energy.LaserConverterBlock laser) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? laser.spec().tint() : 0xFFFFFF, block);
            } else if (block instanceof com.gregtech.gregtech.block.machine.LogisticsCoreControllerBlock) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? com.gregtech.gregtech.content.material.Materials.SteelGalvanized.getColor()
                        : 0xFFFFFF, block);
            } else if (block instanceof com.gregtech.gregtech.block.misc.SpikeBlock spike) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? spike.materialTint(state.getValue(com.gregtech.gregtech.block.misc.SpikeBlock.SECONDARY))
                        : 0xFFFFFF, block);
            } else if (block instanceof com.gregtech.gregtech.block.misc.ConcreteBlock) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? com.gregtech.gregtech.block.misc.ConcreteBlock.tint(
                                state.getValue(com.gregtech.gregtech.block.misc.ConcreteBlock.COLOR))
                        : 0xFFFFFF, block);
            } else if (block instanceof com.gregtech.gregtech.block.misc.ColoredGlassBlock) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? com.gregtech.gregtech.block.misc.ConcreteBlock.tint(
                                state.getValue(com.gregtech.gregtech.block.misc.ColoredGlassBlock.COLOR))
                        : 0xFFFFFF, block);
            } else if (block instanceof com.gregtech.gregtech.block.tool.AdvancedButtonBlock) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? com.gregtech.gregtech.content.material.Materials.TinAlloy.getColor()
                        : 0xFFFFFF, block);
            } else if (isReactor(block)) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? com.gregtech.gregtech.content.material.Materials.Lead.getColor() : 0xFFFFFF, block);
            } else if (block instanceof com.gregtech.gregtech.block.tool.FluidAttachmentBlock attachment) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? attachment.tintRgb() : 0xFFFFFF, block);
            } else if (block instanceof com.gregtech.gregtech.block.tool.ShapedToolBlock shaped) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? shaped.tintRgb() : 0xFFFFFF, block);
            } else if (block instanceof com.gregtech.gregtech.block.energy.MagnetBlock magnet) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? magnet.tintRgb() : 0xFFFFFF, block);
            } else if (block instanceof MaterialBlockLike materialBlock) {
                int color = materialBlock.material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? color : 0xFFFFFF, block);
            } else if (block instanceof com.gregtech.gregtech.block.misc.PanelBlock panel) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? panel.tintRgb() : 0xFFFFFF, panel);
            } else if (block instanceof com.gregtech.gregtech.block.misc.PileBlock) {
                // GT6 tints the pile's own greyscale textures with the stored material's colour
                // (`BlockTextureDefault.get(sTextureSides, mMaterial.fRGBaSolid, ...)`,
                // MultiTileEntityIngot.java:46-47, MultiTileEntityPlate.java:46-47); the port reads the
                // colour out of the pile's block entity instead.
                event.register((state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) return 0xFFFFFF;
                    if (level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.misc.PileBlockEntity pile) {
                        var material = com.gregtech.gregtech.blockentity.misc.PileBlockEntity.materialOf(pile.stored());
                        if (material != null) return material.getColor();
                    }
                    return 0xFFFFFF;
                }, block);
            } else if (block instanceof com.gregtech.gregtech.block.misc.CoinPileBlock) {
                // The same for GT6's coins, which its renderer colours with `mMaterial.fRGBaSolid`
                // (MultiTileEntityCoin.java:430-435); an unbound pile already carries the port's default
                // copper coin, so the colour always comes from the pile's own coin item.
                event.register((state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) return 0xFFFFFF;
                    if (level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.misc.CoinPileBlockEntity pile
                            && com.gregtech.gregtech.block.misc.CoinPileBlock.isCoin(pile.coinItem())) {
                        return com.gregtech.gregtech.item.MaterialItem.getMaterial(pile.coinItem()).getColor();
                    }
                    return 0xFFFFFF;
                }, block);
            } else if (block instanceof com.gregtech.gregtech.block.tool.RopeBlock rope) {
                // GT6 tints the shared greyscale rope texture with the rope material's colour
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? rope.tintRgb() : 0xFFFFFF, rope);
            } else if (block instanceof com.gregtech.gregtech.block.misc.BarsBlock bars) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? bars.tintRgb() : 0xFFFFFF, bars);
            } else if (block instanceof com.gregtech.gregtech.block.BookShelfBlock shelf) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 && shelf.variant().metal()
                        ? shelf.variant().material().getColor() : 0xFFFFFF, shelf);
            } else if (block instanceof com.gregtech.gregtech.block.tool.DynamiteBlock explosive) {
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? explosive.tintRgb() : 0xFFFFFF, explosive);
            }
        });
        com.gregtech.gregtech.registry.GTMiscBlocks.all().forEach(entry -> {
            if (!(entry.get() instanceof com.gregtech.gregtech.block.misc.PanelBlock)
                    && !(entry.get() instanceof com.gregtech.gregtech.block.misc.AutoToolBlock)
                    && !(entry.get() instanceof com.gregtech.gregtech.block.misc.LongDistEndpointBlock)
                    && !(entry.get() instanceof com.gregtech.gregtech.block.misc.LongDistanceTransformerBlock))
                event.register((state, level, pos, index) -> index == 0 ? com.gregtech.gregtech.content.tool.MiscBlockAppearance.tint(entry.get()) : 0xFFFFFF, entry.get());
        });
        // Surface rocks: tint from the block entity's material (vein indicators).
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || level == null || pos == null) return 0xFFFFFF;
            if (level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.RockBlockEntity rock) {
                var material = com.gregtech.gregtech.api.material.GTMaterialRegistry.get(rock.getMaterial());
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
        GTTanks.all().forEach(entry -> {
            if (entry.isPresent()) {
                TankBlock tank = entry.get();
                int tint = tank.spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, tank);
            }
        });
        com.gregtech.gregtech.registry.GTBoilers.all().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        com.gregtech.gregtech.registry.GTMultiblocks.texturedBlocks().forEach(entry -> {
            if (entry != null && entry.isPresent()) {
                event.register((state, level, pos, tintIndex) ->
                        tintIndex == 0 ? com.gregtech.gregtech.registry.GTMultiblocks.tintOf(entry.get()) : 0xFFFFFF, entry.get());
            }
        });
        com.gregtech.gregtech.registry.GTToolBlocks.manual().forEach(entry -> {
            if (entry.isPresent()) {
                event.register((state, level, pos, tintIndex) -> entry.get().tint(tintIndex), entry.get());
            }
        });
        if (com.gregtech.gregtech.registry.GTToolBlocks.CRANK != null
                && com.gregtech.gregtech.registry.GTToolBlocks.CRANK.isPresent()) {
            event.register((state, level, pos, tintIndex) ->
                    tintIndex == 0 ? com.gregtech.gregtech.block.tool.CrankBlock.tintRgb() : 0xFFFFFF, com.gregtech.gregtech.registry.GTToolBlocks.CRANK.get());
        }
        GTEnergyNodes.all().forEach(entry -> {
            if (entry.isPresent()) {
                var node = entry.get();
                int tint = node.spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, node);
            }
        });
        com.gregtech.gregtech.registry.GTStorage.MASS_STORAGES.forEach(entry -> {
            if (entry.isPresent() && entry.get().material() != null) {
                int tint = entry.get().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        com.gregtech.gregtech.registry.GTStorage.LOGISTICS_MASS_STORAGES.forEach(entry -> {
            if (entry.isPresent()) {
                int tint = com.gregtech.gregtech.content.material.Materials.Black.getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        com.gregtech.gregtech.registry.GTStorage.METAL_CHESTS.forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        java.util.stream.Stream.concat(com.gregtech.gregtech.registry.GTMiscBlocks.ADVANCED_CRAFTING_TABLES.stream(),com.gregtech.gregtech.registry.GTMiscBlocks.CHARGING_CRAFTING_TABLES.stream()).forEach(entry -> event.register((state,level,pos,index) -> index==0?entry.get().material().getColor():0xFFFFFF,entry.get()));
        com.gregtech.gregtech.registry.GTStorage.BOTTLE_CRATES.forEach(entry -> event.register((state,level,pos,index) -> index == 0 ? entry.get().tintRgb() : 0xFFFFFF, entry.get()));
        com.gregtech.gregtech.registry.GTStorage.DRAWERS.forEach(entry -> event.register((state,level,pos,index) -> index == 0 ? entry.get().material().getColor() : 0xFFFFFF, entry.get()));
        java.util.stream.Stream.concat(com.gregtech.gregtech.registry.GTStorage.SAFES.stream(), com.gregtech.gregtech.registry.GTStorage.KEY_SAFES.stream())
                .forEach(entry -> event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? entry.get().material().getColor() : 0xFFFFFF, entry.get()));
        com.gregtech.gregtech.registry.GTSignalWires.all().forEach(entry -> {
            var wire = entry.get();
            event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? wire.material().getColor()
                    : tintIndex == 1 ? 0x604040 : 0xFFFFFF, wire);
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
        com.gregtech.gregtech.registry.GTAxles.all().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        // Gearboxes: tint via material color
        com.gregtech.gregtech.registry.GTGearboxes.allGearboxes().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        // Pumps: tint via material color
        com.gregtech.gregtech.registry.GTPumps.all().forEach(entry -> {
            if (entry.isPresent()) {
                int tint = entry.get().spec().material().getColor();
                event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFF, entry.get());
            }
        });
        // Rotation transformers: tint via material color
        com.gregtech.gregtech.registry.GTGearboxes.allTransformers().forEach(entry -> {
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
                        case 1 -> level!=null&&pos!=null&&level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.machine.KineticSteamEngineBlockEntity steam
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
                                && level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.machine.PistonEngineBlockEntity piston
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
        MaterialFluidVisuals.bootstrap();
        MaterialFluidVisuals.linkAllMoltenMaterials();
        event.registerBlockEntityRenderer(GTBlockEntities.SMELTING_CRUCIBLE.get(), SmeltingCrucibleRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.LARGE_CRUCIBLE.get(), LargeCrucibleRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.PORTABLE_CONTAINER.get(), CapsuleCellRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.MOLD.get(), SmelteryHullRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.MOLD_BASIN.get(), SmelteryHullRenderer::new);
        // (pipe/wire arms render via PipeWireBakedModel — no BERs needed)

        event.registerBlockEntityRenderer(GTBlockEntities.METAL_CHEST.get(), MetalChestRenderer::new);
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
        for (var blockRO : com.gregtech.gregtech.api.machine.MachineRegistry.basicMachines()) {
            if (!blockRO.isPresent()) continue;
            BlockEntityType<?> beType = blockRO.get().beType();
            if (beType == null) continue;
            @SuppressWarnings("unchecked")
            var typed = (BlockEntityType<com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity>) (Object) beType;
            event.registerBlockEntityRenderer(typed, MachineCoverRenderer::new);
        }

        // Engine BERs — heat-colored core overlay
        var steamEngines = com.gregtech.gregtech.api.machine.MachineRegistry.steamEngines();
        var strongSteamEngines = com.gregtech.gregtech.api.machine.MachineRegistry.strongSteamEngines();
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
        event.enqueueWork(()->com.gregtech.gregtech.registry.GTChemicalBatteries.allRegistered().forEach(entry->
                net.minecraft.client.renderer.item.ItemProperties.register(entry.get().asItem(),
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","battery_charge"),
                        (stack,level,entity,seed)->{
                            var item=(com.gregtech.gregtech.item.ChemicalBatteryItem)stack.getItem();
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
        ItemBlockRenderTypes.setRenderLayer(com.gregtech.gregtech.registry.GTDecorBlocks.GLASS_CLEAR.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(com.gregtech.gregtech.registry.GTDecorBlocks.GLASS_GLOW.get(), RenderType.translucent());
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
            if (com.gregtech.gregtech.api.fluid.GTWaterParity.isWorldWater(registration.getKey())
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
            MenuScreens.register(GTMenuTypes.ADVANCED_CRAFTING.get(),com.gregtech.gregtech.client.gui.AdvancedCraftingScreen::new);
            MenuScreens.register(GTMenuTypes.GARBAGE_DUMP.get(), HopperScreen::new);
            MenuScreens.register(GTMenuTypes.BASIC_MACHINE.get(), BasicMachineScreen::new);
            MenuScreens.register(GTMenuTypes.FILTER.get(),com.gregtech.gregtech.client.gui.FilterScreen::new);
            MenuScreens.register(GTMenuTypes.USB_SWITCH.get(), com.gregtech.gregtech.client.gui.DataSwitchScreen::new);
            MenuScreens.register(GTMenuTypes.HDD_SWITCH.get(), com.gregtech.gregtech.client.gui.DataSwitchScreen::new);
            // GT6's bumbliary and advanced bumbliary machines.
            MenuScreens.register(GTMenuTypes.BUMBLIARY.get(), com.gregtech.gregtech.client.gui.BumbliaryScreen::new);
            MenuScreens.register(GTMenuTypes.ADVANCED_BUMBLIARY.get(), com.gregtech.gregtech.client.gui.BumbliaryScreen::new);
        });
    }
    private static int steamPressureColor(int state){
        int n=Math.max(0,Math.min(31,state));
        return n<16?((n*17)<<8)|(255-n*17):((n-16)*17<<16)|((255-(n-16)*17)<<8);
    }
}
