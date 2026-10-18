package com.gregtech.gregtech.client;

//? if >=1.21.1 {
//?} else {
import com.gregtech.gregtech.GregTech;
//?}
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTToolItems;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

//? if >=1.21.1 {
/*@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class GTToolClient {
    private GTToolClient() {}

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (!GTToolHelper.isTool(stack)) {
//? if >=1.21.1 {
                /*return 0xFFFFFFFF;
*///?} else {
                return 0xFFFFFF;
//?}
            }
            int headColor = 0xFF000000 | (GTToolHelper.getHead(stack).getColor() & 0xFFFFFF);
            if (tintIndex == ToolIconSets.OVERLAY_TINT) {
//? if >=1.21.1 {
                /*return 0xFFFFFFFF;
*///?} else {
                return 0xFFFFFF;
//?}
            }
            GTToolType type = ((GTToolItem)stack.getItem()).toolType();
            if(type.definition().isGun())return tintIndex==0?headColor:tintIndex==1?0xFF000000|(GTToolHelper.getHandle(stack).getColor()&0xFFFFFF):0xFFFFFFFF;
            if (type == GTToolType.GEM_PICK) {
                if (tintIndex == 0) {
                    return 0xFF000000 | (GTToolHelper.getHandle(stack).getColor() & 0xFFFFFF);
                }
                if (tintIndex == 1) {
                    return headColor;
                }
//? if >=1.21.1 {
                /*return 0xFFFFFFFF;
*///?} else {
                return 0xFFFFFF;
//?}
            }
            if (type.isHeadless()) {
//? if >=1.21.1 {
                /*return tintIndex == 0 ? headColor : 0xFFFFFFFF;
*///?} else {
                return tintIndex == 0 ? headColor : 0xFFFFFF;
//?}
            }
            if (tintIndex == 0) {
                return 0xFF000000 | (GTToolHelper.getHandle(stack).getColor() & 0xFFFFFF);
            }
            if (tintIndex == 1) {
                return headColor;
            }
//? if >=1.21.1 {
            /*return 0xFFFFFFFF;
*///?} else {
            return 0xFFFFFF;
//?}
        }, GTToolItems.all().values().stream().map(holder -> holder.get()).toArray(net.minecraft.world.item.Item[]::new));
    }
}
