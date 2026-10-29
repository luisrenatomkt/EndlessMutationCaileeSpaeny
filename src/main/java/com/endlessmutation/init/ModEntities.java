package com.endlessmutation.init;

import com.endlessmutation.EndlessMutation;
import com.endlessmutation.Reference;
import com.endlessmutation.entity.EntityHuntress;

import net.minecraftforge.fml.common.registry.EntityRegistry;

/** Registro da entidade (API Forge 1.8.9): id global + ovo de invocacao + tracking. */
public final class ModEntities {
    private ModEntities() {}

    public static void register() {
        int id = EntityRegistry.findGlobalUniqueEntityId();
        // ovo de invocacao (rosa / marrom). O ovo vanilla usa este id global.
        EntityRegistry.registerGlobalEntityID(EntityHuntress.class, Reference.ENTITY_NAME, id, 0xE91E78, 0x60402A);
        // tracking: alcance 80 blocos, atualizacao a cada 3 ticks, com velocidade
        EntityRegistry.registerModEntity(EntityHuntress.class, Reference.ENTITY_NAME, 0, EndlessMutation.instance, 80, 3, true);
    }
}
