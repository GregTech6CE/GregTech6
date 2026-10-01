package com.gregtech.gregtech.content.data;
/** Original GT6 media identity, selection, file keys and tier gates, without platform stack/NBT types. */
public final class UsbDataRules {
 private UsbDataRules(){} public static final int FILES=16,SCANNER_TIER=3;
 public static final String DATA="gt.usb.data",TIER="gt.usb.tier",DRIVE="gt.usb.drive",MATERIAL="gt.replicator.data",DIRECTION="gt.usb.dir";
 public static int mode(int value){return Math.max(0,Math.min(FILES-1,value));}
 public static boolean slot(int value){return value>=0&&value<FILES;}
 public static boolean writable(int mediumTier,int requestedTier){return requestedTier>=0&&requestedTier<=4&&mediumTier>=requestedTier;}
 public static boolean readable(int mediumTier,int requestedTier,int fileTier){return writable(mediumTier,requestedTier)&&fileTier<=requestedTier;}
 public static String slotData(int slot){return DATA+slot;} public static String slotTier(int slot){return TIER+slot;}
 public static int tier(String namespace,String id,String suffix){if(!"gregtech".equals(namespace)||!id.startsWith("usb")||!id.endsWith(suffix)||id.length()!=4+suffix.length())return -1;char n=id.charAt(3);return n>='1'&&n<='4'?n-'0':-1;}
}
