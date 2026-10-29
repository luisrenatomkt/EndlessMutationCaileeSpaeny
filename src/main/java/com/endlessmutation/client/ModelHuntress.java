package com.endlessmutation.client;

import com.endlessmutation.config.ModConfig;
import com.endlessmutation.entity.EntityHuntress;
import com.endlessmutation.evolution.MutationSystem;
import com.endlessmutation.evolution.MutationSystem.Region;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

/**
 * Modelo articulado proprio (ModelBase/ModelRenderer). Cada peca e uma caixa independente; a hierarquia
 * (braco > antebraco > mao, coxa > panturrilha > pe) e feita com matrizes GL, de modo que cada regiao tem
 * sua propria escala calculada por MutationSystem a partir do indice de crescimento (sem listas fixas de modelos).
 * Nenhum objeto de modelo e recriado em runtime: so matrizes mudam por frame.
 * Layout de textura 64x64 documentado em tools/generate_textures.py (mesmos UVs).
 */
public class ModelHuntress extends ModelBase {
    private final ModelRenderer head, hair, hairLong, neck, body, pec, lat, trap, skirt;
    private final ModelRenderer upperArm, foreArm, hand, shoulder, thigh, calf, foot, spike;

    private float partial;
    // poses (preenchidas em setRotationAngles)
    private float headYaw, headPitch;
    private final float[] armX = new float[2], armZ = new float[2], elbow = new float[2], legX = new float[2];
    private final double[] sc = new double[MutationSystem.REGIONS];

    public ModelHuntress() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        head = box(0, 0, -4, -8, -4, 8, 8, 8, 0f);
        hair = box(32, 0, -4, -8, -4, 8, 8, 8, 0.5f);
        hairLong = box(24, 16, -4, 0, 0, 8, 12, 2, 0f);
        neck = box(52, 46, -1.5f, -1.5f, -1.5f, 3, 3, 3, 0f);
        body = box(0, 16, -4, 0, -2, 8, 12, 4, 0f);
        pec = box(32, 56, -3.5f, -2, -1.5f, 7, 4, 3, 0f);
        lat = box(0, 56, -1.5f, -3, -1, 3, 6, 2, 0f);
        trap = box(44, 36, -3, -1.5f, -2, 6, 3, 4, 0f);
        skirt = box(0, 32, -4, 0, -2, 8, 8, 4, 0.6f);
        upperArm = box(44, 16, -2, -2, -2, 4, 6, 4, 0f);
        foreArm = box(44, 26, -2, 0, -2, 4, 5, 4, 0f);
        hand = box(24, 30, -1.5f, 0, -1.5f, 3, 3, 3, 0f);
        shoulder = box(24, 36, -2.5f, -2.5f, -2.5f, 5, 5, 5, 0f);
        thigh = box(0, 44, -2, 0, -2, 4, 6, 4, 0f);
        calf = box(16, 46, -2, 0, -2, 4, 6, 4, 0f);
        foot = box(32, 46, -2, -2, -3, 4, 2, 5, 0.1f);
        spike = box(24, 56, -1, -4, -1, 2, 4, 2, 0f);
    }

    private ModelRenderer box(int u, int v, float x, float y, float z, int w, int h, int d, float inflate) {
        ModelRenderer r = new ModelRenderer(this, u, v);
        r.addBox(x, y, z, w, h, d, inflate);
        r.setRotationPoint(0f, 0f, 0f);
        return r;
    }

    @Override
    public void setLivingAnimations(EntityLivingBase e, float limbSwing, float limbSwingAmount, float partialTicks) {
        this.partial = partialTicks;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float pitch,
                                  float scale, Entity entity) {
        EntityHuntress n = (EntityHuntress) entity;
        float f = limbSwing * 0.6662F, amp = limbSwingAmount, freq = ModConfig.animationFrequency;
        headYaw = netHeadYaw * 0.017453292F;
        headPitch = pitch * 0.017453292F;

        // caminhada / corrida
        armX[0] = MathHelper.cos(f + (float) Math.PI) * 1.2F * amp;
        armX[1] = MathHelper.cos(f) * 1.2F * amp;
        legX[0] = MathHelper.cos(f) * 1.4F * amp;
        legX[1] = MathHelper.cos(f + (float) Math.PI) * 1.4F * amp;
        float breathe = MathHelper.sin(age * 0.067F * freq) * 0.04F;
        float spread = 0.05F + 0.03F * (float) (MutationSystem.regionScale(n.getRenderGrowth(partial), n.getGeneSeed(),
                Region.UPPER_ARM, ModConfig.growthIntensity, ModConfig.maxVisualScale) - 1.0);
        armZ[0] = spread + breathe;
        armZ[1] = -(spread + breathe);
        float bend = -0.12F - 0.9F * Math.min(1f, amp);
        elbow[0] = bend;
        elbow[1] = bend;

        // ataque (swingProgress e preenchido pelo RenderLivingBase)
        if (this.swingProgress > 0f) {
            float s = MathHelper.sin(this.swingProgress * (float) Math.PI);
            armX[0] = -(1.1F + s * 1.3F);
            elbow[0] = -0.3F;
        }
        // alimentacao
        if (n.getHuntState() == EntityHuntress.STATE_EAT) {
            float w = MathHelper.sin(age * 0.9F * freq);
            headPitch += 0.55F + 0.12F * w;
            armX[0] = armX[1] = -1.1F + 0.25F * w;
            armZ[0] = -0.12F;
            armZ[1] = 0.12F;
            elbow[0] = elbow[1] = -1.3F;
        }
    }

    private float w(Region r) { return (float) sc[r.ordinal()]; }
    private float l(Region r) { return (float) MutationSystem.lengthScale(sc[r.ordinal()]); }

    private void joint(float px, float py, float pz, float rx, float ry, float rz, float sx, float sy, float sz, float s) {
        GlStateManager.translate(px * s, py * s, pz * s);
        if (rz != 0f) GlStateManager.rotate(rz * 57.29578F, 0f, 0f, 1f);
        if (ry != 0f) GlStateManager.rotate(ry * 57.29578F, 0f, 1f, 0f);
        if (rx != 0f) GlStateManager.rotate(rx * 57.29578F, 1f, 0f, 0f);
        GlStateManager.scale(sx, sy, sz);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float pitch, float s) {
        setRotationAngles(limbSwing, limbSwingAmount, age, netHeadYaw, pitch, s, entity);
        EntityHuntress n = (EntityHuntress) entity;
        double g = n.getRenderGrowth(partial);
        for (Region r : Region.values())
            sc[r.ordinal()] = MutationSystem.regionScale(g, n.getGeneSeed(), r, ModConfig.growthIntensity, ModConfig.maxVisualScale);

        float tx = 0.5F * (w(Region.CORE) + w(Region.CHEST));
        float tz = 0.5F * (w(Region.CORE) + w(Region.BACK));
        float ty = l(Region.CORE);
        float armW = w(Region.UPPER_ARM), armL = l(Region.UPPER_ARM);
        float thW = w(Region.THIGH), thL = l(Region.THIGH), caW = w(Region.CALF), caL = l(Region.CALF);
        float hips = Math.max(w(Region.HIPS), tx * 0.95F);
        float hipY = 12F * ty;
        float legTotal = hipY + 6F * thL + 6F * caL;

        GlStateManager.pushMatrix();
        GlStateManager.translate(0f, -(legTotal - 24F) * s, 0f);   // mantem os pes no chao

        // ---- torso, peito, costas, trapezio
        GlStateManager.pushMatrix();
        GlStateManager.scale(tx, ty, tz);
        body.render(s);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.translate(0f, 3.5F * ty * s, -0.3F * tz * s);
        float ps = 0.8F * w(Region.CHEST);
        GlStateManager.scale(ps, 0.8F * l(Region.CHEST), ps);
        pec.render(s);
        GlStateManager.popMatrix();

        for (int side = -1; side <= 1; side += 2) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(side * 2.8F * tx * s, 4.5F * ty * s, 1.2F * tz * s);
            float bs = 0.7F * w(Region.BACK);
            GlStateManager.scale(bs, 0.9F * l(Region.BACK), bs);
            lat.render(s);
            GlStateManager.popMatrix();

            GlStateManager.pushMatrix();
            GlStateManager.translate(side * 2.5F * tx * s, 0f, 0f);
            float ts = 0.35F * w(Region.TRAPS);
            GlStateManager.scale(ts, ts, ts);
            trap.render(s);
            GlStateManager.popMatrix();
        }

        // ---- pescoco e cabeca
        GlStateManager.pushMatrix();
        GlStateManager.translate(0f, -0.5F * s, 0f);
        GlStateManager.scale(w(Region.NECK), 1f, w(Region.NECK));
        neck.render(s);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        joint(0f, -1F, 0f, headPitch, headYaw, 0f, w(Region.HEAD), w(Region.HEAD), w(Region.HEAD), s);
        head.render(s);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        hair.render(s);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.translate(0f, -0.5F * s, 2.05F * tz * s);
        GlStateManager.scale(Math.max(1f, tx * 0.95F), ty, 1f);
        hairLong.render(s);
        GlStateManager.popMatrix();

        // ---- espinhos de mutacao (quantidade visual limitada; a logica nao tem teto)
        int spikes = MutationSystem.spikeCountVisual(g, ModConfig.maxVisibleSpikes) / 2;
        float spikeScale = (float) MutationSystem.sane(0.8 + 0.1 * g, 0.8, 3.0, 0.8);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < spikes; i++) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(side * (2.5F * tx + i * 1.6F) * s, (-0.8F + i * 0.4F) * s, 0f);
                GlStateManager.rotate(side * (14f + 11f * i), 0f, 0f, 1f);
                GlStateManager.scale(spikeScale, spikeScale, spikeScale);
                spike.render(s);
                GlStateManager.popMatrix();
            }
        }

        // ---- saia/vestido (acompanha quadris e coxas)
        GlStateManager.pushMatrix();
        GlStateManager.translate(0f, 10F * ty * s, 0f);
        GlStateManager.scale(Math.max(hips, thW) , thL, Math.max(hips, thW));
        skirt.render(s);
        GlStateManager.popMatrix();

        // ---- bracos
        float armX0 = 4F * tx + 2F * armW;
        for (int i = 0; i < 2; i++) {
            float side = i == 0 ? -1f : 1f;
            GlStateManager.pushMatrix();
            joint(side * armX0, 2F * ty, 0f, armX[i], 0f, armZ[i], armW, armL, armW, s);
            upperArm.render(s);
            GlStateManager.pushMatrix();
            float sp = 0.7F * w(Region.SHOULDER) / armW;
            GlStateManager.scale(sp, sp, sp);
            shoulder.render(s);
            GlStateManager.popMatrix();
            // cotovelo
            GlStateManager.pushMatrix();
            float fW = w(Region.FOREARM), fL = l(Region.FOREARM);
            joint(0f, 4F, 0f, elbow[i], 0f, 0f, fW / armW, fL / armL, fW / armW, s);
            foreArm.render(s);
            GlStateManager.pushMatrix();
            float hW = w(Region.HAND), hL = l(Region.HAND);
            joint(0f, 5F, 0f, 0f, 0f, 0f, hW / fW, hL / fL, hW / fW, s);
            hand.render(s);
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
        }

        // ---- pernas
        float legX0 = Math.max(2F * hips, 2F * thW * 1.02F);
        for (int i = 0; i < 2; i++) {
            float side = i == 0 ? -1f : 1f;
            GlStateManager.pushMatrix();
            joint(side * legX0, hipY, 0f, legX[i], 0f, 0f, thW, thL, thW, s);
            thigh.render(s);
            GlStateManager.pushMatrix();
            joint(0f, 6F, 0f, Math.max(0f, -legX[i] * 0.3F), 0f, 0f, caW / thW, caL / thL, caW / thW, s);
            calf.render(s);
            GlStateManager.pushMatrix();
            float fw = w(Region.FOOT), fl = l(Region.FOOT);
            joint(0f, 6F, 0f, 0f, 0f, 0f, fw / caW, fl / caL, fw / caW, s);
            foot.render(s);
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
    }
}
