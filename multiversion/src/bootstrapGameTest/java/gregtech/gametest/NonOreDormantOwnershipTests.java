package gregtech.gametest;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeMap;
import gregtech.content.recipe.DormantOreRows;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}
/** FIXED-PHASE ONLY: real native admission/lookup in an isolated map, not a supported addon lifecycle claim.
 * Same-input supported onRecipesReady control must additionally run in the isolated reload profile.
 */
@GameTestHolder("gregtech_nonore_ownership_audit")
@PrefixGameTestTemplate(false)
public final class NonOreDormantOwnershipTests {
 @GameTest(template="test_empty",timeoutTicks=100)
 public static void dormantDefinitionsNeverClaimCollisionsOrStaleReplacement(GameTestHelper h){
  var map=new RecipeMap(new ArrayList<>(),"gt.recipe.test.nonore_dormant","Nonore dormant","test",
    1,1,1,0,0,0,1,false,false,false,false);
  var ledger=new DormantOreRows();var material=GTMaterialRegistry.get("PVC").resolve();
  try{
   Recipe startup=map.addRecipe1(false,16,16,new ItemStack(Items.STICK),new ItemStack(Items.DIAMOND));
   h.assertTrue(startup!=null,"independent same-input row admitted before definitions");
   ItemStack input=new ItemStack(Items.APPLE);ItemStack output=new ItemStack(Items.COAL);
   long[] chances={10000};
   ledger.define(material,"fine",map,false,input,new ItemStack[]{output},chances,null,64,16);
   ledger.define(material,"collision",map,false,new ItemStack(Items.STICK),new ItemStack[]{new ItemStack(Items.COAL)},null,null,64,16);
   input.setCount(9);output.setCount(9);chances[0]=1;ledger.seal();
   h.assertTrue(ledger.definitionCount()==2&&ledger.owned().isEmpty()&&lookup(map,Items.APPLE)==null,"definition is not admission");
   h.assertTrue(ledger.reconcile(m->false)==0,"NO leaves all definitions dormant");
   h.assertTrue(ledger.reconcile(m->true)==1,"only noncolliding row accepted");
   Recipe first=lookup(map,Items.APPLE);
   h.assertTrue(first!=null&&first.mInputs[0].getCount()==1&&first.mOutputs[0].getCount()==1
      &&first.mChances[0]==10000&&first.mDuration==64&&first.mEUt==16,"defensive immutable values preserved");
   h.assertTrue(lookup(map,Items.STICK)==startup&&!ledger.owned().contains(startup),"collision incumbent never claimed");
   map.captureBaseline(ledger.owned());map.restoreBaseline();
   h.assertTrue(lookup(map,Items.APPLE)==null&&lookup(map,Items.STICK)==startup,"baseline excludes only accepted handle");
   ledger.reconcile(m->true);Recipe second=lookup(map,Items.APPLE);
   h.assertTrue(second!=null&&second!=first&&ledger.owned().contains(second),"fresh object on replay");
   h.assertTrue(lookup(map,Items.APPLE)==second,"warm lookup selects replay");
   map.unlink(second);
   Recipe external=map.addRecipe1(false,16,16,new ItemStack(Items.APPLE),new ItemStack(Items.NETHER_STAR));
   h.assertTrue(external!=null,"replacement fixture admitted");ledger.clear();
   h.assertTrue(lookup(map,Items.APPLE)==external&&lookup(map,Items.STICK)==startup,"stale exact handle does not delete replacement");
   h.assertTrue(ledger.reconcile(m->true)==0&&lookup(map,Items.APPLE)==external,"replay collision preserves replacement");
   map.unlink(external);ledger.reconcile(m->true);ledger.reconcile(m->false);
   h.assertTrue(lookup(map,Items.APPLE)==null&&lookup(map,Items.STICK)==startup&&ledger.definitionCount()==2,"deactivate retains definitions and independent rows");
   // Narrow same-instance lifecycle contract, NOT a fake ServerStoppedEvent or real new-world claim.
   ledger.clear();ledger.clear();
   var nextSnapshot=java.util.Collections.<gregtech.internal.material.GTMaterial>emptySet();
   h.assertTrue(ledger.reconcile(nextSnapshot::contains)==0&&lookup(map,Items.APPLE)==null,
      "new empty eligibility snapshot cannot retain prior activation");
   var laterSnapshot=java.util.Set.of(material);
   h.assertTrue(ledger.reconcile(laterSnapshot::contains)==1,"retained immutable definitions can activate again after clear");
   Recipe later=lookup(map,Items.APPLE);
   h.assertTrue(later!=null&&later!=first&&later!=second&&lookup(map,Items.STICK)==startup,
      "same ledger reuses definitions, not old accepted handles; independent row preserved");
  }finally{ledger.clear();RecipeMap.unregister(map);}
  h.succeed();
 }
 private static Recipe lookup(RecipeMap map,net.minecraft.world.item.Item item){return map.findRecipe(List.of(new ItemStack(item)),List.of(),false,1,12);}
}
