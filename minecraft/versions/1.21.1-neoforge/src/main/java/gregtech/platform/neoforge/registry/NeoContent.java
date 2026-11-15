package gregtech.platform.neoforge.registry;

import gregtech.bootstrap.ContentRegistration;
import gregtech.content.registry.*;
import gregtech.content.worldgen.GTFeatures;
import gregtech.platform.neoforge.enchantment.GTEnchantmentEffectTypes;
import gregtech.platform.neoforge.fluid.FluidRegistries;
import gregtech.platform.neoforge.machine.AddonSensorRegistration;
import gregtech.platform.neoforge.machine.BasicMachineRegistries;
import gregtech.platform.neoforge.smeltery.SmelteryRegistries;
import net.neoforged.bus.api.IEventBus;

/**
 * NeoForge-only content families, queued inside the shared order, plus the one submission point for
 * the shared and addon deferred registers.
 */
public final class NeoContent implements ContentRegistration.PlatformContent {
    private final IEventBus bus;

    public NeoContent(IEventBus bus) {
        this.bus = bus;
    }

    public void bootstrap() {
        ContentRegistration.bootstrap(ContentRegistrar.shared(), this);
    }

    @Override
    public void blocks(ContentRegistrar registrar) {
        FluidRegistries.register(registrar);
        GTSpecialOreBlocks.registerAll(registrar);
        GTPlants.registerAll(registrar);
        GTBlackSands.initialize(registrar);
        GTGrassSoils.initialize(registrar);
        GTSafes.initialize(registrar);
        GTCraftingTables.initialize(registrar);
        GTStorageContainers.initialize(registrar);
        GTRelaysFilters.initialize(registrar);
        GTManualStations.initialize(registrar);
        GTRemainingDecor.initialize(registrar);
        GTMetalChests.initialize(registrar);
        GTBumbleBlocks.initialize(registrar);
        GTSurfaceBlocks.initialize(registrar);
    }

    @Override
    public void machines(ContentRegistrar registrar) {
        GTToolBlocks.registerAll(registrar);
        AddonSensorRegistration.register(registrar);
        GTMassStorages.initialize(registrar);
        GTLogisticsWire.register(registrar);
        GTFluidPipes.registerAll(registrar);
        GTTanks.registerAll(registrar);
        GTBasicMachines.registerAll(registrar);
        SmelteryRegistries.register(registrar);
        GTHoppers.register(registrar);
        // Last: fills the icon ids no earlier family claimed, so it must follow every block family.
        GTRemainingIconBlocks.initialize(registrar);
    }

    /** Attaches every shared, addon and platform content register exactly once. */
    public void submit() {
        FluidRegistries.submit(bus);
        GTSounds.REGISTRY.register(bus);
        BasicMachineRegistries.submit(bus);
        SmelteryRegistries.submit(bus);
        GTEnchantmentEffectTypes.submit(bus);
        GTItems.ITEMS.register(bus);
        GTItems.registerAddonItems(bus);
        GTBlocks.BLOCKS.register(bus);
        GTBlocks.BLOCK_ITEMS.register(bus);
        GTBlocks.registerAddonBlocks(bus);
        GTBlockEntities.register(bus);
        GTMenuTypes.registerAll();
        GTMenuTypes.submit(bus);
        GTToolBlocks.submit(bus);
        GTEnergyNodes.submit(bus);
        GTItemPipes.submit(bus);
        GTRadiationProtection.MATERIALS.register(bus);
        GTCreativeTabs.TABS.register(bus);
        GTProjectiles.register(bus);
        GTFeatures.register(bus);
        gregtech.content.recipe.NeoRecipeSerializers.register(bus);
    }
}
