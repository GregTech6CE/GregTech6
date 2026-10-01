package com.gregtech.gregtech.content.energy;
/** Original checkGears/getRotations; faces down/up/north/south/west/east and axes X=1/Y=2/Z=3. */
public final class GearboxRotationRules {private GearboxRotationRules(){}
 public static boolean gearsWork(int gearMask,int axisCode){
        int mask = gearMask & 63;
        int count = Integer.bitCount(mask);
        if (count <= 1) return true;
        if (count >= 5) return false;
        boolean xPair = (mask & 48) == 48;
        boolean yPair = (mask & 3) == 3;
        boolean zPair = (mask & 12) == 12;
        if (count == 2) {
            if (!xPair && !yPair && !zPair) return true;
            return switch (axisCode) {
                case 1 -> (mask & 48) != 0;
                case 2 -> (mask & 3) != 0;
                case 3 -> (mask & 12) != 0;
                default -> false;
            };
        }
        if (axisCode == 1 && xPair || axisCode == 2 && yPair || axisCode == 3 && zPair) return false;
        int usedAxes = ((mask & 48) != 0 ? 1 : 0)
                + ((mask & 3) != 0 ? 1 : 0) + ((mask & 12) != 0 ? 1 : 0);
        return usedAxes < 3;
 }
 public static int rotations(int gearMask,int axisCode,int input,boolean negative){
  if(!gearsWork(gearMask,axisCode))return 0;
  int face=1<<input,opposite=1<<(input^1),adjacent=gearMask&~face&~opposite;int result=negative?face:0;
  int inputAxis=input<2?2:input<4?3:1;
  if(axisCode!=0&&axisCode==inputAxis){
   if(!negative)result|=opposite;
   if((gearMask&face)!=0){if(!negative)result|=adjacent;return (result&gearMask&63)|64;}
   if((gearMask&opposite)!=0){if(negative)result|=adjacent;return (result&gearMask&63)|64;}
   return 0;
  }
  if((gearMask&face)!=0){if(negative)result|=opposite;if(!negative)result|=adjacent;return (result&gearMask&63)|64;}
  return 0;
 }
}
