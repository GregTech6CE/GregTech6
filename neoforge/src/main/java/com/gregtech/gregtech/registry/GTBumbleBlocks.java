package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.block.tool.BumbliaryBlock;
import com.gregtech.gregtech.block.misc.BumbleHiveBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.*;
public final class GTBumbleBlocks {
 private GTBumbleBlocks(){}
 public static final DeferredHolder<net.minecraft.world.level.block.Block,BumbleHiveBlock> BUMBLE_HIVE=GTBlocks.BLOCKS.register("bumble_hive",()->new BumbleHiveBlock(BumbleHiveBlock.defaultProperties()));
 private static final java.util.Map<DyeColor,DeferredHolder<net.minecraft.world.level.block.Block,BumbleHiveBlock>> HIVE_VARIANTS=registerHives();
 private static java.util.Map<DyeColor,DeferredHolder<net.minecraft.world.level.block.Block,BumbleHiveBlock>> registerHives(){var hives=new java.util.EnumMap<DyeColor,DeferredHolder<net.minecraft.world.level.block.Block,BumbleHiveBlock>>(DyeColor.class);for(var color:DyeColor.values())hives.put(color,GTBlocks.BLOCKS.register("bumble_hive_"+color.getName(),()->new com.gregtech.gregtech.block.misc.FixedBumbleHiveBlock(BumbleHiveBlock.defaultProperties(),color)));return hives;}
 public static BumbleHiveBlock hive(DyeColor color){return HIVE_VARIANTS.get(color).get();}
 public static net.minecraft.world.level.block.Block[] allHives(){var hives=new java.util.ArrayList<net.minecraft.world.level.block.Block>();hives.add(BUMBLE_HIVE.get());HIVE_VARIANTS.values().forEach(h->hives.add(h.get()));return hives.toArray(net.minecraft.world.level.block.Block[]::new);}
 public static final DeferredHolder<net.minecraft.world.level.block.Block,BumbliaryBlock> BUMBLIARY=GTBlocks.BLOCKS.register("bumbliary",()->new BumbliaryBlock("bumbliary",false,BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(5,5).sound(SoundType.WOOD)));
 public static final DeferredHolder<net.minecraft.world.level.block.Block,BumbliaryBlock> ADVANCED_BUMBLIARY=GTBlocks.BLOCKS.register("advanced_bumbliary",()->new BumbliaryBlock("advanced_bumbliary",true,BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6,6).sound(SoundType.WOOD)));
 public static void initialize(){for(var holder:java.util.List.of(BUMBLE_HIVE,BUMBLIARY,ADVANCED_BUMBLIARY))GTBlocks.BLOCK_ITEMS.register(holder.getId().getPath(),()->new BlockItem(holder.get(),new Item.Properties().stacksTo(holder==BUMBLE_HIVE?64:16)));for(var holder:HIVE_VARIANTS.values())GTBlocks.BLOCK_ITEMS.register(holder.getId().getPath(),()->new BlockItem(holder.get(),new Item.Properties()));}
}
