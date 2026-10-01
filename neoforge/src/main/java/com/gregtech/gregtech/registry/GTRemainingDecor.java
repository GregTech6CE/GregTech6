package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.block.misc.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockBehaviour;import net.minecraft.world.level.material.MapColor;import net.neoforged.neoforge.registries.DeferredHolder;import java.util.*;
public final class GTRemainingDecor {private GTRemainingDecor(){}public static final List<DeferredHolder<Block,PanelBlock>> PANELS=new ArrayList<>();
public static DeferredHolder<Block,RoadStripeRailBlock> RAILROAD;public static DeferredHolder<Block,GregLanternBlock> GREG_LANTERN;
private static <T extends Block> DeferredHolder<Block,T> reg(String id,java.util.function.Supplier<T> factory){var b=GTBlocks.BLOCKS.register(id,factory);GTBlocks.BLOCK_ITEMS.register(id,()->new net.minecraft.world.item.BlockItem(b.get(),new net.minecraft.world.item.Item.Properties()));return b;}
public static void initialize(){
 reg("loot_crate",()->new com.gregtech.gregtech.block.LootCrateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(4,4).requiresCorrectToolForDrops().sound(SoundType.WOOD)));
 for(var v:com.gregtech.gregtech.content.transport.PanelCatalog.ALL)PANELS.add(reg(v.id(),()->new PanelBlock(v.material(),v.tint(),BlockBehaviour.Properties.of().mapColor(switch(v.mapColor()){case "WOOD"->MapColor.WOOD;case "STONE"->MapColor.STONE;case "WOOL"->MapColor.WOOL;case "COLOR_BLACK"->MapColor.COLOR_BLACK;default->MapColor.COLOR_GRAY;}).strength(v.hardness(),v.hardness()).requiresCorrectToolForDrops())));
 RAILROAD=reg("railroad",()->new RoadStripeRailBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(.35F,20F).sound(SoundType.METAL)));
 GREG_LANTERN=reg("greg_lantern",()->new GregLanternBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1F,1F).sound(SoundType.WOOD).lightLevel(s->15).noOcclusion()));
}
}
