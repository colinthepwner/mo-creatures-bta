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
import teamport.creatures.core.MMHunting;
import teamport.creatures.core.MMUtils;
import teamport.creatures.core.item.MMItems;

import java.util.List;

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

	public static final int DATA_GROWTH = 17;

	private static final byte GROWTH_NEWBORN = 35;
	private static final byte GROWTH_ADULT = 100;

	private static final byte GROWTH_TAME_LIMIT = 80;

	private static final String[] VARIANT_TEXTURES = {
		"bigcat_lion", "bigcat_lion", "bigcat_panther", "bigcat_cheetah",
		"bigcat_tiger", "bigcat_leopard", "bigcat_tiger_white"
	};

	private static final float[] VARIANT_WIDTH = {1.0F, 1.1F, 0.9F, 0.8F, 1.1F, 0.8F, 1.2F};
	private static final float[] VARIANT_HEIGHT = {1.0F, 1.1F, 0.9F, 0.8F, 1.1F, 0.8F, 1.2F};
	private static final float[] VARIANT_LENGTH = {1.0F, 1.0F, 0.9F, 1.0F, 1.1F, 0.9F, 1.2F};
	private static final float[] VARIANT_SPEEDS = {1.4F, 1.4F, 1.6F, 1.9F, 1.6F, 1.7F, 1.7F};
	private static final double[] VARIANT_RANGES = {8.0, 4.0, 6.0, 6.0, 8.0, 4.0, 10.0};

	private static final float BITE_RANGE = 3.0F;

	private static final int[] VARIANT_DAMAGE = {5, 5, 4, 3, 6, 3, 8};
	private static final int[] VARIANT_HEALTH = {25, 30, 20, 20, 35, 25, 40};

	private boolean hungry = true;

	private boolean hasEaten;

	public MobBigCat(World world) {
		super(world);

		setTextureIdentifier(MoreMobs.MOD_ID, "bigcat");
		scoreValue = 300;
		heartsHalvesLife = 40;

		setSize(0.9F, 1.3F);

		mobDrops.add(new WeightedRandomLootObject(Items.LEATHER.getDefaultStack(), 1, 2));

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
	protected void dropDeathItems() {
		super.dropDeathItems();

		if (isAdult() && random.nextInt(3) != 0) {
			dropItem(MMItems.BIGCAT_CLAW.id, 1 + random.nextInt(2));
		}
	}

	@Override
	protected boolean makeStepSound() {
		return false;
	}

	public int getVariant() {
		int variant = getSkinVariant();
		return variant < 0 || variant >= VARIANT_COUNT ? VARIANT_LIONESS : variant;
	}

	public void setVariant(int variant) {
		setSkinVariant(variant < 0 || variant >= VARIANT_COUNT ? VARIANT_LIONESS : variant);
		applyVariant();
	}

	private void applyVariant() {
		moveSpeed = VARIANT_SPEEDS[getVariant()];
		setHealthRaw(getMaxHealth());
	}

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

	public boolean hasMane() {
		return getVariant() == VARIANT_LION && isAdult();
	}

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

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/" + VARIANT_TEXTURES[getVariant()] + "/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/bigcat_lion/0.png";
	}

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

	private void grow() {
		if (isAdult() || random.nextInt(250) != 0) return;

		setGrowth(getGrowth() + 1);
		if (isAdult()) {

			applyVariant();
		}
	}

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

	private Entity findPrey(double range) {
		Entity best = null;
		double bestDistance = -1.0;

		for (Entity candidate : world.getEntitiesWithinAABBExcludingEntity(this, MMUtils.grow(bb, range, range, range))) {
			if (!(candidate instanceof Mob)) continue;
			if (candidate instanceof Player || candidate == vehicle || candidate == passenger) continue;
			if (!candidate.isAlive()) continue;

			if (!MMHunting.isHuntable(candidate)) continue;

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

		if (distance < BITE_RANGE && entity.bb.maxY > bb.minY && entity.bb.minY < bb.maxY) {
			attackTime = 20;
			entity.hurt(this, getAttackDamage(), DamageType.COMBAT);
			hungry = false;
			return;
		}

		if (distance < 6.0F && random.nextInt(50) == 0 && onGround) {
			double dx = entity.x - x;
			double dz = entity.z - z;
			float length = MathHelper.sqrt(dx * dx + dz * dz);
			xd = dx / (double) length * 0.5 * 0.8F + xd * 0.2F;
			zd = dz / (double) length * 0.5 * 0.8F + zd * 0.2F;
			yd = 0.4F;
		}
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (!super.hurt(attacker, damage, type)) return false;
		if (attacker == null || attacker == this || attacker == vehicle || attacker == passenger) return true;

		if (attacker instanceof ProjectileArrow && ((ProjectileArrow) attacker).owner != null) {
			attacker = ((ProjectileArrow) attacker).owner;
		}

		if (!isTamed() && world.getDifficulty().canHostileMobsSpawn()) {
			if (!(attacker instanceof Player) || ((Player) attacker).getGamemode() != Gamemodes.CREATIVE) {
				setSitting(false);
				setTarget(attacker);
			}
		}
		return true;
	}

	@Override
	public boolean interact(@NotNull Player player) {
		ItemStack held = player.getHeldItem();

		if (!world.isClientSide) {
			if (held != null && isFeed(held)) {
				MMUtils.consumeHeld(player, held);
				hungry = false;
				world.playSoundAtEntity(null, this, "creatures:mob.bigcat.eat", 1.0F,
					(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);

				if (isTamed()) {
					heal(4);
					return true;
				}

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

	private static boolean isFeed(@NotNull ItemStack held) {
		return held.itemID == Items.FOOD_PORKCHOP_RAW.id
			|| held.itemID == Items.FOOD_FISH_RAW.id
			|| held.itemID == MMItems.PET_FOOD.id;
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

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {

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
