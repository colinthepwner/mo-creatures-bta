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

/**
 * Mo' Creatures' mouse: four hearts of prey animal that runs from absolutely everything, climbs walls
 * to get away, and can be picked up by the tail and carried around.
 * <p>
 * Coat colour is a variant on this one class and lives in {@link #getSkinVariant()}, BTA 8.0's
 * built-in variant slot — already synched, already saved by {@link Mob} as {@code SkinVariant}.
 * "Being carried" is synched too, in a flags byte, because the renderer hangs the mouse upside down
 * when it is: a plain field would never reach the client and every carried mouse would render the
 * right way up.
 */
public class MobMouse extends MobAnimal {
	public static final int VARIANT_GREY = 0;
	public static final int VARIANT_BROWN = 1;
	public static final int VARIANT_WHITE = 2;
	private static final int VARIANT_COUNT = 3;

	public static final int MASK_CARRIED = 0b0000_0001;
	public static final int DATA_GENERIC_FLAGS = 16;

	private static final String[] VARIANT_TEXTURES = {"mouse", "mouse_brown", "mouse_white"};

	/** How far a mouse notices something worth running from. */
	private static final double FRIGHT_RANGE = 6.0;
	/** How far it tries to get away in one bolt. */
	private static final double FLIGHT_DISTANCE = 8.0;

	public MobMouse(World world) {
		super(world);
		// The setter, not a bare textureIdentifier assignment: it also repoints variantJsonPath away
		// from the player skin's variant list, which getTextureReference() would otherwise resolve
		// against and hand back a char skin key instead of "0".
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

	/** The original's coat odds: 51% grey, 30% brown, 19% white. */
	private int rollVariant() {
		int roll = random.nextInt(100);
		if (roll <= 50) return VARIANT_GREY;
		if (roll <= 80) return VARIANT_BROWN;
		return VARIANT_WHITE;
	}

	/** Clamped so a stray byte cannot index off the end of the texture table. */
	public int getVariant() {
		int variant = getSkinVariant();
		return variant < 0 || variant >= VARIANT_COUNT ? VARIANT_GREY : variant;
	}

	@Override
	public int getMaxHealth() {
		return 4;
	}

	/** True while the mouse is dangling from someone's hand; the renderer turns it upside down. */
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

	/** Mice scale walls, the same trick the rats use. */
	@Override
	public boolean canClimb() {
		return horizontalCollision;
	}

	/** Held by the tail, so it hangs below the hand rather than sitting on top of it. */
	@Override
	public double getRidingHeight() {
		return isPassenger() ? heightOffset - 1.7 : heightOffset;
	}

	@Override
	protected void updateAI() {
		super.updateAI();

		// Roughly four times a second, check whether anything alive is close enough to be alarming.
		if (!world.isClientSide && random.nextInt(15) == 0) {
			Entity threat = findThreat(FRIGHT_RANGE);
			if (threat != null) {
				bolt(threat);
			}
		}

		// While being carried, swing round to face the same way the carrier does.
		if (!onGround && isPassenger() && vehicle instanceof Entity) {
			yRot = ((Entity) vehicle).yRot;
		}
	}

	/** Anything living that is not another mouse. Mice are not brave about this. */
	private Entity findThreat(double range) {
		Entity threat = null;
		for (Entity candidate : world.getEntitiesWithinAABBExcludingEntity(this, MMUtils.grow(bb, range, 4.0, range))) {
			if (!(candidate instanceof Mob) || candidate instanceof MobMouse) continue;
			threat = candidate;
		}
		return threat;
	}

	/**
	 * Runs directly away from the threat, but not in a straight line — the heading is jittered, and
	 * the mouse then hunts around that point for somewhere it can actually path to, which is what
	 * makes a startled mouse look like it is panicking rather than retreating.
	 */
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

			// Somewhere it can stand: open space with solid ground under it.
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

	/**
	 * Right click picks the mouse up by the tail; right click again drops it, with a squeak and a
	 * shove in whatever direction the carrier was moving.
	 */
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

	/**
	 * Mice will settle on more than just grass — 2.12.1 widened this specifically so they were not
	 * confined to meadows.
	 */
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
		// Coat colour rides along in Mob's own "SkinVariant" key.
		super.addAdditionalSaveData(tag);
		tag.putBoolean("IsCarried", isCarried());
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setCarried(tag.getBoolean("IsCarried"));
	}
}
