package gregtech.platform.neoforge.smeltery;

import gregtech.api.material.*;
import net.minecraft.core.*;import net.minecraft.world.level.block.state.BlockState;
/** Bootstrap-facing facade over the full original mold, shared by all39 materials. */
public final class MoldEntity extends gregtech.content.blockentity.machine.MoldBlockEntity{
 public MoldEntity(BlockPos pos,BlockState state){super(pos,state);}
 public void serverTick(){serverTick(level,worldPosition,getBlockState(),this);}
 public boolean use(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return gregtech.event.SmelteryInteractionHandler.interact(this,player,hand,hit)!=net.minecraft.world.InteractionResult.PASS;}
 public void dropContents(){if(level!=null&&!level.isClientSide)dropContent();}
 public long fillMold(gregtech.api.material.GTMaterial material,long amount,long temperature,Direction side){return fillMold(material,amount,temperature,side.ordinal());}
 @Override public long fillMold(gregtech.api.material.GTMaterial material,long amount,long temperature,int side){
  // Retain the existing native output preflight before consuming a molten form unavailable on this platform.
  if(level!=null&&level.getBlockEntity(worldPosition.below()) instanceof gregtech.content.blockentity.machine.MoldBasinBlockEntity)return super.fillMold(material,amount,temperature,side);
  var recipe=gregtech.engine.machine.crucible.MoldShapes.recipe(getMoldShape());if(recipe==null||material==null)return 0;
  var output=recipe.blockSolid()?gregtech.content.registry.GTBlocks.getStack(gregtech.engine.prefix.BlockMaterialPrefix.blockSolid,material):gregtech.content.registry.GTItems.getStack(recipe.itemPrefix(),material);return output.isEmpty()?0:super.fillMold(material,amount,temperature,side);
 }
}
