package gregtech.platform.forge;

import gregtech.bootstrap.ContentRegistration;
import gregtech.bootstrap.content.FluidBootstrap;
import gregtech.bootstrap.content.ToolBootstrap;
import gregtech.content.registry.AddonSensorRegistration;
import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.GTBasicMachines;
import gregtech.content.registry.GTBlockEntities;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTBumbleBlocks;
import gregtech.content.registry.GTBushes;
import gregtech.content.registry.GTCreativeTabs;
import gregtech.content.registry.GTDecorBlocks;
import gregtech.content.registry.GTEnchantments;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTIconSetBlocks;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTMenuTypes;
import gregtech.content.registry.GTMiscBlocks;
import gregtech.content.registry.GTProjectiles;
import gregtech.content.registry.GTSounds;
import gregtech.content.registry.GTStorage;
import gregtech.content.registry.GTSurfaceBlocks;
import gregtech.content.registry.GTToolBlocks;
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
        GTBumbleBlocks.initializeHives(registrar);
        GTDecorBlocks.registerAll();
        GTMiscBlocks.registerAll(registrar);
    }

    @Override
    public void machines(ContentRegistrar registrar) {
        gregtech.content.fluid.FluidPipeDefinitions.register(registrar);
        gregtech.content.fluid.TankDefinitions.register(registrar);
        gregtech.content.registry.GTBurningBoxes.registerAll(registrar);
        gregtech.content.registry.GTSmeltery.register(registrar);
        gregtech.content.registry.GTHoppers.register(registrar);
        AddonSensorRegistration.register(registrar);
        GTBasicMachines.registerAll(registrar);
        GTStorage.registerAll(registrar);
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
        GTMenuTypes.MENU_TYPES.register(bus);
        GTProjectiles.register(bus);
        GTFeatures.register(bus);
        GTEnchantments.register(bus);
    }
}
