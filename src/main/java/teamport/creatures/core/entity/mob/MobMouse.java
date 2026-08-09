package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.MMUtils;

public class MobMouse extends MobAnimal {
	public static final int VARIANT_GREY = 0;
	public static final int VARIANT_BROWN = 1;
	public static final int VARIANT_WHITE = 2;
	private static final int VARIANT_COUNT = 3;

	public static final int MASK_CARRIED = 0b0000_0001;
	public static final int DATA_GENERIC_FLAGS = 16;

	private static final String[] VARIANT_TEXTURES = {"mouse", "mouse_brown", "mouse_white"};

	private static final double FRIGHT_RANGE = 6.0;

	private static final double FLIGHT_DISTANCE = 8.0;

	public MobMouse(World world) {
		super(world);

		setTextureIdentifier(MoreMobs.MOD_ID, "mouse");
		setSize(0.3F, 0.3F);
		scoreValue = -5;
		moveSpeed = 0.9F;
		heartsHalvesLife = 10;

		setSkinVariant(rollVariant());
		setHealthRaw(getMaxHealth());

		mobDrops.add(new WeightedRandomLootObject(Items.SEEDS_WHEAT.getDefaultStack(), 0, 2));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_GENERIC_FLAGS, (byte) 0, Byte.class);
	}

	private int rollVariant() {
		int roll = random.nextInt(100);
		if (roll <= 50) return VARIANT_GREY;
		if (roll <= 80) return VARIANT_BROWN;
		return VARIANT_WHITE;
	}

	public int getVariant() {
		int variant = getSkinVariant();
		return variant < 0 || variant >= VARIANT_COUNT ? VARIANT_GREY : variant;
	}

	@Override
	public int getMaxHealth() {
		return 4;
	}

	public boolean isCarried() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_CARRIED) != 0;
	}

	public void setCarried(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		entityData.set(DATA_GENERIC_FLAGS, flag ? (byte) (data | MASK_CARRIED) : (byte) (data & ~MASK_CARRIED));
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/" + VARIANT_TEXTURES[getVariant()] + "/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/mouse/0.png";
	}

	@Override
	protected boolean makeStepSound() {
		return false;
	}

	@Override
	public String getLivingSound() {
		return "creatures:mob.mouse";
	}

	@Override
	protected String getHurtSound() {
		return "creatures:mob.mouse.hurt";
	}

	@Override
	protected String getDeathSound() {
		return "creatures:mob.mouse.death";
	}

	@Override
	protected float getSoundVolume() {
		return 0.3F;
	}

	@Override
	public boolean canClimb() {
		return horizontalCollision;
	}

	@Override
	public double getRidingHeight() {
		return isPassenger() ? heightOffset - 1.7 : heightOffset;
	}

	@Override
	protected void updateAI() {
		super.updateAI();

		if (!world.isClientSide && random.nextInt(15) == 0) {
			Entity threat = findThreat(FRIGHT_RANGE);
			if (threat != null) {
				bolt(threat);
			}
		}

		if (!onGround && isPassenger() && vehicle instanceof Entity) {
			yRot = ((Entity) vehicle).yRot;
		}
	}

	private Entity findThreat(double range) {
		Entity threat = null;
		for (Entity candidate : world.getEntitiesWithinAABBExcludingEntity(this, MMUtils.grow(bb, range, 4.0, range))) {
			if (!(candidate instanceof Mob) || candidate instanceof MobMouse) continue;
			threat = candidate;
		}
		return threat;
	}

	private void bolt(Entity threat) {
		double angle = Math.atan2(x - threat.x, z - threat.z)
			+ (random.nextFloat() - random.nextFloat()) * 0.75;
		int targetX = MathHelper.floor(x + Math.sin(angle) * FLIGHT_DISTANCE);
		int targetY = MathHelper.floor(bb.minY);
		int targetZ = MathHelper.floor(z + Math.cos(angle) * FLIGHT_DISTANCE);

		for (int attempt = 0; attempt < 16; attempt++) {
			int tileX = targetX + random.nextInt(4) - random.nextInt(4);
			int tileY = targetY + random.nextInt(3) - random.nextInt(3);
			int tileZ = targetZ + random.nextInt(4) - random.nextInt(4);
			if (tileY <= 4) continue;

			if (!isOpen(tileX, tileY, tileZ)) continue;
			if (isOpen(tileX, tileY - 1, tileZ)) continue;

			setPathToEntity(world.getEntityPathToXYZ(this, tileX, tileY, tileZ, 16.0F));
			return;
		}
	}

	private boolean isOpen(int tileX, int tileY, int tileZ) {
		Block<?> block = world.getBlockType(new TilePos(tileX, tileY, tileZ));
		return block == null || block == Blocks.AIR;
	}

	@Override
	public boolean interact(@NotNull Player player) {
		if (!world.isClientSide) {
			if (player.passenger == this) {
				player.ejectRider();
				setCarried(false);
				world.playSoundAtEntity(null, this, "mob.chickenplop", 1.0F,
					(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
			} else if (player.passenger == null) {
				yRot = player.yRot;
				startRiding(player);
				setCarried(isPassenger());
			}

			xd = player.xd * 5.0;
			yd = player.yd / 2.0 + 0.5;
			zd = player.zd * 5.0;
		}
		return true;
	}

	@Override
	public boolean canSpawnHere() {
		if (!world.checkIfAABBIsClear(bb) || world.getIsAnyLiquid(bb)) return false;
		if (!world.getEntitiesWithinAABBExcludingEntity(this, bb).isEmpty()) return false;

		Block<?> ground = world.getBlockType(new TilePos(MathHelper.floor(x),
			MathHelper.floor(bb.minY) - 1, MathHelper.floor(z)));
		return ground == Blocks.GRASS || ground == Blocks.DIRT || ground == Blocks.STONE
			|| ground == Blocks.SAND || ground == Blocks.GRAVEL;
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 6;
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {

		super.addAdditionalSaveData(tag);
		tag.putBoolean("IsCarried", isCarried());
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setCarried(tag.getBoolean("IsCarried"));
	}
}
