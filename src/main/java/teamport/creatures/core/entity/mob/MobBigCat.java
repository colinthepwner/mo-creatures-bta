package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.entity.projectile.ProjectileArrow;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.player.gamemode.Gamemodes;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import net.minecraft.core.world.biome.Biome;
import net.minecraft.core.world.biome.Biomes;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.MMUtils;

import java.util.List;

/**
 * Mo' Creatures' big cats: lion, lioness, tiger, white tiger, panther, cheetah and snow leopard.
 * <p>
 * The original shipped all of these as one {@code EntityBigCat} keyed off a {@code typeint}, and this
 * keeps that shape rather than splitting into seven classes — same call the 7.3 rewrite made for the
 * polar bear and the arctic fox. The species lives in {@link #getSkinVariant()}, BTA 8.0's built-in
 * variant slot, which is already synched over {@code DATA_VARIANT} and already written to NBT as
 * {@code SkinVariant} by {@link net.minecraft.core.entity.Mob} itself. Rolling a private NBT int
 * instead collides with that key and corrupts on load, so do not.
 * <p>
 * Everything else the renderer needs is synched too. Tamed/sitting ride in a flags byte the same way
 * {@link MobBear}'s anger does, and the cub's growth is a synched byte rather than a plain field,
 * because the renderer scales the model by it and plain fields never leave the server.
 * <p>
 * Deliberate departures from the original, all forced by what this port has to work with:
 * <ul>
 *   <li><b>Taming.</b> The original wanted a cub fed raw pork or fish and <em>then</em> a medallion
 *       placed on it. There is no medallion item here (nor a whip, rope or naming GUI), so the second
 *       step became a second helping: a small cub that has already eaten may take to whoever feeds it
 *       again. The "must still be small" gate from 2.10.2 is kept — an almost-grown cub cannot be
 *       tamed at all.</li>
 *   <li><b>Drops.</b> The original dropped a BigCat Claw, whose only use was crafting the whip.
 *       Neither exists here, so big cats drop leather.</li>
 *   <li><b>Snow leopards.</b> The original scanned for snow blocks within one block of the spawn.
 *       This uses the glacier/tundra biome test the rest of the mod already spawns off.</li>
 * </ul>
 */
public class MobBigCat extends MobAnimal {
	public static final int VARIANT_LIONESS = 0;
	public static final int VARIANT_LION = 1;
	public static final int VARIANT_PANTHER = 2;
	public static final int VARIANT_CHEETAH = 3;
	public static final int VARIANT_TIGER = 4;
	public static final int VARIANT_LEOPARD = 5;
	public static final int VARIANT_TIGER_WHITE = 6;
	private static final int VARIANT_COUNT = 7;

	public static final int MASK_TAMED = 0b0000_0001;
	public static final int MASK_SITTING = 0b0000_0010;
	public static final int DATA_GENERIC_FLAGS = 16;
	/**
	 * Cub growth as a whole percentage, 35 at birth through to 100 when adult. Synched, because the
	 * renderer scales the model by it. It is a byte rather than the float the original used because
	 * {@code SynchedEntityData} exposes no float getter; a percent is finer than the 1% growth steps
	 * the original took anyway.
	 */
	public static final int DATA_GROWTH = 17;

	private static final byte GROWTH_NEWBORN = 35;
	private static final byte GROWTH_ADULT = 100;
	/** Above this a cub is too far grown to still be tamed, per the original's 2.10.2 rule. */
	private static final byte GROWTH_TAME_LIMIT = 80;

	/**
	 * Texture folder, render scale, speed, hunting range, bite damage and health, per species.
	 * <p>
	 * Lioness and lion share a folder on purpose: they shared one body texture in the original too,
	 * and what tells them apart is the mane, which is its own layer with its own texture.
	 */
	private static final String[] VARIANT_TEXTURES = {
		"bigcat_lion", "bigcat_lion", "bigcat_panther", "bigcat_cheetah",
		"bigcat_tiger", "bigcat_leopard", "bigcat_tiger_white"
	};
	/**
	 * Build, per species, as width/height/length — the original scaled the three axes separately, so a
	 * cheetah is a lioness stretched long and slimmed down rather than a smaller lioness.
	 */
	private static final float[] VARIANT_WIDTH = {1.0F, 1.1F, 0.9F, 0.8F, 1.1F, 0.8F, 1.2F};
	private static final float[] VARIANT_HEIGHT = {1.0F, 1.1F, 0.9F, 0.8F, 1.1F, 0.8F, 1.2F};
	private static final float[] VARIANT_LENGTH = {1.0F, 1.0F, 0.9F, 1.0F, 1.1F, 0.9F, 1.2F};
	private static final float[] VARIANT_SPEEDS = {1.4F, 1.4F, 1.6F, 1.9F, 1.6F, 1.7F, 1.7F};
	private static final double[] VARIANT_RANGES = {8.0, 4.0, 6.0, 6.0, 8.0, 4.0, 10.0};
	private static final int[] VARIANT_DAMAGE = {5, 5, 4, 3, 6, 3, 8};
	private static final int[] VARIANT_HEALTH = {25, 30, 20, 20, 35, 25, 40};

	/** Hunger gates every hunt in the original: a fed cat leaves you alone. Server-side. */
	private boolean hungry = true;
	/** Set once a cub has been fed, which is what unlocks taming. Server-side. */
	private boolean hasEaten;

	public MobBigCat(World world) {
		super(world);
		// Through the setter, not by assigning textureIdentifier: the setter also repoints
		// variantJsonPath, which otherwise stays aimed at the player skin's variant list and makes
		// getTextureReference() hand back a char skin key instead of "0".
		setTextureIdentifier(MoreMobs.MOD_ID, "bigcat");
		scoreValue = 300;
		heartsHalvesLife = 40;

		// The original kept one collision box for every species and varied only the render scale.
		setSize(0.9F, 1.3F);

		mobDrops.add(new WeightedRandomLootObject(Items.LEATHER.getDefaultStack(), 1, 2));

		// Mob's constructor seeds DATA_VARIANT with a random byte, so it has to be pinned to a real
		// species before anything reads it. spawnInit() replaces this for naturally spawned cats.
		setSkinVariant(rollVariant());
		setGrowth(GROWTH_ADULT);
		applyVariant();
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_GENERIC_FLAGS, (byte) 0, Byte.class);
		entityData.define(DATA_GROWTH, GROWTH_ADULT, Byte.class);
	}

	@Override
	protected boolean makeStepSound() {
		return false;
	}

	// --- variant ----------------------------------------------------------------------------------

	/** Clamped so a corrupt or unset byte reads as a lioness rather than blowing up an array index. */
	public int getVariant() {
		int variant = getSkinVariant();
		return variant < 0 || variant >= VARIANT_COUNT ? VARIANT_LIONESS : variant;
	}

	public void setVariant(int variant) {
		setSkinVariant(variant < 0 || variant >= VARIANT_COUNT ? VARIANT_LIONESS : variant);
		applyVariant();
	}

	/**
	 * Re-reads the per-species stats. Health has to be reset explicitly: {@code Mob}'s constructor
	 * seeds the health slot from {@link #getMaxHealth()} before the variant is known.
	 */
	private void applyVariant() {
		moveSpeed = VARIANT_SPEEDS[getVariant()];
		setHealthRaw(getMaxHealth());
	}

	/** The original's spawn odds: 6% lioness, 20% lion, 25% panther, 20% cheetah, 5% white tiger, rest tiger. */
	private int rollVariant() {
		int roll = random.nextInt(100);
		if (roll <= 5) return VARIANT_LIONESS;
		if (roll <= 25) return VARIANT_LION;
		if (roll <= 50) return VARIANT_PANTHER;
		if (roll <= 70) return VARIANT_CHEETAH;
		if (roll <= 75) return VARIANT_TIGER_WHITE;
		return VARIANT_TIGER;
	}

	@Override
	public int getMaxHealth() {
		int adultHealth = VARIANT_HEALTH[getVariant()];
		return isAdult() ? adultHealth : Math.max(4, adultHealth / 2);
	}

	public int getAttackDamage() {
		return isAdult() ? VARIANT_DAMAGE[getVariant()] : 1;
	}

	public double getHuntingRange() {
		return VARIANT_RANGES[getVariant()];
	}

	/** Model scale for the renderer, per axis: the species' build, shrunk while the cat is still a cub. */
	public float getRenderScaleX() {
		return VARIANT_WIDTH[getVariant()] * cubFactor();
	}

	public float getRenderScaleY() {
		return VARIANT_HEIGHT[getVariant()] * cubFactor();
	}

	public float getRenderScaleZ() {
		return VARIANT_LENGTH[getVariant()] * cubFactor();
	}

	private float cubFactor() {
		return isAdult() ? 1.0F : getGrowth() / 100.0F;
	}

	/** Only the grown male lion wears the mane, which is a second piece of geometry. */
	public boolean hasMane() {
		return getVariant() == VARIANT_LION && isAdult();
	}

	// --- synched state ----------------------------------------------------------------------------

	/** @return growth as a whole percentage, 35..100. */
	public int getGrowth() {
		return entityData.getByte(DATA_GROWTH);
	}

	public void setGrowth(int growth) {
		entityData.set(DATA_GROWTH, (byte) Math.min(GROWTH_ADULT, Math.max(GROWTH_NEWBORN, growth)));
	}

	public boolean isAdult() {
		return getGrowth() >= GROWTH_ADULT;
	}

	public boolean isTamed() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_TAMED) != 0;
	}

	public void setTamed(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		entityData.set(DATA_GENERIC_FLAGS, flag ? (byte) (data | MASK_TAMED) : (byte) (data & ~MASK_TAMED));
	}

	public boolean isSitting() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_SITTING) != 0;
	}

	public void setSitting(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		entityData.set(DATA_GENERIC_FLAGS, flag ? (byte) (data | MASK_SITTING) : (byte) (data & ~MASK_SITTING));
	}

	// --- textures and sound -----------------------------------------------------------------------

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/" + VARIANT_TEXTURES[getVariant()] + "/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/bigcat_lion/0.png";
	}

	/** The mane is drawn as its own layer, so it needs its own texture. */
	public String getManeTexture() {
		return "/assets/creatures/textures/entity/bigcat_mane/" + getTextureReference() + ".png";
	}

	@Override
	public String getLivingSound() {
		return isAdult() ? "creatures:mob.bigcat" : "creatures:mob.bigcat.cub";
	}

	@Override
	protected String getHurtSound() {
		return isAdult() ? "creatures:mob.bigcat.hurt" : "creatures:mob.bigcat.cub.hurt";
	}

	@Override
	protected String getDeathSound() {
		return isAdult() ? "creatures:mob.bigcat.death" : "creatures:mob.bigcat.cub.death";
	}

	@Override
	protected float getSoundVolume() {
		return 0.5F;
	}

	// --- AI ---------------------------------------------------------------------------------------

	@Override
	protected boolean isMovementCeased() {
		return isSitting();
	}

	@Override
	protected boolean canDespawn() {
		return !isTamed() && super.canDespawn();
	}

	@Override
	protected void updateAI() {
		super.updateAI();
		if (world.isClientSide) return;

		grow();

		// A fed cat is a calm cat. It gets peckish again on its own, but never while it is sitting.
		if (!hungry && !isSitting() && random.nextInt(200) == 0) {
			hungry = true;
		}

		if (hungry && !isSitting() && attackTime == 0) {
			scavenge();
		}

		if (isSitting()) {
			moveForward = 0.0F;
			moveStrafing = 0.0F;
			isJumping = false;
		}
	}

	/** Cubs put on 1% of their growth roughly every 250 ticks, so growing up takes a long while. */
	private void grow() {
		if (isAdult() || random.nextInt(250) != 0) return;

		setGrowth(getGrowth() + 1);
		if (isAdult()) {
			// Reaching adulthood re-rolls the stat table, so top the cat back up to its adult health.
			applyVariant();
		}
	}

	/**
	 * Hungry cats go for dropped raw pork or fish, exactly as in the original: they path towards
	 * anything within twelve blocks, and eat whatever they end up standing next to. Throwing food to
	 * a wild cub is how the original expected you to start taming one, and it still is.
	 */
	private void scavenge() {
		EntityItem sighted = findFood(12.0);
		if (sighted == null) return;

		lookAt(sighted, 30.0F, 30.0F);
		setPathToEntity(world.getEntityPathToXYZ(this, MathHelper.floor(sighted.x), MathHelper.floor(sighted.y),
			MathHelper.floor(sighted.z), 16.0F));

		if (random.nextInt(80) != 0) return;
		EntityItem within = findFood(2.0);
		if (within == null) return;

		within.remove();
		heal(10);
		if (!isAdult() && getGrowth() < GROWTH_TAME_LIMIT) {
			hasEaten = true;
		}
		world.playSoundAtEntity(null, this, "creatures:mob.bigcat.eat", 1.0F,
			(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
		hungry = false;
	}

	private EntityItem findFood(double range) {
		List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, MMUtils.grow(bb, range, range, range));
		for (EntityItem item : items) {
			if (item.item == null) continue;
			if (item.item.itemID == Items.FOOD_PORKCHOP_RAW.id || item.item.itemID == Items.FOOD_FISH_RAW.id) {
				return item;
			}
		}
		return null;
	}

	/**
	 * The original's aggression table: lionesses, tigers and white tigers always take a swing at a
	 * player in range, everything else only occasionally. Tamed cats and cubs never do, and nothing
	 * hunts unless it is hungry.
	 */
	@Override
	protected Entity findPlayerToAttack() {
		if (isTamed() || isSitting() || !hungry) return null;
		if (!world.getDifficulty().canHostileMobsSpawn()) return null;

		Player player = world.getClosestPlayerToEntity(this, getHuntingRange());
		if (player != null && isAdult() && player.gamemode.hasHostileMobs()) {
			int variant = getVariant();
			boolean alwaysHunts = variant == VARIANT_LIONESS || variant == VARIANT_TIGER || variant == VARIANT_TIGER_WHITE;
			if (alwaysHunts || random.nextInt(30) == 0) {
				hungry = false;
				return player;
			}
		}

		if (random.nextInt(80) == 0) {
			Entity prey = findPrey(10.0);
			if (prey != null) {
				hungry = false;
				return prey;
			}
		}
		return null;
	}

	/**
	 * Livestock, and rival cats. Cubs only pick on things smaller than themselves; adults will start
	 * a catfight with another breed but never with their own, and a male lion never goes for a
	 * lioness. Nothing picks a fight it is already losing on health, and nobody bothers a white tiger.
	 */
	private Entity findPrey(double range) {
		Entity best = null;
		double bestDistance = -1.0;

		for (Entity candidate : world.getEntitiesWithinAABBExcludingEntity(this, MMUtils.grow(bb, range, range, range))) {
			if (!(candidate instanceof Mob)) continue;
			if (candidate instanceof Player || candidate == vehicle || candidate == passenger) continue;
			if (!candidate.isAlive()) continue;

			// A cub only takes on something smaller than it is.
			if (!isAdult() && (candidate.bbWidth > 0.5F || candidate.bbHeight > 0.5F)) continue;

			if (candidate instanceof MobBigCat) {
				MobBigCat other = (MobBigCat) candidate;
				if (!isAdult()) continue;
				if (isTamed() && other.isTamed()) continue;
				if (other.getVariant() == VARIANT_TIGER_WHITE) continue;
				if (getVariant() != VARIANT_LION && getVariant() == other.getVariant()) continue;
				if (getVariant() == VARIANT_LION && other.getVariant() == VARIANT_LIONESS) continue;
				if (getHealth() < other.getHealth()) continue;
			} else if (isTamed() && candidate instanceof MobKitty) {
				continue;
			}

			double distance = candidate.distanceToSqr(this);
			if (bestDistance != -1.0 && distance >= bestDistance) continue;
			if (!canEntityBeSeen(candidate)) continue;

			bestDistance = distance;
			best = candidate;
		}
		return best;
	}

	@Override
	protected void attackEntity(@NotNull Entity entity, float distance) {
		if (entity instanceof EntityItem) return;

		if (distance > 2.0F && distance < 6.0F && random.nextInt(50) == 0) {
			// The pounce. Big cats close the last few blocks in one leap rather than trotting up.
			if (onGround) {
				double dx = entity.x - x;
				double dz = entity.z - z;
				float length = MathHelper.sqrt(dx * dx + dz * dz);
				xd = dx / (double) length * 0.5 * 0.8F + xd * 0.2F;
				zd = dz / (double) length * 0.5 * 0.8F + zd * 0.2F;
				yd = 0.4F;
			}
		} else if (distance < 2.5F && entity.bb.maxY > bb.minY && entity.bb.minY < bb.maxY) {
			attackTime = 20;
			entity.hurt(this, getAttackDamage(), DamageType.COMBAT);
			hungry = false;
		}
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (!super.hurt(attacker, damage, type)) return false;
		if (attacker == null || attacker == this || attacker == vehicle || attacker == passenger) return true;

		if (attacker instanceof ProjectileArrow && ((ProjectileArrow) attacker).owner != null) {
			attacker = ((ProjectileArrow) attacker).owner;
		}

		// A tamed cat will not turn on its keeper, and nothing fights back on peaceful.
		if (!isTamed() && world.getDifficulty().canHostileMobsSpawn()) {
			if (!(attacker instanceof Player) || ((Player) attacker).getGamemode() != Gamemodes.CREATIVE) {
				setSitting(false);
				setTarget(attacker);
			}
		}
		return true;
	}

	/**
	 * Feeding is the whole taming ritual in this port — see the class note — but it is still two
	 * steps, the way the original was. A small cub has to have eaten first, either from your hand or
	 * off the ground; only then does a second helping of raw pork or fish stand a chance of winning
	 * it over. Feeding a cat that is already yours heals it, and a bare hand sits it down.
	 */
	@Override
	public boolean interact(@NotNull Player player) {
		ItemStack held = player.getHeldItem();

		if (!world.isClientSide) {
			if (held != null && (held.itemID == Items.FOOD_PORKCHOP_RAW.id || held.itemID == Items.FOOD_FISH_RAW.id)) {
				held.consumeItem(player);
				hungry = false;
				world.playSoundAtEntity(null, this, "creatures:mob.bigcat.eat", 1.0F,
					(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);

				if (isTamed()) {
					heal(4);
					return true;
				}

				// An almost-grown cub is already too set in its ways, and an adult is hopeless.
				if (isAdult() || getGrowth() >= GROWTH_TAME_LIMIT) {
					showTamingFX(false);
					return true;
				}

				lookAt(player, 30.0F, 30.0F);
				if (hasEaten && random.nextInt(3) == 0) {
					setTamed(true);
					setSitting(false);
					setTarget(null);
					setHealthRaw(getMaxHealth());
					showTamingFX(true);
				} else {
					hasEaten = true;
					showTamingFX(false);
				}
				return true;
			}

			if (isTamed() && held == null) {
				setSitting(!isSitting());
				return true;
			}
		}

		return super.interact(player);
	}

	private void showTamingFX(boolean success) {
		String particle = success ? "heart" : "smoke";
		for (int i = 0; i < 7; i++) {
			world.spawnParticle(particle,
				x + (double) (random.nextFloat() * bbWidth * 2.0F) - (double) bbWidth,
				y + 0.5 + (double) (random.nextFloat() * bbHeight),
				z + (double) (random.nextFloat() * bbWidth * 2.0F) - (double) bbWidth,
				random.nextGaussian() * 0.02,
				random.nextGaussian() * 0.02,
				random.nextGaussian() * 0.02,
				0,
				false);
		}
	}

	// --- spawning ---------------------------------------------------------------------------------

	/**
	 * Snow country gets snow leopards. Everywhere else a cat joins whatever pride is already nearby —
	 * a male lion draws lionesses, and white tigers never come in pairs — falling back to a fresh roll
	 * when there is nothing around. A quarter of them arrive as cubs.
	 */
	@Override
	public void spawnInit() {
		int blockX = MathHelper.floor(x);
		int blockY = MathHelper.floor(bb.minY);
		int blockZ = MathHelper.floor(z);
		Biome biome = world.getBlockBiome(blockX, blockY, blockZ);

		if (biome == Biomes.OVERWORLD_GLACIER || biome == Biomes.OVERWORLD_TUNDRA) {
			setVariant(VARIANT_LEOPARD);
		} else {
			int nearby = findNearbyPrideVariant(12.0);
			setVariant(nearby >= 0 ? nearby : rollVariant());
		}

		if (random.nextInt(4) == 0) {
			setGrowth(GROWTH_NEWBORN);
			applyVariant();
		}
	}

	/** @return the species a newcomer should take from the pride around it, or -1 if it is alone. */
	private int findNearbyPrideVariant(double range) {
		for (MobBigCat other : world.getEntitiesWithinAABB(MobBigCat.class, MMUtils.grow(bb, range, range, range))) {
			if (other == this) continue;
			int variant = other.getVariant();
			if (variant == VARIANT_LION) return VARIANT_LIONESS;
			if (variant == VARIANT_TIGER_WHITE) return VARIANT_TIGER;
			return variant;
		}
		return -1;
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 4;
	}

	// --- persistence ------------------------------------------------------------------------------

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		// The species itself is written by Mob as "SkinVariant"; do not shadow that key.
		super.addAdditionalSaveData(tag);
		tag.putByte("Growth", (byte) getGrowth());
		tag.putBoolean("IsTamed", isTamed());
		tag.putBoolean("IsSitting", isSitting());
		tag.putBoolean("IsHungry", hungry);
		tag.putBoolean("HasEaten", hasEaten);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.containsKey("Growth")) setGrowth(tag.getByte("Growth"));
		setTamed(tag.getBoolean("IsTamed"));
		setSitting(tag.getBoolean("IsSitting"));
		hungry = !tag.containsKey("IsHungry") || tag.getBoolean("IsHungry");
		hasEaten = tag.getBoolean("HasEaten");
		moveSpeed = VARIANT_SPEEDS[getVariant()];
	}
}
