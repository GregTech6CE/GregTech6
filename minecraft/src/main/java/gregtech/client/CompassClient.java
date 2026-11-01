package gregtech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
@net.neoforged.fml.common.EventBusSubscriber(modid = "gt6ce",
        bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6ce",
        bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class CompassClient {
    private CompassClient() {}

    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (String mode : java.util.List.of("north", "face", "spawn", "center", "death"))
                ItemProperties.register(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "compass_" + mode)),
                        ResourceLocation.fromNamespaceAndPath("gt6ce", "compass_frame"), (stack, level, entity, seed) -> {
                            var player = Minecraft.getInstance().player;
                            if (player == null) return 0;
                            double yaw = player.getYRot(), angle;
                            if (mode.equals("north")) angle = yaw + 180;
                            else if (mode.equals("face")) angle = 180 - yaw;
                            else {
                                BlockPos target = mode.equals("spawn") ? player.level().getSharedSpawnPos()
                                        : mode.equals("center") ? BlockPos.ZERO : player.getLastDeathLocation()
                                                .filter(pos -> pos.dimension().equals(player.level().dimension()))
                                                .map(net.minecraft.core.GlobalPos::pos).orElse(null);
                                if (target == null) return Math.floorMod(player.level().getGameTime(), 16);
                                double x = target.getX() + .5 - player.getX(), z = target.getZ() + .5 - player.getZ();
                                if (x*x + z*z < 1) return Math.floorMod(player.level().getGameTime(), 16);
                                angle = yaw + 90 - Math.toDegrees(Math.atan2(z, x));
                            }
                            return Math.floorMod((int)Math.floor(.5 + 16 * angle / 360), 16);
                        });
        });
    }
}
