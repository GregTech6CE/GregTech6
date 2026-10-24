package com.gregtech.gregtech.gametest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
/** Test-owned 9-cube only. Readiness retries never retry an assertion or a mutated body. */
final class OreHostPhysicsFixture {
 private static final BlockPos[] CELLS={new BlockPos(2,2,2),new BlockPos(3,3,3)};
 // Both loaders: helper coordinates are controller-relative; NBT starts at controller + (0,1,0).
 static BlockPos templatePos(GameTestHelper h,BlockPos local){return h.absolutePos(local.above());}
 static void run(GameTestHelper h, Consumer<List<ServerPlayer>> body){ ready(h,body,0); }
 private static void ready(GameTestHelper h, Consumer<List<ServerPlayer>> body,int attempt){
  boolean loaded=true;
  for(int x=0;x<9;x++)for(int z=0;z<9;z++) loaded &= h.getLevel().hasChunkAt(templatePos(h,new BlockPos(x,1,z)));
  if(!loaded && attempt<20){h.runAfterDelay(1,()->ready(h,body,attempt+1));return;}
  h.assertTrue(loaded,"FIXTURE_PENDING: owned ore chunks not ready within20ticks; gameplay assertions not executed");
  var old=new java.util.ArrayList<net.minecraft.world.level.block.state.BlockState>();
  for(var relative:CELLS){
   h.assertTrue(relative.getX()>0&&relative.getX()<8&&relative.getY()>0&&relative.getY()<8&&relative.getZ()>0&&relative.getZ()<8,"strict owned interior, never controller");
   var pos=templatePos(h,relative);
   h.assertTrue(net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(templatePos(h,BlockPos.ZERO),templatePos(h,new BlockPos(8,8,8))).isInside(pos),"declared paired9cube bounds own mutation cell");
   h.assertTrue(h.getLevel().getBlockEntity(pos)==null,"no controller or other block entity at mutation cell");
   h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"owned mutation cell is initially air: template="+relative+", world="+pos+", actual="+h.getLevel().getBlockState(pos));
   h.assertTrue(h.getLevel().getBlockState(pos.below()).is(Blocks.STONE),"owned support installed by template: world="+pos.below()+", actual="+h.getLevel().getBlockState(pos.below()));
   old.add(h.getLevel().getBlockState(pos));
  }
  var players=new ArrayList<ServerPlayer>();
  try { body.accept(players); }
  finally {
   try {for(var player:players){player.getInventory().clearContent();player.removeAllEffects();player.discard();}}
   finally {for(int i=0;i<CELLS.length;i++)h.getLevel().setBlock(templatePos(h,CELLS[i]),old.get(i),2);}
  }
  for(int i=0;i<CELLS.length;i++)h.assertTrue(h.getLevel().getBlockState(templatePos(h,CELLS[i])).equals(old.get(i)),"owned cells restored before success");
  h.succeed();
 }
}
