package gregtech.content.data;

import gregtech.content.block.MaterialBlockLike;
import gregtech.engine.machine.GTMachineBlock;
import net.minecraft.world.level.block.*;

/** GT6 Loader_MultiTileEntities material groups, adapted to Forge harvest tags.
 * Hardness/resistance remain supplied by each material/definition, not a global constant.
 */

/** GT6 Loader_MultiTileEntities material groups, adapted to native harvest tags; both platforms share BlockHarvestNames.
 * Hardness/resistance remain supplied by each material/definition, not a global constant.
 */
public final class BlockHarvestPolicy {
 private BlockHarvestPolicy() {}
 public enum Tool { PICKAXE, AXE, SHOVEL, SWORD, WRENCH, CROWBAR, CUTTER, SHEARS, HAND }
 public static java.util.Optional<SourceBlockProperties.Params> source(Block block) {
  var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
  if(!id.getNamespace().equals("gt6ce"))return java.util.Optional.empty();
  var original=SourceBlockProperties.block(id.getPath());
  if(original.isEmpty() && block instanceof gregtech.content.block.machine.BasicMachineBlock machine)
   original=SourceBlockProperties.basic(machine.basicSpec().machineName(),machine.basicSpec().tier());
  return original;
 }
 public static boolean handHarvestable(Block block) {
  return source(block).map(SourceBlockProperties.Params::handHarvestable).orElse(!block.defaultBlockState().requiresCorrectToolForDrops());
 }
 public static Tool tool(Block block) {
  if(block instanceof MaterialBlockLike material && material.prefix().isCrate())return Tool.CROWBAR;
  var original=source(block);
  if(original.isPresent())return Tool.valueOf(original.get().tool().toUpperCase(java.util.Locale.ROOT));
  if(block instanceof gregtech.content.block.misc.LongDistPipeBlock line) return line.isWire()?Tool.CUTTER:Tool.WRENCH;
  if(block instanceof gregtech.content.block.misc.LongDistEndpointBlock || block instanceof gregtech.content.block.misc.LongDistanceTransformerBlock) return Tool.WRENCH;
  if(block instanceof LiquidBlock || block instanceof BushBlock || block instanceof gregtech.content.block.RockBlock || block instanceof gregtech.content.block.TwigBlock) return Tool.HAND;
  if(block instanceof gregtech.content.block.plant.BaleBlock) return Tool.SWORD;
  if(block instanceof LeavesBlock) return Tool.SHEARS;
  if(block instanceof gregtech.content.block.BookShelfBlock shelf) return shelf.variant().metal() ? Tool.WRENCH : Tool.AXE;
  if(block instanceof gregtech.content.block.inventory.MassStorageBlock) return Tool.WRENCH;
  if(block instanceof MaterialBlockLike m) {
   if(m.prefix().isCrate())return Tool.CROWBAR;
   if(m.prefix().falling())return Tool.SHOVEL;
   return Tool.PICKAXE;
  }
//? if >=1.21.1 {
  /*if(block instanceof gregtech.engine.energy.WireMaterialLike || block instanceof gregtech.content.block.energy.SignalWireBlock) return Tool.CUTTER;
  var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
*///?} else {
  if(block instanceof gregtech.content.block.energy.ElectricWireBlock || block instanceof gregtech.content.block.energy.SignalWireBlock) return Tool.CUTTER;
  var id=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block);
//?}
  String path=id==null?"":id.getPath();
  // Wooden hulls take precedence over machinery implemented by the same base class.
  if(BlockHarvestNames.woodenHull(path)) return Tool.AXE;
  if(block instanceof GTMachineBlock || block instanceof gregtech.content.block.energy.EnergyNodeBlock
      || block instanceof gregtech.content.block.inventory.SafeBlock
      || block instanceof gregtech.content.block.inventory.UsbSwitchBlock
      || block instanceof gregtech.content.block.inventory.DrawerQuadBlock
      || block instanceof gregtech.content.block.misc.AdvancedCraftingTableBlock
      || block instanceof gregtech.content.block.machine.FluidPipeBlock
      || block instanceof gregtech.content.block.machine.ItemPipeBlock
      || path.startsWith("axle_") || path.startsWith("gearbox_")
      || block.getClass().getSimpleName().endsWith("ControllerBlock")
      || block instanceof gregtech.content.block.machine.MultiblockPortBlock) return Tool.WRENCH;
  var sound=block.defaultBlockState().getSoundType();
  if(sound==SoundType.WOOD || sound==SoundType.BAMBOO_WOOD) return Tool.AXE;
  if(sound==SoundType.WOOL) return Tool.SHEARS;
  if(sound==SoundType.SAND || sound==SoundType.GRAVEL || sound==SoundType.SNOW) return Tool.SHOVEL;
  return Tool.PICKAXE;
 }
 public static int level(Block block) {
  var original=source(block);if(original.isPresent())return original.get().level();
  if(block instanceof gregtech.content.block.misc.LongDistPipeBlock) return 3;
  if(block instanceof gregtech.content.block.misc.LongDistEndpointBlock endpoint) return endpoint.material().getToolQuality();
  if(block instanceof gregtech.content.block.misc.LongDistanceTransformerBlock endpoint) return endpoint.material().getToolQuality();
  if(block instanceof gregtech.content.block.BookShelfBlock shelf && shelf.variant().metal())
   return Math.max(0,shelf.variant().material().getToolQuality());
  if(block instanceof gregtech.content.block.misc.BarsBlock bars) return bars.harvestLevel();
  if(block instanceof gregtech.content.block.RockOreBlock ore) return ore.oreSpec().harvestLevel();
  if(block instanceof gregtech.content.block.VanillaOreBlock ore) return ore.oreSpec().harvestLevel();
  if(block instanceof gregtech.content.block.misc.ConcreteBlock concrete) return concrete.reinforced()?3:1;
  if(block instanceof gregtech.content.block.stone.GTStoneBlock stone) return stone.stoneType().harvestLevel();
  if(block instanceof gregtech.content.block.stone.GTStoneSlabBlock slab) return slab.stoneType().harvestLevel();
  if(block instanceof gregtech.content.block.inventory.LogisticsMassStorageBlock logistics)
   return Math.max(0,logistics.constructionMaterial().getToolQuality());
  if(block instanceof gregtech.content.block.inventory.MassStorageBlock storage && storage.material()!=null)
   return Math.max(0,storage.material().getToolQuality());
  if(block instanceof MaterialBlockLike m && tool(block)==Tool.PICKAXE) return m.prefix().harvestLevel(m.material());
  return 0;
 }
 /** State-sensitive source ore policy; unrelated blocks retain their existing contract. */
 public static int level(net.minecraft.world.level.block.state.BlockState state) {
  return OreHostPhysics.params(state).map(SourceOreHostPhysics.Params::harvest).orElseGet(()->level(state.getBlock()));
 }
 public static Tool tool(net.minecraft.world.level.block.state.BlockState state) {
  if(state.getBlock() instanceof gregtech.content.block.OreBlock
      && gregtech.content.block.OreBlock.stoneOf(state).isSediment())return Tool.SHOVEL;
  return OreHostPhysics.params(state).map(p->Tool.valueOf(p.tool())).orElseGet(()->tool(state.getBlock()));
 }
 public static String tag(Tool tool) { return BlockHarvestNames.tag(tool.name()); }
}
