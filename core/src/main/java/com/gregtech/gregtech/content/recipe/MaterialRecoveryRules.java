package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.math.BigInteger;
/** Original VanillaRecoveryRecipes pulverizing arithmetic and dust pile selection. */
public final class MaterialRecoveryRules {
 private MaterialRecoveryRules(){}
 public record DustPile(MaterialPrefix prefix,int count){}
 public static long pulverizedAmount(GTMaterial source,long amount){
  return BigInteger.valueOf(amount).multiply(BigInteger.valueOf(source.getTargetPulverAmount())).divide(BigInteger.valueOf(GTValues.U)).longValueExact();
 }
 public static long shredderWork(GTMaterial material){
  return (material.getName().contains("Quartz")?64L:material.hasAny(MaterialProperty.WOOD,MaterialProperty.STONE,MaterialProperty.GEM)?2L:256L)*Math.max(1,material.getToolQuality()+1);
 }
 public static DustPile dust(long amount){
  long unit=GTValues.U;
  if(amount<unit/72)return null;
  if(amount>=unit&&(amount>=unit*16||amount%unit==0))return pile(MaterialPrefix.dust,amount/unit);
  if(amount>=unit/4&&(amount>=unit*8||amount%(unit/4)<=amount%(unit/9)))return pile(MaterialPrefix.dustSmall,amount/(unit/4));
  if(amount>=unit/9&&(amount>=unit||amount%(unit/9)<=amount%(unit/72)))return pile(MaterialPrefix.dustTiny,amount/(unit/9));
  return pile(MaterialPrefix.dustDiv72,amount/(unit/72));
 }
 private static DustPile pile(MaterialPrefix prefix,long count){return new DustPile(prefix,(int)Math.min(64,count));}
}
