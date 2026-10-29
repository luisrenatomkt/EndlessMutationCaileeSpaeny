package com.endlessmutation.proxy;

import com.endlessmutation.Reference;
import com.endlessmutation.client.RenderHuntress;
import com.endlessmutation.entity.EntityHuntress;
import com.endlessmutation.init.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent e) {
        super.preInit(e);
        RenderingRegistry.registerEntityRenderingHandler(EntityHuntress.class, new IRenderFactory<EntityHuntress>() {
            @Override
            public Render<? super EntityHuntress> createRenderFor(RenderManager manager) {
                return new RenderHuntress(manager);
            }
        });
    }

    @Override
    public void init(FMLInitializationEvent e) {
        super.init(e);
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(ModItems.summoner, 0,
                new ModelResourceLocation(Reference.MODID + ":summoner", "inventory"));
    }
}
