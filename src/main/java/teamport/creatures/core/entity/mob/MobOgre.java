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

/**
 * The green ogre — the mod's signature block-breaking brute.
 * <p>
 * The original tracked a player up to a configurable "smell" range, and every so often triggered a
 * {@code DestroyBlast}: a full explosion-shaped ray cast that chewed a crater out of the terrain and
 * only spared blocks whose blast resistance was above a threshold. That is far too destructive to
 * port literally — a wandering ogre would quietly demolish a base while nobody was watching. The
 * behaviour kept here is the part the readme actually advertises, "will destroy blocks in their path
 * to their target": the ogre punches a hole through whatever is between it and whoever it is chasing,
 * and nothing else.
 * <p>
 * Everything about that is bounded by the {@code SMASH_*} constants below plus the vanilla
 * {@code doMobGriefing} game rule. See {@link #smashBlock(int, int, int)} for the per-block rules.
 * <p>
 * The three knobs the original itself exposed — how far an ogre notices you, how much it can chew
 * through, and the difficulty it spawns on — come from {@code config/creatures.cfg} through
 * {@link #awarenessRange()}, {@link #blastCeiling()} and {@link #spawnDifficulty()}, which the fire
 * and cave ogres override to read their own entries. The {@code SMASH_*} caps around them have no
 * counterpart in the original and are not configurable.
 */
public class MobOgre extends MobMonster {
	// --- Block destruction bounds. Config-friendly: every one of these is a hard cap, and the ---
	// --- feature as a whole is off if BLOCK_BREAKING_ENABLED is false.                        ---

	/** Master switch. With this off an ogre is a plain (if large) melee monster. */
	public static final boolean BLOCK_BREAKING_ENABLED = true;
	/** How far ahead of itself, in blocks, an ogre can reach. Deliberately barely past its own arms. */
	public static final int SMASH_REACH = 2;
	/**
	 * How far up from its own feet it reaches. Never reaches below them — see {@link #smashBlock}.
	 * An ogre stands four blocks tall, so anything less and it cannot fit through its own doorway.
	 */
	public static final int SMASH_HEIGHT = 4;
	/** Hard cap on blocks removed by a single swing. */
	public static final int MAX_BLOCKS_PER_SMASH = 6;
	/** Sideways offsets tried, centre first, so a capped-out swing still leaves a centred opening. */
	private static final int[] SMASH_SIDES = {0, -1, 1};
	/** Minimum ticks between swings that break anything. */
	public static final int SMASH_COOLDOWN_TICKS = 40;
	/** Furthest an ogre will bother tunnelling towards a target it cannot see. */
	public static final double SMASH_PURSUIT_RANGE = 12.0D;

	public static final int MASK_ANGRY = 0b0000_0001;
	public static final int MASK_ATTACKING = 0b0000_0010;
	public static final int DATA_GENERIC_FLAGS = 16;

	/** Ticks left on the swing pose. Server-side; the renderer reads {@link #MASK_ATTACKING} instead. */
	private int attackPoseTicks;
	private int smashCooldown;

	/** Set by the fire ogre: blocks it smashes are left burning. */
	protected boolean smashSetsFire;
	/**
	 * Set by the fire and cave ogres, which cook in the open. The green one does not — "Green ogres
	 * don't burn on sunlight, they become docile unless attacked".
	 */
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

	// --- Config hooks -----------------------------------------------------------------------------

	/**
	 * How far off this ogre smells a player — {@code HostileMobs.ogreRange}, the original's
	 * {@code OgreRange}, which {@code EntityOgre}'s constructor assigned straight to its
	 * {@code attackRange}.
	 *
	 * <p>The original's default is 12 and its slider stops at 24. The readme's "they can smell
	 * players 24 blocks apart" is a line from the 2.x changelog that predates the setting existing,
	 * so 24 is the ceiling rather than the norm; this port had been treating it as the norm.
	 */
	protected double awarenessRange() {
		return MMConfig.ogreRange;
	}

	/**
	 * Blocks at or above this blast resistance are immune. {@code Block.getBlastResistance} is
	 * {@code blastResistance / 5}, which puts dirt at 0.5, planks and glass at 1.0 and stone at
	 * exactly 2.0.
	 *
	 * <p>This used to be a flat 2.0, which spared stone, on the belief that the original did the
	 * same. It did not. {@code EntityOgre.DestroyingOgre} hands the job to
	 * {@code Destroyer.DestroyBlast(world, self, x, y + 1, z, destroyForce, bogrefire)}, and that is
	 * an explosion: it walks rays out from the ogre and tests each block's explosion resistance
	 * against a decaying blast intensity, exactly as TNT does. TNT clears stone, so an ogre did too —
	 * {@code destroyForce} is the configurable {@code mod_mocreatures.ogreStrength}, not a material
	 * cutoff. The surrounding caps (reach, height, six blocks a swing, never below its own feet, no
	 * tile entities, no fluids) are what keep it bounded instead.
	 *
	 * <p>Since there is no explosion here to give a force to, {@code HostileMobs.ogreStrength} scales
	 * this ceiling instead — see {@link MMConfig#blastCeiling}, which is pinned so the original's
	 * default of 2.5 gives the 60 this port already used. Obsidian and anything else built to survive
	 * TNT stays out of reach at any setting, and bedrock is excluded separately by its negative
	 * hardness.
	 */
	protected float blastCeiling() {
		return MMConfig.blastCeiling(MMConfig.ogreStrength);
	}

	/** Lowest difficulty this ogre spawns on — {@code ogreSpawnDifficulty}, meaning that or harder. */
	protected Difficulty spawnDifficulty() {
		return MMConfig.ogreSpawnDifficulty;
	}

	// --- Synched renderer state -------------------------------------------------------------------
	// Both flags drive the pose, so both have to be in entityData; a plain field would only ever be
	// true on the server and the client would draw an ogre that never swings.

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

	// --- Textures ---------------------------------------------------------------------------------

	/** Directory under {@code textures/entity} the asset bridge is expected to fill. */
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

	// --- Sounds -----------------------------------------------------------------------------------

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

	// --- AI ---------------------------------------------------------------------------------------

	/**
	 * The green ogre only hunts in the dark. In daylight it is, as the readme puts it, docile unless
	 * attacked — {@link #hurt} still hands it a target regardless of the light level.
	 */
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
		// A connecting swing also wrecks whatever the target was standing behind, which is what makes
		// fighting one in a doorway pointless.
		smashTowards(entity);
	}

	/** Hook for variants that do something extra on a landed hit. Only runs on a connecting swing. */
	protected void onHitTarget(@NotNull Entity entity) {
	}

	/**
	 * Called by {@code MobPathfinder} when the ogre has a target it cannot see — i.e. there is a wall
	 * in the way. This is where the mob earns its reputation. The base implementation is empty, so
	 * there is nothing to call through to.
	 */
	@Override
	protected void attackBlockedEntity(Entity entity, float distance) {
		// The swing pose only goes up if something actually came down, otherwise a cornered ogre
		// would windmill at a wall it is on cooldown for.
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

	// --- Block destruction ------------------------------------------------------------------------

	/**
	 * Removes up to {@link #MAX_BLOCKS_PER_SMASH} blocks from the slab of space directly between the
	 * ogre and {@code towards}. Refuses to run at all unless the game rule allows mob griefing.
	 *
	 * @return true if anything was actually broken
	 */
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

		// Sideways offset, so the ogre carves an opening wide enough to walk through rather than a
		// one-block peephole.
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

	/**
	 * The whole of the "may an ogre break this?" decision.
	 * <ul>
	 *   <li>Never below its own feet, so it cannot mine out the floor it (or a build) stands on.</li>
	 *   <li>Never the bottom world layer, and never an unloaded chunk.</li>
	 *   <li>Never anything unbreakable — {@code withSetUnbreakable()} sets hardness to -1, which is
	 *       what bedrock uses.</li>
	 *   <li>Never a block with a tile entity: chests, furnaces, signs, spawners, and this mod's own
	 *       litterbox all keep their contents.</li>
	 *   <li>Never a fluid, so an ogre by the coast cannot drain the sea.</li>
	 *   <li>Never anything at or above {@link #blastCeiling()}.</li>
	 * </ul>
	 *
	 * @return true if a block was actually removed
	 */
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

	// --- Daylight ---------------------------------------------------------------------------------

	/**
	 * Fire and cave ogres cook in the sun; green ones do not. The original docked health outright
	 * rather than setting the mob alight, which is the only thing that would work for the fire ogre
	 * (it is fire immune), so that is kept.
	 */
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

	// --- Spawning ---------------------------------------------------------------------------------

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
