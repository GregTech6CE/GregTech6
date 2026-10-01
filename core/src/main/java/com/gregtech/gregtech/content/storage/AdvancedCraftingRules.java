package com.gregtech.gregtech.content.storage;
import java.util.stream.IntStream;import java.util.function.IntPredicate;import java.util.function.IntUnaryOperator;
/** Original addressed slot layout and exact ingredient consumption order. */
public final class AdvancedCraftingRules {
 private AdvancedCraftingRules(){} public static final int SIZE=71;
 public static int[] inputs(){return IntStream.concat(IntStream.range(0,21),IntStream.range(35,71)).toArray();}
 public static int[] storage(){return IntStream.concat(IntStream.range(0,16),IntStream.range(35,71)).toArray();}
 public static int source(IntPredicate matches,IntUnaryOperator count,int gridSlot){
  if(matches.test(gridSlot)&&count.applyAsInt(gridSlot)>1)return gridSlot;
  for(int i=21;i<30;i++)if(matches.test(i)&&count.applyAsInt(i)>1)return i;
  for(int i=70;i>=35;i--)if(matches.test(i))return i;
  for(int i=20;i>=0;i--)if(matches.test(i))return i;
  for(int i=21;i<30;i++)if(matches.test(i))return i;
  return -1;
 }
 public static int[] automationSlots(boolean flush,boolean blocked16,boolean blocked36){
  var out=new java.util.ArrayList<Integer>();out.add(33);
  if(flush)for(int i=21;i<30;i++)out.add(i);
  if(!blocked16)for(int i=0;i<16;i++)out.add(i);
  if(!blocked36)for(int i=35;i<71;i++)out.add(!flush&&i==63?64:!flush&&i==64?63:i);
  return out.stream().mapToInt(Integer::intValue).toArray();
 }
}
