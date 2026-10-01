package com.gregtech.gregtech.content.transport;
/** GT6 powered launch, momentum doubling and unpowered braking. Axis: 0 other, 1 EW, 2 NS. */
public final class TrackMotionRules {private TrackMotionRules(){}public record Motion(double x,double y,double z){}
 public static Motion apply(double x,double y,double z,boolean powered,int axis,boolean negativeWall,boolean positiveWall){
  double horizontal=Math.sqrt(x*x+z*z);if(powered){if(horizontal>.01D)return new Motion(x*2,y,z*2);
   if(axis==1){if(negativeWall)return new Motion(.02,y,z);if(positiveWall)return new Motion(-.02,y,z);}
   else if(axis==2){if(negativeWall)return new Motion(x,y,.02);if(positiveWall)return new Motion(x,y,-.02);}
   return new Motion(x,y,z);}
  return horizontal<.03D?new Motion(0,0,0):new Motion(x/2,0,z/2);
 }
}
