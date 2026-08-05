package teamport.creatures;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import teamport.creatures.core.MMAudit;
import teamport.creatures.core.MMRecipes;
import teamport.creatures.core.block.MMBlocks;
import teamport.creatures.core.entity.MMEntities;
import teamport.creatures.core.item.MMItems;
import turniplabs.halplibe.HalpLibe;
import turniplabs.halplibe.event.defs.CommonEvents;
import turniplabs.halplibe.util.dependency.Key;

public class MoreMobs implements ModInitializer {
	public static final String MOD_ID = HalpLibe.registerMod("creatures", true);
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// IDEAS LIST
	// Baby animals
	// Handcannon bunnies
	// TNT bunnies
	// Seahorses
	// Horse armor
	// Bee swarms

	@Override
	public void onInitialize() {
		CommonEvents.BEFORE_GAME_START.listen(Key.of(MOD_ID), this::beforeGameStart);
		CommonEvents.AFTER_ITEM_INIT.listen(Key.of(MOD_ID), this::afterItemInit);
		MMRecipes.init();
		LOGGER.info("Mo Creatures has been initialized.");
	}

	public void beforeGameStart() {
		new MMBlocks();
		new MMItems();

		MMEntities.initEntities();
	}

	public void afterItemInit() {
		MMItems.addItemsToTags();
		MMAudit.run();
	}
}
