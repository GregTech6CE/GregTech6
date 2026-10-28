package gregtech.content.data;

import gregtech.api.material.GTValues;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.content.block.MaterialBlockLike;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.fluid.FluidDisplayItem;import net.minecraft.network.chat.Component;
*///?} else {
import gregtech.content.item.FluidItem;import net.minecraft.network.chat.Component;
//?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
//?}
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Registry-driven built-in datapack: avoids thousands of tiny processResources files. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
//?}
public final class MaterialTagPack extends AbstractPackResources {
    private Map<ResourceLocation,byte[]> resources;
//? if >=1.21.1 {
    /*public MaterialTagPack(PackLocationInfo info) { super(info); }
*///?} else {
    public MaterialTagPack(String id) { super(id,true); }
//?}
    @SubscribeEvent public static void packs(AddPackFindersEvent event) {
        if(event.getPackType()!=PackType.SERVER_DATA) return;
        event.addRepositorySource(output->{
//? if >=1.21.1 {
            /*var location=new PackLocationInfo("gt6ce:material_tags",Component.literal("GregTech material tags"),PackSource.BUILT_IN,Optional.empty());
            var pack=Pack.readMetaAndCreate(location,new Pack.ResourcesSupplier(){
                public PackResources openPrimary(PackLocationInfo info){return new MaterialTagPack(info);}
                public PackResources openFull(PackLocationInfo info,Pack.Metadata metadata){return new MaterialTagPack(info);}
            },PackType.SERVER_DATA,new PackSelectionConfig(true,Pack.Position.BOTTOM,true));
*///?} else {
            var pack=Pack.readMetaAndCreate("gt6ce:material_tags",Component.literal("GregTech material tags"),true,
                    MaterialTagPack::new,PackType.SERVER_DATA,Pack.Position.BOTTOM,PackSource.BUILT_IN);
//?}
            if(pack!=null) output.accept(pack);
        });
    }
    private synchronized Map<ResourceLocation,byte[]> data() {
        if(resources!=null) return resources;
        var tags=new TreeMap<String,Set<String>>();
        var copperMaterials=MaterialGroups.Cu.getReRegistrations().stream().map(m->m.resolve()).collect(java.util.stream.Collectors.toSet());
        var ironMaterials=MaterialGroups.Iron.getReRegistrations().stream().map(m->m.resolve()).collect(java.util.stream.Collectors.toSet());
        // Per-material tag: every form of one material (gear, rod, ingot, dust, block, fluid, unit...)
        // shares "gt6ce:material/<material>", which is what recipe filtering and pack scripts want.
        // Per-form tags drop the "items" directory from their name: the tag is

// Per-form tags drop the "item" directory from their name: the tag is
        // "gt6ce:<prefix registry name>/<material>" (e.g. gt6ce:ingot/iron).
        // There is deliberately NO umbrella "gt6ce:material" tag that lists all of them: resolving it
        // enumerates every material item in the mod, which costs JEI several seconds on each open.
//? if >=1.21.1 {
        /*add(tags,"gt6ce","item","ammunition/sticks_wood","minecraft:stick");
*///?} else {
        add(tags,"gt6ce","items","ammunition/sticks_wood","minecraft:stick");
//?}
        // Vanilla sticks are the source Wood stick even before material-link bootstrap finishes.
        add(tags,"gt6ce","items","molds/forms/stick/anywoodplastic","minecraft:stick");
        add(tags,"forge","items","clay_balls","minecraft:clay_ball");
        add(tags,"gt6ce","items","molds/forms/ingot/brick","minecraft:brick");
        add(tags,"gt6ce","items","ingot/brick","minecraft:brick");
        add(tags,"forge","items","chests/wooden","minecraft:chest");
        add(tags,"forge","items","chests/wooden","minecraft:trapped_chest");
        add(tags,"forge","items","slimeballs","minecraft:slime_ball");
        for (String sandstone : java.util.List.of("sandstone","cut_sandstone","smooth_sandstone","red_sandstone","cut_red_sandstone","smooth_red_sandstone"))
            add(tags,"forge","items","sandstone","minecraft:"+sandstone);
        var moldForms=gregtech.content.recipe.ClayMoldCatalog.FORMS.stream()
                .collect(java.util.stream.Collectors.groupingBy(f -> f.prefix().equals("casingSmall") ? "itemCasing" : f.prefix()));
        var byMaterial=new TreeMap<String,Set<String>>();
        var blocksByMaterial=new TreeMap<String,Set<String>>();
//? if >=1.21.1 {
        /*for(var item:BuiltInRegistries.ITEM) {
            var id=BuiltInRegistries.ITEM.getKey(item); if(id==null||item==Items.AIR)continue;
*///?} else {
        for(var item:ForgeRegistries.ITEMS.getValues()) {
            var id=ForgeRegistries.ITEMS.getKey(item); if(id==null||item==Items.AIR)continue;
//?}
            if(item instanceof BlockItem stoneItem
                    && stoneItem.getBlock() instanceof gregtech.content.block.stone.GTStoneBlock stone
                    && stone.variant() == gregtech.content.block.stone.StoneVariant.COBBLE)
                add(tags,"minecraft","items","stone_crafting_materials",id.toString());
            if(item instanceof BlockItem pane && (pane.getBlock() instanceof net.minecraft.world.level.block.StainedGlassPaneBlock
                    || id.toString().equals("minecraft:glass_pane")))
                add(tags,"forge","items","glass_panes",id.toString());
            if (item instanceof DyeItem dye) add(tags,"forge","items","dyes/"+dye.getDyeColor().getName(),id.toString());
            if (id.getNamespace().equals("minecraft") && id.getPath().startsWith("music_disc_"))
                add(tags,"minecraft","items","music_discs",id.toString());
            var form=registeredForm(item);
            if(form!=null) {
                for(var moldForm:moldForms.getOrDefault(form.prefix().getName(),java.util.List.of()))
                    if(moldForm.accepts(form.prefix(),form.material()))
                        add(tags,"gt6ce","items",moldForm.tag(),id.toString());
                for(var family:gregtech.content.recipe.TechnologyIngredients.FAMILIES)
                    if(family.accepts(form.prefix(),form.material()))
//? if >=1.21.1 {
                        /*add(tags,"gt6ce","item","technology/"+family.tag(),id.toString());
*///?} else {
                        add(tags,"gt6ce","items","technology/"+family.tag(),id.toString());
//?}
                if(form.prefix()==MaterialPrefix.stick || form.prefix()==MaterialPrefix.plateTiny) {
                    var material=form.material().resolve();
                    boolean wood=gregtech.content.tool.OriginalToolMaterials.inFamily(material,"Wood")
                            || java.util.stream.Stream.of(MaterialGroups.Wood, MaterialGroups.WoodNormal,
                                    MaterialGroups.WoodMagical, MaterialGroups.WoodTreated)
                            .flatMap(group -> group.getReRegistrations().stream()).anyMatch(member -> member.resolve() == material);
                    boolean plastic=gregtech.content.tool.OriginalToolMaterials.inFamily(material,"Plastic");
                    if(form.prefix()==MaterialPrefix.stick && wood)
//? if >=1.21.1 {
                        /*add(tags,"gt6ce","item","ammunition/sticks_wood",id.toString());
*///?} else {
                        add(tags,"gt6ce","items","ammunition/sticks_wood",id.toString());
//?}
                    // One-way convenience conversion; vanilla sticks must not match their own recipe.
                    if(form.prefix()==MaterialPrefix.stick && wood && item != Items.STICK)
                        add(tags,"gt6ce","items","convertible_wooden_rods",id.toString());
                    if(plastic)
//? if >=1.21.1 {
                        /*add(tags,"gt6ce","item",form.prefix()==MaterialPrefix.stick
*///?} else {
                        add(tags,"gt6ce","items",form.prefix()==MaterialPrefix.stick
//?}
                                ? "ammunition/sticks_plastic" : "ammunition/tiny_plastic_plates",id.toString());
                }

                if(gregtech.content.recipe.MaterialArrowRules.isArrow(form.prefix(),form.material()))
//? if >=1.21.1 {
                    /*add(tags,"minecraft","item","arrows",id.toString());
*///?} else {
                    add(tags,"minecraft","items","arrows",id.toString());
//?}
                String common=MaterialEquivalence.tagPath(form);
//? if >=1.21.1 {
                /*if(common!=null) add(tags,"forge","item",common,id.toString());
*///?} else {
                if(common!=null) add(tags,"forge","items",common,id.toString());
//?}
                addMaterial(byMaterial,form.material(),id.toString());
                // The canonical WoodTreated plate is a placeable BlockItem. Material
                // forms backed by blocks still need the same GT prefix tag as an item.
//? if >=1.21.1 {
                /*add(tags,"gt6ce","item",form.prefix().getRegistryName()+"/"
*///?} else {
                add(tags,"gt6ce","items",form.prefix().getRegistryName()+"/"
//?}
                        +MaterialEquivalence.materialName(form.material()),id.toString());
                String sourcePrefix = gregtech.engine.prefix.PrefixRegistry.sourceName(form.prefix().getName());
                if (!sourcePrefix.equals(form.prefix().getName()))
                    add(tags,"gt6ce","items",MaterialPrefix.camelToSnake(sourcePrefix)+"/"
                            +MaterialEquivalence.materialName(form.material()),id.toString());
            } else {
                // Vanilla-unified items (minecraft:iron_block, ...) carry a material without a prefix.
                var data=gregtech.engine.material.ItemMaterialRegistry.explicit(item).orElse(null);
                if(data!=null&&data.components().size()==1) {
                    var material=data.components().get(0).material().resolve();
                    if(material.isValid()) {
                        addMaterial(byMaterial,material,id.toString());
                        String name=MaterialEquivalence.materialName(material);
                        // Vanilla block shapes are explicit; a 2U axle/log is not an ore.
                        if (item instanceof BlockItem && id.getNamespace().equals("minecraft")) {
                            if (java.util.Set.of("iron_block","gold_block","copper_block","netherite_block","diamond_block","emerald_block","lapis_block","coal_block","redstone_block").contains(id.getPath()))
                                add(tags,"forge","items","storage_blocks/"+name,id.toString());
                            else if (id.getPath().endsWith("_ore") || id.getPath().equals("ancient_debris"))
                                add(tags,"forge","items","ores/"+name,id.toString());
                        }
                    }
                }
            }
            if(id.getNamespace().equals("gt6ce"))
                for(String tag:gregtech.content.recipe.TechnologyIngredients.usbTags(id.getPath()))
//? if >=1.21.1 {
                    /*add(tags,"gt6ce","item",tag,id.toString());
*///?} else {
                    add(tags,"gt6ce","items",tag,id.toString());
//?}
            for(var tool:gregtech.content.tool.CraftingToolDefinitions.ALL)
                if(id.getNamespace().equals("gt6ce") && (tool.tip().equals(id.getPath()) || tool.token().equals(id.getPath())))
//? if >=1.21.1 {
                    /*for(String toolKind:tool.kinds()) add(tags,"forge","item","tools/"+toolKind,id.toString());
*///?} else {
                    for(String toolKind:tool.kinds()) add(tags,"forge","items","tools/"+toolKind,id.toString());
//?}
            // GT tools: standard Forge convention, e.g. forge:tools/wrench, plus the extra GT6
            // identities of the same tool (a Knife is also a blade, a Monkey Wrench also a wrench).
// Registry tags describe tool identities, before any stack receives material/durability data.
            if(item instanceof gregtech.content.item.GTToolItem tool)
                for(String toolKind:gregtech.content.tool.ToolTagIdentities.all(tool.toolType().id()))
//? if >=1.21.1 {
                    /*add(tags,"forge","item","tools/"+toolKind,id.toString());
*///?} else {
                    add(tags,"forge","items","tools/"+toolKind,id.toString());
//?}
            // GT6 fluid containers: one item per fluid, so a material's fluid forms belong to that
            // material's tag (RegisteredFluids knows which material a fluid was created from).
//? if >=1.21.1 {
            /*if(item instanceof FluidDisplayItem fluidItem) {
*///?} else {
            if(item instanceof FluidItem fluidItem) {
//?}
                // GT6's fluids know their material: either as the declaration's materialKey (the molten
                // and generated entries) or through the side binding created when a generated gas/liquid
                // reuses a dedicated fluid (RegisteredFluids.BOUND_MATERIALS).
                String bound=fluidItem.fluidEntry().materialKey();
                if(bound==null) bound=RegisteredFluids.boundMaterial(fluidItem.fluidEntry().registryName());
                if(bound!=null) {
                    var material=gregtech.engine.material.GTMaterialRegistry.get(bound);
                    if(material!=null&&material.isValid()) {
                        String name=MaterialEquivalence.materialName(material);
//? if >=1.21.1 {
                        /*add(tags,"gt6ce","item","fluid/"+name,id.toString());
*///?} else {
                        add(tags,"gt6ce","items","fluid/"+name,id.toString());
//?}
                        addMaterial(byMaterial,material,id.toString());
                    }
                }
                continue;
//? if >=1.21.1 {
            /*}
            if(item instanceof BlockItem b && b.getBlock() instanceof gregtech.content.block.BlackSandBlock sand) {
                String name=MaterialEquivalence.materialName(sand.material());
                for(String kind:List.of("item","block")) {
                    add(tags,"gt6ce",kind,"block_dust/"+name,id.toString());
                    add(tags,"forge",kind,"storage_blocks/"+name,id.toString());
                }
                addMaterial(blocksByMaterial,sand.material(),id.toString());
*///?} else {
//?}
            }
            if(item instanceof BlockItem b && b.getBlock() instanceof MaterialBlockLike m) {
                String name=MaterialEquivalence.materialName(m.material());
                String group=switch(m.prefix().getName()) {
                    case "blockIngot","blockGem","blockDust" -> "storage_blocks";
                    case "blockRaw" -> "storage_blocks/raw";
                    case "ore" -> "ores";
                    default -> null;
                };
//? if >=1.21.1 {
                /*for(String kind:List.of("item","block")) {
*///?} else {
                for(String kind:List.of("items","blocks")) {
//?}
                    add(tags,"gt6ce",kind,m.prefix().getRegistryName()+"/"+name,id.toString());
                    if(group!=null) add(tags,"forge",kind,group.equals("storage_blocks/raw")?"storage_blocks/raw_"+name:group+"/"+name,id.toString());
                    if(!m.prefix().oreDictionary().isEmpty())
                        add(tags,"forge",kind,m.prefix().oreDictionary()+"/"+name,id.toString());
                }
                if(ironMaterials.contains(m.material().resolve()))
//? if >=1.21.1 {
                    /*add(tags,"gt6ce","item",m.prefix().getRegistryName()+"/any_iron_or_steel",id.toString());
*///?} else {
                    add(tags,"gt6ce","items",m.prefix().getRegistryName()+"/any_iron_or_steel",id.toString());
//?}
                addMaterial(blocksByMaterial,m.material(),id.toString());
            }

//? if >=1.21.1 {
//?} else {
            if(item instanceof BlockItem b && b.getBlock() instanceof gregtech.content.block.BlackSandBlock sand) {
                String name=MaterialEquivalence.materialName(sand.material());
                for(String kind:List.of("items","blocks")) {
                    add(tags,"gt6ce",kind,"block_dust/"+name,id.toString());
                    add(tags,"forge",kind,"storage_blocks/"+name,id.toString());
                }
                addMaterial(blocksByMaterial,sand.material(),id.toString());
            }
//?}
            if(item instanceof BlockItem b) {
                var block=b.getBlock();
                if(block instanceof gregtech.content.block.machine.FluidPipeBlock pipe
                        && ironMaterials.contains(pipe.spec().material().resolve()))
//? if >=1.21.1 {
                    /*add(tags,"gt6ce","item","pipe_"+pipe.spec().size().name().toLowerCase(java.util.Locale.ROOT)+"/any_iron_or_steel",id.toString());
*///?} else {
                    add(tags,"gt6ce","items","pipe_"+pipe.spec().size().name().toLowerCase(java.util.Locale.ROOT)+"/any_iron_or_steel",id.toString());
//?}
                if(block instanceof gregtech.content.block.machine.ItemPipeBlock pipe
                        && ironMaterials.contains(pipe.spec().material().resolve()) && !pipe.spec().size().restrictive())
//? if >=1.21.1 {
                    /*add(tags,"gt6ce","item","pipe_"+pipe.spec().size().name().toLowerCase(java.util.Locale.ROOT)+"/any_iron_or_steel",id.toString());
                if(block instanceof gregtech.engine.energy.WireMaterialLike wire) {
*///?} else {
                    add(tags,"gt6ce","items","pipe_"+pipe.spec().size().name().toLowerCase(java.util.Locale.ROOT)+"/any_iron_or_steel",id.toString());
                if(block instanceof gregtech.content.block.energy.ElectricWireBlock wire) {
//?}
                    String wireForm=(wire.spec().insulated()?"cable_":"wire_")+String.format(java.util.Locale.ROOT,"%02d",wire.spec().size());
                    String wireMaterial=MaterialEquivalence.materialName(wire.spec().material());
                    add(tags,"gt6ce","items",wireForm+"/"+wireMaterial,id.toString());
                    String sourceForm=(wire.spec().insulated()?"cable_gt":"wire_gt")+String.format(java.util.Locale.ROOT,"%02d",wire.spec().size());
                    add(tags,"gt6ce","items",sourceForm+"/"+wireMaterial,id.toString());
                    if(copperMaterials.contains(wire.spec().material().resolve()))
//? if >=1.21.1 {
                        /*add(tags,"gt6ce","item",wireForm+"/any_copper",id.toString());
*///?} else {
                        add(tags,"gt6ce","items",wireForm+"/any_copper",id.toString());
//?}
                    if(ironMaterials.contains(wire.spec().material().resolve()))
//? if >=1.21.1 {
                        /*add(tags,"gt6ce","item",wireForm+"/any_iron_or_steel",id.toString());
*///?} else {
                        add(tags,"gt6ce","items",wireForm+"/any_iron_or_steel",id.toString());
//?}
                }
                if(block instanceof gregtech.content.block.DenseOreBlock denseOre
                        && denseOre.material().isValid()) {
                    String name=MaterialEquivalence.materialName(denseOre.material());
                    // The existing block item is the GT6 oreDense form, not a new material item.
//? if >=1.21.1 {
                    /*for(String kind:List.of("item","block")) {
*///?} else {
                    for(String kind:List.of("items","blocks")) {
//?}
                        add(tags,"gt6ce",kind,"ore_dense/"+name,id.toString());
                        add(tags,"forge",kind,"ores/"+name,id.toString());
                    }
                    addMaterial(blocksByMaterial,denseOre.material(),id.toString());
                }
                if(block instanceof gregtech.content.block.VanillaOreBlock ore
                        && ore.material().isValid()) {
                    String name=MaterialEquivalence.materialName(ore.material());
//? if >=1.21.1 {
                    /*for(String kind:List.of("item","block")) {
*///?} else {
                    for(String kind:List.of("items","blocks")) {
//?}
                        add(tags,"gt6ce",kind,"ore_vanillastone/"+name,id.toString());
                        add(tags,"forge",kind,"ores/"+name,id.toString());
                    }
                    addMaterial(blocksByMaterial,ore.material(),id.toString());
                }
            }
        }
        // GT6 ANY.Iron includes ten iron/steel materials, not just vanilla iron.
//? if >=1.21.1 {
        /*var ironScrews = tags.computeIfAbsent("gt6ce:tags/item/screws/any_iron_or_steel.json", k -> new TreeSet<>());
*///?} else {
        var ironScrews = tags.computeIfAbsent("gt6ce:tags/items/screws/any_iron_or_steel.json", k -> new TreeSet<>());
//?}
        for (var material : MaterialGroups.Iron.getReRegistrations()) {
            String path = "screws/" + MaterialEquivalence.materialName(material.resolve());
//? if >=1.21.1 {
            /*if (tags.containsKey("forge:tags/item/" + path + ".json")) ironScrews.add("#forge:" + path);
*///?} else {
            if (tags.containsKey("forge:tags/items/" + path + ".json")) ironScrews.add("#forge:" + path);
//?}
        }
//? if >=1.21.1 {
        /*var ironDoublePlates=tags.computeIfAbsent("gt6ce:tags/item/plate_double/any_iron_or_steel.json",k->new TreeSet<>());
*///?} else {
        var ironDoublePlates=tags.computeIfAbsent("gt6ce:tags/items/plate_double/any_iron_or_steel.json",k->new TreeSet<>());
//?}
        for(var material:MaterialGroups.Iron.getReRegistrations()) {
            String path="plate_double/"+MaterialEquivalence.materialName(material.resolve());
//? if >=1.21.1 {
            /*if(tags.containsKey("gt6ce:tags/item/"+path+".json"))ironDoublePlates.add("#gt6ce:"+path);
*///?} else {
            if(tags.containsKey("gt6ce:tags/items/"+path+".json"))ironDoublePlates.add("#gt6ce:"+path);
//?}
        }
        byMaterial.forEach((name,values)->{
//? if >=1.21.1 {
            /*var tag=tags.computeIfAbsent("gt6ce:tags/item/material/"+name+".json",k->new TreeSet<>());
*///?} else {
            var tag=tags.computeIfAbsent("gt6ce:tags/items/material/"+name+".json",k->new TreeSet<>());
//?}
            tag.addAll(values);
            tag.addAll(blocksByMaterial.getOrDefault(name,Set.of()));
        });
        // Wood form tags for the wooden-pipe recipes (GT6's OD.plankAnyWood / OD.beamWood): vanilla
        // planks and slabs join through nested tag references, the port's own species are listed.
        // The beam identity is the modern stripped log, so vanilla stripped logs join it as well.
//? if >=1.21.1 {
        /*tags.computeIfAbsent("gt6ce:tags/item/wooden_planks.json", k -> new TreeSet<>())
*///?} else {
        tags.computeIfAbsent("gt6ce:tags/items/wooden_planks.json", k -> new TreeSet<>())
//?}
                .add("#minecraft:planks");
//? if >=1.21.1 {
        /*tags.computeIfAbsent("gt6ce:tags/item/wooden_slabs.json", k -> new TreeSet<>())
*///?} else {
        tags.computeIfAbsent("gt6ce:tags/items/wooden_slabs.json", k -> new TreeSet<>())
//?}
                .add("#minecraft:wooden_slabs");
//? if >=1.21.1 {
        /*for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
*///?} else {
        for (var item : ForgeRegistries.ITEMS.getValues()) {
            var id = ForgeRegistries.ITEMS.getKey(item);
//?}
            if (id == null || !id.getNamespace().equals("gt6ce")) continue;
//? if >=1.21.1 {
            /*if(item instanceof net.minecraft.world.item.BlockItem blockItem
                    && blockItem.getBlock() instanceof gregtech.content.block.wood.WoodPlanksBlock) {
                tags.computeIfAbsent("gt6ce:tags/item/wooden_planks.json",key->new TreeSet<>()).add(id.toString());
                tags.computeIfAbsent("minecraft:tags/item/planks.json",key->new TreeSet<>()).add(id.toString());
                tags.computeIfAbsent("minecraft:tags/block/planks.json",key->new TreeSet<>()).add(id.toString());
            }
            if(item instanceof net.minecraft.world.item.BlockItem blockItem
                    && blockItem.getBlock() instanceof gregtech.content.block.wood.WoodStrippedLogBlock)
                tags.computeIfAbsent("gt6ce:tags/item/stripped_logs.json",key->new TreeSet<>()).add(id.toString());
*///?} else {
            if (item instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof gregtech.content.block.wood.WoodPlanksBlock) {
                tags.computeIfAbsent("gt6ce:tags/items/wooden_planks.json", k -> new TreeSet<>())
                        .add(id.toString());
                tags.computeIfAbsent("minecraft:tags/items/planks.json", k -> new TreeSet<>()).add(id.toString());
                tags.computeIfAbsent("minecraft:tags/blocks/planks.json", k -> new TreeSet<>()).add(id.toString());
            }
            if (item instanceof BlockItem strippedItem
                    && strippedItem.getBlock() instanceof gregtech.content.block.wood.WoodStrippedLogBlock) {
                tags.computeIfAbsent("gt6ce:tags/items/stripped_logs.json", k -> new TreeSet<>())
                        .add(id.toString());
            }
//?}
        }
        // GT6 mapped modern stripped logs to beamWood (LoaderItemList:1457-1468); include the vanilla
        // log/wood pairs the port's wood list covers.
//? if >=1.21.1 {
        /*var strippedLogs = tags.computeIfAbsent("gt6ce:tags/item/stripped_logs.json", k -> new TreeSet<>());
*///?} else {
        var strippedLogs = tags.computeIfAbsent("gt6ce:tags/items/stripped_logs.json", k -> new TreeSet<>());
//?}
        for (String wood : new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"}) {
            strippedLogs.add("minecraft:stripped_" + wood + "_log");
            strippedLogs.add("minecraft:stripped_" + wood + "_wood");
        }
        for (String wood : new String[]{"crimson", "warped"}) {
            strippedLogs.add("minecraft:stripped_" + wood + "_stem");
            strippedLogs.add("minecraft:stripped_" + wood + "_hyphae");
        }
        // GT logs and stripped logs are logs for axe chaining, worldgen tag checks and cross-mod use.
//? if >=1.21.1 {
        /*var logBlocks = tags.computeIfAbsent("minecraft:tags/block/logs.json", k -> new TreeSet<>());
        var logItems = tags.computeIfAbsent("minecraft:tags/item/logs.json", k -> new TreeSet<>());
*///?} else {
        var logBlocks = tags.computeIfAbsent("minecraft:tags/blocks/logs.json", k -> new TreeSet<>());
        var logItems = tags.computeIfAbsent("minecraft:tags/items/logs.json", k -> new TreeSet<>());
//?}
        for (gregtech.content.block.wood.WoodSpecies sp : gregtech.content.block.wood.WoodSpecies.values()) {
            logBlocks.add("gt6ce:log_" + sp.id());
            logBlocks.add("gt6ce:stripped_log_" + sp.id());
            logItems.add("gt6ce:log_" + sp.id());
            logItems.add("gt6ce:stripped_log_" + sp.id());
        }
        // GT6 LIST_SLABS / slabAnyWood: native slabs participate in normal slab crafting.
        for(String wood:gregtech.content.plant.WoodSlabCatalog.WOODS){
            String id="gt6ce:slab_"+wood;
//? if >=1.21.1 {
            /*for(String tag:new String[]{"minecraft:tags/item/wooden_slabs.json","minecraft:tags/item/slabs.json",
                    "minecraft:tags/block/wooden_slabs.json","minecraft:tags/block/slabs.json"})
*///?} else {
            for(String tag:new String[]{"minecraft:tags/items/wooden_slabs.json","minecraft:tags/items/slabs.json",
                    "minecraft:tags/blocks/wooden_slabs.json","minecraft:tags/blocks/slabs.json"})
//?}
                tags.computeIfAbsent(tag,k->new TreeSet<>()).add(id);
        }
        // Common spelling aliases used by Forge 1.20 packs. References keep datapack additions shared.
        var aliases=gregtech.engine.material.MaterialTagAliases.ALTERNATE_SPELLINGS;
        for(var path:new ArrayList<>(tags.keySet())) for(var alias:aliases.entrySet()) {
            String suffix="/"+alias.getKey()+".json";
            if(path.startsWith("forge:")&&path.endsWith(suffix)) {
                String alt=path.substring(0,path.length()-suffix.length())+"/"+alias.getValue()+".json";
                // Union aliases without recursive tag references.
                var values=tags.get(path);tags.computeIfAbsent(alt,k->new TreeSet<>()).addAll(values);
            }
        }
//? if >=1.21.1 {
        /*for (var path:new ArrayList<>(tags.keySet())) {
            if(path.startsWith("forge:")) tags.computeIfAbsent("c:"+path.substring(6),k->new TreeSet<>()).addAll(tags.get(path));
        }
*///?} else {
//?}
        var gson=new com.google.gson.Gson(); var result=new HashMap<ResourceLocation,byte[]>();
//? if >=1.21.1 {
        /*tags.forEach((path,values)->result.put(ResourceLocation.parse(path),gson.toJson(Map.of("replace",false,"values",compatibleValues(path,values,aliases))).getBytes(StandardCharsets.UTF_8)));
        result.put(ResourceLocation.parse("gt6ce:tags/item/ammunition/feathers.json"),gson.toJson(Map.of(
*///?} else {
        tags.forEach((path,values)->result.put(ResourceLocation.parse(path),gson.toJson(Map.of("replace",false,"values",values)).getBytes(StandardCharsets.UTF_8)));
        result.put(ResourceLocation.parse("gt6ce:tags/items/ammunition/feathers.json"),gson.toJson(Map.of(
//?}
                "replace",false,"values",List.of("minecraft:feather",
                        Map.of("id","#forge:feathers","required",false),
                        Map.of("id","#c:feathers","required",false)))).getBytes(StandardCharsets.UTF_8));
        resources=Collections.unmodifiableMap(result); return resources;
//? if >=1.21.1 {
    /*}

    /^* Legacy recipe tags accept native common additions; c tags retain their independent values. ^/
    private static List<Object> compatibleValues(String path, Set<String> values, Map<String,String> aliases) {
        var result = new ArrayList<Object>(values);
        String prefix = path.startsWith("forge:tags/item/") ? "forge:tags/item/"
                : path.startsWith("forge:tags/block/") ? "forge:tags/block/" : null;
        if (prefix == null) return result;
        String common = path.substring(prefix.length(), path.length() - ".json".length());
        result.add(Map.of("id", "#c:" + common, "required", false));
        for (var alias : aliases.entrySet()) {
            String from = "/" + alias.getKey(), to = "/" + alias.getValue();
            if (common.endsWith(from)) {
                result.add(Map.of("id", "#c:" + common.substring(0, common.length() - from.length()) + to, "required", false));
            } else if (common.endsWith(to)) {
                result.add(Map.of("id", "#c:" + common.substring(0, common.length() - to.length()) + from, "required", false));
            }
        }
        return result;
*///?} else {
//?}
    }

    /** Owned items and explicitly registered compositions only. Tag-derived data must not re-enter this pack. */
    private static MaterialEquivalence.Form registeredForm(Item item) {
        if (item instanceof gregtech.engine.material.MaterialFormItem materialItem)
            return new MaterialEquivalence.Form(materialItem.getPrefix(), materialItem.getMaterial().resolve());
        var data = gregtech.engine.material.ItemMaterialRegistry.explicit(item).orElse(null);
        if (data == null || data.prefix() == null || data.components().size() != 1
                || data.amount() != data.prefix().getMaterialWeight()) return null;
        return new MaterialEquivalence.Form(data.prefix(), data.material().resolve());
    }

    /** Aggregate tag value list for one material (forms + its blocks). */
    private static void addMaterial(Map<String,Set<String>> target,gregtech.api.material.GTMaterial material,String value) {
        if(material==null||!material.isValid())return;
        target.computeIfAbsent(MaterialEquivalence.materialName(material),k->new TreeSet<>()).add(value);
    }
    private static void add(Map<String,Set<String>> tags,String ns,String kind,String path,String value) {
//? if >=1.21.1 {
        /*// 1.21 registries use singular resource directories. Historical callers still
        // supply plural Forge names; normalize before both leaf and parent creation.
        kind = switch (kind) { case "items" -> "item"; case "blocks" -> "block"; default -> kind; };
*///?} else {
//?}
        tags.computeIfAbsent(ns+":tags/"+kind+"/"+path+".json",k->new TreeSet<>()).add(value);
        int slash=path.indexOf('/');
        if(slash>0) tags.computeIfAbsent(ns+":tags/"+kind+"/"+path.substring(0,slash)+".json",k->new TreeSet<>()).add("#"+ns+":"+path);
    }
    public IoSupplier<InputStream> getRootResource(String... path) {
        if(path.length!=1||!path[0].equals("pack.mcmeta"))return null;
//? if >=1.21.1 {
        /*return ()->new ByteArrayInputStream("{\"pack\":{\"pack_format\":48,\"description\":\"GregTech material tags\"}}".getBytes(StandardCharsets.UTF_8));
*///?} else {
        return ()->new ByteArrayInputStream("{\"pack\":{\"pack_format\":15,\"description\":\"GregTech material tags\"}}".getBytes(StandardCharsets.UTF_8));
//?}
    }
    public IoSupplier<InputStream> getResource(PackType type,ResourceLocation id) {
        if(type!=PackType.SERVER_DATA)return null; var bytes=data().get(id);
        return bytes==null?null:()->new ByteArrayInputStream(bytes);
    }
    public void listResources(PackType type,String ns,String path,ResourceOutput out) {
        if(type==PackType.SERVER_DATA)data().forEach((id,bytes)->{if(id.getNamespace().equals(ns)&&id.getPath().startsWith(path+"/"))out.accept(id,()->new ByteArrayInputStream(bytes));});
    }
//? if >=1.21.1 {
    /*public Set<String> getNamespaces(PackType type) { return type==PackType.SERVER_DATA?Set.of("forge","c","gt6ce","minecraft"):Set.of(); }
*///?} else {
    public Set<String> getNamespaces(PackType type) { return type==PackType.SERVER_DATA?Set.of("forge","gt6ce","minecraft"):Set.of(); }
//?}
    public void close() { resources=null; }
}
