package gregtech.bootstrap.registration;

import gregtech.bootstrap.InitializationStep;
import gregtech.registry.AddonPipeRegistration;
import gregtech.registry.GTBlocks;
import gregtech.registry.GTItems;
import gregtech.registry.GTMultiblocks;
import gregtech.registry.GTMenuTypes;
import gregtech.registry.GTSounds;
import gregtech.registry.GTTechnological;
import net.minecraftforge.eventbus.api.IEventBus;

/** Final Phase B step — submits all shared DeferredRegisters after every bootstrapper has queued. */
public record DeferredRegistrySubmission(IEventBus bus) implements InitializationStep {
    @Override
    public void run() {
        GTItems.ITEMS.register(bus);
        GTItems.registerAddonItems(bus);
        GTTechnological.ITEMS.register(bus);
        gregtech.registry.GTMultiItems.register(bus);
        // §108: the few GT6 food items the port was actually missing (today: none - the 266 food
        // items were all registered already, only their numbers were missing) submit here.
        gregtech.registry.GTFoodItems.register(bus);
        GTBlocks.BLOCKS.register(bus);
        GTBlocks.BLOCK_ITEMS.register(bus);
        GTMultiblocks.registerAddonParts();
        GTMultiblocks.registerAddonBlocks(bus);
        AddonPipeRegistration.submit(bus);
        // GTIconSetBlocks uses GTBlocks.BLOCKS / GTBlocks.BLOCK_ITEMS directly
        GTSounds.REGISTRY.register(bus);
        GTMenuTypes.MENU_TYPES.register(bus);
    }
}
