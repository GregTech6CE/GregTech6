package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.block.MaterialBlockLike;
import com.gregtech.gregtech.item.FluidItem;import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.*;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Registry-driven built-in datapack: avoids thousands of tiny processResources files. */
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class MaterialTagPack extends AbstractPackResources {
    private Map<ResourceLocation,byte[]> resources;
    public MaterialTagPack(String id) { super(id,true); }
    @SubscribeEvent public static void packs(AddPackFindersEvent event) {
        if(event.getPackType()!=PackType.SERVER_DATA) return;
        event.addRepositorySource(output->{
            var pack=Pack.readMetaAndCreate("gregtech:material_tags",Component.literal("GregTech material tags"),true,
                    MaterialTagPack::new,PackType.SERVER_DATA,Pack.Position.BOTTOM,PackSource.BUILT_IN);
            if(pack!=null) output.accept(pack);
        });
    }
    private synchronized Map<ResourceLocation,byte[]> data() {
        if(resources!=null) return resources;
        var tags=new TreeMap<String,Set<String>>();
        var copperMaterials=MaterialGroups.Cu.getReRegistrations().stream().map(m->m.resolve()).collect(java.util.stream.Collectors.toSet());
        // Per-material tag: every form of one material (gear, rod, ingot, dust, block, fluid, unit...)
        // shares "gregtech:material/<material>", which is what recipe filtering and pack scripts want.
        // Per-form tags drop the "items" directory from their name: the tag is
        // "gregtech:<prefix registry name>/<material>" (e.g. gregtech:ingot/iron).
        // There is deliberately NO umbrella "gregtech:material" tag that lists all of them: resolving it
        // enumerates every material item in the mod, which costs JEI several seconds on each open.
        add(tags,"gregtech","items","ammunition/sticks_wood","minecraft:stick");
        add(tags,"forge","items","clay_balls","minecraft:clay_ball");
        add(tags,"gregtech","items","molds/forms/ingot/brick","minecraft:brick");
        var moldForms=com.gregtech.gregtech.content.recipe.ClayMoldCatalog.FORMS.stream()
                .collect(java.util.stream.Collectors.groupingBy(f -> f.prefix().equals("casingSmall") ? "itemCasing" : f.prefix()));
        var byMaterial=new TreeMap<String,Set<String>>();
        var blocksByMaterial=new TreeMap<String,Set<String>>();
        for(var item:ForgeRegistries.ITEMS.getValues()) {
            var id=ForgeRegistries.ITEMS.getKey(item); if(id==null||item==Items.AIR)continue;
            if(item instanceof BlockItem pane && (pane.getBlock() instanceof net.minecraft.world.level.block.StainedGlassPaneBlock
                    || id.toString().equals("minecraft:glass_pane")))
                add(tags,"forge","items","glass_panes",id.toString());
            var form=MaterialEquivalence.form(new ItemStack(item));
            if(form!=null) {
                for(var moldForm:moldForms.getOrDefault(form.prefix().getName(),java.util.List.of()))
                    if(moldForm.accepts(form.prefix(),form.material()))
                        add(tags,"gregtech","items",moldForm.tag(),id.toString());
                for(var family:com.gregtech.gregtech.content.recipe.TechnologyIngredients.FAMILIES)
                    if(family.accepts(form.prefix(),form.material()))
                        add(tags,"gregtech","items","technology/"+family.tag(),id.toString());
                if(form.prefix()==MaterialPrefix.stick || form.prefix()==MaterialPrefix.plateTiny) {
                    var material=form.material().resolve();
                    boolean wood=com.gregtech.gregtech.content.tool.OriginalToolMaterials.inFamily(material,"Wood");
                    boolean plastic=com.gregtech.gregtech.content.tool.OriginalToolMaterials.inFamily(material,"Plastic");
                    if(form.prefix()==MaterialPrefix.stick && wood)
                        add(tags,"gregtech","items","ammunition/sticks_wood",id.toString());
                    if(plastic)
                        add(tags,"gregtech","items",form.prefix()==MaterialPrefix.stick
                                ? "ammunition/sticks_plastic" : "ammunition/tiny_plastic_plates",id.toString());
                }

                if(com.gregtech.gregtech.content.recipe.MaterialArrowRules.isArrow(form.prefix(),form.material()))
                    add(tags,"minecraft","items","arrows",id.toString());
                String common=MaterialEquivalence.tagPath(form);
                if(common!=null) add(tags,"forge","items",common,id.toString());
                addMaterial(byMaterial,form.material(),id.toString());
                // The canonical WoodTreated plate is a placeable BlockItem. Material
                // forms backed by blocks still need the same GT prefix tag as an item.
                add(tags,"gregtech","items",form.prefix().getRegistryName()+"/"
                        +MaterialEquivalence.materialName(form.material()),id.toString());
            } else {
                // Vanilla-unified items (minecraft:iron_block, ...) carry a material without a prefix.
                var data=com.gregtech.gregtech.api.material.ItemMaterialRegistry.base(item).orElse(null);
                if(data!=null&&data.components().size()==1) {
                    var material=data.components().get(0).material().resolve();
                    if(material.isValid()) {
                        addMaterial(byMaterial,material,id.toString());
                        String name=MaterialEquivalence.materialName(material);
                        if(data.prefix()==null&&data.components().get(0).amount()==GTValues.U*9)
                            add(tags,"forge","items","storage_blocks/"+name,id.toString());
                        else if(data.prefix()==null&&data.components().get(0).amount()==GTValues.U*2)
                            add(tags,"forge","items","ores/"+name,id.toString());
                    }
                }
            }
            if(id.getNamespace().equals("gregtech"))
                for(String tag:com.gregtech.gregtech.content.recipe.TechnologyIngredients.usbTags(id.getPath()))
                    add(tags,"gregtech","items",tag,id.toString());
            for(var tool:com.gregtech.gregtech.content.tool.CraftingToolDefinitions.ALL)
                if(id.getNamespace().equals("gregtech") && (tool.tip().equals(id.getPath()) || tool.token().equals(id.getPath())))
                    for(String toolKind:tool.kinds()) add(tags,"forge","items","tools/"+toolKind,id.toString());
            // GT tools: standard Forge convention, e.g. forge:tools/wrench.
            if(item instanceof com.gregtech.gregtech.item.GTToolItem tool)
                add(tags,"forge","items","tools/"+tool.toolType().id(),id.toString());
            // GT6 fluid containers: one item per fluid, so a material's fluid forms belong to that
            // material's tag (RegisteredFluids knows which material a fluid was created from).
            if(item instanceof FluidItem fluidItem) {
                // GT6's fluids know their material: either as the declaration's materialKey (the molten
                // and generated entries) or through the side binding created when a generated gas/liquid
                // reuses a dedicated fluid (RegisteredFluids.BOUND_MATERIALS).
                String bound=fluidItem.fluidEntry().materialKey();
                if(bound==null) bound=RegisteredFluids.boundMaterial(fluidItem.fluidEntry().registryName());
                if(bound!=null) {
                    var material=com.gregtech.gregtech.api.material.GTMaterialRegistry.get(bound);
                    if(material!=null&&material.isValid()) {
                        String name=MaterialEquivalence.materialName(material);
                        add(tags,"gregtech","items","fluid/"+name,id.toString());
                        addMaterial(byMaterial,material,id.toString());
                    }
                }
                continue;
            }
            if(item instanceof BlockItem b && b.getBlock() instanceof MaterialBlockLike m) {
                String name=MaterialEquivalence.materialName(m.material());
                String group=switch(m.prefix().getName()) {
                    case "blockIngot","blockGem","blockDust" -> "storage_blocks";
                    case "blockRaw" -> "storage_blocks/raw";
                    case "ore" -> "ores";
                    default -> null;
                };
                for(String kind:List.of("items","blocks")) {
                    add(tags,"gregtech",kind,m.prefix().getRegistryName()+"/"+name,id.toString());
                    if(group!=null) add(tags,"forge",kind,group.equals("storage_blocks/raw")?"storage_blocks/raw_"+name:group+"/"+name,id.toString());
                }
                addMaterial(blocksByMaterial,m.material(),id.toString());
            }
            if(item instanceof BlockItem b && b.getBlock() instanceof com.gregtech.gregtech.block.BlackSandBlock sand) {
                String name=MaterialEquivalence.materialName(sand.material());
                for(String kind:List.of("items","blocks")) {
                    add(tags,"gregtech",kind,"block_dust/"+name,id.toString());
                    add(tags,"forge",kind,"storage_blocks/"+name,id.toString());
                }
                addMaterial(blocksByMaterial,sand.material(),id.toString());
            }
            if(item instanceof BlockItem b) {
                var block=b.getBlock();
                if(block instanceof com.gregtech.gregtech.block.energy.ElectricWireBlock wire
                        && copperMaterials.contains(wire.spec().material().resolve())) {
                    String wireForm=(wire.spec().insulated()?"cable_":"wire_")+String.format(java.util.Locale.ROOT,"%02d",wire.spec().size());
                    add(tags,"gregtech","items",wireForm+"/any_copper",id.toString());
                }
                if(block instanceof com.gregtech.gregtech.block.DenseOreBlock denseOre
                        && denseOre.material().isValid()) {
                    String name=MaterialEquivalence.materialName(denseOre.material());
                    // The existing block item is the GT6 oreDense form, not a new material item.
                    for(String kind:List.of("items","blocks")) {
                        add(tags,"gregtech",kind,"ore_dense/"+name,id.toString());
                        add(tags,"forge",kind,"ores/"+name,id.toString());
                    }
                    addMaterial(blocksByMaterial,denseOre.material(),id.toString());
                }
                if(block instanceof com.gregtech.gregtech.block.VanillaOreBlock ore
                        && ore.material().isValid()) {
                    String name=MaterialEquivalence.materialName(ore.material());
                    for(String kind:List.of("items","blocks")) {
                        add(tags,"gregtech",kind,"ore_vanillastone/"+name,id.toString());
                        add(tags,"forge",kind,"ores/"+name,id.toString());
                    }
                    addMaterial(blocksByMaterial,ore.material(),id.toString());
                }
            }
        }
        // GT6 ANY.Iron includes ten iron/steel materials, not just vanilla iron.
        var ironScrews = tags.computeIfAbsent("gregtech:tags/items/screws/any_iron_or_steel.json", k -> new TreeSet<>());
        for (var material : MaterialGroups.Iron.getReRegistrations()) {
            String path = "screws/" + MaterialEquivalence.materialName(material.resolve());
            if (tags.containsKey("forge:tags/items/" + path + ".json")) ironScrews.add("#forge:" + path);
        }
        var ironDoublePlates=tags.computeIfAbsent("gregtech:tags/items/plate_double/any_iron_or_steel.json",k->new TreeSet<>());
        for(var material:MaterialGroups.Iron.getReRegistrations()) {
            String path="plate_double/"+MaterialEquivalence.materialName(material.resolve());
            if(tags.containsKey("gregtech:tags/items/"+path+".json"))ironDoublePlates.add("#gregtech:"+path);
        }
        byMaterial.forEach((name,values)->{
            var tag=tags.computeIfAbsent("gregtech:tags/items/material/"+name+".json",k->new TreeSet<>());
            tag.addAll(values);
            tag.addAll(blocksByMaterial.getOrDefault(name,Set.of()));
        });
        // Wood form tags for the wooden-pipe recipes (GT6's OD.plankAnyWood / OD.beamWood): vanilla
        // planks and slabs join through nested tag references, the port's own species are listed.
        tags.computeIfAbsent("gregtech:tags/items/wooden_planks.json", k -> new TreeSet<>())
                .add("#minecraft:planks");
        tags.computeIfAbsent("gregtech:tags/items/wooden_slabs.json", k -> new TreeSet<>())
                .add("#minecraft:wooden_slabs");
        for (var item : ForgeRegistries.ITEMS.getValues()) {
            var id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null || !id.getNamespace().equals("gregtech")) continue;
            if (item instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof com.gregtech.gregtech.block.wood.WoodPlanksBlock) {
                tags.computeIfAbsent("gregtech:tags/items/wooden_planks.json", k -> new TreeSet<>())
                        .add(id.toString());
            }
            if (item instanceof BlockItem beamItem
                    && beamItem.getBlock() instanceof com.gregtech.gregtech.block.wood.WoodBeamBlock) {
                tags.computeIfAbsent("gregtech:tags/items/wooden_beams.json", k -> new TreeSet<>())
                        .add(id.toString());
            }
        }
        // Common spelling aliases used by Forge 1.20 packs. References keep datapack additions shared.
        var aliases=Map.of("aluminium","aluminum","quartz","nether_quartz","sulfur","sulphur","tungsten_steel","tungstensteel");
        for(var path:new ArrayList<>(tags.keySet())) for(var alias:aliases.entrySet()) {
            String suffix="/"+alias.getKey()+".json";
            if(path.startsWith("forge:")&&path.endsWith(suffix)) {
                String alt=path.substring(0,path.length()-suffix.length())+"/"+alias.getValue()+".json";
                // Union aliases without recursive tag references.
                var values=tags.get(path);tags.computeIfAbsent(alt,k->new TreeSet<>()).addAll(values);
            }
        }
        var gson=new com.google.gson.Gson(); var result=new HashMap<ResourceLocation,byte[]>();
        tags.forEach((path,values)->result.put(ResourceLocation.parse(path),gson.toJson(Map.of("replace",false,"values",values)).getBytes(StandardCharsets.UTF_8)));
        result.put(ResourceLocation.parse("gregtech:tags/items/ammunition/feathers.json"),gson.toJson(Map.of(
                "replace",false,"values",List.of("minecraft:feather",
                        Map.of("id","#forge:feathers","required",false),
                        Map.of("id","#c:feathers","required",false)))).getBytes(StandardCharsets.UTF_8));
        resources=Collections.unmodifiableMap(result); return resources;
    }

    /** Aggregate tag value list for one material (forms + its blocks). */
    private static void addMaterial(Map<String,Set<String>> target,com.gregtech.gregtech.api.material.GTMaterial material,String value) {
        if(material==null||!material.isValid())return;
        target.computeIfAbsent(MaterialEquivalence.materialName(material),k->new TreeSet<>()).add(value);
    }
    private static void add(Map<String,Set<String>> tags,String ns,String kind,String path,String value) {
        tags.computeIfAbsent(ns+":tags/"+kind+"/"+path+".json",k->new TreeSet<>()).add(value);
        int slash=path.indexOf('/');
        if(slash>0) tags.computeIfAbsent(ns+":tags/"+kind+"/"+path.substring(0,slash)+".json",k->new TreeSet<>()).add("#"+ns+":"+path);
    }
    public IoSupplier<InputStream> getRootResource(String... path) {
        if(path.length!=1||!path[0].equals("pack.mcmeta"))return null;
        return ()->new ByteArrayInputStream("{\"pack\":{\"pack_format\":15,\"description\":\"GregTech material tags\"}}".getBytes(StandardCharsets.UTF_8));
    }
    public IoSupplier<InputStream> getResource(PackType type,ResourceLocation id) {
        if(type!=PackType.SERVER_DATA)return null; var bytes=data().get(id);
        return bytes==null?null:()->new ByteArrayInputStream(bytes);
    }
    public void listResources(PackType type,String ns,String path,ResourceOutput out) {
        if(type==PackType.SERVER_DATA)data().forEach((id,bytes)->{if(id.getNamespace().equals(ns)&&id.getPath().startsWith(path+"/"))out.accept(id,()->new ByteArrayInputStream(bytes));});
    }
    public Set<String> getNamespaces(PackType type) { return type==PackType.SERVER_DATA?Set.of("forge","gregtech","minecraft"):Set.of(); }
    public void close() { resources=null; }
}
