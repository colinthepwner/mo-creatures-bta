package teamport.creatures.core.item;

import net.minecraft.core.item.Item;
import net.minecraft.core.item.Items;
import teamport.creatures.MMConfig;
import turniplabs.halplibe.helper.ItemBuilder;
import turniplabs.halplibe.helper.creativeInventory.CreativeInventoryCategory;
import turniplabs.halplibe.helper.creativeInventory.CreativeInventoryPlacement;

import static teamport.creatures.MoreMobs.MOD_ID;

public final class MMItems {
	private static int startingID = MMConfig.cfg.getInt("IDs.startingItemID");
	private static int nextID() {
		return ++startingID;
	}

	private static final ItemBuilder DROP = new ItemBuilder(MOD_ID)
		.setCreativeInventoryPlacement(new CreativeInventoryPlacement.Category(CreativeInventoryCategory.DROPS));
	private static final ItemBuilder FEED = new ItemBuilder(MOD_ID)
		.setCreativeInventoryPlacement(new CreativeInventoryPlacement.Category(CreativeInventoryCategory.FOOD));

	public static final Item BIGCAT_CLAW = DROP.build(item("bigcat_claw"));

	public static final Item SHARK_TEETH = DROP.build(item("shark_teeth"));

	public static final Item SUGAR_LUMP = FEED.build(item("sugar_lump"));

	public static final Item PET_FOOD = FEED.build(item("pet_food"));

	public static void addItemsToTags() {
		Items.EGG_CHICKEN.withTags(MMItemTags.FOXES_FAVORITE_ITEM);
		Items.SEEDS_WHEAT.withTags(MMItemTags.BIRDS_FAVORITE_ITEM);
		Items.SEEDS_PUMPKIN.withTags(MMItemTags.BIRDS_FAVORITE_ITEM);
		Items.WHEAT.withTags(MMItemTags.BUNNIES_FAVORITE_ITEM);
	}

	private static Item item(String name) {
		return new Item(name, MOD_ID + ":item/" + name, nextID());
	}
}
