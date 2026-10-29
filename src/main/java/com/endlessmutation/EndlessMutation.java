package com.endlessmutation;

import com.endlessmutation.command.CommandEndless;
import com.endlessmutation.config.ModConfig;
import com.endlessmutation.init.ModEntities;
import com.endlessmutation.init.ModItems;
import com.endlessmutation.proxy.CommonProxy;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = Reference.MODID, name = Reference.NAME, version = Reference.VERSION, acceptedMinecraftVersions = Reference.MC_VERSION)
public class EndlessMutation {
    @Mod.Instance(Reference.MODID)
    public static EndlessMutation instance;

    @SidedProxy(clientSide = "com.endlessmutation.proxy.ClientProxy", serverSide = "com.endlessmutation.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        ModConfig.load(e.getSuggestedConfigurationFile());
        ModEntities.register();
        ModItems.register();
        proxy.preInit(e);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        proxy.init(e);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        e.registerServerCommand(new CommandEndless());
    }
}
