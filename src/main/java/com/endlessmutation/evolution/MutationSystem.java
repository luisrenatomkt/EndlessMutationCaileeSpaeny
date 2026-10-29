package com.endlessmutation.evolution;

/**
 * Fórmulas de mutação (puras, sem Minecraft). Converte o índice de crescimento g
 * em escalas por região, multiplicadores de combate e contagem de efeitos.
 * A progressão lógica não tem teto; apenas a ESCALA VISUAL é limitada (parâmetro maxScale).
 */
public final class MutationSystem {
    private MutationSystem() {}

    public enum Region {
        HEAD(0.03), NECK(0.28), TRAPS(0.42), SHOULDER(0.38), CHEST(0.34), BACK(0.34),
        UPPER_ARM(0.34), FOREARM(0.26), HAND(0.12), CORE(0.16), HIPS(0.20),
        THIGH(0.32), CALF(0.22), FOOT(0.08);
        public final double weight;
        Region(double w) { weight = w; }
    }

    public static final int REGIONS = Region.values().length;

    private static double hash01(int seed, int salt) {
        long z = (seed * 0x9E3779B97F4A7C15L) + (salt + 1) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= (z >>> 31);
        return ((z >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }

    public static double sane(double v, double lo, double hi, double fallback) {
        if (Double.isNaN(v)) return fallback;
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    /** Escala (largura/espessura) de uma região. Sempre finita e em [1, maxScale]. */
    public static double regionScale(double g, int seed, Region r, double intensity, double maxScale) {
        double gg = sane(g, 0.0, 1e9, 0.0);
        double max = sane(maxScale, 1.0, 50.0, 3.5);
        double inten = sane(intensity, 0.0, 100.0, 1.0);
        double gene = 0.85 + 0.30 * hash01(seed, r.ordinal());
        double raw = 1.0 + r.weight * gene * inten * Math.pow(gg, 0.9);
        return sane(raw, 1.0, max, 1.0);
    }

    public static double[] allScales(double g, int seed, double intensity, double maxScale) {
        double[] out = new double[REGIONS];
        for (Region r : Region.values()) out[r.ordinal()] = regionScale(g, seed, r, intensity, maxScale);
        return out;
    }

    /** Escala do comprimento (muito menor que a espessura). */
    public static double lengthScale(double widthScale) {
        return 1.0 + (sane(widthScale, 1.0, 50.0, 1.0) - 1.0) * 0.18;
    }

    /** Escala uniforme da entidade (hitbox e renderização geral). */
    public static double entityScale(double g, double maxEntityScale) {
        double max = sane(maxEntityScale, 1.0, 6.0, 2.4);
        return sane(1.0 + 0.05 * Math.pow(sane(g, 0, 1e9, 0), 0.9), 1.0, max, 1.0);
    }

    public static double strengthMultiplier(double g, double mutationMult, double cap) {
        double v = 1.0 + 0.55 * Math.pow(sane(g, 0, 1e9, 0), 1.15) * sane(mutationMult, 0, 100, 1);
        return sane(v, 1.0, sane(cap, 1.0, 1e9, 1e6), 1.0);
    }

    public static double speedMultiplier(double g, double mutationMult, double cap) {
        double v = 1.0 + 0.08 * Math.pow(sane(g, 0, 1e9, 0), 0.8) * sane(mutationMult, 0, 100, 1);
        return sane(v, 1.0, sane(cap, 1.0, 10.0, 3.0), 1.0);
    }

    /** Cooldown de ataque em ticks (menor com a evolução, mínimo 4). */
    public static int attackCooldown(double g, int baseTicks) {
        double v = baseTicks / (1.0 + 0.10 * Math.pow(sane(g, 0, 1e9, 0), 0.7));
        return (int) Math.max(4, Math.round(sane(v, 4, 1e6, baseTicks)));
    }

    /** Quantidade LÓGICA de espinhos (sem teto) e quantidade visual (limitada). */
    public static long spikeCountLogical(double g) { return (long) Math.floor(sane(g, 0, 1e9, 0) * 0.9); }
    public static int spikeCountVisual(double g, int maxVisual) {
        return (int) Math.min(Math.max(0, maxVisual), spikeCountLogical(g));
    }
}
