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
