package gregtech.content.block.machine;

import gregtech.engine.machine.BasicMachineSpec;
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
        setBeTypeSupplier(()->gregtech.content.registry.GTBlockEntities.COKE_OVEN.get());
    }
    public static BasicMachineSpec makeSpec() {
        var p=gregtech.content.multiblock.OriginalMultiblockMachineParameters.cokeOven();
        return new BasicMachineSpec(p.id(),p.material(),p.machineName(),p.energyType(),p.tier(),p.energyIn(),p.energyOut(),
                p.hardness(),p.blastResistance(),gregtech.engine.energy.FaceConfig.from(p.faceConfig()),p.constructionMaterials(),
                gregtech.content.data.MachineRecipeMaps.CokeOven,p.parallelLimit(),p.energyInMin(),p.energyInMax());
    }
    @Override public gregtech.content.blockentity.machine.BasicMachineBlockEntity createBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state) {
        return new gregtech.content.blockentity.machine.CokeOvenControllerBlockEntity(pos,state);
    }
//? if >=1.21.1 {
    /*@Override public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
*///?} else {
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.BlockGetter context,List<Component> tooltip,TooltipFlag flag) {
//?}
        gregtech.client.OriginalControllerTooltips.structure(
                gregtech.content.multiblock.OriginalControllerTooltipData.Family.COKE_OVEN,tooltip);
        gregtech.client.OriginalControllerTooltips.basic(basicSpec(),tooltip);
    }
}
