package com.endlessmutation.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.endlessmutation.ai.AIAttackPrey;
import com.endlessmutation.ai.AIEatPrey;
import com.endlessmutation.ai.AIHuntAnimals;
import com.endlessmutation.config.ModConfig;
import com.endlessmutation.evolution.EvolutionData;
import com.endlessmutation.evolution.MutationSystem;
import com.endlessmutation.persistence.EvolutionNBT;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

/**
 * NPC predadora imortal. Toda logica de evolucao roda SO no servidor; o cliente recebe via DataWatcher
 * apenas: indice de crescimento (float), semente de genes (int) e estado de caca (byte).
 */
public class EntityHuntress extends EntityCreature {
    public static final int DW_GROWTH = 20, DW_SEED = 21, DW_STATE = 22;
    public static final byte STATE_IDLE = 0, STATE_HUNT = 1, STATE_ATTACK = 2, STATE_EAT = 3;

    private final EvolutionData evo = new EvolutionData();

    // estado de IA (nao persistido por referencia; so UUIDs)
    private int preyEntityId = -1;
    private UUID preyUuid;
    private UUID pendingPreyUuid;
    private UUID lastHitUuid;
    private int lastHitTick = -1000;
    private int attackReadyTick = 0;
    private int nextSearchTick = 20;
    private int feedTicksLeft = 0;
    private boolean allowRemoval = false;
    private final Map<UUID, Integer> blacklist = new HashMap<UUID, Integer>();

    // cliente
    private float renderGrowth = 0f, prevRenderGrowth = 0f;
    private float appliedScale = 1f;

    public EntityHuntress(World world) {
        super(world);
        this.setSize(0.6F, 1.8F);
        this.isImmuneToFire = true;
        this.stepHeight = 1.0F;
        this.experienceValue = 0;
        this.enablePersistence();
        ((PathNavigateGround) this.getNavigator()).setAvoidsWater(false);
        ((PathNavigateGround) this.getNavigator()).setCanSwim(true);

        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new AIEatPrey(this));
        this.tasks.addTask(2, new AIAttackPrey(this));
        this.tasks.addTask(3, new AIHuntAnimals(this));
        this.tasks.addTask(5, new EntityAIWander(this, 0.8D));
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(7, new EntityAILookIdle(this));

        if (!world.isRemote) {
            this.dataWatcher.updateObject(DW_SEED, Integer.valueOf(this.rand.nextInt()));
            this.applyAttributes();
            this.refreshNameTag();
        }
    }

    // ------------------------------------------------------------------ registro de dados
    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(DW_GROWTH, Float.valueOf(0f));
        this.dataWatcher.addObject(DW_SEED, Integer.valueOf(0));
        this.dataWatcher.addObject(DW_STATE, Byte.valueOf(STATE_IDLE));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.attackDamage);
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(20.0D);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(ModConfig.baseSpeed);
        this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(ModConfig.baseAttack);
        this.getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(64.0D);
    }

    // ------------------------------------------------------------------ acessores
    public EvolutionData getEvolution() { return evo; }
    public float getGrowth() { return this.dataWatcher.getWatchableObjectFloat(DW_GROWTH); }
    public int getGeneSeed() { return this.dataWatcher.getWatchableObjectInt(DW_SEED); }
    public byte getHuntState() { return this.dataWatcher.getWatchableObjectByte(DW_STATE); }
    public boolean isFeeding() { return getHuntState() == STATE_EAT; }

    public void setHuntState(byte s) {
        if (!worldObj.isRemote && getHuntState() != s) this.dataWatcher.updateObject(DW_STATE, Byte.valueOf(s));
    }

    public float getRenderGrowth(float partial) {
        return prevRenderGrowth + (renderGrowth - prevRenderGrowth) * partial;
    }

    // ------------------------------------------------------------------ presa atual (por id, nunca referencia direta)
    public EntityLivingBase getPrey() {
        if (preyEntityId < 0) return null;
        Entity e = worldObj.getEntityByID(preyEntityId);
        if (e instanceof EntityLivingBase && e.getUniqueID().equals(preyUuid)) return (EntityLivingBase) e;
        return null;
    }

    public boolean hasPreyAssigned() { return preyEntityId >= 0; }

    public void setPrey(EntityLivingBase p) {
        this.preyEntityId = p.getEntityId();
        this.preyUuid = p.getUniqueID();
        setHuntState(STATE_HUNT);
    }

    public void clearPrey() {
        this.preyEntityId = -1;
        this.preyUuid = null;
        if (!isFeeding()) setHuntState(STATE_IDLE);
    }

    public UUID consumePendingPrey() { UUID u = pendingPreyUuid; pendingPreyUuid = null; return u; }

    public void scheduleNextSearch() {
        double h = evo.getHunger() / 100.0;
        nextSearchTick = ticksExisted + (int) Math.max(10, ModConfig.searchInterval * (1.0 - 0.5 * h));
    }
    public boolean searchDue() { return ticksExisted >= nextSearchTick; }

    public void blacklist(UUID id, int ticks) {
        if (blacklist.size() > 64) pruneBlacklist();
        blacklist.put(id, ticksExisted + ticks);
    }
    public boolean isBlacklisted(UUID id) {
        Integer until = blacklist.get(id);
        return until != null && until > ticksExisted;
    }
    private void pruneBlacklist() {
        for (Iterator<Map.Entry<UUID, Integer>> it = blacklist.entrySet().iterator(); it.hasNext();)
            if (it.next().getValue() <= ticksExisted) it.remove();
    }

    // ------------------------------------------------------------------ combate
    public double getReachSq(EntityLivingBase t) {
        double w = this.width * 2.0F;
        return w * w + t.width;
    }

    /** Ataque nativo (attackEntityFrom + DamageSource de mob), respeitando cooldown. */
    public boolean attackPrey(EntityLivingBase t) {
        if (worldObj.isRemote || ticksExisted < attackReadyTick) return false;
        attackReadyTick = ticksExisted + MutationSystem.attackCooldown(getGrowth(), ModConfig.attackCooldown);
        this.swingItem();
        float dmg = (float) MutationSystem.sane(
                this.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue(), 0.0, 1.0e6, 1.0);
        boolean hit = t.attackEntityFrom(DamageSource.causeMobDamage(this), dmg);
        if (hit) {
            lastHitUuid = t.getUniqueID();
            lastHitTick = ticksExisted;
            setHuntState(STATE_ATTACK);
            if (t.getHealth() <= 0f || t.isDead) tryConsume(t);
        }
        return hit;
    }

    /**
     * Unico ponto de recompensa. Exige: presa morta + golpe desta NPC nos ultimos 10 s + UUID nunca consumido.
     * A idempotencia fica em EvolutionData.consumeOnce (sobrevive a salvar/carregar).
     */
    private void tryConsume(EntityLivingBase prey) {
        UUID id = prey.getUniqueID();
        boolean engaged = id.equals(lastHitUuid) && ticksExisted - lastHitTick <= 200;
        if (engaged && evo.consumeOnce(id, ModConfig.energyFor(prey), ModConfig.massFactor)) {
            this.feedTicksLeft = ModConfig.feedTicks;
            this.preyEntityId = -1;
            this.preyUuid = null;
            this.getNavigator().clearPathEntity();
            setHuntState(STATE_EAT);
            onEvolutionChanged();
        } else {
            clearPrey();
        }
    }

    /** Depuracao (admin): equivale a uma refeicao valida, mas sem presa real. */
    public boolean debugMeal(double energy) {
        boolean ok = evo.consumeOnce(UUID.randomUUID(), energy, ModConfig.massFactor);
        if (ok) onEvolutionChanged();
        return ok;
    }

    // ------------------------------------------------------------------ evolucao -> atributos / sincronizacao
    private void onEvolutionChanged() {
        double g = evo.growthIndex(ModConfig.growthMassScale);
        this.dataWatcher.updateObject(DW_GROWTH, Float.valueOf((float) MutationSystem.sane(g, 0, 1e9, 0)));
        applyAttributes();
        refreshNameTag();
    }

    private void applyAttributes() {
        double g = getGrowth();
        if (!worldObj.isRemote) g = evo.growthIndex(ModConfig.growthMassScale);
        double sm = MutationSystem.strengthMultiplier(g, ModConfig.mutationMultiplier, ModConfig.maxAttackDamage);
        double dmg = MutationSystem.sane(ModConfig.baseAttack * sm, 0.5, ModConfig.maxAttackDamage, ModConfig.baseAttack);
        double spd = ModConfig.baseSpeed * MutationSystem.speedMultiplier(g, ModConfig.mutationMultiplier, ModConfig.maxSpeedMultiplier);
        this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(dmg);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(MutationSystem.sane(spd, 0.05, 1.5, ModConfig.baseSpeed));
    }

    public void refreshNameTag() {
        String lvl = evo.level(ModConfig.levelBaseEnergy, ModConfig.levelCurveExponent).toString();
        this.setCustomNameTag("\u00a7d" + "Predadora Mutante" + " \u00a77Lv. " + lvl);
        this.setAlwaysRenderNameTag(true);
    }

    public String describe() {
        double g = evo.growthIndex(ModConfig.growthMassScale);
        return "Nivel=" + evo.level(ModConfig.levelBaseEnergy, ModConfig.levelCurveExponent)
                + " | Energia=" + evo.getEnergy() + " | Massa=" + evo.getMass()
                + " | Refeicoes=" + evo.getMeals() + " | g=" + String.format(java.util.Locale.ROOT, "%.3f", g)
                + " | Fome=" + String.format(java.util.Locale.ROOT, "%.1f", evo.getHunger())
                + " | Dano=" + String.format(java.util.Locale.ROOT, "%.1f", getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue())
                + " | Espinhos(logico)=" + MutationSystem.spikeCountLogical(g);
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) { tickClient(); return; }
        tickServer();
        syncSize();
    }

    private void tickServer() {
        // fogo/afogamento/vida: protecoes adicionais (a principal e attackEntityFrom -> false)
        if (this.isBurning()) this.extinguish();
        if (this.getAir() < 300) this.setAir(300);

        if (!isFeeding()) {
            EntityLivingBase p = getPrey();
            if (p != null) {
                if (p.isDead || p.getHealth() <= 0f) tryConsume(p);
                else if (!PreyRules.isValidPrey(this, p)) clearPrey();
            } else if (preyEntityId >= 0) {
                // presa sumiu (removida/descarregada) sem confirmacao de morte por nos: sem recompensa
                clearPrey();
            }
        } else if (--feedTicksLeft <= 0) {
            feedTicksLeft = 0;
            setHuntState(STATE_IDLE);
        }

        if (ticksExisted % 20 == 0) {
            evo.tickHunger(ModConfig.hungerRate);   // so influencia a busca
            if (this.posY < -20.0D) {                // vazio: reposiciona em vez de morrer
                BlockPos sp = worldObj.getTopSolidOrLiquidBlock(worldObj.getSpawnPoint());
                this.setPositionAndUpdate(sp.getX() + 0.5, sp.getY() + 1, sp.getZ() + 0.5);
                this.fallDistance = 0f;
            }
        }
    }

    private void tickClient() {
        prevRenderGrowth = renderGrowth;
        float target = getGrowth();
        float diff = target - renderGrowth;
        renderGrowth = Math.abs(diff) < 1.0E-4f ? target : renderGrowth + diff * 0.08f;
        if (ModConfig.particles && getHuntState() == STATE_EAT && ticksExisted % 3 == 0) {
            worldObj.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                    posX + (rand.nextDouble() - 0.5) * width, posY + height * 0.8, posZ + (rand.nextDouble() - 0.5) * width,
                    0.0, 0.1, 0.0);
        }
        syncSize();
    }

    private void syncSize() {
        float s = (float) MutationSystem.entityScale(getGrowth(), ModConfig.maxEntityScale);
        if (Math.abs(s - appliedScale) > 0.02f) {
            appliedScale = s;
            this.setSize(Math.min(0.6F * s, 1.7F), 1.8F * s);
        }
    }

    @Override public float getEyeHeight() { return this.height * 0.9F; }

    // ------------------------------------------------------------------ imortalidade
    @Override public boolean attackEntityFrom(DamageSource source, float amount) { return false; }
    @Override public boolean isEntityInvulnerable(DamageSource source) { return true; }
    @Override public void onDeath(DamageSource cause) { /* nunca morre */ }
    @Override public void setHealth(float h) { super.setHealth(this.getMaxHealth()); }
    @Override public boolean isEntityAlive() { return true; }
    @Override public void fall(float distance, float multiplier) { /* sem dano de queda */ }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean isPotionApplicable(PotionEffect effect) { return false; }
    @Override public void addPotionEffect(PotionEffect effect) { /* ignorado */ }
    @Override public void setFire(int seconds) { /* ignorado */ }
    @Override protected boolean canDespawn() { return false; }
    @Override protected void despawnEntity() { /* nunca some */ }
    @Override public boolean canBeCollidedWith() { return true; }

    /** Remocao so por comando administrativo (/endlessmutation remove). */
    public void forceRemove() { this.allowRemoval = true; this.setDead(); }

    @Override
    public void setDead() {
        if (allowRemoval) super.setDead();
    }

    // ------------------------------------------------------------------ persistencia
    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        NBTTagCompound t = new NBTTagCompound();
        t.setInteger("FormatVersion", EvolutionData.FORMAT_VERSION);
        t.setTag("Evolution", EvolutionNBT.write(evo));
        t.setInteger("GeneSeed", getGeneSeed());
        t.setByte("FeedState", getHuntState() == STATE_EAT ? STATE_EAT : STATE_IDLE);
        t.setInteger("FeedTicksLeft", feedTicksLeft);
        if (preyUuid != null) {
            t.setLong("PreyM", preyUuid.getMostSignificantBits());
            t.setLong("PreyL", preyUuid.getLeastSignificantBits());
        }
        tag.setTag("EndlessMutation", t);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (!tag.hasKey("EndlessMutation", 10)) return;       // entidade antiga: padroes seguros
        NBTTagCompound t = tag.getCompoundTag("EndlessMutation");
        EvolutionNBT.read(t.getCompoundTag("Evolution"), evo);
        if (t.hasKey("GeneSeed")) this.dataWatcher.updateObject(DW_SEED, Integer.valueOf(t.getInteger("GeneSeed")));
        this.feedTicksLeft = Math.max(0, Math.min(400, t.getInteger("FeedTicksLeft")));
        if (t.getByte("FeedState") == STATE_EAT && feedTicksLeft > 0) setHuntState(STATE_EAT); // so animacao: a recompensa ja foi dada
        else setHuntState(STATE_IDLE);
        if (t.hasKey("PreyM")) this.pendingPreyUuid = new UUID(t.getLong("PreyM"), t.getLong("PreyL"));
        this.preyEntityId = -1;   // referencia anterior ao salvamento nunca e presumida valida
        this.preyUuid = null;
        onEvolutionChanged();
    }
}
