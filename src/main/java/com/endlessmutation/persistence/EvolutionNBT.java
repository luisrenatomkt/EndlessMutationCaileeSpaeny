package com.endlessmutation.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.endlessmutation.evolution.EvolutionData;
import com.endlessmutation.evolution.SciNumber;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** Serializacao NBT (1.8.9). Nunca grava referencias a objetos; so numeros e UUIDs. */
public final class EvolutionNBT {
    private EvolutionNBT() {}

    public static NBTTagCompound write(EvolutionData d) {
        NBTTagCompound t = new NBTTagCompound();
        t.setInteger("FormatVersion", EvolutionData.FORMAT_VERSION);
        t.setDouble("EnergyM", d.getEnergy().mantissa());
        t.setLong("EnergyE", d.getEnergy().exponent());
        t.setDouble("MassM", d.getMass().mantissa());
        t.setLong("MassE", d.getMass().exponent());
        t.setLong("Meals", d.getMeals());
        t.setDouble("Hunger", d.getHunger());
        NBTTagList list = new NBTTagList();
        for (UUID u : d.getRecentPrey()) {
            NBTTagCompound c = new NBTTagCompound();
            c.setLong("M", u.getMostSignificantBits());
            c.setLong("L", u.getLeastSignificantBits());
            list.appendTag(c);
        }
        t.setTag("RecentPrey", list);
        return t;
    }

    public static void read(NBTTagCompound t, EvolutionData d) {
        if (t == null) return;
        SciNumber e = t.hasKey("EnergyM") ? SciNumber.of(t.getDouble("EnergyM"), t.getLong("EnergyE")) : SciNumber.ZERO;
        SciNumber m = t.hasKey("MassM") ? SciNumber.of(t.getDouble("MassM"), t.getLong("MassE")) : SciNumber.ZERO;
        double hunger = t.hasKey("Hunger") ? t.getDouble("Hunger") : 50.0;
        List<UUID> recent = new ArrayList<UUID>();
        if (t.hasKey("RecentPrey", 9)) {
            NBTTagList l = t.getTagList("RecentPrey", 10);
            for (int i = 0; i < l.tagCount(); i++) {
                NBTTagCompound c = l.getCompoundTagAt(i);
                recent.add(new UUID(c.getLong("M"), c.getLong("L")));
            }
        }
        d.restore(e, m, t.getLong("Meals"), hunger, recent);
    }
}
