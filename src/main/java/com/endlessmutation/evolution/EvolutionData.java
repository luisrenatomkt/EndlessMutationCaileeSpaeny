package com.endlessmutation.evolution;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Estado evolutivo puro (sem classes do Minecraft). Regra central:
 * energia e massa SÓ mudam em {@link #consumeOnce}. Nada aqui depende de tempo.
 */
public final class EvolutionData {
    public static final int FORMAT_VERSION = 1;
    private static final int RECENT_MAX = 64;

    private SciNumber energy = SciNumber.ZERO;   // energia evolutiva acumulada
    private SciNumber mass = SciNumber.ZERO;     // massa muscular acumulada
    private long meals = 0L;
    private double hunger = 50.0;                 // 0..100, só influencia comportamento de caça
    private final ArrayDeque<UUID> recent = new ArrayDeque<UUID>();

    public SciNumber getEnergy() { return energy; }
    public SciNumber getMass() { return mass; }
    public long getMeals() { return meals; }
    public double getHunger() { return hunger; }
    public List<UUID> getRecentPrey() { return new ArrayList<UUID>(recent); }

    public boolean wasConsumed(UUID id) { return id != null && recent.contains(id); }

    /**
     * Concede energia/massa UMA vez por presa (idempotente por UUID).
     * @return true se a recompensa foi aplicada agora.
     */
    public boolean consumeOnce(UUID preyId, double energyGain, double massFactor) {
        if (preyId == null || wasConsumed(preyId)) return false;
        if (Double.isNaN(energyGain) || Double.isInfinite(energyGain) || energyGain <= 0.0) return false;
        double mf = (Double.isNaN(massFactor) || Double.isInfinite(massFactor) || massFactor < 0) ? 0.0 : massFactor;
        energy = energy.add(energyGain);
        mass = mass.add(energyGain * mf);
        meals++;
        hunger = Math.max(0.0, hunger - Math.min(100.0, 10.0 + energyGain));
        recent.addLast(preyId);
        while (recent.size() > RECENT_MAX) recent.removeFirst();
        return true;
    }

    /** Fome sobe com o tempo, mas NUNCA altera energia, massa ou nível. */
    public void tickHunger(double amount) {
        if (Double.isNaN(amount) || amount <= 0) return;
        hunger = Math.min(100.0, hunger + amount);
    }

    /** log10 do nível = (log10(E) - log10(E0)) / p. Retorna -inf se E < E0 (nível 0). */
    public double levelLog10(double baseEnergy, double curveExp) {
        if (energy.isZero() || !(baseEnergy > 0) || !(curveExp > 0)) return Double.NEGATIVE_INFINITY;
        double l = (energy.log10() - Math.log10(baseEnergy)) / curveExp;
        return l < 0 ? Double.NEGATIVE_INFINITY : l;
    }

    /** Nível como número científico (sem teto lógico). */
    public SciNumber level(double baseEnergy, double curveExp) {
        double l = levelLog10(baseEnergy, curveExp);
        if (Double.isInfinite(l)) return SciNumber.ZERO;
        SciNumber n = SciNumber.fromLog10(l);
        return n.exponent() < 15 ? SciNumber.of(Math.floor(n.toDoubleSaturated())) : n;
    }

    /** Índice de crescimento g = ln(1 + massa/M0): contínuo, finito, ilimitado em termos lógicos. */
    public double growthIndex(double m0) { return mass.ln1pScaled(m0); }

    public void restore(SciNumber e, SciNumber m, long meals, double hunger, List<UUID> recentIds) {
        this.energy = e == null ? SciNumber.ZERO : e;
        this.mass = m == null ? SciNumber.ZERO : m;
        this.meals = Math.max(0L, meals);
        this.hunger = (Double.isNaN(hunger) || hunger < 0) ? 50.0 : Math.min(100.0, hunger);
        this.recent.clear();
        if (recentIds != null) for (UUID u : recentIds) if (u != null) recent.addLast(u);
        while (recent.size() > RECENT_MAX) recent.removeFirst();
    }
}
