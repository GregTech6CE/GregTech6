package gregtech.client;

import gregtech.content.material.Materials;
//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialTextureSet;
import net.minecraft.resources.ResourceLocation;

/**
 * Crucible fill textures (GT6 {@code MultiTileEntitySmeltery} pass 5).
 * <p>
 * Solid content: fixed {@code OP.blockRaw} gray — {@code block/material_icons/metallic/blockraw.png}
 * (GT6 {@code BlockTextureDefault.get(Materials.Invalid, OP.blockRaw, CA_GRAY_64)}), tinted like vanadium raw ore.
 * Molten content: {@link MaterialFluidVisuals} → dedicated {@code block/fluids/*} or generic {@code molten} icon.
 */
public final class CrucibleContentIcons {
    /** Fixed gray solid fill — always metallic {@code blockraw}. */
    public static final ResourceLocation SOLID_GRAY_BASE =
//? if >=1.21.1 {
            /*ResourceLocation.fromNamespaceAndPath("gt6ce","block/material_icons/metallic/blockraw");
*///?} else {
            GregTech.id("block/material_icons/metallic/blockraw");
//?}
    public static final ResourceLocation SOLID_GRAY_OVERLAY =
//? if >=1.21.1 {
            /*ResourceLocation.fromNamespaceAndPath("gt6ce","block/material_icons/metallic/blockraw_overlay");
*///?} else {
            GregTech.id("block/material_icons/metallic/blockraw_overlay");
//?}

    private CrucibleContentIcons() {}

    /** Vanadium raw ore gray ({@code Materials.Vanadium} / GT6 {@code CA_GRAY_64}-like). */
    public static int solidGrayTint() {
        return Materials.Vanadium.resolve().getColor() | 0xFF000000;
    }

    public static MaterialTextureSet resolveSet(GTMaterial material) {
        return MaterialIcons.resolveTextureSet(material.resolve());
    }

    public static MaterialFluidVisuals.Appearance moltenAppearance(GTMaterial material) {
        return MaterialFluidVisuals.forMaterial(material);
    }

    public static int surfaceTint(GTMaterial material, MaterialFluidVisuals.Appearance appearance,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (material.resolve() == Materials.Water.resolve() && level != null)
            return 0xFF000000 | net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(level, pos);
        return appearance.tintBase() ? appearance.tintArgb() : 0xFFFFFFFF;
    }

    /** A liquid phase alone is not luminous; only hot contents contribute incandescent light. */
    public static int surfaceLight(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            long temperature, int fallback) {
        int ambient = level == null ? fallback : net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos);
        int emission = (int) Math.max(0, Math.min(15, (temperature - 700) / 40));
        return net.minecraft.client.renderer.LightTexture.pack(
                Math.max(net.minecraft.client.renderer.LightTexture.block(ambient), emission),
                net.minecraft.client.renderer.LightTexture.sky(ambient));
    }

    /** @deprecated use {@link #moltenAppearance(GTMaterial)} */
    @Deprecated
    public static ResourceLocation moltenBase(GTMaterial material) {
        return moltenAppearance(material).baseTexture();
    }

    /** @deprecated use {@link #moltenAppearance(GTMaterial)} */
    @Deprecated
    public static ResourceLocation moltenOverlay(GTMaterial material) {
        return moltenAppearance(material).overlayTexture();
    }

    public static ResourceLocation blockIcon(MaterialTextureSet set, String prefix, boolean overlay) {
        String file = prefix + (overlay ? "_overlay" : "");
//? if >=1.21.1 {
        /*return ResourceLocation.fromNamespaceAndPath("gt6ce","block/material_icons/" + set.folder() + "/" + file);
*///?} else {
        return GregTech.id("block/material_icons/" + set.folder() + "/" + file);
//?}
    }
}
