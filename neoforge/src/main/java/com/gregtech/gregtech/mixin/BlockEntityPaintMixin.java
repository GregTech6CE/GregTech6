package com.gregtech.gregtech.mixin;
import com.gregtech.gregtech.api.tool.Paintable;import com.gregtech.gregtech.content.tool.PaintState;
import net.minecraft.core.BlockPos;import net.minecraft.nbt.CompoundTag;import net.minecraft.world.level.block.entity.BlockEntity;import net.minecraft.world.level.block.Block;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.core.registries.BuiltInRegistries;import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
/** Brokestar paint layer attached to original GT entities without a second machine hierarchy. */
@Mixin(BlockEntity.class)
public abstract class BlockEntityPaintMixin implements Paintable {
@Unique private final PaintState gregtech$paint=new PaintState();
@Unique private BlockEntity gregtech$self(){return (BlockEntity)(Object)this;}
@Unique private boolean gregtech$own(){return BuiltInRegistries.BLOCK.getKey(gregtech$self().getBlockState().getBlock()).getNamespace().equals("gregtech");}
@Unique private void gregtech$changed(){var self=gregtech$self();self.setChanged();self.requestModelDataUpdate();var level=self.getLevel();if(level==null)return;var state=self.getBlockState();level.sendBlockUpdated(self.getBlockPos(),state,state,Block.UPDATE_CLIENTS);if(level instanceof ServerLevel server){var packet=ClientboundBlockEntityDataPacket.create(self);BlockPos pos=self.getBlockPos();for(var player:server.players())if(player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=4096)player.connection.send(packet);}}
@Override public boolean paint(int rgb){if(!gregtech$own()||!gregtech$paint.paint(rgb))return false;gregtech$changed();return true;}
@Override public boolean mixPaint(int rgb){if(!gregtech$own()||!gregtech$paint.mix(rgb))return false;gregtech$changed();return true;}
@Override public boolean unpaint(){if(!gregtech$own()||!gregtech$paint.clear())return false;gregtech$changed();return true;}
@Override public boolean isPainted(){return gregtech$own()&&gregtech$paint.painted();}
@Override public int getPaint(){return gregtech$paint.color();}
@Unique private void gregtech$write(CompoundTag tag){if(!gregtech$own())return;tag.putBoolean("gt.painted",gregtech$paint.painted());if(gregtech$paint.painted())tag.putInt("gt.color",gregtech$paint.color());}
@Unique private void gregtech$read(CompoundTag tag){if(!gregtech$own())return;boolean changed=gregtech$paint.restore(tag.getBoolean("gt.painted"),tag.contains("gt.color")?tag.getInt("gt.color"):0xFFFFFF);var self=gregtech$self();var level=self.getLevel();if(changed&&level!=null&&level.isClientSide){self.requestModelDataUpdate();var state=self.getBlockState();level.sendBlockUpdated(self.getBlockPos(),state,state,Block.UPDATE_CLIENTS);}}
@Inject(method="saveAdditional",at=@At("RETURN"),require=1)private void gregtech$save(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup,CallbackInfo ci){gregtech$write(tag);}
@Inject(method="loadAdditional",at=@At("RETURN"),require=1)private void gregtech$load(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup,CallbackInfo ci){gregtech$read(tag);}
@Inject(method="getUpdateTag",at=@At("RETURN"),require=1)private void gregtech$update(net.minecraft.core.HolderLookup.Provider lookup,CallbackInfoReturnable<CompoundTag> cir){gregtech$write(cir.getReturnValue());}
}
