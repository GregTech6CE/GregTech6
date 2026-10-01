package com.gregtech.gregtech.content.recipe;
import java.util.List;
/** Exact original available crop juice/residue/yield rows. */
public final class CropProcessingCatalog {
 private CropProcessingCatalog(){}
 public record Row(String input,String fluid,int amount,long chance,String residue){}
 public static final List<Row> ROWS=List.of(
  new Row("minecraft:apple","Juice_Apple",100,7000,"fruit_remains"),
  new Row("gregtech:apple","Juice_Apple",100,7000,"fruit_remains"),
  new Row("minecraft:melon_slice","Juice_Melon",250,6000,"fruit_remains"),
  new Row("gregtech:blackberry","Juice_Blackberry",100,4000,"fruit_remains"),
  new Row("gregtech:blueberry","Juice_Blueberry",100,4000,"fruit_remains"),
  new Row("gregtech:raspberry","Juice_Raspberry",100,4000,"fruit_remains"),
  new Row("gregtech:cranberry","Juice_Cranberry",100,4000,"fruit_remains"),
  new Row("gregtech:gooseberry","Juice_Gooseberry",100,5000,"fruit_remains"),
  new Row("gregtech:strawberry","Juice_Strawberry",100,4000,"fruit_remains"),
  new Row("gregtech:lemon","Juice_Lemon",125,7000,"fruit_remains"),
  new Row("gregtech:pomegranate","Juice_Pomegranate",100,9000,"fruit_remains"),
  new Row("gregtech:banana","Juice_Banana",100,8000,"fruit_remains"),
  new Row("gregtech:ananas","Juice_Ananas",200,9000,"fruit_remains"),
  new Row("minecraft:beetroot","Juice_Beet",200,7000,"vegetable_remains"),
  new Row("minecraft:carrot","Juice_Carrot",100,7000,"vegetable_remains"),
  new Row("minecraft:pumpkin","Juice_Pumpkin",1000,9000,"vegetable_remains"),
  new Row("gregtech:tomato","Juice_Tomato",100,5000,"vegetable_remains"),
  new Row("gregtech:cucumber","Juice_Cucumber",150,5000,"vegetable_remains"),
  new Row("gregtech:pickle","Juice_Cucumber",150,5000,"vegetable_remains"),
  new Row("gregtech:onion","Juice_Onion",100,7000,"vegetable_remains"));
 public static int pressed(int amount){return amount-(amount<100?amount/3:1+amount/250)*25;}
}
