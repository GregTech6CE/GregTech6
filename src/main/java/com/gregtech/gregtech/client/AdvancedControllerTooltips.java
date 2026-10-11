/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * ImplosionCompressor/FusionReactor/MatterFabricator.addToolTips plus original parent rows. */
package com.gregtech.gregtech.client;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.content.multiblock.OriginalAdvancedControllerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.List;
public final class AdvancedControllerTooltips {
    private AdvancedControllerTooltips() {}
    public static void append(BasicMachineSpec spec,List<Component> lines) {
        if(CommonBlockTooltips.containsKey(lines,"gt.lang.recipes"))return;
        lines.add(Component.translatable("gt.lang.structure").withStyle(ChatFormatting.AQUA).append(":"));
        for(var key:OriginalAdvancedControllerData.structureKeys(spec.machineName()))lines.add(Component.translatable(key).withStyle(ChatFormatting.WHITE));
        BasicMachineSourceTooltips.appendDefaults(spec,lines,OriginalAdvancedControllerData.cheapOverclocking(spec.machineName()),10000,true,OriginalAdvancedControllerData.chargedEnergy(spec.machineName()));
    }
}
