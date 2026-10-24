package gregtech.gametest;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.*;
import net.minecraft.core.BlockPos;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
@GameTestHolder("gt6ce") @PrefixGameTestTemplate(false)
public final class BlockHarvestTests {
 @GameTest(template="test_empty") public static void allOrdinaryBlocksHaveLoot(GameTestHelper h) throws Exception {
  var missing=new TreeSet<String>(); int checked=0;
  for(var b:ForgeRegistries.BLOCKS.getValues()) {
   var id=ForgeRegistries.BLOCKS.getKey(b);
   if(!id.getNamespace().equals("gt6ce") || b.asItem()==Items.AIR || b instanceof LiquidBlock) continue;
   // Specialized drops (ore, rocks, stateful machines) have their own regression tests.
   if(b.getClass().getMethod("getDrops",net.minecraft.world.level.block.state.BlockState.class,net.minecraft.world.level.storage.loot.LootParams.Builder.class).getDeclaringClass()!=net.minecraft.world.level.block.state.BlockBehaviour.class) continue;
   var state=b.defaultBlockState(); if(state.getDestroySpeed(h.getLevel(),h.absolutePos(BlockPos.ZERO))<0)continue;
   checked++;
   var drops=Block.getDrops(state,h.getLevel(),h.absolutePos(BlockPos.ZERO),null);
   if(drops.stream().noneMatch(s->!s.isEmpty())) missing.add(id.toString());
  }
  java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/block-loot-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("checked",checked,"missing",missing)));
  h.assertTrue(missing.isEmpty(),"Missing block loot: "+missing); h.succeed();
 }
 @GameTest(template="test_empty") public static void everyBlockHasHarvestClassification(GameTestHelper h) {
  int count=0;
  for(var b:ForgeRegistries.BLOCKS.getValues()) {
   var id=ForgeRegistries.BLOCKS.getKey(b);
   if(!id.getNamespace().equals("gt6ce")||b instanceof LiquidBlock)continue;
   var tag=net.minecraft.tags.BlockTags.create(net.minecraft.resources.ResourceLocation.parse(gregtech.data.BlockHarvestPolicy.tag(gregtech.data.BlockHarvestPolicy.tool(b))));
   h.assertTrue(b.defaultBlockState().is(tag),"Missing harvest tag: "+id);count++;
  }
  h.assertTrue(count>2000,"Audit must cover actual registry");h.succeed();
 }
 @GameTest(template="test_empty") public static void survivalBreakDropsOneBlock(GameTestHelper h) {
  var block=ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("gt6ce","axle_steel_1"));
  var pos=h.absolutePos(new BlockPos(1,2,1)); h.getLevel().setBlock(pos,block.defaultBlockState(),3);
  var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
  player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
  var wrench=gregtech.item.GTToolItem.create(gregtech.api.tool.GTToolType.WRENCH,gregtech.content.material.Materials.Steel,gregtech.api.material.GTMaterialRegistry.get("Wood"));
  player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wrench);
  h.assertTrue(block.defaultBlockState().canHarvestBlock(h.getLevel(),pos,player),"Wrench can harvest axle");
  h.assertTrue(player.gameMode.destroyBlock(pos),"Actual survival break succeeds");
  var items=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1));
  h.assertTrue(items.stream().filter(e->e.getItem().is(block.asItem())).mapToInt(e->e.getItem().getCount()).sum()==1,"Exactly one axle, not zero or duplicate");h.succeed();
 }
 @GameTest(template="test_empty") public static void materialTierAndWoodTools(GameTestHelper h) {
  var pos=h.absolutePos(new BlockPos(1,2,1));var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
  var high=ForgeRegistries.BLOCKS.getValues().stream().filter(b->b instanceof gregtech.block.MaterialBlockLike && gregtech.data.BlockHarvestPolicy.level(b)>=3).findFirst().orElseThrow();
  player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.WOODEN_PICKAXE));
  h.assertTrue(!high.defaultBlockState().canHarvestBlock(h.getLevel(),pos,player),"Wood pickaxe cannot bypass material tier");
  var wood=ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("gt6ce","wood_barrel"));
  h.assertTrue(wood.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE),"Wood barrel axe tag");
  var wire=ForgeRegistries.BLOCKS.getValues().stream().filter(b->b instanceof gregtech.block.energy.ElectricWireBlock).findFirst().orElseThrow();
  var cutter=gregtech.item.GTToolItem.create(gregtech.api.tool.GTToolType.WIRE_CUTTER,gregtech.content.material.Materials.Steel,gregtech.api.material.GTMaterialRegistry.get("Wood"));
  h.assertTrue(gregtech.api.tool.GTToolHelper.isCorrectToolForDrops(cutter,wire.defaultBlockState()),"Wire cutter recognized");h.succeed();
 }
 @GameTest(template="test_empty") public static void barrelKeepsFluidWithoutSpillingToNeighbor(GameTestHelper h) {
  var block=gregtech.registry.GTTanks.WOOD_BARREL.get();
  var pos=h.absolutePos(new BlockPos(1,2,1));var neighbor=pos.east();
  h.getLevel().setBlock(pos,block.defaultBlockState(),3);h.getLevel().setBlock(neighbor,block.defaultBlockState(),3);
  var be=(gregtech.blockentity.machine.TankBlockEntity)h.getLevel().getBlockEntity(pos);
  var other=(gregtech.blockentity.machine.TankBlockEntity)h.getLevel().getBlockEntity(neighbor);
  be.getFluidTank().fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
  var drops=Block.getDrops(block.defaultBlockState(),h.getLevel(),pos,be);
  h.assertTrue(drops.size()==1&&drops.get(0).is(block.asItem()),"One barrel drop");
  h.getLevel().removeBlock(pos,false);
  h.assertTrue(other.getFluidTank().isEmpty(),"Breaking does not also export saved fluid");
  h.getLevel().setBlock(pos,block.defaultBlockState(),3);
  net.minecraft.world.item.BlockItem.updateCustomBlockEntityTag(h.getLevel(),null,pos,drops.get(0));
  var restored=(gregtech.blockentity.machine.TankBlockEntity)h.getLevel().getBlockEntity(pos);
  h.assertTrue(restored.getFluidTank().getFluid().getAmount()==1000,"Replacement preserves all water");h.succeed();
 }
}
