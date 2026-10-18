package com.gregtech.gregtech.item.behavior;
import net.minecraft.core.*;import net.minecraft.world.level.Level;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.ItemStack;
/** Original extinguisher callback, shared by actual fuse-bearing block entities and future spray items. */
public interface Extinguishable {long onExtinguish(Level level,BlockPos pos,Direction side,Player player,ItemStack can,boolean sneaking,float x,float y,float z);}
