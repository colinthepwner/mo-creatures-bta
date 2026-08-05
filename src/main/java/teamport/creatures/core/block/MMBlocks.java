package teamport.creatures.core.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.material.Materials;
import net.minecraft.core.item.block.ItemBlock;
import net.minecraft.core.sound.BlockSounds;
import net.minecraft.core.util.collection.NamespaceID;
import teamport.creatures.MMConfig;
import turniplabs.halplibe.helper.BlockBuilder;
import turniplabs.halplibe.helper.EntityHelper;
import turniplabs.halplibe.helper.creativeInventory.CreativeInventoryCategory;
import turniplabs.halplibe.helper.creativeInventory.CreativeInventoryPlacement;

import static teamport.creatures.MoreMobs.MOD_ID;

public final class MMBlocks {
	private static int startingID = MMConfig.cfg.getInt("IDs.startingBlockID");
	private static int nextID() {
		return ++startingID;
	}

	/**
	 * The kitty's litter box. In BTA 8.0 the behaviour lives in a separately supplied
	 * {@code BlockLogic} and the tile entity is attached here rather than by overriding a factory on
	 * the block, so {@code BlockBuilder.setTileEntity} replaces 7.2's {@code getNewBlockEntity}.
	 * The block model is left unassigned on purpose — an unregistered block falls through to the
	 * dispatcher's empty model, which is what the 7.2 {@code setBlockModel(BlockModelEmpty::new)}
	 * asked for, since everything visible is drawn by {@code LitterboxRenderer}.
	 */
	public static final Block<LitterboxBlockLogic> LITTERBOX = new BlockBuilder(MOD_ID)
		.setBlockSound(BlockSounds.WOOD)
		.setTileEntity(LitterboxEntity::new)
		.setBlockItem(MMBlocks::litterboxItem)
		.setCreativeInventoryPlacement(new CreativeInventoryPlacement.Category(CreativeInventoryCategory.PLACEABLES))
		.build("litterbox", nextID(), block -> new LitterboxBlockLogic(block, Materials.WOOD));

	/**
	 * 7.2 gave the litter box a hand-rolled {@code ItemPlaceable} capped at one per stack. BTA 8.0
	 * derives the placement item from the block itself, so all that is left to carry over is the
	 * stack limit — which is why {@code MMItems} has no litter box entry of its own.
	 */
	private static ItemBlock<?> litterboxItem(Block<?> block) {
		ItemBlock<?> item = new ItemBlock<>(block);
		item.setMaxStackSize(1);
		return item;
	}

	static {
		// Tile entities are keyed by NamespaceID in 8.0 rather than by the old free-form save string.
		EntityHelper.addMapping(LitterboxEntity.class, NamespaceID.getPermanent(MOD_ID, "litterbox"));
	}
}
