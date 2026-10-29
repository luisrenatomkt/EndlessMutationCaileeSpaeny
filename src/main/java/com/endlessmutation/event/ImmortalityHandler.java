package com.endlessmutation.event;

import com.endlessmutation.config.ModConfig;
import com.endlessmutation.entity.EntityHuntress;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Camada extra de imortalidade (somente para EntityHuntress) + descarte dos drops das presas devoradas. */
public class ImmortalityHandler {
    @SubscribeEvent
    public void onAttack(LivingAttackEvent e) {
        if (e.entityLiving instanceof EntityHuntress) e.setCanceled(true);
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent e) {
        if (e.entityLiving instanceof EntityHuntress) e.setCanceled(true);
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent e) {
        if (e.entityLiving instanceof EntityHuntress) e.setCanceled(true);
    }

    @SubscribeEvent
    public void onDrops(LivingDropsEvent e) {
        if (ModConfig.consumeDrops && e.source != null && e.source.getEntity() instanceof EntityHuntress) {
            e.setCanceled(true);   // a presa foi devorada
        }
    }
}
