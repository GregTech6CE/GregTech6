package gregtech.event;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.Materials;
import gregtech.content.registry.GTItems;
import gregtech.engine.fluid.HandContainerTransfer;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
@net.neoforged.fml.common.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
/**
 * GT6 BlockTreeLogA/B/C's axe/saw/knife click applied to logs the port does not own (vanilla and
 * registered modded logs): strip them and drop bark. GT logs are handled by {@code WoodLogBlock#use}
 * because their strip target needs the species-specific stripped block.
 */
public final class LogStrippingEvents {
    private LogStrippingEvents() {}

    @SubscribeEvent
    public static void strip(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (player == null || stack.isEmpty()) return;
        boolean axe = GTToolHelper.matchesTool(stack, GTToolType.AXE)
                || GTToolHelper.matchesTool(stack, GTToolType.DOUBLE_AXE);
        boolean sawOrKnife = GTToolHelper.matchesTool(stack, GTToolType.SAW)
                || GTToolHelper.matchesTool(stack, GTToolType.KNIFE);
        if (!axe && !sawOrKnife) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof gregtech.content.block.wood.WoodLogBlock) return;
        BlockState stripped = AxeItem.getAxeStrippingState(state);
        if (stripped == null) return;
        if (level.isClientSide) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (!level.setBlock(pos, stripped, 3)) return;
        level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
        HandContainerTransfer.give(player, GTItems.getStack(MaterialPrefix.dust, Materials.Bark, 1));
        EquipmentSlot slot = event.getHand() == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        GTToolHelper.damageForToolClickReturn(stack, axe ? 500L : 1000L, player, slot);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }
}
