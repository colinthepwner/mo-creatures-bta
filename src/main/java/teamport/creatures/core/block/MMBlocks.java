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

	public static final Block<LitterboxBlockLogic> LITTERBOX = new BlockBuilder(MOD_ID)
		.setBlockSound(BlockSounds.WOOD)
		.setTileEntity(LitterboxEntity::new)
		.setBlockItem(MMBlocks::litterboxItem)
		.setCreativeInventoryPlacement(new CreativeInventoryPlacement.Category(CreativeInventoryCategory.PLACEABLES))
		.build("litterbox", nextID(), block -> new LitterboxBlockLogic(block, Materials.WOOD));

	private static ItemBlock<?> litterboxItem(Block<?> block) {
		ItemBlock<?> item = new ItemBlock<>(block);
		item.setMaxStackSize(1);
		return item;
	}

	static {

		EntityHelper.addMapping(LitterboxEntity.class, NamespaceID.getPermanent(MOD_ID, "litterbox"));
	}
}
