package gregtech.engine.machine;

/** Engine type enum for KU generators. */
public enum EngineType {
    ELECTRIC("kinetic_electric"),
    FLUX("kinetic_flux"),
    STEAM("kinetic_steam"),
    ROTATION("kinetic_rotation"),
    DIESEL("kinetic_diesel");

    private final String textureFolder;

    EngineType(String textureFolder) {
        this.textureFolder = textureFolder;
    }

    public String textureFolder() {
        return textureFolder;
    }
}
