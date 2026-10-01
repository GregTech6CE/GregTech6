package com.gregtech.gregtech.content.logistics;
/** Original ghost layout and whitelist/blacklist semantics. */
public final class FilterPolicy {private FilterPolicy(){}public static final int TEMPLATES=54;public static boolean validSlot(int slot){return slot>=0&&slot<TEMPLATES;}public static boolean allows(boolean blacklist,boolean found){return blacklist!=found;}public static boolean matches(boolean present,boolean sameItem,boolean untagged,boolean sameMetadata){return present&&sameItem&&(untagged||sameMetadata);}}
