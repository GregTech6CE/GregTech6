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
    private static final java.util.regex.Pattern UNRESOLVED_NAME=java.util.regex.Pattern.compile("(?:item|block|fluid_type|material)\\.gregtech\\.[a-z_]");
    private static final java.util.regex.Pattern LATIN_WORD=java.util.regex.Pattern.compile("[A-Za-z]{3}");
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
            var englishSnapshot=new java.util.LinkedHashMap<String,String>();
            var unresolved=new ArrayList<String>();
            for(var registered:BuiltInRegistries.ITEM) {
                var id=BuiltInRegistries.ITEM.getKey(registered);
                if(!id.getNamespace().equals("gregtech"))continue;
                String name=new ItemStack(registered).getHoverName().getString();
                if(name.isBlank()||UNRESOLVED_NAME.matcher(name).find())
                    unresolved.add(id+" = "+name);
                englishSnapshot.put(id.toString(),name);
                englishNames++;
            }
            require(unresolved.isEmpty(),"Unresolved installed English names: "+unresolved+"; total="+unresolved.size());
            int machineNamesEnglish=verifyMachineNames();
            int originalEnglishForms=verifyOriginalEnglishForms();
            int toolHintsEnglish=verifyToolHints(false);
            int coloredEnglish=verifyColoredConstruction(false);
            int stoneEnglish=verifySourceStones(false);
            int bushEnglish=verifyBushes(false);
            int coinsEnglish=verifyCoins(false);
            int technologyEnglish=verifyTechnologyNames(false);
            int lootEnglish=verifyLootChestNames(false),fluidsEnglish=verifyDeclaredFluidNames();
            int fluidFallbacksEnglish=verifyFluidMaterialFallbacks(false);
            int wiresOresEnglish=verifyAdditionalBlockNames();
            var generatedEnglish=verifyGeneratedNames();
            manager.setSelected("zh_cn");
            manager.onResourceManagerReload(client.getResourceManager());
            int machineNamesChinese=verifyMachineNames();
            int coloredChinese=verifyColoredConstruction(true);
            int stoneChinese=verifySourceStones(true);
            int bushChinese=verifyBushes(true);
            int coinsChinese=verifyCoins(true);
            int technologyChinese=verifyTechnologyNames(true);
            int lootChinese=verifyLootChestNames(true),fluidsChinese=verifyDeclaredFluidNames();
            int fluidFallbacksChinese=verifyFluidMaterialFallbacks(true);
            int wiresOresChinese=verifyAdditionalBlockNames();
            var generatedChinese=verifyGeneratedNames();
            var nameInventory=recordNameInventory(client,englishSnapshot);
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
            var identitySamples=Map.ofEntries(
                    Map.entry("bale_rye","gt.block.bale.crop.0"),
                    Map.entry("bale_grass_rotten","gt.block.bale.grass.3"),
                    Map.entry("sand_granite_magnetite","gt.block.sands.2"),
                    Map.entry("spike_super","gt.block.spikes.super.0"),
                    Map.entry("filter_oredict","gt.multitileentity.30259"),
                    Map.entry("sapling_large_hazel","gt.block.sapling.12"),
                    Map.entry("leaves_opaque_maple_red","gt.block.leaves.1"),
                    Map.entry("crank","gt.multitileentity.32111"),
                    Map.entry("auto_igniter_steel","gt.multitileentity.15010"),
                    Map.entry("item_pipe_restrictive_huge_brass","gt.multitileentity.25007"),
                    Map.entry("pipe_nonuple_wood","gt.multitileentity.26006"),
                    Map.entry("fluid_cell_wax","gt.multitileentity.32600"),
                    Map.entry("fluid_measuring_pot","gt.multitileentity.32738"),
                    Map.entry("glowtus_lime","gt.block.lilypad.glowtus.10"),
                    Map.entry("track_steel","gt.block.rail.steel"),
                    Map.entry("log_dry","gt.block.log.1.0"),
                    Map.entry("planks_cinnamon","gt.block.planks.5"),
                    Map.entry("battery_lithium_cobalt_mv","gt.multitileentity.14032"),
                    Map.entry("compact_electric_motor_luv","gt.multiitem.technological.12006"),
                    Map.entry("extruder_shape_tinypipe","gt.multiitem.technological.10009"));
            var mismatches=new ArrayList<String>();
            for(var entry:identitySamples.entrySet()) {
                String actual=item(entry.getKey()).getHoverName().getString(), expected=original(entry.getValue());
                if(!actual.equals(expected))mismatches.add(entry.getKey()+": "+actual+" != "+expected);
            }
            require(mismatches.isEmpty(),"Installed original identity names: "+mismatches);
            var shape=item("extruder_shape_tinypipe");
            require(tooltip(shape).stream().anyMatch(c -> c.getString().equals(original("gt.multiitem.technological.10009.tooltip"))),
                    "Newly imported original shape description reaches native tooltip");
            var toolSamples=List.of(com.gregtech.gregtech.api.tool.GTToolType.SWORD,
                    com.gregtech.gregtech.api.tool.GTToolType.GEM_PICK,
                    com.gregtech.gregtech.api.tool.GTToolType.POCKET_MULTITOOL);
            for(var type:toolSamples) {
                var stack=com.gregtech.gregtech.api.tool.GTToolHelper.displayTool(type);
                require(stack.getHoverName().getString().contains(original("gt.metatool.01."+type.gt6Id())),
                        "Assembled tool keeps localized original kind "+type);
                require(!stack.getHoverName().getString().contains("%s"),"No unexpanded assembled tool format");
            }

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
            report.addProperty("sourceIdentitySamples",identitySamples.size());
            report.addProperty("machineNamesEnglish",machineNamesEnglish);
            report.addProperty("originalEnglishMaterialFormSamples",originalEnglishForms);
            report.addProperty("machineNamesChinese",machineNamesChinese);
            report.addProperty("coloredConstructionNamesEnglish",coloredEnglish);
            report.addProperty("coloredConstructionNamesChinese",coloredChinese);
            report.addProperty("sourceStoneNamesEnglish",stoneEnglish);
            report.addProperty("sourceStoneNamesChinese",stoneChinese);
            report.addProperty("bushNamesAndOutputsEnglish",bushEnglish);
            report.addProperty("bushNamesAndOutputsChinese",bushChinese);
            report.addProperty("coinNamesAndMaterialsEnglish",coinsEnglish);
            report.addProperty("coinNamesAndMaterialsChinese",coinsChinese);
            report.addProperty("additionalSourceBlockNamesEnglish",wiresOresEnglish);
            report.addProperty("additionalSourceBlockNamesChinese",wiresOresChinese);
            report.addProperty("lootChestNamesEnglish",lootEnglish);
            report.addProperty("lootChestNamesChinese",lootChinese);
            report.addProperty("declaredFluidNamesEnglish",fluidsEnglish);
            report.addProperty("declaredFluidNamesChinese",fluidsChinese);
            report.addProperty("materialFluidFallbackNamesEnglish",fluidFallbacksEnglish);
            report.addProperty("materialFluidFallbackNamesChinese",fluidFallbacksChinese);
            report.addProperty("sourceTechnologyNamesEnglish",technologyEnglish);
            report.addProperty("sourceTechnologyNamesChinese",technologyChinese);
            report.addProperty("chineseItemNames",englishSnapshot.size());
            report.addProperty("latinNameCandidates",nameInventory.get("candidateCount").getAsInt());
            report.add("materialFormNames",nameInventory.getAsJsonObject("materialFormNames"));
            report.addProperty("assembledToolNames",toolSamples.size());
            report.addProperty("toolHintsEnglish",toolHintsEnglish);
            report.addProperty("toolHintsChinese",verifyToolHints(true));
            report.add("generatedNamesEnglish",generatedEnglish);report.add("generatedNamesChinese",generatedChinese);
            report.addProperty("newSourceDescriptions",1);
            var samples=new JsonArray();for(var c:ironLines)samples.add(c.getString());report.add("moltenIronTooltip",samples);
            report.addProperty("scope","Installed resource-manager language and actual item callbacks; explicitly supplied native lookup for Neo payloads at title screen; no in-world player hover or survival claim");
            return report;
        } finally {
            manager.setSelected(previous);manager.onResourceManagerReload(client.getResourceManager());
        }
    }

    private static void requireInstalledName(ItemStack stack) {
        String key=stack.getDescriptionId();
        require(Language.getInstance().has(key), "Generated native name key "+key);
        require(stack.getHoverName().getString().equals(Component.translatable(key).getString()),
                "Generated original name reaches item callback "+key);
    }
    private static JsonObject verifyGeneratedNames() {
        for(String id:ANVILS.keySet())requireInstalledName(item(id));
        String[] states={"drone","princess","queen","dead","scanned_drone","scanned_princess","scanned_queen","scanned_dead"};
        int bees=0;
        for(var species:GTBumbleSpecies.SPECIES)for(String state:states) {
            var stack=item(species.beeId(state));requireInstalledName(stack);bees++;
            String key=stack.getDescriptionId()+".tooltip";
            require(Language.getInstance().has(key),"Generated bee description key "+key);
            String expected=Component.translatable(key).getString();
            var lines=tooltip(stack).stream().filter(c->has(c,key)).toList();
            require(lines.size()==(expected.isEmpty()?0:1),"Generated bee description count "+key);
            if(!expected.isEmpty())require(lines.get(0).getString().equals(expected),"Generated bee description "+key);
        }
        var books=com.gregtech.gregtech.content.book.ColoredBookRules.VARIANTS;
        for(var variant:books)requireInstalledName(item(variant.path()));
        var canvases=com.gregtech.gregtech.content.cover.CanvasRules.VARIANTS;
        for(var variant:canvases)requireInstalledName(item(variant.path()));
        var panels=com.gregtech.gregtech.content.transport.PanelCatalog.ALL;
        for(var spec:panels)requireInstalledName(item(spec.id()));
        int bottles=0;
        for(var registered:BuiltInRegistries.ITEM)if(registered instanceof com.gregtech.gregtech.item.BottleItem) {
            var stack=new ItemStack(registered);requireInstalledName(stack);
            String key=stack.getDescriptionId()+".tooltip";
            require(Language.getInstance().has(key),"Original bottle description key "+key);
            String expected=Component.translatable(key).getString();
            var lines=tooltip(stack).stream().filter(c->has(c,key)).toList();
            require(lines.size()==(expected.isEmpty()?0:1),"Bottle description appears once or stays empty "+key);
            if(!expected.isEmpty())require(lines.get(0).getString().equals(expected),"Installed bottle description "+key);
            bottles++;
        }
        require(bottles>100,"Complete registered bottle family");
        int tabs=0;
        for(var tab:BuiltInRegistries.CREATIVE_MODE_TAB) {
            var id=BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if(!id.getNamespace().equals("gregtech"))continue;
            Component title=tab.getDisplayName();
            require(title.getContents() instanceof TranslatableContents,"Creative category uses a language key "+id);
            String key=((TranslatableContents)title.getContents()).getKey();
            require(Language.getInstance().has(key),"Creative category key "+key);
            require(title.getString().equals(Component.translatable(key).getString()),"Installed creative category "+id);
            tabs++;
        }
        require(bees==640&&books.size()==28&&canvases.size()==16&&tabs>100,"Complete generated name families");
        var report=new JsonObject();report.addProperty("bees",bees);report.addProperty("anvils",ANVILS.size());
        report.addProperty("books",books.size());report.addProperty("canvases",canvases.size());
        report.addProperty("panels",panels.size());report.addProperty("creativeTabs",tabs);
        report.addProperty("bottles",bottles);
        report.addProperty("faceMasks",verifyFaceNames());
        report.addProperty("machineLabels",verifyMachineLabels());
        report.addProperty("engineDescriptions",verifyEngineDescriptions());
        return report;
    }

    private static int verifyMachineLabels() {
        String bottom=original("gt.lang.face.bottom"),front=original("gt.lang.face.front");
        require(com.gregtech.gregtech.client.TooltipHelper.energyInLine(32,16,64,"EU",bottom).getString()
                .equals(original("gt.lang.energy.input")+": 32 EU/t (16 to 64, "+bottom+")"),"Original input label and unchanged numbers");
        require(com.gregtech.gregtech.client.TooltipHelper.energyOutLine(8,"KU").getString()
                .equals(original("gt.lang.energy.output")+": 8 KU/t"),"Original output label");
        require(com.gregtech.gregtech.client.TooltipHelper.energyInNone().getString()
                .equals(original("gt.lang.energy.input")+": "+original("gt.lang.face.none")),"No input label");
        for(var type:Map.of("items_in","item.input","items_out","item.output","fluids_in","fluid.input","fluids_out","fluid.output").entrySet()) {
            var line=com.gregtech.gregtech.client.TooltipHelper.ioLine(type.getKey(),9,0,net.minecraft.ChatFormatting.GREEN);
            require(line!=null&&line.getString().equals(original("gt.lang."+type.getValue())+": "+bottom+" (auto), "+front+" (no auto)"),
                    "Original I/O label and face composition "+type.getKey());
            require(com.gregtech.gregtech.client.TooltipHelper.ioLine(type.getKey(),0,0,net.minecraft.ChatFormatting.GREEN)==null,
                    "Empty I/O mask produces no line "+type.getKey());
        }
        return 7;
    }

    private static int verifyEngineDescriptions() {
        int count=0;
        for(var registered:BuiltInRegistries.ITEM)if(registered instanceof BlockItem item
                && item.getBlock() instanceof com.gregtech.gregtech.block.machine.EngineBlock engine) {
            var material=switch(engine.engineType()) {
                case ELECTRIC -> engine.engineSpec(com.gregtech.gregtech.api.machine.ElectricEngineSpec.class).material();
                case FLUX -> engine.engineSpec(com.gregtech.gregtech.api.machine.FluxEngineSpec.class).material();
                case STEAM -> engine.engineSpec(com.gregtech.gregtech.api.machine.SteamEngineData.class).material();
                case ROTATION -> engine.engineSpec(com.gregtech.gregtech.api.machine.RotationEngineSpec.class).material();
                case DIESEL -> engine.engineSpec(com.gregtech.gregtech.api.machine.DieselEngineSpec.class).material();
            };
            var lines=tooltip(new ItemStack(registered)).stream().skip(1).toList();
            String key=material.getTranslationKey();
            // Harvest-level hints may legitimately name the same material inside another row.
            var materialLines=lines.stream().filter(c->c.getContents() instanceof TranslatableContents tr
                    &&tr.getKey().equals(key)).toList();
            require(materialLines.size()==1&&materialLines.get(0).getString().equals(
                    com.gregtech.gregtech.api.material.MaterialPresentation.name(material).getString()),
                    "Engine uses its localized material exactly once "+BuiltInRegistries.ITEM.getKey(registered));
            if(engine.engineType()!=com.gregtech.gregtech.api.machine.EngineType.DIESEL) {
                require(lines.stream().anyMatch(c->has(c,"gt.lang.efficiency")),"Engine source efficiency label");
                require(lines.stream().anyMatch(c->has(c,"gt.lang.energy.capacity")),"Engine source capacity label");
            }
            if(engine.engineType()==com.gregtech.gregtech.api.machine.EngineType.STEAM) {
                require(lines.stream().anyMatch(c->has(c,"fluid_type.gregtech.steam")),"Steam input uses localized fluid name");
                require(lines.stream().filter(c->has(c,"gt.lang.energy.input")||has(c,"gt.lang.energy.capacity"))
                        .noneMatch(c->c.getString().contains(" Steam"))
                        ||net.minecraft.locale.Language.getInstance().getOrDefault("fluid_type.gregtech.steam").equals("Steam"),
                        "Steam tooltip has no hardcoded fluid name");
            }
            require(lines.stream().noneMatch(c->has(c,"tooltip.gregtech.machine.energy_in")
                    ||has(c,"tooltip.gregtech.machine.energy_out")),"Engine has no obsolete untranslated energy title");
            count++;
        }
        require(count>=10,"Registered engine tooltip family");return count;
    }

    private static int verifyFaceNames() {
        // Current public machine-relative mask layout, independent of the helper's switch.
        int[] sides={0,1,4,2,3,5};
        String[] names={"bottom","top","left","right","front","back"};
        for(int i=0;i<sides.length;i++)
            require(com.gregtech.gregtech.client.TooltipHelper.dirName(sides[i]).equals(original("gt.lang.face."+names[i])),
                    "Installed source machine face "+names[i]);
        for(int mask=0;mask<64;mask++) {
            var expected=new ArrayList<String>();
            for(int i=0;i<sides.length;i++)if((mask&(1<<sides[i]))!=0)expected.add(original("gt.lang.face."+names[i]));
            String label=mask==0?original("gt.lang.face.none"):mask==63?original("gt.lang.face.any"):String.join(", ",expected);
            require(com.gregtech.gregtech.client.TooltipHelper.dirMaskToString(mask).equals(label),"Installed machine face mask "+mask);
        }
        return 64;
    }

    private static String original(String key) {
        require(Language.getInstance().has(key),"Original key present "+key);
        return Language.getInstance().getOrDefault(key);
    }
    private static int verifyColoredConstruction(boolean chinese) {
        var families=Map.ofEntries(Map.entry("asphalt","asphalt"),Map.entry("concrete","concrete"),
                Map.entry("concrete_reinforced","concrete.reinforced"),Map.entry("cfoam","cfoam"),
                Map.entry("cfoam_fresh","cfoam.fresh"),
                Map.entry("glass_clear","glass"),Map.entry("glass_glow","glass.glow"),
                Map.entry("cfoam_slab","cfoam.slab.0"),Map.entry("glass_glow_slab","glass.glow.slab.0"));
        int checked=0;var mismatches=new ArrayList<String>();
        for(var entry:families.entrySet())for(var color:DyeColor.values()) {
            var block=((BlockItem)item(entry.getKey()).getItem()).getBlock();
            require(block.defaultBlockState().getProperties().stream().anyMatch(p -> p.getName().equals("color")),
                    "Colored fixture must have a real color state: "+entry.getKey());
            var stack=com.gregtech.gregtech.block.misc.ConcreteBlock.coloredItem(block,color);
            String key="block.gregtech."+entry.getKey()+"."+color.getName();
            String actual=stack.getHoverName().getString();
            String expected=original(key);
            if(chinese) require(expected.equals(original("gt.block."+entry.getValue()+"."+(15-color.getId()))),
                    "Exact original colored phrase "+key);
            if(!actual.equals(expected))mismatches.add(key+": "+actual+" != "+expected);
            checked++;
        }
        require(mismatches.isEmpty(),"Installed colored construction names: "+mismatches);
        return checked;
    }
    private static int verifyLootChestNames(boolean chinese) {
        int checked=0;
        for(String entry:com.gregtech.gregtech.content.loot.LootChestCatalog.ENTRIES) {
            String[] parts=entry.split("\\|");var stack=item("loot_chest_"+parts[0]);
            String expected=original(stack.getDescriptionId());
            if(chinese)require(expected.equals(original("gt.multitileentity.32745")),"Original loot shell name");
            require(stack.getHoverName().getString().equals(expected),"Installed loot chest name "+entry);
            String key=com.gregtech.gregtech.content.storage.OriginalStorageTooltipData.lootKey(parts[1]);
            String label="Contains Loot of "+original(key);
            require(tooltip(stack).stream().filter(c -> c.getString().equals(label)).count()==1,"Loot table label once "+entry);
            var data=new net.minecraft.nbt.CompoundTag();data.putBoolean("GTLootGenerated",true);
            data.putString("id","gregtech:metal_chest");stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data));
            require(tooltip(stack).stream().noneMatch(c -> c.getString().startsWith("Contains Loot of ")),"Consumed chest does not advertise unrolled loot "+entry);
            checked++;
        }
        return checked;
    }
    private static int verifyDeclaredFluidNames() {
        int checked=0;
        for(var registered:BuiltInRegistries.ITEM)if(registered instanceof com.gregtech.gregtech.platform.neoforge.fluid.FluidDisplayItem fluidItem) {
            String key="fluid_type.gregtech."+com.gregtech.gregtech.data.FluidCatalog.sanitizePath(fluidItem.fluidEntry().registryName());
            if(!Language.getInstance().has(key))continue;
            var stack=new ItemStack(registered);
            require(stack.getHoverName().getString().equals(original(key)),"Installed declared fluid name "+key);
            checked++;
        }
        require(checked>100,"Declared fluid catalog checked in actual locale");return checked;
    }
    private static int verifyFluidMaterialFallbacks(boolean chinese) {
        // Original FL helper names of twelve registered GT items. Vanilla water/lava
        // are carried as display payloads, not registered fluid_item_water/lava items.
        // Importing an English-only complete key must not shadow these Chinese names.
        String[] rows={"aluminiumfluoride|Aluminium Fluoride|AluminiumFluoride",
                "constructionfoam|C-Foam|ConstructionFoam","cryolite|Cryolite|Cryolite",
                "freshwater|Fresh Water|FreshWater","fryingoilhot|Hot Frying Oil|FryingOilHot",
                "iodine|Iodine|Iodine",
                "lithiumchlorate|Lithium Chlorate|LithiumChlorate","lithiumchloride|Lithium Chloride|LithiumChloride",
                "molten_graphene|Molten Graphene|Graphene","oliveoil|Olive Oil|OliveOil",
                "uumatter|UU-Matter|UUMatter","waterdistilled|Distilled Water|WaterDistilled"};
        for(String row:rows) {
            String[] parts=row.split("\\|");var stack=item("fluid_item_"+parts[0]);
            require(!Language.getInstance().has("fluid_type.gregtech."+parts[0]),"Material fluid fallback remains available "+parts[0]);
            String expected=chinese?original("gt.material."+parts[2]):parts[1];
            if(chinese && parts[0].startsWith("molten_"))
                expected=Component.translatable("fluid_type.gregtech.molten_material",expected).getString();
            require(stack.getHoverName().getString().equals(expected),"Source formula/material fluid name "+parts[0]);
        }
        return rows.length;
    }
    private static int verifyToolHints(boolean chinese) {
        int count=0;
        for(var definition:com.gregtech.gregtech.api.tool.ToolDefinition.values()) {
            if(definition.tooltipKey()==null)continue;
            var type=com.gregtech.gregtech.api.tool.GTToolType.valueOf(definition.name());
            var stack=com.gregtech.gregtech.api.tool.GTToolHelper.displayTool(type);
            String key="tooltip.gregtech.tool_hint."+definition.tooltipKey();
            String expected=original(key);
            String source=definition==com.gregtech.gregtech.api.tool.ToolDefinition.BUILDER_WAND
                    ? "gt.lang.use.builder.wand.to.ease.building" : "gt.metatool.01."+definition.gt6Id()+".tooltip";
            if(chinese)require(expected.equals(original(source)),"Original tool description or behavior instruction "+type);
            var rows=tooltip(stack).stream().filter(c -> has(c,key)).toList();
            require(rows.size()==(expected.isEmpty()?0:1),"Source tool description appears once or remains absent "+type);
            if(!expected.isEmpty())require(rows.get(0).getString().equals(expected),"Installed tool description "+type);
            count++;
        }
        require(count==10,"Complete hinted tool catalog");return count;
    }
    private static int verifyAdditionalBlockNames() {
        var ids=new ArrayList<String>();
        for(var wire:com.gregtech.gregtech.content.energy.WireCatalog.specifications())
            ids.add(com.gregtech.gregtech.content.energy.WireCatalog.registryId(wire));
        for(var family:com.gregtech.gregtech.content.energy.SignalWireCatalog.families()) {
            ids.add("wire_01_"+family.name());ids.add("cable_01_"+family.name());
        }
        for(var icon:com.gregtech.gregtech.block.SpecialOreDefinitions.ORDERED_ICONS)
            ids.add(icon.startsWith("ore_")?"block_"+icon:icon);
        for(String tier:new String[]{"lv","mv","hv","ev","iv"})
            for(String family:new String[]{"co2_laser","flux_laser","laser_absorber"})ids.add(family+"_"+tier);
        ids.addAll(List.of("zpm","zpm_discharger_basic","zpm_discharger_advanced","zpm_discharger_elite",
                "ingot_pile","plate_pile","plate_gem_pile","coin_pile","sandwich_block","sensor_kilogibblometer"));
        ids.addAll(List.of("log_hole_maple","log_sap_maple","log_hole_rainbowood","log_sap_rainbowood",
                "log_hole_rubber","log_resin_rubber"));
        ids.addAll(List.of("boiler_wall","turbine_wall","large_crucible_wall","cryo_distillation_wall",
                "large_gas_turbine_wall","large_dynamo_wall","heat_exchanger_wall","bedrock_drill_wall",
                "lightning_rod_wall","extender_basic","extender_advanced","extender_elite","extender_wireless"));
        for(String id:ids) {
            var stack=item(id);String key="block.gregtech."+id;
            require(stack.getDescriptionId().equals(key),"Source-bound registry description identity "+id);
            // Exact English/source-Chinese resource contents are independently pinned by the language gate.
            require(stack.getHoverName().getString().equals(original(key)),"Installed complete source block name "+id);
        }
        require(ids.size()==707,"Complete wire, special-ore and utility catalog");return ids.size();
    }
    private static void verifySourceTechnology(String id, String source, boolean chinese) {
        var stack=item(id);String key=stack.getDescriptionId(),expected=original(key);
        if(chinese)require(expected.equals(original(source)),"Technology original identity "+id);
        require(stack.getHoverName().getString().equals(expected),"Installed technology name "+id);
        if(Language.getInstance().has(key+".tooltip")) {
            String description=original(key+".tooltip");
            if(chinese)require(description.equals(original(source+".tooltip")),"Technology original description "+id);
            var rows=tooltip(stack).stream().filter(c -> has(c,key+".tooltip")).toList();
            require(rows.size()==(description.isEmpty()?0:1),"Technology description appears once or is intentionally empty "+id);
            if(!description.isEmpty())require(rows.get(0).getString().equals(description),"Exact installed technology description "+id);
        }
    }
    private static int verifyTechnologyNames(boolean chinese) {
        int count=0;
        verifySourceTechnology("ink_bottle","gt.multiitem.bottles.32000",chinese);count++;
        verifySourceTechnology("bottled_indigo_dye","gt.multiitem.bottles.32001",chinese);count++;
        for(int tier=1;tier<=4;tier++)for(var form:Map.of("stick",32000,"cable",32010,"hdd",32020).entrySet()) {
            verifySourceTechnology("usb"+tier+"_"+form.getKey(),"gt.multiitem.technological."+(form.getValue()+tier),chinese);count++;
        }
        String[] gems={"diamond","ruby","emerald","sapphire"};
        for(int gem=0;gem<gems.length;gem++)for(var form:Map.of("circuit",30401,"processor",30501).entrySet()) {
            verifySourceTechnology("crystal_"+form.getKey()+"_"+gems[gem],"gt.multiitem.technological."+(form.getValue()+gem),chinese);count++;
        }
        String[] molds={"empty","bun","bread","baguette","cylinder","toast"};
        for(int mold=0;mold<molds.length;mold++) {
            verifySourceTechnology("foodmold_shape_"+molds[mold],"gt.multiitem.technological."+(10800+mold),chinese);count++;
        }
        verifySourceTechnology("slicer_shape_eights_hollow","gt.multiitem.technological.10904",chinese);count++;
        for(var dye:com.gregtech.gregtech.content.tool.PaintingRules.DYES)for(boolean used:new boolean[]{false,true}) {
            verifySourceTechnology(used?dye.usedId():dye.fullId(),"gt.multiitem.randomtools."+(1000+2*dye.index()+(used?1:0)),chinese);count++;
        }
        for(int configuration=0;configuration<25;configuration++) {
            String id="integrated_circuit_"+configuration;verifySourceTechnology(id,"gt.integrated_circuit",chinese);
            var stack=item(id);String expected=original("gt.integrated_circuit.configuration")+"== "+configuration;
            require(tooltip(stack).stream().filter(c -> c.getString().equals(expected)).count()==1,"Selector configuration appears once "+id);
            require(stack.getItem() instanceof com.gregtech.gregtech.item.SelectorTagItem tag && tag.isCatalyst(),"Selector keeps recipe catalyst identity");
            count++;
        }
        require(count==86,"Complete source-bound technology and dye bottle catalog");return count;
    }
    private static int verifyCoins(boolean chinese) {
        String expected=original("item.gregtech.coin");int count=0;
        if(chinese)require(expected.equals(original("gt.multitileentity.32700")),"Original coin family name");
        for(var registered:BuiltInRegistries.ITEM)if(registered instanceof com.gregtech.gregtech.item.CoinItem coin) {
            var stack=new ItemStack(registered);
            require(stack.getHoverName().getString().equals(expected),"Original coin name "+BuiltInRegistries.ITEM.getKey(registered));
            String material=com.gregtech.gregtech.api.material.MaterialPresentation.name(coin.getMaterial()).getString();
            require(tooltip(stack).stream().filter(c -> c.getString().equals(material)).count()==1,"Coin material shown exactly once");
            count++;
        }
        require(count>500,"Complete material coin catalog");
        return count;
    }

    private static int verifyOriginalEnglishForms() {
        // Independent examples from LanguageHandler.getLocalName and OP, through installed item callbacks.
        var day = java.time.LocalDate.now();
        boolean april = day.getMonthValue() == 4 && day.getDayOfMonth() <= 2;
        var expected = Map.ofEntries(
                Map.entry("oredict.dustWheat", "Flour"), Map.entry("oredict.dustOat", "Oatmeal"),
                Map.entry("oredict.dustCorn", "Cornmeal"), Map.entry("oredict.rockGtStone", "Rock"),
                Map.entry("oredict.rockGtMeteoricIron", "Meteorite"),
                Map.entry("oredict.plateGemDiamond", "Crystalline Diamond Plate"), Map.entry("oredict.gemDiamond", "Diamond"),
                Map.entry("oredict.gemChippedIce", "Ice Cubes"), Map.entry("oredict.plateTinyPaper", "Tiny piece of Paper"),
                Map.entry("oredict.plateDoublePaper", "Paperboard"), Map.entry("oredict.plateTriplePaper", "Carton"),
                Map.entry("oredict.plateQuadruplePaper", "Cardboard"), Map.entry("oredict.plateQuintuplePaper", "Thick Cardboard"),
                Map.entry("oredict.stickWoodTreated", "Treated Stick"),
                Map.entry("oredict.bulletGtSmallEmpty", april ? "Small Bolt Shaft" : "Small Bullet Casing"),
                Map.entry("oredict.arrowGtPlasticEmpty", "Headless Plastic Arrow"),
                Map.entry("oredict.oreRawGold", "Raw Native Gold Ore"), Map.entry("oredict.dustBone", "Bonemeal"));
        var found = new java.util.HashSet<String>();
        for (var registered : BuiltInRegistries.ITEM) {
            if (!BuiltInRegistries.ITEM.getKey(registered).getNamespace().equals("gregtech")) continue;
            var stack = new ItemStack(registered);
            String source = materialSourceKey(stack);
            if (source == null || !expected.containsKey(source)) continue;
            require(stack.getHoverName().getString().equals(expected.get(source)),
                    "Installed original English form " + source + ": " + stack.getHoverName().getString());
            found.add(source);
        }
        require(found.equals(expected.keySet()), "All original English examples are registered: " + found);
        return found.size();
    }

    private static String materialSourceKey(ItemStack stack) {
        if(stack.getItem() instanceof com.gregtech.gregtech.item.CoinItem)return "gt.multitileentity.32700";
        if(stack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem form)
            return com.gregtech.gregtech.api.prefix.PrefixRegistry.sourceTranslationKey(form.getPrefix().getName(),form.getMaterial().getName());
        if(stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof com.gregtech.gregtech.block.MaterialBlockLike form)
            return com.gregtech.gregtech.api.prefix.PrefixRegistry.sourceTranslationKey(form.prefix().getName(),form.material().getName());
        return null;
    }
    private static JsonObject recordNameInventory(Minecraft client, Map<String,String> englishNames) {
        var candidates=new JsonArray();var missingForms=new JsonArray();int checked=0,sourceForms=0,matchedForms=0,sourceCasings=0;
        for(var registered:BuiltInRegistries.ITEM) {
            var id=BuiltInRegistries.ITEM.getKey(registered);
            if(!id.getNamespace().equals("gregtech"))continue;
            var stack=new ItemStack(registered);
            String name=stack.getHoverName().getString();
            require(!name.isBlank()&&!UNRESOLVED_NAME.matcher(name).find(),"Unresolved installed Chinese name "+id+" = "+name);
            require(englishNames.containsKey(id.toString()),"Same registered items in both locales "+id);
            var entry=new JsonObject();entry.addProperty("id",id.toString());
            entry.addProperty("descriptionKey",stack.getDescriptionId());
            entry.addProperty("english",englishNames.get(id.toString()));entry.addProperty("chinese",name);
            entry.addProperty("unchanged",name.equals(englishNames.get(id.toString())));
            String source=materialSourceKey(stack);
            if(source!=null) {
                sourceForms++;
                boolean available=Language.getInstance().has(source);
                entry.addProperty("sourceKey",source);entry.addProperty("sourceNameAvailable",available);
                if(available) {
                    require(name.equals(original(source)),"Existing original material name bypassed: "+id+" / "+source+" / "+name);
                    matchedForms++;
                    if(source.startsWith("oredict.casingSmall"))sourceCasings++;
                } else missingForms.add(entry);
            }
            if(LATIN_WORD.matcher(name).find())candidates.add(entry);
            checked++;
        }
        require(checked==englishNames.size(),"Complete registered Chinese name scan");
        require(sourceForms>10000&&matchedForms>10000&&sourceCasings>100,"Complete original material form name scan");
        var forms=new JsonObject();forms.addProperty("registered",sourceForms);forms.addProperty("exactOriginal",matchedForms);
        forms.addProperty("missingOriginal",missingForms.size());forms.addProperty("originalCasingNames",sourceCasings);
        var inventory=new JsonObject();inventory.addProperty("registeredItems",checked);
        inventory.addProperty("candidateCount",candidates.size());inventory.add("candidates",candidates);
        inventory.add("materialFormNames",forms);inventory.add("missingMaterialFormNames",missingForms);
        inventory.addProperty("scope","Actual default item names containing Latin words under zh_cn; candidates include legitimate source names/acronyms, not confirmed missing translations. Material forms separately verify every available whole-name source key. No player inventory or world data.");
        try {
            java.nio.file.Files.writeString(client.gameDirectory.toPath().resolve("language-names.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(inventory),java.nio.charset.StandardCharsets.UTF_8);
        } catch(java.io.IOException error) {throw new IllegalStateException("Could not write installed language inventory",error);}
        return inventory;
    }
    private static int verifySourceStones(boolean chinese) {
        // Loader_Rocks registers these 17 families. Port-only rocks have no original block phrases.
        var sources=java.util.Set.of("granite.black","granite.red","basalt","marble","limestone",
                "granite","diorite","andesite","komatiite","greenschist","blueschist",
                "kimberlite","quartzite","prismarine.light","prismarine.dark","slate","shale");
        int count=0;
        for(var stone:com.gregtech.gregtech.block.stone.StoneType.values()) {
            if(!sources.contains(stone.textureFolder().substring("gt.stone.".length())))continue;
            for(var variant:com.gregtech.gregtech.block.stone.StoneVariant.values())for(boolean slab:new boolean[]{false,true}) {
                String id=stone.registryId()+"_"+variant.registrySuffix()+(slab?"_slab":"");
                var stack=item(id);
                String expected=original("block.gregtech."+id);
                if(chinese)require(expected.equals(original(stone.textureFolder()+(slab?".slab.0.":".")+variant.meta())),
                        "Exact original stone phrase "+id);
                require(stack.getHoverName().getString().equals(expected),"Installed stone item name "+id);
                require(((BlockItem)stack.getItem()).getBlock().getName().getString().equals(expected),"Installed stone block name "+id);
                count++;
            }
        }
        require(count==544,"Complete original full/slab stone catalog");
        return count;
    }
    private static int verifyBushes(boolean chinese) {
        String expected=original("block.gregtech.bush");
        if(chinese)require(expected.equals(original("gt.multitileentity.32759")),"Original bush family name");
        var blocks=com.gregtech.gregtech.registry.GTBushes.allBlocks();
        require(blocks.length==10+com.gregtech.gregtech.content.plant.MaterialBerryBushCatalog.variants().size(),
                "Complete unplanted/food/cotton/material bush catalog");
        for(var block:blocks) {
            var bush=(com.gregtech.gregtech.block.plant.BushBlock)block;
            var stack=new ItemStack(block);
            require(stack.getHoverName().getString().equals(expected)&&block.getName().getString().equals(expected),
                    "Bush keeps original family name "+bush.berryId());
            var key=com.gregtech.gregtech.content.plant.GTBerryBushes.itemId(bush.berryId());
            String output;
            if(key==null)output=original("tooltip.gregtech.bush.set_output");
            else {
                var berry=new ItemStack(BuiltInRegistries.ITEM.get(key));
                require(!berry.isEmpty(),"Bush output item exists "+key);
                output=berry.getHoverName().getString();
            }
            require(tooltip(stack).stream().filter(c -> c.getString().equals(output)).count()==1,
                    "Bush shows its localized output exactly once "+bush.berryId());
        }
        return blocks.length;
    }
    private static int verifyMachineNames() {
        int checked=0;
        var mismatches=new ArrayList<String>();
        for(var registered:BuiltInRegistries.ITEM) {
            if(!(registered instanceof com.gregtech.gregtech.block.machine.ItemPipeBlockItem)
                    && !(registered instanceof com.gregtech.gregtech.content.transport.fluid.FluidTransportBlockItem)
                    && !(registered instanceof com.gregtech.gregtech.item.BookShelfBlockItem))continue;
            var stack=new ItemStack(registered);
            String key=stack.getDescriptionId(),actual=stack.getHoverName().getString();
            if(!Language.getInstance().has(key)||!actual.equals(Component.translatable(key).getString()))
                mismatches.add(BuiltInRegistries.ITEM.getKey(registered)+": "+actual+" != "+key);
            checked++;
        }
        require(checked>=400&&mismatches.isEmpty(),"Registered machine names must use current locale: "+mismatches+"; checked="+checked);
        return checked;
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
