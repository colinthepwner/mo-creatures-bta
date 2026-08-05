package teamport.creatures.core.entity;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityDispatcher;
import net.minecraft.core.entity.factories.EntityFactory;
import net.minecraft.core.util.collection.NamespaceID;
import teamport.creatures.core.entity.mob.MobBear;
import teamport.creatures.core.entity.mob.MobBird;
import teamport.creatures.core.entity.mob.MobBunny;
import teamport.creatures.core.entity.mob.MobFox;

import static teamport.creatures.MoreMobs.MOD_ID;

public final class MMEntities {
	private MMEntities() {}

	/** Entity ids registered by this mod, in registration order. Used by {@link teamport.creatures.core.MMAudit}. */
	public static final java.util.List<String> REGISTERED_IDS = new java.util.ArrayList<>();

	/**
	 * Registers an entity with BTA's dispatcher. As of BTA 8.0 the display name is a language key
	 * rather than a literal. BTA resolves mob names through the guidebook namespace, so the key is
	 * {@code guidebook.section.mob.<langName>.name} — those entries already exist in
	 * {@code lang/creatures/en_US/guidebook.lang}.
	 *
	 * @param name     the entity id, which is also the network/save id
	 * @param langName the guidebook lang name, which is not always the entity id
	 *                 (e.g. {@code horse_unicorn} is documented as {@code unicorn})
	 */
	private static <T extends Entity> void register(Class<T> entityClass, String name, String langName, EntityFactory<T> factory) {
		EntityDispatcher.getInstance().addMapping(
			entityClass,
			NamespaceID.getPermanent(MOD_ID, name),
			factory,
			"guidebook.section.mob." + langName + ".name"
		);
		REGISTERED_IDS.add(name);
	}

	private static <T extends Entity> void register(Class<T> entityClass, String name, EntityFactory<T> factory) {
		register(entityClass, name, name, factory);
	}

	public static void initEntities() {
		register(MobBear.class, "bear", MobBear::new);
		register(MobBird.class, "bird", MobBird::new);
		register(MobFox.class, "fox", MobFox::new);
		register(MobBunny.class, "bunny", MobBunny::new);
	}
}
