package gregtech.compat.jade;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.*;

/** Keep Jade's ordinary GT storage bars on the same unit policy as explicit machine providers. */
public enum FluidUnitsJadeProvider implements
//? if >=1.21.1 {
        /*IServerExtensionProvider<CompoundTag>,
*///?} else {
        IServerExtensionProvider<BlockEntity, CompoundTag>,
//?}
        IClientExtensionProvider<CompoundTag, FluidView> {
    INSTANCE;

    @Override public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_units");
    }

    @Override public int getDefaultPriority() { return -100; }

    @Override
//? if >=1.21.1 {
    /*public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
        return accessor instanceof BlockAccessor block ? groups(block.getBlockEntity()) : null;
*///?} else {
    public List<ViewGroup<CompoundTag>> getGroups(ServerPlayer player, ServerLevel world,
                                                 BlockEntity entity, boolean showDetails) {
        return groups(entity);
//?}
    }

    private static List<ViewGroup<CompoundTag>> groups(BlockEntity entity) {
        if (entity == null || entity.getLevel() == null
                || !BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock()).getNamespace().equals("gt6ce")) return null;
//? if >=1.21.1 {
        /*var handler = entity.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                entity.getBlockPos(), null);
*///?} else {
        var handler = entity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).orElse(null);
//?}
        if (handler == null) return null;
        var tanks = new ArrayList<CompoundTag>();
        for (int i = 0; i < handler.getTanks(); i++) {
            var fluid = handler.getFluidInTank(i);
            long amount = fluid.getAmount(), capacity = handler.getTankCapacity(i);
            if (handler instanceof gregtech.engine.fluid.FluidTankGT tank) {
                amount = tank.getAmount(); capacity = tank.capacity();
            } else if (handler.getTanks() == 1) {
                var storage = gregtech.engine.fluid.FluidStorageInspection.read(entity, handler);
                amount = storage.amount(); capacity = storage.capacity();
            }
            if (capacity > 0) tanks.add(FluidView.writeDefault(JadeFluidObject.of(fluid.getFluid(), amount), capacity));
        }
        return tanks.isEmpty() ? List.of() : List.of(new ViewGroup<>(tanks));
    }

    @Override public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor,
                                                                     List<ViewGroup<CompoundTag>> groups) {
        return ClientViewGroup.map(groups, JadeBars::readFluid, null);
    }
}
