package com.gregtech.gregtech.worldgen;
/** Original source24 ground-litter rows, shared by the actual two feature executors. */
public final class NetherScatterCatalog {
 private NetherScatterCatalog(){}
 public record Scatter(String name,String material,String itemId,boolean rawOre){}
    public static final java.util.List<Scatter> TABLE = java.util.List.of(
            new Scatter("gem.netherquartz", "NetherQuartz", "", false),      // case 0
            new Scatter("gem.glowstone", "Glowstone", "", false),            // case 1
            new Scatter("raw.ancientdebris", "AncientDebris", "", true),     // case 2
            new Scatter("brick.quartz_or_debris", "AncientDebris", "", true),// case 3
            new Scatter("brick.glowstone_or_debris", "Glowstone", "", false),// case 4
            new Scatter("brick.obsidian_or_debris", "Obsidian", "", false),  // case 5
            new Scatter("soul.gloomstone", "Gloomstone", "", false),         // case 6
            new Scatter("soul.gloomstone2", "Gloomstone", "", false),        // case 7
            new Scatter("soul.quartz", "NetherQuartz", "", false),           // case 8
            new Scatter("soul.quartz2", "NetherQuartz", "", false),          // case 9
            new Scatter("soul.quartz3", "NetherQuartz", "", false),          // case 10
            new Scatter("soul.quartz4", "NetherQuartz", "", false),          // case 11
            new Scatter("rock.obsidian", "Obsidian", "", false),             // case 12
            new Scatter("rock.basalt", "Basalt", "", false),                 // case 13
            new Scatter("rock.basalt2", "Basalt", "", false),                // case 14
            new Scatter("rock.basalt3", "Basalt", "", false),                // case 15
            new Scatter("gravel.flint1", "Blackstone", "", false),           // cases 16-23 use flint on gravel
            new Scatter("gravel.flint2", "Blackstone", "", false),
            new Scatter("gravel.flint3", "Blackstone", "", false),
            new Scatter("gravel.flint4", "Blackstone", "", false),
            new Scatter("gravel.flint5", "Blackstone", "", false),
            new Scatter("gravel.flint6", "Blackstone", "", false),
            new Scatter("gravel.flint7", "Blackstone", "", false),
            new Scatter("gravel.flint8", "Blackstone", "", false));
}
