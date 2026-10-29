package com.endlessmutation;

import com.endlessmutation.evolution.*;
import com.endlessmutation.evolution.MutationSystem.Region;
import java.util.*;

/** Testes isolados (sem JUnit/Minecraft). Execute: gradlew selfTest. Sai com código 1 se algo falhar. */
public class EvolutionSelfTest {
    static int pass = 0, fail = 0;
    static void check(String name, boolean ok) {
        if (ok) { pass++; System.out.println("PASS  " + name); }
        else { fail++; System.out.println("FAIL  " + name); }
    }

    public static void main(String[] a) {
        check("SciNumber rejeita NaN/Inf/negativo", SciNumber.of(Double.NaN).isZero() && SciNumber.of(Double.POSITIVE_INFINITY).isZero() && SciNumber.of(-5).isZero());
        check("SciNumber soma 1e300+1e300=2e300", Math.abs(SciNumber.of(1e300).add(1e300).mantissa() - 2.0) < 1e-9 && SciNumber.of(1e300).add(1e300).exponent() == 300);
        check("SciNumber ignora parcela irrelevante", SciNumber.of(1e100).add(1.0).equals(SciNumber.of(1e100)));
        check("SciNumber ordem", SciNumber.of(5).compareTo(SciNumber.of(50)) < 0 && SciNumber.ZERO.compareTo(SciNumber.of(1)) < 0);

        EvolutionData d = new EvolutionData();
        d.tickHunger(1e9);
        for (int i = 0; i < 100000; i++) d.tickHunger(0.5);
        check("Tempo/fome nao alteram energia", d.getEnergy().isZero());
        check("Tempo/fome nao alteram massa", d.getMass().isZero());
        check("Tempo/fome nao alteram nivel", d.level(10, 1.6).isZero() && d.getMeals() == 0);
        check("Fome limitada a 100", d.getHunger() <= 100.0);

        UUID u = UUID.randomUUID();
        double hungerBefore = d.getHunger();
        check("Primeira refeicao concede", d.consumeOnce(u, 10, 0.6));
        SciNumber e1 = d.getEnergy();
        check("Segunda com mesmo UUID NAO concede", !d.consumeOnce(u, 10, 0.6) && d.getEnergy().equals(e1) && d.getMeals() == 1);
        check("Energia invalida (NaN/neg/Inf) rejeitada", !d.consumeOnce(UUID.randomUUID(), Double.NaN, 1) && !d.consumeOnce(UUID.randomUUID(), -3, 1) && !d.consumeOnce(UUID.randomUUID(), Double.POSITIVE_INFINITY, 1) && d.getMeals() == 1);
        check("Refeicao reduz a fome", d.getHunger() < hungerBefore);

        EvolutionData r = new EvolutionData();
        r.restore(d.getEnergy(), d.getMass(), d.getMeals(), d.getHunger(), d.getRecentPrey());
        check("Restore preserva energia/massa/refeicoes", r.getEnergy().equals(d.getEnergy()) && r.getMass().equals(d.getMass()) && r.getMeals() == d.getMeals());
        check("Restore preserva idempotencia", r.wasConsumed(u) && !r.consumeOnce(u, 10, 0.6));
        EvolutionData bad = new EvolutionData();
        bad.restore(null, null, -5, Double.NaN, null);
        check("Restore com dados ausentes usa padroes seguros", bad.getEnergy().isZero() && bad.getMeals() == 0 && bad.getHunger() == 50.0);

        EvolutionData m = new EvolutionData();
        double prevG = -1, prevLv = -1; boolean mono = true, finite = true;
        for (int i = 0; i < 2000; i++) {
            m.consumeOnce(UUID.randomUUID(), 10, 2.0);
            double g = m.growthIndex(2.0);
            double lv = m.levelLog10(10, 1.6);
            if (g < prevG) mono = false;
            if (Double.isNaN(g) || Double.isInfinite(g)) finite = false;
            prevG = g; if (!Double.isInfinite(lv)) { if (lv < prevLv) mono = false; prevLv = lv; }
        }
        check("g e nivel monotonicos e finitos (2000 refeicoes)", mono && finite);
        check("Cada refeicao aumenta massa", m.getMeals() == 2000 && m.getMass().compareTo(SciNumber.of(1000)) > 0);

        EvolutionData x = new EvolutionData();
        for (int i = 0; i < 1000; i++) x.consumeOnce(UUID.randomUUID(), 1e300, 1.0);
        for (int i = 0; i < 1000; i++) x.consumeOnce(UUID.randomUUID(), 1.7e308, 1.0);
        double gx = x.growthIndex(2.0);
        check("Energia > Double.MAX continua finita em SciNumber", x.getEnergy().exponent() > 308 && !x.getEnergy().isZero());
        check("g finito em energia extrema", !Double.isNaN(gx) && !Double.isInfinite(gx) && gx > 600);
        check("nivel extremo calculavel", !x.level(10, 1.6).isZero() && !Double.isInfinite(x.levelLog10(10, 1.6)));
        double[] sc = MutationSystem.allScales(gx, 12345, 1.0, 3.5);
        boolean ok = true; for (double s : sc) if (Double.isNaN(s) || Double.isInfinite(s) || s < 1.0 || s > 3.5) ok = false;
        check("Escalas por regiao finitas e dentro do limite visual", ok);
        check("Escala visual satura sem encerrar progressao", sc[Region.UPPER_ARM.ordinal()] == 3.5 && MutationSystem.spikeCountLogical(gx) > 500);
        check("Multiplicadores finitos e limitados", MutationSystem.strengthMultiplier(gx, 1, 1e6) <= 1e6 && MutationSystem.speedMultiplier(gx, 1, 3.0) <= 3.0 && MutationSystem.attackCooldown(gx, 16) >= 4);
        check("Entrada NaN gera escala 1.0", MutationSystem.regionScale(Double.NaN, 1, Region.CHEST, 1, 3.5) == 1.0 && MutationSystem.entityScale(Double.NaN, 2.4) == 1.0);
        check("Sem alimentacao: escalas = 1.0", MutationSystem.regionScale(0.0, 77, Region.THIGH, 1, 3.5) == 1.0);

        double gs = 0; boolean inc = true; double prev = 1.0;
        for (int i = 0; i < 50; i++) { gs += 0.5; double s = MutationSystem.regionScale(gs, 9, Region.UPPER_ARM, 1, 100); if (s < prev) inc = false; prev = s; }
        check("Escala do braco cresce com g", inc && prev > 1.5);
        check("Genes por individuo diferem entre seeds", MutationSystem.regionScale(5, 1, Region.CHEST, 1, 9) != MutationSystem.regionScale(5, 2, Region.CHEST, 1, 9));
        check("Cooldown de ataque diminui", MutationSystem.attackCooldown(10, 16) < MutationSystem.attackCooldown(0, 16));
        check("Forca cresce com g", MutationSystem.strengthMultiplier(10, 1, 1e6) > MutationSystem.strengthMultiplier(1, 1, 1e6));

        System.out.println("\nResultado: " + pass + " passaram, " + fail + " falharam.");
        if (fail > 0) System.exit(1);
    }
}
