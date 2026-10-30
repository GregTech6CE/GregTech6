package gregtech.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
//?}

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Resource-pack textures in GUI pixels: fixed end caps, repeated four-pixel middle. */
@EventBusSubscriber(modid = "gt6ce", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class MachineGuiChrome {
    private static final int BRAND_X = 79, BRAND_Y = 62, BRAND_SIZE = 19;
    private static final Map<ResourceLocation, Boolean> BRAND_SPACE = new ConcurrentHashMap<>();

    private MachineGuiChrome() {}

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resources -> {
            BRAND_SPACE.clear();
            GuiTemplates.reload();
        });
    }

    /** Resolve the registry translation key, never the translated/possibly ambiguous display text. */
    public static ItemStack iconForTitle(Component title) {
        if (title.getContents() instanceof TranslatableContents translated) {
            String key = translated.getKey();
            if (key.startsWith("block.") || key.startsWith("item.")) {
                String name = key.substring(key.indexOf('.') + 1);
                int separator = name.indexOf('.');
                if (separator > 0) {
                    ResourceLocation id = ResourceLocation.tryParse(name.substring(0, separator) + ":" + name.substring(separator + 1));
                    if (id != null && BuiltInRegistries.ITEM.containsKey(id))
                        return new ItemStack(BuiltInRegistries.ITEM.get(id));
                }
                // Namespaced addon paths may contain '/', which translation keys replace with '.'.
                for (var item : BuiltInRegistries.ITEM)
                    if (item.getDescriptionId().equals(key)) return new ItemStack(item);
            }
            // Paged containers wrap their item name in a translated title with a page number.
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Component name) {
                    ItemStack icon = iconForTitle(name);
                    if (!icon.isEmpty()) return icon;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static void title(GuiGraphics graphics, Font font, Component title, ItemStack icon, int maxWidth) {
        int textX = icon.isEmpty() ? 4 : 17;
        int tabWidth = titleWidth(font, title, icon, maxWidth);
        int middleWidth = tabWidth - 7;
        RenderSystem.enableBlend();
        graphics.setColor(1, 1, 1, 1);
        var label = GuiTemplates.sprite("base/label");
        label.region(graphics, 0, -10, 0, 0, 4, 14);
        for (int x = 4; x < 4 + middleWidth; x += 4)
            label.region(graphics, x, -10, 4, 0, 4, 14);
        label.region(graphics, 4 + middleWidth, -10, 8, 0, 3, 14);
        if (!icon.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(4, -7, 0);
            graphics.pose().scale(10f / 16f, 10f / 16f, 1);
            graphics.renderItem(icon, 0, 0);
            graphics.pose().popPose();
        }
        int textWidth = titleTextWidth(tabWidth, icon);
        if (font.width(title) > textWidth) {
            FormattedText shortened = font.substrByWidth(title, Math.max(0, textWidth - font.width("...")));
            var display = Language.getInstance().getVisualOrder(
                    FormattedText.composite(shortened, FormattedText.of("...")));
            graphics.drawString(font, display, textX, -7, 0x404040, false);
        } else {
            graphics.drawString(font, title, textX, -7, 0x404040, false);
        }
    }

    public static int titleWidth(Font font, Component title, ItemStack icon, int maxWidth) {
        int contentWidth = font.width(title) + (icon.isEmpty() ? 0 : 13) + 2;
        int middleWidth = Math.min(((contentWidth + 3) / 4) * 4, Math.max(0, (maxWidth - 15) / 4 * 4));
        return 4 + middleWidth + 3;
    }

    public static int titleTextWidth(int tabWidth, ItemStack icon) {
        return Math.max(0, tabWidth - 3 - (icon.isEmpty() ? 4 : 17) - 2);
    }

    public static void brand(GuiGraphics graphics, ResourceLocation texture, List<Slot> slots) {
        for (Slot slot : slots)
            if (slot.x - 1 < BRAND_X + BRAND_SIZE && slot.x + 17 > BRAND_X
                    && slot.y - 1 < BRAND_Y + BRAND_SIZE && slot.y + 17 > BRAND_Y) return;
        brand(graphics, texture);
    }

    public static void brand(GuiGraphics graphics, ResourceLocation texture) {
        if (texture == null || !BRAND_SPACE.computeIfAbsent(texture, MachineGuiChrome::hasBlankBrandArea)) return;
        RenderSystem.enableBlend();
        graphics.setColor(1, 1, 1, 1);
        GuiTemplates.sprite("icon/icon_g").draw(graphics, BRAND_X, BRAND_Y);
    }

    private static boolean hasBlankBrandArea(ResourceLocation texture) {
        var template = GuiTemplates.find(GuiTemplates.forTexture(texture));
        if (template.isPresent()) return template.get().brand();
        var resource = Minecraft.getInstance().getResourceManager().getResource(GuiTemplates.legacyTexture(texture));
        if (resource.isEmpty()) return false;
        try (var stream = resource.get().open(); var image = NativeImage.read(stream)) {
            // GUI sheets have 256 logical pixels; read the equivalent area in HD resource packs.
            int left = BRAND_X * image.getWidth() / 256;
            int top = BRAND_Y * image.getHeight() / 256;
            int right = (BRAND_X + BRAND_SIZE) * image.getWidth() / 256;
            int bottom = (BRAND_Y + BRAND_SIZE) * image.getHeight() / 256;
            if (left >= right || top >= bottom) return false;
            int background = image.getPixelRGBA(left, top);
            if ((background >>> 24) != 255) return false;
            for (int y = top; y < bottom; y++) for (int x = left; x < right; x++)
                if (image.getPixelRGBA(x, y) != background) return false;
            return true;
        } catch (IOException error) {
            return false;
        }
    }
}
