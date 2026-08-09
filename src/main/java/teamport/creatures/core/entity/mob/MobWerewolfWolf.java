package teamport.creatures.core.entity.mob;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
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
import teamport.creatures.core.MMHunting;
import teamport.creatures.core.MMUtils;

import java.util.List;

public class MobWerewolfWolf extends MobMonster {

	public static final double HUNT_RANGE = 16.0D;

	public static final double PREY_RANGE = 10.0D;

	public static final int PREY_SEARCH_CHANCE = 80;

	public static final double DEVOUR_RADIUS = 2.0D;

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

		if (random.nextInt(PREY_SEARCH_CHANCE) == 0) {
			return findPrey();
		}
		return null;
	}

	protected Entity findPrey() {
		List<MobAnimal> nearby = world.getEntitiesWithinAABB(MobAnimal.class,
			MMUtils.grow(bb, PREY_RANGE, PREY_RANGE, PREY_RANGE));

		Mob closest = null;
		double closestDistance = -1.0D;

		for (MobAnimal candidate : nearby) {

			if (!candidate.isAlive() || isPackMate(candidate) || isTooBigToHunt(candidate)
				|| !MMHunting.isHuntable(candidate)) {
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

	protected void devourDrops() {
		MMHunting.devourDrops(this, DEVOUR_RADIUS, DEVOUR_MAX_ITEM_AGE);
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
