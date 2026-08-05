package teamport.creatures.core.entity.mob;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.MobFlying;
import net.minecraft.core.entity.monster.Enemy;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.Difficulty;
import net.minecraft.core.enums.LightLayer;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.weather.Weather;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MoreMobs;

/**
 * The wraith: a spirit that drifts through the dark and, being a spirit, through walls.
 * <p>
 * The original built this on its own flying-mob base and then never actually gave it noclip; walls
 * still stopped it, which rather undercut the idea. Here it genuinely passes through blocks —
 * {@code setNoPhysics(true)} on {@link Entity}'s shared flags, which is synched, so the client draws
 * it inside the wall too. There is no pathfinding to speak of: it simply steers at whatever it is
 * haunting, and wanders on a waypoint when it has nobody.
 * <p>
 * Daylight is still fatal ("Fixed bug with Wraiths surviving daylight"), so it catches fire in the
 * open sun in the same way BTA's zombies do.
 */
public class MobWraith extends MobFlying implements Enemy {
	/** How far away it notices a player. */
	public static final double HUNT_RANGE = 20.0D;
	/**
	 * Target drift speed, in blocks per tick. {@code MobFlying} bleeds 9% of the velocity off every
	 * tick, so the speed it actually settles at is a good deal lower than this.
	 */
	public static final double DRIFT_SPEED = 0.2D;
	/** How sharply it corrects course; low values give the floating, unhurried feel. */
	public static final double DRIFT_ACCELERATION = 0.12D;
	/** Reach of its cold touch. */
	public static final float ATTACK_REACH = 2.5F;
	/** Ticks it burns for when caught in the sun. */
	public static final int DAYLIGHT_BURN_TICKS = 300;

	/** {@code MobFlying} sits outside the monster hierarchy, so it carries its own attack strength. */
	protected int attackStrength;
	protected Entity hauntTarget;
	private int retargetCooldown;
	private int courseChangeCooldown;
	private double waypointX;
	private double waypointY;
	private double waypointZ;

	public MobWraith(World world) {
		super(world);
		setTextureIdentifier(MoreMobs.MOD_ID, "wraith");

		setSize(1.5F, 1.5F);
		moveSpeed = 0.5F;
		attackStrength = 3;
		scoreValue = 200;
		heartsHalvesLife = 20;

		// A wraith is not solid. This is a synched shared flag, so the client agrees.
		setNoPhysics(true);

		mobDrops.add(new WeightedRandomLootObject(Items.BONE.getDefaultStack(), 0, 2));
	}

	@Override
	public int getMaxHealth() {
		return 10;
	}

	@Override
	protected boolean makeStepSound() {
		return false;
	}

	/** Nothing shoves a ghost. */
	@Override
	public boolean isPushable() {
		return false;
	}

	// --- Textures ---------------------------------------------------------------------------------

	protected String textureFolder() {
		return "wraith";
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/" + textureFolder() + "/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/" + textureFolder() + "/0.png";
	}

	// --- Sounds -----------------------------------------------------------------------------------

	@Override
	public String getLivingSound() {
		return "creatures:mob.wraith";
	}

	@Override
	protected String getHurtSound() {
		return "creatures:mob.wraith.hurt";
	}

	@Override
	protected String getDeathSound() {
		return "creatures:mob.wraith.death";
	}

	@Override
	protected float getSoundVolume() {
		return 0.6F;
	}

	// --- Life cycle -------------------------------------------------------------------------------

	/** {@code MobFlying} does not inherit the monster base, so peaceful clean-up is done here. */
	@Override
	public void tick() {
		super.tick();
		if (!world.isClientSide && !world.getDifficulty().canHostileMobsSpawn()) {
			remove();
		}
	}

	@Override
	public void onLivingUpdate() {
		if (!world.isClientSide) {
			updateAttackStrength();
			handleDaylight();
		}
		super.onLivingUpdate();
	}

	/** The original re-derived the wraith's bite from the difficulty on every tick. */
	protected void updateAttackStrength() {
		attackStrength = world.getDifficulty() == Difficulty.EASY ? 2 : 3;
	}

	/** Split out so the flame wraith, which is fire immune, can burn down differently. */
	protected void handleDaylight() {
		if (!world.isDaytime()) {
			return;
		}

		float brightness = calcBrightness(1.0F);
		if (brightness <= 0.5F) {
			return;
		}
		if (!world.canBlockSeeTheSky(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z))) {
			return;
		}
		if (random.nextFloat() * 30.0F >= (brightness - 0.4F) * 2.0F) {
			return;
		}

		Weather weather = world.getCurrentWeather();
		if (weather != null && weather.isDamp()) {
			return;
		}

		remainingFireTicks = DAYLIGHT_BURN_TICKS;
	}

	// --- AI ---------------------------------------------------------------------------------------

	@Override
	protected void updateAI() {
		if (world.isClientSide) {
			return;
		}

		if (hauntTarget != null && !hauntTarget.isAlive()) {
			hauntTarget = null;
		}
		if (hauntTarget == null && --retargetCooldown <= 0) {
			retargetCooldown = 20;
			Player player = world.getClosestPlayerToEntity(this, HUNT_RANGE);
			if (player != null && player.getGamemode().hasHostileMobs()) {
				hauntTarget = player;
			}
		}

		if (hauntTarget != null) {
			lookAt(hauntTarget, 30.0F, 30.0F);
			yBodyRot = yRot;

			driftTowards(hauntTarget.x, hauntTarget.y, hauntTarget.z);
			attackEntity(hauntTarget, hauntTarget.distanceTo(this));
			return;
		}

		wander();
	}

	/** Straight-line steering. Walls are not consulted; that is the whole point of a wraith. */
	protected void driftTowards(double tx, double ty, double tz) {
		double dx = tx - x;
		double dy = ty - y;
		double dz = tz - z;
		double length = Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
		if (length < 0.1D) {
			return;
		}

		xd += ((dx / length) * DRIFT_SPEED - xd) * DRIFT_ACCELERATION;
		yd += ((dy / length) * DRIFT_SPEED - yd) * DRIFT_ACCELERATION;
		zd += ((dz / length) * DRIFT_SPEED - zd) * DRIFT_ACCELERATION;
	}

	private void wander() {
		if (courseChangeCooldown-- <= 0 || distanceToSqr(waypointX, waypointY, waypointZ) < 4.0D) {
			courseChangeCooldown = 40 + random.nextInt(60);
			waypointX = x + (random.nextDouble() - 0.5D) * 16.0D;
			waypointY = y + (random.nextDouble() - 0.5D) * 8.0D;
			waypointZ = z + (random.nextDouble() - 0.5D) * 16.0D;

			// Stay inside the world even though blocks do not stop it.
			waypointY = Math.max(2.0D, Math.min(world.getHeightBlocks() - 2.0D, waypointY));
		}

		driftTowards(waypointX, waypointY, waypointZ);

		double dx = waypointX - x;
		double dz = waypointZ - z;
		if ((dx * dx) + (dz * dz) > 1.0E-4D) {
			yRot = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
			yBodyRot = yRot;
		}
	}

	protected void attackEntity(@NotNull Entity entity, float distance) {
		if (attackTime > 0 || distance >= ATTACK_REACH) {
			return;
		}
		if (entity.bb.maxY <= bb.minY || entity.bb.minY >= bb.maxY) {
			return;
		}

		attackTime = 20;
		entity.hurt(this, attackStrength, DamageType.COMBAT);
		onHitTarget(entity);
	}

	/** Hook for the flame variant. */
	protected void onHitTarget(@NotNull Entity entity) {
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (!super.hurt(attacker, damage, type)) {
			return false;
		}
		if (attacker != null && attacker != this && !(attacker instanceof Player
			&& !((Player) attacker).gamemode.hasHostileMobs())) {
			hauntTarget = attacker;
		}
		return true;
	}

	// --- Spawning ---------------------------------------------------------------------------------

	/**
	 * {@code MobFlying} has no monster spawn rules of its own, so the usual dark-and-unlit test is
	 * reproduced here.
	 */
	@Override
	public boolean canSpawnHere() {
		if (!world.getDifficulty().canHostileMobsSpawn()) {
			return false;
		}

		TilePos pos = new TilePos(x, bb.minY, z);
		if (world.getSavedLightValue(LightLayer.Block, pos) > 0) {
			return false;
		}
		if (world.getSavedLightValue(LightLayer.Sky, pos) > random.nextInt(32)) {
			return false;
		}
		return world.getBlockLightValue(pos) <= 4 && super.canSpawnHere();
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 2;
	}
}
