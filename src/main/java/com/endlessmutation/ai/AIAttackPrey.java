package com.endlessmutation.ai;

import com.endlessmutation.entity.EntityHuntress;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

/** Ataca a presa quando em alcance e com linha de visao; o cooldown e aplicado pela entidade. */
public class AIAttackPrey extends EntityAIBase {
    private final EntityHuntress npc;

    public AIAttackPrey(EntityHuntress npc) {
        this.npc = npc;
        this.setMutexBits(2);
    }

    @Override
    public boolean shouldExecute() {
        return !npc.worldObj.isRemote && !npc.isFeeding() && npc.getPrey() != null;
    }

    @Override
    public boolean continueExecuting() { return shouldExecute(); }

    @Override
    public void updateTask() {
        EntityLivingBase p = npc.getPrey();
        if (p == null) return;
        npc.getLookHelper().setLookPositionWithEntity(p, 30.0F, 30.0F);
        if (npc.getDistanceSqToEntity(p) <= npc.getReachSq(p) && npc.getEntitySenses().canSee(p)) {
            npc.attackPrey(p);
        }
    }
}
