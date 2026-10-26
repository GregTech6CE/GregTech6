package gregtech.client;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.content.plant.BedrockFlowers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
//?}

/** GT6 BlockFlowersA/B tells the player which bedrock deposit each plant indicates. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, value = Dist.CLIENT)
//?}
public final class BedrockFlowerTooltips {
    private BedrockFlowerTooltips() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
//? if >=1.21.1 {
        /*var id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (id == null || !"gt6ce".equals(id.getNamespace())) return;
*///?} else {
        var id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null || !GregTech.NAMESPACE.equals(id.getNamespace())) return;
//?}
        BedrockFlowers.Flower flower = BedrockFlowers.byId(id.getPath());
        if (flower == null) return;
        event.getToolTip().add(Component.translatable("tooltip.gt6ce.bedrock_flower.indicates",
                Component.translatable("tooltip.gt6ce.bedrock_flower.deposit." + flower.indicator()))
                .withStyle(ChatFormatting.GRAY));
        if (flower.occursInRealLife())
            event.getToolTip().add(Component.translatable("tooltip.gt6ce.bedrock_flower.real_life")
                    .withStyle(ChatFormatting.DARK_GRAY));
    }
}
