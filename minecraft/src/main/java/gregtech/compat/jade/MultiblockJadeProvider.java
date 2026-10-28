package gregtech.compat.jade;

import gregtech.engine.energy.IEnergyBlock;
import gregtech.engine.multiblock.StructureController;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.data.GregTechTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
*///?} else {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Small, explicit inspection payload; never sends inventories or the full machine save. */
public enum MultiblockJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static CompoundTag inspect(BlockEntity entity) {
        var data = new CompoundTag();
        var inspected = entity;
        if (entity instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity port)
            entity = port.sensorTarget();
        if (!(entity instanceof StructureController) && entity.getLevel() instanceof net.minecraft.server.level.ServerLevel server) {
            var owner = gregtech.engine.multiblock.tracking.WorldStructureClaims.owner(server, entity.getBlockPos());
            if (owner != null && server.hasChunkAt(owner)) {
                var target = server.getBlockEntity(owner);
                if (target != null && !target.isRemoved()) entity = target;
            }
            if (entity == inspected && entity instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity) {
                // Incomplete atomic shells have no owner yet. Reuse the wand's unambiguous read-only lookup.
                var target = gregtech.engine.multiblock.tracking.StructureWatchIndex.toolTarget(server, entity.getBlockPos());
                if (target != null) entity = target.trackedEntity();
            }
        }
        if (entity == inspected && entity instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity) {
            data.putBoolean("unbound", true);
            return data;
        }
        if (!(entity instanceof StructureController controller)) return data;
        data.putBoolean("formed", controller.isStructureOk());
        if (entity != inspected) {
            data.putString("controller", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock()).toString());
            data.putLong("position", entity.getBlockPos().asLong());
            // Parts show the association and status, not another copy of every controller tank.
            return data;
        }
        if (entity instanceof gregtech.content.blockentity.machine.FusionReactorControllerBlockEntity fusion) {
            data.putLong("fusionCharge", fusion.chargeRemaining());
            data.putLong("fusionOutput", fusion.isRunning() ? fusion.outputEU() : 0);
        }
        if (entity instanceof gregtech.content.blockentity.machine.AxialGeneratorBlockEntity axial) {
            data.putString("axialState", axial.isOverloaded() ? "overloaded" : axial.isStopped() ? "stopped" : "enabled");
            data.putBoolean("axialSteam", axial.grade().steam());
            data.putInt("axialInput", axial.grade().input());
            data.putInt("axialOutput", axial.grade().output());
        }
        if (!(entity instanceof BasicMachineBlockEntity)) {
            final var inspectedController = entity;
            var energies = new ListTag();
            if (entity instanceof IEnergyBlock machine) {
                // Include fuel heat and rear-port RU buffers even when the controller itself has no energy port.
                for (var type : java.util.List.of(GregTechTags.Energy.EU, GregTechTags.Energy.RU,
                        GregTechTags.Energy.HU, GregTechTags.Energy.KU)) {
                    long capacity = machine.getEnergyCapacity(type, null);
                    if (capacity <= 0) continue;
                    var row = new CompoundTag();
                    row.putString("unit", type.getShortName());
                    row.putLong("stored", machine.getEnergyStored(type, null));
                    row.putLong("capacity", capacity);
                    energies.add(row);
                }
            }
            data.put("energy", energies);
            var fluids = new ListTag();
//? if >=1.21.1 {
            /*var level = entity.getLevel();
            var handler = level == null ? null : level.getCapability(Capabilities.FluidHandler.BLOCK, entity.getBlockPos(), null);
            if (handler != null) {
*///?} else {
            entity.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(handler -> {
//?}
                for (int i = 0; i < Math.min(16, handler.getTanks()); i++) {
                    var fluid = handler.getFluidInTank(i);
                    if (fluid.isEmpty()) continue;
                    var row = new CompoundTag();
//? if >=1.21.1 {
                    /*row.put("fluid", fluid.save(level.registryAccess()));
*///?} else {
                    row.put("fluid", fluid.writeToNBT(new CompoundTag()));
//?}
                    if (inspectedController instanceof gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity vessel) {
                        row.putLong("amount", vessel.fluidAmountForInspection());
                        row.putLong("capacity", vessel.fluidCapacityForInspection());
                    } else {
                        row.putLong("amount", fluid.getAmount());
                        row.putLong("capacity", handler.getTankCapacity(i));
                    }
                    fluids.add(row);
                }
//? if >=1.21.1 {
            /*}
*///?} else {
            });
//?}
            data.put("fluids", fluids);
        }
        return data;
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        var entity = accessor.getBlockEntity();
        if (entity == null) return;
        var data = inspect(entity);
        if (!data.isEmpty()) tag.put("gt_structure", data);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains("gt_structure")) return;
        var data = accessor.getServerData().getCompound("gt_structure");
        if (data.getBoolean("unbound")) {
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.multiblock.unbound")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            return;
        }
        if (data.contains("controller")) {
            var id = ResourceLocation.tryParse(data.getString("controller"));
            if (id != null) {
                var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id);
                var pos = net.minecraft.core.BlockPos.of(data.getLong("position"));
                JadeText.add(tooltip, Component.translatable("jade.gt6ce.multiblock.controller", block.getName(),
                        pos.getX(), pos.getY(), pos.getZ()));
            }
        }
        JadeText.add(tooltip, Component.translatable("jade.gt6ce.multiblock." + (data.getBoolean("formed") ? "formed" : "incomplete"))
                .withStyle(data.getBoolean("formed") ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
        if (data.contains("fusionCharge")) {
            JadeText.add(tooltip, Component.translatable("gt6ce.fusion.startup", data.getLong("fusionCharge")));
            JadeText.add(tooltip, Component.translatable("gt6ce.fusion.output", data.getLong("fusionOutput")));
        }
        if (data.contains("axialState")) {
            JadeText.add(tooltip, Component.translatable("gt.axial.state." + data.getString("axialState")));
            JadeText.add(tooltip, Component.translatable(data.getBoolean("axialSteam") ? "gt.tooltip.axial.steam" : "gt.tooltip.axial.dynamo",
                    data.getInt("axialInput"), data.getInt("axialOutput")));
        }
        for (var value : data.getList("energy", 10)) {
            var row = (CompoundTag) value;
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.multiblock.energy", row.getLong("stored"), row.getLong("capacity"),
                    row.getString("unit")));
        }
        for (var value : data.getList("fluids", 10)) {
            if (accessor.getBlockEntity() instanceof gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity)
                break;
            var row = (CompoundTag) value;
//? if >=1.21.1 {
            /*var fluid = net.neoforged.neoforge.fluids.FluidStack.parseOptional(accessor.getLevel().registryAccess(), row.getCompound("fluid"));
*///?} else {
            var fluid = net.minecraftforge.fluids.FluidStack.loadFluidStackFromNBT(row.getCompound("fluid"));
//?}
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.multiblock.fluid", fluid.getDisplayName(),
                    row.contains("amount") ? row.getLong("amount") : fluid.getAmount(), row.getLong("capacity")));
        }
        JadeText.add(tooltip, Component.translatable("jade.gt6ce.multiblock.jei"));
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "multiblock");
    }
}
