package com.gregtech.gregtech.item;

import com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Original TOOL_geigercounter reports each reactor rod's last neutron count and on/off state. */
public final class GeigerCounterItem extends TechItem {
    public GeigerCounterItem() { super("Geiger Counter", new Properties().stacksTo(1)); }
    public static Component reading(ReactorCoreBlockEntity core) {
        String values=java.util.Arrays.stream(core.neutrons).mapToObj(n->n+" n").collect(java.util.stream.Collectors.joining("; "));
        return Component.translatable("message.gregtech.geiger.reading",values,
                Component.translatable(core.stopped?"message.gregtech.reactor.stopped":"message.gregtech.reactor.running"));
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ReactorCoreBlockEntity core)) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide && context.getPlayer()!=null)
            context.getPlayer().displayClientMessage(reading(core),false);
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
