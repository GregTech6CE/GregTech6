package com.gregtech.gregtech.platform.neoforge.smeltery;

import com.gregtech.gregtech.api.machine.InitialSmelteryDefinitions;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Real GT6 block/BE identities; domain specifications come exclusively from core. */
public final class SmelteryRegistries {
    private SmelteryRegistries() {}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("gregtech");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("gregtech");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "gregtech");
    public static final DeferredItem<SmelteryToolItem> CHISEL = com.gregtech.gregtech.registry.GTToolItems.CHISEL;
    public static final DeferredItem<SmelteryToolItem> PINCERS = com.gregtech.gregtech.registry.GTToolItems.PINCERS;

    private static final java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.SolidBurningBoxBlock>> SOLID_BOXES=new java.util.ArrayList<>();
    private static final java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.BurningBoxBlock>> FUEL_BOXES=new java.util.ArrayList<>();
    public static java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.SolidBurningBoxBlock>> solidBoxes(){return java.util.List.copyOf(SOLID_BOXES);}
    public static java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.BurningBoxBlock>> fuelBoxes(){return java.util.List.copyOf(FUEL_BOXES);}
    private static BlockBehaviour.Properties boxProperties(com.gregtech.gregtech.api.machine.MachineSpec spec){return BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(spec.hardness(),spec.blastResistance()).requiresCorrectToolForDrops().lightLevel(state->state.getValue(com.gregtech.gregtech.block.machine.GTFacingMachineBlock.LIT)?13:0);}
    private static DeferredBlock<com.gregtech.gregtech.block.machine.SolidBurningBoxBlock> solidBox(com.gregtech.gregtech.api.machine.MachineSpec spec){var holder=BLOCKS.register(spec.id(),()->new com.gregtech.gregtech.block.machine.SolidBurningBoxBlock(spec,boxProperties(spec)));SOLID_BOXES.add(holder);ITEMS.register(spec.id(),()->new com.gregtech.gregtech.block.machine.BurningBoxBlockItem(holder.get(),new Item.Properties().stacksTo(16),spec));return holder;}
    private static void fuelBox(com.gregtech.gregtech.api.machine.BurningBoxDefinitions.Entry entry){var spec=entry.spec();var holder=BLOCKS.register(spec.id(),()->new com.gregtech.gregtech.block.machine.BurningBoxBlock(entry.fuelType(),spec,boxProperties(spec)));FUEL_BOXES.add(holder);ITEMS.register(spec.id(),()->new com.gregtech.gregtech.block.machine.BurningBoxBlockItem(holder.get(),new Item.Properties().stacksTo(16),spec));}
    public static final DeferredBlock<com.gregtech.gregtech.block.machine.SolidBurningBoxBlock> BRICK_BURNING_BOX=solidBox(InitialSmelteryDefinitions.brickBurningBox());
    private static final java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock>> CRUCIBLES=new java.util.ArrayList<>();
    public static final DeferredBlock<com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock> CERAMIC_CRUCIBLE=crucible(InitialSmelteryDefinitions.ceramicCrucible());
    private static DeferredBlock<com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock> crucible(com.gregtech.gregtech.api.machine.CrucibleSpec spec){var block=BLOCKS.register(spec.id(),()->new com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock(spec,com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock.defaultProperties(spec).noOcclusion()));CRUCIBLES.add(block);return block;}
    public static java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock>> crucibles(){return java.util.List.copyOf(CRUCIBLES);}
    private static final java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.MoldBlock>> MOLDS=new java.util.ArrayList<>();
    private static final java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.MoldBasinBlock>> BASINS=new java.util.ArrayList<>();
    private static final java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.CrucibleFaucetBlock>> FAUCETS=new java.util.ArrayList<>();
    private static final java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.CrucibleCrossingBlock>> CROSSINGS=new java.util.ArrayList<>();
    public static java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.MoldBlock>> molds(){return java.util.List.copyOf(MOLDS);}
    public static java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.MoldBasinBlock>> basins(){return java.util.List.copyOf(BASINS);}
    public static java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.CrucibleFaucetBlock>> faucets(){return java.util.List.copyOf(FAUCETS);}
    public static java.util.List<DeferredBlock<com.gregtech.gregtech.block.machine.CrucibleCrossingBlock>> crossings(){return java.util.List.copyOf(CROSSINGS);}
    private static DeferredBlock<com.gregtech.gregtech.block.machine.MoldBlock> mold(com.gregtech.gregtech.api.machine.CrucibleSpec base){var spec=com.gregtech.gregtech.api.machine.SmelteryCompanionDefinitions.of(base,com.gregtech.gregtech.api.machine.SmelteryCompanionDefinitions.Kind.MOLD);var holder=BLOCKS.register(spec.id(),()->new com.gregtech.gregtech.block.machine.MoldBlock(spec,com.gregtech.gregtech.block.machine.MoldBlock.defaultProperties(spec)));MOLDS.add(holder);ITEMS.register(spec.id(),()->new com.gregtech.gregtech.block.machine.CrucibleHullBlockItem(holder.get(),new Item.Properties().stacksTo(16),spec));return holder;}
    public static final DeferredBlock<com.gregtech.gregtech.block.machine.MoldBlock> CERAMIC_MOLD=mold(InitialSmelteryDefinitions.ceramicCrucible());
    private static void otherCompanions(com.gregtech.gregtech.api.machine.CrucibleSpec base){
     var basin=com.gregtech.gregtech.api.machine.SmelteryCompanionDefinitions.of(base,com.gregtech.gregtech.api.machine.SmelteryCompanionDefinitions.Kind.BASIN);var b=BLOCKS.register(basin.id(),()->new com.gregtech.gregtech.block.machine.MoldBasinBlock(basin,com.gregtech.gregtech.block.machine.MoldBasinBlock.defaultProperties(basin)));BASINS.add(b);ITEMS.register(basin.id(),()->new com.gregtech.gregtech.block.machine.CrucibleHullBlockItem(b.get(),new Item.Properties().stacksTo(16),basin));
     var faucet=com.gregtech.gregtech.api.machine.SmelteryCompanionDefinitions.of(base,com.gregtech.gregtech.api.machine.SmelteryCompanionDefinitions.Kind.FAUCET);var f=BLOCKS.register(faucet.id(),()->new com.gregtech.gregtech.block.machine.CrucibleFaucetBlock(faucet,com.gregtech.gregtech.block.machine.CrucibleFaucetBlock.defaultProperties(faucet)));FAUCETS.add(f);ITEMS.register(faucet.id(),()->new com.gregtech.gregtech.block.machine.CrucibleHullBlockItem(f.get(),new Item.Properties().stacksTo(16),faucet));
     var crossing=com.gregtech.gregtech.api.machine.SmelteryCompanionDefinitions.of(base,com.gregtech.gregtech.api.machine.SmelteryCompanionDefinitions.Kind.CROSSING);var x=BLOCKS.register(crossing.id(),()->new com.gregtech.gregtech.block.machine.CrucibleCrossingBlock(crossing,com.gregtech.gregtech.block.machine.CrucibleCrossingBlock.defaultProperties(crossing)));CROSSINGS.add(x);ITEMS.register(crossing.id(),()->new com.gregtech.gregtech.block.machine.CrucibleHullBlockItem(x.get(),new Item.Properties().stacksTo(16),crossing));
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolidBurningBoxEntity>> SOLID_BURNING_BOX = ENTITIES.register("solid_burning_box",
            () -> BlockEntityType.Builder.of(SolidBurningBoxEntity::new, SOLID_BOXES.stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmeltingCrucibleEntity>> SMELTING_CRUCIBLE = ENTITIES.register("smelting_crucible",
            () -> BlockEntityType.Builder.of(SmeltingCrucibleEntity::new, CRUCIBLES.stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MoldEntity>> MOLD = ENTITIES.register("mold",
            () -> BlockEntityType.Builder.of(MoldEntity::new, MOLDS.stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    static {

        ITEMS.register("smelting_crucible_ceramic", () -> new com.gregtech.gregtech.block.machine.SmeltingCrucibleBlockItem(CERAMIC_CRUCIBLE.get(), new Item.Properties().stacksTo(16), InitialSmelteryDefinitions.ceramicCrucible()));

    }

    public static void register(IEventBus bus) {
        for(var entry:com.gregtech.gregtech.api.machine.BurningBoxDefinitions.all()){if(entry.spec().id().equals("burning_box_solid_brick"))continue;if(entry.fuelType()==com.gregtech.gregtech.api.machine.BurningBoxFuelType.SOLID)solidBox(entry.spec());else fuelBox(entry);}
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Native original burning boxes: solid={} fluid/gas/fluidized={}",SOLID_BOXES.size(),FUEL_BOXES.size());
        bus.addListener(SmelteryRegistries::capabilities);
        for(var spec:com.gregtech.gregtech.api.machine.OriginalCrucibleDefinitions.all()){if(spec.id().equals("smelting_crucible_ceramic"))continue;var holder=crucible(spec);ITEMS.register(spec.id(),()->new com.gregtech.gregtech.block.machine.SmeltingCrucibleBlockItem(holder.get(),new Item.Properties().stacksTo(16),spec));}
        for(var base:com.gregtech.gregtech.api.machine.OriginalCrucibleDefinitions.all()){if(!base.id().equals("smelting_crucible_ceramic"))mold(base);otherCompanions(base);}
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Native original smeltery companions: molds={} basins={} faucets={} crossings={}",MOLDS.size(),BASINS.size(),FAUCETS.size(),CROSSINGS.size());
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Native original smelting crucibles: {}",CRUCIBLES.size());
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
    }
    private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event){
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.BURNING_BOX.get(),com.gregtech.gregtech.blockentity.machine.BurningBoxBlockEntity::itemCapability);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.BURNING_BOX.get(),com.gregtech.gregtech.blockentity.machine.BurningBoxBlockEntity::fluidCapability);
    }
}
