package gregtech.content.item;

import gregtech.content.blockentity.energy.ReactorCoreBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Original TOOL_geigercounter reports each reactor rod's last neutron count and on/off state. */
public final class GeigerCounterItem extends TechItem {

    public GeigerCounterItem() {
        super("Geiger Counter", new Properties().stacksTo(1));
    }

    public static Component reading(ReactorCoreBlockEntity core) {
        String neutronReadings = java.util.Arrays.stream(core.neutrons)
                .mapToObj(neutronCount -> neutronCount + " n")
                .collect(java.util.stream.Collectors.joining("; "));
        return Component.translatable(
                "message.gt6ce.geiger.reading",
                neutronReadings,
                Component.translatable(
                        core.stopped ? "message.gt6ce.reactor.stopped" : "message.gt6ce.reactor.running"));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ReactorCoreBlockEntity core))
            return InteractionResult.PASS;
        if (!context.getLevel().isClientSide && context.getPlayer() != null)
            context.getPlayer().displayClientMessage(reading(core), false);
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
