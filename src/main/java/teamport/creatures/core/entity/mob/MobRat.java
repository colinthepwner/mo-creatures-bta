package teamport.creatures.core.entity.mob;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.monster.MobMonster;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.entity.projectile.ProjectileArrow;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.MMUtils;

import java.util.List;

public class MobRat extends MobMonster {
	public static final int VARIANT_BROWN = 0;
	public static final int VARIANT_BLACK = 1;
	public static final int VARIANT_WHITE = 2;
	protected static final int VARIANT_COUNT = 3;

	protected static final float LIGHT_GIVE_UP = 0.5F;

	private static final double SWARM_CALL_RANGE = 16.0;

	private static final String[] VARIANT_TEXTURES = {"rat", "rat_black", "rat_white"};

	public MobRat(World world) {
		super(world);
		setTextureIdentifier(MoreMobs.MOD_ID, textureFolder());
		scoreValue = 50;
		moveSpeed = 0.6F;
		attackStrength = 1;

		setSize(0.5F, 0.5F);
		setSkinVariant(rollVariant());
		setHealthRaw(getMaxHealth());

		mobDrops.add(new WeightedRandomLootObject(Items.COAL.getDefaultStack(), 0, 1));
	}

	protected String textureFolder() {
		return "rat";
	}

	protected int rollVariant() {
		int roll = random.nextInt(100);
		if (roll <= 65) return VARIANT_BROWN;
		if (roll <= 98) return VARIANT_BLACK;
		return VARIANT_WHITE;
	}

	public int getVariant() {
		int variant = getSkinVariant();
		return variant < 0 || variant >= VARIANT_COUNT ? VARIANT_BROWN : variant;
	}

	@Override
	public int getMaxHealth() {
		return 10;
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/" + VARIANT_TEXTURES[getVariant()] + "/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/rat/0.png";
	}

	@Override
	protected boolean makeStepSound() {
		return false;
	}

	@Override
	public String getLivingSound() {
		return "creatures:mob.rat";
	}

	@Override
	protected String getHurtSound() {
		return "creatures:mob.rat.hurt";
	}

	@Override
	protected String getDeathSound() {
		return "creatures:mob.rat.death";
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F;
	}

	@Override
	public boolean canClimb() {
		return horizontalCollision;
	}

	@Override
	protected Entity findPlayerToAttack() {
		if (calcBrightness(1.0F) >= LIGHT_GIVE_UP) return null;

		Player player = world.getClosestPlayerToEntity(this, 16.0);
		if (player != null && !player.gamemode.hasHostileMobs()) return null;
		return player;
	}

	@Override
	protected void attackEntity(@NotNull Entity entity, float distance) {
		if (calcBrightness(1.0F) > LIGHT_GIVE_UP && random.nextInt(100) == 0) {
			setTarget(null);
			return;
		}
		super.attackEntity(entity, distance);
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (!super.hurt(attacker, damage, type)) return false;
		if (attacker == null) return true;

		if (attacker instanceof ProjectileArrow && ((ProjectileArrow) attacker).owner != null) {
			attacker = ((ProjectileArrow) attacker).owner;
		}
		if (attacker == this) return true;

		if (attacker instanceof Player) {
			setTarget(attacker);
		}

		List<MobRat> nest = world.getEntitiesWithinAABB(MobRat.class,
			MMUtils.grow(bb, SWARM_CALL_RANGE, 4.0, SWARM_CALL_RANGE));
		for (MobRat rat : nest) {
			if (rat != this && rat.getTarget() == null) {
				rat.setTarget(attacker);
			}
		}
		return true;
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 5;
	}
}
