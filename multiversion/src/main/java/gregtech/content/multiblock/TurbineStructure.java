package gregtech.content.multiblock;
import gregtech.internal.multiblock.MultiblockLayout;
public final class TurbineStructure {private TurbineStructure(){}public static final MultiblockLayout LAYOUT=MultiblockLayout.fromShared(SharedTurbineStructure.LAYOUT);public static gregtech.internal.multiblock.MultiblockLayout.Role role(int right,int up,int back){return gregtech.internal.multiblock.MultiblockLayout.Role.valueOf(SharedTurbineStructure.role(right,up,back).name());}}
