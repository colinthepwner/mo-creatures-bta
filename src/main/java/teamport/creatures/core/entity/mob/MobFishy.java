package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Items;
import net.minecraft.core.player.gamemode.Gamemodes;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MMConfig;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.MMHunting;
import teamport.creatures.core.MMUtils;

import java.util.List;
import java.util.Random;

public class MobFishy extends MobAquaticBase {
	public static final int VARIETIES = 10;

	public static final int PIRANHA = 9;

	private static final float ADULT_GROWTH = 1.0F;
	private static final float BABY_GROWTH = 0.3F;
	private static final float REACH = 2.0F;

	private static final double SCHOOL_RANGE = 6.0;

	public MobFishy(World world) {
		super(world);
		textureIdentifier = new NamespaceID(MoreMobs.MOD_ID, "fishy");
		scoreValue = 10;
		heartsHalvesLife = 10;

		setSize(0.3F, 0.3F);
		setVariety(rollVariety(random));
		setGrowth(0.6F + random.nextFloat() * (ADULT_GROWTH - 0.6F));

		mobDrops.add(new WeightedRandomLootObject(Items.FOOD_FISH_RAW.getDefaultStack(), 1));
	}

	public int getVariety() {
		return Math.floorMod(getSkinVariant(), VARIETIES);
	}

	public void setVariety(int variety) {
		setSkinVariant(Math.floorMod(variety, VARIETIES));
	}

	public boolean isPiranha() {
		return getVariety() == PIRANHA;
	}

	public static int rollVariety(Random random) {
		return random.nextInt(MMConfig.spawnPiranhas ? VARIETIES : PIRANHA);
	}

	@Override
	public float adultGrowth() {
		return ADULT_GROWTH;
	}

	@Override
	public float babyGrowth() {
		return BABY_GROWTH;
	}

	@Override
	protected int growthInterval() {
		return 100;
	}

	@Override
	protected float growthStep() {
		return 0.02F;
	}

	@Override
	protected float swimThrust() {
		return 0.035F;
	}

	@Override
	protected float cruiseSpeed() {
		return 0.5F;
	}

	@Override
	protected int targetSearchInterval() {
		return isPiranha() ? 30 : 0;
	}

	@Override
	protected float loseTargetRange() {
		return 12.0F;
	}

	@Override
	protected Entity findSwimTarget() {
		if (!isPiranha() || !isAdult() || !world.getDifficulty().canHostileMobsSpawn()) {
			return null;
		}

		Player player = world.getClosestPlayerToEntity(this, 12.0);
		if (player != null && player.isAlive() && player.gamemode != Gamemodes.CREATIVE) {
			return player;
		}

		Mob nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (Mob candidate : world.getEntitiesWithinAABB(Mob.class, MMUtils.grow(bb, 12.0, 6.0, 12.0))) {
			if (candidate == this || !candidate.isAlive() || candidate instanceof MobAquaticBase
				|| candidate instanceof MobSharkEgg || candidate instanceof MobFishyEgg
				|| candidate instanceof Player

				|| !MMHunting.isHuntable(candidate)) {
				continue;
			}
			double distance = distanceToSqr(candidate);
			if (distance < nearestDistance) {
				nearest = candidate;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	@Override
	protected boolean steerSpecial() {
		if (isPiranha()) {
			return false;
		}

		List<MobFishy> shoal = world.getEntitiesWithinAABB(MobFishy.class,
			MMUtils.grow(bb, SCHOOL_RANGE, SCHOOL_RANGE / 2.0, SCHOOL_RANGE));

		double centreX = 0.0;
		double centreY = 0.0;
		double centreZ = 0.0;
		float headingSin = 0.0F;
		float headingCos = 0.0F;
		int counted = 0;

		for (MobFishy other : shoal) {
			if (other == this || other.getVariety() != getVariety()) {
				continue;
			}
			centreX += other.x;
			centreY += other.y;
			centreZ += other.z;

			headingSin += MathHelper.sin(other.yRot * MathHelper.DEG_TO_RAD);
			headingCos += MathHelper.cos(other.yRot * MathHelper.DEG_TO_RAD);
			counted++;
		}

		if (counted == 0) {
			return false;
		}

		centreX /= counted;
		centreY /= counted;
		centreZ /= counted;

		float shoalYaw = (float) Math.toDegrees(Math.atan2(headingSin, headingCos));
		float towardsCentre = (float) Math.toDegrees(Math.atan2(centreZ - z, centreX - x)) - 90.0F;

		double spread = distanceToSqr(centreX, centreY, centreZ);
		float desired = spread > 9.0 ? towardsCentre : shoalYaw;

		yRot = rotationLerp(yRot, desired, TURN_RATE);
		moveForward = cruiseSpeed();
		climb += MathHelper.clamp((centreY - y) * 0.02, -0.02, 0.02);
		return true;
	}

	@Override
	protected void attackEntity(@NotNull Entity entity, float distance) {
		if (distance >= REACH || !isPiranha() || !isAdult()) {
			return;
		}
		if (entity.bb.maxY <= bb.minY || entity.bb.minY >= bb.maxY) {
			return;
		}

		attackTime = 20;
		entity.hurt(this, 1, DamageType.COMBAT);
	}

	@Override
	protected void dropDeathItems() {
		if (isAdult() && random.nextInt(100) < 70) {
			super.dropDeathItems();
			return;
		}

		int clutch = 1 + random.nextInt(2);
		for (int i = 0; i < clutch; i++) {
			MobFishyEgg egg = new MobFishyEgg(world);
			egg.moveTo(x, y, z, random.nextFloat() * 360.0F, 0.0F);
			egg.setVariety(getVariety());
			world.entityJoinedWorld(egg);
		}
	}

	@Override
	public int getMaxHealth() {
		return 4;
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/fishy/" + getVariety() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/fishy/0.png";
	}

	@Override
	public String getLivingSound() {
		return "liquid.splash";
	}

	@Override
	protected String getHurtSound() {
		return "damage.hurtflesh";
	}

	@Override
	protected String getDeathSound() {
		return "damage.hurtflesh";
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 6;
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("Variety", getVariety());
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.containsKey("Variety")) {
			setVariety(tag.getInteger("Variety"));
		}
	}
}
