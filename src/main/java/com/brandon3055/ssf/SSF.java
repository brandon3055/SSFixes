package com.brandon3055.ssf;

import com.brandon3055.ssf.modules.ModuleBagDupeFix;
import com.brandon3055.ssf.modules.ModuleLagHunter;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

@Mod(modid = SSF.MODID, name = SSF.MODNAME,version = SSF.VERSION)
public class SSF
{
    public static final String MODID = "ssfixes";
	public static final String MODNAME = "Server Side Fixes";
    public static final String VERSION = "${mod_version}";
    public static File modConfigDir;
    public static Configuration configuration;

    private void registerModules() {
        ModuleRegistry.register(new ModuleBagDupeFix());
        ModuleRegistry.register(new ModuleLagHunter());
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        modConfigDir = new File(event.getModConfigurationDirectory(), "brandon3055/ServerSideFixes");
        if (!modConfigDir.exists()) {
            modConfigDir.mkdirs();
        }
        configuration = new Configuration(new File(modConfigDir, "ServerSideFixes.cfg"));
        registerModules();
        ModuleRegistry.loadModules(configuration);
        MinecraftForge.EVENT_BUS.register(new ModEventHandler());
    }
}
