package com.endlessmutation.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.endlessmutation.config.ModConfig;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ambient.EntityBat;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;

/**
 * Regras de presa. Hierarquia real em 1.8.9: Cow, Mooshroom(extends Cow), Sheep, Pig, Chicken, Rabbit,
 * Horse (inclui burro/mula: tipos de EntityHorse), Wolf e Ocelot (EntityTameable) herdam de EntityAnimal.
 * Bat (EntityAmbientCreature) e Squid (EntityWaterMob) NAO herdam de EntityAnimal e sao tratados a parte;
 * nomes extras podem ser adicionados na configuracao (extraPrey).
 */
public final class PreyRules {
    private PreyRules() {}

    public static boolean isValidPrey(EntityHuntress npc, EntityLivingBase e) {
        if (e == null || e == npc) return false;
        if (e instanceof EntityPlayer || e instanceof EntityHuntress) return false;
        if (e.isDead || e.getHealth() <= 0f) return false;
        if (ModConfig.listed(ModConfig.excludedPrey, e)) return false;
        boolean kind = e instanceof EntityAnimal || e instanceof EntityBat || e instanceof EntitySquid
                || ModConfig.listed(ModConfig.extraPrey, e);
        if (!kind || e instanceof EntityMob) return false;
        if (e instanceof EntityWolf && ((EntityWolf) e).isTamed() && !ModConfig.huntTamedWolves) return false;
        if (ModConfig.skipRiddenMounts && e.riddenByEntity instanceof EntityPlayer) return false;
        return true;
    }

    /** Busca por AABB (nunca global), com pontuacao, e checagem de caminho apenas nos melhores candidatos. */
    public static EntityLivingBase findBest(final EntityHuntress npc) {
        double r = ModConfig.searchRadius * (1.0 + 0.5 * npc.getEvolution().getHunger() / 100.0);
        double rv = Math.min(r, 12.0);
        List<EntityLivingBase> all = npc.worldObj.getEntitiesWithinAABB(EntityLivingBase.class,
                npc.getEntityBoundingBox().expand(r, rv, r));
        final List<EntityLivingBase> cands = new ArrayList<EntityLivingBase>();
        for (EntityLivingBase e : all) {
            if (!isValidPrey(npc, e) || npc.isBlacklisted(e.getUniqueID())) continue;
            if (npc.getDistanceSqToEntity(e) > r * r) continue;
            cands.add(e);
        }
        if (cands.isEmpty()) return null;

        UUID pending = npc.consumePendingPrey();
        Collections.sort(cands, new Comparator<EntityLivingBase>() {
            public int compare(EntityLivingBase a, EntityLivingBase b) {
                return Double.compare(score(npc, a), score(npc, b));
            }
        });
        if (pending != null) {
            for (EntityLivingBase c : cands) if (pending.equals(c.getUniqueID())) { cands.remove(c); cands.add(0, c); break; }
        }
        int checked = 0;
        for (EntityLivingBase c : cands) {
            if (checked++ >= 5) break;
            if (!ModConfig.requirePath || reachable(npc, c)) return c;
            npc.blacklist(c.getUniqueID(), 400);
        }
        return null;
    }

    private static double score(EntityHuntress npc, EntityLivingBase e) {
        return npc.getDistanceSqToEntity(e) / ModConfig.priorityWeight(e);
    }

    public static boolean reachable(EntityHuntress npc, EntityLivingBase t) {
        PathEntity p = npc.getNavigator().getPathToEntityLiving(t);
        if (p == null) return false;
        PathPoint f = p.getFinalPathPoint();
        if (f == null) return false;
        double dx = f.xCoord + 0.5 - t.posX, dz = f.zCoord + 0.5 - t.posZ, dy = f.yCoord - t.posY;
        return dx * dx + dz * dz <= 2.5 * 2.5 && Math.abs(dy) <= 3.0;
    }
}
