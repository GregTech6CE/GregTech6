package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.block.misc.LegacyConstructionBlockItem;
import com.gregtech.gregtech.content.recipe.VanillaRecoveryRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.List;

@GameTestHolder("gt6ce_forge_processing_followup") @PrefixGameTestTemplate(false)
public final class ForgeProcessingFollowupTests {
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200)
    public static void shredderOutputsNeverExceedDeclaredMaterial(GameTestHelper h) {
        VanillaCompositionTests.shredderOutputsNeverExceedDeclaredMaterial(h);
    }

    @GameTest(template="test_blueprint_empty",timeoutTicks=1200)
    public static void legacyPaletteAliasesRecoverWithoutLosingStoredContents(GameTestHelper h) {
        int families=0, colors=0;
        for(var recipe:VanillaRecoveryRecipes.recipes()) {
            var bare=recipe.mInputs[0];
            if(!(bare.getItem() instanceof LegacyConstructionBlockItem))continue;
            families++;
            var snapshot=bare.copy();
            h.assertTrue(RecipeInputs.consume(recipe,List.of(bare),List.of(),1)!=null,"bare legacy input executes");
            var found=MachineRecipeMaps.Shredder.findRecipe(List.of(bare),List.of(),false,1,16);
            h.assertTrue(found!=null&&RecipeInputs.consume(found,List.of(bare),List.of(),1)!=null,"native lookup admits bare alias");
            for(var color:DyeColor.values()) {
                var legacy=bare.copy();var state=new CompoundTag();state.putString("color",color.getName());legacy.addTagElement("BlockStateTag",state);
                var canonical=LegacyConstructionBlockItem.canonical(legacy);
                h.assertTrue(canonical.getItem()!=bare.getItem(),"legacy color maps to fixed registry identity");
                for(var offered:List.of(legacy,canonical)) {
                    var consumed=RecipeInputs.consume(recipe,List.of(offered),List.of(),1);
                    h.assertTrue(consumed!=null&&consumed.items().get(0).isEmpty(),"same family accepts legacy and fixed color: "+color);
                    var unsafe=offered.copy();unsafe.getOrCreateTag().put("BlockEntityTag",new CompoundTag());
                    h.assertTrue(RecipeInputs.consume(recipe,List.of(unsafe),List.of(),1)==null,"palette normalization preserves stored-content rejection");
                }
                var wrong=legacy.copy();wrong.getTagElement("BlockStateTag").putString("color",DyeColor.byId((color.getId()+1)%16).getName());
                h.assertTrue(!RecipeInputs.matches(legacy,wrong),"explicit legacy color does not become a wildcard");
                colors++;
            }
            h.assertTrue(ItemStack.isSameItemSameTags(snapshot,bare)&&snapshot.getCount()==bare.getCount(),"matching never mutates recipe prototype");
        }
        h.assertTrue(families==6&&colors==96,"all six affected aliases and sixteen colors exercised: "+families+" / "+colors);
        h.succeed();
    }
}
