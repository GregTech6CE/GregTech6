package gregtech.engine.inventory;

import gregtech.client.gui.AdvancedCraftingMenu;
import gregtech.content.block.inventory.DrawerQuadBlock;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.machine.FunctionalPartInteraction;
import gregtech.content.block.misc.AdvancedCraftingTableBlock;
import gregtech.content.block.misc.AdvancedCraftingTableBlockEntity;
import gregtech.content.block.misc.FilterBlockEntity;
import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.content.blockentity.inventory.DrawerQuadBlockEntity;
import gregtech.content.blockentity.inventory.SafeBlockEntity;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.machine.BehaviorMachineBlockEntity;
import gregtech.content.blockentity.machine.FunctionalPartRuntime;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
//? if <1.21.1 {
import net.minecraftforge.network.NetworkHooks;
//?}

/** Opens existing GT menus without running tools, container transfers, or machine interactions. */
public final class SpectatorMenus {
    private SpectatorMenus() {}

    public static boolean open(ServerPlayer player, Level level, BlockHitResult hit) {
        if (!player.isSpectator()) return false;
        var pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("gt6ce")) return false;
        var entity = level.getBlockEntity(pos);
        Component title = state.getBlock().getName();
        if (entity instanceof BasicMachineBlockEntity machine && state.getBlock() instanceof BasicMachineBlock block) {
            block.openMenu(player, machine);
        } else if (entity instanceof BehaviorMachineBlockEntity machine) {
//? if >=1.21.1 {
            /*player.openMenu(machine, buf -> buf.writeBlockPos(pos));
*///?} else {
            NetworkHooks.openScreen(player, machine, buf -> buf.writeBlockPos(pos));
//?}
        } else if (entity instanceof AdvancedCraftingTableBlockEntity table) {
            boolean upper = hit.getDirection() == Direction.UP;
            if (!upper && hit.getDirection().getAxis() != state.getValue(AdvancedCraftingTableBlock.FACING).getAxis()) return false;
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> upper
                    ? new AdvancedCraftingMenu(id, inv, table) : table.storageMenu(id, inv), title));
        } else if (entity instanceof DrawerQuadBlockEntity drawer) {
            Direction facing = state.getValue(DrawerQuadBlock.FACING);
            if (hit.getDirection() != facing) return false;
            int page = DrawerQuadBlock.quadrant(facing, pos, hit.getLocation());
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> drawer.createMenu(page, id, inv),
                    Component.translatable("gui.gt6ce.drawer.page", title, page + 1)));
        } else if (entity instanceof SafeBlockEntity safe) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> safe.createMenu(id, inv), title));
        } else if (entity instanceof EnergyNodeBlockEntity node && node.isBatteryBox()) {
            player.openMenu(node.batteryMenu());
        } else if (entity instanceof FunctionalPartRuntime.Host host
                && host.function() != null && host.function().opensMenu()) {
            FunctionalPartInteraction.openMenu(player, host);
        } else if (entity instanceof FilterBlockEntity filter) {
//? if >=1.21.1 {
            /*player.openMenu(filter, buf -> buf.writeBoolean(filter.prefixMode()));
*///?} else {
            NetworkHooks.openScreen(player, filter, buf -> buf.writeBoolean(filter.prefixMode()));
//?}
        } else if (entity instanceof MenuProvider provider) {
            player.openMenu(provider);
        } else {
            return false;
        }
        return true;
    }
}
