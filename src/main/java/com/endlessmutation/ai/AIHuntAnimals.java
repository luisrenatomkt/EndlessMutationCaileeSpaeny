package com.endlessmutation.ai;

import com.endlessmutation.entity.EntityHuntress;
import com.endlessmutation.entity.PreyRules;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

/** Procura (com intervalo) e persegue a presa; abandona alvos impossiveis de alcancar. */
public class AIHuntAnimals extends EntityAIBase {
    private final EntityHuntress npc;
    private double bestDist;
    private int stuckChecks, chaseTicks;

    public AIHuntAnimals(EntityHuntress npc) {
        this.npc = npc;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (npc.isFeeding() || npc.worldObj.isRemote) return false;
        if (npc.hasPreyAssigned()) return npc.getPrey() != null;
        if (!npc.searchDue()) return false;
        npc.scheduleNextSearch();
        EntityLivingBase p = PreyRules.findBest(npc);
        if (p == null) return false;
        npc.setPrey(p);
        return true;
    }

    @Override
    public boolean continueExecuting() {
        return !npc.isFeeding() && npc.getPrey() != null;
    }

    @Override
    public void startExecuting() {
        bestDist = Double.MAX_VALUE;
        stuckChecks = 0;
        chaseTicks = 0;
        npc.setSprinting(true);
        EntityLivingBase p = npc.getPrey();
        if (p != null) npc.getNavigator().tryMoveToEntityLiving(p, 1.15D);
    }

    @Override
    public void updateTask() {
        EntityLivingBase p = npc.getPrey();
        if (p == null) return;
        chaseTicks++;
        npc.getLookHelper().setLookPositionWithEntity(p, 30.0F, 30.0F);
        if (chaseTicks % 10 == 0) npc.getNavigator().tryMoveToEntityLiving(p, 1.15D);
        if (chaseTicks % 20 == 0) {
            double d = npc.getDistanceSqToEntity(p);
            if (d > bestDist - 0.5) stuckChecks++; else { stuckChecks = 0; bestDist = d; }
            if (stuckChecks >= 4 || chaseTicks > 900) {      // sem progresso por 4 s ou perseguicao longa demais
                npc.blacklist(p.getUniqueID(), 600);
                npc.clearPrey();
            }
        }
    }

    @Override
    public void resetTask() {
        npc.getNavigator().clearPathEntity();
        npc.setSprinting(false);
    }
}
