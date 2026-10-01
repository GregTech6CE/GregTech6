package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.block.stone.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Survival entries from GT6 BlockStones and Loader_Recipes_Handlers (157,403-416). */
public final class StoneAndToolSurvivalRecipes {
    private static final List<Recipe> ADDED=new ArrayList<>();
    private static final List<Recipe> STONE_EXTRUSIONS=new ArrayList<>();
    public static List<Recipe> recipes(){return List.copyOf(ADDED);}
    /** The BlockStones rows of {@link #registerStoneExtrusions()}, for reporting and tests. */
    public static List<Recipe> stoneExtrusions(){return List.copyOf(STONE_EXTRUSIONS);}
    public static int stoneExtrusionCount(){return STONE_EXTRUSIONS.size();}
    private static void add(RecipeMap map, ItemStack in, ItemStack out, long ticks) {
        if(in.isEmpty()||out.isEmpty())return;
        var recipe=new Recipe(new ItemStack[]{in},new ItemStack[]{out},null,null,null,null,ticks,16,0);
        if(map.addRecipe(recipe)!=null)ADDED.add(recipe);
    }
    public static int register() {
        // Dense BlockRockOres / BlockCrystalOres are handled by Loader_Recipes_OreProcessing,
        // which applies GT6's doubled ore yield. Keep this class for the stone tool chain.
        for(var material:GTMaterialRegistry.sortedMaterials()) {
            if(material.resolve()!=material||!material.isValid()||material.has(MaterialProperty.ANTIMATTER))continue;
            var rock=GTItems.getStack(MaterialPrefix.rockGt,material);
            var dust=GTItems.getStack(MaterialPrefix.dustSmall,material,9);
            if(!rock.isEmpty()&&!dust.isEmpty()) {
                var recipe=new Recipe(new ItemStack[]{rock,ItemStack.EMPTY},new ItemStack[]{dust},null,null,null,null,16,16,0);
                if(MachineRecipeMaps.Anvil.addRecipe(recipe)!=null)ADDED.add(recipe);
            }
            for(var prefix:com.gregtech.gregtech.api.prefix.PrefixRegistry.all()) {
                if(!prefix.getName().startsWith("toolHeadRaw"))continue;
                var target=com.gregtech.gregtech.api.prefix.PrefixRegistry.all().stream().filter(p -> p.getName().equals(prefix.getName().replace("toolHeadRaw","toolHead"))).findFirst().orElse(null);
                if(target!=null)add(MachineRecipeMaps.Sharpening,GTItems.getStack(prefix,material),GTItems.getStack(target,material),16);
            }
        }
        registerStoneExtrusions();
        return ADDED.size();
    }

    /**
     * GT6 {@code BlockStones:286-315} and {@code BlockStones:331-360}: the stone-equal and
     * cobblestone-equal blocks of every rock type extrude into that rock's plates, rods, bolts,
     * (small) gears and *raw tool heads*, 16 EU/t for 32 ticks — with both mold families
     * ({@code IL.Shape_Extruder_*} and {@code IL.Shape_SimpleEx_*}, the port's
     * {@code extruder_shape_*} / {@code low_heat_extruder_shape_*}). These rows are what makes GT6's
     * stone tools reachable: the raw heads they produce are the ones the sharpening chain finishes
     * (the port's stone tool heads come from §24 of the porting notes).
     *
     * <p>GT6 iterates {@code mEqualBlocks[STONE]} and {@code mEqualBlocks[COBBL]}, i.e. only those two
     * variant groups — not every masonry variant. The port's equivalent sets are the {@code _stone} and
     * {@code _cobble} blocks of each {@link StoneType}.</p>
     *
     * <p>Two shapes output blocks rather than material forms: the ingot mold yields the rock's brick
     * variant and the block mold its plain stone variant ({@code ST.make(this, 1, BRICK/STONE)}). For the
     * stone group the block mold therefore re-emits its own input, exactly as in GT6.</p>
     */
    private static final String[][] STONE_EXTRUDER_SHAPES = {
            {"plate", "i:plate:%s:9"},
            {"curvedplate", "i:plateCurved:%s:9"},
            {"rod", "i:stick:%s:18"},
            {"longrod", "i:stickLong:%s:9"},
            {"bolt", "i:bolt:%s:64"},
            {"shovelhead", "i:toolHeadRawShovel:%s:9"},
            {"swordblade", "i:toolHeadRawSword:%s:4"},
            {"hoehead", "i:toolHeadRawHoe:%s:4"},
            {"pickaxehead", "i:toolHeadRawPickaxe:%s:3"},
            {"axehead", "i:toolHeadRawAxe:%s:3"},
            {"gear", "i:gearGt:%s:2"},
            {"smallgear", "i:gearGtSmall:%s:9"},
            {"hammerhead", "i:toolHeadHammer:%s:1"},
    };

    /** {@code IL.Shape_Extruder_*} then {@code IL.Shape_SimpleEx_*}, in GT6's own row order. */
    private static final String[] STONE_EXTRUDER_MOLDS = {"extruder_shape_", "low_heat_extruder_shape_"};

    /** The {shape, output spec} pairs of {@link #registerStoneExtrusions()}, for reporting and tests. */
    public static String[][] stoneExtrusionShapes(){return STONE_EXTRUDER_SHAPES.clone();}

    /** The mould id prefixes of {@link #registerStoneExtrusions()}, for reporting and tests. */
    public static String[] stoneExtrusionMolds(){return STONE_EXTRUDER_MOLDS.clone();}

    private static void registerStoneExtrusions() {
        for (var block : ForgeRegistries.BLOCKS.getValues()) {
            if (!(block instanceof GTStoneBlock stone)) continue;
            var variant = stone.variant();
            if (variant != StoneVariant.STONE && variant != StoneVariant.COBBLE) continue;
            var material = stone.stoneMaterial();
            var input = new ItemStack(block);
            var bricks = blockItem(stone.stoneType().registryId() + "_" + StoneVariant.BRICKS.registrySuffix());
            var plain = blockItem(stone.stoneType().registryId() + "_" + StoneVariant.STONE.registrySuffix());
            for (String family : STONE_EXTRUDER_MOLDS) {
                for (String[] shape : STONE_EXTRUDER_SHAPES) {
                    var out = com.gregtech.gregtech.loaders.c.GTGeneratedChem
                            .resolveSpec(String.format(shape[1], material.getName()));
                    extrude(input, mold(family + shape[0]), out);
                }
                extrude(input, mold(family + "ingot"), bricks);
                extrude(input, mold(family + "block"), plain);
            }
        }
    }

    /**
     * BlockStones passes all five flags as {@code F, F, F, F, T}: no optimization ({@code aOptimize=F},
     * so the stone group's block-mold row survives as a literal self-loop instead of collapsing) and no
     * collision check ({@code aCheckForCollisions=F}, because the stone and cobblestone groups register
     * the same moulds for two different inputs and GT6 allows that).
     */
    private static void extrude(ItemStack input, ItemStack mold, ItemStack output) {
        if (input.isEmpty() || mold.isEmpty() || output == null || output.isEmpty()) return;
        var recipe = MachineRecipeMaps.Extruder.addRecipe2(false, false, false, false, true, 16, 32, input, mold, output);
        if (recipe != null) {
            ADDED.add(recipe);
            STONE_EXTRUSIONS.add(recipe);
        }
    }

    private static ItemStack mold(String id) {
        var item = com.gregtech.gregtech.registry.GTTechnological.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack blockItem(String id) {
        var item = ForgeRegistries.ITEMS.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", id));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
