package com.endlessmutation.config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.common.config.Configuration;

/**
 * Configuração (Forge 1.8.9 Configuration). Todos os valores são validados por
 * faixas min/max. NÃO existe nível máximo configurável: só limites de renderização
 * e de segurança numérica (dano/velocidade finitos).
 */
public final class ModConfig {
    private ModConfig() {}

    // Caça
    public static float searchRadius = 24f;
    public static int searchInterval = 40;          // ticks entre buscas
    public static boolean requirePath = true;       // só alvos com caminho viável
    public static boolean huntTamedWolves = true;
    public static boolean skipRiddenMounts = true;
    public static boolean consumeDrops = true;      // presa devorada não solta itens
    public static String[] preyPriority = {"Cow", "Pig", "Sheep"};
    public static String[] excludedPrey = {};
    public static String[] extraPrey = {};
    public static String[] energyPerPrey = {"Cow=10", "Sheep=8", "Pig=8", "Chicken=3", "Rabbit=3",
            "EntityHorse=20", "Wolf=10", "Ozelot=8", "MushroomCow=12", "Bat=2", "Squid=4"};
    public static float defaultEnergy = 4f;         // + 0.5 * vida máx. quando o tipo não está listado
    public static int feedTicks = 60;
    public static float hungerRate = 0.5f;          // por 20 ticks

    // Combate / atributos iniciais
    public static float baseSpeed = 0.28f;
    public static float baseAttack = 4f;
    public static int attackCooldown = 16;

    // Evolução
    public static float levelBaseEnergy = 10f;
    public static float levelCurveExponent = 1.6f;
    public static float massFactor = 0.6f;
    public static float growthMassScale = 2f;
    public static float growthIntensity = 1f;
    public static float mutationMultiplier = 1f;

    // Limites de segurança (NÃO limitam a progressão lógica)
    public static float maxVisualScale = 3.5f;
    public static float maxEntityScale = 2.4f;
    public static int maxVisibleSpikes = 6;
    public static float maxSpeedMultiplier = 3f;
    public static float maxAttackDamage = 100000f;

    // Cliente / outros
    public static float animationFrequency = 1f;
    public static boolean particles = true;
    public static boolean enableDebugCommands = false;

    private static final Map<String, Double> ENERGY = new HashMap<String, Double>();
    private static final Map<String, Integer> PRIORITY = new HashMap<String, Integer>();

    public static void load(File file) {
        Configuration c = new Configuration(file);
        try {
            c.load();
            String h = "hunting", e = "evolution", s = "safety", m = "misc";
            searchRadius = c.getFloat("searchRadius", h, searchRadius, 4f, 96f, "Raio de busca de presas (blocos)");
            searchInterval = c.getInt("searchInterval", h, searchInterval, 10, 400, "Intervalo entre buscas (ticks)");
            requirePath = c.getBoolean("requirePath", h, requirePath, "Só escolhe presas com caminho viavel");
            huntTamedWolves = c.getBoolean("huntTamedWolves", h, huntTamedWolves, "Lobos domesticados sao presas validas");
            skipRiddenMounts = c.getBoolean("skipRiddenMounts", h, skipRiddenMounts, "Ignora montarias com jogador montado");
            consumeDrops = c.getBoolean("consumeDrops", h, consumeDrops, "Presa devorada nao solta itens");
            preyPriority = c.getStringList("preyPriority", h, preyPriority, "Nomes de entidade em ordem de prioridade");
            excludedPrey = c.getStringList("excludedPrey", h, excludedPrey, "Nomes de entidade que nunca sao presa");
            extraPrey = c.getStringList("extraPrey", h, extraPrey, "Nomes de entidade adicionais tratados como presa");
            energyPerPrey = c.getStringList("energyPerPrey", h, energyPerPrey, "Formato Nome=energia");
            defaultEnergy = c.getFloat("defaultEnergy", h, defaultEnergy, 0.1f, 1000f, "Energia base p/ tipos nao listados");
            feedTicks = c.getInt("feedTicks", h, feedTicks, 20, 400, "Duracao da animacao de alimentacao");
            hungerRate = c.getFloat("hungerRate", h, hungerRate, 0f, 10f, "Fome ganha por segundo (so afeta a busca)");
            baseSpeed = c.getFloat("baseSpeed", h, baseSpeed, 0.1f, 0.6f, "Velocidade inicial");
            baseAttack = c.getFloat("baseAttack", h, baseAttack, 0.5f, 1000f, "Ataque inicial");
            attackCooldown = c.getInt("attackCooldown", h, attackCooldown, 4, 100, "Cooldown base de ataque (ticks)");

            levelBaseEnergy = c.getFloat("levelBaseEnergy", e, levelBaseEnergy, 1f, 1e6f, "Energia do nivel 1");
            levelCurveExponent = c.getFloat("levelCurveExponent", e, levelCurveExponent, 1.05f, 6f, "Curva: nivel = (E/E0)^(1/exp). Sem nivel maximo");
            massFactor = c.getFloat("massFactor", e, massFactor, 0f, 100f, "Massa muscular por energia");
            growthMassScale = c.getFloat("growthMassScale", e, growthMassScale, 0.01f, 1e4f, "M0 em g = ln(1+massa/M0)");
            growthIntensity = c.getFloat("growthIntensity", e, growthIntensity, 0f, 20f, "Intensidade do crescimento visual");
            mutationMultiplier = c.getFloat("mutationMultiplier", e, mutationMultiplier, 0f, 20f, "Multiplicador de forca/velocidade");

            maxVisualScale = c.getFloat("maxVisualScale", s, maxVisualScale, 1.2f, 8f, "Limite VISUAL de escala por regiao");
            maxEntityScale = c.getFloat("maxEntityScale", s, maxEntityScale, 1f, 4f, "Limite de escala/hitbox da entidade");
            maxVisibleSpikes = c.getInt("maxVisibleSpikes", s, maxVisibleSpikes, 0, 16, "Espinhos visiveis (a contagem logica nao tem teto)");
            maxSpeedMultiplier = c.getFloat("maxSpeedMultiplier", s, maxSpeedMultiplier, 1f, 6f, "Teto de multiplicador de velocidade");
            maxAttackDamage = c.getFloat("maxAttackDamage", s, maxAttackDamage, 1f, 1e6f, "Teto de dano por golpe (float finito)");

            animationFrequency = c.getFloat("animationFrequency", m, animationFrequency, 0.1f, 5f, "Frequencia das animacoes (cliente)");
            particles = c.getBoolean("particles", m, particles, "Particulas de alimentacao (cliente)");
            enableDebugCommands = c.getBoolean("enableDebugCommands", m, enableDebugCommands, "Habilita /endlessmutation debugfeed (so admins)");
        } finally {
            if (c.hasChanged()) c.save();
        }
        ENERGY.clear();
        for (String line : energyPerPrey) {
            try {
                String[] p = line.split("=");
                double v = Double.parseDouble(p[1].trim());
                if (v > 0 && !Double.isInfinite(v)) ENERGY.put(p[0].trim(), v);
            } catch (Exception ignored) { /* entrada invalida ignorada */ }
        }
        PRIORITY.clear();
        for (int i = 0; i < preyPriority.length; i++) PRIORITY.put(preyPriority[i].trim(), i);
    }

    public static String nameOf(EntityLivingBase e) {
        String n = EntityList.getEntityString(e);
        return n == null ? "" : n;
    }

    public static double energyFor(EntityLivingBase prey) {
        Double v = ENERGY.get(nameOf(prey));
        if (v != null) return v;
        return defaultEnergy + 0.5 * Math.max(0f, prey.getMaxHealth());
    }

    /** Peso de prioridade >= 1 (quanto maior, mais atraente). */
    public static double priorityWeight(EntityLivingBase prey) {
        Integer i = PRIORITY.get(nameOf(prey));
        return i == null ? 1.0 : 1.0 + (preyPriority.length - i) * 0.15;
    }

    public static boolean listed(String[] list, EntityLivingBase e) {
        String n = nameOf(e);
        for (String s : list) if (s.trim().equals(n)) return true;
        return false;
    }
}
