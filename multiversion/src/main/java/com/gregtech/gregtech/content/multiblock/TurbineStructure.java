package com.gregtech.gregtech.content.multiblock;
import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
public final class TurbineStructure {private TurbineStructure(){}public static final MultiblockLayout LAYOUT=MultiblockLayout.fromShared(SharedTurbineStructure.LAYOUT);public static com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role role(int right,int up,int back){return com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.valueOf(SharedTurbineStructure.role(right,up,back).name());}}
