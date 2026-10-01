package com.gregtech.gregtech.content.logistics;
/** GT6 six-side numbering: opposite faces differ in their low bit. Null side is -1. */
public final class RelayRoutingRules {private RelayRoutingRules(){}public static int exit(boolean bridge,int query,int front,int secondary){return bridge?(query<0?-1:query^1):query==front?secondary:front;}}
