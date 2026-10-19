package com.gregtech.gregtech.bootstrap.content;

import com.gregtech.gregtech.data.GregTechData;
import com.gregtech.gregtech.bootstrap.IGTLoader;

public record Loader_Data() implements IGTLoader {
    @Override
    public void run() {
        GregTechData.init();
    }
}
