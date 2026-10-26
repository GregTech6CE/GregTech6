package gregtech.compat.jade;

import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.machine.BoilerTankBlock;
import gregtech.content.block.machine.EngineBlock;
import gregtech.content.block.machine.MoldBasinBlock;
import gregtech.content.block.machine.MoldBlock;
import gregtech.content.block.machine.SmeltingCrucibleBlock;
import gregtech.content.block.machine.SolidBurningBoxBlock;
import gregtech.content.block.misc.DungeonPortalBlock;
import gregtech.content.block.misc.DungeonPortalBlockEntity;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.machine.BoilerTankBlockEntity;
import gregtech.content.blockentity.machine.EngineBaseBlockEntity;
import gregtech.content.blockentity.machine.MoldBasinBlockEntity;
import gregtech.content.blockentity.machine.MoldBlockEntity;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity;
*///?} else {
import gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity;
//?}
import gregtech.content.blockentity.machine.SolidBurningBoxBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Optional Jade integration; loaded only when Jade is present. Not a hard mod dependency. */
@WailaPlugin
public class GTJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(OmniJadeProvider.INSTANCE, net.minecraft.world.level.block.entity.BlockEntity.class);
        registration.registerBlockDataProvider(FluidVesselJadeProvider.INSTANCE, gregtech.content.blockentity.machine.TankBlockEntity.class);
        registration.registerBlockDataProvider(FluidVesselJadeProvider.INSTANCE, gregtech.content.blockentity.machine.FluidPipeBlockEntity.class);
        registration.registerBlockDataProvider(MagicFieldAbsorberJadeProvider.INSTANCE, gregtech.content.blockentity.energy.MagicFieldAbsorberBlockEntity.class);
        registration.registerBlockDataProvider(EnergyBufferJadeProvider.INSTANCE, gregtech.content.blockentity.energy.PowerCellBlockEntity.class);
        registration.registerBlockDataProvider(DustFunnelJadeProvider.INSTANCE,
                gregtech.content.blockentity.tool.DustFunnelBlockEntity.class);
        registration.registerBlockDataProvider(TurbineJadeProvider.INSTANCE, gregtech.content.blockentity.energy.EnergyNodeBlockEntity.class);
        registration.registerBlockDataProvider(EnergyBufferJadeProvider.INSTANCE,gregtech.content.blockentity.energy.ChemicalBatteryBlockEntity.class);
        registration.registerBlockDataProvider(ExtenderJadeProvider.INSTANCE,gregtech.content.block.misc.ExtenderBlockEntity.class);
        registration.registerBlockDataProvider(ReactorJadeProvider.INSTANCE,gregtech.content.blockentity.energy.ReactorCoreBlockEntity.class);
        registration.registerBlockDataProvider(PortalJadeProvider.INSTANCE, DungeonPortalBlockEntity.class);
        registration.registerBlockDataProvider(MultiblockJadeProvider.INSTANCE, net.minecraft.world.level.block.entity.BlockEntity.class);
        registration.registerBlockDataProvider(BurningBoxJadeProvider.INSTANCE, SolidBurningBoxBlockEntity.class);
//? if >=1.21.1 {
        /*registration.registerBlockDataProvider(CrucibleJadeProvider.INSTANCE, SmeltingCrucibleEntity.class);
*///?} else {
        registration.registerBlockDataProvider(CrucibleJadeProvider.INSTANCE, SmeltingCrucibleBlockEntity.class);
//?}
        registration.registerBlockDataProvider(CrucibleJadeProvider.INSTANCE, gregtech.content.blockentity.machine.MultiblockPortBlockEntity.class);
        registration.registerBlockDataProvider(MoldJadeProvider.INSTANCE, MoldBlockEntity.class);
        registration.registerBlockDataProvider(MoldJadeProvider.INSTANCE, MoldBasinBlockEntity.class);
        registration.registerBlockDataProvider(BasicMachineJadeProvider.INSTANCE, BasicMachineBlockEntity.class);
        registration.registerBlockDataProvider(EngineJadeProvider.INSTANCE, EngineBaseBlockEntity.class);
        registration.registerBlockDataProvider(BoilerJadeProvider.INSTANCE, BoilerTankBlockEntity.class);
        registration.registerBlockDataProvider(BoilerJadeProvider.INSTANCE, gregtech.content.blockentity.machine.OriginalLargeBoilerControllerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(OmniJadeProvider.INSTANCE, net.minecraft.world.level.block.Block.class);
        registration.registerBlockComponent(FluidVesselJadeProvider.INSTANCE, gregtech.content.block.machine.TankBlock.class);
        registration.registerBlockComponent(FluidVesselJadeProvider.INSTANCE, gregtech.content.block.machine.FluidPipeBlock.class);
        registration.registerBlockComponent(MagicFieldAbsorberJadeProvider.INSTANCE, gregtech.content.block.energy.MagicFieldAbsorberBlock.class);
        registration.registerBlockComponent(EnergyBufferJadeProvider.INSTANCE, gregtech.content.block.energy.PowerCellBlock.class);
        registration.registerBlockComponent(DustFunnelJadeProvider.INSTANCE,
                gregtech.content.block.tool.DustFunnelBlock.class);
        registration.registerBlockComponent(TurbineJadeProvider.INSTANCE, gregtech.content.block.energy.EnergyNodeBlock.class);
        registration.registerBlockComponent(ConstructionNameJadeProvider.INSTANCE,
                gregtech.content.block.misc.ColoredConstructionBlock.class);
        registration.registerBlockComponent(ConstructionNameJadeProvider.INSTANCE,
                gregtech.content.block.misc.ConcreteBlock.class);
        registration.registerBlockComponent(WorldFluidNameJadeProvider.INSTANCE,
                gregtech.content.block.GTWorldFluidBlock.class);
        registration.registerBlockComponent(EnergyBufferJadeProvider.INSTANCE,gregtech.content.block.energy.ChemicalBatteryBlock.class);
        registration.registerBlockComponent(ExtenderJadeProvider.INSTANCE,gregtech.content.block.misc.SourceExtenderBlock.class);
        registration.registerBlockComponent(ReactorJadeProvider.INSTANCE,gregtech.content.block.energy.ReactorCoreBlock.class);
        registration.registerBlockComponent(ReactorJadeProvider.INSTANCE,gregtech.content.block.energy.ReactorCore2x2Block.class);
        registration.registerBlockComponent(PortalJadeProvider.INSTANCE, DungeonPortalBlock.class);
        registration.registerBlockComponent(MultiblockJadeProvider.INSTANCE, net.minecraft.world.level.block.Block.class);
        registration.registerBlockComponent(BurningBoxJadeProvider.INSTANCE, SolidBurningBoxBlock.class);
        registration.registerBlockComponent(CrucibleJadeProvider.INSTANCE, SmeltingCrucibleBlock.class);
        registration.registerBlockComponent(CrucibleJadeProvider.INSTANCE, gregtech.content.block.machine.LargeCrucibleControllerBlock.class);
        registration.registerBlockComponent(CrucibleJadeProvider.INSTANCE, gregtech.content.block.machine.MultiblockPortBlock.class);
        registration.registerBlockComponent(MoldJadeProvider.INSTANCE, MoldBlock.class);
        registration.registerBlockComponent(MoldJadeProvider.INSTANCE, MoldBasinBlock.class);
        registration.registerBlockComponent(BasicMachineJadeProvider.INSTANCE, BasicMachineBlock.class);
        registration.registerBlockComponent(EngineJadeProvider.INSTANCE, EngineBlock.class);
        registration.registerBlockComponent(BoilerJadeProvider.INSTANCE, BoilerTankBlock.class);
        registration.registerBlockComponent(BoilerJadeProvider.INSTANCE, gregtech.content.block.machine.OriginalLargeBoilerControllerBlock.class);
        registration.registerBlockComponent(RockTwigJadeProvider.INSTANCE, gregtech.content.block.RockBlock.class);
        registration.registerBlockComponent(RockTwigJadeProvider.INSTANCE, gregtech.content.block.TwigBlock.class);
    }
}
