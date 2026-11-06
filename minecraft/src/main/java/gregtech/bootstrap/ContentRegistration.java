package gregtech.bootstrap;

import com.mojang.logging.LogUtils;
import gregtech.bootstrap.content.BlockBootstrap;
import gregtech.content.registry.ContentRegistrar;
import gregtech.content.transport.ItemPipeDefinitions;
import org.slf4j.Logger;

/**
 * The single ordering of GT6's shared content families.
 *
 * <p>Both platform entries call {@link #bootstrap(ContentRegistrar, PlatformContent)} once, in this
 * order, and submit the deferred registers they hold afterwards. Platform-only families are queued
 * through {@link PlatformContent} at the phase they belong to, so a family never has one entry point
 * per platform and the shared order never has a second copy.
 *
 * <p>The order encodes the dependencies the families have on each other: materials and prefixes are
 * ready before this runs; block items exist before block entities, menus and capabilities are built;
 * creative tabs are assembled after every item and block; recipes and addons run after the platform
 * has submitted the registries.
 */
public final class ContentRegistration {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Platform-only families, inserted between the shared families of the matching phase. */
    public interface PlatformContent {
        /** Queued after the shared world blocks, before the shared machine families. */
        default void blocks(ContentRegistrar registrar) {}

        /** Queued after the shared machine families, before addon structure parts. */
        default void machines(ContentRegistrar registrar) {}
    }

    private static final PlatformContent NO_PLATFORM_CONTENT = new PlatformContent() {};

    private ContentRegistration() {}

    public static void bootstrap(ContentRegistrar registrar) {
        bootstrap(registrar, NO_PLATFORM_CONTENT);
    }

    public static void bootstrap(ContentRegistrar registrar, PlatformContent platform) {
        registerItems(registrar);
        registerBlocks(registrar);
        platform.blocks(registrar);
        registerMachines(registrar);
        platform.machines(registrar);
        gregtech.content.registry.GTMultiblocks.registerAddonParts(registrar);
        registerCreativeTabs(registrar);
    }

    private static void registerItems(ContentRegistrar registrar) {
        // Fuel rods also queue the radiation suit items they share a material family with.
        gregtech.content.registry.GTFuelRods.register(registrar);
        gregtech.content.registry.GTColoredBooks.register(registrar);
        int materialItems = gregtech.content.registry.GTItems.registerAll(registrar);
        gregtech.content.registry.GTTechnological.register(registrar);
        gregtech.content.registry.GTMultiItems.register(registrar);
        gregtech.content.registry.GTDungeonKeys.registerAll();
        gregtech.content.registry.GTToolItems.register(registrar);
        gregtech.content.registry.GTElectricItems.register(registrar);
        LOGGER.info(
                "Queued {} material items, {} technological, {} multi-items, {} tool items",
                materialItems,
                gregtech.content.registry.GTTechnological.all().size(),
                gregtech.content.registry.GTMultiItems.all().size(),
                gregtech.content.registry.GTToolItems.all().size());
    }

    private static void registerBlocks(ContentRegistrar registrar) {
        BlockBootstrap.register();
        gregtech.content.registry.SharedDecorBlocks.initialize();
        gregtech.content.registry.GTWoods.registerAll();
        gregtech.content.registry.GTTreeHoles.registerAll();
        gregtech.content.registry.GTFluidSprings.initialize();
        gregtech.content.registry.GTConstructionBlocks.initialize();
        gregtech.content.registry.GTBars.initialize();
        gregtech.content.registry.GTSpikes.initialize();
        gregtech.content.registry.GTMaterialPiles.initialize();
        gregtech.content.registry.GTCoinPiles.initialize();
        gregtech.content.registry.GTBookShelves.initialize();
        gregtech.content.registry.GTSandwich.initialize();
        gregtech.content.registry.GTDungeonBlocks.registerAll();
        gregtech.content.registry.GTLongDistance.initialize();
        gregtech.content.registry.GTTrackBlocks.registerAll();
        gregtech.content.registry.GTLootChests.registerAll();
    }

    private static void registerMachines(ContentRegistrar registrar) {
        gregtech.content.registry.GTWires.registerAll(registrar);
        gregtech.content.registry.GTSignalWires.register(registrar);
        gregtech.content.registry.GTChemicalBatteries.registerAll(registrar);
        gregtech.content.registry.GTGearboxes.registerAll(registrar);
        gregtech.content.registry.GTGearboxes.bootstrap();
        gregtech.content.registry.GTEnergyNodes.registerAll(registrar);
        gregtech.content.registry.GTAxles.registerAll(registrar);
        gregtech.content.registry.GTAxles.bootstrap();
        gregtech.content.registry.GTEngines.registerAll();
        gregtech.content.registry.GTBoilers.registerAll();
        gregtech.content.registry.GTPumps.registerAll();
        gregtech.content.registry.GTPumps.bootstrap();
        gregtech.content.registry.GTMultiblocks.registerAll();
        gregtech.content.registry.GTLasers.registerAll();
        gregtech.content.registry.GTSensors.registerAll();
        ItemPipeDefinitions.register(registrar);
    }

    private static void registerCreativeTabs(ContentRegistrar registrar) {
        gregtech.content.registry.GTCreativeTabIcons.register(registrar);
        gregtech.content.registry.GTCreativeTabs.registerTabs();
    }
}
