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
import teamport.creatures.core.MMUtils;

public class MobHorseNightmare extends MobHorse {

	private static final int CHARGE = 500;

	private int fireCharge = 0;

	public MobHorseNightmare(World world) {
		super(world);

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

		return "/assets/creatures/textures/entity/horse/2.png";
	}

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
			MMUtils.consumeHeld(player, item);
			fireCharge = CHARGE;
			playEatingSound();
			return false;
		}
		return super.interact(player);
	}

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
