package teamport.creatures.core.entity.mob;

import net.minecraft.core.entity.animal.MobChicken;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemSeeds;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import teamport.creatures.MoreMobs;

/**
 * A duck is a chicken that swims. Egg laying, feather drops and the fall-damage exemption all come
 * from {@link MobChicken}; this adds the duck's own skin, voice, buoyancy and seed-following.
 */
public class MobDuck extends MobChicken {
	public MobDuck(World world) {
		super(world);
		// MobChicken's constructor points the texture identifier at minecraft:chicken, so it has to be
		// re-pointed after super(). This also rebases defaultTexture and variantJsonPath; the duck has
		// a single skin, so the missing variants.json resolves back to "0".
		setTextureIdentifier(MoreMobs.MOD_ID, "duck");
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/duck/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/duck/0.png";
	}

	@Override
	public String getLivingSound() {
		return "creatures:mob.duck";
	}

	@Override
	protected String getHurtSound() {
		return "creatures:mob.duck.hurt";
	}

	@Override
	protected String getDeathSound() {
		return "creatures:mob.duck.hurt";
	}

	/**
	 * Chickens sink; the base swim handling bleeds 0.02 off {@code yd} every tick a mob is submerged.
	 * Pushing back slightly harder than that leaves the duck riding the surface rather than walking
	 * along the bottom, which is the one thing that visibly separates it from its ancestor.
	 */
	@Override
	public void moveEntityWithHeading(float moveStrafing, float moveForward) {
		super.moveEntityWithHeading(moveStrafing, moveForward);

		if (isInWater()) {
			yd += 0.03;

			if (yd > 0.06) {
				yd = 0.06;
			}
		}
	}

	@Override
	protected void updateAI() {
		super.updateAI();
		// EXPERIMENTAL //
		// Player follow code for the upcoming 7.3 release. Follow items are: Seeds.
		Player player = world.getClosestPlayerToEntity(this, 16.0);
		if (player != null && (player.distanceToSqr(x, y, z) > 4.0)) {
			ItemStack heldStack = player.getCurrentEquippedItem();
			if (heldStack != null && heldStack.getItem() instanceof ItemSeeds) {
				// faceEntity is gone in BTA 8.0; lookAt is the same yaw/pitch-clamped turn.
				lookAt(player, 30.0F, 30.0F);
				moveForward = 1.0F;

				if (player.distanceToSqr(this) <= 12.0) {
					moveForward = 0.0F;
				}
			}
		}
	}
}
