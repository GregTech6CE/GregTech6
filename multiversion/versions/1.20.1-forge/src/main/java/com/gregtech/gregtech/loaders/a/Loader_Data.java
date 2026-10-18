package com.gregtech.gregtech.loaders.a;

import com.gregtech.gregtech.data.GregTechData;
import com.gregtech.gregtech.loaders.IGTLoader;

public record Loader_Data() implements IGTLoader {
    @Override
    public void run() {
        GregTechData.init();
    }
}
