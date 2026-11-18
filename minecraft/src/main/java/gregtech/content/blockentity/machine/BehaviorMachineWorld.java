package gregtech.content.blockentity.machine;

import com.mojang.authlib.GameProfile;
import gregtech.api.addon.machine.BehaviorRangeRules;
import gregtech.api.addon.platform.WorldAccess;
import gregtech.engine.fluid.CompressedGasReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

final class BehaviorMachineWorld implements WorldAccess {
    private final BlockEntity machine;
    private final int maxRange;
    private final BehaviorMachineItems items;
    private int breaksLeft;
    private int itemsLeft;
    @Nullable private UUID placerId;
    private String placerName = "";

    BehaviorMachineWorld(BlockEntity machine, int maxRange, BehaviorMachineItems items) {
        this.machine = machine;
        this.maxRange = maxRange;
        this.items = items;
    }

    void beginTick() {
        breaksLeft = BehaviorRangeRules.BREAKS_PER_TICK;
        itemsLeft = BehaviorRangeRules.ITEMS_PER_TICK;
    }

    void rememberPlacer(Player player) {
        if (player == null) return;
        placerId = player.getUUID();
        String name = player.getGameProfile().getName();
        placerName = name == null ? "" : name;
        machine.setChanged();
    }

    void writePlacer(CompoundTag tag) {
        if (placerId != null) {
            tag.putString("gt.placer", placerId.toString());
            tag.putString("gt.placer_name", placerName);
        }
    }

    void readPlacer(CompoundTag tag) {
        placerId = null;
        placerName = "";
        if (tag.contains("gt.placer")) {
            try {
                placerId = UUID.fromString(tag.getString("gt.placer"));
                placerName = tag.getString("gt.placer_name");
            } catch (IllegalArgumentException ignored) {
                placerId = null;
            }
        }
    }

    @Override
    public boolean breakBlock(BlockPos target) {
        Level level = machine.getLevel();
        if (level == null || level.isClientSide || !(level instanceof ServerLevel server) || target == null) return false;
        BlockPos origin = machine.getBlockPos();
        int dx = target.getX() - origin.getX();
        int dy = target.getY() - origin.getY();
        int dz = target.getZ() - origin.getZ();
        if (!BehaviorRangeRules.inside(maxRange, dx, dy, dz)) return false;
        if (breaksLeft <= 0 || !level.hasChunkAt(target)) return false;
        BlockState state = level.getBlockState(target);
        if (state.isAir() || state.getDestroySpeed(level, target) < 0) return false;
        Player actor = actor(server, origin);
        List<ItemStack> drops = Block.getDrops(state, server, target, level.getBlockEntity(target), actor, ItemStack.EMPTY);
        if (!items.dropsFit(drops) || !breakAllowed(server, target, state, actor)) return false;
        if (!level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) return false;
        for (ItemStack drop : drops) items.insertOutput(drop);
        breaksLeft--;
        machine.setChanged();
        return true;
    }

    @Override
    public int collectItems(int radius, boolean voidThem) {
        Level level = machine.getLevel();
        if (level == null || level.isClientSide || !(level instanceof ServerLevel server)) return 0;
        if (!BehaviorRangeRules.radiusAllowed(maxRange, radius) || itemsLeft <= 0) return 0;
        BlockPos origin = machine.getBlockPos();
        var box = new AABB(origin.getX() - radius, origin.getY() - radius, origin.getZ() - radius,
                origin.getX() + radius + 1.0, origin.getY() + radius + 1.0, origin.getZ() + radius + 1.0);
        int taken = 0;
        for (ItemEntity entity : server.getEntitiesOfClass(ItemEntity.class, box)) {
            if (itemsLeft <= 0) break;
            if (!entity.isAlive()) continue;
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) continue;
            if (voidThem) {
                entity.discard();
            } else {
                int before = stack.getCount();
                ItemStack left = items.insertOutput(stack.copy());
                if (left.getCount() == before) continue;
                if (left.isEmpty()) entity.discard();
                else entity.setItem(left);
            }
            itemsLeft--;
            taken++;
        }
        if (taken > 0) machine.setChanged();
        return taken;
    }

    @Override
    public int pushCompressedGas(Direction face, FluidStack stack) {
        Level level = machine.getLevel();
        if (face == null || stack == null || stack.isEmpty() || level == null) return 0;
        BlockPos target = machine.getBlockPos().relative(face);
        if (!level.hasChunkAt(target)) return 0;
        if (!(level.getBlockEntity(target) instanceof CompressedGasReceiver receiver)) return 0;
        return receiver.receiveCompressedGas(face.getOpposite(), stack);
    }

    private Player actor(ServerLevel server, BlockPos origin) {
        if (placerId != null && server.getServer() != null) {
            ServerPlayer online = server.getServer().getPlayerList().getPlayer(placerId);
            if (online != null) return online;
        }
        UUID id = placerId == null ? new UUID(0, 0) : placerId;
        String name = placerName == null || placerName.isBlank() ? "gt6ce" : placerName;
        if (name.length() > 16) name = name.substring(0, 16);
        var profile = new GameProfile(id, name);
//? if >=1.21.1 {
/*        var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.get(server, profile);
*///?} else {
        var fake = net.minecraftforge.common.util.FakePlayerFactory.get(server, profile);
//?}
        fake.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
        return fake;
    }

    private static boolean breakAllowed(ServerLevel server, BlockPos pos, BlockState state, Player player) {
//? if >=1.21.1 {
/*        var event = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(server, pos, state, player);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
*///?} else {
        var event = new net.minecraftforge.event.level.BlockEvent.BreakEvent(server, pos, state, player);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
//?}
    }
}
