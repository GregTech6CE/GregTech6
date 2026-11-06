package gregtech.bootstrap.content;

import com.mojang.logging.LogUtils;
import gregtech.bootstrap.InitializationStep;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
import gregtech.platform.forge.ForgeContentRegistrar;
import org.slf4j.Logger;

/**
 * Queues the shared material item stream into the Forge registries.
 * The definition loop itself lives in {@link GTItems#registerAll}.
 */
public record ItemBootstrap() implements InitializationStep {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void run() {
        ForgeContentRegistrar registrar = new ForgeContentRegistrar();
        LOGGER.info("Queued {} material items for registration", GTItems.registerAll(registrar));

        gregtech.content.registry.GTColoredBooks.register(registrar);
        GTTechnological.register(registrar);
        LOGGER.info(
                "Queued {} technological items for registration",
                GTTechnological.all().size());

        gregtech.content.registry.GTMultiItems.register(registrar);
        LOGGER.info(
                "Queued {} multi-items (tools/bottles/food/bumblebees) for registration",
                gregtech.content.registry.GTMultiItems.all().size());

        // GT6's food items are all inside the multi-item table above; this registry only holds
        // the handful that were genuinely missing (today none - see GTFoodItemsGen.GAP).
        gregtech.content.registry.GTFoodItems.register(registrar);
        LOGGER.info(
                "Queued {} GT6 food items the multi-item table was missing",
                gregtech.content.registry.GTFoodItems.registered());

        gregtech.content.registry.GTFuelRods.register(registrar);
        LOGGER.info("Queued {} fuel rod items for registration", gregtech.content.registry.GTFuelRods.ALL.size());
    }
}
