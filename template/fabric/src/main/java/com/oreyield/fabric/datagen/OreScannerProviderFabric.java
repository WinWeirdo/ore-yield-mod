package com.oreyield.fabric.datagen;

import com.oreyield.datagen.OreScannerProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public final class OreScannerProviderFabric implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        generator.createPack().addProvider((output, registriesFuture) -> new OreScannerProvider(output));
    }
}
