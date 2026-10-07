package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.gregtech.gregtech.api.fluid.FluidDisplayBinding;
import com.gregtech.gregtech.content.bumble.GTBumbleSpecies;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTFluidItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Installed item callbacks with the real resource-manager zh_cn locale; isolated delivery probe only. */
final class LanguageDeliveryChecks {
    private static final String FLUID = "gregtech.fluid_display.";
    private static final Map<String,String> ANVILS = Map.ofEntries(
        Map.entry("anvil","gt.multitileentity.32031"),
        Map.entry("anvil_adamantium","gt.multitileentity.32048"),
        Map.entry("anvil_arsenicbronze","gt.multitileentity.32107"),
        Map.entry("anvil_arseniccopper","gt.multitileentity.32106"),
        Map.entry("anvil_blacksteel","gt.multitileentity.32034"),
        Map.entry("anvil_blackstone","gt.multitileentity.32095"),
        Map.entry("anvil_bluesteel","gt.multitileentity.32035"),
        Map.entry("anvil_bronze","gt.multitileentity.32028"),
        Map.entry("anvil_desh","gt.multitileentity.32071"),
        Map.entry("anvil_draconium","gt.multitileentity.32049"),
        Map.entry("anvil_draconiumawakened","gt.multitileentity.32068"),
        Map.entry("anvil_efrine","gt.multitileentity.32092"),
        Map.entry("anvil_fierysteel","gt.multitileentity.32039"),
        Map.entry("anvil_gaiaspirit","gt.multitileentity.32047"),
        Map.entry("anvil_graniteblack","gt.multitileentity.32026"),
        Map.entry("anvil_granitered","gt.multitileentity.32027"),
        Map.entry("anvil_hslatungstenalloy","gt.multitileentity.32091"),
        Map.entry("anvil_infinity","gt.multitileentity.32069"),
        Map.entry("anvil_iridium","gt.multitileentity.32046"),
        Map.entry("anvil_ironwood","gt.multitileentity.32030"),
        Map.entry("anvil_lead","gt.multitileentity.32050"),
        Map.entry("anvil_manasteel","gt.multitileentity.32033"),
        Map.entry("anvil_netherite","gt.multitileentity.32088"),
        Map.entry("anvil_octine","gt.multitileentity.32038"),
        Map.entry("anvil_redsteel","gt.multitileentity.32036"),
        Map.entry("anvil_stone","gt.multitileentity.32025"),
        Map.entry("anvil_syrmorite","gt.multitileentity.32029"),
        Map.entry("anvil_terrasteel","gt.multitileentity.32041"),
        Map.entry("anvil_thaumium","gt.multitileentity.32032"),
        Map.entry("anvil_titanium","gt.multitileentity.32040"),
        Map.entry("anvil_titaniumgold","gt.multitileentity.32043"),
        Map.entry("anvil_tungsten","gt.multitileentity.32045"),
        Map.entry("anvil_tungstensteel","gt.multitileentity.32044"),
        Map.entry("anvil_vanadiumsteel","gt.multitileentity.32037"),
        Map.entry("anvil_voidmetal","gt.multitileentity.32042")
    );
    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }
    static JsonObject capture(Minecraft client, GuiGraphics g) {
        var manager = client.getLanguageManager();
        String previous = manager.getSelected();
        try {
            manager.setSelected("en_us");
            manager.onResourceManagerReload(client.getResourceManager());
            int englishNames=0;
            var unresolved=new ArrayList<String>();
            for(var registered:BuiltInRegistries.ITEM) {
                var id=BuiltInRegistries.ITEM.getKey(registered);
                if(!id.getNamespace().equals("gregtech"))continue;
                String name=new ItemStack(registered).getHoverName().getString();
                if(name.isBlank()||name.matches("(?s).*(?:item|block|fluid_type|material)\\.gregtech\\.[a-z_].*"))
                    unresolved.add(id+" = "+name);
                englishNames++;
            }
            require(unresolved.isEmpty(),"Unresolved installed English names: "+unresolved+"; total="+unresolved.size());
            manager.setSelected("zh_cn");
            manager.onResourceManagerReload(client.getResourceManager());
            require(Language.getInstance().has("gt.multiitem.bumblebee.0"), "Original Chinese resource is loaded");
            int names=0, nonempty=0, empty=0;
            String[] states={"drone","princess","queen","dead","scanned_drone","scanned_princess","scanned_queen","scanned_dead"};
            int[] metas={0,1,2,4,5,6,7,9};
            ItemStack beeExample=ItemStack.EMPTY;
            for (var species : GTBumbleSpecies.SPECIES) for (int i=0;i<states.length;i++) {
                var stack=item(species.beeId(states[i]));
                String source="gt.multiitem.bumblebee."+(species.id()+metas[i]);
                require(stack.getHoverName().getString().equals(original(source)), "Original numbered bee name "+source);
                String expected=original(source+".tooltip");
                String nativeKey=stack.getDescriptionId()+".tooltip";
                var actual=tooltip(stack).stream().filter(c -> has(c,nativeKey)).toList();
                require(actual.size()==(expected.isEmpty()?0:1), "Original empty/nonempty bee description "+source);
                if (!expected.isEmpty()) {
                    require(actual.get(0).getString().equals(expected), "Exact original bee description "+source);
                    nonempty++; if(beeExample.isEmpty())beeExample=stack;
                } else empty++;
                names++;
            }
            for (var entry:ANVILS.entrySet()) require(item(entry.getKey()).getHoverName().getString().equals(original(entry.getValue())),
                    "Original numbered anvil name "+entry.getKey());
            require(names==640 && nonempty==360 && empty==280,"Complete source bee catalog and original empty rows");

            var ironFluid=new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse("gregtech:molten_iron")),144);
            var oxygen=GTFluids.stack("Oxygen",1000);
            var swamp=GTFluids.stack("Swampwater",1000);
            require(!ironFluid.isEmpty()&&!oxygen.isEmpty()&&!swamp.isEmpty(),"Native fluid fixture identities");
            require(icon(ironFluid).getHoverName().getString().equals(original("fluid.molten.iron")),"Actual molten iron item uses original name");
            require(icon(oxygen).getHoverName().getString().equals(original("fluid.oxygen")),"Actual oxygen item uses original name");
            require(icon(swamp).getHoverName().getString().equals(original("gt.multiitem.bottles.6")),"World water preserves GT source name");
            require(tooltip(icon(ironFluid)).stream().noneMatch(c -> has(c,FLUID+"amount")),"Creative icon does not invent quantity");
            var iron=display(ironFluid);var ironLines=properties(iron);
            require(ironLines.stream().anyMatch(c -> c.getString().equals("Amount: 144 L")),"Real display payload quantity");
            require(ironLines.stream().anyMatch(c -> has(c,FLUID+"castable")),"Actual registered iron ingot enables mold hint");
            require(ironLines.stream().noneMatch(c -> has(c,FLUID+"creative_fill")),"Null-player tooltip has no infinite fill instruction");
            verifyProperties(ironFluid,ironLines);
            verifyProperties(oxygen,properties(display(oxygen)));
            verifyProperties(swamp,properties(display(swamp)));
            var pahoehoe=GTFluids.stack("Lava_Pahoehoe",1000);
            var volcanic=GTFluids.stack("Lava_Volcanic",1000);
            var lava=new FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000);
            var hot=properties(display(lava));var hotter=properties(display(volcanic));
            verifyProperties(pahoehoe,properties(display(pahoehoe)));verifyProperties(lava,hot);verifyProperties(volcanic,hotter);
            require(hot.stream().anyMatch(c -> has(c,"gt.recipe.fuels.hot")&&c.getString().contains(": 80 GU/L; 80_000 GU total")),"Actual hot recipe maximum, vanilla lava");
            require(hotter.stream().anyMatch(c -> has(c,"gt.recipe.fuels.hot")&&c.getString().contains(": 1280 GU/L; 1_280_000 GU total")),"Actual volcanic recipe maximum");
            var water=new FluidStack(net.minecraft.world.level.material.Fluids.WATER,250);
            var waterLines=properties(display(water));verifyProperties(water,waterLines);
            require(waterLines.stream().anyMatch(c -> has(c,FLUID+"owner_vanilla")),"Vanilla ownership remains vanilla in a GT display carrier");

            int width=client.getWindow().getGuiScaledWidth(),height=client.getWindow().getGuiScaledHeight();
            g.pose().pushPose();g.pose().translate(0,0,2000);g.fill(0,0,width,height,0xff14141c);
            g.drawString(client.font,"GregTech.lang / zh_cn / installed item callbacks",8,5,0xffffff,false);
            int col=width/2;
            draw(client,g,icon(ironFluid),ironLines,7,22,col-14,height-76);
            draw(client,g,icon(volcanic),hotter,col+7,22,col-14,height-76);
            g.renderItem(beeExample,8,height-47);g.drawString(client.font,beeExample.getHoverName(),30,height-44,0xffffff,false);
            var anvil=item("anvil_arsenicbronze");g.renderItem(anvil,col+7,height-47);
            g.drawString(client.font,anvil.getHoverName(),col+29,height-44,0xffffff,false);
            g.drawString(client.font,"640 bee names / 360 descriptions / 280 empty / 35 anvils",8,height-15,0xaaaaaa,false);
            g.pose().popPose();
            var report=new JsonObject();report.addProperty("locale","zh_cn");report.addProperty("englishItemNames",englishNames);report.addProperty("beeNames",names);
            report.addProperty("beeDescriptions",nonempty);report.addProperty("emptyBeeDescriptions",empty);
            report.addProperty("anvilNames",ANVILS.size());report.addProperty("fluidNameSamples",3);
            report.addProperty("fluidPropertySamples",7);report.addProperty("hotRecipeValues",2);
            var samples=new JsonArray();for(var c:ironLines)samples.add(c.getString());report.add("moltenIronTooltip",samples);
            report.addProperty("scope","Installed resource-manager language and actual item callbacks; explicitly supplied native lookup for Neo payloads at title screen; no in-world player hover or survival claim");
            return report;
        } finally {
            manager.setSelected(previous);manager.onResourceManagerReload(client.getResourceManager());
        }
    }
    private static String original(String key) {
        require(Language.getInstance().has(key),"Original key present "+key);
        return Language.getInstance().getOrDefault(key);
    }
    private static ItemStack item(String path) {
        var id=ResourceLocation.parse("gregtech:"+path);require(BuiltInRegistries.ITEM.containsKey(id),"Registered item "+id);
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }
    private static ItemStack icon(FluidStack fluid) {
        return item("fluid_item_"+BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getPath());
    }
    private static boolean has(Component c,String key) {
        return c.getContents() instanceof TranslatableContents t && t.getKey().equals(key) || c.getSiblings().stream().anyMatch(s -> has(s,key));
    }
    private static void verifyProperties(FluidStack fluid,List<Component> lines) {
        var type=fluid.getFluid().getFluidType();int k=type.getTemperature(fluid);
        require(lines.stream().anyMatch(c -> c.getString().equals("Temperature: "+k+" K ("+(k-273)+"°C)")),"Actual fluid temperature "+fluid);
        require(lines.stream().anyMatch(c -> c.getString().startsWith("Density: "+type.getDensity(fluid)+" ;")),"Actual fluid density "+fluid);
        require(lines.stream().anyMatch(c -> c.getString().equals("Viscosity: "+type.getViscosity(fluid)))==(type.getViscosity(fluid)!=0),"Actual fluid viscosity "+fluid);
        require(lines.stream().anyMatch(c -> c.getString().equals("Luminosity: "+type.getLightLevel(fluid)))==(type.getLightLevel(fluid)!=0),"Actual fluid light level "+fluid);
    }
    private static void draw(Minecraft client,GuiGraphics g,ItemStack stack,List<Component> lines,int x,int y,int width,int maxY) {
        g.renderItem(stack,x,y);g.drawString(client.font,stack.getHoverName(),x+21,y+4,0xffffff,false);y+=24;
        for(var line:lines)for(var wrapped:client.font.split(line,width)) {
            require(y<maxY,"Language preview fits the screenshot");g.drawString(client.font,wrapped,x,y,0xffffff,false);y+=10;
        }
    }
    private static List<Component> tooltip(ItemStack s) { return s.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.NORMAL); }
    private static net.minecraft.core.RegistryAccess lookup() { return net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY); }
    private static ItemStack display(FluidStack f) { return FluidDisplayBinding.display(lookup(),f); }
    private static List<Component> properties(ItemStack s) { var lines=new ArrayList<Component>();s.getItem().appendHoverText(s,Item.TooltipContext.of(lookup()),lines,TooltipFlag.NORMAL);return lines; }
}
