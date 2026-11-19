package gregtech.engine.tool;

import gregtech.api.material.GTMaterial;
import gregtech.content.tool.WoodenBucketCatalog;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.mod.GregTechIdentity;

/** Layered item model and tint rules for wooden bucket variants. */
public final class WoodenBucketVisuals {
    public static final String BASE_TEXTURE = GregTechIdentity.MOD_ID + ":item/wooden_buckets/wooden_bucket_base";
    public static final String BAND_OVERLAY_TEXTURE =
            GregTechIdentity.MOD_ID + ":item/wooden_buckets/wooden_bucket_band_overlay";
    public static final String LIQUID_OVERLAY_TEXTURE =
            GregTechIdentity.MOD_ID + ":item/wooden_buckets/wooden_bucket_liquid_overlay";
    public static final String RAINBOW_SAP_OVERLAY_TEXTURE =
            GregTechIdentity.MOD_ID + ":item/wooden_buckets/wooden_bucket_liquid_rainbow_sap";

    private WoodenBucketVisuals() {}

    /** Builds the layered {@code minecraft:item/generated} model JSON for {@code content}. */
    public static String modelJson(WoodenBucketCatalog.Content content) {
        if (content.empty()) {
            return "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\""
                    + BASE_TEXTURE
                    + "\",\"layer1\":\""
                    + BAND_OVERLAY_TEXTURE
                    + "\"}}";
        }
        String liquidLayer = "rainbow_sap".equals(content.id()) ? RAINBOW_SAP_OVERLAY_TEXTURE : LIQUID_OVERLAY_TEXTURE;
        return "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\""
                + BASE_TEXTURE
                + "\",\"layer1\":\""
                + BAND_OVERLAY_TEXTURE
                + "\",\"layer2\":\""
                + liquidLayer
                + "\"}}";
    }

    /** Returns the registered {@link GTMaterial} color for the bucket's metal band. */
    public static int bandRgb(WoodenBucketCatalog.Band band) {
        GTMaterial material = GTMaterialRegistry.get(band.material()).resolve();
        return material.isValid() ? material.getColor() & 0xFFFFFF : 0xFFFFFF;
    }

    /**
     * Representative flat RGB tint per bucket liquid, stored on {@link WoodenBucketCatalog.Content}.
     */
    public static int liquidRgb(WoodenBucketCatalog.Content content) {
        return content.liquidRgb();
    }

    /**
     * Layer 0 stays untinted so single-layer resource pack overrides keep their original colours.
     */
    public static int tintRgb(WoodenBucketCatalog.Band band, WoodenBucketCatalog.Content content, int layer) {
        return switch (layer) {
            case 1 -> bandRgb(band);
            case 2 -> liquidRgb(content);
            default -> 0xFFFFFF;
        };
    }
}
