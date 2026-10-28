package gregtech.bootstrap.registration;

import gregtech.bootstrap.InitializationStep;
import gregtech.content.registry.AddonPipeRegistration;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTMenuTypes;
import gregtech.content.registry.GTMultiblocks;
import gregtech.content.registry.GTSounds;
import gregtech.content.registry.GTTechnological;
import net.minecraftforge.eventbus.api.IEventBus;

/** Final Phase B step — submits all shared DeferredRegisters after every bootstrapper has queued. */
public record DeferredRegistrySubmission(IEventBus bus) implements InitializationStep {
    @Override
    public void run() {
        GTItems.ITEMS.register(bus);
        GTItems.registerAddonItems(bus);
        GTTechnological.ITEMS.register(bus);
        gregtech.content.registry.GTMultiItems.register(bus);
        // §108: the few GT6 food items the port was actually missing (today: none - the 266 food
        // items were all registered already, only their numbers were missing) submit here.
        gregtech.content.registry.GTFoodItems.register(bus);
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
