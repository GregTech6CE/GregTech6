package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.block.energy.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockBehaviour;import net.neoforged.neoforge.registries.DeferredHolder;import java.util.List;
public final class GTZpmArtifacts {private GTZpmArtifacts(){}public static DeferredHolder<Block,ZpmModuleBlock> ZPM;public static DeferredHolder<Block,ZpmDischargerBlock> BASIC,ADVANCED,ELITE;
 public static void initialize(){ZPM=GTBlocks.BLOCKS.register("zpm",()->new ZpmModuleBlock(BlockBehaviour.Properties.of().strength(.5f,1)));item("zpm",ZPM);BASIC=discharger("zpm_discharger_basic",false);ADVANCED=discharger("zpm_discharger_advanced",true);ELITE=discharger("zpm_discharger_elite",false);}
 private static DeferredHolder<Block,ZpmDischargerBlock> discharger(String id,boolean electric){var b=GTBlocks.BLOCKS.register(id,()->new ZpmDischargerBlock(BlockBehaviour.Properties.of().strength(4,50).requiresCorrectToolForDrops(),electric));item(id,b);return b;}
 private static void item(String id,java.util.function.Supplier<? extends Block> b){GTBlocks.BLOCK_ITEMS.register(id,()->new net.minecraft.world.item.BlockItem(b.get(),new net.minecraft.world.item.Item.Properties()));}
}
