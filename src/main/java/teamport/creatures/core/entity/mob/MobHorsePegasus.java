package teamport.creatures.core.entity.mob;

import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import teamport.creatures.core.MMRiderInput;

public class MobHorsePegasus extends MobHorse {

	public float wingFlap;
	public float oWingFlap;

	public float wingSpread;
	public float oWingSpread;

	public MobHorsePegasus(World world) {
		super(world);
		moveSpeed = 0.45F;
	}

	@Override
	public int geneticValue() {
		return 5;
	}

	@Override
	public String textureIndex() {
		return "4";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/horse/4.png";
	}

	@Override
	public String getLivingSound() {
		return "creatures:mob.horse.pegasus";
	}

	@Override
	protected int annoyanceRate() {
		return 40;
	}

	@Override
	protected int annoyanceLimit() {
		return 400;
	}

	@Override
	protected int tameRate() {
		return 15;
	}

	@Override
	protected int tameThreshold() {
		return 1600;
	}

	@Override
	protected double buckStrength() {
		return 1.0;
	}

	@Override
	protected int tameParticleCount() {
		return 1;
	}

	@Override
	protected void tickUntamedRider(Player rider) {

		jump();
		super.tickUntamedRider(rider);
	}

	@Override

	public void moveEntityWithHeading(float moveStrafing, float moveForward) {
		if (passenger == null) {
			super.moveEntityWithHeading(moveStrafing, moveForward);
			return;
		}

		if (isInWater() || isInLava()) ejectRider();

		MMRiderInput passengerInput = isHorseSaddled() ? MMRiderInput.of(passenger) : null;
		if (passengerInput != null) {

			if (passengerInput.jump && y < world.getHeightBlocks()) jump();
			yRot = passenger.yRot;
			if (isInWater() || isInLava()) ejectRider();

			if (!onGround) {
				super.moveRelative(passengerInput.strafe, passengerInput.forward, moveSpeed / 6);
			} else {
				super.moveRelative(passengerInput.strafe, passengerInput.forward, moveSpeed / 10);
			}

			super.moveEntityWithHeading(passengerInput.strafe, passengerInput.forward);
			return;
		}

		super.moveEntityWithHeading(0.0F, 0.0F);
	}

	@Override
	protected void jump() {
		if (passenger == null) {
			super.jump();
			yd = 0.52;
		} else {
			yd = 0.21;
			if (isSprinting()) {
				float f = yRot * 0.01745329F;
				xd -= MathHelper.sin(f) * 0.2F;
				zd += MathHelper.cos(f) * 0.2F;
			}
		}
	}

	@Override
	public void onLivingUpdate() {
		oWingFlap = wingFlap;
		oWingSpread = wingSpread;

		if (!onGround && (yd < 0.0 || yd > 0.0)) {
			yd *= 0.75;
			wingSpread = Math.min(1.0F, wingSpread + 0.2F);
		} else {
			wingSpread = Math.max(0.0F, wingSpread - 0.2F);
		}

		wingFlap += 0.05F;
		if (wingFlap >= 2.0F) {

			wingFlap -= 2.0F;
			oWingFlap -= 2.0F;
		}

		super.onLivingUpdate();
	}

	@Override
	protected void causeFallDamage(float distance) {
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 1;
	}
}
