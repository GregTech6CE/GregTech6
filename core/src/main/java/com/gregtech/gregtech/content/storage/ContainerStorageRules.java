package com.gregtech.gregtech.content.storage;
import java.util.stream.IntStream;import java.util.List;
/** Original storage addressing; facing ordinals use Minecraft DOWN/UP/NORTH/SOUTH/WEST/EAST. */
public final class ContainerStorageRules {
 private ContainerStorageRules(){} public static final int COMPARTMENTS=4,PAGE_SIZE=36,DRAWER_SLOTS=144,BOTTLE_SLOTS=9;
 public static final List<String> ARMOR_ORDER=List.of("HEAD","CHEST","LEGS","FEET");
 private static final int[][] ROTATIONS={{0,1,2,3,4,5},{0,1,2,3,4,5},{0,1,3,5,4,2},{0,1,5,3,2,4},{0,1,2,4,3,5},{0,1,4,2,5,3}};
 public static int[] drawerSlots(boolean sided,int facing,int side){int local=!sided||side<0?3:ROTATIONS[facing][side];return IntStream.range(0,DRAWER_SLOTS).filter(i->switch(local){case 0->i>=72;case 1->i<72;case 2->i/36%2==0;case 4->i/36%2==1;default->true;}).toArray();}
 public static int quadrant(int facing,double x,double y,double z){double u=switch(facing){case 0,1,3->x;case 2->1-x;case 4->z;case 5->1-z;default->throw new IllegalArgumentException();};double v=switch(facing){case 0->1-z;case 1->z;default->1-y;};return(u>.5?1:0)|(v>.5?2:0);}
 public static int bottleSlot(double x,double z){return(x<5.5/16?0:x<10.5/16?1:2)+(z<5.5/16?0:z<10.5/16?3:6);}
 public record Box(double x0,double y0,double z0,double x1,double y1,double z1){}
 public static Box bottleBounds(int slot,int part){if(slot<0||slot>=9||part<0||part>2)throw new IllegalArgumentException();double x=1+5*(slot%3),z=1+5*(slot/3);return switch(part){case 0->new Box(x/16+.01,1/16.,z/16+.01,(x+4)/16-.01,12/16.,(z+4)/16-.01);case 1->new Box(x/16+.005,1/16.,z/16+.005,(x+4)/16-.005,13/16.,(z+4)/16-.005);default->new Box((x+1)/16,13/16.,(z+1)/16,(x+3)/16,1,(z+3)/16);};}
}
