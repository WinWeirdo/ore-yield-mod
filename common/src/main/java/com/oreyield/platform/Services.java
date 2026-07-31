package com.oreyield.platform;

import java.util.ServiceLoader;

public final class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz).findFirst()
                .orElseThrow(() -> new IllegalStateException("No platform implementation for " + clazz.getName()));
    }

    private Services() {}
}
