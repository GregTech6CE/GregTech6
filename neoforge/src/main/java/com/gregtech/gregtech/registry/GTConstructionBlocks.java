package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.block.misc.*;import net.minecraft.world.item.Item;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockBehaviour;import net.minecraft.world.level.material.MapColor;import net.neoforged.neoforge.registries.DeferredHolder;
public final class GTConstructionBlocks {private GTConstructionBlocks(){}public static DeferredHolder<Block,AsphaltBlock> ASPHALT;public static DeferredHolder<Block,ConcreteBlock> CONCRETE,CONCRETE_REINFORCED;public static DeferredHolder<Block,CFoamBlock> CFOAM,CFOAM_FRESH;public static DeferredHolder<Block,ColoredGlassBlock> GLASS_CLEAR,GLASS_GLOW;
 public static void initialize(){
 ASPHALT=GTBlocks.BLOCKS.register("asphalt",()->new AsphaltBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3,3).requiresCorrectToolForDrops().sound(SoundType.STONE)));item("asphalt",ASPHALT);
 CONCRETE=concrete("concrete",false,1.5f,20);CONCRETE_REINFORCED=concrete("concrete_reinforced",true,6,80);
 CFOAM=GTBlocks.BLOCKS.register("cfoam",()->new CFoamBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1,1).requiresCorrectToolForDrops().sound(SoundType.WOOL)));item("cfoam",CFOAM);
 CFOAM_FRESH=GTBlocks.BLOCKS.register("cfoam_fresh",()->new CFoamBlock(true,BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(.5f,.5f).requiresCorrectToolForDrops().sound(SoundType.WOOL)));item("cfoam_fresh",CFOAM_FRESH);
 GLASS_CLEAR=glass("glass_clear",false);GLASS_GLOW=glass("glass_glow",true);
 }
 private static DeferredHolder<Block,ConcreteBlock> concrete(String id,boolean reinforced,float hardness,float resistance){var b=GTBlocks.BLOCKS.register(id,()->new ConcreteBlock(reinforced,BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(hardness,resistance).requiresCorrectToolForDrops().sound(SoundType.STONE)));GTBlocks.BLOCK_ITEMS.register(id,()->new ConcreteBlockItem(b.get(),new Item.Properties()));return b;}
 private static DeferredHolder<Block,ColoredGlassBlock> glass(String id,boolean glow){var b=GTBlocks.BLOCKS.register(id,()->new ColoredGlassBlock(glow,BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(.75f,5).sound(SoundType.GLASS).lightLevel(state->glow?15:0).noOcclusion().isValidSpawn((s,l,p,e)->false)));GTBlocks.BLOCK_ITEMS.register(id,()->new ColoredGlassBlockItem(b.get(),new Item.Properties()));return b;}
 private static void item(String id,DeferredHolder<Block,? extends Block> b){GTBlocks.BLOCK_ITEMS.register(id,()->new net.minecraft.world.item.BlockItem(b.get(),new Item.Properties()));}
}
