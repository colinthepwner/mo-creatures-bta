package teamport.creatures.core.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicRotatable;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePosc;

/**
 * The litter box. Right-clicking a filthy one with sand cleans it out again.
 * <p>
 * BTA 8.0 split blocks into a {@code Block} registry object plus a {@link net.minecraft.core.block.BlockLogic}
 * that carries the behaviour, so the 7.2 {@code LitterboxBlock extends BlockTileEntityRotatable} becomes
 * this logic class on top of {@link BlockLogicRotatable}. The tile entity is no longer created by
 * overriding a factory method either — it is attached at registration time in {@code MMBlocks}.
 * {@code onBlockRightClicked} was renamed to {@link #onInteracted} and takes a position object,
 * and {@code renderAsNormalBlock} became {@link #isCubeShaped}.
 */
public class LitterboxBlockLogic extends BlockLogicRotatable {

	public LitterboxBlockLogic(Block<?> block, Material material) {
		super(block, material);
		setBlockBounds(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);
	}

	@Override
	public boolean onInteracted(World world, TilePosc pos, Player player, Side side, double xHit, double yHit) {
		TileEntity tileEntity = world.getTileEntity(pos);
		ItemStack heldItem = player.getHeldItem();

		if (tileEntity instanceof LitterboxEntity && ((LitterboxEntity) tileEntity).isFilthy) {
			if (heldItem != null && heldItem.itemID == Blocks.SAND.id()) {
				((LitterboxEntity) tileEntity).isFilthy = false;
				heldItem.consumeItem(player);
			}
		}

		return true;
	}

	@Override
	public boolean isCubeShaped() {
		return false;
	}

	@Override
	public boolean isSolidRender() {
		return false;
	}

	@Override
	public ItemStack[] getBreakResult(World world, EnumDropCause dropCause, int meta, TileEntity tileEntity) {
		return new ItemStack[]{new ItemStack(block)};
	}
}
