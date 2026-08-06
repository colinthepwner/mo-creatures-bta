package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Items;
import net.minecraft.core.player.gamemode.Gamemodes;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.MMUtils;

import java.util.List;

/**
 * The shark: the reason the original mod's install notes end with "punch the sharks in the nose".
 * <p>
 * It hunts anything alive in its water — players first, then whatever else is in range — and eats
 * the drops afterwards so a kill does not leave a slick of items behind. Only a shark raised from
 * an egg is tame; a tame one leaves players alone and never despawns. There is no way to gentle a
 * wild one, which is deliberate.
 * <p>
 * Two drops in the original have no equivalent here yet. Shark teeth were a mod item, so bone
 * stands in; shark eggs were an item that placed an entity, so the entity is dropped directly.
 */
public class MobShark extends MobAquaticBase {
	public static final int MASK_TAMED = 0b0000_0001;
	public static final int DATA_GENERIC_FLAGS = 17;

	private static final float ADULT_GROWTH = 2.0F;
	private static final float BABY_GROWTH = 0.3F;
	/** Below this it is still a pup: too small to bite, too small to spawn eggs. */
	private static final float FIGHTING_GROWTH = 1.0F;
	/** Egg-laying size, so only a well-grown shark seeds the next generation. */
	private static final float BREEDING_GROWTH = 1.5F;
	private static final float REACH = 3.5F;
	private static final int BITE_DAMAGE = 5;

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

	/** Grown enough to bite and to be worth being afraid of. */
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

	/**
	 * Players are checked first and at full range; everything else is a fallback. Other sharks and
	 * shark eggs are off the menu — a nest that eats itself does not last long.
	 */
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
		// Players are handled above so that they are always preferred and always at full range.
		return !(candidate instanceof Player);
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

	/**
	 * A shark that kills a shoal would otherwise leave a raft of items floating on the surface, so
	 * it takes the drops with it. Player drops are left alone, which the item's own age tells us:
	 * anything a player threw has had time on the clock.
	 */
	private void eatNearbyDrops() {
		List<EntityItem> drops = world.getEntitiesWithinAABB(EntityItem.class, MMUtils.grow(bb, 3.0, 3.0, 3.0));
		for (EntityItem drop : drops) {
			if (drop.age < 50) {
				drop.remove();
			}
		}
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

	/**
	 * Teeth most of the time; occasionally a clutch of eggs instead, but only from a shark big
	 * enough to have bred. The eggs are spawned as entities because this port has no egg item.
	 */
	@Override
	protected void dropDeathItems() {
		super.dropDeathItems();

		if (random.nextInt(100) < 90) {
			// Shark teeth were a mod item in the original; bone is the closest thing that exists here.
			dropItem(Items.BONE.id, 1 + random.nextInt(3));
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

	/**
	 * The original gave the shark no voice at all, so everything here is vanilla: the idle is the
	 * water it displaces rather than a call, and being struck sounds like flesh because a shark has
	 * no recorded hurt line to play instead.
	 */
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
