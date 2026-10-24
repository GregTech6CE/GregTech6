package gregtech.bootstrap.content;

import gregtech.data.GregTechData;
import gregtech.bootstrap.InitializationStep;

public record DataBootstrap() implements InitializationStep {
    @Override
    public void run() {
        GregTechData.init();
    }
}
