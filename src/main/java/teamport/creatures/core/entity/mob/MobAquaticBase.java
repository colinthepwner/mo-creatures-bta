package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.material.Materials;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.animal.MobWaterAnimal;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import org.jetbrains.annotations.NotNull;

public abstract class MobAquaticBase extends MobWaterAnimal {

	public static final int DATA_GROWTH = 16;

	protected static final double WATER_DRAG = 0.86;

	protected static final float TURN_RATE = 8.0F;

	protected static final double MAX_CLIMB = 0.06;

	protected double climb;

	private float wanderYaw;
	private double wanderClimb;
	private boolean hasWanderHeading;

	protected MobAquaticBase(World world) {
		super(world);
		heartsHalvesLife = 20;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_GROWTH, (short) 1000, Short.class);
	}

	public float getGrowth() {
		return entityData.getShort(DATA_GROWTH) / 1000.0F;
	}

	public void setGrowth(float growth) {
		entityData.set(DATA_GROWTH, (short) Math.round(MathHelper.clamp(growth, 0.05F, 30.0F) * 1000.0F));
	}

	public abstract float adultGrowth();

	public abstract float babyGrowth();

	public boolean isAdult() {
		return getGrowth() >= adultGrowth();
	}

	protected int growthInterval() {
		return 50;
	}

	protected float growthStep() {
		return 0.01F;
	}

	protected void tickGrowth() {
		if (isAdult() || random.nextInt(growthInterval()) != 0) {
			return;
		}
		setGrowth(Math.min(getGrowth() + growthStep(), adultGrowth()));
	}

	protected float swimThrust() {
		return 0.03F;
	}

	protected float cruiseSpeed() {
		return 0.6F;
	}

	@Override
	public void moveEntityWithHeading(float strafe, float forward) {
		if (!isInWater()) {

			super.moveEntityWithHeading(strafe, forward);
			return;
		}

		moveRelative(strafe, forward, swimThrust());
		yd += climb;
		move(xd, yd, zd);

		xd *= WATER_DRAG;
		yd *= WATER_DRAG;
		zd *= WATER_DRAG;

		updateSwimAnimation();
	}

	private void updateSwimAnimation() {
		walkAnimSpeedO = walkAnimSpeed;

		double dx = x - xo;
		double dy = y - yo;
		double dz = z - zo;
		float travelled = Math.min(MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 4.0F, 1.0F);

		walkAnimSpeed += (travelled - walkAnimSpeed) * 0.4F;
		walkAnimPos += walkAnimSpeed;
	}

	protected int targetSearchInterval() {
		return 0;
	}

	protected float loseTargetRange() {
		return 24.0F;
	}

	protected Entity findSwimTarget() {
		return null;
	}

	protected boolean steerSpecial() {
		return false;
	}

	@Override
	protected void updateAI() {
		tickGrowth();
		climb = 0.0;

		if (isMovementBlocked()) {
			tryToDespawn();
			return;
		}

		if (!isInWater()) {

			moveForward = 0.0F;
			moveStrafing = 0.0F;
			setTarget(null);
			tryToDespawn();
			return;
		}

		Entity chase = getTarget();
		if (chase != null && (!chase.isAlive() || distanceTo(chase) > loseTargetRange())) {
			setTarget(null);
			chase = null;
		}
		if (chase == null && targetSearchInterval() > 0 && random.nextInt(targetSearchInterval()) == 0) {
			chase = findSwimTarget();
			setTarget(chase);
		}

		if (chase != null) {
			chaseTarget(chase);
		} else if (!steerSpecial()) {
			wander();
		}

		holdDepth();
		tryToDespawn();
	}

	protected void chaseTarget(Entity chase) {
		swimToward(chase, 1.0F);
		attackEntity(chase, distanceTo(chase));
	}

	protected void swimToward(Entity destination, float throttle) {
		lookAt(destination, 30.0F, 30.0F);
		moveForward = throttle;
		climb = MathHelper.clamp((destination.y - y) * 0.03, -MAX_CLIMB, MAX_CLIMB);
	}

	protected void wander() {
		if (!hasWanderHeading || random.nextInt(70) == 0) {
			wanderYaw = yRot + (random.nextFloat() - random.nextFloat()) * 90.0F;
			wanderClimb = (random.nextDouble() - random.nextDouble()) * 0.01;
			hasWanderHeading = true;
		}

		yRot = rotationLerp(yRot, wanderYaw, TURN_RATE);
		moveForward = cruiseSpeed();
		climb += wanderClimb;
	}

	protected void holdDepth() {
		if (!isInWater()) {
			return;
		}

		if (!isWaterAt(x, bb.maxY + 1.0, z)) {
			climb -= 0.02;
		} else if (!isWaterAt(x, bb.minY - 0.6, z)) {
			climb += 0.02;
		}
		climb = MathHelper.clamp(climb, -MAX_CLIMB, MAX_CLIMB);
	}

	protected boolean isWaterAt(double px, double py, double pz) {
		TilePos pos = new TilePos(MathHelper.floor(px), MathHelper.floor(py), MathHelper.floor(pz));
		return world.getBlockMaterial(pos) == Materials.WATER;
	}

	@Override
	public void trySuffocate() {
		if (isAlive() && !isInWater()) {
			airSupply--;
			if (airSupply == -20) {
				airSupply = 0;
				hurt(null, 2, DamageType.DROWN);
			}
			remainingFireTicks = 0;
		} else {
			airSupply = airMaxSupply;
		}
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (isInWater() || isPassenger()) {
			return;
		}

		if (onGround && random.nextInt(5) == 0) {
			yd = 0.25 + random.nextDouble() * 0.15;
			xd = (random.nextDouble() - 0.5) * 0.3;
			zd = (random.nextDouble() - 0.5) * 0.3;
			yRot = random.nextFloat() * 360.0F;
		}
	}

	@Override
	protected boolean makeStepSound() {
		return false;
	}

	@Override
	public boolean canSpawnHere() {
		return isWaterAt(x, y, z) && super.canSpawnHere();
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putFloat("Growth", getGrowth());
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.containsKey("Growth")) {
			setGrowth(tag.getFloat("Growth"));
		}
	}
}
