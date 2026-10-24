package gregtech.gametest;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.prefix.BlockMaterialPrefix;
import gregtech.block.*;
import gregtech.registry.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}
/** API-neutral tests-only draft. Expected literals transcribed from independently pinned source tuples,
 * not computed through the production physics helper. Existing Ironq2 avoids metadata-repair self-proof. */
@GameTestHolder("gregtech_ore_source_audit") @PrefixGameTestTemplate(false)
public final class OreHostPhysicsTests {
 private record Row(String host,String mode,float hardness,float resistance){}
 private static final Row[] SOURCE={
  new Row("stone_granite_black","normal",12.0F,24.0F),
  new Row("stone_granite_black","broken",4.5F,9.0F),
  new Row("stone_granite_black","small",12.0F,24.0F),
  new Row("stone_granite_red","normal",12.0F,24.0F),
  new Row("stone_granite_red","broken",4.5F,9.0F),
  new Row("stone_granite_red","small",12.0F,24.0F),
  new Row("stone_basalt","normal",6.0F,9.0F),
  new Row("stone_basalt","broken",2.0F,3.0F),
  new Row("stone_basalt","small",6.0F,9.0F),
  new Row("stone_marble","normal",1.5F,2.25F),
  new Row("stone_marble","broken",1.0F,0.74F),
  new Row("stone_marble","small",1.0F,1.5F),
  new Row("stone_limestone","normal",1.5F,2.25F),
  new Row("stone_limestone","broken",1.0F,0.74F),
  new Row("stone_limestone","small",1.0F,1.5F),
  new Row("stone_granite","normal",3.0F,4.5F),
  new Row("stone_granite","broken",1.0F,1.5F),
  new Row("stone_granite","small",2.0F,3.0F),
  new Row("stone_diorite","normal",1.5F,2.25F),
  new Row("stone_diorite","broken",1.0F,0.74F),
  new Row("stone_diorite","small",1.0F,1.5F),
  new Row("stone_andesite","normal",1.5F,2.25F),
  new Row("stone_andesite","broken",1.0F,0.74F),
  new Row("stone_andesite","small",1.0F,1.5F),
  new Row("stone_komatiite","normal",6.0F,9.0F),
  new Row("stone_komatiite","broken",2.0F,3.0F),
  new Row("stone_komatiite","small",6.0F,9.0F),
  new Row("stone_greenschist","normal",1.5F,2.25F),
  new Row("stone_greenschist","broken",1.0F,0.74F),
  new Row("stone_greenschist","small",1.0F,1.5F),
  new Row("stone_blueschist","normal",1.5F,2.25F),
  new Row("stone_blueschist","broken",1.0F,0.74F),
  new Row("stone_blueschist","small",1.0F,1.5F),
  new Row("stone_kimberlite","normal",6.0F,9.0F),
  new Row("stone_kimberlite","broken",2.0F,3.0F),
  new Row("stone_kimberlite","small",6.0F,9.0F),
  new Row("stone_quartzite","normal",1.5F,2.25F),
  new Row("stone_quartzite","broken",1.0F,0.74F),
  new Row("stone_quartzite","small",1.0F,1.5F),
  new Row("stone_prismarine_light","normal",1.5F,2.25F),
  new Row("stone_prismarine_light","broken",1.0F,0.74F),
  new Row("stone_prismarine_light","small",1.0F,1.5F),
  new Row("stone_prismarine_dark","normal",1.5F,2.25F),
  new Row("stone_prismarine_dark","broken",1.0F,0.74F),
  new Row("stone_prismarine_dark","small",1.0F,1.5F),
  new Row("stone_slate","normal",1.5F,2.25F),
  new Row("stone_slate","broken",1.0F,0.74F),
  new Row("stone_slate","small",1.0F,1.5F),
  new Row("stone_shale","normal",1.5F,2.25F),
  new Row("stone_shale","broken",1.0F,0.74F),
  new Row("stone_shale","small",1.0F,1.5F),
  new Row("stone","normal",3.0F,3.0F),
  new Row("stone","broken",1.0F,1.0F),
  new Row("stone","small",2.0F,2.0F),
  new Row("netherrack","normal",1.5F,1.5F),
  new Row("netherrack","broken",1.0F,0.4F),
  new Row("netherrack","small",1.0F,1.0F),
  new Row("end_stone","normal",3.0F,6.0F),
  new Row("end_stone","broken",1.0F,2.0F),
  new Row("end_stone","small",2.0F,4.0F),
  new Row("sand","normal",1.2F,1.8F),
  new Row("sand","small",1.0F,1.2F),
  new Row("red_sand","normal",1.2F,1.8F),
  new Row("red_sand","small",1.0F,1.2F),
  new Row("gravel","normal",1.8F,2.4F),
  new Row("gravel","small",1.2F,1.6F),
  new Row("granite","normal",3.0F,3.0F),
  new Row("granite","broken",1.0F,1.0F),
  new Row("granite","small",2.0F,2.0F),
  new Row("diorite","normal",3.0F,3.0F),
  new Row("diorite","broken",1.0F,1.0F),
  new Row("diorite","small",2.0F,2.0F),
  new Row("andesite","normal",3.0F,3.0F),
  new Row("andesite","broken",1.0F,1.0F),
  new Row("andesite","small",2.0F,2.0F)
 };
 private static OreBlock ore(GameTestHelper h,boolean small){
  var material=GTMaterialRegistry.get("Iron").resolve();
  h.assertTrue(material.isValid()&&material.getId()==260&&material.getToolQuality()==2,"independent existing Iron quality2 fixture");
  var item=GTBlocks.getStack(small?BlockMaterialPrefix.oreSmall:BlockMaterialPrefix.ore,material);
  h.assertTrue(!item.isEmpty()&&item.getItem() instanceof BlockItem,"actual native ore block item");
  var block=((BlockItem)item.getItem()).getBlock();
  h.assertTrue(block.getClass()==OreBlock.class,"actual native OreBlock owner");return (OreBlock)block;
 }
 private static BlockState state(GameTestHelper h,String host,String mode){
  var rock=OreHostStone.byId(host);h.assertTrue(rock.id().equals(host),"exact native host "+host);
  return ore(h,mode.equals("small")).stateFor(rock).setValue(OreBlock.BROKEN,mode.equals("broken"));
 }
 private static void near(GameTestHelper h,float actual,float expected,String message){h.assertTrue(Math.abs(actual-expected)<0.00001F,message+": expected "+expected+", got "+actual);}
 @GameTest(template="ore_host_physics_owned",batch="ore_host_physics",timeoutTicks=100)
 public static void actualStateHardnessAndContextualBlastFollowLiteralSourceHosts(GameTestHelper h){
        OreHostPhysicsFixture.run(h, players -> actualStateHardnessAndContextualBlastFollowLiteralSourceHostsBody(h, players));
    }
    private static void actualStateHardnessAndContextualBlastFollowLiteralSourceHostsBody(GameTestHelper h, java.util.List<net.minecraft.server.level.ServerPlayer> players) {
  h.assertTrue(SOURCE.length==75,"fixed independent source matrix length");
  var pos=OreHostPhysicsFixture.templatePos(h,new BlockPos(2,2,2));
  for(var row:SOURCE){var state=state(h,row.host(),row.mode());
   h.getLevel().setBlock(pos,state,2);
   near(h,state.getDestroySpeed(h.getLevel(),pos),row.hardness(),"native cached-state hardness hook "+row.host()+"/"+row.mode());
   near(h,state.getBlock().getExplosionResistance(state,h.getLevel(),pos,null),row.resistance(),"native contextual blast "+row.host()+"/"+row.mode());
  }
 }
 @GameTest(template="ore_host_physics_owned",batch="ore_host_physics",timeoutTicks=100)
 public static void unreviewedHostsAndNonOreDenseClassesKeepExistingPhysics(GameTestHelper h){
        OreHostPhysicsFixture.run(h, players -> unreviewedHostsAndNonOreDenseClassesKeepExistingPhysicsBody(h, players));
    }
    private static void unreviewedHostsAndNonOreDenseClassesKeepExistingPhysicsBody(GameTestHelper h, java.util.List<net.minecraft.server.level.ServerPlayer> players) {
  var pos=OreHostPhysicsFixture.templatePos(h,new BlockPos(2,2,2));
  for(String host:new String[]{"deepslate","tuff","stone_chalk"})for(String mode:new String[]{"normal","small","broken"}){
   var state=state(h,host,mode);
   h.getLevel().setBlock(pos,state,2);
   near(h,state.getDestroySpeed(h.getLevel(),pos),9F,"UNKNOWN hardness exact legacy "+host+"/"+mode);
   near(h,state.getBlock().getExplosionResistance(state,h.getLevel(),pos,null),mode.equals("broken")?4.5F:9F,"UNKNOWN blast exact legacy "+host+"/"+mode);
  }
  h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),2);
  near(h,Blocks.STONE.defaultBlockState().getDestroySpeed(h.getLevel(),pos),1.5F,"vanilla stone untouched");
  var dense=java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(),false)
    .filter(b->b instanceof RockOreBlock rock&&rock.oreSpec().material().equals("NetherQuartz")).toList();
  h.assertTrue(dense.size()==1,"one actual registered dense NetherQuartz control");
  var block=dense.get(0);h.getLevel().setBlock(pos,block.defaultBlockState(),2);near(h,block.defaultBlockState().getDestroySpeed(h.getLevel(),pos),1.5F,"dense quartz independent hardness");
  near(h,block.getExplosionResistance(block.defaultBlockState(),h.getLevel(),pos,null),6F,"dense quartz independent blast");
  var crystalId=net.minecraft.resources.ResourceLocation.parse("gt6ce:crystal_ore_cinnabar");
  var crystal=BuiltInRegistries.BLOCK.get(crystalId);
  h.assertTrue(BuiltInRegistries.BLOCK.containsKey(crystalId)&&crystal instanceof CrystalOreBlock,"actual registered dense cinnabar crystal control exists");
  h.getLevel().setBlock(pos,crystal.defaultBlockState(),2);
  near(h,crystal.defaultBlockState().getDestroySpeed(h.getLevel(),pos),0.3F,"dense crystal independent hardness");
  near(h,crystal.getExplosionResistance(crystal.defaultBlockState(),h.getLevel(),pos,null),0.3F,"dense crystal independent blast");
  var bedrock=state(h,"bedrock","normal");
  h.getLevel().setBlock(pos,bedrock,2);
  near(h,bedrock.getBlock().getExplosionResistance(bedrock,h.getLevel(),pos,null),3600000F,"bedrock blast sentinel unchanged");
//? if >=1.21.1 {
/*  var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
  var player=h.makeMockPlayer();
//?}
  near(h,bedrock.getDestroyProgress(player,h.getLevel(),pos),0F,"bedrock remains unbreakable");
  var loot=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.atCenterOf(pos))
    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,net.minecraft.world.item.ItemStack.EMPTY)
    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE,bedrock);
  h.assertTrue(((OreBlock)bedrock.getBlock()).getDrops(bedrock,loot).isEmpty(),"bedrock remains no-drop");

 }
}
