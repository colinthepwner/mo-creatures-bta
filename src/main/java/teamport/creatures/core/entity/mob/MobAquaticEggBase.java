package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;

public abstract class MobAquaticEggBase extends Mob {
	private static final int HATCH_TICKS = 50;

	private int incubation;

	protected MobAquaticEggBase(World world) {
		super(world);
		heartsHalvesLife = 20;
		setSize(0.25F, 0.25F);
	}

	protected abstract Mob hatch();

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

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
