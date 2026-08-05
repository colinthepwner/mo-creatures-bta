package teamport.creatures.core.entity.mob;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.monster.MobMonster;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.Difficulty;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.MMUtils;

import java.util.List;

/**
 * The wolf that runs with the werewolves — the original's {@code EntityWWolf}, and the shape a
 * {@link MobWerewolf} wears once it has turned. It shares the {@code werewolf_wolf} model.
 * <p>
 * Unlike BTA's tameable wolf this one is a monster: it stalks players in the dark, and when there is
 * nobody to hunt it goes after livestock instead. Its bite gets worse with the difficulty, which is
 * how the original scaled it. Having eaten, it wolfs down whatever the kill dropped — bounded to
 * items that have only just hit the ground, so a player's own dropped inventory is never on the menu.
 */
public class MobWerewolfWolf extends MobMonster {
	/** How far it will notice a player, in the dark. */
	public static final double HUNT_RANGE = 16.0D;
	/** How far it will look for something smaller to eat. */
	public static final double PREY_RANGE = 10.0D;
	/** Chance per tick of looking for prey when there is no player to chase. */
	public static final int PREY_SEARCH_CHANCE = 80;

	/** Whether a fed wolf eats the drops off its kill. */
	public static final boolean DEVOURS_DROPS = true;
	/** Radius it will clear drops within. */
	public static final double DEVOUR_RADIUS = 2.0D;
	/**
	 * Only items this fresh count as part of the kill. Anything older was already lying there and is
	 * very likely a player's, so the wolf leaves it alone.
	 */
	public static final int DEVOUR_MAX_ITEM_AGE = 40;

	public MobWerewolfWolf(World world) {
		super(world);
		setTextureIdentifier(MoreMobs.MOD_ID, "werewolf_wolf");

		setSize(0.9F, 1.3F);
		moveSpeed = 1.0F;
		attackStrength = 2;
		scoreValue = 150;
		heartsHalvesLife = 10;

		mobDrops.add(new WeightedRandomLootObject(Items.LEATHER.getDefaultStack(), 0, 2));
	}

	@Override
	public int getMaxHealth() {
		return 20;
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/werewolf_wolf/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/werewolf_wolf/0.png";
	}

	// Vanilla wolf sounds; this mod ships no wolf audio of its own and BTA's are a good match.
	@Override
	public String getLivingSound() {
		return "mob.wolf.growl";
	}

	@Override
	protected String getHurtSound() {
		return "mob.wolf.hurt";
	}

	@Override
	protected String getDeathSound() {
		return "mob.wolf.death";
	}

	@Override
	protected float getSoundVolume() {
		return 0.6F;
	}

	@Override
	protected void updateAI() {
		if (!world.isClientSide) {
			// The original re-read the difficulty every tick rather than at spawn, so a wolf that was
			// already loaded gets nastier the moment the world is turned up.
			Difficulty difficulty = world.getDifficulty();
			if (difficulty == Difficulty.HARD || difficulty == Difficulty.NORMAL) {
				attackStrength = 5;
			} else if (difficulty == Difficulty.EASY) {
				attackStrength = 3;
			} else {
				attackStrength = 1;
			}
		}
		super.updateAI();
	}

	@Override
	protected Entity findPlayerToAttack() {
		if (calcBrightness(1.0F) < 0.5F) {
			Player player = world.getClosestPlayerToEntity(this, HUNT_RANGE);
			if (player != null && canEntityBeSeen(player) && player.getGamemode().hasHostileMobs()) {
				return player;
			}
			return null;
		}

		// Daylight: no interest in players, but a sheep is a sheep.
		if (random.nextInt(PREY_SEARCH_CHANCE) == 0) {
			return findPrey();
		}
		return null;
	}

	/** Nearest visible animal that is not itself a predator. */
	protected Entity findPrey() {
		List<MobAnimal> nearby = world.getEntitiesWithinAABB(MobAnimal.class,
			MMUtils.grow(bb, PREY_RANGE, PREY_RANGE, PREY_RANGE));

		Mob closest = null;
		double closestDistance = -1.0D;

		for (MobAnimal candidate : nearby) {
			if (!candidate.isAlive() || isPackMate(candidate) || isTooBigToHunt(candidate)) {
				continue;
			}

			double distance = candidate.distanceToSqr(this);
			if (distance > PREY_RANGE * PREY_RANGE) {
				continue;
			}
			if ((closestDistance < 0.0D || distance < closestDistance) && canEntityBeSeen(candidate)) {
				closestDistance = distance;
				closest = candidate;
			}
		}

		return closest;
	}

	private boolean isPackMate(Entity entity) {
		return entity instanceof MobWerewolfWolf || entity instanceof MobWerewolf;
	}

	/** Bears and boars fight back hard enough that the original's wolves left them alone. */
	private boolean isTooBigToHunt(Entity entity) {
		return entity instanceof MobBear || entity instanceof MobBoar;
	}

	@Override
	protected void attackEntity(@NotNull Entity entity, float distance) {
		if (attackTime > 0 || distance >= 2.5F) {
			return;
		}
		if (entity.bb.maxY <= bb.minY || entity.bb.minY >= bb.maxY) {
			return;
		}

		attackTime = 20;
		entity.hurt(this, attackStrength, DamageType.COMBAT);

		if (!(entity instanceof Player)) {
			devourDrops();
		}
	}

	/** Clears the freshly dropped remains of a kill. Never touches anything that was already lying about. */
	protected void devourDrops() {
		if (!DEVOURS_DROPS || world.isClientSide) {
			return;
		}

		List<EntityItem> drops = world.getEntitiesWithinAABB(EntityItem.class,
			MMUtils.grow(bb, DEVOUR_RADIUS, DEVOUR_RADIUS, DEVOUR_RADIUS));

		for (EntityItem drop : drops) {
			if (drop.age < DEVOUR_MAX_ITEM_AGE) {
				drop.remove();
			}
		}
	}

	@Override
	public boolean canSpawnHere() {
		return world.getDifficulty().canHostileMobsSpawn() && super.canSpawnHere();
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 6;
	}
}
