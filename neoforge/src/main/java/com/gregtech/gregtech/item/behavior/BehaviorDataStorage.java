package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.recipe.GTMaterialDataRecipes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * GT6 {@code gregtech.items.behaviors.Behavior_DataStorage} — the tooltip of a USB <em>stick</em>, the
 * one-file medium, plus the shared data renderer both that class and the drive form call.
 *
 * <p>GT6 hangs the behaviour on {@code MultiItemRandomTools:422-438} / {@code MultiItemTechnological}
 * and prints ({@code Behavior_DataStorage:37-48}):</p>
 *
 * <ul>
 *   <li>the data compound rendered by {@code UT.NBT.getDataToolTip(data, list, <b>true</b>)}
 *       ({@code UT.java:2237-2269}) — the verbose form, because a stick holds exactly one file; and</li>
 *   <li>{@code "Data: USB <tier>.0"} in dark grey ({@code :42}).</li>
 * </ul>
 *
 * <p><b>One dead branch, kept as a note:</b> GT6's {@code else} — {@code "This Stick is Empty"} — is
 * unreachable, because 1.7.10's {@code getCompoundTag} returns an empty compound rather than
 * {@code null}, so the {@code tUSB != null} test never fails. The port therefore prints nothing for a
 * stick that was never written to, which is what GT6 does; the string is still provided as a lang key
 * so a future reader does not have to re-derive why it is missing.</p>
 */
public final class BehaviorDataStorage {

    private BehaviorDataStorage() {}

    /**
     * The stick's tooltip lines — GT6 {@code Behavior_DataStorage:37-48}.
     *
     * @param stack the stick; a stack without any tag prints nothing, exactly like the original
     */
    public static void tooltip(ItemStack stack, List<Component> lines) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = com.gregtech.gregtech.content.data.UsbDataMedia.tag(stack);
        if (tag == null) return;                              // GT6: `if (aStack.hasTagCompound())`
        CompoundTag data = GTMaterialDataRecipes.usbData(stack);
        if (data != null) dataTooltip(data, lines, true);     // :41 — the verbose form
        lines.add(Component.translatable("gt.tooltip.usb.tier",
                String.valueOf(tag.getByte(GTMaterialDataRecipes.NBT_USB_TIER)))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * GT6 {@code UT.NBT.getDataToolTip}: image and document details share this entry point with
     * material replication data; sticks are verbose and HDD file rows are compact.
     *
     * <p>{@code allDetails} is GT6's own switch: a stick passes {@code true} and gets the material plus
     * what replicating it takes, a drive slot passes {@code false} and gets the one-line form
     * ({@code Behavior_DataStorage16:49}). A material the replicator refuses keeps GT6's
     * {@code "(Not Replicatable)"} line ({@code UT.java:2266}) — in the port that is every antimatter
     * material, since {@code replicationRecipe} refuses those.</p>
     *
     * <p><b>Energy figure:</b> GT6 prints {@code (neutrons + protons) * 65536} QU ({@code UT.java:2261})
     * while the port's replicator charges {@code nucleons * REPLICATOR_EU_PER_NUCLEON} — the tooltip
     * prints the port's own number, so what a player reads is what the machine spends (the GT6 figure is
     * the matter/antimatter requirement of a machine the port does not have).</p>
     */
    public static void dataTooltip(CompoundTag data, List<Component> lines, boolean allDetails) {
        com.gregtech.gregtech.content.cover.CanvasData.tooltip(data,lines);
        if(com.gregtech.gregtech.content.data.VisualDocumentData.tooltip(data,lines,allDetails))return;
        if (data == null || !data.contains(GTMaterialDataRecipes.NBT_REPLICATOR_DATA)) return;
        GTMaterial material = com.gregtech.gregtech.api.material.GTMaterialRegistry
                .get(data.getShort(GTMaterialDataRecipes.NBT_REPLICATOR_DATA));
        if (!material.isValid()) return;
        String name = material.getLocalName();
        if (!GTMaterialDataRecipes.isReplicable(material)) {
            lines.add(Component.translatable("gt.tooltip.usb.material_data", name)
                    .withStyle(ChatFormatting.AQUA)
                    .append(Component.translatable("gt.tooltip.usb.not_replicable")
                            .withStyle(ChatFormatting.GOLD)));
            return;
        }
        if (!allDetails) {
            lines.add(Component.translatable("gt.tooltip.usb.material_short", name,
                    String.valueOf(material.getNeutrons()), String.valueOf(material.getProtons()),
                    String.valueOf(replicatorEnergy(material))).withStyle(ChatFormatting.AQUA));
            return;
        }
        lines.add(Component.translatable("gt.tooltip.usb.material_data", name)
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("gt.tooltip.usb.replicable_hint").withStyle(ChatFormatting.AQUA));
        lines.add(body("gt.tooltip.usb.neutral_matter", String.valueOf(material.getNeutrons()),
                ChatFormatting.YELLOW));
        lines.add(body("gt.tooltip.usb.charged_matter", String.valueOf(material.getProtons()),
                ChatFormatting.RED));
        lines.add(body("gt.tooltip.usb.energy", String.valueOf(replicatorEnergy(material)),
                ChatFormatting.AQUA));
    }

    /** The port's replicator cost for one material, in QU — see the javadoc on the class. */
    public static long replicatorEnergy(GTMaterial material) {
        return GTMaterialDataRecipes.replicatorEnergy(material);
    }

    private static Component body(String key, String value, ChatFormatting valueStyle) {
        return Component.translatable(key).withStyle(ChatFormatting.WHITE)
                .append(Component.literal(value).withStyle(valueStyle));
    }
}
