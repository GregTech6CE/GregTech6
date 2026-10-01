package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.api.material.MaterialPresentation;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.data.RegisteredFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidType;

/** Display names and tooltip descriptions for GT6 {@link FL} fluids. */
public final class GTFluidType {
    private GTFluidType() {}

    public static Component describe(RegisteredFluids.FluidEntry entry, String langPath) {
        GTMaterial material = resolveMaterial(entry);
        if (material.isValid()) {
            // generated gas./liquid. fluids reuse the molten TEXTURE mode, so
            // only the registry name decides the "Molten" naming
            if (entry.registryName().startsWith("molten.")) {
                return Component.translatable("fluid_type.gregtech.molten_material", MaterialPresentation.name(material));
            }
            return Component.literal(material.getDisplayNameFallback());
        }
        return Component.translatable("fluid_type." + GregTech.MODID + "." + langPath);
    }

    /** Full tooltip lines matching GT6 original format. */
    public static java.util.List<Component> describeTooltip(RegisteredFluids.FluidEntry entry) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        GTMaterial material = resolveMaterial(entry);
        boolean isMolten = entry.registryName().startsWith("molten.");
        boolean isGas = !isMolten
                && (entry.gas() || (material.isValid() && material.has(MaterialProperty.GAS)));
        boolean isFlammable = material.isValid() && material.has(MaterialProperty.FLAMMABLE);
        boolean isMagical = material.isValid() && material.has(MaterialProperty.MAGICAL);
        boolean isMagnetic = material.isValid() && material.has(MaterialProperty.MAGNETIC);
        boolean isAntimatter = material.isValid() && material.has(MaterialProperty.ANTIMATTER);
        boolean isUnburnable = material.isValid() && material.has(MaterialProperty.UNBURNABLE);
        boolean isMetal = material.isValid() && material.has(MaterialProperty.METAL);
        boolean isAlloy = material.isValid() && material.has(MaterialProperty.ALLOY);
        boolean isElement = material.isValid() && material.has(MaterialProperty.ELEMENT);

        // Chemical formula (Gold)
        if (material.isValid()) {
            String formula = material.getResolvedTooltipChemical();
            if (formula != null && !formula.isEmpty()) {
                lines.add(Component.literal(formula).withStyle(ChatFormatting.YELLOW));
            }
        }

        // Temperature (Red)
        int tempK;
        if (isMolten) {
            tempK = material.getMeltingPoint();
        } else if (isGas) {
            tempK = material.getBoilingPoint() > 0 && material.getBoilingPoint() < 100000
                    ? material.getBoilingPoint() : 298;
        } else {
            tempK = material.isValid() && material.getMeltingPoint() > 0
                    ? material.getMeltingPoint() : 298;
        }
        int tempC = tempK - 273;
        lines.add(Component.literal("Temperature: " + formatTempFull(tempK, tempC))
                .withStyle(ChatFormatting.RED));

        // State (Green label, colored state text, Aqua hint)
        String stateText;
        ChatFormatting stateColor;
        String hint = null;
        if (isMolten) {
            stateText = "Liquid";
            stateColor = ChatFormatting.BLUE;
            hint = "Might able to cast into Molds";
        } else if (isGas) {
            stateText = "Gas";
            stateColor = ChatFormatting.AQUA;
        } else if (material.isValid() && material.getBoilingPoint() <= GregTechConstants.DEF_ENV_TEMP && material.getBoilingPoint() > 0) {
            stateText = "Gas (at room temp)";
            stateColor = ChatFormatting.AQUA;
        } else {
            stateText = "Liquid";
            stateColor = ChatFormatting.BLUE;
        }

        Component stateLine;
        if (hint != null) {
            stateLine = Component.literal("State: ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(stateText).withStyle(stateColor))
                    .append(Component.literal(" (" + hint + ")").withStyle(ChatFormatting.AQUA));
        } else {
            stateLine = Component.literal("State: ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(stateText).withStyle(stateColor));
        }
        lines.add(stateLine);

        // Density + Buoyancy (Green)
        float density = material.isValid() ? material.getDensity() : 1.0f;
        String densityStr;
        if (density == 0f) {
            densityStr = "0";
        } else if (density < 0) {
            densityStr = String.valueOf((int) density);
        } else if (density < 1f) {
            densityStr = String.format("%.2f", density);
        } else {
            densityStr = String.valueOf((int) (density * 1000f));
        }
        String buoyancy;
        if (density < 0) {
            buoyancy = "Lighter than Air (typically moves up)";
        } else if (density == 0) {
            buoyancy = "Very light (rises in air)";
        } else if (density < 0.001f) {
            buoyancy = "Lighter than Air (typically moves up)";
        } else if (density < 1f) {
            buoyancy = "Lighter than Water";
        } else {
            buoyancy = "Heavier than Air (typically moves down)";
        }
        lines.add(Component.literal("Density: " + densityStr + " ; " + buoyancy)
                .withStyle(ChatFormatting.GREEN));

        // Luminosity (Yellow)
        if (material.isValid()) {
            int color = material.getColor();
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int brightness = (r + g + b) / 3;
            if (brightness > 100) {
                lines.add(Component.literal("Luminosity: " + brightness)
                        .withStyle(ChatFormatting.YELLOW));
            }
        }

        // Viscosity (Blue)
        int viscosity;
        if (isMolten) {
            viscosity = 6000;
        } else if (isGas) {
            viscosity = 200;
        } else if (material.isValid()) {
            viscosity = (int) (density * 1000f);
            if (viscosity < 200) viscosity = 200;
            if (viscosity > 10000) viscosity = 10000;
        } else {
            viscosity = 1000;
        }
        lines.add(Component.literal("Viscosity: " + viscosity)
                .withStyle(ChatFormatting.BLUE));

        // Material property hints (Gold/Orange for special, Green for simple)
        boolean hasSpecialProps = false;
        if (isMagical) {
            lines.add(Component.literal("Magical! Handle with Care!").withStyle(ChatFormatting.GOLD));
            hasSpecialProps = true;
        }
        if (isAntimatter) {
            lines.add(Component.literal("Antimatter! Handle with extreme Care!").withStyle(ChatFormatting.GOLD));
            hasSpecialProps = true;
        }
        if (isMagnetic) {
            lines.add(Component.literal("Magnetic! Keep away from Electronic Devices!").withStyle(ChatFormatting.GOLD));
            hasSpecialProps = true;
        }
        if (isUnburnable) {
            lines.add(Component.literal("Fireproof!").withStyle(ChatFormatting.GOLD));
            hasSpecialProps = true;
        }

        if (!hasSpecialProps && material.isValid()) {
            lines.add(Component.literal("This is a simple Fluid that is easy to handle")
                    .withStyle(ChatFormatting.GREEN));
        }

        // Fuel values
        if (material.isValid() && !isUnburnable) {
            long burnTime = material.getFurnaceBurnTime();
            if (burnTime > 0) {
                long guPerUnit = burnTime * 2 / 5;
                if (guPerUnit < 1) guPerUnit = 1;
                lines.add(Component.literal("Burnable Fuels: ").withStyle(ChatFormatting.RED)
                        .append(Component.literal(String.valueOf(guPerUnit)).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" GU/L").withStyle(ChatFormatting.YELLOW)));
            }

            if (isGas && isFlammable) {
                long gasValue = burnTime * 3 / 5;
                if (gasValue < 1) gasValue = 1;
                lines.add(Component.literal("Gas Fuels: ").withStyle(ChatFormatting.RED)
                        .append(Component.literal(String.valueOf(gasValue)).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" GU/L").withStyle(ChatFormatting.YELLOW)));
            }
        }

        // Footer
        lines.add(Component.literal("Fluid owned by GT6").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Rightclick Blocks to fill their Tanks with this Fluid!")
                .withStyle(ChatFormatting.AQUA));

        return lines;
    }

    private static String formatTempFull(int kelvin, int celsius) {
        if (kelvin <= 0) return "N/A";
        return String.format("%,d K (%d °C)", kelvin, celsius);
    }

    private static GTMaterial resolveMaterial(RegisteredFluids.FluidEntry entry) {
        if (entry.materialKey() != null) {
            return GTMaterialRegistry.get(entry.materialKey()).resolve();
        }
        if (entry.registryName().startsWith("molten.")) {
            return GTMaterialRegistry.get(entry.registryName().substring("molten.".length())).resolve();
        }
        return GTMaterialRegistry.get("NULL");
    }
}
