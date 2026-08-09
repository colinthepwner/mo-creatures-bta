package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.monster.MobMonster;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.item.tool.ItemToolSword;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MMConfig;
import teamport.creatures.MoreMobs;

public class MobWerewolf extends MobMonster {
	public static final int MASK_TRANSFORMED = 0b0000_0001;
	public static final int MASK_TRANSFORMING = 0b0000_0010;
	public static final int MASK_HUNCHED = 0b0000_0100;
	public static final int DATA_GENERIC_FLAGS = 16;

	public static final int TRANSFORM_CHANCE = 250;

	public static final int TRANSFORM_LENGTH = 30;

	public static final int UNARMED_DAMAGE = 1;

	public static final int SILVER_MULTIPLIER = 2;

	public static final int HEALTH_HUMAN = 15;
	public static final int HEALTH_BEAST = 40;

	private int transformCounter;

	public MobWerewolf(World world) {
		super(world);
		setTextureIdentifier(MoreMobs.MOD_ID, "werewolf");

		setSize(0.9F, 1.3F);
		moveSpeed = 0.7F;
		attackStrength = 4;
		scoreValue = 250;
		heartsHalvesLife = 20;

		mobDrops.add(new WeightedRandomLootObject(Items.LEATHER.getDefaultStack(), 0, 2));
		mobDrops.add(new WeightedRandomLootObject(Items.BONE.getDefaultStack(), 0, 2));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_GENERIC_FLAGS, (byte) 0, Byte.class);
	}

	public boolean isTransformed() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_TRANSFORMED) != 0;
	}

	public void setTransformed(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		entityData.set(DATA_GENERIC_FLAGS, flag ? (byte) (data | MASK_TRANSFORMED) : (byte) (data & ~MASK_TRANSFORMED));
	}

	public boolean isTransforming() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_TRANSFORMING) != 0;
	}

	public void setTransforming(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		entityData.set(DATA_GENERIC_FLAGS, flag ? (byte) (data | MASK_TRANSFORMING) : (byte) (data & ~MASK_TRANSFORMING));
	}

	public boolean isHunched() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_HUNCHED) != 0;
	}

	public void setHunched(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		entityData.set(DATA_GENERIC_FLAGS, flag ? (byte) (data | MASK_HUNCHED) : (byte) (data & ~MASK_HUNCHED));
	}

	@Override
	public int getMaxHealth() {
		return isTransformed() ? HEALTH_BEAST : HEALTH_HUMAN;
	}

	@Override
	public String getEntityTexture() {
		String folder = isTransformed() ? "werewolf_beast" : "werewolf";
		return "/assets/creatures/textures/entity/" + folder + "/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/werewolf/0.png";
	}

	@Override
	public String getLivingSound() {
		return isTransformed() ? "creatures:mob.werewolf" : "random.breath";
	}

	@Override
	protected String getHurtSound() {
		return isTransformed() ? "creatures:mob.werewolf.hurt" : "creatures:mob.werewolf.human.hurt";
	}

	@Override
	protected String getDeathSound() {
		return isTransformed() ? "creatures:mob.werewolf.death" : "creatures:mob.werewolf.human.death";
	}

	public boolean isNight() {
		return !world.isDaytime();
	}

	@Override
	protected Entity findPlayerToAttack() {
		if (!isTransformed()) {
			return null;
		}
		return super.findPlayerToAttack();
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (world.isClientSide) {
			return;
		}

		if (!isTransforming() && isNight() != isTransformed() && random.nextInt(TRANSFORM_CHANCE) == 0) {
			setTransforming(true);
			transformCounter = 0;
		}

		if (isTransforming()) {
			tickTransformation();
		}
	}

	@Override
	protected void updateAI() {

		if (isTransforming()) {
			moveStrafing = 0.0F;
			moveForward = 0.0F;
			randomYawVelocity = 0.0F;
			isJumping = false;
			return;
		}

		super.updateAI();

		if (world.isClientSide) {
			return;
		}

		if (!isTransformed()) {

			setTarget(null);
			setHunched(false);
			return;
		}

		if (target != null && (Math.abs(target.x - x) > 3.0D || Math.abs(target.z - z) > 3.0D)) {
			setHunched(true);
		} else if (isHunched() && random.nextInt(50) == 0) {
			setHunched(false);
		}
	}

	private void tickTransformation() {
		if (random.nextInt(3) != 0) {
			return;
		}
		transformCounter++;

		if (transformCounter % 2 == 0) {
			setPos(x + 0.3D, y, z);
			hurt(null, 1, DamageType.GENERIC);
		} else {
			setPos(x - 0.3D, y, z);
		}

		if (transformCounter == 10) {
			world.playSoundAtEntity(null, this, "creatures:mob.werewolf.transform", 1.0F,
				(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
		}

		if (transformCounter > TRANSFORM_LENGTH) {
			transform();
		}
	}

	public void transform() {
		for (int i = 0; i < 30; i++) {
			world.spawnParticle("explode",
				x + (random.nextFloat() - 0.5D) * bbWidth,
				bb.minY + random.nextFloat() * bbHeight,
				z + (random.nextFloat() - 0.5D) * bbWidth,
				(random.nextFloat() - 0.5D) * 0.1D, random.nextFloat() * 0.1D, (random.nextFloat() - 0.5D) * 0.1D,
				0, false);
		}

		setTransformed(!isTransformed());
		setTransforming(false);
		setHunched(false);
		setTarget(null);
		transformCounter = 0;
		setHealthRaw(getMaxHealth());
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (isTransformed() && attacker instanceof Player) {
			damage = beastDamageFrom((Player) attacker, damage);
		}
		return super.hurt(attacker, damage, type);
	}

	private int beastDamageFrom(Player player, int damage) {
		ItemStack held = player.getHeldItem();
		if (held == null || !(held.getItem() instanceof ItemToolSword)) {
			return UNARMED_DAMAGE;
		}
		if (held.getItem() == Items.TOOL_SWORD_GOLD) {
			return damage * SILVER_MULTIPLIER;
		}
		return damage;
	}

	@Override
	protected void attackEntity(@NotNull Entity entity, float distance) {
		if (!isTransformed()) {
			setTarget(null);
			return;
		}

		if (distance > 2.0F && distance < 6.0F && random.nextInt(15) == 0 && onGround) {
			setHunched(true);

			double dx = entity.x - x;
			double dz = entity.z - z;
			float length = MathHelper.sqrt((dx * dx) + (dz * dz));
			if (length > 1.0E-4F) {
				xd = ((dx / length) * 0.5D * 0.8D) + (xd * 0.2D);
				zd = ((dz / length) * 0.5D * 0.8D) + (zd * 0.2D);
				yd = 0.4D;
			}
			return;
		}

		super.attackEntity(entity, distance);
	}

	@Override
	public boolean canSpawnHere() {
		return MMConfig.spawnsAt(world.getDifficulty(), MMConfig.werewolfSpawnDifficulty)
			&& super.canSpawnHere();
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 1;
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Transformed", isTransformed());
		tag.putBoolean("Transforming", isTransforming());
		tag.putInt("TransformCounter", transformCounter);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setTransformed(tag.getBoolean("Transformed"));
		setTransforming(tag.getBoolean("Transforming"));
		transformCounter = tag.getInteger("TransformCounter");
	}
}
