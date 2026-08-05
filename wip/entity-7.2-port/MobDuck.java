package teamport.creatures.core.entity.mob;

import net.minecraft.core.entity.animal.MobChicken;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.Items;
import net.minecraft.core.item.ItemSeeds;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;

public class MobDuck extends MobChicken {
	public MobDuck(World world) {
		super(world);
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/duck/0.png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/duck/0.png";
	}

	@Override
	public String getLivingSound() {
		return "creatures.duck";
	}

	@Override
	public String getHurtSound() {
		return "creatures.duckhurt";
	}

	@Override
	public String getDeathSound() {
		return "creatures.duckhurt";
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
				faceEntity(player, 30.0F, 30.0F);
				moveForward = 1.0F;

				if (player.distanceToSqr(this) <= 12.0)
					moveForward = 0.0F;
			}
		}
	}
}
