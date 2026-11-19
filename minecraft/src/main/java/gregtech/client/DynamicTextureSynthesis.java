package gregtech.client;

import com.mojang.blaze3d.platform.NativeImage;
import gregtech.engine.definition.TexturePaletteTable;
import gregtech.engine.mod.GregTechIdentity;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;

/**
 * Lazy runtime sprite synthesizer backed by {@link TexturePaletteTable}.
 *
 * <p>Reads source templates directly from the mod classpath rather than the active {@code
 * ResourceManager} so user resource packs overriding a template sprite never alter derived sprites,
 * while user overrides of any derived path still shadow this {@code Pack.Position.BOTTOM} pack.
 */
final class DynamicTextureSynthesis {
    private DynamicTextureSynthesis() {}

    static boolean containsAssetPath(String assetPath) {
        return TexturePaletteTable.load().containsAssetPath(assetPath);
    }

    static InputStream open(String assetPath) throws IOException {
        TexturePaletteTable table = TexturePaletteTable.load();
        byte[] mcmeta = table.mcmetaBytes(assetPath);
        if (mcmeta != null) {
            return new ByteArrayInputStream(mcmeta);
        }
        TexturePaletteTable.Entry entry = table.entry(assetPath);
        if (entry == null) {
            throw new IOException("Unknown dynamic texture: " + assetPath);
        }
        String templateResource = "/assets/" + GregTechIdentity.MOD_ID + "/" + entry.templatePath();
        try (InputStream templateStream = DynamicTextureSynthesis.class.getResourceAsStream(templateResource)) {
            if (templateStream == null) {
                throw new IOException("Missing template texture on classpath: " + templateResource);
            }
            if (entry.ruleCount() == 0) {
                return new ByteArrayInputStream(templateStream.readAllBytes());
            }
            // NativeImage.read(stream) expands RGB-only templates (such as steam.png) to Format.RGBA.
            try (NativeImage source = NativeImage.read(templateStream);
                    NativeImage recolored = source.mappedCopy(entry::mapAbgr)) {
                return new ByteArrayInputStream(recolored.asByteArray());
            }
        }
    }

    static void listResources(String directory, PackResources.ResourceOutput output) {
        String prefix = directory.isEmpty() ? "" : directory + "/";
        TexturePaletteTable table = TexturePaletteTable.load();
        for (var entry : table.entries()) {
            String path = entry.assetPath();
            if (path.startsWith(prefix)) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GregTechIdentity.MOD_ID, path);
                output.accept(id, () -> open(path));
            }
        }
        for (String mcmetaPath : table.mcmetaByPath().keySet()) {
            if (mcmetaPath.startsWith(prefix)) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GregTechIdentity.MOD_ID, mcmetaPath);
                output.accept(id, () -> open(mcmetaPath));
            }
        }
    }
}
