package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.data.RegisteredFluids;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * GT6-style material → fluid appearance for crucible / tank rendering.
 * <p>
 * Dedicated textures live in {@code block/fluids/{registryName}.png}; molten metals without a
 * dedicated file fall back to {@code block/material_icons/{set}/molten.png} tinted by material color
 * (GT6 {@code getTextureMolten()}).
 */
public final class MaterialFluidVisuals {
    /** Crucible / BER appearance for one material state. */
    public record Appearance(
            ResourceLocation baseTexture,
            @Nullable ResourceLocation overlayTexture,
            int tintArgb,
            boolean tintBase
    ) {}

    private static final Map<String, RegisteredFluids.FluidEntry> BY_REGISTRY = new HashMap<>();
    private static final Map<String, RegisteredFluids.FluidEntry> BY_MATERIAL = new HashMap<>();
    private static boolean indexed;

    private MaterialFluidVisuals() {}

    public static void bootstrap() {
        if (indexed) {
            return;
        }
        indexed = true;
        RegisteredFluids.bootstrap();
        for (RegisteredFluids.FluidEntry entry : RegisteredFluids.all().values()) {
            BY_REGISTRY.put(entry.registryName(), entry);
            if (entry.materialKey() != null) {
                BY_MATERIAL.put(GTMaterial.sanitize(entry.materialKey()), entry);
            }
            if (entry.registryName().startsWith("molten.")) {
                String suffix = entry.registryName().substring("molten.".length());
                BY_MATERIAL.putIfAbsent(GTMaterial.sanitize(suffix), entry);
            }
        }
    }

    public static Appearance forMaterial(GTMaterial material) {
        bootstrap();
        GTMaterial resolved = material.resolve();
        if (!resolved.isValid()) {
            return genericMolten(resolved);
        }

        RegisteredFluids.FluidEntry entry = BY_MATERIAL.get(resolved.getName());
        if (entry == null) {
            entry = BY_REGISTRY.get(defaultMoltenRegistry(resolved));
        }
        if (entry != null) {
            return fromFluidEntry(entry, resolved);
        }
        if (resolved.has(MaterialProperty.MOLTEN)
                || resolved.has(MaterialProperty.METAL)
                || resolved.has(MaterialProperty.ALLOY)) {
            return genericMolten(resolved);
        }
        return genericMolten(resolved);
    }

    @Nullable
    public static RegisteredFluids.FluidEntry fluidEntry(GTMaterial material) {
        bootstrap();
        GTMaterial resolved = material.resolve();
        RegisteredFluids.FluidEntry entry = BY_MATERIAL.get(resolved.getName());
        if (entry != null) {
            return entry;
        }
        return BY_REGISTRY.get(defaultMoltenRegistry(resolved));
    }

    private static Appearance fromFluidEntry(RegisteredFluids.FluidEntry entry, GTMaterial material) {
        var visual=FluidAppearance.appearance(entry);
        return new Appearance(visual.texture(),null,visual.tint(),true);
    }

    private static Appearance genericMolten(GTMaterial material) {
        GTMaterial resolved = material.resolve();
        MaterialTextureSet set = MaterialIcons.resolveTextureSet(resolved);
        int color = com.gregtech.gregtech.api.fluid.FluidVisualPolicy.moltenTint(resolved);
        return new Appearance(
                CrucibleContentIcons.blockIcon(set, "molten", false),
                CrucibleContentIcons.blockIcon(set, "molten", true),
                color,
                true);
    }

    private static String defaultMoltenRegistry(GTMaterial material) {
        return "molten." + material.getName().toLowerCase(Locale.ROOT);
    }

    /** Resolve material linked to a FL field name (e.g. {@code "Iron"} → {@code Materials.Iron}). */
    public static void linkMaterial(String flField, String materialName) {
        bootstrap();
        RegisteredFluids.FluidEntry entry = RegisteredFluids.all().get(flField);
        if (entry != null) {
            BY_MATERIAL.put(GTMaterial.sanitize(materialName), entry);
        }
    }

    /** Auto-link molten fluids to registered materials after materials load. */
    public static void linkAllMoltenMaterials() {
        bootstrap();
        GTMaterialRegistry.init();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid() || material.getId() <= 0) {
                continue;
            }
            String registryName = defaultMoltenRegistry(material);
            RegisteredFluids.FluidEntry entry = BY_REGISTRY.get(registryName);
            if (entry != null) {
                BY_MATERIAL.putIfAbsent(material.getName(), entry);
            }
        }
    }
}
