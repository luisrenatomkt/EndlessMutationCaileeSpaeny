package com.endlessmutation.evolution;

/**
 * Número não negativo em notação científica (mantissa double + expoente long).
 * Permite acumular energia/massa muito além do limite do double (1.8e308) sem
 * overflow, NaN ou infinito. Imutável; usado apenas em eventos raros (refeições),
 * nunca em cálculos por tick.
 */
public final class SciNumber implements Comparable<SciNumber> {
    public static final SciNumber ZERO = new SciNumber(0.0, 0L);
    private static final double LN10 = Math.log(10.0);

    private final double m; // 0, ou [1,10)
    private final long e;

    private SciNumber(double m, long e) { this.m = m; this.e = e; }

    private static SciNumber norm(double m, long e) {
        if (!(m > 0.0) || Double.isInfinite(m)) return ZERO;
        while (m >= 10.0) { m /= 10.0; e++; }
        while (m < 1.0) { m *= 10.0; e--; }
        return new SciNumber(m, e);
    }

    /** Valores inválidos (NaN, infinito, negativo, ~0) viram ZERO. */
    public static SciNumber of(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 1e-300) return ZERO;
        return norm(v, 0L);
    }

    public static SciNumber of(double mantissa, long exponent) {
        if (Double.isNaN(mantissa) || Double.isInfinite(mantissa) || mantissa <= 0.0) return ZERO;
        return norm(mantissa, exponent);
    }

    public static SciNumber fromLog10(double l) {
        if (Double.isNaN(l) || Double.isInfinite(l)) return ZERO;
        double fl = Math.floor(l);
        return norm(Math.pow(10.0, l - fl), (long) fl);
    }

    public boolean isZero() { return m == 0.0; }
    public double mantissa() { return m; }
    public long exponent() { return e; }

    public SciNumber add(SciNumber o) {
        if (o.isZero()) return this;
        if (isZero()) return o;
        SciNumber big = compareTo(o) >= 0 ? this : o;
        SciNumber small = big == this ? o : this;
        long d = big.e - small.e;
        if (d > 17) return big;
        return norm(big.m + small.m * Math.pow(10.0, -d), big.e);
    }

    public SciNumber add(double v) { return add(of(v)); }

    public SciNumber multiply(double k) {
        if (isZero() || Double.isNaN(k) || Double.isInfinite(k) || k <= 0.0) return ZERO;
        return norm(m * k, e);
    }

    /** log10; ZERO retorna -infinito (chamadores tratam). */
    public double log10() {
        if (isZero()) return Double.NEGATIVE_INFINITY;
        return e + Math.log10(m);
    }

    /** ln(1 + this/scale), sempre finito para scale>0. */
    public double ln1pScaled(double scale) {
        if (isZero() || !(scale > 0.0) || Double.isInfinite(scale)) return 0.0;
        double t = log10() - Math.log10(scale);
        if (t > 15.0) return t * LN10;
        return Math.log1p(Math.pow(10.0, t));
    }

    /** Conversão saturada (apenas para exibição). */
    public double toDoubleSaturated() {
        if (isZero()) return 0.0;
        if (e > 307) return Double.MAX_VALUE;
        if (e < -300) return 0.0;
        return m * Math.pow(10.0, e);
    }

    @Override public int compareTo(SciNumber o) {
        if (isZero() || o.isZero()) return isZero() ? (o.isZero() ? 0 : -1) : 1;
        if (e != o.e) return e < o.e ? -1 : 1;
        return Double.compare(m, o.m);
    }

    @Override public boolean equals(Object o) {
        return o instanceof SciNumber && compareTo((SciNumber) o) == 0;
    }
    @Override public int hashCode() { return Double.hashCode(m) * 31 + Long.hashCode(e); }

    @Override public String toString() {
        if (isZero()) return "0";
        if (e < 9 && e >= 0) {
            double v = m * Math.pow(10.0, e);
            if (v >= 1000) return String.format(java.util.Locale.ROOT, "%.0f", v);
            return String.format(java.util.Locale.ROOT, "%.1f", v);
        }
        return String.format(java.util.Locale.ROOT, "%.2fe%d", m, e);
    }
}
