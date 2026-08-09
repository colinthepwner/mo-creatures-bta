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
import teamport.creatures.MMConfig;
import teamport.creatures.MoreMobs;

public class MobWraith extends MobFlying implements Enemy {

	public static final double HUNT_RANGE = 20.0D;

	public static final double DRIFT_SPEED = 0.2D;

	public static final double DRIFT_ACCELERATION = 0.12D;

	public static final float ATTACK_REACH = 2.5F;

	public static final int DAYLIGHT_BURN_TICKS = 300;

	public static final int HAUNT_CEILING = 6;

	public static final int HAUNT_DEPTH = 20;

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

	@Override
	public boolean isPushable() {
		return false;
	}

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

	protected void updateAttackStrength() {
		attackStrength = world.getDifficulty() == Difficulty.EASY ? 2 : 3;
	}

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

			waypointY = tetherToFloor(waypointX, waypointY, waypointZ);
		}

		driftTowards(waypointX, waypointY, waypointZ);

		double dx2 = waypointX - x;
		double dz2 = waypointZ - z;
		if ((dx2 * dx2) + (dz2 * dz2) > 1.0E-4D) {
			yRot = (float) (Math.atan2(dz2, dx2) * 180.0D / Math.PI) - 90.0F;
			yBodyRot = yRot;
		}
	}

	private double tetherToFloor(double wx, double wy, double wz) {
		int bx = MathHelper.floor(wx);
		int bz = MathHelper.floor(wz);
		int top = Math.min(MathHelper.floor(wy), world.getHeightBlocks() - 1);

		int floor = 2;
		for (int by = Math.max(top, 2); by > 1; by--) {
			if (world.getBlockId(bx, by, bz) != 0) {
				floor = by + 1;
				break;
			}
		}

		double lowest = Math.max(2.0D, floor - HAUNT_DEPTH);
		double highest = Math.min(world.getHeightBlocks() - 2.0D, floor + HAUNT_CEILING);
		return Math.max(lowest, Math.min(highest, wy));
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

	@Override
	public boolean canSpawnHere() {

		if (!MMConfig.spawnsAt(world.getDifficulty(), MMConfig.wraithSpawnDifficulty)) {
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
