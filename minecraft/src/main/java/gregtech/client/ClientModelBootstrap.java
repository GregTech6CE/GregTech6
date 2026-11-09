package gregtech.client;

import gregtech.engine.mod.GregTechIdentity;
import com.mojang.logging.LogUtils;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}
import org.slf4j.Logger;

/**
 * Single registration point for every client model event.
 *
 * <p>The model classes keep their static handlers; this class owns the only
 * {@code @SubscribeEvent} declarations and the only ordering. Block-item transforms are captured
 * first, every NORMAL map mutator follows, then the LOW portable-fluid pass, and finally the
 * LOWEST alpha pass that also rebuilds the tool resolver. Nothing here may be wrapped in
 * {@code SetupOnce}: F3+T and resource-pack reloads repeat the whole bake.</p>
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class ClientModelBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientModelBootstrap() {}

    @SubscribeEvent
    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        CrucibleClientModels.registerSharedModels(event);
        FluidItemClientModels.registerSharedModels(event);
        GTToolModel.registerIconsetModels(event);
        MachineBlockClientModels.registerSharedModels(event);
        MaterialBlockClientModels.registerSharedModels(event);
        MaterialClientModels.registerSharedModels(event);
        OreClientModels.registerTemplate(event);
        RawOreRockModels.register(event);
        SmelteryClientModels.registerSharedModels(event);
        StoneBlockClientModels.registerSharedModels(event);
        registerPlatformAdditional(event);
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        SharedInventoryModelPack.loaders(event);
    }

    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        // Every bake owns new transforms; capture once before any handler reads them.
        BlockItemModelHelper.captureFromVanillaBlockItem(BakedModelMap.of(event.getModels()));
        MachineBlockClientModels.onModifyBakingResult(event);
        MaterialBlockClientModels.onModifyBakingResult(event);
        MaterialClientModels.onModifyBakingResult(event);
        OreClientModels.onModifyBakingResult(event);
        StoneBlockClientModels.onModifyBakingResult(event);
        SmelteryClientModels.onModifyBakingResult(event);
        CrucibleClientModels.onModifyBakingResult(event);
        CreativeTabIconClientModels.onModifyBakingResult(event);
        GTToolModel.onModifyBakingResult(event);
        FluidItemClientModels.onModifyBakingResult(event);
        FluidSpringClient.models(event);
        RawOreRockModels.bake(event);
        modifyPlatformBakingResult(event);
        LOGGER.info("[{}] Client model bootstrap: NORMAL baking pass", GregTechIdentity.REGISTRY_NAMESPACE);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void modifyBakingResultLow(ModelEvent.ModifyBakingResult event) {
        PortableFluidItemModels.models(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void modifyBakingResultLowest(ModelEvent.ModifyBakingResult event) {
        ConfiguredAlphaModels.models(event);
    }

//? if >=1.21.1 {
    /*private static void registerPlatformAdditional(ModelEvent.RegisterAdditional event) {
        ItemLogisticsClientSetup.additional(event);
        PartDesignClientModels.register(event);
    }

    private static void modifyPlatformBakingResult(ModelEvent.ModifyBakingResult event) {
        AxleClientSetup.models(event);
        FluidTransportClientSetup.models(event);
        GearboxClientSetup.models(event);
        ItemLogisticsClientSetup.models(event);
        PartDesignClientModels.bake(event);
        SignalWireClientSetup.models(event);
        ElectricWireClientSetup.models(event);
    }
*///?} else {
    private static void registerPlatformAdditional(ModelEvent.RegisterAdditional event) {
        HopperClientModels.registerSharedModels(event);
        PartDesignClientModels.register(event);
    }

    private static void modifyPlatformBakingResult(ModelEvent.ModifyBakingResult event) {
        GearboxClientModels.onModifyBakingResult(event);
        HopperClientModels.onModifyBakingResult(event);
        PartDesignClientModels.bake(event);
        PipeWireClientModels.onModifyBakingResult(event);
    }
//?}
}
