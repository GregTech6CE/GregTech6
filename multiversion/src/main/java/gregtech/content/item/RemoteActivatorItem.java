package gregtech.content.item;
import gregtech.content.item.behavior.BehaviorRemote;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

/** Existing technological ID with the original remote binding and activation behavior. */
public final class RemoteActivatorItem extends TechItem {

    public RemoteActivatorItem(String name, Properties properties) {
        super(name, properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var blockPos = context.getClickedPos();
        var hitLocation = context.getClickLocation();
        return BehaviorRemote.useOn(context.getLevel(), blockPos, context.getClickedFace(), context.getPlayer(), stack,
                (float)(hitLocation.x - blockPos.getX()), (float)(hitLocation.y - blockPos.getY()),
                (float)(hitLocation.z - blockPos.getZ())).acted() ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        return BehaviorRemote.useInAir(level, player, stack).acted() ? InteractionResultHolder.consume(stack) : InteractionResultHolder.pass(stack);
    }
}
