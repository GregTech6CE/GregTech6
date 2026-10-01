package com.gregtech.gregtech.content.transport;
/** Original mode wrap/slot limits, preserving byte and saved-key semantics. */
public final class HopperControlRules {
 private HopperControlRules(){}
 public static byte cycle(byte mode,boolean sneak,boolean queue){byte next=(byte)(mode+(sneak?-1:1));if(sneak&&next<(queue?1:0))return 64;if(!sneak&&next>64)return (byte)(queue?1:0);return next;}
 public static int slotLimit(byte mode,boolean queue){return mode<=0?64:queue?Math.max(1,Math.min(64,mode)):mode*Math.max(1,64/mode);}
}
