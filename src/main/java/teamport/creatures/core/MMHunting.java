package teamport.creatures.core;

import java.util.List;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.animal.MobWolf;

import teamport.creatures.MMConfig;
import teamport.creatures.core.entity.mob.MobHorse;

/**
 * The original's "Hunter Creatures" settings, in the two places every predator needs them.
 *
 * <p>DrZhark's mod read {@code HuntersAttackHorses}, {@code HuntersAttackWolves} and
 * {@code HuntersDestroyDrops} from five entities apiece — bear, big cat, shark, wild wolf and the
 * piranha — rather than from one place, and the settings existed because players kept losing their
 * horses and their dogs to a lion that wandered past. Same three questions here, asked once.
 */
public final class MMHunting {

	private MMHunting() {
	}

	/**
	 * Whether a predator is allowed to pick this out as prey.
	 *
	 * <p>Covers only the two exclusions the settings describe. Everything else a mob does not want to
	 * eat — its own kind, something too big, something already dead — stays that mob's own business,
	 * because none of it is configurable and all of it differs per predator.
	 *
	 * <p>{@link MobHorse} catches the unicorn and the pegasus too, both being subclasses, which is
	 * right: the original's check was {@code entity instanceof EntityHorse} and its unicorn and
	 * pegasus were variants of that one class.
	 */
	public static boolean isHuntable(Entity candidate) {
		if (!MMConfig.huntersAttackHorses && candidate instanceof MobHorse) {
			return false;
		}
		return MMConfig.huntersAttackWolves || !(candidate instanceof MobWolf);
	}

	/**
	 * Clears the remains of a kill, if {@code Hunters.destroyDrops} allows it.
	 *
	 * <p>Only items young enough to have come from the kill are taken. Anything older was already
	 * lying there and is very likely a player's, which is the distinction the original drew too once
	 * it stopped hunters eating everything in sight: "now the hunters only destroy newly spawned
	 * items not dropped by humans".
	 *
	 * @param maxItemAge age in ticks past which a drop is left alone
	 */
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
