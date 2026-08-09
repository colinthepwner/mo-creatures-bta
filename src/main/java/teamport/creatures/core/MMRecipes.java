package teamport.creatures.core;

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
	}
}
