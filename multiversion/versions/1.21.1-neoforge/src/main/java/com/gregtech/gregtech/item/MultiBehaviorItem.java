package com.gregtech.gregtech.item;
import net.minecraft.world.*;import net.minecraft.world.entity.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.*;import net.minecraft.world.item.context.UseOnContext;
    public final class MultiBehaviorItem extends TechItem {
        public MultiBehaviorItem(String name,Item.Properties props) {
            super(name, props);
        }

        /**
         * §108: GT6's multi-item behaviour slot - duct tape, the five sprays, the lighters, the
         * portable/debug scanner and the matches all act through {@code onItemUseFirst}. The
         * dispatcher decides by registry id first, so the other ~1270 multi-items fall straight
         * through to {@code PASS} without allocating anything.
         */
        @Override
        public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
            com.gregtech.gregtech.item.behavior.ItemBehaviors.Outcome outcome =
                    com.gregtech.gregtech.item.behavior.ItemBehaviors.useOn(
                            context.getLevel(), context.getClickedPos(), context.getClickedFace(),
                            context.getPlayer(), stack,
                            (float) (context.getClickLocation().x - context.getClickedPos().getX()),
                            (float) (context.getClickLocation().y - context.getClickedPos().getY()),
                            (float) (context.getClickLocation().z - context.getClickedPos().getZ()));
            if (context.getPlayer() != null) {
                context.getPlayer().setItemInHand(context.getHand(), outcome.stack());
            }
            return outcome.acted()?InteractionResult.SUCCESS:InteractionResult.PASS;
        }

        /** Entity half of the same dispatch (the extinguisher douses burning mobs, and so on). */
        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                      InteractionHand hand) {
            com.gregtech.gregtech.item.behavior.ItemBehaviors.Outcome outcome =
                    com.gregtech.gregtech.item.behavior.ItemBehaviors.useOnEntity(target, player, stack);
            player.setItemInHand(hand,outcome.stack());
            return outcome.acted()?InteractionResult.SUCCESS:InteractionResult.PASS;
        }

        /**
         * §110: GT6's multi-item {@code onItemRightClick} slot. Of the port's ~1270 multi-items only the
         * remote activator has one ({@code Behavior_Remote:76-90}, "activate every bound coordinate of
         * this dimension"), so everything else falls straight through.
         *
         * <p><b>Falling through means calling {@code super}, not returning {@code PASS}.</b>
         * {@code Item.use}'s default implementation is what starts the eating animation for an edible
         * item, and ~60 of the port's multi-items are GT6 food ({@code GTMultiItems:94-98}). Returning
         * a bare {@code pass(held)} here would have quietly made every one of them inedible — the same
         * "what happens when the behaviour is not mine" question §108.7.3 is about.</p>
         */
        @Override
        public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, Player player,
                                                      InteractionHand hand) {
            ItemStack held = player.getItemInHand(hand);
            com.gregtech.gregtech.item.behavior.ItemBehaviors.Outcome outcome =
                    com.gregtech.gregtech.item.behavior.ItemBehaviors.useInAir(level, player, held);
            if (!outcome.acted()) return super.use(level, player, hand);
            player.setItemInHand(hand, outcome.stack());
            return InteractionResultHolder.success(outcome.stack());
        }
    }
