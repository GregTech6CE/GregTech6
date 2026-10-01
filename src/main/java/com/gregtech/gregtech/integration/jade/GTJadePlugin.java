package com.gregtech.gregtech.integration.jade;

import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.block.machine.BoilerTankBlock;
import com.gregtech.gregtech.block.machine.EngineBlock;
import com.gregtech.gregtech.block.machine.MoldBasinBlock;
import com.gregtech.gregtech.block.machine.MoldBlock;
import com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock;
import com.gregtech.gregtech.block.machine.SolidBurningBoxBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity;
import com.gregtech.gregtech.blockentity.machine.EngineBaseBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MoldBasinBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MoldBlockEntity;
import com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity;
import com.gregtech.gregtech.blockentity.machine.SolidBurningBoxBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Optional Jade integration; loaded only when Jade is present. Not a hard mod dependency. */
@WailaPlugin
public class GTJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(EnergyBufferJadeProvider.INSTANCE,com.gregtech.gregtech.blockentity.energy.ChemicalBatteryBlockEntity.class);
        registration.registerBlockDataProvider(ExtenderJadeProvider.INSTANCE,com.gregtech.gregtech.block.misc.ExtenderBlockEntity.class);
        registration.registerBlockDataProvider(ReactorJadeProvider.INSTANCE,com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity.class);
        registration.registerBlockDataProvider(MultiblockJadeProvider.INSTANCE, net.minecraft.world.level.block.entity.BlockEntity.class);
        registration.registerBlockDataProvider(BurningBoxJadeProvider.INSTANCE, SolidBurningBoxBlockEntity.class);
        registration.registerBlockDataProvider(CrucibleJadeProvider.INSTANCE, SmeltingCrucibleBlockEntity.class);
        registration.registerBlockDataProvider(CrucibleJadeProvider.INSTANCE, com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity.class);
        registration.registerBlockDataProvider(MoldJadeProvider.INSTANCE, MoldBlockEntity.class);
        registration.registerBlockDataProvider(MoldJadeProvider.INSTANCE, MoldBasinBlockEntity.class);
        registration.registerBlockDataProvider(BasicMachineJadeProvider.INSTANCE, BasicMachineBlockEntity.class);
        registration.registerBlockDataProvider(EngineJadeProvider.INSTANCE, EngineBaseBlockEntity.class);
        registration.registerBlockDataProvider(BoilerJadeProvider.INSTANCE, BoilerTankBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(EnergyBufferJadeProvider.INSTANCE,com.gregtech.gregtech.block.energy.ChemicalBatteryBlock.class);
        registration.registerBlockComponent(ExtenderJadeProvider.INSTANCE,com.gregtech.gregtech.block.misc.SourceExtenderBlock.class);
        registration.registerBlockComponent(ReactorJadeProvider.INSTANCE,com.gregtech.gregtech.block.energy.ReactorCoreBlock.class);
        registration.registerBlockComponent(ReactorJadeProvider.INSTANCE,com.gregtech.gregtech.block.energy.ReactorCore2x2Block.class);
        registration.registerBlockComponent(MultiblockJadeProvider.INSTANCE, net.minecraft.world.level.block.Block.class);
        registration.registerBlockComponent(BurningBoxJadeProvider.INSTANCE, SolidBurningBoxBlock.class);
        registration.registerBlockComponent(CrucibleJadeProvider.INSTANCE, SmeltingCrucibleBlock.class);
        registration.registerBlockComponent(CrucibleJadeProvider.INSTANCE, com.gregtech.gregtech.block.machine.LargeCrucibleControllerBlock.class);
        registration.registerBlockComponent(CrucibleJadeProvider.INSTANCE, com.gregtech.gregtech.block.machine.MultiblockPortBlock.class);
        registration.registerBlockComponent(MoldJadeProvider.INSTANCE, MoldBlock.class);
        registration.registerBlockComponent(MoldJadeProvider.INSTANCE, MoldBasinBlock.class);
        registration.registerBlockComponent(BasicMachineJadeProvider.INSTANCE, BasicMachineBlock.class);
        registration.registerBlockComponent(EngineJadeProvider.INSTANCE, EngineBlock.class);
        registration.registerBlockComponent(BoilerJadeProvider.INSTANCE, BoilerTankBlock.class);
        registration.registerBlockComponent(RockTwigJadeProvider.INSTANCE, com.gregtech.gregtech.block.RockBlock.class);
        registration.registerBlockComponent(RockTwigJadeProvider.INSTANCE, com.gregtech.gregtech.block.TwigBlock.class);
    }
}
