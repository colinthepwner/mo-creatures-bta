package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.data.gamerule.GameRules;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.monster.MobMonster;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.Difficulty;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MMConfig;
import teamport.creatures.MoreMobs;

public class MobOgre extends MobMonster {

	public static final boolean BLOCK_BREAKING_ENABLED = true;

	public static final int SMASH_REACH = 2;

	public static final int SMASH_HEIGHT = 4;

	public static final int MAX_BLOCKS_PER_SMASH = 6;

	private static final int[] SMASH_SIDES = {0, -1, 1};

	public static final int SMASH_COOLDOWN_TICKS = 40;

	public static final double SMASH_PURSUIT_RANGE = 12.0D;

	public static final int MASK_ANGRY = 0b0000_0001;
	public static final int MASK_ATTACKING = 0b0000_0010;
	public static final int DATA_GENERIC_FLAGS = 16;

	private int attackPoseTicks;
	private int smashCooldown;

	protected boolean smashSetsFire;

	protected boolean burnsInDaylight;

	public MobOgre(World world) {
		super(world);
		setTextureIdentifier(MoreMobs.MOD_ID, "ogre");

		setSize(1.5F, 4.0F);
		moveSpeed = 0.5F;
		attackStrength = 3;
		scoreValue = 300;
		heartsHalvesLife = 60;

		mobDrops.add(new WeightedRandomLootObject(Items.LEATHER.getDefaultStack(), 1, 3));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_GENERIC_FLAGS, (byte) 0, Byte.class);
	}

	@Override
	public int getMaxHealth() {
		return 35;
	}

	protected double awarenessRange() {
		return MMConfig.ogreRange;
	}

	protected float blastCeiling() {
		return MMConfig.blastCeiling(MMConfig.ogreStrength);
	}

	protected Difficulty spawnDifficulty() {
		return MMConfig.ogreSpawnDifficulty;
	}

	public boolean isOgreAngry() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_ANGRY) != 0;
	}

	public void setOgreAngry(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		entityData.set(DATA_GENERIC_FLAGS, flag ? (byte) (data | MASK_ANGRY) : (byte) (data & ~MASK_ANGRY));
	}

	public boolean isOgreAttacking() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_ATTACKING) != 0;
	}

	public void setOgreAttacking(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		entityData.set(DATA_GENERIC_FLAGS, flag ? (byte) (data | MASK_ATTACKING) : (byte) (data & ~MASK_ATTACKING));
	}

	protected String textureFolder() {
		return "ogre";
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/" + textureFolder() + "/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/" + textureFolder() + "/0.png";
	}

	@Override
	public String getLivingSound() {
		return "creatures:mob.ogre";
	}

	@Override
	protected String getHurtSound() {
		return "creatures:mob.ogre.hurt";
	}

	@Override
	protected String getDeathSound() {
		return "creatures:mob.ogre.death";
	}

	@Override
	protected float getSoundVolume() {
		return 1.0F;
	}

	@Override
	protected Entity findPlayerToAttack() {
		if (calcBrightness(1.0F) >= 0.5F) {
			return null;
		}

		Player player = world.getClosestPlayerToEntity(this, awarenessRange());
		if (player != null && player.getGamemode().hasHostileMobs()) {
			return player;
		}
		return null;
	}

	@Override
	protected void updateAI() {
		super.updateAI();

		if (world.isClientSide) {
			return;
		}

		if (smashCooldown > 0) {
			smashCooldown--;
		}
		if (attackPoseTicks > 0 && --attackPoseTicks == 0) {
			setOgreAttacking(false);
		}

		setOgreAngry(target != null && target.isAlive());
	}

	@Override
	protected void attackEntity(@NotNull Entity entity, float distance) {
		if (attackTime > 0 || distance >= 3.0F) {
			return;
		}
		if (entity.bb.maxY <= bb.minY || entity.bb.minY >= bb.maxY) {
			return;
		}

		attackTime = 20;
		beginSwing();
		entity.hurt(this, attackStrength, DamageType.COMBAT);
		onHitTarget(entity);

		smashTowards(entity);
	}

	protected void onHitTarget(@NotNull Entity entity) {
	}

	@Override
	protected void attackBlockedEntity(Entity entity, float distance) {

		if (distance <= SMASH_PURSUIT_RANGE && smashTowards(entity)) {
			beginSwing();
		}
	}

	private void beginSwing() {
		if (world.isClientSide) {
			return;
		}
		setOgreAttacking(true);
		attackPoseTicks = 10;
	}

	protected boolean smashTowards(Entity towards) {
		if (!BLOCK_BREAKING_ENABLED || world.isClientSide || smashCooldown > 0) {
			return false;
		}
		if (!Boolean.TRUE.equals(world.getGameRuleValue(GameRules.DO_MOB_GRIEFING))) {
			return false;
		}

		double dirX;
		double dirZ;
		if (towards != null) {
			dirX = towards.x - x;
			dirZ = towards.z - z;
		} else {
			dirX = -MathHelper.sin(yRot * MathHelper.DEG_TO_RAD);
			dirZ = MathHelper.cos(yRot * MathHelper.DEG_TO_RAD);
		}

		double length = Math.sqrt((dirX * dirX) + (dirZ * dirZ));
		if (length < 1.0E-4D) {
			return false;
		}
		dirX /= length;
		dirZ /= length;

		double sideX = -dirZ;
		double sideZ = dirX;

		int feetY = MathHelper.floor(bb.minY);
		int broken = 0;

		for (int step = 1; step <= SMASH_REACH && broken < MAX_BLOCKS_PER_SMASH; step++) {
			for (int sideIndex = 0; sideIndex < SMASH_SIDES.length && broken < MAX_BLOCKS_PER_SMASH; sideIndex++) {
				int side = SMASH_SIDES[sideIndex];
				for (int up = 0; up < SMASH_HEIGHT && broken < MAX_BLOCKS_PER_SMASH; up++) {
					int bx = MathHelper.floor(x + (dirX * step) + (sideX * side));
					int by = feetY + up;
					int bz = MathHelper.floor(z + (dirZ * step) + (sideZ * side));

					if (smashBlock(bx, by, bz)) {
						broken++;
					}
				}
			}
		}

		if (broken > 0) {
			smashCooldown = SMASH_COOLDOWN_TICKS;
			world.playSoundAtEntity(null, this, "creatures:mob.ogre.destroy", 1.0F,
				(random.nextFloat() - random.nextFloat()) * 0.2F + 0.8F);
			return true;
		}
		return false;
	}

	protected boolean smashBlock(int bx, int by, int bz) {
		if (by < MathHelper.floor(bb.minY) || by < 1 || by >= world.getHeightBlocks()) {
			return false;
		}

		TilePos pos = new TilePos(bx, by, bz);
		if (!world.isBlockLoaded(pos)) {
			return false;
		}

		Block<?> block = world.getBlockType(pos);
		if (!canSmash(block)) {
			return false;
		}

		block.dropWithCause(world, EnumDropCause.EXPLOSION, pos, world.getBlockData(pos), world.getTileEntity(pos), null);
		world.setBlockTypeNotify(pos, Blocks.AIR);
		block.onDestroyedByExplosion(world, pos);

		if (smashSetsFire && random.nextInt(3) == 0) {
			TilePos below = new TilePos(bx, by - 1, bz);
			Block<?> floor = world.getBlockType(below);
			if (floor != null && floor != Blocks.AIR && floor.isSolidRender()) {
				world.setBlockTypeNotify(pos, Blocks.FIRE);
			}
		}

		return true;
	}

	protected boolean canSmash(Block<?> block) {
		if (block == null || block == Blocks.AIR) {
			return false;
		}
		if (block.getHardness() < 0.0F) {
			return false;
		}
		if (block.isEntityTile) {
			return false;
		}
		if (block.hasTag(BlockTags.IS_WATER) || block.hasTag(BlockTags.IS_LAVA) || block.hasTag(BlockTags.IS_ACID)) {
			return false;
		}
		return block.getBlastResistance(this) < blastCeiling();
	}

	@Override
	public void onLivingUpdate() {
		if (burnsInDaylight && !world.isClientSide && world.isDaytime()) {
			float brightness = calcBrightness(1.0F);
			if (brightness > 0.5F
				&& world.canBlockSeeTheSky(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z))
				&& random.nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F) {
				hurt(null, 2, DamageType.FIRE);
			}
		}
		super.onLivingUpdate();
	}

	@Override
	public boolean canSpawnHere() {
		return MMConfig.spawnsAt(world.getDifficulty(), spawnDifficulty()) && super.canSpawnHere();
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 3;
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Angry", isOgreAngry());
		tag.putInt("SmashCooldown", smashCooldown);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setOgreAngry(tag.getBoolean("Angry"));
		smashCooldown = tag.getInteger("SmashCooldown");
	}
}
