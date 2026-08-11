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
import teamport.creatures.core.entity.mob.MobHorseNightmare;
import teamport.creatures.core.entity.mob.MobHorsePack;
import teamport.creatures.core.entity.mob.MobHorsePegasus;
import teamport.creatures.core.entity.mob.MobHorsePegasusBlack;
import teamport.creatures.core.entity.mob.MobHorseUnicorn;
import teamport.creatures.core.entity.mob.MobKitty;

import static teamport.creatures.MoreMobs.MOD_ID;

public final class MMEntities {
	private MMEntities() {}

	public static final java.util.List<String> REGISTERED_IDS = new java.util.ArrayList<>();

	public static final java.util.Map<String, String> REGISTERED_NAME_KEYS = new java.util.LinkedHashMap<>();

	private static <T extends Entity> void register(Class<T> entityClass, String name, String langName, EntityFactory<T> factory) {
		String nameKey = "guidebook.section.mob." + langName + ".name";
		EntityDispatcher.getInstance().addMapping(
			entityClass,
			NamespaceID.getPermanent(MOD_ID, name),
			factory,
			nameKey
		);
		REGISTERED_IDS.add(name);
		REGISTERED_NAME_KEYS.put(name, nameKey);
	}

	private static <T extends Entity> void register(Class<T> entityClass, String name, EntityFactory<T> factory) {
		register(entityClass, name, name, factory);
	}

	public static void initEntities() {

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

		register(MobHorsePack.class, "horse_pack", "horse_pack", MobHorsePack::new);
		register(MobHorseNightmare.class, "horse_nightmare", "horse_nightmare", MobHorseNightmare::new);
		register(MobHorsePegasusBlack.class, "horse_pegasus_black", "horse_pegasus_black",
			MobHorsePegasusBlack::new);
	}
}
