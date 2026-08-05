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

/**
 * Shared behaviour for the mod's three swimmers.
 * <p>
 * {@link MobWaterAnimal} only supplies the spawn category and a "does not drown" flag; everything
 * that makes a fish behave like a fish is here. Three things are worth calling out:
 * <ul>
 *   <li><b>Movement is not the base class's.</b> {@code Mob.moveEntityWithHeading}'s water branch
 *       accelerates at a fixed 0.02 and bleeds 0.02 off {@code yd} every tick, which caps a swimmer
 *       at roughly two blocks a second and walks it along the seabed. This class drives the
 *       velocity itself so speed is tunable per mob and the mob can climb and dive on purpose.</li>
 *   <li><b>Suffocation is inverted.</b> {@code Mob.trySuffocate} drowns things that are underwater
 *       and cannot breathe there. A fish's problem is the opposite, so the check is flipped — the
 *       same shape BTA's own squid uses.</li>
 *   <li><b>Growth is synched.</b> The renderer scales the model by it, so it cannot be a plain
 *       field. {@code SynchedEntityData} has no float accessor, hence the fixed-point short.</li>
 * </ul>
 */
public abstract class MobAquaticBase extends MobWaterAnimal {
	/** Render scale in thousandths — see the class note on why this is a short rather than a float. */
	public static final int DATA_GROWTH = 16;

	/** Fraction of its velocity a swimmer keeps each tick. Water is thick. */
	protected static final double WATER_DRAG = 0.86;
	/** Degrees of heading change allowed per tick while wandering. */
	protected static final float TURN_RATE = 8.0F;
	/** Ceiling on the vertical thrust any one tick may apply. */
	protected static final double MAX_CLIMB = 0.06;

	/**
	 * Vertical thrust for the current tick. Purely motion — the client never reads it, so unlike
	 * growth it is fine as a plain field.
	 */
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

	// --- growth -------------------------------------------------------------------------------

	/** Uniform model scale, and the mod's stand-in for age. */
	public float getGrowth() {
		return entityData.getShort(DATA_GROWTH) / 1000.0F;
	}

	public void setGrowth(float growth) {
		entityData.set(DATA_GROWTH, (short) Math.round(MathHelper.clamp(growth, 0.05F, 30.0F) * 1000.0F));
	}

	/** Growth at which the mob counts as grown: it can breed, fight and be ridden. */
	public abstract float adultGrowth();

	/** Growth a newly hatched or newly born one starts at. */
	public abstract float babyGrowth();

	public boolean isAdult() {
		return getGrowth() >= adultGrowth();
	}

	/** One-in-N chance per tick of putting on {@link #growthStep()}. */
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

	// --- movement -----------------------------------------------------------------------------

	/** Forward acceleration per tick. Terminal speed works out at roughly {@code thrust / 0.14}. */
	protected float swimThrust() {
		return 0.03F;
	}

	/** Throttle used when the mob has nowhere in particular to be. */
	protected float cruiseSpeed() {
		return 0.6F;
	}

	@Override
	public void moveEntityWithHeading(float strafe, float forward) {
		if (!isInWater()) {
			// Beached: fall and skid like anything else. The flopping is in onLivingUpdate.
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

	/**
	 * The walk-cycle counters normally fall out of the base class's own movement code, which this
	 * class bypasses; without this the fins and tail would freeze. The vertical component is folded
	 * in as well, otherwise a fish diving straight down stops moving its tail.
	 */
	private void updateSwimAnimation() {
		walkAnimSpeedO = walkAnimSpeed;

		double dx = x - xo;
		double dy = y - yo;
		double dz = z - zo;
		float travelled = Math.min(MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 4.0F, 1.0F);

		walkAnimSpeed += (travelled - walkAnimSpeed) * 0.4F;
		walkAnimPos += walkAnimSpeed;
	}

	// --- AI -----------------------------------------------------------------------------------

	/**
	 * One-in-N chance per tick of looking for something to chase; zero for mobs that never pick a
	 * target of their own accord.
	 */
	protected int targetSearchInterval() {
		return 0;
	}

	/** Distance past which a chase is abandoned. */
	protected float loseTargetRange() {
		return 24.0F;
	}

	/** Chooses something to chase, or null. Only called when the mob has no target. */
	protected Entity findSwimTarget() {
		return null;
	}

	/**
	 * Steering hook run before the target/wander decision, for behaviour that outranks it — the
	 * dolphin's interest in dropped fish, the fishy's schooling. Returns true if it took the wheel.
	 */
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
			// Stranded. There is no steering to be had on dry land, and a fish that walks around
			// looks far worse than one that lies there flopping.
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

	/** Points the mob at something and opens the throttle, climbing or diving to match its depth. */
	protected void swimToward(Entity destination, float throttle) {
		lookAt(destination, 30.0F, 30.0F);
		moveForward = throttle;
		climb = MathHelper.clamp((destination.y - y) * 0.03, -MAX_CLIMB, MAX_CLIMB);
	}

	/** Lazy meander: hold a heading for a while, then pick a new one. */
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

	/**
	 * Keeps the mob off the surface and off the seabed. Swimming into air stalls it, and dragging
	 * along the bottom looks wrong; both corrections are gentle enough that a deliberate chase
	 * still wins.
	 */
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

	// --- air, spawning, housekeeping ----------------------------------------------------------

	/**
	 * Inverted from {@link net.minecraft.core.entity.Mob#trySuffocate()}: air is what kills a fish,
	 * not water. Same shape as BTA's own squid, except that the test is "any part of me is wet"
	 * rather than "my eyes are under" — a dolphin cruising along the surface is not drowning.
	 */
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

		// Stranded. Thrashing occasionally throws it far enough to find water again, which is the
		// only way out short of a player picking it up.
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
