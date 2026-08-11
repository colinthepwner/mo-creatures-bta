package teamport.creatures.core;

import net.minecraft.core.item.IItemConvertible;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import teamport.creatures.core.item.MMItems;
import turniplabs.halplibe.event.defs.CommonEvents;
import turniplabs.halplibe.helper.RecipeBuilder;
import turniplabs.halplibe.util.dependency.Key;

import static teamport.creatures.MoreMobs.MOD_ID;

public final class MMRecipes {
	private MMRecipes() {}

	public static void init() {
		CommonEvents.RECIPES_NAMESPACE_INIT.listen(Key.of(MOD_ID), () -> RecipeBuilder.initNameSpace(MOD_ID));
		CommonEvents.RECIPES_READY.listen(Key.of(MOD_ID), MMRecipes::onRecipesReady);
	}

	private static void onRecipesReady() {

		RecipeBuilder.Shapeless(MOD_ID)
			.addInput(Items.DUST_SUGAR)
			.addInput(Items.DUST_SUGAR)
			.addInput(Items.WHEAT)
			.create("sugar_lump", MMItems.SUGAR_LUMP.getDefaultStack());

		RecipeBuilder.Shapeless(MOD_ID)
			.addInput(MMItems.SHARK_TEETH)
			.create("shark_teeth_to_bone", Items.BONE.getDefaultStack());

		petFood("pet_food_bone", Items.BONE);
		petFood("pet_food_shark_teeth", MMItems.SHARK_TEETH);
		petFood("pet_food_bigcat_claw", MMItems.BIGCAT_CLAW);
	}

	private static void petFood(String id, IItemConvertible grit) {
		RecipeBuilder.Shapeless(MOD_ID)
			.addInput(Items.WHEAT)
			.addInput(Items.FOOD_FISH_RAW)
			.addInput(grit)
			.create(id, new ItemStack(MMItems.PET_FOOD, 3));
	}
}
