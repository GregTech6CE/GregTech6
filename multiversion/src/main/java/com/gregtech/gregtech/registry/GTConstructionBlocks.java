package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.block.misc.*;import net.minecraft.world.item.Item;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockBehaviour;import net.minecraft.world.level.material.MapColor;
public final class GTConstructionBlocks {private GTConstructionBlocks(){}
 private static final java.util.List<RegisteredBlock<? extends Block>> ALL=new java.util.ArrayList<>();
 public static java.util.List<RegisteredBlock<? extends Block>> all(){return java.util.List.copyOf(ALL);}public static RegisteredBlock<AsphaltBlock> ASPHALT;public static RegisteredBlock<CFoamSlabBlock> CFOAM_SLAB;public static RegisteredBlock<ConcreteBlock> CONCRETE,CONCRETE_REINFORCED;public static RegisteredBlock<CFoamBlock> CFOAM,CFOAM_FRESH;public static RegisteredBlock<ColoredGlassBlock> GLASS_CLEAR,GLASS_GLOW;
 public static void initialize(){if(!ALL.isEmpty())throw new IllegalStateException("Construction blocks registered twice");
 var clearSlab=RegisteredBlock.registerBlock("glass_clear_slab",()->new ColoredGlassSlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.5f).sound(SoundType.GLASS).noOcclusion()));item("glass_clear_slab",clearSlab);
 var glowSlab=RegisteredBlock.registerBlock("glass_glow_slab",()->new ColoredGlassSlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.5f).sound(SoundType.GLASS).noOcclusion().lightLevel(state->15)));item("glass_glow_slab",glowSlab);
 CFOAM_SLAB=RegisteredBlock.registerBlock("cfoam_slab",()->new CFoamSlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1,1).requiresCorrectToolForDrops().sound(SoundType.WOOL)));item("cfoam_slab",CFOAM_SLAB);
 ASPHALT=RegisteredBlock.registerBlock("asphalt",()->new AsphaltBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3,3).requiresCorrectToolForDrops().sound(SoundType.STONE)));item("asphalt",ASPHALT);
 CONCRETE=concrete("concrete",false,1.5f,20);CONCRETE_REINFORCED=concrete("concrete_reinforced",true,6,80);
 CFOAM=RegisteredBlock.registerBlock("cfoam",()->new CFoamBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1,1).requiresCorrectToolForDrops().sound(SoundType.WOOL)));item("cfoam",CFOAM);
 CFOAM_FRESH=RegisteredBlock.registerBlock("cfoam_fresh",()->new com.gregtech.gregtech.block.misc.FreshCFoamBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(.5f,.5f).requiresCorrectToolForDrops().sound(SoundType.WOOL)));item("cfoam_fresh",CFOAM_FRESH);
 GLASS_CLEAR=glass("glass_clear",false);GLASS_GLOW=glass("glass_glow",true);
 }
 private static RegisteredBlock<ConcreteBlock> concrete(String id,boolean reinforced,float hardness,float resistance){var b=RegisteredBlock.registerBlock(id,()->new ConcreteBlock(reinforced,BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(hardness,resistance).requiresCorrectToolForDrops().sound(SoundType.STONE)));ALL.add(b);GTBlocks.BLOCK_ITEMS.register(id,()->new ConcreteBlockItem(b.get(),new Item.Properties()));return b;}
 private static RegisteredBlock<ColoredGlassBlock> glass(String id,boolean glow){var b=RegisteredBlock.registerBlock(id,()->new ColoredGlassBlock(glow,BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(.75f,5).sound(SoundType.GLASS).lightLevel(state->glow?15:0).noOcclusion().isValidSpawn((s,l,p,e)->false)));ALL.add(b);GTBlocks.BLOCK_ITEMS.register(id,()->new ColoredGlassBlockItem(b.get(),new Item.Properties()));return b;}
 private static void item(String id,RegisteredBlock<? extends Block> b){ALL.add(b);GTBlocks.BLOCK_ITEMS.register(id,()->new ColoredConstructionBlockItem(b.get(),new Item.Properties()));}
}
