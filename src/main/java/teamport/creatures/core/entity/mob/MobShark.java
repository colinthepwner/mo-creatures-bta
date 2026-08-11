package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import teamport.creatures.core.item.MMItems;
import net.minecraft.core.player.gamemode.Gamemodes;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MMConfig;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.MMHunting;
import teamport.creatures.core.MMUtils;

public class MobShark extends MobAquaticBase {
	public static final int MASK_TAMED = 0b0000_0001;
	public static final int DATA_GENERIC_FLAGS = 17;

	private static final float ADULT_GROWTH = 2.0F;
	private static final float BABY_GROWTH = 0.3F;

	private static final float FIGHTING_GROWTH = 1.0F;

	private static final float BREEDING_GROWTH = 1.5F;
	private static final float REACH = 3.5F;
	private static final int BITE_DAMAGE = 5;

	private static final double DEVOUR_RADIUS = 3.0D;

	private static final int DEVOUR_MAX_ITEM_AGE = 50;

	public MobShark(World world) {
		super(world);
		textureIdentifier = new NamespaceID(MoreMobs.MOD_ID, "shark");
		scoreValue = 200;

		setSize(1.8F, 1.3F);
		setGrowth(FIGHTING_GROWTH + random.nextFloat() * (ADULT_GROWTH - FIGHTING_GROWTH));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_GENERIC_FLAGS, (byte) 0, Byte.class);
	}

	public boolean isSharkTamed() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_TAMED) != 0;
	}

	public void setSharkTamed(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		if (flag) {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data | MASK_TAMED));
		} else {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data & ~MASK_TAMED));
		}
	}

	@Override
	public float adultGrowth() {
		return ADULT_GROWTH;
	}

	@Override
	public float babyGrowth() {
		return BABY_GROWTH;
	}

	public boolean isGrownEnoughToHunt() {
		return getGrowth() >= FIGHTING_GROWTH;
	}

	@Override
	protected float swimThrust() {
		return 0.05F;
	}

	@Override
	protected float cruiseSpeed() {
		return 0.7F;
	}

	@Override
	protected int targetSearchInterval() {
		return 30;
	}

	@Override
	protected float loseTargetRange() {
		return 20.0F;
	}

	@Override
	protected Entity findSwimTarget() {
		if (!isGrownEnoughToHunt() || !world.getDifficulty().canHostileMobsSpawn()) {
			return null;
		}

		if (!isSharkTamed()) {
			Player player = world.getClosestPlayerToEntity(this, 16.0);
			if (player != null && player.isAlive() && player.gamemode != Gamemodes.CREATIVE) {
				return player;
			}
		}

		Mob nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (Mob candidate : world.getEntitiesWithinAABB(Mob.class, MMUtils.grow(bb, 16.0, 12.0, 16.0))) {
			if (!isPrey(candidate)) {
				continue;
			}
			double distance = distanceToSqr(candidate);
			if (distance < nearestDistance) {
				nearest = candidate;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	private boolean isPrey(Mob candidate) {
		if (candidate == this || !candidate.isAlive()) {
			return false;
		}
		if (candidate instanceof MobShark || candidate instanceof MobSharkEgg) {
			return false;
		}

		if (!MMHunting.isHuntable(candidate)) {
			return false;
		}

		if (!MMConfig.dolphinsAttackSharks && candidate instanceof MobDolphin) {
			return false;
		}

		return !(candidate instanceof Player);
	}

	@Override
	public boolean canSpawnHere() {
		return MMConfig.spawnsAt(world.getDifficulty(), MMConfig.sharkSpawnDifficulty)
			&& super.canSpawnHere();
	}

	@Override
	protected void attackEntity(@NotNull Entity entity, float distance) {
		if (distance >= REACH || !isGrownEnoughToHunt()) {
			return;
		}
		if (entity.bb.maxY <= bb.minY || entity.bb.minY >= bb.maxY) {
			return;
		}

		attackTime = 20;
		entity.hurt(this, BITE_DAMAGE, DamageType.COMBAT);

		if (!(entity instanceof Player)) {
			eatNearbyDrops();
		}
	}

	private void eatNearbyDrops() {
		MMHunting.devourDrops(this, DEVOUR_RADIUS, DEVOUR_MAX_ITEM_AGE);
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (!super.hurt(attacker, damage, type)) {
			return false;
		}
		if (attacker != null && attacker != this) {
			setTarget(attacker);
		}
		return true;
	}

	@Override
	protected void dropDeathItems() {
		super.dropDeathItems();

		if (random.nextInt(100) < 90) {

			dropItem(MMItems.SHARK_TEETH.id, 1 + random.nextInt(3));
			return;
		}

		if (getGrowth() <= BREEDING_GROWTH || !world.getDifficulty().canHostileMobsSpawn()) {
			return;
		}
		int clutch = 1 + random.nextInt(3);
		for (int i = 0; i < clutch; i++) {
			MobSharkEgg egg = new MobSharkEgg(world);
			egg.moveTo(x, y, z, random.nextFloat() * 360.0F, 0.0F);
			world.entityJoinedWorld(egg);
		}
	}

	@Override
	public int getMaxHealth() {
		return 25;
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/shark/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/shark/0.png";
	}

	@Override
	public String getLivingSound() {
		return "liquid.splash";
	}

	@Override
	protected String getHurtSound() {
		return "damage.hurtflesh";
	}

	@Override
	protected String getDeathSound() {
		return "damage.hurtflesh";
	}

	@Override
	protected boolean canDespawn() {
		return super.canDespawn() && !isSharkTamed();
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 2;
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Tamed", isSharkTamed());
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setSharkTamed(tag.getBoolean("Tamed"));
	}
}
