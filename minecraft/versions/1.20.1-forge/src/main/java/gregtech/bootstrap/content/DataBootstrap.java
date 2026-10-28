package gregtech.bootstrap.content;

import gregtech.bootstrap.InitializationStep;
import gregtech.content.data.GregTechData;

public record DataBootstrap() implements InitializationStep {
    @Override
    public void run() {
        GregTechData.init();
    }
}
