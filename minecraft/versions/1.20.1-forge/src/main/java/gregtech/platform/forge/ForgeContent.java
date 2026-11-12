package gregtech.platform.forge;

import gregtech.bootstrap.ContentRegistration;
import gregtech.bootstrap.content.FluidBootstrap;
import gregtech.bootstrap.content.ToolBootstrap;
import gregtech.content.registry.*;
import gregtech.content.worldgen.GTFeatures;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Forge-only content families, queued inside the shared order, plus the one submission point for the
 * shared and addon deferred registers.
 */
public final class ForgeContent implements ContentRegistration.PlatformContent {
    private final IEventBus bus;

    public ForgeContent(IEventBus bus) {
        this.bus = bus;
    }

    public void bootstrap() {
        ContentRegistration.bootstrap(ContentRegistrar.shared(), this);
    }

    @Override
    public void blocks(ContentRegistrar registrar) {
        new FluidBootstrap().run();
        GTIconSetBlocks.registerAll(registrar);
        GTBushes.registerAll(registrar);
        GTSurfaceBlocks.initializeDiggables(registrar);
        GTSafes.initialize(registrar);
        GTCraftingTables.initialize(registrar);
        GTStorageContainers.initialize(registrar);
        GTRelaysFilters.initialize(registrar);
        GTMetalChests.initialize(registrar);
        GTBumbleBlocks.initialize(registrar);
        GTManualStations.initialize(registrar);
        GTDecorBlocks.registerAll();
        GTMiscBlocks.registerAll(registrar);
    }

    @Override
    public void machines(ContentRegistrar registrar) {
        GTFluidPipes.registerAll(registrar);
        GTTanks.registerAll(registrar);
        gregtech.content.registry.GTBurningBoxes.registerAll(registrar);
        gregtech.content.registry.GTSmeltery.register(registrar);
        gregtech.content.registry.GTHoppers.register(registrar);
        GTLogisticsWire.register(registrar);
        AddonSensorRegistration.register(registrar);
        GTBasicMachines.registerAll(registrar);
        GTMassStorages.initialize(registrar);
        GTToolBlocks.registerAll(registrar);
    }

    /** Attaches every shared, addon and platform content register exactly once. */
    public void submit() {
        GTFluids.FLUID_TYPES.register(bus);
        GTFluids.FLUIDS.register(bus);
        GTFluids.registerAddonFluids(bus);
        ToolBootstrap.submit(bus);
        GTItems.ITEMS.register(bus);
        GTItems.registerAddonItems(bus);
        GTBlocks.BLOCKS.register(bus);
        GTBlocks.BLOCK_ITEMS.register(bus);
        GTBlocks.registerAddonBlocks(bus);
        GTBlockEntities.register(bus);
        GTCreativeTabs.TABS.register(bus);
        GTSounds.REGISTRY.register(bus);
        GTMenuTypes.registerAll();
        GTMenuTypes.submit(bus);
        GTProjectiles.register(bus);
        GTFeatures.register(bus);
        GTEnchantments.register(bus);
    }
}
