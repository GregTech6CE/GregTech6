package com.gregtech.gregtech.platform.neoforge.smeltery;

import com.gregtech.gregtech.api.machine.ManualToolRules;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** First two real manual tool kinds; 1.21 stack components carry legacy GT.ToolStats. */
public final class SmelteryToolItem extends com.gregtech.gregtech.item.GTToolItem {
    public enum Kind { CHISEL, PINCERS }
    private final Kind kind;
    public SmelteryToolItem(Kind kind) { super(new Properties().stacksTo(1),com.gregtech.gregtech.api.tool.GTToolType.valueOf(kind.name())); this.kind = kind; }
    public Kind kind() { return kind; }
    public ItemStack assemble(GTMaterial head, GTMaterial handle) {
        if(head==null||!head.isValid()||handle==null||!handle.isValid()||head.getToolTypes()<(kind==Kind.CHISEL?2:1)||(kind==Kind.CHISEL&&!MaterialPrefix.toolHeadChisel.isValidFor(head)))return ItemStack.EMPTY;
        return com.gregtech.gregtech.api.tool.GTToolHelper.write(new ItemStack(this),head,handle);
    }
    public static boolean matches(ItemStack stack, Kind kind) {
        if (!com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(stack,com.gregtech.gregtech.api.tool.GTToolType.valueOf(kind.name())) || !stack.has(DataComponents.MAX_DAMAGE)) return false;
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.copyTag().contains("GT.ToolStats")) return false;
        var stats = data.copyTag().getCompound("GT.ToolStats");
        return GTMaterialRegistry.get(stats.getString("head")).isValid() && stack.getDamageValue() < stack.getMaxDamage();
    }
    public static void wear(ItemStack stack, Player player, InteractionHand hand) {
        if (!player.isCreative()) stack.hurtAndBreak(1, player, hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
    }
}
