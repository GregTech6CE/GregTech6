package com.gregtech.gregtech.event;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.GTMachineBlock;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.blockentity.machine.CrucibleFaucetBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MoldBasinBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MoldBlockEntity;
import com.gregtech.gregtech.registry.GTSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles interactions with smeltery components (mold, basin, faucet, crossing).
 */
@Mod.EventBusSubscriber(modid = GregTech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SmelteryInteractionHandler {
    private SmelteryInteractionHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = event.getLevel().getBlockEntity(pos);
        if (blockEntity == null) return;

        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        BlockHitResult hit = event.getHitVec();

        if (blockEntity instanceof MoldBlockEntity mold) {
            if (event.getLevel().isClientSide()) {
                player.swing(event.getHand());
                if (hit.getDirection() == Direction.UP) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.CONSUME);
                }
                return;
            }
            InteractionResult result = handleMoldInteraction(mold, player, event.getHand(), held, event.getLevel(), hit);
            if (result != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        } else if (blockEntity instanceof MoldBasinBlockEntity basin) {
            if (event.getLevel().isClientSide()) {
                player.swing(event.getHand());
                if (hit.getDirection() == Direction.UP) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.CONSUME);
                }
                return;
            }
            InteractionResult result = handleBasinInteraction(basin, player, event.getHand(), held, event.getLevel(), hit);
            if (result != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        } else if (blockEntity instanceof CrucibleFaucetBlockEntity faucet) {
            if (event.getLevel().isClientSide()) {
                player.swing(event.getHand());
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
                return;
            }
            InteractionResult result = handleFaucetInteraction(faucet, player, event.getHand(), held, event.getLevel());
            if (result != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockEntity blockEntity = event.getLevel().getBlockEntity(pos);
        if (blockEntity == null) return;

        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        Direction clickedFace = event.getFace();

        if (blockEntity instanceof MoldBlockEntity mold) {
            double hitX = 0.5 + clickedFace.getStepX() * 0.5;
            double hitZ = 0.5 + clickedFace.getStepZ() * 0.5;
            handleMoldToolClick(mold, player, held, clickedFace, hitX, hitZ, event.getLevel(), pos);
        }
    }

    /** Play wrench sound when a machine block is dismantled with a wrench. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getState().getBlock() instanceof GTMachineBlock)) return;
        Player player = event.getPlayer();
        if (GTToolHelper.isMachineWrench(player.getMainHandItem())) {
            event.getLevel().playSound(null, event.getPos(), GTSounds.WRENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private static InteractionResult handleMoldInteraction(MoldBlockEntity mold, Player player,
            InteractionHand hand, ItemStack held, Level level, BlockHitResult hit) {
        // Pincers or empty hand: pickup when mold has content, pour when empty
        if (held.isEmpty() || GTToolHelper.matchesTool(held, GTToolType.PINCERS)) {
            boolean usingPincers = GTToolHelper.matchesTool(held, GTToolType.PINCERS);
            boolean pickedUp = mold.tryPickupWithPincers(player, usingPincers);
            if (pickedUp) {
                level.playSound(null, mold.getBlockPos(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.3F, 1.0F);
                if (usingPincers) {
                    GTToolHelper.damageForUse(held, 1, player);
                }
                player.swing(hand);
                return InteractionResult.CONSUME;
            }
            InteractionResult pourResult = mold.tryPour(player);
            if (pourResult != InteractionResult.PASS) {
                player.swing(hand);
                return pourResult;
            }
            return InteractionResult.PASS;
        }
        // Chisel shape selection on top face
        if (GTToolHelper.matchesTool(held, GTToolType.CHISEL)) {
            if (hit.getDirection() == Direction.UP) {
                double hitX = hit.getLocation().x - mold.getBlockPos().getX();
                double hitZ = hit.getLocation().z - mold.getBlockPos().getZ();
                boolean changed = mold.trySelectShape(player, hand, hitX, hitZ);
                if (changed) {
                    level.playSound(null, mold.getBlockPos(), SoundEvents.METAL_BREAK, SoundSource.BLOCKS, 0.5F, 0.5F);
                    GTToolHelper.damageForUse(held, 1, player);
                }
            }
            player.swing(hand);
            return InteractionResult.CONSUME;
        }
        // Soft hammer: reset settings
        if (GTToolHelper.matchesTool(held, GTToolType.SOFT_HAMMER)) {
            mold.resetSettings();
            level.playSound(null, mold.getBlockPos(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.1F, 1.0F);
            GTToolHelper.damageForUse(held, 1, player);
            player.swing(hand);
            return InteractionResult.CONSUME;
        }
        // Monkey wrench: toggle auto-input / redstone
        if (GTToolHelper.matchesTool(held, GTToolType.MONKEY_WRENCH)) {
            Direction clickedFace = hit.getDirection();
            if (clickedFace == Direction.UP) {
                mold.toggleRedstoneControl();
            } else if (clickedFace.getAxis().isHorizontal()) {
                mold.toggleAutoInput(clickedFace);
            }
            level.playSound(null, mold.getBlockPos(), GTSounds.WRENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            GTToolHelper.damageForUse(held, 1, player);
            player.swing(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult handleBasinInteraction(MoldBasinBlockEntity basin, Player player,
            InteractionHand hand, ItemStack held, Level level, BlockHitResult hit) {
        // Pincers or empty hand: pickup when basin has content, pour when empty
        if (hit.getDirection() == Direction.UP && (held.isEmpty() || GTToolHelper.matchesTool(held, GTToolType.PINCERS))) {
            boolean usingPincers = GTToolHelper.matchesTool(held, GTToolType.PINCERS);
            boolean pickedUp = basin.tryPickupWithPincers(player, usingPincers);
            if (pickedUp) {
                level.playSound(null, basin.getBlockPos(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.3F, 1.0F);
                if (usingPincers) {
                    GTToolHelper.damageForUse(held, 1, player);
                }
                player.swing(hand);
                return InteractionResult.CONSUME;
            }
            InteractionResult pourResult = basin.tryPour(player);
            if (pourResult != InteractionResult.PASS) {
                player.swing(hand);
                return pourResult;
            }
            return InteractionResult.PASS;
        }
        // Monkey wrench to toggle auto-pull on top face
        if (GTToolHelper.matchesTool(held, GTToolType.MONKEY_WRENCH)) {
            basin.toggleAutoInput(Direction.UP);
            GTToolHelper.damageForUse(held, 1, player);
            player.swing(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult handleFaucetInteraction(CrucibleFaucetBlockEntity faucet, Player player,
            InteractionHand hand, ItemStack held, Level level) {
        // Wrench rotation is handled by CrucibleFaucetBlock.use() — let it pass through
        if (GTToolHelper.isMachineWrench(held)) {
            return InteractionResult.PASS;
        }
        boolean poured = faucet.pullFromCrucible();
        if (poured) {
            level.playSound(null, faucet.getBlockPos(), SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        player.swing(hand);
        return InteractionResult.CONSUME;
    }

    private static void handleMoldToolClick(MoldBlockEntity mold, Player player, ItemStack held,
            Direction clickedFace, double hitX, double hitZ, Level level, BlockPos pos) {
        boolean consumed = false;

        if (GTToolHelper.matchesTool(held, GTToolType.CHISEL) && clickedFace == Direction.UP) {
            consumed = mold.trySelectShape(player, InteractionHand.MAIN_HAND, hitX, hitZ);
            if (consumed) {
                level.playSound(null, pos, SoundEvents.METAL_BREAK, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
        } else if (GTToolHelper.matchesTool(held, GTToolType.SOFT_HAMMER)) {
            mold.resetSettings();
            consumed = true;
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.1F, 1.0F);
        } else if (GTToolHelper.matchesTool(held, GTToolType.MONKEY_WRENCH)) {
            if (clickedFace == Direction.UP) {
                mold.toggleRedstoneControl();
            } else if (clickedFace.getAxis().isHorizontal()) {
                mold.toggleAutoInput(clickedFace);
            }
            level.playSound(null, pos, GTSounds.WRENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            consumed = true;
        } else if (GTToolHelper.matchesTool(held, GTToolType.PINCERS)) {
            boolean pickedUp = mold.tryPickupWithPincers(player, true);
            if (pickedUp) {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.3F, 1.0F);
                GTToolHelper.damageForUse(held, 1, player);
            }
            return;
        }

        if (consumed) {
            GTToolHelper.damageForUse(held, 1, player);
        }
    }
}
