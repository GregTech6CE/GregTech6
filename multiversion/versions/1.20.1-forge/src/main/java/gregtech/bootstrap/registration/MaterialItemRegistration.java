package gregtech.bootstrap.registration;

import gregtech.bootstrap.InitializationStep;
import gregtech.content.registry.GTMaterialRegistration;

public record MaterialItemRegistration() implements InitializationStep {
    @Override
    public void run() {
        GTMaterialRegistration.registerAll();
    }
}
