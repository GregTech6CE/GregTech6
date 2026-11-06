package gregtech.platform.forge;

import gregtech.bootstrap.ContentRegistration;
import gregtech.bootstrap.content.FluidBootstrap;
import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.GTBasicMachines;
import gregtech.content.registry.GTBlockEntities;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTBumbleBlocks;
import gregtech.content.registry.GTBushes;
import gregtech.content.registry.GTCreativeTabs;
import gregtech.content.registry.GTDecorBlocks;
import gregtech.content.registry.GTEnchantments;
import gregtech.content.registry.GTIconSetBlocks;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTMachines;
import gregtech.content.registry.GTMenuTypes;
import gregtech.content.registry.GTMiscBlocks;
import gregtech.content.registry.GTProjectiles;
import gregtech.content.registry.GTSounds;
import gregtech.content.registry.GTStorage;
import gregtech.content.registry.GTSurfaceBlocks;
import gregtech.content.registry.GTToolBlocks;
import gregtech.content.worldgen.GTFeatures;
import gregtech.engine.machine.MachineRegistry;
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
        ContentRegistration.bootstrap(new ForgeContentRegistrar(), this);
    }

    @Override
    public void blocks(ContentRegistrar registrar) {
        new FluidBootstrap(bus).run();
        GTIconSetBlocks.registerAll();
        GTBushes.registerAll();
        GTSurfaceBlocks.initializeDiggables();
        GTBumbleBlocks.initializeHives();
        GTDecorBlocks.registerAll();
        GTMiscBlocks.registerAll();
    }

    @Override
    public void machines(ContentRegistrar registrar) {
        gregtech.content.fluid.FluidPipeDefinitions.register(registrar);
        gregtech.content.fluid.TankDefinitions.register();
        GTMachines.bootstrap();
        gregtech.content.registry.AddonSensorRegistration.register(bus);
        GTBasicMachines.registerAll(bus);
        GTStorage.registerAll();
        GTToolBlocks.registerAll(registrar);
        MachineRegistry.registerBasicMachineBeTypes(GTBlockEntities.BLOCK_ENTITY_TYPES);
        MachineRegistry.registerEngineBeTypes(GTBlockEntities.BLOCK_ENTITY_TYPES);
    }

    /** Attaches every shared, addon and platform content register exactly once. */
    public void submit() {
        GTItems.ITEMS.register(bus);
        GTItems.registerAddonItems(bus);
        GTBlocks.BLOCKS.register(bus);
        GTBlocks.BLOCK_ITEMS.register(bus);
        GTBlocks.registerAddonBlocks(bus);
        GTBlockEntities.BLOCK_ENTITY_TYPES.register(bus);
        GTCreativeTabs.TABS.register(bus);
        GTSounds.REGISTRY.register(bus);
        GTMenuTypes.registerAll();
        GTMenuTypes.MENU_TYPES.register(bus);
        GTProjectiles.register(bus);
        GTFeatures.register(bus);
        GTEnchantments.register(bus);
    }
}
