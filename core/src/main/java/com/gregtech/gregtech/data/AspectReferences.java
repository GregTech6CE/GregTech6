package com.gregtech.gregtech.data;

import java.util.LinkedHashMap;
import java.util.Map;

/** Thaumcraft aspects from GT6 TC.java. Auto-transpiled skeleton. */
public class AspectReferences {
    protected AspectReferences() {}

    public static final class Aspect {
        private final String name;
        Aspect(String name) { this.name = name; }
        public String getName() { return name; }
        @Override public String toString() { return name; }
    }

    private static final Map<String, Aspect> REGISTRY = new LinkedHashMap<>();

    private static Aspect aspect(String field, String name) {
        Aspect a = new Aspect(name);
        REGISTRY.put(field, a);
        return a;
    }

    public static final Aspect
            ORDO = aspect("ORDO", "ORDO"),
            PERDITIO = aspect("PERDITIO", "PERDITIO"),
            TERRA = aspect("TERRA", "TERRA"),
            AQUA = aspect("AQUA", "AQUA"),
            AER = aspect("AER", "AER"),
            IGNIS = aspect("IGNIS", "IGNIS"),
            LUX = aspect("LUX", "LUX"),
            TEMPESTAS = aspect("TEMPESTAS", "TEMPESTAS"),
            MOTUS = aspect("MOTUS", "MOTUS"),
            VACUOS = aspect("VACUOS", "VACUOS"),
            GELUM = aspect("GELUM", "GELUM"),
            POTENTIA = aspect("POTENTIA", "POTENTIA"),
            VITREUS = aspect("VITREUS", "VITREUS"),
            VICTUS = aspect("VICTUS", "VICTUS"),
            VENEMUM = aspect("VENEMUM", "VENEMUM"),
            PERMUTATIO = aspect("PERMUTATIO", "PERMUTATIO"),
            VOLATUS = aspect("VOLATUS", "VOLATUS"),
            VINCULUM = aspect("VINCULUM", "VINCULUM"),
            ITER = aspect("ITER", "ITER"),
            METALLUM = aspect("METALLUM", "METALLUM"),
            HERBA = aspect("HERBA", "HERBA"),
            LIMUS = aspect("LIMUS", "LIMUS"),
            SANO = aspect("SANO", "SANO"),
            MORTUUS = aspect("MORTUUS", "MORTUUS"),
            BESTIA = aspect("BESTIA", "BESTIA"),
            FAMES = aspect("FAMES", "FAMES"),
            TENEBRAE = aspect("TENEBRAE", "TENEBRAE"),
            PRAECANTIO = aspect("PRAECANTIO", "PRAECANTIO"),
            REFLEXIO = aspect("REFLEXIO", "REFLEXIO"),
            RADIO = aspect("RADIO", "RADIO"),
            ARBOR = aspect("ARBOR", "ARBOR"),
            AURAM = aspect("AURAM", "AURAM"),
            VITIUM = aspect("VITIUM", "VITIUM"),
            SPIRITUS = aspect("SPIRITUS", "SPIRITUS"),
            EXAMINIS = aspect("EXAMINIS", "EXAMINIS"),
            ALIENIS = aspect("ALIENIS", "ALIENIS"),
            CORPUS = aspect("CORPUS", "CORPUS"),
            MAGNETO = aspect("MAGNETO", "MAGNETO"),
            SENSUS = aspect("SENSUS", "SENSUS"),
            COGNITIO = aspect("COGNITIO", "COGNITIO"),
            STRONTIO = aspect("STRONTIO", "STRONTIO"),
            HUMANUS = aspect("HUMANUS", "HUMANUS"),
            PERFODIO = aspect("PERFODIO", "PERFODIO"),
            INSTRUMENTUM = aspect("INSTRUMENTUM", "INSTRUMENTUM"),
            MESSIS = aspect("MESSIS", "MESSIS"),
            LUCRUM = aspect("LUCRUM", "LUCRUM"),
            TELUM = aspect("TELUM", "TELUM"),
            TUTAMEN = aspect("TUTAMEN", "TUTAMEN"),
            PANNUS = aspect("PANNUS", "PANNUS"),
            FABRICO = aspect("FABRICO", "FABRICO"),
            METO = aspect("METO", "METO"),
            MACHINA = aspect("MACHINA", "MACHINA"),
            NEBRISUM = aspect("NEBRISUM", "NEBRISUM"),
            ELECTRUM = aspect("ELECTRUM", "ELECTRUM");

    public static Map<String, Aspect> allAspects() {
        return Map.copyOf(REGISTRY);
    }

    public static void bootstrap() {
        if (REGISTRY.isEmpty()) {
            throw new IllegalStateException("TC aspects failed to initialize");
        }
    }
}
