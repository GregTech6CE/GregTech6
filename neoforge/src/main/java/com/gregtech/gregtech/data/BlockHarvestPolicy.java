package com.gregtech.gregtech.data;

import com.gregtech.gregtech.block.MaterialBlockLike;
import com.gregtech.gregtech.api.machine.GTMachineBlock;
import net.minecraft.world.level.block.*;

/** GT6 Loader_MultiTileEntities material groups, adapted to native harvest tags; both platforms share BlockHarvestNames.
 * Hardness/resistance remain supplied by each material/definition, not a global constant.
 */
public final class BlockHarvestPolicy {
 private BlockHarvestPolicy() {}
 public enum Tool { PICKAXE, AXE, SHOVEL, SWORD, WRENCH, CROWBAR, CUTTER, SHEARS, HAND }
 public static Tool tool(Block block) {
  if(block instanceof com.gregtech.gregtech.block.misc.LongDistPipeBlock line) return line.isWire()?Tool.CUTTER:Tool.WRENCH;
  if(block instanceof com.gregtech.gregtech.block.misc.LongDistEndpointBlock || block instanceof com.gregtech.gregtech.block.misc.LongDistanceTransformerBlock) return Tool.WRENCH;
  if(block instanceof LiquidBlock || block instanceof BushBlock || block instanceof com.gregtech.gregtech.block.RockBlock || block instanceof com.gregtech.gregtech.block.TwigBlock) return Tool.HAND;
  if(block instanceof com.gregtech.gregtech.block.plant.BaleBlock) return Tool.SWORD;
  if(block instanceof LeavesBlock) return Tool.SHEARS;
  if(block instanceof com.gregtech.gregtech.block.BookShelfBlock shelf) return shelf.variant().metal() ? Tool.WRENCH : Tool.AXE;
  if(block instanceof com.gregtech.gregtech.block.inventory.MassStorageBlock) return Tool.WRENCH;
  if(block instanceof MaterialBlockLike m) {
   if(m.prefix().isCrate())return Tool.CROWBAR;
   if(m.prefix().falling())return Tool.SHOVEL;
   return Tool.PICKAXE;
  }
  if(block instanceof com.gregtech.gregtech.api.energy.WireMaterialLike || block instanceof com.gregtech.gregtech.block.energy.SignalWireBlock) return Tool.CUTTER;
  var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
  String path=id==null?"":id.getPath();
  // Wooden hulls take precedence over machinery implemented by the same base class.
  if(BlockHarvestNames.woodenHull(path)) return Tool.AXE;
  if(block instanceof GTMachineBlock || block instanceof com.gregtech.gregtech.block.energy.EnergyNodeBlock
      || block instanceof com.gregtech.gregtech.block.inventory.SafeBlock
      || block instanceof com.gregtech.gregtech.block.inventory.UsbSwitchBlock
      || block instanceof com.gregtech.gregtech.block.inventory.DrawerQuadBlock
      || block instanceof com.gregtech.gregtech.block.misc.AdvancedCraftingTableBlock
      || block instanceof com.gregtech.gregtech.block.machine.FluidPipeBlock
      || block instanceof com.gregtech.gregtech.block.machine.ItemPipeBlock
      || path.startsWith("axle_") || path.startsWith("gearbox_")
      || block.getClass().getSimpleName().endsWith("ControllerBlock")
      || block instanceof com.gregtech.gregtech.block.machine.MultiblockPortBlock) return Tool.WRENCH;
  var sound=block.defaultBlockState().getSoundType();
  if(sound==SoundType.WOOD || sound==SoundType.BAMBOO_WOOD) return Tool.AXE;
  if(sound==SoundType.WOOL) return Tool.SHEARS;
  if(sound==SoundType.SAND || sound==SoundType.GRAVEL || sound==SoundType.SNOW) return Tool.SHOVEL;
  return Tool.PICKAXE;
 }
 public static int level(Block block) {
  if(block instanceof com.gregtech.gregtech.block.misc.LongDistPipeBlock) return 3;
  if(block instanceof com.gregtech.gregtech.block.misc.LongDistEndpointBlock endpoint) return endpoint.material().getToolQuality();
  if(block instanceof com.gregtech.gregtech.block.misc.LongDistanceTransformerBlock endpoint) return endpoint.material().getToolQuality();
  if(block instanceof com.gregtech.gregtech.block.BookShelfBlock shelf && shelf.variant().metal())
   return Math.max(0,shelf.variant().material().getToolQuality());
  if(block instanceof com.gregtech.gregtech.block.misc.BarsBlock bars) return bars.harvestLevel();
  if(block instanceof com.gregtech.gregtech.block.RockOreBlock ore) return ore.oreSpec().harvestLevel();
  if(block instanceof com.gregtech.gregtech.block.VanillaOreBlock ore) return ore.oreSpec().harvestLevel();
  if(block instanceof com.gregtech.gregtech.block.misc.ConcreteBlock concrete) return concrete.reinforced()?3:1;
  if(block instanceof com.gregtech.gregtech.block.stone.GTStoneBlock stone) return stone.stoneType().harvestLevel();
  if(block instanceof com.gregtech.gregtech.block.stone.GTStoneSlabBlock slab) return slab.stoneType().harvestLevel();
  if(block instanceof com.gregtech.gregtech.block.inventory.LogisticsMassStorageBlock logistics)
   return Math.max(0,logistics.constructionMaterial().getToolQuality());
  if(block instanceof com.gregtech.gregtech.block.inventory.MassStorageBlock storage && storage.material()!=null)
   return Math.max(0,storage.material().getToolQuality());
  if(block instanceof MaterialBlockLike m && tool(block)==Tool.PICKAXE) return m.prefix().harvestLevel(m.material());
  return 0;
 }
 public static String tag(Tool tool) { return BlockHarvestNames.tag(tool.name()); }
}
