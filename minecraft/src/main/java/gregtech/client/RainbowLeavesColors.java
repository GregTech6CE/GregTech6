package gregtech.client;

import gregtech.content.block.wood.WoodSpecies;
import gregtech.content.registry.GTWoods;
import net.minecraft.core.BlockPos;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
//?}

@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RainbowLeavesColors {
    private static final int[] COLORS = {0xff0000, 0xff4000, 0xff8000, 0xffc000, 0xffff00, 0xc0ff00,
            0x80ff00, 0x40ff00, 0x00ff00, 0x00ff40, 0x00ff80, 0x00ffc0, 0x00ffff, 0x00c0ff,
            0x0080ff, 0x0040ff, 0x0000ff, 0x4000ff, 0x8000ff, 0xc000ff, 0xff00ff, 0xff00c0, 0xff0080, 0xff0040};
    private RainbowLeavesColors() {}

    private static int color(BlockPos pos) {
        if (pos == null) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            return COLORS[level == null ? 0 : (int) Math.floorMod(level.getGameTime() / 10, COLORS.length)];
        }
        long sum = Math.abs((long) pos.getX()) + Math.abs((long) pos.getY()) + Math.abs((long) pos.getZ());
        return COLORS[(int) (sum % COLORS.length)];
    }

    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint == 0 ? color(pos) : 0xffffff,
                GTWoods.leaves(WoodSpecies.RAINBOWOOD));
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
//? if >=1.21.1 {
        /*event.register(ItemColorARGB.opaque((stack, tint) -> tint == 0 ? color(null) : 0xffffff),
                GTWoods.leaves(WoodSpecies.RAINBOWOOD).asItem());
*///?} else {
        event.register((stack, tint) -> tint == 0 ? color(null) : 0xffffff,
                GTWoods.leaves(WoodSpecies.RAINBOWOOD).asItem());
//?}
    }
}
