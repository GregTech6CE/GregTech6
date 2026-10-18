package com.gregtech.gregtech.item;
import com.gregtech.gregtech.item.behavior.BehaviorRemote;
import net.minecraft.world.*;import net.minecraft.world.item.*;import net.minecraft.world.item.context.UseOnContext;import net.minecraft.world.level.Level;import net.minecraft.world.entity.player.Player;
/** Existing technological ID with the original remote binding and activation behavior. */
public final class RemoteActivatorItem extends TechItem {
 public RemoteActivatorItem(String name,Properties properties){super(name,properties);}
 @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){var pos=context.getClickedPos();var point=context.getClickLocation();return BehaviorRemote.useOn(context.getLevel(),pos,context.getClickedFace(),context.getPlayer(),stack,(float)(point.x-pos.getX()),(float)(point.y-pos.getY()),(float)(point.z-pos.getZ())).acted()?InteractionResult.CONSUME:InteractionResult.PASS;}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);return BehaviorRemote.useInAir(level,player,stack).acted()?InteractionResultHolder.consume(stack):InteractionResultHolder.pass(stack);}
}
