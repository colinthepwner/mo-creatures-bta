package teamport.creatures.core;

import turniplabs.halplibe.event.defs.CommonEvents;
import turniplabs.halplibe.helper.RecipeBuilder;
import turniplabs.halplibe.util.dependency.Key;

import static teamport.creatures.MoreMobs.MOD_ID;

/**
 * Recipe hookup. The {@code RecipeEntrypoint} interface was deprecated in HalpLibe 6.1.0 in favour
 * of the event system, so namespace init and recipe registration are wired up as listeners.
 */
public final class MMRecipes {
	private MMRecipes() {}

	public static void init() {
		CommonEvents.RECIPES_NAMESPACE_INIT.listen(Key.of(MOD_ID), () -> RecipeBuilder.initNameSpace(MOD_ID));
		CommonEvents.RECIPES_READY.listen(Key.of(MOD_ID), MMRecipes::onRecipesReady);
	}

	private static void onRecipesReady() {
	}
}
