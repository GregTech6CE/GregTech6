package gregtech.integration.jade;

import gregtech.block.machine.BasicMachineBlock;
import gregtech.block.machine.BoilerTankBlock;
import gregtech.block.machine.EngineBlock;
import gregtech.block.machine.MoldBasinBlock;
import gregtech.block.machine.MoldBlock;
import gregtech.block.machine.SmeltingCrucibleBlock;
import gregtech.block.machine.SolidBurningBoxBlock;
import gregtech.blockentity.machine.BasicMachineBlockEntity;
import gregtech.blockentity.machine.BoilerTankBlockEntity;
import gregtech.blockentity.machine.EngineBaseBlockEntity;
import gregtech.blockentity.machine.MoldBasinBlockEntity;
import gregtech.blockentity.machine.MoldBlockEntity;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity;
*///?} else {
import gregtech.blockentity.machine.SmeltingCrucibleBlockEntity;
//?}
import gregtech.blockentity.machine.SolidBurningBoxBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Optional Jade integration; loaded only when Jade is present. Not a hard mod dependency. */
@WailaPlugin
public class GTJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MagicFieldAbsorberJadeProvider.INSTANCE, gregtech.blockentity.energy.MagicFieldAbsorberBlockEntity.class);
        registration.registerBlockDataProvider(EnergyBufferJadeProvider.INSTANCE, gregtech.blockentity.energy.PowerCellBlockEntity.class);
        registration.registerBlockDataProvider(DustFunnelJadeProvider.INSTANCE,
                gregtech.blockentity.tool.DustFunnelBlockEntity.class);
        registration.registerBlockDataProvider(TurbineJadeProvider.INSTANCE, gregtech.blockentity.energy.EnergyNodeBlockEntity.class);
        registration.registerBlockDataProvider(EnergyBufferJadeProvider.INSTANCE,gregtech.blockentity.energy.ChemicalBatteryBlockEntity.class);
        registration.registerBlockDataProvider(ExtenderJadeProvider.INSTANCE,gregtech.block.misc.ExtenderBlockEntity.class);
        registration.registerBlockDataProvider(ReactorJadeProvider.INSTANCE,gregtech.blockentity.energy.ReactorCoreBlockEntity.class);
        registration.registerBlockDataProvider(MultiblockJadeProvider.INSTANCE, net.minecraft.world.level.block.entity.BlockEntity.class);
        registration.registerBlockDataProvider(BurningBoxJadeProvider.INSTANCE, SolidBurningBoxBlockEntity.class);
//? if >=1.21.1 {
        /*registration.registerBlockDataProvider(CrucibleJadeProvider.INSTANCE, SmeltingCrucibleEntity.class);
*///?} else {
        registration.registerBlockDataProvider(CrucibleJadeProvider.INSTANCE, SmeltingCrucibleBlockEntity.class);
//?}
        registration.registerBlockDataProvider(CrucibleJadeProvider.INSTANCE, gregtech.blockentity.machine.MultiblockPortBlockEntity.class);
        registration.registerBlockDataProvider(MoldJadeProvider.INSTANCE, MoldBlockEntity.class);
        registration.registerBlockDataProvider(MoldJadeProvider.INSTANCE, MoldBasinBlockEntity.class);
        registration.registerBlockDataProvider(BasicMachineJadeProvider.INSTANCE, BasicMachineBlockEntity.class);
        registration.registerBlockDataProvider(EngineJadeProvider.INSTANCE, EngineBaseBlockEntity.class);
        registration.registerBlockDataProvider(BoilerJadeProvider.INSTANCE, BoilerTankBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MagicFieldAbsorberJadeProvider.INSTANCE, gregtech.block.energy.MagicFieldAbsorberBlock.class);
        registration.registerBlockComponent(EnergyBufferJadeProvider.INSTANCE, gregtech.block.energy.PowerCellBlock.class);
        registration.registerBlockComponent(DustFunnelJadeProvider.INSTANCE,
                gregtech.block.tool.DustFunnelBlock.class);
        registration.registerBlockComponent(TurbineJadeProvider.INSTANCE, gregtech.block.energy.EnergyNodeBlock.class);
        registration.registerBlockComponent(ConstructionNameJadeProvider.INSTANCE,
                gregtech.block.misc.ColoredConstructionBlock.class);
        registration.registerBlockComponent(ConstructionNameJadeProvider.INSTANCE,
                gregtech.block.misc.ConcreteBlock.class);
        registration.registerBlockComponent(EnergyBufferJadeProvider.INSTANCE,gregtech.block.energy.ChemicalBatteryBlock.class);
        registration.registerBlockComponent(ExtenderJadeProvider.INSTANCE,gregtech.block.misc.SourceExtenderBlock.class);
        registration.registerBlockComponent(ReactorJadeProvider.INSTANCE,gregtech.block.energy.ReactorCoreBlock.class);
        registration.registerBlockComponent(ReactorJadeProvider.INSTANCE,gregtech.block.energy.ReactorCore2x2Block.class);
        registration.registerBlockComponent(MultiblockJadeProvider.INSTANCE, net.minecraft.world.level.block.Block.class);
        registration.registerBlockComponent(BurningBoxJadeProvider.INSTANCE, SolidBurningBoxBlock.class);
        registration.registerBlockComponent(CrucibleJadeProvider.INSTANCE, SmeltingCrucibleBlock.class);
        registration.registerBlockComponent(CrucibleJadeProvider.INSTANCE, gregtech.block.machine.LargeCrucibleControllerBlock.class);
        registration.registerBlockComponent(CrucibleJadeProvider.INSTANCE, gregtech.block.machine.MultiblockPortBlock.class);
        registration.registerBlockComponent(MoldJadeProvider.INSTANCE, MoldBlock.class);
        registration.registerBlockComponent(MoldJadeProvider.INSTANCE, MoldBasinBlock.class);
        registration.registerBlockComponent(BasicMachineJadeProvider.INSTANCE, BasicMachineBlock.class);
        registration.registerBlockComponent(EngineJadeProvider.INSTANCE, EngineBlock.class);
        registration.registerBlockComponent(BoilerJadeProvider.INSTANCE, BoilerTankBlock.class);
        registration.registerBlockComponent(RockTwigJadeProvider.INSTANCE, gregtech.block.RockBlock.class);
        registration.registerBlockComponent(RockTwigJadeProvider.INSTANCE, gregtech.block.TwigBlock.class);
    }
}
