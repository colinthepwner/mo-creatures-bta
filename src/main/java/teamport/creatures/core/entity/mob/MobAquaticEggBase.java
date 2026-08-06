package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * Shared behaviour for the two eggs.
 * <p>
 * An egg is a mob rather than a block so that it sinks, drifts and can be destroyed, but it has no
 * AI of its own: {@link #isMovementBlocked()} is pinned true, which makes {@code Mob} skip the AI
 * pass entirely and leaves nothing but gravity acting on it. The incubation therefore lives in
 * {@link #onLivingUpdate()}.
 * <p>
 * Incubation only advances underwater, so an egg thrown up a beach simply waits. The counter needs
 * {@link #HATCH_TICKS} successes at a one-in-twenty roll, which works out at roughly fifty seconds
 * of submersion.
 * <p>
 * The original had a matching <em>item</em> for each egg that a player could pocket and place. This
 * port has no custom items yet, so eggs exist only in entity form and are dropped straight into the
 * water by the adults.
 */
public abstract class MobAquaticEggBase extends Mob {
	private static final int HATCH_TICKS = 50;

	private int incubation;

	protected MobAquaticEggBase(World world) {
		super(world);
		heartsHalvesLife = 20;
		setSize(0.25F, 0.25F);
	}

	/** Builds the hatchling, already positioned by the caller. */
	protected abstract Mob hatch();

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		// An egg keeps the facing it was laid at; it has no reason to tilt or to turn.
		xRot = 0.0F;
		yBodyRot = yRot;

		if (world.isClientSide || !isInWater() || random.nextInt(20) != 0) {
			return;
		}

		if (++incubation < HATCH_TICKS) {
			return;
		}

		Mob hatchling = hatch();
		hatchling.moveTo(x, y, z, random.nextFloat() * 360.0F, 0.0F);
		world.entityJoinedWorld(hatchling);

		world.playSoundAtEntity(null, this, "mob.chickenplop", 1.0F,
			(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
		remove();
	}

	/** Never; an egg has no AI to run and nothing to gain from a walk cycle. */
	@Override
	protected boolean isMovementBlocked() {
		return true;
	}

	@Override
	protected boolean makeStepSound() {
		return false;
	}

	@Override
	protected void causeFallDamage(float distance) {
	}

	@Override
	public int getMaxHealth() {
		return 4;
	}

	/**
	 * An egg has nothing to say while it incubates, but it has four hearts and can be broken, so
	 * hitting one gives back vanilla's pop and cracking one open gives back the shell shattering.
	 */
	@Override
	public String getLivingSound() {
		return null;
	}

	@Override
	protected String getHurtSound() {
		return "random.pop";
	}

	@Override
	protected String getDeathSound() {
		return "random.glass";
	}

	@Override
	protected boolean canDespawn() {
		return false;
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("Incubation", incubation);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		incubation = tag.getInteger("Incubation");
	}
}
