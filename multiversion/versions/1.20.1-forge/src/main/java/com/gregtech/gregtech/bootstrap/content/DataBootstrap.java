package com.gregtech.gregtech.bootstrap.content;

import com.gregtech.gregtech.data.GregTechData;
import com.gregtech.gregtech.bootstrap.InitializationStep;

public record DataBootstrap() implements InitializationStep {
    @Override
    public void run() {
        GregTechData.init();
    }
}
