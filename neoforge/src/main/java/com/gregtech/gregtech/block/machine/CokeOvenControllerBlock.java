package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** GT6 fire-brick coke oven using the native basic-machine GUI, tools and processing. */
public class CokeOvenControllerBlock extends BasicMachineBlock {
    public CokeOvenControllerBlock() {this(Properties.of());}
    public CokeOvenControllerBlock(Properties properties) {
        super(makeSpec(),properties.strength(5,5).sound(net.minecraft.world.level.block.SoundType.STONE).requiresCorrectToolForDrops());
        setBeTypeSupplier(()->com.gregtech.gregtech.registry.GTBlockEntities.COKE_OVEN.get());
    }
    public static BasicMachineSpec makeSpec() {
        var p=com.gregtech.gregtech.content.multiblock.OriginalMultiblockMachineParameters.cokeOven();
        return new BasicMachineSpec(p.id(),p.material(),p.machineName(),p.energyType(),p.tier(),p.energyIn(),p.energyOut(),
                p.hardness(),p.blastResistance(),com.gregtech.gregtech.api.energy.FaceConfig.from(p.faceConfig()),p.constructionMaterials(),
                com.gregtech.gregtech.data.MachineRecipeMaps.CokeOven,p.parallelLimit(),p.energyInMin(),p.energyInMax());
    }
    @Override public com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity createBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state) {
        return new com.gregtech.gregtech.blockentity.machine.CokeOvenControllerBlockEntity(pos,state);
    }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        super.appendHoverText(stack,context,tooltip,flag);
        tooltip.add(Component.translatable("gregtech.coke.structure"));
        tooltip.add(Component.translatable("gregtech.coke.controller"));
        tooltip.add(Component.translatable("gregtech.coke.ignition"));
    }
}
