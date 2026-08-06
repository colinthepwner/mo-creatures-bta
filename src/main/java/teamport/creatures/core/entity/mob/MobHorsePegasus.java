package teamport.creatures.core.entity.mob;

import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.input.PlayerInput;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;

/**
 * A horse with wings. Holding jump while saddled beats it upwards until it reaches half the world
 * height, it glides rather than falls, and it never takes fall damage.
 * <p>
 * The 7.2 source drove the wing beat with Dragonfly's {@code AnimationState}, which BTA 8.0 removed
 * along with the rest of the entity animation system. The cycle is kept here as a plain pair of
 * interpolatable floats instead — {@link #wingFlap} is the phase of the beat and
 * {@link #wingSpread} how far the wings are unfolded — and
 * {@code MobRendererHorsePegasus} turns them into bone rotations. One beat per second, matching the
 * 20-tick loop {@code animation/horse_pegasus.animation.json} used to play.
 */
public class MobHorsePegasus extends MobHorse {
	/** Phase of the wing beat, wrapped to the [0, 2) period of the renderer's sine. */
	public float wingFlap;
	public float oWingFlap;
	/** 0 while the wings are folded on the ground, 1 while they are beating. */
	public float wingSpread;
	public float oWingSpread;

	public MobHorsePegasus(World world) {
		super(world);
		moveSpeed = 0.45F;
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/horse/4.png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/horse/4.png";
	}

	/**
	 * The original shipped a pegasus call of its own (pegasus.ogg) that nothing in this port had
	 * been wired to; hurt and death still fall through to the ordinary horse lines.
	 */
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
		// An untamed pegasus bolts skyward rather than just bucking on the spot.
		jump();
		super.tickUntamedRider(rider);
	}

	@Override
	public void moveEntityWithHeading(float moveStrafing, float moveForward) {
		if (passenger != null) {
			if (isInWater() || isInLava()) ejectRider();

			if (isHorseSaddled()) {
				if (passenger instanceof PlayerLocal) {
					PlayerInput passengerInput = ((PlayerLocal) passenger).input;
					if (passengerInput.jump && y < (double) world.getHeightBlocks() / 2) jump();
					yRot = passenger.yRot;
					if (isInWater() || isInLava()) ejectRider();

					// Move faster in the air.
					if (!onGround) {
						super.moveRelative(passengerInput.moveStrafe, passengerInput.moveForward, moveSpeed / 6);
					} else {
						super.moveRelative(passengerInput.moveStrafe, passengerInput.moveForward, moveSpeed / 10);
					}

					super.moveEntityWithHeading(passengerInput.moveStrafe, passengerInput.moveForward);
				}
			} else super.moveEntityWithHeading(moveStrafing, moveForward);
		} else {
			super.moveEntityWithHeading(moveStrafing, moveForward);
		}
	}

	@Override
	protected void jump() {
		if (!world.isClientSide) {
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
			// Keep the phase small enough to stay precise, without breaking interpolation.
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
