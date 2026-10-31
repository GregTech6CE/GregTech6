/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * ImplosionCompressor/FusionReactor/MatterFabricator.addToolTips plus original parent rows. */
package gregtech.client;

import gregtech.content.multiblock.OriginalAdvancedControllerData;
import gregtech.engine.machine.BasicMachineSpec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class AdvancedControllerTooltips {
    private AdvancedControllerTooltips() {}

    public static void append(BasicMachineSpec spec, List<Component> lines) {
        if (CommonBlockTooltips.containsKey(lines, "gt.lang.recipes")) return;
        lines.add(Component.translatable("gt.lang.structure")
                .withStyle(ChatFormatting.AQUA)
                .append(":"));
        for (var key : OriginalAdvancedControllerData.structureKeys(spec.machineName()))
            lines.add(gregtech.engine.fluid.FluidUnits.text(key).withStyle(ChatFormatting.WHITE));
        BasicMachineSourceTooltips.appendDefaults(
                spec,
                lines,
                spec.cheapOverclocking(),
                spec.processingEfficiency(),
                true,
                OriginalAdvancedControllerData.chargedEnergy(spec.machineName()));
    }
}
