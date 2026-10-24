/* Adapted from Gregorius Techneticies, Copyright (c) 2019; LGPL-3.0-or-later.
 * Original MultiTileEntityPanel / Loader_MultiTileEntities; provenance in panels-source-20261006.json. */
package gregtech.item;
/** Native item boundary to the shared original panel identity. */
public interface PanelItemView {
    gregtech.content.transport.PanelCatalog.Spec panelSpec();
    default net.minecraft.network.chat.Component coloredPanelName() {
        var spec = panelSpec();
        var base = net.minecraft.network.chat.Component.translatable("block.gt6ce.panel_" + spec.kind());
        return spec.color().isEmpty() ? base : net.minecraft.network.chat.Component.translatable(
                "item.gt6ce.colored_panel", net.minecraft.network.chat.Component.translatable("color.minecraft." + spec.color()), base);
    }
}
