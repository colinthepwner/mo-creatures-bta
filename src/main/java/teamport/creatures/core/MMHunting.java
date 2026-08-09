package teamport.creatures.core;

import java.util.List;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.animal.MobWolf;

import teamport.creatures.MMConfig;
import teamport.creatures.core.entity.mob.MobHorse;

public final class MMHunting {

	private MMHunting() {
	}

	public static boolean isHuntable(Entity candidate) {
		if (!MMConfig.huntersAttackHorses && candidate instanceof MobHorse) {
			return false;
		}
		return MMConfig.huntersAttackWolves || !(candidate instanceof MobWolf);
	}

	public static void devourDrops(Mob hunter, double radius, int maxItemAge) {
		if (!MMConfig.huntersDestroyDrops || hunter.world.isClientSide) {
			return;
		}

		List<EntityItem> drops = hunter.world.getEntitiesWithinAABB(EntityItem.class,
			MMUtils.grow(hunter.bb, radius, radius, radius));

		for (EntityItem drop : drops) {
			if (drop.age < maxItemAge) {
				drop.remove();
			}
		}
	}
}
