package com.gregtech.gregtech.integration.client;
import mezz.jei.api.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
@JeiPlugin
public final class RecipePresentationJeiSmoke implements IModPlugin {
    static IJeiRuntime runtime;
    @Override public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath("gregtech","recipe_presentation_smoke");}
    @Override public void onRuntimeAvailable(IJeiRuntime value){runtime=value;}
}
