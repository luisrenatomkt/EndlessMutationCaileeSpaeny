package com.endlessmutation.proxy;

import com.endlessmutation.event.ImmortalityHandler;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent e) { }

    public void init(FMLInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(new ImmortalityHandler());
    }
}
