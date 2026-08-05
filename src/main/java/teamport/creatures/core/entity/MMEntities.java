package teamport.creatures.core.entity;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityDispatcher;
import net.minecraft.core.entity.factories.EntityFactory;
import net.minecraft.core.util.collection.NamespaceID;
import teamport.creatures.core.entity.mob.MobBigCat;
import teamport.creatures.core.entity.mob.MobRat;
import teamport.creatures.core.entity.mob.MobRatHell;
import teamport.creatures.core.entity.mob.MobMouse;
import teamport.creatures.core.entity.mob.MobDolphin;
import teamport.creatures.core.entity.mob.MobShark;
import teamport.creatures.core.entity.mob.MobFishy;
import teamport.creatures.core.entity.mob.MobSharkEgg;
import teamport.creatures.core.entity.mob.MobFishyEgg;
import teamport.creatures.core.entity.mob.MobOgre;
import teamport.creatures.core.entity.mob.MobOgreFire;
import teamport.creatures.core.entity.mob.MobOgreCave;
import teamport.creatures.core.entity.mob.MobWerewolf;
import teamport.creatures.core.entity.mob.MobWerewolfWolf;
import teamport.creatures.core.entity.mob.MobWraith;
import teamport.creatures.core.entity.mob.MobWraithFlame;
import teamport.creatures.core.entity.mob.MobBear;
import teamport.creatures.core.entity.mob.MobBird;
import teamport.creatures.core.entity.mob.MobBoar;
import teamport.creatures.core.entity.mob.MobBunny;
import teamport.creatures.core.entity.mob.MobDeerMoC;
import teamport.creatures.core.entity.mob.MobDuck;
import teamport.creatures.core.entity.mob.MobFox;
import teamport.creatures.core.entity.mob.MobHorse;
import teamport.creatures.core.entity.mob.MobHorsePegasus;
import teamport.creatures.core.entity.mob.MobHorseUnicorn;
import teamport.creatures.core.entity.mob.MobKitty;

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
		// Append-only: reordering these silently swaps mobs in worlds saved before the change.
		// MMAudit's fingerprint exists to catch exactly that.
		register(MobBear.class, "bear", MobBear::new);
		register(MobBird.class, "bird", MobBird::new);
		register(MobFox.class, "fox", MobFox::new);
		register(MobBunny.class, "bunny", MobBunny::new);
		register(MobBoar.class, "boar", MobBoar::new);
		register(MobDuck.class, "duck", MobDuck::new);
		register(MobHorse.class, "horse", MobHorse::new);
		register(MobHorseUnicorn.class, "horse_unicorn", "unicorn", MobHorseUnicorn::new);
		register(MobHorsePegasus.class, "horse_pegasus", "pegasus", MobHorsePegasus::new);
		register(MobKitty.class, "kitty", MobKitty::new);
		register(MobDeerMoC.class, "deer", MobDeerMoC::new);
		register(MobBigCat.class, "bigcat", MobBigCat::new);
		register(MobRat.class, "rat", MobRat::new);
		register(MobRatHell.class, "rat_hell", "rat_hell", MobRatHell::new);
		register(MobMouse.class, "mouse", MobMouse::new);
		register(MobDolphin.class, "dolphin", MobDolphin::new);
		register(MobShark.class, "shark", MobShark::new);
		register(MobFishy.class, "fishy", MobFishy::new);
		register(MobSharkEgg.class, "shark_egg", "shark_egg", MobSharkEgg::new);
		register(MobFishyEgg.class, "fishy_egg", "fishy_egg", MobFishyEgg::new);
		register(MobOgre.class, "ogre", MobOgre::new);
		register(MobOgreFire.class, "ogre_fire", "ogre_fire", MobOgreFire::new);
		register(MobOgreCave.class, "ogre_cave", "ogre_cave", MobOgreCave::new);
		register(MobWerewolf.class, "werewolf", MobWerewolf::new);
		register(MobWerewolfWolf.class, "werewolf_wolf", "werewolf_wolf", MobWerewolfWolf::new);
		register(MobWraith.class, "wraith", MobWraith::new);
		register(MobWraithFlame.class, "wraith_flame", "wraith_flame", MobWraithFlame::new);
	}
}
