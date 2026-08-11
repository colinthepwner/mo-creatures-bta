package teamport.creatures.core.entity.mob;

import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.core.MMUtils;

public class MobHorseUnicorn extends MobHorse {
	public MobHorseUnicorn(World world) {
		super(world);
		moveSpeed = 1.4F;
	}

	@Override
	public int geneticValue() {
		return 4;
	}

	@Override
	public String textureIndex() {
		return "3";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/horse/3.png";
	}

	@Override
	protected int annoyanceRate() {
		return 30;
	}

	@Override
	protected int tameRate() {
		return 15;
	}

	@Override
	protected int tameThreshold() {
		return 1200;
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
	protected boolean isFollowItem(ItemStack stack) {
		return super.isFollowItem(stack) || stack.itemID == Items.FOOD_APPLE_GOLD.id;
	}

	@Override
	public boolean interact(@NotNull Player player) {
		super.interact(player);
		ItemStack item = player.inventory.getCurrentItem();
		if (item != null) {
			if (item.itemID == Items.FOOD_APPLE_GOLD.id) {
				chanceForTame += random.nextInt(100) + 1;
				MMUtils.consumeHeld(player, item);
				playEatingSound();
			}
		}
		return false;
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 1;
	}
}
