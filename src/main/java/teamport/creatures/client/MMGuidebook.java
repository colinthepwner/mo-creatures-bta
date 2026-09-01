package teamport.creatures.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.guidebook.mobs.MobInfoRegistry;
import net.minecraft.client.world.WorldEmpty;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.EntityDispatcher;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.item.ItemStack;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.entity.MMEntities;
import teamport.creatures.extra.mixin.MobScoreAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class MMGuidebook {
	private MMGuidebook() {}

	private static final float ALWAYS = 1.0F;

	public static void registerMobPages() {
		int registered = 0;
		List<String> skipped = new ArrayList<>();

		try {
			for (Map.Entry<Class<? extends Mob>, String> entry : MMEntities.REGISTERED_LANG_NAMES.entrySet()) {
				try {
					register(entry.getKey(), entry.getValue());
					registered++;
				} catch (Throwable t) {

					skipped.add(entry.getKey().getSimpleName());
					MoreMobs.LOGGER.warn("Guidebook: no page for {}", entry.getKey().getSimpleName(), t);
				}
			}
		} catch (Throwable t) {
			MoreMobs.LOGGER.error("Guidebook registration failed; this mod's mobs will be missing from "
				+ "the book. Deliberately swallowed: throwing here would cancel client start-up for "
				+ "every mod loaded after this one.", t);
			return;
		}

		if (skipped.isEmpty()) {
			MoreMobs.LOGGER.info("Guidebook: {} mob pages registered", registered);
		} else {
			MoreMobs.LOGGER.warn("Guidebook: {} mob pages registered, {} skipped: {}",
				registered, skipped.size(), String.join(", ", skipped));
		}
	}

	private static void register(Class<? extends Mob> mobClass, String langName) {
		Mob sample = EntityDispatcher.getInstance().createEntityInWorld(mobClass, WorldEmpty.INSTANCE);
		if (sample == null) {
			throw new IllegalStateException("the dispatcher would not build one");
		}

		MobInfoRegistry.register(
			mobClass,
			"guidebook.section.mob." + langName + ".name",
			"guidebook.section.mob." + langName + ".desc",
			sample.getMaxHealth(),
			((MobScoreAccessor) sample).creatures$getScoreValue(),
			dropsOf(sample)
		);
	}

	private static MobInfoRegistry.MobDrop[] dropsOf(Mob sample) {
		List<MobInfoRegistry.MobDrop> drops = new ArrayList<>();

		for (WeightedRandomLootObject loot : sample.mobDrops) {
			ItemStack stack = loot.getDefinedItemStack();
			if (stack == null) continue;

			int min = loot.isRandomYield() ? loot.getMinYield() : loot.getFixedYield();
			int max = loot.isRandomYield() ? loot.getMaxYield() : loot.getFixedYield();
			if (max <= 0) continue;

			drops.add(new MobInfoRegistry.MobDrop(stack, ALWAYS, Math.max(0, min), max));
		}

		return drops.toArray(new MobInfoRegistry.MobDrop[0]);
	}
}
