package com.brandon3055.ssf;

import com.brandon3055.ssf.modules.SSModuleBase;
import net.minecraftforge.common.config.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by brandon3055 on 27/11/2016.
 */
public class ModuleRegistry {
    private static final Map<SSModuleBase, Boolean> REGISTRY = new HashMap<SSModuleBase, Boolean>();
    private static boolean registryInitialized = false;

    public static void register(SSModuleBase module) {
        register(module, true);
    }

    public static void register(SSModuleBase module, boolean enabledByDefault) {
        if (registryInitialized) {
            throw new RuntimeException("Modules must be registered in preInit before the registry is initialized!");
        }
        REGISTRY.put(module, enabledByDefault);
    }

    public static void loadModules(Configuration config) {
        config.setCategoryComment("Modules", "This section allows you do disable/enable any of the available SSF Modules.");
        for (SSModuleBase module : REGISTRY.keySet()) {
            boolean enabled = config.get("Modules", module.moduleID, REGISTRY.get(module), module.moduleDescription).getBoolean(REGISTRY.get(module));
            REGISTRY.put(module, enabled);

            if (enabled) {
                module.loadConfig(config);
                module.initialize();
            }
        }

        if (config.hasChanged()) {
            config.save();
        }

        registryInitialized = true;
    }
}
