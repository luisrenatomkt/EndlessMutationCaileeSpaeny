package com.endlessmutation.ai;

import com.endlessmutation.entity.EntityHuntress;

import net.minecraft.entity.ai.EntityAIBase;

/** Sequencia de alimentacao (parada + olhar para baixo). A recompensa ja foi concedida pela entidade, uma unica vez. */
public class AIEatPrey extends EntityAIBase {
    private final EntityHuntress npc;

    public AIEatPrey(EntityHuntress npc) {
        this.npc = npc;
        this.setMutexBits(7);
    }

    @Override public boolean shouldExecute() { return npc.isFeeding(); }
    @Override public boolean continueExecuting() { return npc.isFeeding(); }

    @Override
    public void startExecuting() {
        npc.getNavigator().clearPathEntity();
        npc.setSprinting(false);
    }

    @Override
    public void updateTask() {
        npc.getLookHelper().setLookPosition(npc.posX, npc.posY - 1.0D, npc.posZ + 0.01D, 10.0F, 40.0F);
    }
}
