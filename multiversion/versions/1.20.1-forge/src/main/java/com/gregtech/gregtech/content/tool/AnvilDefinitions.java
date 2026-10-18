package com.gregtech.gregtech.content.tool;
/** Platform registration over the shared original variant catalogue. */
public final class AnvilDefinitions {private AnvilDefinitions(){} public static void registerAll(){for(var v:AnvilVariantCatalog.ALL)com.gregtech.gregtech.registry.GTToolBlocks.anvil(v.id(),v.material(),v.durability());}}
