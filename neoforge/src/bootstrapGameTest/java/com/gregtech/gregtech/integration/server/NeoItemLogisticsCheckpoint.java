package com.gregtech.gregtech.integration.server;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.content.transport.*;
import com.gregtech.gregtech.content.recipe.PipeRecipes;
import com.gregtech.gregtech.block.machine.ItemPipeBlock;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.api.tool.ScrewdriverUseTarget;
import com.gregtech.gregtech.content.cover.CoverUtilityBehaviors;
import net.minecraft.core.*;import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.server.level.ServerLevel;import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;import net.neoforged.neoforge.capabilities.Capabilities;
/** Opt-in real native capabilities/ticks/component persistence checkpoint, excluded from the jar. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,value=Dist.DEDICATED_SERVER)
public final class NeoItemLogisticsCheckpoint {
 private NeoItemLogisticsCheckpoint(){}
 private static ServerLevel world;private static int ticks;private static boolean finished;
 private static final BlockPos PIPE=new BlockPos(128,100,128);
 private static ItemPipeBlockEntity first,second;
 private static ItemStack named(Item item,int count,String name){var stack=new ItemStack(item,count);stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));return stack;}
 private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Item logistics checkpoint: "+message);}
 private static int count(ChestBlockEntity chest){int count=0;for(int i=0;i<chest.getContainerSize();i++)count+=chest.getItem(i).getCount();return count;}
 private static Block block(String id){return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech",id));}
 private static void put(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){require(world.getBlockState(pos).isAir(),"fresh fixture position "+pos);require(world.setBlock(pos,state,Block.UPDATE_ALL),"place "+pos);}
 @SubscribeEvent public static void started(ServerStartedEvent event){
  if(!Boolean.getBoolean("gregtech.integration.itemLogisticsSmoke"))return;
  world=event.getServer().overworld();world.setChunkForced(PIPE.getX()>>4,PIPE.getZ()>>4,true);world.getChunk(PIPE);
  require(GTItemPipes.all().size()==126&&HopperRegistries.hoppers().size()==60&&HopperRegistries.queues().size()==60,"complete registration counts");
  require(PipeRecipes.entries().stream().anyMatch(row->row.itemPipe()&&row.material().equalsIgnoreCase("Brass")),"actual item pipe welder recipe");
  var pipe=block("item_pipe_medium_ultimet");require(pipe instanceof ItemPipeBlock,"actual pipe factory");
  put(PIPE.west(),Blocks.CHEST.defaultBlockState());put(PIPE.east(2),Blocks.CHEST.defaultBlockState());
  put(PIPE,pipe.defaultBlockState().setValue(ItemPipeBlock.WEST,true).setValue(ItemPipeBlock.EAST,true));put(PIPE.east(),pipe.defaultBlockState().setValue(ItemPipeBlock.WEST,true).setValue(ItemPipeBlock.EAST,true));
  first=(ItemPipeBlockEntity)world.getBlockEntity(PIPE);second=(ItemPipeBlockEntity)world.getBlockEntity(PIPE.east());
  var cap=world.getCapability(Capabilities.ItemHandler.BLOCK,PIPE,Direction.WEST);require(cap!=null,"native pipe capability");
  var stack=named(Items.CARROT,4,"route-marker");require(cap.insertItem(0,stack,true).isEmpty()&&first.getStackInSlot(0).isEmpty(),"pipe insert simulation");
  var filter=new ItemStack(GTTechnological.get("item_filter"));require(first.attachCover(Direction.WEST,filter),"actual item filter attachment");
  var filtered=world.getCapability(Capabilities.ItemHandler.BLOCK,PIPE,Direction.WEST);require(filtered!=cap&&filtered.insertItem(0,stack,true).getCount()==4,"native capability invalidation and empty whitelist rejection");
  require(first.clickFilterCover(Direction.WEST,stack),"filter component write");require(filtered.insertItem(0,stack,true).isEmpty()&&!filtered.insertItem(0,new ItemStack(Items.POTATO),true).isEmpty(),"filter accepts matched item and rejects others");
  first.removeCover(Direction.WEST);cap=world.getCapability(Capabilities.ItemHandler.BLOCK,PIPE,Direction.WEST);require(cap.insertItem(0,stack,false).isEmpty(),"actual sided pipe input");
  // Native codec's count bound must not truncate the original high-capacity buffers.
  var bigState=block("item_pipe_medium_vibranium_silver").defaultBlockState();var big=new ItemPipeBlockEntity(new BlockPos(140,100,128),bigState);big.setLevel(world);var large=named(Items.STONE,512,"large-buffer");require(big.insertItem(0,large,false).isEmpty(),"original 512-count buffer");
  var restored=new ItemPipeBlockEntity(big.getBlockPos(),bigState);restored.setLevel(world);restored.loadWithComponents(big.saveWithoutMetadata(world.registryAccess()),world.registryAccess());require(restored.getStackInSlot(0).getCount()==512&&ItemStack.isSameItemSameComponents(restored.getStackInSlot(0),large),"large buffer save/load retains count and components");
  var clientCopy=new ItemPipeBlockEntity(big.getBlockPos(),bigState);clientCopy.setLevel(world);clientCopy.handleUpdateTag(big.getUpdateTag(world.registryAccess()),world.registryAccess());require(clientCopy.getStackInSlot(0).getCount()==512,"large buffer update tag");
  var hp=new BlockPos(144,100,128);var hs=block("hopper_steel").defaultBlockState().setValue(DirectionalBlock.FACING,Direction.EAST);put(hp.east(),Blocks.CHEST.defaultBlockState());put(hp,hs);var hopper=(HopperBlockEntity)world.getBlockEntity(hp);
  require(hs.getBlock() instanceof ScrewdriverUseTarget,"real screwdriver dispatch target");for(int i=0;i<4;i++)hopper.cycleMode(false);hopper.toggleExactMode();require(hopper.getMode()==4&&hopper.getExactMode(),"hopper mode controls");
  var input=world.getCapability(Capabilities.ItemHandler.BLOCK,hp,Direction.UP);var output=world.getCapability(Capabilities.ItemHandler.BLOCK,hp,Direction.EAST);var carrots=named(Items.CARROT,8,"exact-marker");require(input!=null&&output!=null&&output.insertItem(0,carrots,false).getCount()==8&&input.insertItem(0,carrots,false).isEmpty(),"hopper sided insertion");
  for(int i=0;i<4;i++)HopperBlockEntity.serverTick(world,hp,hs,hopper);require(count((ChestBlockEntity)world.getBlockEntity(hp.east()))==4&&hopper.getStackInSlot(0).getCount()==4,"first exact emission is four with no loss");
  var savedHopper=new HopperBlockEntity(hp,hs);savedHopper.setLevel(world);savedHopper.loadWithComponents(hopper.saveWithoutMetadata(world.registryAccess()),world.registryAccess());require(savedHopper.getMode()==4&&savedHopper.getExactMode()&&savedHopper.getStackInSlot(0).getCount()==4,"hopper mode/inventory component load");
  var qp=new BlockPos(152,100,128);var qs=block("queue_hopper_steel").defaultBlockState().setValue(DirectionalBlock.FACING,Direction.UP);put(qp,qs);var queue=(QueueHopperBlockEntity)world.getBlockEntity(qp);var qcap=world.getCapability(Capabilities.ItemHandler.BLOCK,qp,Direction.WEST);require(qcap!=null&&qcap.getSlots()==2,"queue exposes entry and exit");
  var a=named(Items.CARROT,2,"first");var b=named(Items.POTATO,3,"second");require(qcap.insertItem(0,a,false).isEmpty(),"first FIFO insertion");for(int i=0;i<8;i++)QueueHopperBlockEntity.serverTick(world,qp,qs,queue);require(qcap.insertItem(0,b,false).isEmpty(),"second FIFO insertion");for(int i=0;i<8;i++)QueueHopperBlockEntity.serverTick(world,qp,qs,queue);
  var oldest=qcap.extractItem(1,64,false);require(oldest.getCount()==2&&ItemStack.isSameItemSameComponents(oldest,a),"FIFO first component stack");for(int i=0;i<8;i++)QueueHopperBlockEntity.serverTick(world,qp,qs,queue);var next=qcap.extractItem(1,64,false);require(next.getCount()==3&&ItemStack.isSameItemSameComponents(next,b),"FIFO second component stack");
  com.mojang.logging.LogUtils.getLogger().info("ITEM_LOGISTICS_CHECKPOINT_PREPARED registry=true recipe=true filters=true oversizedComponents=true exactHopper=true fifo=true");
 }
 @SubscribeEvent public static void tick(ServerTickEvent.Post event){
  if(world==null||finished||++ticks<80)return;
  var target=(ChestBlockEntity)world.getBlockEntity(PIPE.east(2));var origin=(ChestBlockEntity)world.getBlockEntity(PIPE.west());
  require(count(target)==4&&count(origin)==0&&first.getStackInSlot(0).isEmpty()&&second.getStackInSlot(0).isEmpty(),"actual two-pipe route, no duplication and no backflow");
  require(ItemStack.isSameItemSameComponents(target.getItem(0),named(Items.CARROT,4,"route-marker")),"routed components unchanged");
  finished=true;com.mojang.logging.LogUtils.getLogger().info("ITEM_LOGISTICS_CHECKPOINT_SUCCESS registry=true recipe=true filters=true oversizedComponents=true exactHopper=true fifo=true route=true noBackflow=true observedTicks={}",ticks);
 }
}
