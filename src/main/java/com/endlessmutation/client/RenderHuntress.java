package com.endlessmutation.client;

import com.endlessmutation.Reference;
import com.endlessmutation.config.ModConfig;
import com.endlessmutation.entity.EntityHuntress;
import com.endlessmutation.evolution.MutationSystem;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class RenderHuntress extends RenderLiving<EntityHuntress> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Reference.MODID, "textures/entity/huntress.png");

    public RenderHuntress(RenderManager manager) {
        super(manager, new ModelHuntress(), 0.5F);
    }

    @Override
    protected void preRenderCallback(EntityHuntress e, float partialTicks) {
        float s = (float) MutationSystem.entityScale(e.getRenderGrowth(partialTicks), ModConfig.maxEntityScale);
        GlStateManager.scale(s, s, s);
        this.shadowSize = 0.5F * s;
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityHuntress e) { return TEXTURE; }
}
