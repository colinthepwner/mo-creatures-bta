package teamport.creatures.core.entity.mob;

import net.minecraft.core.entity.animal.MobChicken;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemSeeds;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import teamport.creatures.MoreMobs;

public class MobDuck extends MobChicken {
	public MobDuck(World world) {
		super(world);

		setTextureIdentifier(MoreMobs.MOD_ID, "duck");

		setSize(0.3F, 0.4F);
	}

	@Override
	public int getMaxHealth() {
		return 4;
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

		Player player = world.getClosestPlayerToEntity(this, 16.0);
		if (player != null && (player.distanceToSqr(x, y, z) > 4.0)) {
			ItemStack heldStack = player.getCurrentEquippedItem();
			if (heldStack != null && heldStack.getItem() instanceof ItemSeeds) {

				lookAt(player, 30.0F, 30.0F);
				moveForward = 1.0F;

				if (player.distanceToSqr(this) <= 12.0) {
					moveForward = 0.0F;
				}
			}
		}
	}
}
