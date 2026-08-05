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

	/**
	 * Registers an entity with BTA's dispatcher. As of BTA 8.0 the display name is a
	 * language key rather than a literal, so each mob resolves through the mod's lang file.
	 */
	private static <T extends Entity> void register(Class<T> entityClass, String name, EntityFactory<T> factory) {
		EntityDispatcher.getInstance().addMapping(
			entityClass,
			NamespaceID.getPermanent(MOD_ID, name),
			factory,
			"entity." + MOD_ID + "." + name + ".name"
		);
	}

	public static void initEntities() {
		register(MobBear.class, "bear", MobBear::new);
		register(MobBird.class, "bird", MobBird::new);
		register(MobFox.class, "fox", MobFox::new);
		register(MobBunny.class, "bunny", MobBunny::new);
	}
}
