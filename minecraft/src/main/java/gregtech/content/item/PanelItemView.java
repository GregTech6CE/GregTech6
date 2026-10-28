/* Adapted from Gregorius Techneticies, Copyright (c) 2019; LGPL-3.0-or-later.
 * Original MultiTileEntityPanel / Loader_MultiTileEntities; provenance in panels-source-20261006.json. */
package gregtech.content.item;
/** Native item boundary to the shared original panel identity. */
public interface PanelItemView {
    gregtech.content.transport.PanelCatalog.Spec panelSpec();

    default net.minecraft.network.chat.Component panelName(net.minecraft.network.chat.Component baseName) {
        var spec = panelSpec();
        if (spec.kind().equals("wood")) {
            var plank = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.parse(spec.input()));
            if (plank != net.minecraft.world.item.Items.AIR)
                return net.minecraft.network.chat.Component.translatable(
                        "item.gt6ce.material_panel",
                        baseName,
                        new net.minecraft.world.item.ItemStack(plank).getHoverName());
        }
        return spec.color().isEmpty() ? baseName : coloredPanelName();
    }

    default net.minecraft.network.chat.Component coloredPanelName() {
        var spec = panelSpec();
        var base = net.minecraft.network.chat.Component.translatable("block.gt6ce.panel_" + spec.kind());
        return spec.color().isEmpty()
                ? base
                : net.minecraft.network.chat.Component.translatable(
                        "item.gt6ce.colored_panel",
                        net.minecraft.network.chat.Component.translatable("color.minecraft." + spec.color()),
                        base);
    }
}
