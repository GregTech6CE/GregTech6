package gregtech.platform.forge.transport;

import gregtech.engine.fluid.PotionBottleHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class PotionBottleCapabilities {
    private PotionBottleCapabilities() {}

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<ItemStack> event) {
        var stack = event.getObject();
        if (!PotionBottleHandler.acceptsItem(stack.getItem())) return;
        LazyOptional<IFluidHandlerItem> handler = LazyOptional.of(() -> new PotionBottleHandler(stack));
        event.addCapability(ResourceLocation.fromNamespaceAndPath("gt6ce", "potion_bottle"), new ICapabilityProvider() {
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.FLUID_HANDLER_ITEM ? handler.cast() : LazyOptional.empty();
            }
        });
        event.addListener(handler::invalidate);
    }
}
