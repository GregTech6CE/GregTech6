package com.gregtech.gregtech.integration.jade;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.multiblock.StructureController;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Small, explicit inspection payload; never sends inventories or the full machine save. */
public enum MultiblockJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static CompoundTag inspect(BlockEntity entity) {
        var data = new CompoundTag();
        if (!(entity instanceof StructureController controller)) return data;
        data.putBoolean("formed", controller.isStructureOk());
        if(entity instanceof com.gregtech.gregtech.blockentity.machine.FusionReactorControllerBlockEntity fusion) {
            data.putLong("fusionCharge",fusion.chargeRemaining());
            data.putLong("fusionOutput",fusion.isRunning()?fusion.outputEU():0);
        }
        if(entity instanceof com.gregtech.gregtech.blockentity.machine.AxialGeneratorBlockEntity axial) {
            data.putString("axialState",axial.isOverloaded()?"overloaded":axial.isStopped()?"stopped":"enabled");
            data.putBoolean("axialSteam",axial.grade().steam());data.putInt("axialInput",axial.grade().input());data.putInt("axialOutput",axial.grade().output());
        }
        if (entity instanceof BasicMachineBlockEntity machine) {
            data.putInt("progress", machine.getProgressPercent());
            data.putBoolean("running", machine.isRunning());
        } else {
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
            var level=entity.getLevel();
            var handler=level==null?null:level.getCapability(Capabilities.FluidHandler.BLOCK,entity.getBlockPos(),null);
            if(handler!=null) {
                for (int i = 0; i < Math.min(16, handler.getTanks()); i++) {
                    var fluid = handler.getFluidInTank(i);
                    if (fluid.isEmpty()) continue;
                    var row = new CompoundTag();
                    row.put("fluid", fluid.save(level.registryAccess()));
                    row.putInt("capacity", handler.getTankCapacity(i));
                    fluids.add(row);
                }
            }
            data.put("fluids", fluids);
        }
        return data;
    }

    @Override public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        var entity = accessor.getBlockEntity();
        if (entity instanceof StructureController) tag.put("gt_structure", inspect(entity));
    }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains("gt_structure")) return;
        var data = accessor.getServerData().getCompound("gt_structure");
        tooltip.add(Component.translatable("jade.gregtech.multiblock." + (data.getBoolean("formed") ? "formed" : "incomplete"))
                .withStyle(data.getBoolean("formed") ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
        if (data.contains("progress")) tooltip.add(Component.translatable("jade.gregtech.multiblock.progress", data.getInt("progress")));
        if(data.contains("fusionCharge")) {
            tooltip.add(Component.translatable("gregtech.fusion.startup",data.getLong("fusionCharge")));
            tooltip.add(Component.translatable("gregtech.fusion.output",data.getLong("fusionOutput")));
        }
        if(data.contains("axialState")) {
            tooltip.add(Component.translatable("gt.axial.state."+data.getString("axialState")));
            tooltip.add(Component.translatable(data.getBoolean("axialSteam")?"gt.tooltip.axial.steam":"gt.tooltip.axial.dynamo",data.getInt("axialInput"),data.getInt("axialOutput")));
        }
        for (var value : data.getList("energy", 10)) {
            var row = (CompoundTag) value;
            tooltip.add(Component.translatable("jade.gregtech.multiblock.energy", row.getLong("stored"), row.getLong("capacity"), row.getString("unit")));
        }
        for (var value : data.getList("fluids", 10)) {
            var row = (CompoundTag) value;
            var fluid = net.neoforged.neoforge.fluids.FluidStack.parseOptional(accessor.getLevel().registryAccess(),row.getCompound("fluid"));
            tooltip.add(Component.translatable("jade.gregtech.multiblock.fluid", fluid.getDisplayName(), fluid.getAmount(), row.getInt("capacity")));
        }
        tooltip.add(Component.translatable("jade.gregtech.multiblock.jei"));
    }

    @Override public ResourceLocation getUid() { return ResourceLocation.fromNamespaceAndPath("gregtech", "multiblock"); }
}
