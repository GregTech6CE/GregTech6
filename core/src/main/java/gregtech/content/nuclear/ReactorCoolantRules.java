package gregtech.content.nuclear;

import java.util.*;

/** Shared coolant conversion values and rod-parameter modifiers. */
public enum ReactorCoolantRules {
    DISTILLED("DistW", "Steam", 80, 160, 1, true),
    INDUSTRIAL("Coolant_IC2", "Coolant_IC2_Hot", 20, 1, 1, false),
    TIN("GenMolten_Tin", "Hot_Molten_Tin", 40, 1, 3, false),
    SODIUM("GenMolten_Sodium", "Hot_Molten_Sodium", 30, 1, 6, false),
    SEMIHEAVY("GenLiquid_SemiheavyWater", "Hot_Semi_Heavy_Water", 40, 1, 1, true),
    HEAVY("GenLiquid_HeavyWater", "Hot_Heavy_Water", 50, 1, 1, true),
    TRITIATED("GenLiquid_TritiatedWater", "Hot_Tritiated_Water", 60, 1, 1, true),
    LITHIUM_CHLORIDE("GenLiquid_LithiumChloride", "Hot_Molten_LiCl", 15, 1, 1, false),
    CO2("CarbonDioxide", "Hot_Carbon_Dioxide", 20, 1, 1, false),
    HELIUM("Helium", "Hot_Helium", 30, 1, 1, false),
    THORIUM_SALT("Thorium_Salt", "GenLiquid_LithiumChloride", 2560000, 1, 1, false);

    public final String input, output;
    public final int heat, expansion, heatDivider;
    public final boolean moderates;

    ReactorCoolantRules(String inputKey, String outputKey, int heatPerUnit,
                        int outputExpansion, int heatDivisor, boolean moderating) {
        input = inputKey;
        output = outputKey;
        heat = heatPerUnit;
        expansion = outputExpansion;
        heatDivider = heatDivisor;
        moderates = moderating;
    }

    public int self(ReactorRodCatalog.Rod rod) {
        return switch (this) {
            case INDUSTRIAL -> rod.self() * 4;
            case CO2 -> rod.self() * 3;
            case LITHIUM_CHLORIDE -> rod.self() * 5;
            case THORIUM_SALT -> 0;
            default -> rod.self();
        };
    }

    public int emission(ReactorRodCatalog.Rod rod) {
        return switch (this) {
            case INDUSTRIAL -> rod.emission() * 4;
            case HELIUM, LITHIUM_CHLORIDE, THORIUM_SALT -> rod.emission() / 2;
            default -> rod.emission();
        };
    }

    public int divisor(ReactorRodCatalog.Rod rod) {
        return Math.max(1, switch (this) {
            case INDUSTRIAL -> rod.divisor() * 2;
            case TIN, SODIUM, THORIUM_SALT -> rod.divisor() - 1;
            default -> rod.divisor();
        });
    }

    public int maximum(ReactorRodCatalog.Rod rod) {
        return switch (this) {
            case HEAVY -> (rod.maximum() + 7) / 8;
            case TRITIATED -> (rod.maximum() + 15) / 16;
            case LITHIUM_CHLORIDE -> rod.maximum() + (rod.maximum() + 3) / 4;
            case THORIUM_SALT -> rod.maximum() * 4;
            default -> rod.maximum();
        };
    }
}
