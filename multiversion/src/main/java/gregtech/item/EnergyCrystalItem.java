package gregtech.item;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialFormItem;
import gregtech.block.energy.ChemicalBatteryBlock;
import gregtech.content.energy.EnergyCrystalSpec;
import gregtech.data.MaterialPrefix;
import gregtech.registry.GTChemicalBatteries;
import net.minecraft.world.item.Item;

/** One canonical Energium gem: both a recipe material and a rechargeable, placeable LU crystal. */
public final class EnergyCrystalItem extends ChemicalBatteryItem implements MaterialFormItem {
    public EnergyCrystalItem(ChemicalBatteryBlock block) { super(block); }
    @Override public EnergyCrystalSpec spec() { return (EnergyCrystalSpec) super.spec(); }
    @Override public GTMaterial getMaterial() { return spec().material(); }
    @Override public MaterialPrefix getPrefix() { return spec().prefix(); }

    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, flags);
*///?} else {
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flags) {
        super.appendHoverText(stack, level, tooltip, flags);
//?}
        gregtech.client.MaterialTooltips.append(stack, getMaterial(), getPrefix(), tooltip, flags);
    }

    public static Item createOrMaterial(MaterialPrefix prefix, GTMaterial material) {
        EnergyCrystalSpec spec = EnergyCrystalSpec.find(prefix, material);
        if (spec == null) return new MaterialItem(new Properties().stacksTo(64), prefix, material);
        return GTChemicalBatteries.allRegistered().stream().map(java.util.function.Supplier::get)
                .filter(block -> block.spec().id().equals(spec.id())).findFirst()
                .map(EnergyCrystalItem::new).orElseThrow(() -> new IllegalStateException("Missing crystal block " + spec.id()));
    }
}
