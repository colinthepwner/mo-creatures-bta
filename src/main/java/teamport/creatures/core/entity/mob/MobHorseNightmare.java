package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import org.jetbrains.annotations.NotNull;

/**
 * Genetic value 7, out of a pairing that adds to 9 — a unicorn and a pegasus. Fast, tough and fire
 * proof, and it will lay a trail of fire behind it once its ability has been charged.
 * <p>
 * The ability is not free and does not last: feeding the nightmare redstone sets a counter of 500,
 * and every second tick it is ridden with charge left it drops fire where its hooves were and takes
 * a tick off the counter. The rider does not burn — the original puts their fire out on the same
 * pass, which its changelog records as a deliberate fix rather than an accident.
 */
public class MobHorseNightmare extends MobHorse {
	/** Ticks of fire-laying a single feed buys, and the ceiling past which it will not eat more. */
	private static final int CHARGE = 500;

	private int fireCharge = 0;

	public MobHorseNightmare(World world) {
		super(world);
		// HorseSpeed 1.3 in the original, the same as a unicorn's, so it takes the unicorn's figure.
		moveSpeed = 1.4F;
		fireImmune = true;
	}

	@Override
	public int geneticValue() {
		return 7;
	}

	@Override
	public String textureIndex() {
		return "6";
	}

	@Override
	public String getDefaultEntityTexture() {
		// No built-in art for the bred horses; the black coat is the closest this repo ships.
		return "/assets/creatures/textures/entity/horse/2.png";
	}

	@Override
	public int getMaxHealth() {
		return 50;
	}

	/** Temper 700: harder to break than anything short of a black pegasus. */
	@Override
	protected int annoyanceLimit() {
		return 700;
	}

	@Override
	protected int annoyanceRate() {
		return 40;
	}

	@Override
	protected int tameThreshold() {
		return 1600;
	}

	@Override
	protected double buckStrength() {
		return 1.0;
	}

	@Override
	protected int tameParticleCount() {
		return 1;
	}

	@Override
	public boolean interact(@NotNull Player player) {
		ItemStack item = player.inventory.getCurrentItem();
		if (item != null && isHorseTamed()
			&& item.itemID == Items.DUST_REDSTONE.id && fireCharge <= CHARGE) {
			item.consumeItem(player);
			fireCharge = CHARGE;
			playEatingSound();
			return false;
		}
		return super.interact(player);
	}

	/**
	 * Fire where the hooves were, on every second tick it is ridden. Placed only where fire will
	 * actually take, so a nightmare ridden over water or across bare stone leaves nothing behind.
	 */
	private void layFire() {
		TilePos pos = new TilePos(
			MathHelper.floor(x), MathHelper.floor(bb.minY), MathHelper.floor(z));

		Block<?> here = world.getBlockType(pos);
		if (here == null || here == Blocks.AIR) {
			TilePos below = new TilePos(pos.x, pos.y - 1, pos.z);
			Block<?> floor = world.getBlockType(below);
			if (floor != null && floor != Blocks.AIR && floor.isSolidRender()) {
				world.setBlockTypeNotify(pos, Blocks.FIRE);
			}
		}

		if (passenger instanceof Mob) {
			((Mob) passenger).remainingFireTicks = 0;
		}

		fireCharge--;
	}

	@Override
	protected void updateAI() {
		super.updateAI();
		if (!world.isClientSide && passenger != null && fireCharge > 0 && random.nextInt(2) == 0) {
			layFire();
		}
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("FireCharge", fireCharge);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		fireCharge = tag.getInteger("FireCharge");
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 1;
	}
}
